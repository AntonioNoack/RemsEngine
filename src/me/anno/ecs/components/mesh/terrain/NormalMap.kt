package me.anno.ecs.components.mesh.terrain

import org.joml.Vector3f

fun interface NormalMap {
    fun get(xi: Int, zi: Int, dst: Vector3f)
}