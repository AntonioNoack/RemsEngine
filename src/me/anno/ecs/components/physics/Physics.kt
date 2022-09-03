package me.anno.ecs.components.physics

import cz.advel.stack.Stack
import me.anno.Engine
import me.anno.config.DefaultStyle.black
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.RemsEngine
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.debug.FrameTimings
import me.anno.utils.structures.sets.ParallelHashSet
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.reflect.KClass

abstract class Physics<InternalRigidBody : Component, ExternalRigidBody>(
    val rigidComponentClass: KClass<InternalRigidBody>
) : Component() {

    companion object {
        private val LOGGER = LogManager.getLogger(Physics::class)
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
    val rigidBodies = HashMap<Entity, BodyWithScale<ExternalRigidBody>?>()

    @NotSerializedProperty
    val nonStaticRigidBodies = HashMap<Entity, BodyWithScale<ExternalRigidBody>>()

    @SerializedProperty
    var targetUpdatesPerSecond = 30.0

    @DebugProperty
    val numNonStatic get() = nonStaticRigidBodies.size

    @DebugProperty
    val numRigidbodies get() = rigidBodies.size

    @DebugProperty
    val numInvalid get() = invalidEntities.size

    @DebugProperty
    @NotSerializedProperty
    var timeNanos = 0L

    var printValidations = false

    abstract fun updateGravity()

    @DebugAction
    fun invalidateAll() {
        entity?.forAll {
            if (rigidComponentClass.isInstance(it)) {
                val e = (it as? Component)?.entity
                if (e != null) invalidate(e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        rigidBodies.clear()
        nonStaticRigidBodies.clear()
    }

    fun invalidate(entity: Entity) {
        if (printValidations) LOGGER.debug("invalidated ${System.identityHashCode(this)}")
        invalidEntities.add(entity)
    }

    fun validate() {
        if (printValidations) LOGGER.debug("validating ${System.identityHashCode(this)}")
        invalidEntities.process2x({ entity ->
            remove(entity, false)
            removeConstraints(entity)
        }, { entity ->
            val rigidbody = addOrGet(entity)
            entity.isPhysicsControlled = rigidbody != null
        })
    }

    abstract fun removeConstraints(entity: Entity)

    fun <V : Component> getValidComponents(entity: Entity, clazz: KClass<V>): Sequence<V> {
        // only collect colliders, which are appropriate for this: stop at any other rigidbody
        return sequence {
            @Suppress("unchecked_cast")
            yieldAll(entity.components.filter { it.isEnabled && clazz.isInstance(it) } as List<V>)
            for (child in entity.children) {
                if (child.isEnabled && !child.hasComponent(rigidComponentClass, false)) {
                    yieldAll(getValidComponents(child, clazz))
                }
            }
        }
    }

    open fun remove(entity: Entity, fallenOutOfWorld: Boolean) {
        val rigid = rigidBodies.remove(entity) ?: return
        LOGGER.debug("- ${entity.prefabPath ?: entity.name.ifBlank { entity.className }}")
        nonStaticRigidBodies.remove(entity)
        worldRemoveRigidbody(rigid.body)
    }

    abstract fun createRigidbody(
        entity: Entity,
        rigidBody: InternalRigidBody
    ): BodyWithScale<ExternalRigidBody>?

    abstract fun onCreateRigidbody(
        entity: Entity,
        rigidbody: InternalRigidBody,
        bodyWithScale: BodyWithScale<ExternalRigidBody>
    )

    fun getRigidbody(rigidBody: InternalRigidBody): ExternalRigidBody? {

        // todo when a rigidbody is invalidated, also re-create all constrained rigidbodies!
        // todo otherwise we'll get issues, where one partner no longer is part of the world...

        // todo possible solution: instead of recreating the rigidbody instance, just clear all properties,
        // todo and write them again :)

        // todo also we need to somehow ensure, that constrained rigidbodies are enabled as well
        // todo we can't have constraints between two static rigidbodies

        val entity = rigidBody.entity!!
        var newlyCreated = false
        val bodyWithScale = rigidBodies.getOrPut(entity) {
            newlyCreated = true
            createRigidbody(entity, rigidBody)
        }
        if (newlyCreated && bodyWithScale != null) {
            // after creating and registering, so
            // it works for circular constraint dependencies
            onCreateRigidbody(entity, rigidBody, bodyWithScale)
            LOGGER.debug("+ ${entity.prefabPath ?: entity.name.ifBlank { entity.className }}")
        }
        return bodyWithScale?.body
    }

    fun addOrGet(entity: Entity): ExternalRigidBody? {
        // LOGGER.info("adding ${entity.name} maybe, ${entity.getComponent(Rigidbody::class, false)}")
        val rigidbody = entity.getComponent(rigidComponentClass, false) ?: return null
        return if (rigidbody.isEnabled) getRigidbody(rigidbody) else null
    }

    private fun callUpdates() {
        for (body in rigidBodies.keys) {
            body.physicsUpdate()
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

    @SerializedProperty
    var synchronousPhysics = false
        set(value) {
            if (field != value) {
                stopWorker()
                field = value
            }
        }

    var showDebug = false

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

                    val targetUPS = targetUpdatesPerSecond
                    val targetStep = 1.0 / targetUPS
                    val targetStepNanos = (targetStep * 1e9).toLong()

                    // stop if received updates for no more than 1-3s
                    val targetTime = Engine.nanoTime
                    if (abs(targetTime - lastUpdate) > simulationTimeoutMillis * MILLIS_TO_NANOS) {
                        LOGGER.debug("Stopping work, ${(targetTime - lastUpdate) / 1e6} > $simulationTimeoutMillis")
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
                        val t0 = System.nanoTime()
                        val debug = false //Engine.gameTime > 10e9 // wait 10s
                        if (debug) {
                            Stack.printClassUsage()
                            Stack.printSizes()
                        }
                        step(targetStepNanos, debug)
                        val t1 = System.nanoTime()
                        addEvent { FrameTimings.putValue((t1 - t0) * 1e-9f, 0xffff99 or black) }
                    }
                }
            } catch (e: InterruptedException) {
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

    override fun onUpdate(): Int {
        super.onUpdate()
        lastUpdate = Engine.nanoTime
        val shallExecute = updateInEditMode || RenderView.currentInstance?.playMode != PlayMode.EDITING
        if (shallExecute) {
            if (synchronousPhysics) {
                step((Engine.deltaTime * 1e9).toLong(), false)
            } else {
                if (isEnabled) {
                    if (workerThread == null) {
                        startWorker()
                    }
                } else stopWorker()
            }
        } else stopWorker()
        return 1
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWorker()
    }

    abstract fun worldStepSimulation(step: Double)

    abstract fun worldRemoveRigidbody(rigidbody: ExternalRigidBody)

    abstract fun isActive(rigidbody: ExternalRigidBody): Boolean

    abstract fun convertTransformMatrix(
        rigidbody: ExternalRigidBody, scale: Vector3d,
        dstTransform: Matrix4x3d
    )

    @DebugAction
    fun manualStep() {
        // dt = 1e9 / 60
        step(16_666_667L, true)
    }

    open fun step(dt: Long, printSlack: Boolean) {

        // clock.start()

        // val oldSize = rigidBodies.size
        validate()
        // val newSize = rigidBodies.size
        // clock.stop("added ${newSize - oldSize} entities")

        callUpdates()

        val step = dt * 1e-9
        worldStepSimulation(step)

        // clock.stop("calculated changes, step ${dt * 1e-9}", 0.1)

        this.timeNanos += dt

        // is not correct for the physics, but we use it for gfx only anyway
        // val time = Engine.gameTime

        val deadEntities = ArrayList<Entity>()
        val deadRigidBodies = ArrayList<ExternalRigidBody>()

        for ((entity, rigidbodyWithScale) in nonStaticRigidBodies) {

            val (rigidbody, scale) = rigidbodyWithScale
            if (!isActive(rigidbody)) continue

            val dst = entity.transform
            val dstTransform = dst.globalTransform
            convertTransformMatrix(rigidbody, scale, dstTransform)

            dst.setStateAndUpdate(me.anno.ecs.Transform.State.VALID_GLOBAL)

            if (!allowedSpace.testPoint(dstTransform.m30, dstTransform.m31, dstTransform.m32)) {
                // delete the entity
                deadEntities.add(entity)
                deadRigidBodies.add(rigidbody)
                continue
            }

        }

        updateWheels()

        for (i in deadEntities.indices) {
            remove(deadEntities[i], true)
            worldRemoveRigidbody(deadRigidBodies[i])
        }

        // update the local transforms last, so all global transforms have been completely updated
        for ((entity, bodyWithScale) in nonStaticRigidBodies) {
            if (!isActive(bodyWithScale.body)) continue
            // val dst = entity.transform
            // dst.calculateLocalTransform((entity.parent as? Entity)?.transform)
            entity.invalidateAABBsCompletely()
            entity.invalidateChildTransforms()
            entity.validateTransform()
        }

        // clock.total("physics step", 0.1)

    }

    open fun updateWheels(){

    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Physics<*, *>
        clone.allowedSpace.set(allowedSpace)
        clone.targetUpdatesPerSecond = targetUpdatesPerSecond
        clone.timeNanos = timeNanos
        clone.synchronousPhysics = synchronousPhysics
        clone.gravity.set(gravity)
    }

}