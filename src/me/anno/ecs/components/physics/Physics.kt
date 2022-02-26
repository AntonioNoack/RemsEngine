package me.anno.ecs.components.physics

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.components.physics.constraints.Constraint
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.structures.sets.ParallelHashSet
import me.anno.utils.types.AABBs.set
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import kotlin.reflect.KClass

abstract class Physics<InternalRigidBody : Component, ExternalRigidBody>(val irbClass: KClass<InternalRigidBody>) :
    Component() {

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

    @NotSerializedProperty
    var time = 0L

    abstract fun updateGravity()

    override fun onCreate() {
        super.onCreate()
        rigidBodies.clear()
        nonStaticRigidBodies.clear()
    }

    fun invalidate(entity: Entity) {
        // LOGGER.debug("invalidated ${System.identityHashCode(this)}")
        invalidEntities.add(entity)
    }

    fun validate() {
        // LOGGER.debug("validating ${System.identityHashCode(this)}")
        invalidEntities.process2x({ entity ->
            remove(entity, false)
            // will be re-added by addOrGet
            // todo add to invalidEntities somehow? mmh...
            entity.allComponents(Constraint::class) {
                val other = it.other?.entity
                if (other != null) {
                    remove(other, false)
                }
                false
            }
        }, { entity ->
            val rigidbody = addOrGet(entity)
            entity.isPhysicsControlled = rigidbody != null
        })
    }

    fun <V : Component> getValidComponents(entity: Entity, clazz: KClass<V>): Sequence<V> {
        // only collect colliders, which are appropriate for this: stop at any other rigidbody
        return sequence {
            // todo also only collect physics colliders, not click-colliders
            @Suppress("unchecked_cast")
            yieldAll(entity.components.filter { it.isEnabled && clazz.isInstance(it) } as List<V>)
            for (child in entity.children) {
                if (child.isEnabled && !child.hasComponent(irbClass, false)) {
                    yieldAll(getValidComponents(child, clazz))
                }
            }
        }
    }

    open fun remove(entity: Entity, fallenOutOfWorld: Boolean){
        val rigid = rigidBodies.remove(entity) ?: return
        LOGGER.debug("- ${entity.prefabPath}")
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
            LOGGER.debug("+ ${entity.prefabPath}")
        }
        return bodyWithScale?.body
    }

    fun addOrGet(entity: Entity): ExternalRigidBody? {
        // LOGGER.info("adding ${entity.name} maybe, ${entity.getComponent(Rigidbody::class, false)}")
        val rigidbody = entity.getComponent(irbClass, false) ?: return null
        return if (rigidbody.isEnabled) {
            getRigidbody(rigidbody)
        } else null
    }

    fun callUpdates() {
        /* val tmp = Stack.borrowTrans()
        for ((body, scaledBody) in rigidBodies) {
            val physics = scaledBody.second
            physics.clearForces() // needed???...
            // testing force: tornado
            physics.getWorldTransform(tmp)
            val f = 1.0 + 0.01 * sq(tmp.origin.x, tmp.origin.z)
            physics.applyCentralForce(Vector3d(tmp.origin.z / f, 0.0, -tmp.origin.x / f))
        }*/
        for (body in rigidBodies.keys) {
            body.physicsUpdate()
        }
    }

    override fun onUpdate(): Int {
        // todo call this async, and when the step is done
        step((Engine.deltaTime * 1e9f).toLong(), false)
        return 1
    }

    @HideInInspector
    @NotSerializedProperty
    fun startWork() {
        /*val syncMaster = ECSTypeLibrary.syncMaster
        var first = true
        syncMaster.addThread("Physics", {

            if (first) {
                Thread.sleep(2000)
                this.time = Engine.gameTime
                first = false
                LOGGER.warn("Starting physics")
            }

            val targetUPS = targetUpdatesPerSecond
            val targetStep = 1.0 / targetUPS
            val targetStepNanos = (targetStep * 1e9).toLong()

            //  todo if too far back in time, just simulate that we are good

            val targetTime = Engine.gameTime
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
                val debug = false //Engine.gameTime > 10e9 // wait 10s
                if (debug) {
                    Stack.printClassUsage()
                    Stack.printSizes()
                }
                step(targetStepNanos, debug)
                val t1 = System.nanoTime()
                addEvent { FrameTimes.putValue((t1 - t0) * 1e-9f, 0xffff99 or black) }
                0
            }

        }, { debugDraw })*/
    }

    abstract fun worldStepSimulation(step: Double)

    abstract fun worldRemoveRigidbody(rigidbody: ExternalRigidBody)

    abstract fun isActive(rigidbody: ExternalRigidBody): Boolean

    abstract fun convertTransformMatrix(
        rigidbody: ExternalRigidBody, scale: Vector3d,
        dstTransform: Matrix4x3d
    )

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

        this.time += dt

        // is not correct for the physics, but we use it for gfx only anyways
        // val time = Engine.gameTime

        val deadEntities = ArrayList<Entity>()
        val deadRigidBodies = ArrayList<ExternalRigidBody>()

        for ((entity, rigidbodyWithScale) in nonStaticRigidBodies) {

            val (rigidbody, scale) = rigidbodyWithScale
            if (!isActive(rigidbody)) continue

            val dst = entity.transform
            val dstTransform = dst.globalTransform
            convertTransformMatrix(rigidbody, scale, dstTransform)

            dst.setStateAfterUpdate(me.anno.ecs.Transform.State.VALID_GLOBAL)

            if (!allowedSpace.testPoint(dstTransform.m30(), dstTransform.m31(), dstTransform.m32())) {
                // delete the entity
                deadEntities.add(entity)
                deadRigidBodies.add(rigidbody)
                continue
            }

        }

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
        }

        // clock.total("physics step", 0.1)

    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Physics<*, *>
        clone.allowedSpace.set(allowedSpace)
        clone.targetUpdatesPerSecond = targetUpdatesPerSecond
        clone.time = time
    }

}