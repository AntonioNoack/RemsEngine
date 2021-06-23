package me.anno.ecs.components.collider

import me.anno.ecs.Component
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

/**
 * uses a point cloud for collisions
 * */
class ConvexCollider: Component() {

    @SerializedProperty
    val points = ArrayList<Vector3d>()

    override fun getClassName(): String = "ConvexCollider"

}