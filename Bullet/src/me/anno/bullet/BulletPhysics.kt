package me.anno.bullet

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
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
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.allComponents
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.BodyWithScale
import me.anno.ecs.components.physics.Physics
import me.anno.ecs.components.physics.events.FallenOutOfWorld
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderState.cameraMatrix
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.Logging.hash32
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3f
import javax.vecmath.Quat4d
import javax.vecmath.Vector3d
import kotlin.math.max

open class BulletPhysics : Physics<Rigidbody, RigidBody>(Rigidbody::class) {

    // todo onPreEnable() // before all children
    // todo onPostEnable() // after all children
    // -> components can be registered before/after enable :)

    // I use jBullet2, however I have modified it to use doubles for everything
    // this may be bad for performance, but it also allows our engine to run much larger worlds
    // if we need top-notch-performance, I just should switch to a native implementation

    // todo ideally for bullet, we would need a non-symmetric collision matrix:
    // this would allow for pushing, ignoring, and such
    //
    //   t y p e s
    // t
    // y
    // p  whether it can be moved by the other
    // e
    // s

    @NotSerializedProperty
    private val sampleWheels = ArrayList<WheelInfo>()

    @NotSerializedProperty
    var bulletInstance: DiscreteDynamicsWorld? = null

    @NotSerializedProperty
    var raycastVehicles: HashMap<Entity, RaycastVehicle>? = null

    override fun onCreate() {
        super.onCreate()
        raycastVehicles = HashMap()
        bulletInstance = createBulletWorldWithGravity()
    }

    private fun createCollider(entity: Entity, colliders: List<Collider>, scale: org.joml.Vector3d): CollisionShape {
        val firstCollider = colliders.first()
        return if (colliders.size == 1 && firstCollider.entity === entity) {
            // there is only one, and no transform needs to be applied -> use it directly
            createBulletShape(firstCollider, scale)
        } else {
            val jointCollider = CompoundShape()
            for (collider in colliders) {
                val (transform, subCollider) = createBulletCollider(collider, entity, scale)
                jointCollider.addChildShape(transform, subCollider)
            }
            jointCollider
        }
    }

    private val inertia = Vector3d()
    override fun createRigidbody(entity: Entity, rigidBody: Rigidbody): BodyWithScale<RigidBody>? {

        val colliders = getValidComponents(entity, Collider::class)
            .filter { it.hasPhysics }.toList()
        return if (colliders.isNotEmpty()) {

            // bullet does not work correctly with scale changes: create larger shapes directly
            val globalTransform = entity.transform.globalTransform
            val scale = globalTransform.getScale(org.joml.Vector3d())

            // copy all knowledge from ecs to bullet
            val collider = createCollider(entity, colliders, scale)

            val mass = max(0.0, rigidBody.mass)
            if (mass > 0.0) collider.calculateLocalInertia(mass, inertia)

            val transform1 = mat4x3ToTransform(globalTransform, scale)

            // convert the center of mass to a usable transform
            val com0 = rigidBody.centerOfMass
            val com1 = Transform()
            com1.basis.setIdentity()
            com1.origin.set(com0.x, com0.y, com0.z)

            // create the motion state
            val motionState = DefaultMotionState(transform1, com1)
            val rbInfo = RigidBodyConstructionInfo(mass, motionState, collider, inertia)
            rbInfo.friction = rigidBody.friction
            rbInfo.restitution = rigidBody.restitution
            rbInfo.linearDamping = rigidBody.linearDamping
            rbInfo.angularDamping = rigidBody.angularDamping
            rbInfo.linearSleepingThreshold = rigidBody.linearSleepingThreshold
            rbInfo.angularSleepingThreshold = rigidBody.angularSleepingThreshold

            val rb = RigidBody(rbInfo)
            rb.deactivationTime = rigidBody.sleepingTimeThreshold
            rb.ccdMotionThreshold = 1e-7
            val sp = Vector3d()
            val r = DoubleArray(1)
            collider.getBoundingSphere(sp, r)
            rb.ccdSweptSphereRadius = r[0]

            BodyWithScale(rb, scale)
        } else null
    }

    private fun defineVehicle(entity: Entity, vehicleComp: Vehicle, body: RigidBody) {
        // todo correctly create vehicle, if the body is scaled
        val tuning = VehicleTuning()
        tuning.frictionSlip = vehicleComp.frictionSlip
        tuning.suspensionDamping = vehicleComp.suspensionDamping
        tuning.suspensionStiffness = vehicleComp.suspensionStiffness
        tuning.suspensionCompression = vehicleComp.suspensionCompression
        tuning.maxSuspensionTravelCm = vehicleComp.maxSuspensionTravelCm
        val world = bulletInstance!!
        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(tuning, body, raycaster)
        vehicle.setCoordinateSystem(0, 1, 2)
        val wheels = vehicleComp.wheels
        for (wheel in wheels) {
            val info = wheel.createBulletInstance(entity, vehicle)
            info.clientInfo = wheel
            wheel.bulletInstance = info
            sampleWheels.add(info)
        }
        // vehicle.currentSpeedKmHour
        // vehicle.applyEngineForce()
        world.addVehicle(vehicle)
        body.activationState = CollisionObject.DISABLE_DEACTIVATION
        raycastVehicles!![entity] = vehicle
    }

    override fun onCreateRigidbody(entity: Entity, rigidbody: Rigidbody, bodyWithScale: BodyWithScale<RigidBody>) {

        val body = bodyWithScale.body

        // vehicle stuff
        if (rigidbody is Vehicle) {
            defineVehicle(entity, rigidbody, body)
        }

        // activate
        if (rigidbody.activeByDefault) body.activationState = CollisionObject.ACTIVE_TAG

        bulletInstance!!.addRigidBody(
            body, // todo re-activate / correct groups and masks
            // clamp(rigidbody.group, 0, 15).toShort(),
            // rigidbody.collisionMask
        )

        // must be done after adding the body to the world,
        // because it is overridden by World.addRigidbody()
        if (rigidbody.overrideGravity) {
            body.setGravity(castB(rigidbody.gravity))
        }

        rigidBodies[entity] = bodyWithScale
        rigidbody.bulletInstance = body

        registerNonStatic(entity, rigidbody.isStatic, bodyWithScale)

        val constraints = rigidbody.constraints
        for (i in constraints.indices) {
            val constraint = constraints[i]
            if (constraint.isEnabled) {
                // ensure the constraint exists
                val rigidbody2 = constraint.entity!!.getComponent(Rigidbody::class, false)!!
                addConstraint(constraint, getRigidbody(rigidbody2)!!, rigidbody2, rigidbody)
            }
        }

        // create all constraints
        entity.allComponents(Constraint::class, false) { c ->
            val other = c.other
            if (other != null && other != rigidbody && other.isEnabled) {
                addConstraint(c, body, rigidbody, other)
            }
            false
        }
    }

    private fun addConstraint(c: Constraint<*>, body: RigidBody, rigidbody: Rigidbody, other: Rigidbody) {
        val oldInstance = c.bulletInstance
        val world = bulletInstance!!
        if (oldInstance != null) {
            world.removeConstraint(oldInstance)
            c.bulletInstance = null
        }
        if (!rigidbody.isStatic || !other.isStatic) {
            val otherBody = getRigidbody(other)
            if (otherBody != null) {
                // create constraint
                val constraint = c.createConstraint(body, otherBody, c.getTA(), c.getTB())
                c["bulletInstance"] = constraint
                world.addConstraint(constraint, c.disableCollisionsBetweenLinked)
            }
        } else {
            LOGGER.warn("Cannot constrain two static bodies!, ${rigidbody.prefabPath} to ${other.prefabPath}")
        }
    }

    override fun removeConstraints(entity: Entity) {
        entity.forAllComponents(Constraint::class) {
            val other = it.other?.entity
            if (other != null) {
                remove(other, false)
            }
        }
    }

    override fun remove(entity: Entity, fallenOutOfWorld: Boolean) {
        super.remove(entity, fallenOutOfWorld)
        val world = bulletInstance!!
        entity.forAllComponents(Constraint::class) {
            val bi = it.bulletInstance
            if (bi != null) {
                it.bulletInstance = null
                world.removeConstraint(bi)
                // LOGGER.debug("- ${it.prefabPath}")
            }
        }
        val rigid2 = entity.getComponent(Rigidbody::class, false)
        if (rigid2 != null) {
            for (c in rigid2.constraints) {
                val bi = c.bulletInstance
                if (bi != null) {
                    world.removeConstraint(bi)
                    c.bulletInstance = null
                    // LOGGER.debug("- ${c.prefabPath}")
                }
            }
        }
        val vehicle = raycastVehicles?.remove(entity) ?: return
        world.removeVehicle(vehicle)
        entity.isPhysicsControlled = false
        if (fallenOutOfWorld) {
            if (rigid2 != null) {
                // when something falls of the world, often it's nice to directly destroy the object,
                // because it will no longer be needed
                // call event, so e.g., we could add it back to a pool of entities, or respawn it
                entity.forAllComponents(Component::class) {
                    if (it is FallenOutOfWorld) it.onFallOutOfWorld()
                }
                if (rigid2.deleteWhenKilledByDepth) {
                    entity.parentEntity?.deleteChild(entity)
                }
            }
        }
    }

    override fun step(dt: Long, printSlack: Boolean) {
        // just in case
        if (printSlack) {
            Stack.printClassUsage()
            Stack.printSizes()
        }
        Stack.reset(printSlack)
        super.step(dt, printSlack)
    }

    var maxSubSteps = 16
    var fixedStep = 1.0 / 120.0 // 0.0 for flexible steps

    override fun worldStepSimulation(step: Double) {
        try {
            Stack.reset(false)
            bulletInstance?.stepSimulation(step, maxSubSteps, if (fixedStep <= 0.0) step else fixedStep)
        } catch (e: Exception) {
            e.printStackTrace()
            LOGGER.warn("Crashed thread: ${Thread.currentThread().name}")
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            LOGGER.warn("Crashed thread: ${Thread.currentThread().name}")
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            LOGGER.warn("Crashed thread: ${Thread.currentThread().name}")
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            LOGGER.warn("Crashed thread: ${Thread.currentThread().name}")
        }
    }

    override fun isActive(rigidbody: RigidBody): Boolean {
        return rigidbody.isActive
    }

    override fun worldRemoveRigidbody(rigidbody: RigidBody) {
        bulletInstance?.removeRigidBody(rigidbody)
    }

    override fun convertTransformMatrix(rigidbody: RigidBody, scale: org.joml.Vector3d, dstTransform: Matrix4x3d) {
        val tmpTransform = Stack.borrowTrans()
        rigidbody.getWorldTransform(tmpTransform)
        transformToMat4x3(tmpTransform, scale, dstTransform)
    }

    override fun updateWheels() {
        super.updateWheels()
        val scale = JomlPools.vec3d.create()
        scale.set(1.0)
        for ((_, v) in raycastVehicles!!) {
            val wheelInfos = v.wheelInfo
            for (i in wheelInfos.indices) {
                val wheel = v.getWheelInfo(i).clientInfo as? VehicleWheel ?: continue
                val entity = wheel.entity ?: continue
                val dst = entity.transform
                val dstTransform = dst.globalTransform
                // todo use correct scale
                val tr = wheelInfos[i].worldTransform
                transformToMat4x3(tr, scale, dstTransform)
                dst.setStateAndUpdate(me.anno.ecs.Transform.State.VALID_GLOBAL)
            }
        }
        JomlPools.vec3d.sub(1)
    }

    private fun drawDebug(view: RenderView?) {

        val debugDraw = debugDraw ?: return

        // define camera transform
        debugDraw.stack.set(cameraMatrix)
        debugDraw.cam.set(cameraPosition)

        if (view == null || showDebug || view.renderMode == RenderMode.PHYSICS) {
            drawContactPoints()
            drawAABBs()
            drawVehicles()
        }
    }

    private fun drawContactPoints() {
        val dispatcher = bulletInstance?.dispatcher ?: return
        val numManifolds = dispatcher.numManifolds
        val worldScale = RenderState.worldScale
        val cam = cameraPosition
        val color = black or 0x777777
        for (i in 0 until numManifolds) {
            val contactManifold = dispatcher.getManifoldByIndexInternal(i) ?: break
            for (j in 0 until contactManifold.numContacts) {
                val cp = contactManifold.getContactPoint(j)
                val a = cp.positionWorldOnB
                val n = cp.normalWorldOnB
                addLine(
                    (a.x - cam.x) * worldScale,
                    (a.y - cam.y) * worldScale,
                    (a.z - cam.z) * worldScale,
                    (a.x + n.x - cam.x) * worldScale,
                    (a.y + n.y - cam.y) * worldScale,
                    (a.z + n.z - cam.z) * worldScale,
                    color
                )
            }
        }
    }

    override fun createInspector(
        list: PanelListY, style: Style,
        getGroup: (nameDesc: NameDesc) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        list.add(
            TextPanel(
                "" +
                        "States:\n" +
                        "- active: white\n" +
                        "- island sleeping: green\n" +
                        "- wants deactivation: violet\n" +
                        "- disable deactivation: red\n" +
                        "- disable simulation: yellow", style
            )
        )
    }

    private fun drawAABBs() {

        val tmpTrans = Stack.newTrans()
        val minAabb = Stack.newVec()
        val maxAabb = Stack.newVec()

        val collisionObjects = bulletInstance?.collisionObjectArray ?: return

        val bounds = JomlPools.aabbd.create()
        for (i in 0 until collisionObjects.size) {

            val colObj = collisionObjects[i] ?: break
            val color = when (colObj.activationState) {
                CollisionObject.ACTIVE_TAG -> -1
                CollisionObject.ISLAND_SLEEPING -> 0x00ff00
                CollisionObject.WANTS_DEACTIVATION -> 0x00ffff
                CollisionObject.DISABLE_DEACTIVATION -> 0xff0000
                CollisionObject.DISABLE_SIMULATION -> 0xffff00
                else -> 0xff0000
            }.withAlpha(255)

            // todo draw the local coordinate arrows
            // debugDrawObject(colObj.getWorldTransform(tmpTrans), colObj.collisionShape, color)

            try {
                colObj.collisionShape.getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            DrawAABB.drawAABB(
                bounds
                    .setMin(minAabb.x, minAabb.y, minAabb.z)
                    .setMax(maxAabb.x, maxAabb.y, maxAabb.z),
                color
            )
        }

        JomlPools.aabbd.sub(1)
        Stack.subTrans(1)
        Stack.subVec(2)
    }

    private fun drawVehicles() {

        val worldScale = RenderState.worldScale
        val world = bulletInstance ?: return
        val vehicles = world.vehicles ?: return

        val wheelPosWS = Stack.newVec()
        val axle = Stack.newVec()
        val tmp = Stack.newVec()

        fun transform(a: Vector3d, worldScale: Double, dst: Vector3f = Vector3f()): Vector3f {
            val pos = cameraPosition
            return dst.set(
                ((a.x - pos.x) * worldScale).toFloat(),
                ((a.y - pos.y) * worldScale).toFloat(),
                ((a.z - pos.z) * worldScale).toFloat()
            )
        }

        fun drawLine(a: Vector3d, b: Vector3d, worldScale: Double, color: Int) {
            val t0 = JomlPools.vec3f.create()
            val t1 = JomlPools.vec3f.create()
            addLine(
                transform(a, worldScale, t0),
                transform(b, worldScale, t1),
                color
            )
            JomlPools.vec3f.sub(2)
        }

        for (i in 0 until vehicles.size) {
            val vehicle = vehicles[i] ?: break
            for (v in 0 until vehicle.numWheels) {

                val wheelColor = (if (vehicle.getWheelInfo(v).raycastInfo.isInContact) 0x0000ff else 0xff0000) or black

                wheelPosWS.set(vehicle.getWheelInfo(v).worldTransform.origin)
                axle.set(
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(0, vehicle.rightAxis),
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(1, vehicle.rightAxis),
                    vehicle.getWheelInfo(v).worldTransform.basis.getElement(2, vehicle.rightAxis)
                )

                tmp.add(wheelPosWS, axle)
                drawLine(wheelPosWS, tmp, worldScale, wheelColor)
                drawLine(
                    wheelPosWS, vehicle.getWheelInfo(v).raycastInfo.contactPointWS,
                    worldScale, wheelColor
                )
            }
        }

        Stack.subVec(3)

        val actions = world.actions
        for (i in 0 until actions.size) {
            val action = actions[i] ?: break
            action.debugDraw(debugDraw)
        }
    }

    override fun invalidate(entity: Entity) {
        val rb0 = entity.getComponent(Rigidbody::class, false)
        val rb = rb0?.entity ?: return
        if (printValidations) LOGGER.debug("Invalidated {}", hash32(this))
        invalidEntities.add(rb)
    }

    override fun invalidateTransform(entity: Entity) {
        entity.validateTransform() // we need to know the global transform
        val rigidbody = entity.getComponent(Rigidbody::class, false) ?: return
        val globalTransform = entity.transform.globalTransform
        // todo support scale changes, and adjust Entity.scale then, too
        val scale = globalTransform.getScale(org.joml.Vector3d())
        val transform = mat4x3ToTransform(globalTransform, scale)
        rigidbody.bulletInstance?.setWorldTransform(transform)
    }

    private var debugDraw: BulletDebugDraw? = null
    override fun onDrawGUI(all: Boolean) {
        val view = RenderView.currentInstance
        drawDebug(view)
    }

    private fun createBulletWorld(): DiscreteDynamicsWorld {
        val world = Companion.createBulletWorld()
        debugDraw = debugDraw ?: BulletDebugDraw()
        world.debugDrawer = debugDraw
        return world
    }

    private fun createBulletWorldWithGravity(): DiscreteDynamicsWorld {
        val world = createBulletWorld()
        val tmp = Stack.borrowVec()
        tmp.set(gravity.x, gravity.y, gravity.z)
        world.setGravity(tmp)
        return world
    }

    override fun updateGravity() {
        val tmp = Stack.borrowVec()
        tmp.set(gravity.x, gravity.y, gravity.z)
        bulletInstance?.setGravity(tmp)
    }

    override val className: String get() = "BulletPhysics"

    companion object {

        init {
            BulletGlobals.setDeactivationTime(1.0)
        }

        fun createBulletWorld(): DiscreteDynamicsWorld {
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val bp = DbvtBroadphase()
            val solver = SequentialImpulseConstraintSolver()
            return DiscreteDynamicsWorld(dispatcher, bp, solver, collisionConfig)
        }

        private val LOGGER = LogManager.getLogger(BulletPhysics::class)

        fun castB(s: org.joml.Vector3d): Vector3d {
            val v = Stack.borrowVec()
            v.set(s.x, s.y, s.z)
            return v
        }

        fun castB(s: Quaterniond): Quat4d {
            val v = Stack.borrowQuat()
            v.set(s.x, s.y, s.z, s.z)
            return v
        }

        fun mat4x3ToTransform(ourTransform: Matrix4x3d, scale: org.joml.Vector3d): Transform {
            // bullet does not support scale -> we always need to correct it
            val sx = 1.0 / scale.x
            val sy = 1.0 / scale.y
            val sz = 1.0 / scale.z
            val t = Transform()
            val b = t.basis
            // bullet/javax uses normal ij indexing, while joml uses ji indexing
            b.m00 = ourTransform.m00 * sx
            b.m10 = ourTransform.m01 * sx
            b.m20 = ourTransform.m02 * sx
            b.m01 = ourTransform.m10 * sy
            b.m11 = ourTransform.m11 * sy
            b.m21 = ourTransform.m12 * sy
            b.m02 = ourTransform.m20 * sz
            b.m12 = ourTransform.m21 * sz
            b.m22 = ourTransform.m22 * sz
            t.origin.set(ourTransform.m30, ourTransform.m31, ourTransform.m32)
            return t
        }

        fun transformToMat4x3(worldTransform: Transform, scale: org.joml.Vector3d, dstTransform: Matrix4x3d) {

            val basis = worldTransform.basis
            val origin = worldTransform.origin
            val sx = scale.x
            val sy = scale.y
            val sz = scale.z

            // bullet/javax uses normal ij indexing, while joml uses ji indexing
            dstTransform.set(
                basis.m00 * sx, basis.m10 * sy, basis.m20 * sz,
                basis.m01 * sx, basis.m11 * sy, basis.m21 * sz,
                basis.m02 * sx, basis.m12 * sy, basis.m22 * sz,
                origin.x, origin.y, origin.z
            )
        }
    }
}