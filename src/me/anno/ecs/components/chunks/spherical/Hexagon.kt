package me.anno.ecs.components.chunks.spherical

import org.joml.Vector3f

open class Hexagon(val index: Int, val center: Vector3f, val corners: Array<Vector3f>) {
    val neighborIds = IntArray(6) // 5 or 6 entries; empty one will be the last one, and -1

    init {
        neighborIds.fill(-1)
    }
}