package me.anno.ecs.components.chunks.spherical

import me.anno.utils.pooling.RandomAccessPool
import org.joml.Vector3f

open class Hexagon(var index: Long, var center: Vector3f, var corners: Array<Vector3f>) {
    var neighbors = arrayOfNulls<Hexagon>(corners.size) // 5 or 6 entries; empty one will be the last one, and -1
    var neighborIds = LongArray(corners.size)

    init {
        neighborIds.fill(-1)
    }

    override fun hashCode() = index.hashCode()
    override fun equals(other: Any?) = (other === this) || (other is Hexagon && other.index == index)

    companion object {
        val hexagonPool = RandomAccessPool(8192) {
            Hexagon(0, Vector3f(), Array(6) { Vector3f() })
        }
    }
}