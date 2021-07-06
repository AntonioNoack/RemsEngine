package me.anno.ecs.components.collider

import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class SphereCollider : Collider() {

    @SerializedProperty
    var radius = 1.0

    override val className get() = "SphereCollider"

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        return deltaPosition.length() - radius
    }

}