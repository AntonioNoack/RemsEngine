package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.shapes.TriangleCallback
import org.joml.Vector3d

/**
 * @author jezek2
 */
internal class GImpactTriangleCallback : TriangleCallback {

    lateinit var algorithm: GImpactCollisionAlgorithm
    lateinit var body0: CollisionObject
    lateinit var body1: CollisionObject
    lateinit var shape: GImpactShapeInterface

    @JvmField
    var swapped: Boolean = false

    @JvmField
    var margin: Double = 0.0

    override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
        val tri1 = TriangleShapeEx(triangle[0], triangle[1], triangle[2])
        tri1.margin = margin
        val algorithm = algorithm
        if (swapped) {
            algorithm.part0 = partId
            algorithm.face0 = triangleIndex
        } else {
            algorithm.part1 = partId
            algorithm.face1 = triangleIndex
        }
        algorithm.gimpactVsShape(body0, body1, shape, tri1, swapped)
    }
}
