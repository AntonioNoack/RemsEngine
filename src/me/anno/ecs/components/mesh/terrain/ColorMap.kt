package me.anno.ecs.components.mesh.terrain

import org.joml.Vector3f

fun interface ColorMap {
    operator fun get(
        xi: Int, zi: Int, height: Float,
        normal: Vector3f
    ): Int
}