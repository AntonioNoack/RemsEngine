package me.anno.maths.geometry.convexhull

import org.joml.Vector3f

/**
 * Contains resulting plane representation (for fast tests);
 * more memory efficient (and slightly less precise) than ConvexHull
 */
class ConvexHullF(val planes: FloatArray) {

    /**
     * Returns whether a point lies within the hull.
     * */
    operator fun contains(vertex: Vector3f): Boolean {
        return contains(vertex.x, vertex.y, vertex.z)
    }

    /**
     * Returns whether a point lies within the hull.
     * Margin makes this a hull-sphere test.
     * */
    fun contains(vertex: Vector3f, margin: Float): Boolean {
        return contains(vertex.x, vertex.y, vertex.z, margin)
    }

    /**
     * Returns whether a bounds overlaps with the hull.
     * */
    fun contains(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
    ): Boolean {
        val planes = planes
        var i = planes.size
        var inside = true
        while (i > 0) {
            i -= 4
            val pdx = planes[i]
            val pdy = planes[i + 1]
            val pdz = planes[i + 2]
            val pdw = planes[i + 3]
            val x = if (pdx > 0f) minX else maxX
            val y = if (pdy > 0f) minY else maxY
            val z = if (pdz > 0f) minZ else maxZ
            // outside
            inside = inside and !(pdx * x + pdy * y + pdz * z + pdw > 0f) // allow NaN, just in case
        }
        return inside
    }

    /**
     * Returns whether a point lies within the hull.
     * Margin makes this a hull-sphere test.
     * */
    fun contains(
        x: Float, y: Float, z: Float,
        margin: Float = 0f,
    ): Boolean {
        val planes = planes
        var i = planes.size
        var inside = true
        while (i > 0) {
            i -= 4
            val pdx = planes[i]
            val pdy = planes[i + 1]
            val pdz = planes[i + 2]
            val pdw = planes[i + 3]
            // outside
            inside = inside and !(pdx * x + pdy * y + pdz * z + pdw > margin) // allow NaN, just in case
        }
        return inside
    }
}