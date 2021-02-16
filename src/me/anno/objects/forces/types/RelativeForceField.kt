package me.anno.objects.forces.types

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.forces.ForceField
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f
import org.joml.Vector3fc

abstract class RelativeForceField(displayName: String, description: String, dictSubPath: String):
    ForceField(displayName, description, dictSubPath) {

    abstract fun getForce(delta: Vector3fc, time: Double): Vector3fc

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val position = state.position
        val center = this.position[time]
        return getForce(position - center, time) * strength[time]
    }

}