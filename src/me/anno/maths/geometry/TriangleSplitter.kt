package me.anno.maths.geometry

import me.anno.maths.Maths.clamp
import me.anno.utils.types.Booleans.toInt
import kotlin.math.sign

object TriangleSplitter {

    private fun safeSplit(d1: Float, d2: Float): Float {
        return clamp(d1 / (d1 - d2))
    }

    /**
     * Clip a triangle against a plane knowing, that a to b crosses the clipping plane
     * Reference: Exact Buoyancy for Polyhedra by Erin Catto in Game Programming Gems 6 &
     * https://gamedevelopment.tutsplus.com/tutorials/how-to-dynamically-slice-a-convex-shape--gamedev-14479
     * */
    fun <Point : SplittablePoint<Point>> splitTriangle(
        v1: Point, v2: Point, v3: Point,
        d1: Float, d2: Float, d3: Float // plane.dot(tri.a/b/c.position)
    ): List<Point> {

        val s1 = sign(d1)
        val s2 = sign(d2)
        val s3 = sign(d3)

        val isNull = (s1 == 0f).toInt() + (s2 == 0f).toInt() + (s3 == 0f).toInt()
        if (isNull >= 2) return listOf(v1, v2, v3)

        if (s1 == s2 || (s1 == 0f || s2 == 0f)) {
            // if all signs are the same, don't split
            if (s1 == s3) {
                return listOf(v1, v2, v3)
            }
            // AB does not cross the plane
            // -> rotate the points and try again
            return splitTriangle(v2, v3, v1, d2, d3, d1)
        }

        // intersection point
        val ab = v1.split(v2, safeSplit(d1, d2))

        return if (s1 > 0f) {
            if (s3 > 0f) {
                val bc = v2.split(v3, safeSplit(d2, d3))
                listOf(
                    v2, bc, ab,
                    v1, ab, bc,
                    v3, v1, bc
                )
            } else {
                val ac = v1.split(v3, safeSplit(d1, d3))
                listOf(
                    v1, ab, ac,
                    ac, ab, v2,
                    v2, v3, ac
                )
            }
        } else {
            if (s3 > 0f) {
                val ac = v1.split(v3, safeSplit(d1, d3))
                listOf(
                    v1, ab, ac,
                    ab, v2, v3,
                    ac, ab, v3
                )
            } else {
                val bc = v2.split(v3, safeSplit(d2, d3))
                listOf(
                    v2, bc, ab,
                    bc, v3, v1,
                    ab, bc, v1
                )
            }
        }
    }
}