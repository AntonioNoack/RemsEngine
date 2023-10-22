package me.anno.tests.physics.fluid

import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.maths.Maths.ceilDiv
import org.joml.Vector2f
import kotlin.math.sqrt

class FluidSimulation(val width: Int, val height: Int, numSurfaceParticles: Int) {

    val velocity = RWState { Framebuffer("velocity/$it", width, height, TargetType.FloatTarget2) }
    val divergence = Framebuffer("divergence", width, height, TargetType.FloatTarget1)
    val pressure = RWState { Framebuffer("pressure/$it", width, height, TargetType.FloatTarget1) }
    var fluidScaling = 0f
    var numPressureIterations = 20
    var dissipation = 0.2f // friction factor

    val particles = run {
        val w = sqrt(numSurfaceParticles.toFloat()).toInt()
        val h = ceilDiv(numSurfaceParticles, w)
        val targets = arrayOf(
            TargetType.FloatTarget3, // position,
            TargetType.FloatTarget3, // velocity,
            TargetType.FloatTarget3, // rotation (xyz, via order yxz)
            TargetType.FloatTarget4, // min-fluid-height, radius, mass, density
        )
        RWState { Framebuffer("particles/$it", w, h, targets) }
    }

    val texelSize = Vector2f(1f / width, 1f / height)

    fun step(dt: Float) {
        FluidSimulator.step(dt, this)
    }
}