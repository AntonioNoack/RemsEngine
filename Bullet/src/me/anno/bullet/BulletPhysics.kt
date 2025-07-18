package me.anno.bullet

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.ActivationState
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.dispatch.GhostPairCallback
import com.bulletphysics.collision.dispatch.PairCachingGhostObject
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.VehicleTuning
import com.bulletphysics.dynamics.vehicle.WheelInfo
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.bullet.BulletRendering.renderGUI
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.GhostBody
import me.anno.bullet.bodies.KinematicBody
import me.anno.bullet.bodies.PhysicalBody
import me.anno.bullet.bodies.PhysicsBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.bodies.Vehicle
import me.anno.bullet.bodies.VehicleWheel
import me.anno.bullet.constraints.Constraint
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollisionFilters.createFilter
import me.anno.ecs.components.physics.Physics
import me.anno.ecs.components.physics.PhysicsBodyBase
import me.anno.ecs.components.physics.ScaledBody
import me.anno.ecs.components.physics.events.FallenOutOfWorld
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.pipeline.Pipeline
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toLongOr
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3
import org.joml.Vector3d
import speiger.primitivecollections.IntHashSet
import kotlin.math.abs
import kotlin.math.max

open class BulletPhysics : Physics<PhysicsBody<*>, CollisionObject>(PhysicsBody::class), OnDrawGUI {

    @NotSerializedProperty
    var bulletInstance: DiscreteDynamicsWorld = createBulletWorldWithGravity()

    @NotSerializedProperty
    private val raycastVehicles = HashMap<Entity, RaycastVehicle>()

    private fun createCollider(
        entity: Entity, colliders: List<Collider>,
        scale: Vector3d, centerOfMass: Vector3d
    ): CollisionShape {
        val firstCollider = colliders.first()
        return if (colliders.size == 1 &&
            firstCollider.entity === entity &&
            centerOfMass.lengthSquared() == 0.0
        ) {
            // there is only one, and no transform needs to be applied -> use it directly
            createBulletShape(firstCollider, scale)
        } else {
            val jointCollider = CompoundShape()
            for (collider in colliders) {
                val (transform, subCollider) = createBulletCollider(
                    collider, entity,
                    scale, centerOfMass
                )
                jointCollider.addChildShape(transform, subCollider)
            }
            jointCollider
        }
    }

    override fun createRigidbody(
        entity: Entity,
        rigidBody: PhysicsBody<*>
    ): ScaledBody<PhysicsBody<*>, CollisionObject>? {

        val colliders = rigidBody.activeColliders
        getValidComponents(entity, rigidComponentClass, Collider::class, colliders)
        if (colliders.isEmpty()) return null

        // bullet does not work correctly with scale changes: create larger shapes directly
        val globalTransform = entity.transform.globalTransform
        val scale = globalTransform.getScale(Vector3d())
        val centerOfMass =
            if (rigidBody is DynamicBody) rigidBody.centerOfMass // create a copy of this vector?
            else Vector3d()

        // copy all knowledge from ecs to bullet
        val collider = createCollider(entity, colliders, scale, centerOfMass)

        val inertia = Stack.newVec()
        val mass = if (rigidBody is DynamicBody) max(0.0, rigidBody.mass) else 0.0
        if (mass > 0.0) {
            collider.calculateLocalInertia(mass, inertia)
        }

        val body =
            if (rigidBody is GhostBody) PairCachingGhostObject()
            else RigidBody(mass, collider, inertia)
        body.collisionShape = collider
        body.userData = rigidBody

        setMatrix(body, globalTransform, scale, centerOfMass)

        if (body is RigidBody && rigidBody is PhysicalBody) {
            body.friction = rigidBody.friction
            body.restitution = rigidBody.restitution
            if (rigidBody is DynamicBody) {
                body.linearDamping = rigidBody.linearDamping
                body.angularDamping = rigidBody.angularDamping
                body.linearSleepingThreshold = rigidBody.linearSleepingThreshold
                body.angularSleepingThreshold = rigidBody.angularSleepingThreshold
            }
        }
        body.ccdMotionThreshold = 1e-7

        val center = Stack.newVec()
        body.ccdSweptSphereRadius = collider.getBoundingSphere(center)
        if (mass > 0.0 && rigidBody is DynamicBody && body is RigidBody) { // else not supported
            body.setLinearVelocity(rigidBody.globalLinearVelocity)
            body.setAngularVelocity(rigidBody.globalAngularVelocity)
        }
        Stack.subVec(2) // inertia, center

        return ScaledBody(rigidBody, body, scale, centerOfMass)
    }

    private fun defineVehicle(entity: Entity, vehicleComp: Vehicle, body: RigidBody) {
        // todo correctly create vehicle, if the body is scaled
        val tuning = VehicleTuning()
        tuning.frictionSlip = vehicleComp.frictionSlip
        tuning.suspensionDamping = vehicleComp.suspensionDamping
        tuning.suspensionStiffness = vehicleComp.suspensionStiffness
        tuning.suspensionCompression = vehicleComp.suspensionCompression
        tuning.maxSuspensionTravel = vehicleComp.maxSuspensionTravelCm
        val world = bulletInstance
        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(tuning, body, raycaster)
        vehicle.setCoordinateSystem(0, 1, 2)
        val wheels = vehicleComp.wheels
        for (i in wheels.indices) {
            val wheel = wheels[i]
            val info = createWheelInfo(wheel, entity, vehicle)
            info.clientInfo = wheel
            wheel.bulletInstance = info
        }
        // vehicle.currentSpeedKmHour
        // vehicle.applyEngineForce()
        world.addVehicle(vehicle)
        body.activationState = ActivationState.ALWAYS_ACTIVE
        raycastVehicles[entity] = vehicle
    }

    private fun createWheelInfo(wheel: VehicleWheel, vehicleEntity: Entity, bulletVehicle: RaycastVehicle): WheelInfo {
        val transform = wheel.entity!!.fromLocalToOtherLocal(vehicleEntity, wheel.lockedTransform)
        // +w
        val position = transform.getTranslation(Vector3d())
        // raycast direction, e.g. down, so -y
        val wheelDirection = Vector3d(-transform.m10.toDouble(), -transform.m11.toDouble(), -transform.m12.toDouble())
        val scale = abs(transform.getScaleLength() / Maths.SQRT3)
        val actualWheelRadius = wheel.radius * scale
        // wheel axis, e.g. x axis, so +x
        val wheelAxle = Vector3d(-transform.m00.toDouble(), -transform.m01.toDouble(), -transform.m02.toDouble())
        val tuning = VehicleTuning()
        tuning.frictionSlip = wheel.frictionSlip
        tuning.suspensionDamping = wheel.suspensionDampingRelaxation
        tuning.suspensionStiffness = wheel.suspensionStiffness
        tuning.suspensionCompression = wheel.suspensionDampingCompression
        tuning.maxSuspensionTravel = wheel.maxSuspensionTravel
        val wheelInfo = bulletVehicle.addWheel(
            position, wheelDirection, wheelAxle,
            wheel.suspensionRestLength, actualWheelRadius, tuning
        )
        wheelInfo.brake = wheel.brakeForce
        wheelInfo.engineForce = wheel.engineForce
        wheelInfo.steering = wheel.steering
        wheelInfo.rollInfluence = wheel.rollInfluence
        return wheelInfo
    }

    override fun onCreateRigidbody(
        entity: Entity, rigidbody: PhysicsBody<*>,
        scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>
    ) {

        val bulletBody = scaledBody.external

        // vehicle stuff
        if (rigidbody is Vehicle && bulletBody is RigidBody) {
            defineVehicle(entity, rigidbody, bulletBody)
        }

        // activate
        bulletBody.activationState =
            if (rigidbody is DynamicBody) { // mmhh..
                if (rigidbody.activeByDefault) ActivationState.ACTIVE
                else ActivationState.WANTS_DEACTIVATION
            } else ActivationState.ALWAYS_ACTIVE

        bulletBody.collisionFlags = when (rigidbody) {
            // todo allow custom material response for per-triangle friction/restitution (terrain)
            is StaticBody -> CollisionFlags.STATIC_OBJECT
            is KinematicBody -> CollisionFlags.KINEMATIC_OBJECT
            is GhostBody -> CollisionFlags.NO_CONTACT_RESPONSE
            else -> 0
        }

        if (bulletBody is RigidBody && rigidbody is DynamicBody) {
            bulletBody.setGravity(if (rigidbody.overrideGravity) rigidbody.gravity else gravity)
        }

        val filter = createFilter(rigidbody.groupId, rigidbody.collisionMask)
        bulletInstance.addCollisionObject(bulletBody, filter)

        // must be done after adding the body to the world,
        // because it is overridden by World.addRigidbody()
        if (rigidbody is DynamicBody && bulletBody is RigidBody && rigidbody.overrideGravity) {
            bulletBody.setGravity(rigidbody.gravity)
        }

        when (rigidbody) {
            is GhostBody -> rigidbody.nativeInstance = bulletBody as PairCachingGhostObject
            is PhysicalBody -> rigidbody.nativeInstance = bulletBody as RigidBody
        }

        // create all constraints
        if (rigidbody is PhysicalBody && bulletBody is RigidBody) {
            entity.forAllComponents(Constraint::class, false) { constraint ->
                val otherEngineBody = constraint.other
                if (otherEngineBody != null && otherEngineBody != rigidbody && otherEngineBody.isEnabled) {
                    addConstraint(constraint, bulletBody, rigidbody, otherEngineBody)
                }
            }
        }
    }

    private fun addConstraint(
        constraint: Constraint<*>,
        bulletBody: RigidBody, engineBody: PhysicalBody,
        otherEngineBody: PhysicalBody
    ) {
        val oldConstraint = constraint.bulletInstance
        val world = bulletInstance
        if (oldConstraint != null) {
            world.removeConstraint(oldConstraint)
            constraint.bulletInstance = null
        }

        if (engineBody is DynamicBody || otherEngineBody is DynamicBody) {
            val otherBody = getRigidbody(otherEngineBody)
            if (otherBody is RigidBody) {
                // create constraint
                val bulletConstraint = constraint.createConstraint(
                    bulletBody, otherBody,
                    constraint.getTA(), constraint.getTB()
                )
                @Suppress("UNCHECKED_CAST")
                (constraint as Constraint<TypedConstraint>).bulletInstance = bulletConstraint
                world.addConstraint(bulletConstraint, constraint.disableCollisionsBetweenLinked)
            } else LOGGER.warn("Expected RigidBody")
        } else {
            LOGGER.warn("Cannot constrain two static/kinematic bodies!, ${engineBody.prefabPath} to ${otherEngineBody.prefabPath}")
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
        entity.forAllComponents(Constraint::class) { constraint ->
            val bulletConstraint = constraint.bulletInstance
            if (bulletConstraint != null) {
                constraint.bulletInstance = null
                world.removeConstraint(bulletConstraint)
            }
        }

        val physicalBody = entity.getComponent(PhysicsBodyBase::class, true)
        physicalBody?.activeColliders?.clear()

        val rigidBody = entity.getComponent(PhysicalBody::class, true)
        if (rigidBody != null) {
            val activeConstraints = rigidBody.activeConstraints
            for (i in activeConstraints.indices) {
                val constraint = activeConstraints[i]
                val bulletConstraint = constraint.bulletInstance
                if (bulletConstraint != null) {
                    world.removeConstraint(bulletConstraint)
                    constraint.bulletInstance = null
                }
            }
        }

        val vehicle = raycastVehicles.remove(entity)
        if (vehicle != null) {
            // restore wheel transforms
            val wheels = vehicle.wheels
            for (i in wheels.indices) {
                val wheel = wheels[i]
                val wheelI = wheel.clientInfo as VehicleWheel
                val wheelE = wheelI.entity ?: continue
                wheelE.transform.setGlobal(
                    wheelI.lockedTransform.mul(
                        entity.transform.globalTransform,
                        wheelE.transform.globalTransform
                    )
                )
                // transform * entity
                wheelI
            }
            world.removeVehicle(vehicle)
        }

        entity.isPhysicsControlled = false
        if (fallenOutOfWorld) {
            if (rigidBody != null) {
                // when something falls of the world, often it's nice to directly destroy the object,
                // because it will no longer be needed
                // call event, so e.g., we could add it back to a pool of entities, or respawn it
                entity.forAllComponents(FallenOutOfWorld::class) { component ->
                    component.onFallOutOfWorld()
                }
                if (rigidBody is DynamicBody && rigidBody.deleteWhenKilledByDepth) {
                    entity.parentEntity?.deleteChild(entity)
                }
            }
        }
    }

    override fun step(dtNanos: Long, printSlack: Boolean) {
        // just in case
        if (printSlack) {
            Stack.printSizes()
        }
        // Stack.limit = 1024
        Stack.reset(printSlack)
        super.step(dtNanos, printSlack)
    }

    @DebugAction
    fun manualStep2() {
        // dt = 1e9 / 60
        val step = if (fixedStep == 0.0) 1.0 / 60.0 else fixedStep
        step((step * 1e9).toLong(), true)
    }

    var maxSubSteps = 16
    var fixedStep = 1.0 / 120.0 // 0.0 for flexible steps
    var deactivationTime = 0.1

    override fun worldStepSimulation(step: Double) {
        try {
            Stack.reset(false)

            // define all thread-local constants
            BulletGlobals.deactivationTime = deactivationTime

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

    override fun isDynamic(rigidbody: CollisionObject): Boolean {
        return rigidbody is RigidBody && !rigidbody.isStaticOrKinematicObject
    }

    override fun isActive(scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>): Boolean {
        val rigidbody = scaledBody.external
        return rigidbody is RigidBody && rigidbody.isActive
    }

    override fun worldRemoveRigidbody(scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>) {
        bulletInstance.removeCollisionObject(scaledBody.external)
    }

    override fun getMatrix(
        rigidbody: CollisionObject, dstTransform: Matrix4x3,
        scale: Vector3d, centerOfMass: Vector3d
    ) {
        val worldTransform = rigidbody.worldTransform
        transformToMat4x3(worldTransform, scale, centerOfMass, dstTransform)
    }

    override fun setMatrix(
        rigidbody: CollisionObject, srcTransform: Matrix4x3,
        scale: Vector3d, centerOfMass: Vector3d
    ) {
        val worldTransform = rigidbody.worldTransform
        mat4x3ToTransform(srcTransform, scale, centerOfMass, worldTransform)
        if (rigidbody is RigidBody) rigidbody.interpolationWorldTransform.set(worldTransform)
    }

    override fun updateDynamicRigidBody(
        entity: Entity, rigidbodyScaled: ScaledBody<PhysicsBody<*>, CollisionObject>
    ) {
        super.updateDynamicRigidBody(entity, rigidbodyScaled)
        val (dst, src) = rigidbodyScaled
        if (dst !is DynamicBody || src !is RigidBody) return
        dst.globalLinearVelocity.set(src.linearVelocity)
        dst.globalAngularVelocity.set(src.angularVelocity)
    }

    override fun updateWheels() {
        super.updateWheels()
        val scale = JomlPools.vec3d.create().set(1.0)
        val centerOfMass = JomlPools.vec3d.create().set(0.0)
        for ((_, v) in raycastVehicles) {
            val wheelInfos = v.wheels
            for (i in wheelInfos.indices) {
                val wheelInfo = wheelInfos[i]
                val wheel = wheelInfo.clientInfo as? VehicleWheel ?: continue
                val entity = wheel.entity ?: continue
                val dst = entity.transform
                val dstTransform = dst.globalTransform
                // todo use correct scale
                val tr = wheelInfo.worldTransform
                transformToMat4x3(tr, scale, centerOfMass, dstTransform)
                dst.setStateAndUpdate(me.anno.ecs.Transform.State.VALID_GLOBAL)
            }
        }
        JomlPools.vec3d.sub(2) // scale, centerOfMass
    }

    var enableDebugRendering = false

    @DebugProperty
    val numManifolds: Int
        get() = bulletInstance.dispatcher.numManifolds

    @DebugProperty
    val numContactPoints: Int
        get() {
            var sum = 0
            val dispatcher = bulletInstance.dispatcher
            try {
                for (i in 0 until dispatcher.numManifolds) {
                    sum += dispatcher.getManifold(i).numContacts
                }
            } catch (_: Exception) {
            }
            return sum
        }

    @DebugProperty
    val numIslands: Int
        get() {
            val objects = bulletInstance.collisionObjects
            val islandIds = IntHashSet()
            for (i in objects.indices) {
                val co = objects[i]
                val islandId = co.islandTag
                if (islandId != -1) {
                    islandIds.add(islandId)
                }
            }
            return islandIds.size
        }

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (enableDebugRendering) {
            renderGUI(pipeline)
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

    override fun invalidateTransform(entity: Entity) {
        entity.validateTransform() // we need to know the global transform
        val dynamicBody = entity.getComponent(DynamicBody::class, false) ?: return
        val globalTransform = entity.transform.globalTransform
        // todo support scale changes, and adjust Entity.scale then, too
        val scale = JomlPools.vec3d.create()
        globalTransform.getScale(scale)
        val transform = mat4x3ToTransform(globalTransform, scale, dynamicBody.centerOfMass, Transform())
        dynamicBody.nativeInstance?.setWorldTransform(transform)
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

        fun createBulletWorld(): DiscreteDynamicsWorld {
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val bp = DbvtBroadphase()
            val solver = SequentialImpulseConstraintSolver()
            val world = DiscreteDynamicsWorld(dispatcher, bp, solver)
            world.broadphase.overlappingPairCache.setInternalGhostPairCallback(GhostPairCallback())
            return world
        }

        private val LOGGER = LogManager.getLogger(BulletPhysics::class)

        fun mat4x3ToTransform(
            ourTransform: Matrix4x3, scale: Vector3d,
            centerOfMass: Vector3d, dst: Transform
        ): Transform {
            // bullet does not support scale -> we always must correct it
            dst.basis.set(ourTransform).scale(1.0 / scale.x, 1.0 / scale.y, 1.0 / scale.z)
            dst.basis.transform(centerOfMass, dst.origin) // local -> world
            dst.origin.add(ourTransform.m30, ourTransform.m31, ourTransform.m32)
            return dst
        }

        fun transformToMat4x3(
            worldTransform: Transform, scale: Vector3d,
            centerOfMass: Vector3d, dstTransform: Matrix4x3
        ): Matrix4x3 {
            // bullet does not support scale -> we always need to correct it
            val origin = worldTransform.origin
            val epsilon = 1e-3
            if (abs(scale.x) > epsilon && abs(scale.y) > epsilon && abs(scale.z) > epsilon) {
                // some little extra calculations, but also more accurate, because of float-double conversions
                return dstTransform.set(worldTransform.basis)
                    .scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                    .translate(-centerOfMass.x / scale.x, -centerOfMass.y / scale.y, -centerOfMass.z / scale.z)
                    .translateLocal(origin.x, origin.y, origin.z)
            }

            return dstTransform.set(worldTransform.basis)
                .translate(-centerOfMass.x, -centerOfMass.y, -centerOfMass.z)
                .scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                .translateLocal(origin.x, origin.y, origin.z)
        }
    }
}