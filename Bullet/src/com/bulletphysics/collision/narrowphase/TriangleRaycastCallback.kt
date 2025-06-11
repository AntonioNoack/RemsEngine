package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setSub

/**
 * @author jezek2
 */
abstract class TriangleRaycastCallback(from: Vector3d, to: Vector3d) : TriangleCallback {

    val from: Vector3d = Vector3d()
    val to: Vector3d = Vector3d()

    var hitFraction: Double

    init {
        this.from.set(from)
        this.to.set(to)
        this.hitFraction = 1.0
    }

    override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
        val vert0 = triangle[0]
        val vert1 = triangle[1]
        val vert2 = triangle[2]

        val v10 = Stack.newVec()
        v10.setSub(vert1, vert0)

        val v20 = Stack.newVec()
        v20.setSub(vert2, vert0)

        val triangleNormal = Stack.newVec()
        triangleNormal.setCross(v10, v20)

        val dist = vert0.dot(triangleNormal)
        var distA = triangleNormal.dot(from)
        distA -= dist
        var distB = triangleNormal.dot(to)
        distB -= dist

        if (distA * distB >= 0.0) {
            return  // same sign
        }

        val projLength = distA - distB
        val distance = (distA) / (projLength)

        // Now we have the intersection point on the plane, we'll see if it's inside the triangle
        // Add an epsilon as a tolerance for the raycast,
        // in case the ray hits exacly on the edge of the triangle.
        // It must be scaled for the triangle size.
        if (distance < hitFraction) {
            var edge_tolerance = triangleNormal.lengthSquared()
            edge_tolerance *= -0.0001
            val point = Vector3d()
            setInterpolate3(point, from, to, distance)
            run {
                val v0p = Stack.newVec()
                v0p.setSub(vert0, point)
                val v1p = Stack.newVec()
                v1p.setSub(vert1, point)
                val cp0 = Stack.newVec()
                cp0.setCross(v0p, v1p)

                if (cp0.dot(triangleNormal) >= edge_tolerance) {
                    val v2p = Stack.newVec()
                    v2p.setSub(vert2, point)
                    val cp1 = Stack.newVec()
                    cp1.setCross(v1p, v2p)

                    if (cp1.dot(triangleNormal) >= edge_tolerance) {
                        cp1.setCross(v2p, v0p)
                        if (cp1.dot(triangleNormal) >= edge_tolerance) {
                            if (distA > 0.0) {
                                hitFraction = reportHit(triangleNormal, distance, partId, triangleIndex)
                            } else {
                                val tmp = Stack.newVec()
                                triangleNormal.negate(tmp)
                                hitFraction = reportHit(tmp, distance, partId, triangleIndex)
                                Stack.subVec(1)
                            }
                        }
                    }
                    Stack.subVec(2)
                }
                Stack.subVec(3)
            }
        }

        Stack.subVec(3)
    }

    abstract fun reportHit(hitNormalLocal: Vector3d, hitFraction: Double, partId: Int, triangleIndex: Int): Double
}
