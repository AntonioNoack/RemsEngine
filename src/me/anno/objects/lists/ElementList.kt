package me.anno.objects.lists

import me.anno.io.Saveable
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.distributions.AnimatedDistribution
import me.anno.objects.particles.Particle
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.util.*
import kotlin.collections.ArrayList

// todo make it flexible enough, so it can be used for the particle system
// todo to make everything very flexible <3
abstract class ElementList : Saveable() {

    val spawnRate = AnimatedProperty.floatPlus()
    val lifeTime = AnimatedDistribution(Type.FLOAT_PLUS, 10f)
    var seed = 1234L

    private var random = Random(seed)
    private val generated = ArrayList<Particle>()
    private var lastState: Any? = Triple(spawnRate.toString(), lifeTime.toString(), seed)

    private fun getElements(
        time: Double,
        generator: ElementGenerator
    ): List<Particle> {
        val state = getState()
        if (state != lastState) {
            lastState = state
            random = Random(seed)
            generated.clear()
        }
        var lastTime = generated.lastOrNull()?.birthTime ?: 0.0
        if (time > lastTime) {
            val timeSinceThen = spawnRate.getIntegral<Float>(lastTime, time, false)
            val timeSinceThen1 = timeSinceThen.toInt()
            if (timeSinceThen1 > 0) {
                val index0 = generated.size
                for (i in 0 until timeSinceThen1) {
                    val index = index0 + i
                    val spawnTime = spawnRate.findIntegralX<Float>(lastTime, time)
                    val component = generator.generateEntry(index) ?: return generated
                    val lifeTime = lifeTime.nextV1(time, random).toDouble()
                    val particle = generator.generateParticle(component, index, time, lifeTime)
                    generated += particle
                    lastTime = spawnTime
                }
            }
        }
        return generated
    }

    abstract fun drawElements(
        stack: Matrix4fArrayList,
        time: Double,
        color: Vector4f,
        elements: List<Particle>
    )

    abstract fun getState(): Any?

    fun onDraw(
        stack: Matrix4fArrayList, time: Double, color: Vector4f,
        generator: ElementGenerator,
        superOnDraw: () -> Unit
    ) {
        val children = getElements(time, generator)
        if (children.isNotEmpty()) {
            drawElements(stack, time, color, children)
        } else superOnDraw()
    }

}