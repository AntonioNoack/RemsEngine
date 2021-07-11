package me.anno.ecs.components.physics

import me.anno.ecs.Component
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import kotlin.math.abs

class Rigidbody : Component() {

    @SerializedProperty
    var mass = 1.0

    var centerOfMass = Vector3d()

    @NotSerializedProperty
    var isStatic
        get() = mass <= 0.0
        set(value) {
            if(value){
                // static: the negative value or null
                mass = -abs(mass)
            } else {
                // non-static: positive value
                mass = abs(mass)
                if(mass < 1e-16) mass = 1.0
            }
        }

    fun updatePhysics(){
        // todo remove old physics
        // todo create new bullet body

    }

    override val className get() = "Rigidbody"

}