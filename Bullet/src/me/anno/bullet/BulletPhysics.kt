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
import com.bulletphysics.dynamics.character.KinematicCharacterController
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster
import com.bulletphysics.dynamics.vehicle.RaycastVehicle
import com.bulletphysics.dynamics.vehicle.WheelInstance
import cz.advel.stack.Stack
import me.anno.bullet.BulletRendering.renderGUI
import me.anno.bullet.ToConvexShape.convertToConvexShape
import me.anno.bullet.bodies.CharacterBody
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
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import speiger.primitivecollections.IntHashSet
import kotlin.math.max

open class BulletPhysics : Physics<PhysicsBody<*>, CollisionObject>(PhysicsBody::class), OnDrawGUI {

    @NotSerializedProperty
    var bulletInstance: DiscreteDynamicsWorld = createBulletWorldWithGravity()

    @NotSerializedProperty
    private val raycastVehicles = HashMap<Entity, RaycastVehicle>()

    private fun createCollider(
        entity: Entity, colliders: List<Collider>,
        scale: Vector3f, centerOfMass: Vector3d
    ): CollisionShape {
        val firstCollider = colliders.first()
        return if (colliders.size == 1 &&
            firstCollider.entity === entity && // check transform
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
        val srcTransform = entity.transform.globalTransform
        val scale = srcTransform.getScale(Vector3f())
        val centerOfMass =
            if (rigidBody is DynamicBody) rigidBody.centerOfMass // create a copy of this vector?
            else Vector3d()

        // copy all knowledge from ecs to bullet
        val collider = createCollider(entity, colliders, scale, centerOfMass)

        val inertia = Stack.newVec3f()
        val mass = if (rigidBody is DynamicBody) max(0f, rigidBody.mass) else 0f
        if (mass > 0f) {
            collider.calculateLocalInertia(mass, inertia)
        }

        val dstBody =
            if (rigidBody is GhostBody) PairCachingGhostObject(collider)
            else RigidBody(mass, collider, inertia)
        dstBody.userData = rigidBody

        if (dstBody is RigidBody && rigidBody is DynamicBody) {
            dstBody.angularFactor.set(rigidBody.angularFactor)
        }

        if (rigidBody is CharacterBody) {
            val controller = KinematicCharacterController(
                dstBody as PairCachingGhostObject,
                convertToConvexShape(dstBody.collisionShape),
                rigidBody
            )
            rigidBody.nativeInstance2 = controller
            bulletInstance.addAction(controller)
        }

        val pos = Stack.newVec3d()
        val rot = Stack.newQuat()
        convertEntityToPhysicsI(srcTransform, pos, rot, scale, centerOfMass)
        convertEntityToPhysicsII(pos, rot, dstBody)

        if (dstBody is RigidBody && rigidBody is PhysicalBody) {
            dstBody.friction = rigidBody.friction
            dstBody.restitution = rigidBody.restitution
            if (rigidBody is DynamicBody) {
                dstBody.linearDamping = rigidBody.linearDamping
                dstBody.angularDamping = rigidBody.angularDamping
                dstBody.linearSleepingThreshold = rigidBody.linearSleepingThreshold
                dstBody.angularSleepingThreshold = rigidBody.angularSleepingThreshold
            }
        }
        dstBody.ccdMotionThreshold = 1e-7f

        dstBody.ccdSweptSphereRadius = collider.getBoundingSphere(null)
        if (mass > 0.0 && rigidBody is DynamicBody && dstBody is RigidBody) { // else not supported
            dstBody.setLinearVelocity(rigidBody.globalLinearVelocity)
            dstBody.setAngularVelocity(rigidBody.globalAngularVelocity)
        }

        Stack.subVec3d(1)
        Stack.subVec3f(1) // inertia
        Stack.subQuat(1)

        return ScaledBody(rigidBody, dstBody, scale, centerOfMass, timeNanos, srcTransform)
    }


    private fun defineVehicle(entity: Entity, vehicleComp: Vehicle, body: RigidBody) {
        // todo correctly create vehicle, if the body is scaled
        val world = bulletInstance
        val raycaster = DefaultVehicleRaycaster(world)
        val vehicle = RaycastVehicle(body, raycaster)
        vehicle.setCoordinateSystem(0, 1, 2)
        val wheels = vehicleComp.wheels
        for (i in wheels.indices) {
            val wheel = wheels[i]
            val info = createWheelInfo(wheel, entity, vehicle)
            wheel.bulletInstance = info
        }
        // vehicle.currentSpeedKmHour
        // vehicle.applyEngineForce()
        world.addVehicle(vehicle)
        body.activationState = ActivationState.ALWAYS_ACTIVE
        raycastVehicles[entity] = vehicle
    }

    private fun createWheelInfo(
        wheel: VehicleWheel,
        vehicleEntity: Entity,
        bulletVehicle: RaycastVehicle
    ): WheelInstance {
        val transform = wheel.entity!!.fromLocalToOtherLocal(vehicleEntity, wheel.lockedTransform)
        // +w
        val connectionPointCS = Vector3f(transform.getTranslation(Vector3d()))
        // raycast direction, e.g. down, so -y
        val wheelDirectionCS = Vector3f(-transform.m10, -transform.m11, -transform.m12)
        // wheel axis, e.g. x axis, so +x
        val wheelAxleCS = Vector3f(-transform.m00, -transform.m01, -transform.m02)
        return bulletVehicle.addWheel(connectionPointCS, wheelDirectionCS, wheelAxleCS, wheel, timeNanos)
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
            // CharacterBody is GhostBody
            // KinematicBody is PhysicalBody
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
            val otherBody = getRigidbody(otherEngineBody)?.external
            if (otherBody is RigidBody) {
                // create constraint
                val bulletConstraint = constraint.createConstraint(bulletBody, otherBody)
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
                val wheelI = wheel.config
                val wheelE = wheelI.entity ?: continue
                wheelE.transform.setGlobal(
                    wheelI.lockedTransform.mul(
                        entity.transform.globalTransform,
                        wheelE.transform.globalTransform
                    )
                )
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

    override fun step(printSlack: Boolean) {
        // just in case
        if (printSlack) {
            Stack.printSizes()
        }
        // Stack.limit = 1024
        Stack.reset(printSlack)
        super.step(printSlack)
    }

    var maxSubSteps = 16
    var deactivationTime = 0.1f

    override fun worldStepSimulation(step: Double) {
        try {
            Stack.reset(false)

            // define all thread-local constants
            BulletGlobals.deactivationTime = deactivationTime

            val stepF = step.toFloat()
            bulletInstance.stepSimulation(stepF, maxSubSteps, stepF)
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

    override fun isDynamic(scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>): Boolean {
        val rigidbody = scaledBody.external
        return (rigidbody is RigidBody && !rigidbody.isStaticOrKinematicObject) ||
                scaledBody.internal is CharacterBody
    }

    override fun isActive(scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>): Boolean {
        val rigidbody = scaledBody.external
        return (rigidbody is RigidBody && rigidbody.isActive) ||
                scaledBody.internal is CharacterBody
    }

    override fun worldRemoveRigidbody(scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>) {
        bulletInstance.removeCollisionObject(scaledBody.external)
        val character = scaledBody.internal as? CharacterBody
        val controller = character?.nativeInstance2
        if (controller != null) bulletInstance.removeAction(controller)
    }

    override fun convertEntityToPhysicsII(srcPosition: Vector3d, srcRotation: Quaternionf, dstBody: CollisionObject) {
        val dst = dstBody.worldTransform
        dst.origin.set(srcPosition)
        dst.basis.set(srcRotation)

        if (dstBody is RigidBody) { // is this necessary?
            dstBody.interpolationWorldTransform.set(dst)
        }
    }

    override fun convertPhysicsToEntityI(srcBody: CollisionObject, dstPosition: Vector3d, dstRotation: Quaternionf) {
        val src = srcBody.worldTransform
        dstPosition.set(src.origin)
        src.getRotation(dstRotation)
    }

    override fun updateDynamicRigidBody(
        scaledBody: ScaledBody<PhysicsBody<*>, CollisionObject>,
        timeNanos: Long
    ) {
        super.updateDynamicRigidBody(scaledBody, timeNanos)
        val (dst, src) = scaledBody
        if (dst !is DynamicBody || src !is RigidBody) return
        dst.globalLinearVelocity.set(src.linearVelocity)
        dst.globalAngularVelocity.set(src.angularVelocity)
    }

    override fun updateWheels(timeNanos: Long) {
        super.updateWheels(timeNanos)

        for ((_, v) in raycastVehicles) {
            val wheels = v.wheels
            for (i in wheels.indices) {
                val wheelI = wheels[i]
                val src = wheelI.worldTransform
                synchronized(wheelI) {
                    wheelI.push(timeNanos)
                    wheelI.position1.set(src.origin)
                    src.basis.getNormalizedRotation(wheelI.rotation1)
                }
            }
        }
    }

    override fun updateDynamicEntities(timeNanos: Long) {
        super.updateDynamicEntities(timeNanos)

        val invDt = 1f / timeStepNanos
        // todo use correct scale instead of hardcoded 1.0
        val scale = JomlPools.vec3f.create().set(1.0)
        val centerOfMass = JomlPools.vec3d.create().set(0.0)

        val pos = JomlPools.vec3d.create()
        val rot = JomlPools.quat4f.create()

        for ((_, v) in raycastVehicles) {
            val wheels = v.wheels
            for (i in wheels.indices) {
                val wheelI = wheels[i]
                val dst = wheelI.config.entity?.transform ?: continue
                val dstTransform = dst.globalTransform

                synchronized(wheelI) {
                    wheelI.interpolate(timeNanos, invDt, pos, rot)
                }
                convertPhysicsToEntityII(pos, rot, dstTransform, scale, centerOfMass)
                dst.setStateAndUpdate(me.anno.ecs.Transform.State.VALID_GLOBAL)
            }
        }

        JomlPools.vec3f.sub(1) // scale
        JomlPools.vec3d.sub(2) // centerOfMass
        JomlPools.quat4f.sub(1)
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
        val dynamicBody = entity.getComponent(DynamicBody::class, false) ?: return
        val nativeInstance = dynamicBody.nativeInstance ?: return

        entity.validateTransform() // we need to know the global transform
        val srcMatrix = entity.transform.globalTransform

        // todo support scale changes, and adjust Entity.scale then, too
        val scale = JomlPools.vec3f.create()
        srcMatrix.getScale(scale)

        val pos = JomlPools.vec3d.create()
        val rot = JomlPools.quat4f.create()
        convertEntityToPhysicsI(srcMatrix, pos, rot, scale, dynamicBody.centerOfMass)
        convertEntityToPhysicsII(pos, rot, nativeInstance)
        nativeInstance.activate() // it was moved, so it must be reactivated

        JomlPools.vec3f.sub(1)
        JomlPools.vec3d.sub(1)
        JomlPools.quat4f.sub(1)
    }

    private fun createBulletWorldWithGravity(): DiscreteDynamicsWorld {
        val world = createBulletWorld()
        world.setGravity(gravity)
        return world
    }

    override fun updateGravity() {
        bulletInstance.setGravity(gravity)
    }

    override fun clear() {
        super.clear()
        bulletInstance = createBulletWorldWithGravity()
        raycastVehicles.clear()
        invalidEntities.clear()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(BulletPhysics::class)

        fun createBulletWorld(): DiscreteDynamicsWorld {
            val collisionConfig = DefaultCollisionConfiguration()
            val dispatcher = CollisionDispatcher(collisionConfig)
            val bp = DbvtBroadphase()
            val solver = SequentialImpulseConstraintSolver()
            val world = DiscreteDynamicsWorld(dispatcher, bp, solver)
            world.broadphase.overlappingPairCache.setInternalGhostPairCallback(GhostPairCallback())
            world.debugDrawer = BulletDebugDraw
            return world
        }
    }
}