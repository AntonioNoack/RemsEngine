package me.anno.objects.particles.forces.types

import me.anno.objects.InspectableAnimProperty
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.objects.particles.forces.ForceField
import me.anno.utils.Vectors.minus
import me.anno.utils.Vectors.times
import org.joml.Vector3f

abstract class PositionedField(displayName: String, description: String): ForceField(displayName, description) {

    // val position = AnimatedProperty.vec3()

    abstract fun getForce(delta: Vector3f): Vector3f

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val position = state.position
        val center = this.position[time]
        return getForce(position - center) * strength[time]
    }

    override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(
            InspectableAnimProperty(position, "Position", "Center of the attracting mass")
        )
    }

    /*override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "position", position)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "position" -> position.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }*/

}