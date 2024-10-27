package me.anno.maths.geometry

import org.joml.Vector2f

object Polygons {
    // surely, we have this implemented already somewhere, find it
    // -> only an ugly version in EarCut operating on FloatArray
    fun getPolygonArea(points: List<Vector2f>): Float {
        var sum = 0f
        for (j in points.indices) {
            val i = if (j == 0) points.lastIndex else j - 1
            val ni = points[i]
            val nj = points[j]
            sum += ni.cross(nj)
        }
        return 0.5f * sum
    }
}