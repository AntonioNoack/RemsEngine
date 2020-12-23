package me.anno.objects.particles.forces.impl

import me.anno.objects.particles.forces.types.PositionedField
import org.joml.Vector3f

class GravityField : PositionedField("Central Gravity", "Gravity towards a single point") {

    override fun getForce(delta: Vector3f): Vector3f {
        val l = delta.length()
        return delta / (l * l * l + 1e-16f)
    }

    override fun getClassName() = "GravityField"

}