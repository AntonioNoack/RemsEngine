package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.ForceField
import me.anno.utils.Vectors.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class GlobalForce : ForceField("Global", "Constant Acceleration, e.g. Gravity on Earth") {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        return (getDirection(time) * strength[time]).mul(-1f)
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        drawForcePerParticle(stack, time, color)
    }

    override fun getClassName() = "GlobalForce"

}