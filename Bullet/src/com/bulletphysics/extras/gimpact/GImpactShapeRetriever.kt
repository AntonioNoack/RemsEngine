package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.CollisionShape

/**
 * @author jezek2
 */
internal class GImpactShapeRetriever(private val gimShape: GImpactShapeInterface) {

    private var triShape: TriangleShapeEx? = null
    private var tetraShape: TetrahedronShapeEx? = null

    init {
        // select helper
        if (gimShape.needsRetrieveTriangles()) {
            triShape = TriangleShapeEx()
        } else if (gimShape.needsRetrieveTetrahedrons()) {
            tetraShape = TetrahedronShapeEx()
        }
    }

    fun getChildShape(index: Int): CollisionShape? {
        val triShape = triShape
        val tetraShape = tetraShape
        return if (triShape != null) {
            gimShape.getBulletTriangle(index, triShape)
            triShape
        } else if (tetraShape != null) {
            gimShape.getBulletTetrahedron(index, tetraShape)
            tetraShape
        } else {
            gimShape.getChildShape(index)
        }
    }
}
