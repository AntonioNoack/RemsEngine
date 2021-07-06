package me.anno.objects.forces.impl

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.inspectable.InspectableAnimProperty
import me.anno.animation.AnimatedProperty
import me.anno.objects.forces.types.RelativeForceField
import me.anno.utils.Maths.pow
import me.anno.utils.types.Vectors.times
import org.joml.Vector3fc

class GravityField : RelativeForceField(
    "Central Gravity",
    "Gravity towards a single point", "centralGravity"
) {

    val exponent = AnimatedProperty.float(2f)

    override fun getForce(delta: Vector3fc, time: Double): Vector3fc {
        val l = delta.length()
        return delta * (-pow(l, -(exponent[time] + 1f)) + 1e-16f)
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
        writer.writeObject(this,"exponent", exponent)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "exponent" -> exponent.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override val className get() = "GravityField"

}