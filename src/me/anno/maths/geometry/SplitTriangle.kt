package me.anno.maths.geometry

import me.anno.maths.Maths
import org.joml.Vector2f
import org.joml.Vector3f

object SplitTriangle {

    class Point3D(val pt: Vector3f, val normal: Vector3f, val uv: Vector2f) {
        fun mix(b: Point3D, factor: Float): Point3D = Point3D(
            Maths.mix(pt, b.pt, factor),
            Maths.mix(normal, b.normal, factor),
            Maths.mix(uv, b.uv, factor)
        )
    }

    class Triangle(var a: Point3D, var b: Point3D, var c: Point3D)

    /**
     * Clip a triangle against a plane knowing, that a to b crosses the clipping plane
     * Reference: Exact Buoyancy for Polyhedra by Erin Catto in Game Programming Gems 6 &
     * https://gamedevelopment.tutsplus.com/tutorials/how-to-dynamically-slice-a-convex-shape--gamedev-14479
     * */
    fun splitTriangle(
        tri: Triangle,
        d1: Float, d2: Float, d3: Float
    ): List<Triangle> {

        if ((d1 >= 0f && d2 >= 0f) || (d1 <= 0f && d2 <= 0f)) {
            // AB does not cross the plane
            // -> rotate the points and try again
            val tmp = tri.a
            tri.a = tri.b
            tri.b = tri.c
            tri.c = tmp
            return splitTriangle(tri, d2, d3, d1)
        }

        val a = tri.a
        val b = tri.b
        val c = tri.c

        // intersection point
        val ab = a.mix(b, d1 / (d1 - d2))

        return if (d1 < 0f) {
            if (d3 < 0f) {
                val bc = b.mix(c, d2 / (d2 - d3))
                listOf(
                    Triangle(b, bc, ab),
                    Triangle(bc, c, a),
                    Triangle(ab, bc, a)
                )
            } else {
                val ac = a.mix(c, d1 / (d1 - d3))
                listOf(
                    Triangle(a, ab, ac),
                    Triangle(ab, b, c),
                    Triangle(ac, ab, c)
                )
            }
        } else {
            if (d3 < 0f) {
                val ac = a.mix(c, d1 / (d1 - d3))
                listOf(
                    Triangle(a, ab, ac),
                    Triangle(ac, ab, b),
                    Triangle(b, c, ac)
                )
            } else {
                val bc = b.mix(c, d2 / (d2 - d3))
                listOf(
                    Triangle(b, bc, ab),
                    Triangle(a, ab, bc),
                    Triangle(c, a, bc)
                )
            }
        }
    }

}