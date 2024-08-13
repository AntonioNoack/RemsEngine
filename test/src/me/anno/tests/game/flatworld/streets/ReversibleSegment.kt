package me.anno.tests.game.flatworld.streets

import org.joml.Vector3d

data class ReversibleSegment(val segment: StreetSegment, val reversed: Boolean) {
    val a: Vector3d get() = if (reversed) segment.c else segment.a
    val c: Vector3d get() = if (reversed) segment.a else segment.c
    val length: Double get() = segment.length

    fun interpolate(t: Double, dst: Vector3d = Vector3d()): Vector3d {
        val ti = if (reversed) 1.0 - t else t
        return segment.interpolate(ti, dst)
    }
}