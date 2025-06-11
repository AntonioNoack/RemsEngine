package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.CollisionShape

/**
 * @author jezek2
 */
internal class GIMShapeRetriever(private val gimShape: GImpactShapeInterface) {

    private var triShape: TriangleShapeEx? = null
    private var tetraShape: TetrahedronShapeEx? = null

    init {
        // select retriever
        if (gimShape.needsRetrieveTriangles()) {
            triShape = TriangleShapeEx()
        } else if (gimShape.needsRetrieveTetrahedrons()) {
            tetraShape = TetrahedronShapeEx()
        }
    }

    fun getChildShape(index: Int): CollisionShape {
        val triShape = triShape
        val tetraShape = tetraShape
        if (triShape != null) {
            gimShape.getBulletTriangle(index, triShape)
            return triShape
        } else if (tetraShape != null) {
            gimShape.getBulletTetrahedron(index, tetraShape)
            return tetraShape
        } else {
            return gimShape.getChildShape(index)
        }
    }
}
