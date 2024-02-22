package me.anno.maths.chunks.spherical

import org.joml.Vector3f

fun interface HexagonCreator {
    fun create(i: Long, center: Vector3f, corners: Array<Vector3f>): Hexagon

    object DefaultHexagonCreator : HexagonCreator {
        override fun create(i: Long, center: Vector3f, corners: Array<Vector3f>): Hexagon {
            return Hexagon(i, center, corners)
        }
    }
}