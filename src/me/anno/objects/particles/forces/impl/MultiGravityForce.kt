package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.ForceField
import me.anno.utils.Vectors.minus
import org.joml.Vector3f

class MultiGravityForce : ForceField("Between-Particle Gravity", "Gravity towards all other particles") {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val sum = Vector3f()
        val position = state.position
        for(secondParticle in particles){
            // particle is alive
            val secondState = secondParticle.states.lastOrNull() ?: continue
            if (secondState !== state) {
                val delta = secondState.position - position
                val l = delta.length()
                sum.add(delta / (l * l * l + 1e-30f))
            }
        }
        return sum.mul(strength[time])
    }

    override fun getClassName() = "MultiGravityForce"

}