package me.anno.ecs.components.chunks.spherical

import org.joml.Vector3f

fun interface HexagonCreator {
    fun create(i: Int, center: Vector3f, corners: Array<Vector3f>): Hexagon
}
