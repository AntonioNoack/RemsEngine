package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.BU_Simplex1to4
import org.joml.Vector3d

/**
 * Helper class for tetrahedrons.
 *
 * @author jezek2
 */
class TetrahedronShapeEx : BU_Simplex1to4() {
    init {
        repeat(4) {
            addVertex( Vector3d())
        }
    }

    @Suppress("unused")
    fun setVertices(v0: Vector3d, v1: Vector3d, v2: Vector3d, v3: Vector3d) {
        vertices[0]!!.set(v0)
        vertices[1]!!.set(v1)
        vertices[2]!!.set(v2)
        vertices[3]!!.set(v3)
        recalculateLocalAabb()
    }
}
