package me.anno.bullet

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.getElement
import cz.advel.stack.Stack
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.physics.BodyWithScale
import me.anno.ecs.components.physics.Physics
import me.anno.ecs.components.physics.events.FallenOutOfWorld
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.DrawAABB.drawAABB
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState.cameraMatrix
import me.anno.engine.ui.render.RenderState.cameraPosition
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.gpu.pipeline.Pipeline
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.CountMap
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

open class BulletPhysics : Physics<Rigidbody, RigidBody>(Rigidbody::class), OnDrawGUI {

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
    var bulletInstance: DiscreteDynamicsWorld = createBulletWorldWithGravity()

    @NotSerializedProperty
    private val raycastVehicles = HashMap<Entity, RaycastVehicle>()

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

    override fun createRigidbody(entity: Entity, rigidBody: Rigidbody): BodyWithScale<Rigidbody, RigidBody>? {
        val colliders = rigidBody.activeColliders
        getValidComponents(entity, Collider::class, colliders)
        colliders.removeIf { !it.hasPhysics }
        if (colliders.isEmpty()) return null

        // bullet does not work correctly with scale changes: create larger shapes directly
        val globalTransform = entity.transform.globalTransform
        val scale = globalTransform.getScale(org.joml.Vector3d())

        // copy all knowledge from ecs to bullet
        val collider = createCollider(entity, colliders, scale)

        val inertia = Stack.newVec()
        val mass = max(0.0, rigidBody.mass)
        if (mass > 0.0) {
            collider.calculateLocalInertia(mass, inertia)
        }
        // LOGGER.info("Inertia for collider: $mass -> $inertia")

        val transform1 = mat4x3ToTransform(globalTransform, scale)

        // convert the center of mass to a usable transform
        val com0 = rigidBody.centerOfMass
        val com1 = Transform()
        com1.basis.identity()
        com1.origin.set(com0.x, com0.y, com0.z)

        val motionState = DefaultMotionState(transform1, com1)
        val rbInfo = RigidBodyConstructionInfo(mass, motionState, collider, inertia)
        rbInfo.friction = rigidBody.friction
        rbInfo.restitution = rigidBody.restitution
        rbInfo.linearDamping = rigidBody.linearDamping
        rbInfo.angularDamping = rigidBody.angularDamping
        rbInfo.linearSleepingThreshold = rigidBody.linearSleepingThreshold
        rbInfo.angularSleepingThreshold = rigidBody.angularSleepingThreshold

        val rb = RigidBody(rbInfo)
        rb.ccdMotionThreshold = 1e-7
        val tmp = Stack.borrowVec()
        rb.ccdSweptSphereRadius = collider.getBoundingSphere(tmp)
        if (mass > 0.0) { // else not supported
            rb.setLinearVelocity(rigidBody.linearVelocity)
            rb.setAngularVelocity(rigidBody.angularVelocity)
        }
        Stack.subVec(1)

        return BodyWithScale(rigidBody, rb, scale)
    }

    private fun defineVehicle(entity: Entity, vehicleComp: Vehicle, body: RigidBody) {
        // todo correctly create vehicle, if the body is scaled
        val tuning = VehicleTuning()
        tuning.frictionSlip = vehicleComp.frictionSlip
        tuning.suspensionDamping = vehicleComp.suspensionDamping
        tuning.suspensionStiffness = vehicleComp.suspensionStiffness
        tuning.suspensionCompression = vehicleComp.suspensionCompression
        tuning.maxSuspensionTravelCm = vehicleComp.maxSuspensionTravelCm
        val world = bulletInstance
        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(tuning, body, raycaster)
        vehicle.setCoordinateSystem(0, 1, 2)
        val wheels = vehicleComp.wheels
        for (wheel in wheels) {
            val info = wheel.createBulletInstance(entity, vehicle)
            info.clientInfo = wheel
            wheel.bulletInstance = info
        }
        // vehicle.currentSpeedKmHour
        // vehicle.applyEngineForce()
        world.addVehicle(vehicle)
        body.activationState = ActivationState.DISABLE_DEACTIVATION
        raycastVehicles[entity] = vehicle
    }

    override fun onCreateRigidbody(
        entity: Entity,
        rigidbody: Rigidbody,
        bodyWithScale: BodyWithScale<Rigidbody, RigidBody>
    ) {

        val body = bodyWithScale.external

        // vehicle stuff
        if (rigidbody is Vehicle) {
            defineVehicle(entity, rigidbody, body)
        }

        // activate
        if (rigidbody.activeByDefault) body.activationState = ActivationState.ACTIVE

        bulletInstance.addRigidBody(
            body, // todo re-activate / correct groups and masks
            // clamp(rigidbody.group, 0, 15).toShort(),
            // rigidbody.collisionMask
        )

        // must be done after adding the body to the world,
        // because it is overridden by World.addRigidbody()
        if (rigidbody.overrideGravity) {
            body.setGravity(rigidbody.gravity)
        }

        rigidBodies[entity] = bodyWithScale
        rigidbody.bulletInstance = body

        registerNonStatic(entity, rigidbody.isStatic, bodyWithScale)

        val constraints = rigidbody.linkedConstraints
        for (i in constraints.indices) {
            val constraint = constraints[i]
            if (constraint.isEnabled) {
                // ensure the constraint exists
                val rigidbody2 = constraint.entity
                    ?.getComponent(Rigidbody::class, false)
                    ?: continue
                val rigidbody3 = getRigidbody(rigidbody2) ?: continue
                addConstraint(constraint, rigidbody3, rigidbody2, rigidbody)
            }
        }

        // create all constraints
        entity.forAllComponents(Constraint::class, false) { c ->
            val other = c.other
            if (other != null && other != rigidbody && other.isEnabled) {
                addConstraint(c, body, rigidbody, other)
            }
        }
    }

    private fun addConstraint(c: Constraint<*>, body: RigidBody, rigidbody: Rigidbody, other: Rigidbody) {
        val oldInstance = c.bulletInstance
        val world = bulletInstance
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
        val world = bulletInstance
        entity.forAllComponents(Constraint::class) {
            val bi = it.bulletInstance
            if (bi != null) {
                it.bulletInstance = null
                world.removeConstraint(bi)
                // LOGGER.debug("- ${it.prefabPath}")
            }
        }
        val rigidbody = entity.getComponent(Rigidbody::class, false)
        if (rigidbody != null) {
            rigidbody.activeColliders.clear()
            for (c in rigidbody.linkedConstraints) {
                val bi = c.bulletInstance
                if (bi != null) {
                    world.removeConstraint(bi)
                    c.bulletInstance = null
                    // LOGGER.debug("- ${c.prefabPath}")
                }
            }
        }
        val vehicle = raycastVehicles.remove(entity) ?: return
        world.removeVehicle(vehicle)
        entity.isPhysicsControlled = false
        if (fallenOutOfWorld) {
            if (rigidbody != null) {
                // when something falls of the world, often it's nice to directly destroy the object,
                // because it will no longer be needed
                // call event, so e.g., we could add it back to a pool of entities, or respawn it
                entity.forAllComponents(FallenOutOfWorld::class) {
                    it.onFallOutOfWorld()
                }
                if (rigidbody.deleteWhenKilledByDepth) {
                    entity.parentEntity?.deleteChild(entity)
                }
            }
        }
    }

    override fun step(dt: Long, printSlack: Boolean) {
        // just in case
        if (printSlack) {
            Stack.printSizes()
        }
        // Stack.limit = 1024
        Stack.reset(printSlack)
        super.step(dt, printSlack)
    }

    var maxSubSteps = 16
    var fixedStep = 1.0 / 120.0 // 0.0 for flexible steps

    override fun worldStepSimulation(step: Double) {
        try {
            Stack.reset(false)
            bulletInstance.stepSimulation(step, maxSubSteps, if (fixedStep <= 0.0) step else fixedStep)
        } catch (e: Exception) {
            warnCrash(e)
        } catch (e: OutOfMemoryError) {
            warnCrash(e)
        }
    }

    private fun warnCrash(e: Throwable) {
        e.printStackTrace()
        LOGGER.warn("Crashed thread: ${Thread.currentThread().name}")
    }

    override fun isActive(rigidbody: RigidBody): Boolean {
        return rigidbody.isActive
    }

    override fun worldRemoveRigidbody(rigidbody: RigidBody) {
        bulletInstance.removeRigidBody(rigidbody)
    }

    override fun convertTransformMatrix(rigidbody: RigidBody, scale: Vector3d, dstTransform: Matrix4x3) {
        val tmpTransform = Stack.borrowTrans()
        rigidbody.getWorldTransform(tmpTransform)
        transformToMat4x3(tmpTransform, scale, dstTransform)
    }

    override fun updateNonStaticRigidBody(entity: Entity, rigidbodyWithScale: BodyWithScale<Rigidbody, RigidBody>) {
        super.updateNonStaticRigidBody(entity, rigidbodyWithScale)
        val (dst, src) = rigidbodyWithScale
        val tmp = Stack.newVec()
        src.getLinearVelocity(tmp)
        dst.linearVelocity.set(tmp.x, tmp.y, tmp.z)
        src.getAngularVelocity(tmp)
        dst.angularVelocity.set(tmp.x, tmp.y, tmp.z)
        Stack.subVec(1)
    }

    override fun updateWheels() {
        super.updateWheels()
        val scale = JomlPools.vec3d.create()
        scale.set(1.0)
        for ((_, v) in raycastVehicles) {
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

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        val view = RenderView.currentInstance
        if (view?.renderMode != RenderMode.PHYSICS) return
        // define camera transform
        BulletDebugDraw.stack.set(cameraMatrix)
        BulletDebugDraw.cam.set(cameraPosition)
        // draw stuff
        drawConstraints(pipeline)
        drawColliders(pipeline)
        drawContactPoints()
        drawAABBs()
        drawVehicles()
        drawIslands()
    }

    private fun drawIslands() {
        val transform = Transform()
        val min = Vector3d()
        val max = Vector3d()
        val boundsById = HashMap<Int, AABBd>()
        val countMap = CountMap<Int>()
        for (instance in bulletInstance.collisionObjects) {
            val tag = instance.islandTag
            if (tag < 0) continue // no island available
            // get transformed bounds
            instance.getWorldTransform(transform)
            instance.collisionShape!!.getAabb(transform, min, max)
            // add bounds to island
            boundsById.getOrPut(tag) { AABBd() }
                .union(min.x, min.y, min.z, max.x, max.y, max.z)
            countMap.incAndGet(tag)
        }
        // render all islands as AABBs
        val color = UIColors.dodgerBlue
        for ((tag, bounds) in boundsById) {
            if (countMap[tag] == 1) continue // a not a true island
            drawAABB(bounds, color)
        }
    }

    private fun drawColliders(pipeline: Pipeline) {
        for ((_, bodyWithScale) in rigidBodies) {
            drawColliders(pipeline, bodyWithScale?.internal ?: continue)
        }
    }

    private fun drawColliders(pipeline: Pipeline, rigidbody: Rigidbody) {
        val colliders = rigidbody.activeColliders
        for (i in colliders.indices) {
            colliders.getOrNull(i)?.drawShape(pipeline)
        }
    }

    private fun drawConstraints(pipeline: Pipeline) {
        for ((_, bodyWithScale) in nonStaticRigidBodies) {
            drawConstraints(pipeline, bodyWithScale.internal)
        }
    }

    private fun drawConstraints(pipeline: Pipeline, rigidbody: Rigidbody) {
        val constraints = rigidbody.linkedConstraints
        for (i in constraints.indices) {
            constraints.getOrNull(i)?.onDrawGUI(pipeline, true)
        }
    }

    private fun drawContactPoints() {
        val dispatcher = bulletInstance.dispatcher
        var i = 0
        while (i < dispatcher.numManifolds) {
            val contact = dispatcher.getManifoldByIndexInternal(i)
            drawContactManifold(contact)
            i++
        }
    }

    private fun drawContactManifold(contactManifold: PersistentManifold) {
        for (j in 0 until contactManifold.numContacts) {
            drawContactPoint(contactManifold.getContactPoint(j))
        }
    }

    private fun drawContactPoint(point: ManifoldPoint) {
        val color = UIColors.magenta
        val cam = cameraPosition
        val a = point.positionWorldOnB
        val n = point.normalWorldOnB
        val d = 0.05 * cam.distance(a.x, a.y, a.z)
        val a2 = org.joml.Vector3d(a.x, a.y, a.z)
        val b2 = org.joml.Vector3d(a.x + n.x * d, a.y + n.y * d, a.z + n.z * d)
        DebugShapes.debugArrows.add(DebugLine(a2, b2, color, 0f))
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

        val collisionObjects = bulletInstance.collisionObjects

        val bounds = JomlPools.aabbd.create()
        for (i in 0 until collisionObjects.size) {

            val colObj = collisionObjects[i]
            val color = when (colObj.activationState) {
                ActivationState.ACTIVE -> 0xffffff
                ActivationState.SLEEPING -> 0x333333
                ActivationState.WANTS_DEACTIVATION -> 0x00ffff
                ActivationState.DISABLE_DEACTIVATION -> 0xff0000
                ActivationState.DISABLE_SIMULATION -> 0xffff00
            }.withAlpha(255)

            // todo draw the local coordinate arrows
            // debugDrawObject(colObj.getWorldTransform(tmpTrans), colObj.collisionShape, color)

            try {
                val shape = colObj.collisionShape!!
                shape.getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb)
                if (shape is ConvexHullShape) {
                    val p1 = Vector3d()
                    for (p in shape.points) {
                        p1.set(p)
                        tmpTrans.transform(p1)
                        DebugShapes.debugPoints.add(DebugPoint(org.joml.Vector3d(p1.x, p1.y, p1.z), -1, 0f))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            drawAABB(
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

    private fun transform(a: Vector3d, dst: Vector3f): Vector3f {
        val pos = cameraPosition
        return dst.set(
            (a.x - pos.x).toFloat(),
            (a.y - pos.y).toFloat(),
            (a.z - pos.z).toFloat()
        )
    }

    private fun drawLine(a: Vector3d, b: Vector3d, color: Int) {
        val t0 = JomlPools.vec3f.create()
        val t1 = JomlPools.vec3f.create()
        addLine(transform(a, t0), transform(b, t1), color)
        JomlPools.vec3f.sub(2)
    }

    private fun drawVehicles() {

        val world = bulletInstance
        val vehicles = world.vehicles

        val wheelPosWS = Stack.newVec()
        val axle = Stack.newVec()
        val tmp = Stack.newVec()

        for (i in 0 until vehicles.size) {
            val vehicle = vehicles[i]
            for (v in 0 until vehicle.numWheels) {
                val wheelInfo = vehicle.getWheelInfo(v)
                val wheelColor = (if (wheelInfo.raycastInfo.isInContact) 0x0000ff else 0xff0000) or black

                wheelPosWS.set(wheelInfo.worldTransform.origin)
                val basis = wheelInfo.worldTransform.basis
                val rightAxis = vehicle.rightAxis
                axle.set(basis.getElement(0, rightAxis), basis.getElement(1, rightAxis), basis.getElement(2, rightAxis))

                tmp.add(wheelPosWS, axle)
                drawLine(wheelPosWS, tmp, wheelColor)
                val contact = wheelInfo.raycastInfo.contactPointWS
                drawLine(wheelPosWS, contact, wheelColor)
            }
        }

        Stack.subVec(3)

        val actions = world.actions
        for (i in 0 until actions.size) {
            val action = actions[i] ?: break
            action.debugDraw(BulletDebugDraw)
        }
    }

    override fun invalidateTransform(entity: Entity) {
        entity.validateTransform() // we need to know the global transform
        val rigidbody = entity.getComponent(Rigidbody::class, false) ?: return
        val globalTransform = entity.transform.globalTransform
        // todo support scale changes, and adjust Entity.scale then, too
        val scale = JomlPools.vec3d.create()
        globalTransform.getScale(scale)
        val transform = mat4x3ToTransform(globalTransform, scale)
        rigidbody.bulletInstance?.setWorldTransform(transform)
        JomlPools.vec3d.sub(1)
    }

    private fun createBulletWorld(): DiscreteDynamicsWorld {
        val world = Companion.createBulletWorld()
        world.debugDrawer = BulletDebugDraw
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
        bulletInstance.setGravity(tmp)
    }

    override fun clear() {
        super.clear()
        bulletInstance = createBulletWorldWithGravity()
        raycastVehicles.clear()
        invalidEntities.clear()
    }

    companion object {

        init {
            // todo this is a thread-local value, so we need to configure it for every thread!!!
            BulletGlobals.deactivationTime = 1.0
        }

        fun createBulletWorld(): DiscreteDynamicsWorld {
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val bp = DbvtBroadphase(null)
            val solver = SequentialImpulseConstraintSolver()
            return DiscreteDynamicsWorld(dispatcher, bp, solver)
        }

        private val LOGGER = LogManager.getLogger(BulletPhysics::class)

        fun mat4x3ToTransform(ourTransform: Matrix4x3, scale: Vector3d): Transform {
            val transform = Transform()
            // bullet does not support scale -> we always need to correct it
            transform.basis.set(ourTransform).scale(1.0 / scale.x, 1.0 / scale.y, 1.0 / scale.z)
            transform.origin.set(ourTransform.m30, ourTransform.m31, ourTransform.m32)
            return transform
        }

        fun transformToMat4x3(worldTransform: Transform, scale: Vector3d, dstTransform: Matrix4x3) {
            // bullet does not support scale -> we always need to correct it
            dstTransform.set(worldTransform.basis)
                .scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                .setTranslation(worldTransform.origin)
        }
    }
}