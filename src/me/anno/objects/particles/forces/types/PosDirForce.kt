package me.anno.objects.particles.forces.types

import me.anno.objects.InspectableAnimProperty

abstract class PosDirForce(displayName: String, description: String): DirectionalForce(displayName, description) {

    // val position = AnimatedProperty.vec3()

    override fun listProperties(): List<InspectableAnimProperty> {
        return super.listProperties() + listOf(InspectableAnimProperty(position, "Position"))
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