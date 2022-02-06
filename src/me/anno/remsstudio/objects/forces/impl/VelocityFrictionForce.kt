package me.anno.remsstudio.objects.forces.impl

import me.anno.remsstudio.objects.particles.Particle
import me.anno.remsstudio.objects.particles.ParticleState
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f

class VelocityFrictionForce : PerParticleForce(
    "Velocity Friction",
    "The faster a particle is, the more friction it will experience", "velocityFriction"
) {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        return (state.dPosition * strength[time]).mul(-1f)
    }

    override val className get() = "VelocityFrictionForce"

}