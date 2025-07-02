package me.anno.ecs.components.physics

import me.anno.Engine
import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllChildren
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.Events.addEvent
import me.anno.engine.RemsEngine
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.black
import me.anno.utils.Logging.hash32
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.sets.ParallelHashSet
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.toLongOr
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

abstract class Physics<InternalRigidBody : Component, ExternalRigidBody>(
    val rigidComponentClass: KClass<InternalRigidBody>,
) : System(), OnUpdate {

    companion object {
        private val LOGGER = LogManager.getLogger(Physics::class)

        fun <V : Component, R : Component> getValidComponents(
            root: Entity, rigidComponentClass: KClass<R>, clazz: KClass<V>,
            dst: ArrayList<V> = ArrayList()
        ): List<V> {
            // only collect colliders, which are appropriate for this: stop at any other rigidbody
            Recursion.processRecursive(root) { entity, remaining ->
                entity.forAllComponents(clazz, false) { comp ->
                    dst.add(comp)
                }
                entity.forAllChildren(false) { child ->
                    if (!child.hasComponent(rigidComponentClass, false)) {
                        remaining.add(child)
                    }
                }
            }
            return dst
        }

        fun <V : Component, R : Component> hasValidComponents(
            root: Entity, rigidComponentClass: KClass<R>, clazz: KClass<V>
        ): Boolean {
            // only collect colliders, which are appropriate for this: stop at any other rigidbody
            return Recursion.anyRecursive(root) { entity, remaining ->
                entity.forAllChildren(false) { child ->
                    if (!child.hasComponent(rigidComponentClass, false)) {
                        remaining.add(child)
                    }
                }
                entity.hasComponent(clazz, false)
            }
        }
    }

    // entities outside these bounds will be killed
    @SerializedProperty
    var allowedSpace: AABBd = AABBd()
        .setMin(-1e300, -100.0, -1e300)
        .setMax(+1e300, 1e300, +1e300)

    @SerializedProperty
    var gravity: Vector3d = Vector3d(0.0, -9.81, 0.0)
        set(value) {
            field = value
            updateGravity()
        }

    @NotSerializedProperty
    val invalidEntities = ParallelHashSet<Entity>(256)

    @NotSerializedProperty
    val rigidBodies = HashMap<Entity, ScaledBody<InternalRigidBody, ExternalRigidBody>?>()

    @NotSerializedProperty
    val physicsUpdateListeners = HashSet<OnPhysicsUpdate>()

    @SerializedProperty
    var targetUpdatesPerSecond = 30.0

    @DebugProperty
    @Group("Bodies")
    val numDynamicBodies: Int
        get() = rigidBodies.values.count { scaledBody ->
            scaledBody != null && isDynamic(scaledBody.external)
        }

    @DebugProperty
    @Group("Bodies")
    val numRegisteredBodies: Int
        get() = rigidBodies.size

    @DebugProperty
    @Group("Bodies")
    val numBodies: Int
        get() = rigidBodies.values.count { it != null }

    @DebugProperty
    @Group("Bodies")
    val numInvalidBodies: Int
        get() = invalidEntities.size

    @DebugProperty
    @NotSerializedProperty
    var timeNanos = 0L

    var printValidations = false

    abstract fun updateGravity()

    @DebugAction
    fun invalidateAll() {
        (Systems.world as? Entity)?.forAll {
            val rigidBody = rigidComponentClass.safeCast(it)
            if (rigidBody != null) {
                val e = rigidBody.entity
                if (e != null) invalidate(e)
            }
        }
    }

    open fun invalidate(entity: Entity) {
        if (printValidations) LOGGER.debug("Invalidated {}", hash32(entity))
        invalidEntities.add(entity)
    }

    abstract fun isDynamic(rigidbody: ExternalRigidBody): Boolean

    open fun invalidateTransform(entity: Entity) {
        invalidate(entity)
    }

    override fun clear() {
        // todo this could be done more efficiently, I think...
        for ((_, scaledBody) in rigidBodies) {
            worldRemoveRigidbody(scaledBody ?: continue)
        }
        rigidBodies.clear()
        invalidEntities.clear() // correct??
    }

    fun validate() {
        if (printValidations) LOGGER.debug("Validating {}", hash32(this))
        invalidEntities.process2x(removal, creating)
    }

    private val removal = { entity: Entity ->
        remove(entity, false)
        removeConstraints(entity)
    }

    private val creating = { entity: Entity ->
        val rigidbody = addOrGet(entity)
        entity.isPhysicsControlled = rigidbody != null && isDynamic(rigidbody)
    }

    override fun setContains(entity: Entity, contains: Boolean) {
        invalidate(entity)
    }

    override fun setContains(component: Component, contains: Boolean) {
        invalidate(component.entity ?: return)
        if (component is OnPhysicsUpdate) {
            physicsUpdateListeners.setContains(component, contains)
        }
    }

    abstract fun removeConstraints(entity: Entity)

    open fun remove(entity: Entity, fallenOutOfWorld: Boolean) {
        val rigid = rigidBodies.remove(entity) ?: return
        worldRemoveRigidbody(rigid)
    }

    abstract fun createRigidbody(
        entity: Entity,
        rigidBody: InternalRigidBody
    ): ScaledBody<InternalRigidBody, ExternalRigidBody>?

    abstract fun onCreateRigidbody(
        entity: Entity,
        rigidbody: InternalRigidBody,
        scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>
    )

    fun getRigidbody(rigidBody: InternalRigidBody): ExternalRigidBody? {

        // todo when a rigidbody is invalidated, also re-create all constrained rigidbodies!
        // todo otherwise we'll get issues, where one partner no longer is part of the world...

        // todo possible solution: instead of recreating the rigidbody instance, just clear all properties,
        // todo and write them again :)

        // todo also we need to somehow ensure, that constrained rigidbodies are enabled as well
        // todo we can't have constraints between two static rigidbodies

        val entity = rigidBody.entity!!
        entity.validateTransform()

        var newlyCreated = false
        val scaledBody = rigidBodies.getOrPut(entity) {
            newlyCreated = true
            createRigidbody(entity, rigidBody)
        }
        if (newlyCreated && scaledBody != null) {
            // after creating and registering, so
            // it works for circular constraint dependencies
            onCreateRigidbody(entity, rigidBody, scaledBody)
            // LOGGER.debug("+ ${entity.prefabPath}")
        }
        return scaledBody?.external
    }

    fun isEntityValid(entity: Entity): Boolean {
        return entity.root == Systems.world && entity.allInHierarchy { it.isEnabled }
    }

    fun addOrGet(entity: Entity): ExternalRigidBody? {
        // LOGGER.info("adding ${entity.name} maybe, ${entity.getComponent(Rigidbody::class, false)}")
        return if (isEntityValid(entity)) {
            val rigidbody = entity.getComponent(rigidComponentClass, false) ?: return null
            getRigidbody(rigidbody)
        } else null
    }

    private fun invokePhysicsUpdates(dt: Double) {
        for (component in physicsUpdateListeners) {
            component.onPhysicsUpdate(dt)
        }
    }

    @Docs("If the game hang for that many milliseconds, the physics will stop being simulated, and be restarted on the next update")
    @SerializedProperty
    var simulationTimeoutMillis = 5000L

    @Docs("Whether physics should be executed in the editing environment")
    @SerializedProperty
    var updateInEditMode = false

    @NotSerializedProperty
    var lastUpdate = 0L

    @NotSerializedProperty
    private var workerThread: Thread? = null

    // todo transforms are not 100% stable with async physics
    @SerializedProperty
    var synchronousPhysics = true
        set(value) {
            if (field != value) {
                stopWorker()
                field = value
            }
        }

    @DebugAction
    fun reloadScene() {
        // todo root cannot be restored, why?
        val selected = RemsEngine.collectSelected()
        root.prefab?.invalidateInstance()
        RemsEngine.restoreSelected(selected)
    }

    private fun startWorker() {
        workerThread = thread(name = className) {
            try {
                while (!Engine.shutdown) {

                    val targetStep = 1.0 / targetUpdatesPerSecond
                    val targetStepNanos = (targetStep * 1e9).toLongOr()

                    // stop if received updates for no more than 1-3s
                    val targetTime = Time.nanoTime
                    if (abs(targetTime - lastUpdate) > simulationTimeoutMillis * MILLIS_TO_NANOS) {
                        LOGGER.debug(
                            "Stopping work, {} > {}",
                            (targetTime - lastUpdate) / 1e6,
                            simulationTimeoutMillis
                        )
                        break
                    }

                    // the absolute worst case time
                    val absMinimumTime = targetTime - targetStepNanos * 2
                    if (timeNanos < absMinimumTime) {
                        // report this value somehow...
                        // there may be lots and lots of warnings, if the calculations are too slow
                        val delta = absMinimumTime - timeNanos
                        timeNanos = absMinimumTime
                        val warning = "Physics skipped ${(delta * 1e-9).f1()}s"
                        lastWarning = warning
                        LOGGER.warn(warning)
                    }

                    if (timeNanos > targetTime) {
                        // done :), sleep
                        Thread.sleep((timeNanos - targetTime) / (2 * MILLIS_TO_NANOS))
                    } else {
                        // there is still work to do
                        val t0 = Time.nanoTime
                        val debug = false //Engine.gameTime > 10e9 // wait 10s
                        step(targetStepNanos, debug)
                        val t1 = Time.nanoTime
                        addEvent { FrameTimings.putValue((t1 - t0) * 1e-9f, 0xffff99 or black) }
                    }
                }
            } catch (_: InterruptedException) {
                // we were stopped, which is fine
            } finally {
                workerThread = null
            }
        }
    }

    private fun stopWorker() {
        synchronized(this) {
            workerThread?.interrupt()
            workerThread = null
        }
    }

    override fun onUpdate() {
        lastUpdate = Time.nanoTime
        val playMode = RenderView.currentInstance?.playMode
        val shallExecute = updateInEditMode || (playMode != PlayMode.EDITING && playMode != null)
        if (shallExecute) {
            if (synchronousPhysics) {
                step((Time.deltaTime * 1e9).toLongOr(), false)
            } else {
                if (isEnabled) {
                    if (workerThread == null) {
                        startWorker()
                    }
                } else stopWorker()
            }
        } else stopWorker()
    }

    override fun destroy() {
        super.destroy()
        stopWorker()
    }

    abstract fun worldStepSimulation(step: Double)

    abstract fun worldRemoveRigidbody(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>)

    abstract fun isActive(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>): Boolean

    abstract fun getMatrix(
        rigidbody: ExternalRigidBody, dstTransform: Matrix4x3,
        scale: Vector3d, centerOfMass: Vector3d,
    )

    abstract fun setMatrix(
        rigidbody: ExternalRigidBody, srcTransform: Matrix4x3,
        scale: Vector3d, centerOfMass: Vector3d,
    )

    @DebugAction
    fun manualStep() {
        // dt = 1e9 / 60
        step(16_666_667L, true)
    }

    open fun step(dtNanos: Long, printSlack: Boolean) {
        validate()

        val dtSeconds = dtNanos * 1e-9
        beforeUpdate(dtSeconds)
        worldStepSimulation(dtSeconds)
        timeNanos += dtNanos
        afterUpdate(dtSeconds)
    }

    open fun beforeUpdate(dt: Double) {
        invokePhysicsUpdates(dt)
        updateExternalTransforms()
    }

    open fun afterUpdate(dt: Double) {
        updateInternalTransforms()
        updateWheels()
        validateEntityTransforms()
    }

    fun updateExternalTransforms() {
        for ((entity, scaledBody) in rigidBodies) {
            if (scaledBody == null || isDynamic(scaledBody.external)) continue
            val (_, rigidbody, scale, centerOfMass) = scaledBody

            entity.validateTransform()

            val srcTransform = entity.transform.globalTransform
            setMatrix(rigidbody, srcTransform, scale, centerOfMass)
        }
    }

    fun updateInternalTransforms() {
        val deadEntities = ArrayList<Entity>()
        updateDynamicBodies(deadEntities)
        for (i in deadEntities.indices) {
            remove(deadEntities[i], true)
        }
    }

    fun validateEntityTransforms() {
        // update the local transforms last, so all global transforms have been completely updated
        for ((entity, scaledBody) in rigidBodies) {
            if (scaledBody == null ||
                !isActive(scaledBody) ||
                !isDynamic(scaledBody.external)
            ) continue
            entity.invalidateAABBsCompletely()
            entity.invalidateChildTransforms()
            entity.validateTransform()
        }
    }

    fun updateDynamicBodies(deadEntities: MutableList<Entity>) {
        for ((entity, scaledBody) in rigidBodies) {
            if (scaledBody == null ||
                !isActive(scaledBody) ||
                !isDynamic(scaledBody.external)
            ) continue
            updateDynamicRigidBody(entity, scaledBody)
            checkOutOfBounds(entity, deadEntities)
        }
    }

    open fun checkOutOfBounds(entity: Entity, deadEntities: MutableList<Entity>) {
        val dstTransform = entity.transform.globalTransform
        if (!allowedSpace.testPoint(dstTransform.m30, dstTransform.m31, dstTransform.m32)) {
            // delete the entity
            deadEntities.add(entity)
        }
    }

    open fun updateDynamicRigidBody(
        entity: Entity, rigidbodyScaled: ScaledBody<InternalRigidBody, ExternalRigidBody>
    ) {
        val (_, rigidbody, scale, centerOfMass) = rigidbodyScaled
        val dst = entity.transform
        val dstTransform = dst.globalTransform
        getMatrix(rigidbody, dstTransform, scale, centerOfMass)
        dst.setStateAndUpdate(Transform.State.VALID_GLOBAL)
    }

    open fun updateWheels() {
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Physics<*, *>
        dst.allowedSpace.set(allowedSpace)
        dst.targetUpdatesPerSecond = targetUpdatesPerSecond
        dst.timeNanos = timeNanos
        dst.synchronousPhysics = synchronousPhysics
        dst.gravity.set(gravity)
    }
}