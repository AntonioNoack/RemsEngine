package me.anno.objects.particles.forces.impl

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.inspectable.InspectableAnimProperty
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.ForceField
import me.anno.utils.Maths.pow
import me.anno.utils.Vectors.minus
import me.anno.utils.Vectors.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class BetweenParticleGravity : ForceField("Between-Particle Gravity", "Gravity towards all other particles") {

    // drawing this field probably is computationally too expensive
    // todo we need to draw this field, if there are only a few particles

    val exponent = AnimatedProperty.float(2f)

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        super.onDraw(stack, time, color)
        drawForcePerParticle(stack, time, color)
    }

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val sum = Vector3f()
        val position = state.position
        val exponent = -(exponent[time] + 1f)
        for (secondParticle in particles) {
            // particle is alive
            val secondState = secondParticle.states.lastOrNull() ?: continue
            if (secondState !== state) {
                val delta = secondState.position - position
                val l = delta.length()
                sum.add(delta * pow(l, exponent))
            }
        }
        return sum.mul(strength[time])
    }

    override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(
            InspectableAnimProperty(
                exponent,
                "Exponent",
                "How quickly the force declines with distance"
            )
        )
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "exponent", exponent)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "exponent" -> exponent.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun getClassName() = "MultiGravityForce"

}