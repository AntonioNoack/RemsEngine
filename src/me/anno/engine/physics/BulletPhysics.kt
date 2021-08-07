package me.anno.engine.physics

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_DEACTIVATION
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
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
import cz.advel.stack.Stack
import me.anno.config.DefaultStyle.black
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.Rigidbody
import me.anno.ecs.components.physics.Vehicle
import me.anno.ecs.components.physics.VehicleWheel
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.camPosition
import me.anno.engine.ui.render.RenderView.Companion.viewTransform
import me.anno.gpu.GFX
import me.anno.gpu.buffer.LineBuffer
import me.anno.input.Input
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.debug.FrameTimes
import me.anno.utils.Clock
import me.anno.utils.hpc.SyncMaster
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
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
        fun convertMatrix(ourTransform: Matrix4x3d, scale: org.joml.Vector3d): Matrix4d {
            // bullet does not support scale -> we always need to correct it
            val sx = 1.0 / scale.x
            val sy = 1.0 / scale.y
            val sz = 1.0 / scale.z
            return Matrix4d(// we have to transpose the matrix, because joml uses Axy and vecmath uses Ayx
                ourTransform.m00() * sx, ourTransform.m10() * sy, ourTransform.m20() * sz, ourTransform.m30(),
                ourTransform.m01() * sx, ourTransform.m11() * sy, ourTransform.m21() * sz, ourTransform.m31(),
                ourTransform.m02() * sx, ourTransform.m12() * sy, ourTransform.m22() * sz, ourTransform.m32(),
                0.0, 0.0, 0.0, 1.0
            )
        }
    }

    @SerializedProperty
    var automaticDeathHeight = -100.0

    // todo a play button

    // I use jBullet2, however I have modified it to use doubles for everything
    // this may be bad for performance, but it also allows our engine to run much larger worlds
    // if we need top-notch-performance, I just should switch to a native implementation

    @NotSerializedProperty
    private val sampleWheels = ArrayList<WheelInfo>()

    @NotSerializedProperty
    private var enu1 = HashSet<Entity>()
    private var enu2 = HashSet<Entity>()

    fun invalidate(entity: Entity) {
        synchronized(enu1) {
            enu1.add(entity)
        }
    }

    private fun validate() {
        synchronized(enu1) {
            val tmp = enu1
            enu1 = enu2
            enu2 = tmp
        }
        for (entity in enu2) {
            update(entity)
        }
        enu2.clear()
    }

    @NotSerializedProperty
    private val world = createBulletWorldWithGroundNGravity()

    @NotSerializedProperty
    private val rigidBodies = HashMap<Entity, Pair<org.joml.Vector3d, RigidBody>>()

    private val nonStaticRigidBodies = HashMap<Entity, Pair<org.joml.Vector3d, RigidBody>>()

    @NotSerializedProperty
    private val raycastVehicles = HashMap<Entity, RaycastVehicle>()

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
                if (child.isEnabled && !child.hasComponent(Rigidbody::class, false)) {
                    yieldAll(getValidComponents(child, clazz))
                }
            }
        }
    }

    private fun createRigidbody(entity: Entity, base: Rigidbody): Pair<org.joml.Vector3d, RigidBody>? {

        val colliders = getValidComponents(entity, Collider::class).toList()
        return if (colliders.isNotEmpty()) {

            // bullet does not work correctly with scale changes: create larger shapes directly
            val globalTransform = entity.transform.globalTransform
            val scale = globalTransform.getScale(org.joml.Vector3d())

            // copy all knowledge from ecs to bullet
            val firstCollider = colliders.first()
            val jointCollider: CollisionShape = if (colliders.size == 1 && firstCollider.entity === entity) {
                // there is only one, and no transform needs to be applied -> use it directly
                firstCollider.createBulletShape(scale)
            } else {
                val jointCollider = CompoundShape()
                for (collider in colliders) {
                    val (transform, subCollider) = collider.createBulletCollider(entity, scale)
                    jointCollider.addChildShape(transform, subCollider)
                }
                jointCollider
            }

            val mass = max(0.0, base.mass)
            val inertia = Vector3d()
            if (mass > 0) jointCollider.calculateLocalInertia(mass, inertia)

            val bulletTransform = Transform(convertMatrix(globalTransform, scale))

            // convert the center of mass to a usable transform
            val com0 = base.centerOfMass
            val com1 = Vector3d(com0.x, com0.y, com0.z)
            val com2 = Transform(Matrix4d(Quat4d(0.0, 0.0, 0.0, 1.0), com1, 1.0))

            // create the motion state
            val motionState = DefaultMotionState(bulletTransform, com2)
            val rbInfo = RigidBodyConstructionInfo(mass, motionState, jointCollider, inertia)
            rbInfo.friction = base.friction
            rbInfo.restitution = base.restitution
            rbInfo.linearDamping = base.linearDamping
            rbInfo.angularDamping = base.angularDamping
            rbInfo.linearSleepingThreshold = base.linearSleepingThreshold
            rbInfo.angularSleepingThreshold = base.angularSleepingThreshold

            val rb = RigidBody(rbInfo)
            // rb.deactivationTime = base.sleepingTimeThreshold
            BulletGlobals.setDeactivationTime(1.0)
            /*
            *  if (getActivationState() == DISABLE_DEACTIVATION) {
            return false;
        }

        // disable deactivation
        if (BulletGlobals.isDeactivationDisabled() || (BulletGlobals.getDeactivationTime() == 0f)) {
            return false;
        }

        if ((getActivationState() == ISLAND_SLEEPING) || (getActivationState() == WANTS_DEACTIVATION)) {
            return true;
        }

        if (deactivationTime > BulletGlobals.getDeactivationTime()) {
            return true;
        }
        return false;
            * */
            // println("state: ${rb.activationState}, ${rb.deactivationTime}")

            scale to rb

        } else null

    }

    fun add(entity: Entity): RigidBody? {
        // todo add including constraints and such
        val rigidbody = entity.getComponent(Rigidbody::class, false) ?: return null
        if (rigidbody.isEnabled) {

            val bodyWithScale = createRigidbody(entity, rigidbody) ?: return null
            val (scale, body) = bodyWithScale

            // todo correctly create vehicle, if the body is scaled

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
            rigidBodies[entity] = bodyWithScale
            rigidbody.bulletInstance = body

            if (!rigidbody.isStatic) {
                nonStaticRigidBodies[entity] = bodyWithScale
            }

            return body

        }

        return null

    }

    fun remove(entity: Entity) {
        val rigid = rigidBodies.remove(entity) ?: return
        nonStaticRigidBodies.remove(entity)
        world.removeRigidBody(rigid.second)
        val vehicle = raycastVehicles.remove(entity) ?: return
        world.removeVehicle(vehicle)
        entity.isPhysicsControlled = false
    }

    private fun update(entity: Entity) {
        remove(entity)
        entity.isPhysicsControlled = add(entity) != null
    }

    @SerializedProperty
    var targetUpdatesPerSecond = 30.0

    val clock = Clock()

    var time = 0L

    fun callUpdates() {
        val tmp = Stack.borrowTrans()
        for ((body, scaledBody) in rigidBodies) {
            // val physics = scaledBody.second
            // physics.clearForces() // needed???...
            // testing force: tornado
            // physics.getWorldTransform(tmp)
            // val f = 1.0 + 0.01 * sq(tmp.origin.x, tmp.origin.z)
            // physics.applyCentralForce(Vector3d(tmp.origin.z / f, 0.0, -tmp.origin.x / f))
            body.physicsUpdate()
        }
    }

    fun step(dt: Long, printSlack: Boolean) {

        // clock.start()

        // just in case
        Stack.reset(printSlack)

        // val oldSize = rigidBodies.size
        validate()
        // val newSize = rigidBodies.size
        // clock.stop("added ${newSize - oldSize} entities")

        callUpdates()

        val step = dt * 1e-9
        world.stepSimulation(step, 1, step)

        // clock.stop("calculated changes, step ${dt * 1e-9}", 0.1)

        this.time += dt

        // is not correct for the physics, but we use it for gfx only anyways
        val time = GFX.gameTime

        val tmpTransform = Transform()
        val deadEntities = ArrayList<Entity>()
        val deadRigidBodies = ArrayList<RigidBody>()

        for ((entity, rigidbodyWithScale) in nonStaticRigidBodies) {

            val (scale, rigidbody) = rigidbodyWithScale
            // if (rigidbody.isStaticObject) continue

            // set the global transform
            rigidbody.getWorldTransform(tmpTransform)
            if (tmpTransform.origin.y < automaticDeathHeight) {
                // delete the entity
                deadEntities.add(entity)
                deadRigidBodies.add(rigidbody)
                continue
            }

            val dst = entity.transform
            val basis = tmpTransform.basis
            val origin = tmpTransform.origin
            // bullet/javax uses normal ij indexing, while joml uses ji indexing
            val sx = scale.x
            val sy = scale.y
            val sz = scale.z

            dst.globalTransform.set(
                basis.m00 * sx, basis.m10 * sy, basis.m20 * sz,
                basis.m01 * sx, basis.m11 * sy, basis.m21 * sz,
                basis.m02 * sx, basis.m12 * sy, basis.m22 * sz,
                origin.x, origin.y, origin.z
            )

            dst.update(time, entity, true)

        }

        for (i in deadEntities.indices) {
            remove(deadEntities[i])
            world.removeRigidBody(deadRigidBodies[i])
            // todo kill or disable?
        }

        // update the local transforms last, so all global transforms have been completely updated
        for (entity in nonStaticRigidBodies.keys) {
            val dst = entity.transform
            dst.calculateLocalTransform((entity.parent as? Entity)?.transform)
            entity.invalidateAABBsCompletely()
        }

        // clock.total("physics step", 0.1)

    }

    fun drawDebug(view: RenderView, worldScale: Double) {

        val debugDraw = debugDraw ?: return

        // define camera transform
        debugDraw.stack.set(viewTransform)
        debugDraw.worldScale = worldScale
        debugDraw.cam.set(camPosition)

        if (Input.isKeyDown('y')) {
            drawContactPoints(view)
            drawAABBs(view)
            drawVehicles(view)
            LineBuffer.finish(viewTransform)
        }

    }

    private fun drawContactPoints(view: RenderView) {
        val dispatcher = world.dispatcher
        val numManifolds: Int = dispatcher.numManifolds
        for (i in 0 until numManifolds) {
            val contactManifold = dispatcher.getManifoldByIndexInternal(i) ?: break
            val numContacts = contactManifold.numContacts
            for (j in 0 until numContacts) {
                val cp = contactManifold.getContactPoint(j)
                DrawAABB.drawLine(
                    cp.positionWorldOnB,
                    Vector3d(cp.positionWorldOnB).apply { add(cp.normalWorldOnB) },
                    view.worldScale, 0.5, 0.5, 0.5
                )
            }
        }
    }

    private fun drawAABBs(view: RenderView) {

        val tmpTrans = Stack.newTrans()
        val minAabb = Vector3d()
        val maxAabb = Vector3d()

        val collisionObjects = world.collisionObjectArray

        val worldScale = view.worldScale
        val color = Vector3d()

        for (i in 0 until collisionObjects.size) {

            val colObj = collisionObjects.getQuick(i) ?: break
            when (colObj.activationState) {
                CollisionObject.ACTIVE_TAG -> color.set(1.0, 1.0, 1.0)
                CollisionObject.ISLAND_SLEEPING -> color.set(0.0, 1.0, 0.0)
                CollisionObject.WANTS_DEACTIVATION -> color.set(0.0, 1.0, 1.0)
                CollisionObject.DISABLE_DEACTIVATION -> color.set(1.0, 0.0, 0.0)
                CollisionObject.DISABLE_SIMULATION -> color.set(1.0, 1.0, 0.0)
                else -> color.set(1.0, 0.0, 0.0)
            }

            // todo draw the local coordinate arrows
            // debugDrawObject(colObj.getWorldTransform(tmpTrans), colObj.collisionShape, color)

            colObj.collisionShape.getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)

            DrawAABB.drawAABB(
                AABBd()
                    .setMin(minAabb.x, minAabb.y, minAabb.z)
                    .setMax(maxAabb.x, maxAabb.y, maxAabb.z),
                worldScale,
                color.x, color.y, color.z
            )
        }

    }

    private fun drawVehicles(view: RenderView) {

        val wheelColor = Vector3d()
        val wheelPosWS = Vector3d()
        val axle = Vector3d()
        val tmp = Stack.newVec()

        val worldScale = view.worldScale

        val vehicles = world.vehicles
        for (i in 0 until vehicles.size) {
            val vehicle = vehicles.getQuick(i) ?: break
            for (v in 0 until vehicle.numWheels) {

                if (vehicle.getWheelInfo(v).raycastInfo.isInContact) {
                    wheelColor.set(0.0, 0.0, 1.0)
                } else {
                    wheelColor.set(1.0, 0.0, 0.0)
                }

                wheelPosWS.set(vehicle.getWheelInfo(v).worldTransform.origin)
                axle.set(
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(0, vehicle.rightAxis),
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(1, vehicle.rightAxis),
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(2, vehicle.rightAxis)
                )

                tmp.add(wheelPosWS, axle)
                DrawAABB.drawLine(wheelPosWS, tmp, worldScale, wheelColor.x, wheelColor.y, wheelColor.z)
                DrawAABB.drawLine(
                    wheelPosWS, vehicle.getWheelInfo(v).raycastInfo.contactPointWS,
                    worldScale, wheelColor.x, wheelColor.y, wheelColor.z
                )

            }
        }

        val actions = world.actions
        for (i in 0 until actions.size) {
            val action = actions.getQuick(i) ?: break
            action.debugDraw(debugDraw)
        }

    }

    @HideInInspector
    @NotSerializedProperty
    var syncMaster: SyncMaster? = null
    fun startWork(syncMaster: SyncMaster) {
        this.syncMaster = syncMaster
        var first = true
        syncMaster.addThread("Physics", {

            if (first) {
                Thread.sleep(2000)
                this.time = GFX.gameTime
                first = false
                LOGGER.warn("Starting physics")
            }

            val targetUPS = targetUpdatesPerSecond
            val targetStep = 1.0 / targetUPS
            val targetStepNanos = (targetStep * 1e9).toLong()

            //  todo if too far back in time, just simulate that we are good

            val targetTime = GFX.gameTime
            val absMinimumTime = targetTime - targetStepNanos * 2

            if (this.time < absMinimumTime) {
                // todo report this value somehow...
                // todo there may be lots and lots of warnings, if the calculations are too slow
                // val delta = absMinimumTime - this.time
                this.time = absMinimumTime
                // LOGGER.warn("Physics skipped ${(delta * 1e-9)}s")
            }

            if (this.time > targetTime) {
                // done :), sleep
                (this.time - targetTime) / 2
            } else {
                // there is still work to do
                val t0 = System.nanoTime()
                val debug = false //GFX.gameTime > 10e9 // wait 10s
                if (debug) {
                    Stack.printClassUsage()
                    Stack.printSizes()
                }
                step(targetStepNanos, debug)
                val t1 = System.nanoTime()
                addEvent { FrameTimes.putValue((t1 - t0) * 1e-9f, 0xffff99 or black) }
                0
            }

        }, { debugDraw })
    }


    private var debugDraw: BulletDebugDraw? = null
    override fun onDrawGUI(view: RenderView) {
        super.onDrawGUI(view)

        drawDebug(view, view.worldScale)

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
            steering++
        }
        if (GLFW.GLFW_KEY_RIGHT in Input.keysDown) {
            steering--
        }

        try {
            for (wheel in sampleWheels) {
                wheel.engineForce = if (wheel.bIsFrontWheel) 0.0 else engineForce
                wheel.steering = if (wheel.bIsFrontWheel) steering * 0.5 else 0.0
                wheel.brake = brakeForce
            }
        } catch (e: ConcurrentModificationException) {
            // will flicker a little, when cars are spawned/de-spawned
        }

    }

    /*private fun addSampleVehicle() {
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
        add(entity)
    }*/

    /*private fun addSampleSphere(x: Double, y: Double, z: Double) {
        val entity = Entity()
        entity.transform.localPosition = org.joml.Vector3d(x, y, z)
        entity.transform.invalidateGlobal()
        entity.validateTransforms()
        entity.add(Rigidbody())
        entity.add(SphereCollider())
        add(entity)
    }*/

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
        return world
    }

    override val className: String = "BulletPhysics"

}