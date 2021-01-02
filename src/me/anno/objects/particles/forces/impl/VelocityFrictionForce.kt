package me.anno.objects.particles.forces.impl

import me.anno.language.translation.Dict
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.utils.Vectors.times
import org.joml.Vector3f

class VelocityFrictionForce : PerParticleForce(
    Dict["Velocity Friction", "obj.force.velocityFriction"],
    Dict["The faster a particle is, the more friction it will experience", "org.force.velocityFriction.desc"]
) {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        return (state.dPosition * strength[time]).mul(-1f)
    }

    override fun getClassName() = "VelocityFrictionForce"

}