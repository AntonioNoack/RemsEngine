package me.anno.maths.geometry.convexhull

import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3d
import kotlin.math.max

/**
 * Contains resulting polygonal representation.
 */
class ConvexHull(val vertices: ArrayList<Vector3d>, val triangles: IntArray) {

    operator fun contains(vertex: Vector3d): Boolean {
        return contains(vertex, 0.0)
    }

    /**
     * Returns whether a point lies within the hull.
     * Margin makes this support a hull-sphere test.
     * */
    fun contains(vertex: Vector3d, margin: Double): Boolean {

        val vertices = vertices
        val triangles = triangles

        val normal = JomlPools.vec3d.create()
        val diff = JomlPools.vec3d.create()

        forLoopSafely(triangles.size, 3) { i ->

            val a = vertices[triangles[i]]
            val b = vertices[triangles[i + 1]]
            val c = vertices[triangles[i + 2]]

            // Compute face normal
            subCross(a, b, c, normal)

            // Vector from a vertex of the triangle to the point
            vertex.sub(a, diff)

            // Signed distance from point to the triangle's plane
            val distance = normal.dot(diff) / max(normal.length(), 1e-300)
            if (distance > margin) {

                // Point is outside the convex hull
                JomlPools.vec3d.sub(2)
                return false
            }
        }
        JomlPools.vec3d.sub(2)
        return true
    }
}