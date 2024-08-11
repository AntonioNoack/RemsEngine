package me.anno.tests.game.flatworld.streets

import org.joml.Vector3d

data class ReversibleSegment(val segment: StreetSegment, val reversed: Boolean) {
    val a get() = if (reversed) segment.c else segment.a

    fun interpolate(t: Double): Vector3d {
        val ti = if (reversed) 1.0 - t else t
        return segment.interpolate(ti)
    }
}