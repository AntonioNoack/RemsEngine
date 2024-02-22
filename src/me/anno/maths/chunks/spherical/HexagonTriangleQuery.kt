package me.anno.maths.chunks.spherical

import org.joml.Vector3f

interface HexagonTriangleQuery {
    fun run(
        hex1: Hexagon, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    )

    fun run(
        hex1: Hexagon, hex2: Hexagon, i: Int, minY: Float, maxY: Float,
        callback: (Vector3f, Vector3f, Vector3f) -> Boolean
    )
}