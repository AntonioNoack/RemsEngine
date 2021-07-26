package me.anno.engine.physics

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_DEACTIVATION
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.dynamics.vehicle.WheelInfo
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.engine.ui.RenderView.Companion.camPosition
import me.anno.engine.ui.RenderView.Companion.viewTransform
import me.anno.gpu.GFX
import me.anno.input.Input
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3d
import org.lwjgl.glfw.GLFW
import javax.vecmath.Matrix4d
import javax.vecmath.Quat4d
import javax.vecmath.Vector3d
import kotlin.math.max
import kotlin.reflect.KClass

class BulletPhysics : Component() {

    // todo signed field to split meshes
    // todo with option for face subdivisions

    companion object {
        private val LOGGER = LogManager.getLogger(BulletPhysics::class)
        fun convertMatrix(ourTransform: Matrix4x3d): Matrix4d {
            return Matrix4d(// we have to transpose the matrix, because joml uses Axy and vecmath uses Ayx
                ourTransform.m00(), ourTransform.m10(), ourTransform.m20(), ourTransform.m30(),
                ourTransform.m01(), ourTransform.m11(), ourTransform.m21(), ourTransform.m31(),
                ourTransform.m02(), ourTransform.m12(), ourTransform.m22(), ourTransform.m32(),
                0.0, 0.0, 0.0, 1.0
            )
        }
    }

    // todo a play button

    // I use jBullet2, however I have modified it to use doubles for everything
    // this may be bad for performance, but it also allows our engine to run much larger worlds
    // if we need top-notch-performance, I just should switch to a native implementation

    private val sampleWheels = ArrayList<WheelInfo>()

    val entitiesNeedingUpdate = HashSet<Entity>()

    fun invalidate(entity: Entity) {
        println("invalidating ${entity.name}")
        entitiesNeedingUpdate.add(entity)
    }

    val world = createBulletWorldWithGroundNGravity()

    val rigidBodies = HashMap<Entity, RigidBody>()
    val raycastVehicles = HashMap<Entity, RaycastVehicle>()

    // todo ideally for bullet, we would need a non-symmetric matrix:
    //  --- types ---
    // t
    // y
    // p  whether it can be moved by the other
    // e
    // s
    // todo this would allow for pushing, ignoring, and such...

    init {
        // addSampleVehicle()
        /*val radius = 0.0
        for (i in 2 until 100) {
            addSampleSphere(
                cos(i * 0.01) * radius,
                i * 2.1,
                sin(i * 0.01) * radius
            )
        }*/
    }

    private fun <V : Component> getValidComponents(entity: Entity, clazz: KClass<V>): Sequence<V> {
        // only collect colliders, which are appropriate for this: stop at any other rigidbody
        return sequence {
            // todo also only collect physics colliders, not click-colliders
            yieldAll(entity.components.filter { it.isEnabled && clazz.isInstance(it) } as List<V>)
            for (child in entity.children) {
                if (child.isEnabled && !child.hasComponent<Rigidbody>(false)) {
                    yieldAll(getValidComponents(child, clazz))
                }
            }
        }
    }

    private fun createRigidbody(entity: Entity, base: Rigidbody): RigidBody? {

        val colliders = getValidComponents(entity, Collider::class).toList()
        return if (colliders.isNotEmpty()) {

            // copy all knowledge from ecs to bullet
            val firstCollider = colliders.first()
            val jointCollider: CollisionShape = if (colliders.size == 1 && firstCollider.entity === entity) {
                // there is only one, and no transform needs to be applied -> use it directly
                firstCollider.createBulletShape()
            } else {
                val jointCollider = CompoundShape()
                for (collider in colliders) {
                    val (transform, subCollider) = collider.createBulletCollider(entity)
                    jointCollider.addChildShape(transform, subCollider)
                }
                jointCollider
            }

            val mass = max(0.0, base.mass)
            val inertia = Vector3d()
            if (mass > 0) jointCollider.calculateLocalInertia(mass, inertia)

            val bulletTransform = Transform(convertMatrix(entity.transform.globalTransform))

            // convert the center of mass to a usable transform
            val com0 = base.centerOfMass
            val com1 = Vector3d(com0.x, com0.y, com0.z)
            val com2 = Transform(Matrix4d(Quat4d(0.0, 0.0, 0.0, 1.0), com1, 1.0))

            // create the motion state
            val motionState = DefaultMotionState(bulletTransform, com2)
            val rbInfo = RigidBodyConstructionInfo(mass, motionState, jointCollider, inertia)
            RigidBody(rbInfo)

        } else null

    }

    fun add(entity: Entity) {
        // todo add including constraints and such
        val rigidbody = entity.getComponent<Rigidbody>(false) ?: return
        if (rigidbody.isEnabled) {

            val body = createRigidbody(entity, rigidbody) ?: return

            // if vehicle, add vehicle
            if (rigidbody is Vehicle) {
                val tuning = VehicleTuning()
                tuning.frictionSlip = rigidbody.frictionSlip
                tuning.suspensionDamping = rigidbody.suspensionDamping
                tuning.suspensionStiffness = rigidbody.suspensionStiffness
                tuning.suspensionCompression = rigidbody.suspensionCompression
                tuning.maxSuspensionTravelCm = rigidbody.maxSuspensionTravelCm
                val raycaster = DefaultVehicleRaycaster(world)
                val vehicle = RaycastVehicle(tuning, body, raycaster)
                vehicle.setCoordinateSystem(0, 1, 2)
                val wheels = getValidComponents(entity, VehicleWheel::class)
                for (wheel in wheels) {
                    val info = wheel.createBulletInstance(entity, vehicle)
                    wheel.bulletInstance = info
                    sampleWheels.add(info)
                }
                // vehicle.currentSpeedKmHour
                // vehicle.applyEngineForce()
                world.addVehicle(vehicle)
                body.activationState = DISABLE_DEACTIVATION
                raycastVehicles[entity] = vehicle
            }

            world.addRigidBody(body) // todo what is the mask option, and the group?
            rigidBodies[entity] = body

        }
    }

    fun remove(entity: Entity) {
        val rigid = rigidBodies.remove(entity) ?: return
        world.removeRigidBody(rigid)
        val vehicle = raycastVehicles.remove(entity) ?: return
        world.removeVehicle(vehicle)
    }

    fun update(entity: Entity) {
        remove(entity)
        add(entity)
    }

    var time = 0.0
    fun step(dt: Double) {

        // simple but effective
        for (entity in entitiesNeedingUpdate) {
            remove(entity)
            add(entity)
        }
        entitiesNeedingUpdate.clear()

        // better for shooters
        // if non-stable dt-s are used, it will not be deterministic
        val immediateChanges = false
        world.stepSimulation(dt)
        time += dt
        val tmpTransform = Transform()
        for ((entity, rigidbody) in rigidBodies) {
            val dst: me.anno.ecs.Transform = if (immediateChanges) {
                entity.nextTransform = null // not needed / available
                entity.transform
            } else {
                val tmp = entity.nextTransform ?: entity.transform.clone()
                entity.nextTransform = entity.transform
                entity.transform = tmp
                entity.nextTransform!!
            }
            // set the global transform
            rigidbody.getWorldTransform(tmpTransform)
            val basis = tmpTransform.basis
            val origin = tmpTransform.origin
            // bullet/javax uses normal ij indexing, while joml uses ji indexing
            dst.globalTransform.set(
                basis.m00, basis.m10, basis.m20,
                basis.m01, basis.m11, basis.m21,
                basis.m02, basis.m12, basis.m22,
                origin.x, origin.y, origin.z
            )
            dst.time = time
        }
        // update the local transforms last, so all global transforms have been completely updated
        for (entity in rigidBodies.keys) {
            val dst = if (immediateChanges) entity.transform
            else entity.nextTransform!!
            dst.calculateLocalTransform(entity.parent?.run { nextTransform ?: transform })
        }
        // todo update all transforms, where needed
    }

    private var debugDraw: BulletDebugDraw? = null
    override fun onDrawGUI() {
        super.onDrawGUI()

        val debugDraw = debugDraw ?: return

        // if this works, the world is magical <3

        // define camera transform
        val stack = debugDraw.stack
        stack.clear()
        stack.set(viewTransform)
        debugDraw.cam.set(camPosition)

        // draw the debug world
        stack.pushMatrix()
        world.debugDrawWorld()
        stack.popMatrix()

        var steering = 0.0
        var engineForce = 0.0
        var brakeForce = 0.0
        if (GLFW.GLFW_KEY_SPACE in Input.keysDown) {
            brakeForce++
        }
        if (GLFW.GLFW_KEY_UP in Input.keysDown) {
            engineForce++
        }
        if (GLFW.GLFW_KEY_DOWN in Input.keysDown) {
            engineForce--
        }
        if (GLFW.GLFW_KEY_LEFT in Input.keysDown) {
            steering += 0.5
        }
        if (GLFW.GLFW_KEY_RIGHT in Input.keysDown) {
            steering -= 0.5
        }

        for (wheel in sampleWheels) {
            wheel.engineForce = if (wheel.bIsFrontWheel) 0.0 else engineForce
            wheel.steering = if (wheel.bIsFrontWheel) steering else 0.0
            wheel.brake = brakeForce
        }

        // println("$engineForce x $steering x $brakeForce")

        if (!Input.isShiftDown) {
            step(GFX.deltaTime.toDouble())
        }

    }

    private fun addSampleVehicle() {
        val entity = Entity()
        entity.transform.globalTransform.translate(0.0, 1.0, 0.0)
        entity.add(Vehicle().apply { centerOfMass.set(0.0, -0.5, 0.0) })
        entity.add(BoxCollider().apply { halfExtends.set(1.0, 0.5, 2.0) })
        // add wheels
        for (x in -1..1 step 2) {
            for (z in -1..1 step 2) {
                entity.add(Entity().apply {
                    transform.globalTransform.translate(x * 1.1, 1.0, z * 2.0)
                    add(VehicleWheel().apply {
                        radius = 0.7
                        isFront = z > 0
                    })
                })
            }
        }
        // todo add debug controls for vehicle testing
        add(entity)
    }

    private fun addSampleSphere(x: Double, y: Double, z: Double) {
        val entity = Entity()
        entity.transform.localPosition = org.joml.Vector3d(x, y, z)
        entity.transform.invalidateGlobal()
        entity.updateTransform()
        entity.add(Rigidbody())
        entity.add(SphereCollider())
        add(entity)
    }

    private fun createBulletWorld(): DiscreteDynamicsWorld {
        val collisionConfig = DefaultCollisionConfiguration()
        val dispatcher = CollisionDispatcher(collisionConfig)
        val bp = DbvtBroadphase()
        val solver = SequentialImpulseConstraintSolver()
        val world = DiscreteDynamicsWorld(dispatcher, bp, solver, collisionConfig)
        debugDraw = debugDraw ?: BulletDebugDraw()
        world.debugDrawer = debugDraw
        return world
    }

    private fun createBulletWorldWithGroundNGravity(): DiscreteDynamicsWorld {
        val world = createBulletWorld()
        world.setGravity(Vector3d(0.0, -9.81, 0.0))
        // addGround(world)
        return world
    }

    private fun addGround(world: DiscreteDynamicsWorld) {

        val ground = BoxShape(Vector3d(2000.0, 100.0, 2000.0))

        val groundTransform = Transform()
        groundTransform.setIdentity()
        groundTransform.origin.set(0.0, -100.0, 0.0)

        // mass = 0f = static
        val motionState = DefaultMotionState(groundTransform)
        val rbInfo = RigidBodyConstructionInfo(0.0, motionState, ground, Vector3d())
        val body = RigidBody(rbInfo)

        world.addRigidBody(body)

    }

    override val className: String = "BulletPhysics"

}