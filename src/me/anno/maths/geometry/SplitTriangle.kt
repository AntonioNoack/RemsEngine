package me.anno.maths.geometry

import me.anno.maths.Maths.clamp

object SplitTriangle {

    private fun safeSplit(d1: Float, d2: Float): Float {
        return clamp(d1 / (d1 - d2))
    }

    private fun sign(a: Float): Boolean {
        return a >= 0f
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

        if (sign(d1) == sign(d2)) {
            // if all signs are the same, don't split
            if (sign(d1) == sign(d3)) {
                return listOf(v1, v2, v3)
            }
            // AB does not cross the plane
            // -> rotate the points and try again
            return splitTriangle(v2, v3, v1, d2, d3, d1)
        }

        // intersection point
        val ab = v1.split(v2, safeSplit(d1, d2))

        return if (!sign(d1)) {
            if (!sign(d3)) {
                val bc = v2.split(v3, safeSplit(d2, d3))
                listOf(
                    v2, bc, ab,
                    bc, v3, v1,
                    ab, bc, v1
                )
            } else {
                val ac = v1.split(v3, safeSplit(d1, d3))
                listOf(
                    v1, ab, ac,
                    ab, v2, v3,
                    ac, ab, v3
                )
            }
        } else {
            if (!sign(d3)) {
                val ac = v1.split(v3, safeSplit(d1, d3))
                listOf(
                    v1, ab, ac,
                    ac, ab, v2,
                    v2, v3, ac
                )
            } else {
                val bc = v2.split(v3, safeSplit(d2, d3))
                listOf(
                    v2, bc, ab,
                    v1, ab, bc,
                    v3, v1, bc
                )
            }
        }
    }
}