package me.anno.ecs.components.chunks.spherical

import org.joml.Vector3f

fun interface HexagonCreator {

    fun create(i: Long, center: Vector3f, corners: Array<Vector3f>): Hexagon

    object DefaultHexagonCreator : HexagonCreator {
        override fun create(i: Long, center: Vector3f, corners: Array<Vector3f>): Hexagon {
            return Hexagon(i, center, corners)
        }
    }

    object PooledHexagonCreator : HexagonCreator {
        val pool = Hexagon.hexagonPool
        override fun create(i: Long, center: Vector3f, corners: Array<Vector3f>): Hexagon {
            val hex = pool.create()
            hex.index = i
            hex.center = center
            hex.corners = corners
            return hex
        }
    }
}