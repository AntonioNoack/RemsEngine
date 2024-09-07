package me.anno.tests.game.flatworld.streets

import org.joml.Vector3d

data class ReversibleSegment(val segment: StreetSegment, val reversed: Boolean) {
    val a: Vector3d get() = if (reversed) segment.c else segment.a
    val c: Vector3d get() = if (reversed) segment.a else segment.c
    val b: Vector3d? get() = segment.b
    val length: Double get() = segment.length

    fun interpolate(t: Double, dst: Vector3d = Vector3d()): Vector3d {
        val ti = if (reversed) 1.0 - t else t
        return segment.interpolate(ti, dst)
    }

    fun interpolateDx(t: Double, dx: Double, dst: Vector3d = Vector3d()): Vector3d {
        val ti = if (reversed) 1.0 - t else t
        return segment.interpolateDx(ti, if (reversed) -dx else dx, dst)
    }

    fun splitSegmentDx(t0: Double, t1: Double, dx: Double): StreetSegment {
        if (reversed) {
            val base = segment.splitSegmentDx(1.0 - t1, 1.0 - t0, -dx)
            return StreetSegment(base.c, base.b, base.a)
        } else {
            return segment.splitSegmentDx(t0, t1, dx)
        }
    }
}