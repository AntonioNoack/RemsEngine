package me.anno.games.spiderplaguehotel

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.systems.Updatable
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.max
import me.anno.tests.utils.SpiderPrediction
import me.anno.utils.hpc.threadLocal
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class SpiderLogic(val brain: FullyConnectedNN, val traps: Entity) : Component(), Updatable {

    companion object {

        val spiderWorldScale = 200.0

        var savedDt = 1f

        var defaultStepsPerSeconds = 15
        var simulationMultiplier = 1

        val speedScale = 1f / defaultStepsPerSeconds

        val sizes = intArrayOf(1, 2)
        val stateSize = sizes.sum()
        val states = threadLocal { FloatArray(stateSize) }

        val trapSize = 0.03f
        val trapPositions = ArrayList<Vector2f>()

        fun Vector2f.manhattenDistance(other: Vector2f): Float {
            val dx = x - other.x
            val dy = y - other.y
            return abs(dx) + abs(dy)
        }
    }

    // spiders walk in 2d, in UV space

    val position = Vector2f()
    var rotation = 0f

    override fun update(instances: List<Updatable>) {
        // get game speed somehow
        savedDt += (defaultStepsPerSeconds * simulationMultiplier) * Time.deltaTime.toFloat()
        if (savedDt < 1f) return
        // while dt > threshold, do step
        while (savedDt >= 1f) {
            step(instances)
            savedDt -= 1f
        }
    }

    fun step(instances: List<Updatable>) {
        val state = states.get()
        for (instance in instances) {
            if (instance is SpiderLogic) {
                instance.step(state)
            }
        }
        // update traps
        for (trap in traps.children) {
            trap.getComponent(SpiderTrap::class)?.onUpdate()
        }
    }

    fun step(state: FloatArray) {
        feedNeuralNetwork(state)
        brain.predict(state)
        actOnNeuralNetwork(state)
    }

    fun feedNeuralNetwork(values: FloatArray) {
        // feed information about distance to a trap
        var danger = 0f
        val dangerZone = 1f / trapSize
        for (i in trapPositions.indices) {
            val trap = trapPositions[i]
            val dist = position.manhattenDistance(trap)
            danger += (1f / (1f + dangerZone * dist))
        }
        values[0] = danger
    }

    fun actOnNeuralNetwork(values: FloatArray) {
        // implement stamina, so they don't run around too much
        val transform = transform ?: return
        val speed = 0.1f * speedScale
        val leftLegs = (values[stateSize - 2] * 2f - 1f)
        val rightLegs = (values[stateSize - 1] * 2f - 1f)

        val dRotation = rightLegs - leftLegs
        rotation += dRotation
        // only walking forward
        val dMotion = max(rightLegs + leftLegs, 0f) * 0.5f
        val motion = dMotion * speed
        position.set(
            fract(position.x - motion * sin(rotation)),
            fract(position.y - motion * cos(rotation))
        )

        transform.setLocalPosition(
            (position.x * 2.0 - 1.0) * spiderWorldScale, 1.0,
            (position.y * 2.0 - 1.0) * spiderWorldScale
        )
        transform.localRotation = transform.localRotation
            .rotationY(rotation)

        val spiderComp = getComponent(SpiderPrediction::class)!!
        // todo calculate position in future
        transform.getLocalTransform(dst = spiderComp.futureTransform)

        // todo teleport on edge transition
        // transform.smoothUpdate()
        invalidateAABB()
    }
}