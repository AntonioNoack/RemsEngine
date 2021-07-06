package me.anno.ecs.components.collider

import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

// todo uses a triangle mesh
class MeshCollider : Collider() {

    @SerializedProperty
    var isConvex = true

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        TODO("Not yet implemented")
    }

    override val className get() = "MeshCollider"

}