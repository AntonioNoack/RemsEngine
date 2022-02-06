package me.anno.remsstudio.objects.forces.impl

import me.anno.remsstudio.objects.particles.Particle
import me.anno.remsstudio.objects.particles.ParticleState
import me.anno.remsstudio.objects.forces.ForceField
import me.anno.utils.types.Vectors.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4fc

class GlobalForce : ForceField(
    "Global Force",
    "Constant Acceleration, e.g. Gravity on Earth", "global"
) {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        return (getDirection(time) * strength[time]).mul(-1f)
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        drawForcePerParticle(stack, time, color)
    }

    override val className get() = "GlobalForce"

}