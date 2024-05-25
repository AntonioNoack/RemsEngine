package me.anno.tests.physics.fluid

import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.maths.Maths.ceilDiv
import org.joml.Vector2f
import kotlin.math.sqrt

class FluidSimulation(val width: Int, val height: Int, numSurfaceParticles: Int) {

    val velocity = RWState<IFramebuffer> { Framebuffer("velocity/$it", width, height, TargetType.Float32x2) }
    val divergence = Framebuffer("divergence", width, height, TargetType.Float32x1)
    val pressure = RWState<IFramebuffer> { Framebuffer("pressure/$it", width, height, TargetType.Float32x1) }
    var fluidScaling = 0f
    var numPressureIterations = 20
    var dissipation = 0.2f // friction factor

    val particles = run {
        val w = sqrt(numSurfaceParticles.toFloat()).toInt()
        val h = ceilDiv(numSurfaceParticles, w)
        val targets = listOf(
            TargetType.Float32x3, // position,
            TargetType.Float32x3, // velocity,
            TargetType.Float32x3, // rotation (xyz, via order yxz)
            TargetType.Float32x4, // min-fluid-height, radius, mass, density
        )
        RWState { IFramebuffer.createFramebuffer("particles/$it", w, h, 1, targets, DepthBufferType.NONE) }
    }

    val texelSize = Vector2f(1f / width, 1f / height)

    fun step(dt: Float) {
        FluidSimulator.step(dt, this)
    }
}