package me.anno.objects.particles.forces.types

import me.anno.objects.particles.forces.ForceField
import org.joml.Quaternionf
import org.joml.Vector3f

abstract class DirectionalForce(displayName: String, description: String): ForceField(displayName, description) {

    // val direction = AnimatedProperty.dir3()

    fun getDirection(time: Double): Vector3f {
        val rot = rotationYXZ[time]
        val quat = Quaternionf()
        quat.rotateY(rot.x)
        quat.rotateX(rot.y)
        quat.rotateZ(rot.z)
        return quat.transform(Vector3f(0f,1f,0f))
    }

    /*override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(
            InspectableAnimProperty(direction, "Direction", "Axis of the rotation by velocity")
        )
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "direction", direction)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "direction" -> direction.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }*/

    // todo draw an arrow on the y-axis for our direction

}