package me.anno.ecs.components.chunks.spherical

import org.joml.Vector3f

// todo: planar hexagon grid, can be very useful, too, and chunks may be interesting as well
// todo: planar triangle grid
open class Hexagon(val index: Long, val center: Vector3f, val corners: Array<Vector3f>) {
    val neighbors = arrayOfNulls<Hexagon>(corners.size) // 5 or 6 entries; empty one will be the last one, and -1
    val neighborIds = LongArray(corners.size) { -1 }
    override fun hashCode() = index.hashCode()
    override fun equals(other: Any?) = (other === this) || (other is Hexagon && other.index == index)
}