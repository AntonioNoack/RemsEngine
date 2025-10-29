package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.TriangleCallback
import cz.advel.stack.Stack
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3d

/**
 * @author jezek2
 */
abstract class TriangleRaycastCallback(from: Vector3d, to: Vector3d) : TriangleCallback {

    val from = Vector3d(from)
    val to = Vector3d(to)

    var hitFraction = 1.0

    override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
        val triangleNormal = Stack.newVec()
        subCross(a, b, c, triangleNormal)

        val dist = a.dot(triangleNormal)
        val distA = triangleNormal.dot(from) - dist
        val distB = triangleNormal.dot(to) - dist

        if (distA * distB >= 0.0) {
            return // same sign
        }

        val distance = distA / (distA - distB)

        // Now we have the intersection point on the plane, we'll see if it's inside the triangle
        // Add an epsilon as a tolerance for the raycast,
        // in case the ray hits exactly on the edge of the triangle.
        // It must be scaled for the triangle size.
        if (distance < hitFraction) {
            val edgeTolerance = -triangleNormal.lengthSquared() * 0.0001
            val point = Vector3d()
            from.lerp(to, distance, point)

            val v0p = a.sub(point, Stack.newVec())
            val v1p = b.sub(point, Stack.newVec())
            val cp01 = v0p.cross(v1p, Stack.newVec())

            if (cp01.dot(triangleNormal) >= edgeTolerance) {
                val v2p = c.sub(point, Stack.newVec())
                val cp12 = v1p.cross(v2p, Stack.newVec())

                if (cp12.dot(triangleNormal) >= edgeTolerance) {
                    v2p.cross(v0p, cp12)
                    if (cp12.dot(triangleNormal) >= edgeTolerance) {
                        if (distA <= 0.0) triangleNormal.negate()
                        hitFraction = reportHit(triangleNormal, distance, partId, triangleIndex)
                    }
                }
                Stack.subVec(2)
            }
            Stack.subVec(3)
        }
        Stack.subVec(1) // triangleNormal
    }

    abstract fun reportHit(hitNormalLocal: Vector3d, hitFraction: Double, partId: Int, triangleIndex: Int): Double
}
