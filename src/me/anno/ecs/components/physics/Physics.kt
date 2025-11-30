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
import me.anno.maths.Maths.ceilDiv
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.black
import me.anno.utils.Logging.hash32
import me.anno.utils.Threads
import me.anno.utils.algorithms.Recursion
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.sets.ParallelHashSet
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.toLongOr
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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

        fun convertPhysicsToEntityII(
            srcPosition: Vector3d, srcRotation: Quaternionf,
            dstMatrix: Matrix4x3,
            scale: Vector3f, centerOfMass: Vector3d,
        ) {
            // bullet does not support scale -> we always need to correct it
            // some little extra calculations, but also more accurate, because of float-double conversions
            dstMatrix.set(srcRotation)
                .scale(scale)
                .translate(-centerOfMass.x / scale.x, -centerOfMass.y / scale.y, -centerOfMass.z / scale.z)
                .translateLocal(srcPosition)
        }

        fun convertEntityToPhysicsI(
            srcMatrix: Matrix4x3,
            dstPosition: Vector3d, dstRotation: Quaternionf,
            scale: Vector3f, centerOfMass: Vector3d,
        ) {
            // bullet does not support scale -> we always must correct it
            val sx = 1f / scale.x
            val sy = 1f / scale.y
            val sz = 1f / scale.z
            dstRotation.setFromNormalized( // = srcMatrix.getUnnormalizedRotation(dst), but without 3x sqrt
                srcMatrix.m00 * sx, srcMatrix.m01 * sx, srcMatrix.m02 * sx,
                srcMatrix.m10 * sy, srcMatrix.m11 * sy, srcMatrix.m12 * sy,
                srcMatrix.m20 * sz, srcMatrix.m21 * sz, srcMatrix.m22 * sz,
            )
            centerOfMass
                .rotate(dstRotation, dstPosition)
                .add(srcMatrix.m30, srcMatrix.m31, srcMatrix.m32)
        }
    }

    // entities outside these bounds will be killed
    @SerializedProperty
    var allowedSpace: AABBd = AABBd()
        .setMin(-1e300, -100.0, -1e300)
        .setMax(+1e300, 1e300, +1e300)

    @SerializedProperty
    var gravity: Vector3f = Vector3f(0f, -9.81f, 0f)
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
    var stepsPerSecond = 30f

    @DebugProperty
    @Group("Bodies")
    val numDynamicBodies: Int
        get() = synchronized(rigidBodies) {
            rigidBodies.values.count { scaledBody ->
                scaledBody != null && isDynamic(scaledBody)
            }
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

    abstract fun isDynamic(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>): Boolean

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

    @DebugAction
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

    fun getRigidbody(rigidBody: InternalRigidBody): ScaledBody<InternalRigidBody, ExternalRigidBody>? {

        // todo when a rigidbody is invalidated, also re-create all constrained rigidbodies!
        //  otherwise we'll get issues, where one partner no longer is part of the world...

        // todo possible solution: instead of recreating the rigidbody instance, just clear all properties,
        //  and write them again :)

        val entity = rigidBody.entity!!
        entity.validateTransform()

        var newlyCreated = false
        val scaledBody = synchronized(rigidBodies) {
            rigidBodies.getOrPut(entity) {
                newlyCreated = true
                createRigidbody(entity, rigidBody)
            }
        }
        if (newlyCreated && scaledBody != null) {
            // after creating and registering, so
            // it works for circular constraint dependencies
            onCreateRigidbody(entity, rigidBody, scaledBody)
            // LOGGER.debug("+ ${entity.prefabPath}")
        }
        return scaledBody
    }

    fun isEntityValid(entity: Entity): Boolean {
        return entity.root == Systems.world && entity.allInHierarchy { it.isEnabled }
    }

    fun addOrGet(entity: Entity): ScaledBody<InternalRigidBody, ExternalRigidBody>? {
        // LOGGER.info("adding ${entity.name} maybe, ${entity.getComponent(Rigidbody::class, false)}")
        return if (isEntityValid(entity)) {
            val rigidbody = entity.getComponent(rigidComponentClass, false) ?: return null
            getRigidbody(rigidbody)
        } else null
    }

    private fun callOnPhysicsUpdate(dt: Double) {
        for (component in physicsUpdateListeners) {
            component.onPhysicsUpdate(dt)
        }
    }

    @Docs("If the game hangs for that many milliseconds, the physics will stop being simulated, and be restarted on the next update")
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

    val timeStepNanos get() = max((1e9 / stepsPerSecond).toLongOr(), 1)

    fun skipTimeIfNeeded() {
        // the absolute worst case time
        val targetTime = Time.gameTimeN
        val targetStepNanos = timeStepNanos
        val absMinimumTime = targetTime - targetStepNanos * 3
        if (timeNanos < absMinimumTime) {
            // report this value somehow...
            // there may be lots and lots of warnings, if the calculations are too slow
            val skippedTime = targetTime - timeNanos
            timeNanos = targetTime
            val warning = "Physics skipped ${(skippedTime * 1e-9).f1()}s"
            lastWarning = warning
            LOGGER.warn(warning)
        }
    }

    private fun startWorker() {
        workerThread = Threads.runWorkerThread(className) {
            try {
                while (!Engine.shutdown) {

                    // stop if received updates for no more than 1-3s
                    val diffToLastUpdate = abs(Time.nanoTime - lastUpdate)
                    if (diffToLastUpdate > simulationTimeoutMillis * MILLIS_TO_NANOS) {
                        LOGGER.debug("Stopping work, {} ms > {} ms", diffToLastUpdate / 1e6, simulationTimeoutMillis)
                        break
                    }

                    // the absolute worst case time
                    skipTimeIfNeeded()
                    if (numPhysicsSteps > 0) {
                        // there is still work to do
                        step(false)
                    } else {
                        // done, sleep a little
                        Thread.sleep(1)
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

    var maxStepsPerFrame = 10
    val numPhysicsSteps: Int
        get() = min(ceilDiv(Time.gameTimeN - timeNanos, timeStepNanos).toInt(), maxStepsPerFrame)

    val shallExecute: Boolean
        get() {
            val playMode = RenderView.currentInstance?.playMode
            return updateInEditMode || (playMode != PlayMode.EDITING && playMode != null)
        }

    override fun onUpdate() {
        lastUpdate = Time.nanoTime
        if (shallExecute) {
            if (synchronousPhysics) {
                // todo share the same timing logic in sync and async
                skipTimeIfNeeded()
                repeat(numPhysicsSteps) {
                    step(false)
                }
            } else {
                if (isEnabled) {
                    if (workerThread == null) {
                        startWorker()
                    }
                } else stopWorker()
            }
            updateDynamicEntities(Time.gameTimeN)
            validateEntityTransforms()
        } else stopWorker()
    }

    override fun destroy() {
        super.destroy()
        stopWorker()
    }

    abstract fun worldStepSimulation(step: Double)

    abstract fun worldRemoveRigidbody(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>)

    abstract fun isActive(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>): Boolean

    abstract fun convertPhysicsToEntityI(
        srcBody: ExternalRigidBody,
        dstPosition: Vector3d, dstRotation: Quaternionf,
    )

    abstract fun convertEntityToPhysicsII(
        srcPosition: Vector3d, srcRotation: Quaternionf,
        dstBody: ExternalRigidBody,
    )

    @DebugAction
    fun manualStep() {
        step(true)
    }

    open fun step(printSlack: Boolean) {
        val t0 = Time.nanoTime

        validate()
        val dtNanos = timeStepNanos
        val dtSeconds = dtNanos * 1e-9
        beforeUpdate(dtSeconds, timeNanos)
        worldStepSimulation(dtSeconds)
        timeNanos += dtNanos
        afterUpdate(dtSeconds, timeNanos)

        val t1 = Time.nanoTime
        addEvent { FrameTimings.putValue((t1 - t0) * 1e-9f, 0xffff99 or black) }
    }

    open fun beforeUpdate(dt: Double, timeNanos: Long) {
        callOnPhysicsUpdate(dt)
        updateEntityControlledTransforms()
    }

    open fun afterUpdate(dt: Double, timeNanos: Long) {
        updatePhysicsControlledTransforms(timeNanos)
        updateWheels(timeNanos)
    }

    fun updateEntityControlledTransforms() {
        val pos = Vector3d()
        val rot = Quaternionf()
        synchronized(rigidBodies) {
            for ((entity, scaledBody) in rigidBodies) {
                if (scaledBody == null || isDynamic(scaledBody)) continue
                val (_, dstBody, scale, centerOfMass) = scaledBody

                entity.validateTransform()

                val srcTransform = entity.transform.globalTransform
                convertEntityToPhysicsI(srcTransform, pos, rot, scale, centerOfMass)
                convertEntityToPhysicsII(pos, rot, dstBody)
            }
        }
    }

    fun updatePhysicsControlledTransforms(timeNanos: Long) {
        val deadEntities = ArrayList<Entity>()
        updateDynamicBodies(deadEntities, timeNanos)
        for (i in deadEntities.indices) {
            remove(deadEntities[i], true)
        }
    }

    fun validateEntityTransforms() {
        // update the local transforms last, so all global transforms have been completely updated
        synchronized(rigidBodies) {
            for ((entity, scaledBody) in rigidBodies) {
                if (scaledBody == null ||
                    !isActive(scaledBody) ||
                    !isDynamic(scaledBody)
                ) continue
                entity.invalidateAABBsCompletely()
                entity.invalidateChildTransforms()
                entity.validateTransform()
            }
        }
    }

    fun updateDynamicBodies(deadEntities: MutableList<Entity>, timeNanos: Long) {
        synchronized(rigidBodies) {
            for ((entity, scaledBody) in rigidBodies) {
                if (scaledBody == null ||
                    !isActive(scaledBody) ||
                    !isDynamic(scaledBody)
                ) continue
                updateDynamicRigidBody(scaledBody, timeNanos)
                checkOutOfBounds(entity, deadEntities)
            }
        }
    }

    open fun updateDynamicEntities(timeNanos: Long) {
        val dstPosition = JomlPools.vec3d.create()
        val dstRotation = JomlPools.quat4f.create()
        synchronized(rigidBodies) {
            val invDt = 1f / timeStepNanos
            for ((entity, scaledBody) in rigidBodies) {
                if (scaledBody == null || !isActive(scaledBody) || !isDynamic(scaledBody)) continue

                val dst = entity.transform
                synchronized(scaledBody) {
                    scaledBody.interpolate(timeNanos, invDt, dstPosition, dstRotation)
                }
                convertPhysicsToEntityII(
                    dstPosition, dstRotation, dst.globalTransform,
                    scaledBody.scale, scaledBody.centerOfMass
                )
                dst.setStateAndUpdate(Transform.State.VALID_GLOBAL)
            }
        }
        JomlPools.vec3d.sub(1)
        JomlPools.quat4f.sub(1)
    }

    open fun checkOutOfBounds(entity: Entity, deadEntities: MutableList<Entity>) {
        val dstTransform = entity.transform.globalTransform
        if (!allowedSpace.testPoint(dstTransform.m30, dstTransform.m31, dstTransform.m32)) {
            // delete the entity
            deadEntities.add(entity)
        }
    }

    open fun updateDynamicRigidBody(scaledBody: ScaledBody<InternalRigidBody, ExternalRigidBody>, timeNanos: Long) {
        synchronized(scaledBody) {
            scaledBody.push(timeNanos)
            convertPhysicsToEntityI(scaledBody.external, scaledBody.position1, scaledBody.rotation1)
        }
    }

    open fun updateWheels(timeNanos: Long) {
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Physics<*, *>
        dst.allowedSpace.set(allowedSpace)
        dst.stepsPerSecond = stepsPerSecond
        dst.timeNanos = timeNanos
        dst.synchronousPhysics = synchronousPhysics
        dst.gravity.set(gravity)
    }
}