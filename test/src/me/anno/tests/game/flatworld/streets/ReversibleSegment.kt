package me.anno.tests.game.flatworld.streets

import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.max

data class ReversibleSegment(val segment: StreetSegment, val reversed: Boolean) {
    val a: Vector3d get() = if (reversed) segment.c else segment.a
    val c: Vector3d get() = if (reversed) segment.a else segment.c
    val length: Double get() = segment.length

    fun getDirectionAngle(t: Double): Double {
        val dt = 0.1 / max(1.0, length)
        val dv = interpolate(t + dt).sub(interpolate(t - dt))
        return atan2(dv.x, dv.z)
    }

    fun interpolate(t: Double, dst: Vector3d = Vector3d()): Vector3d {
        val ti = if (reversed) 1.0 - t else t
        return segment.interpolate(ti, dst)
    }
}