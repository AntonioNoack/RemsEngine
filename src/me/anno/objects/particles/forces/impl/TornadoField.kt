package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.types.PosDirForce
import me.anno.utils.Vectors.minus
import me.anno.utils.Vectors.times
import org.joml.Vector3f

class TornadoField: PosDirForce("Tornado", "Circular motion around center") {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {

        val direction = getDirection(time)
        val strength = strength[time]
        val delta = state.position - position[time]

        return delta.cross(direction) * strength

    }

    override fun getClassName() = "TornadoField"

}