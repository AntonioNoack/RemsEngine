package me.anno.ecs.components.collider

import me.anno.ecs.Component
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

// todo collision-effect mappings:
//  - which listener is used
//  - whether a collision happens
//  - whether a can push b
//  - whether b can push a

abstract class Collider : Component() {

    // todo aabb

    @SerializedProperty
    val position = Vector3d()

    abstract fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double

}