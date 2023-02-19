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
            val hex = pool.get()
            hex.index = i
            hex.center.set(center)
            hex.corners = corners
            if (corners.size != hex.neighborIds.size) {
                hex.neighborIds = LongArray(corners.size)
                hex.neighborIds.fill(-1)
                hex.neighbors = arrayOfNulls(corners.size)
            }
            return hex
        }
    }
}