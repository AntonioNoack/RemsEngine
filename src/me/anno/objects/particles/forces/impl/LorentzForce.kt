package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.types.DirectionalForce
import me.anno.utils.Vectors.times
import org.joml.Vector3f

class LorentzForce: DirectionalForce("Lorentz Force", "Circular motion by velocity and rotation axis") {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val v = state.dPosition
        return v.cross(getDirection(time) * strength[time], Vector3f())
    }

    override fun getClassName() = "LorentzForce"

}