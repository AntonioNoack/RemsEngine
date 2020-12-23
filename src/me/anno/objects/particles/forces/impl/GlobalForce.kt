package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.types.DirectionalForce
import me.anno.utils.Vectors.times
import org.joml.Vector3f

class GlobalForce: DirectionalForce("Global", "Constant Acceleration, e.g. Gravity on Earth") {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        return getDirection(time) * strength[time]
    }

    override fun getClassName() = "GlobalForce"

}