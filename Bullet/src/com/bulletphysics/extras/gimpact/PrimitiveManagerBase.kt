package com.bulletphysics.extras.gimpact

/**
 * Prototype Base class for primitive classification.
 *
 * This class is a wrapper for primitive collections.
 *
 * This tells relevant info for the Bounding Box set classes, which take care of space classification.
 *
 * This class can manage Compound shapes and trimeshes, and if it is managing trimesh then the
 * Hierarchy Bounding Box classes will take advantage of primitive Vs Box overlapping tests for
 * getting optimal results and less Per Box compairisons.
 *
 * @author jezek2
 */
interface PrimitiveManagerBase {
    /**
     * Determines if this manager consist on only triangles, which special case will be optimized.
     */
    val isTrimesh: Boolean

    val primitiveCount: Int

    fun getPrimitiveBox(primitiveIndex: Int, dst: AABB)

    /**
     * Retrieves only the points of the triangle, and the collision margin.
     */
    fun getPrimitiveTriangle(primIndex: Int, triangle: PrimitiveTriangle)
}
