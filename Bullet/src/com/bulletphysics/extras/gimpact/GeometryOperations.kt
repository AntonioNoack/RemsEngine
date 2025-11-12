package com.bulletphysics.extras.gimpact

import cz.advel.stack.Stack
import org.joml.Vector4d
import org.joml.Vector3d

/**
 * @author jezek2
 */
internal object GeometryOperations {
    /**
     * Calc a plane from a triangle edge and a normal.
     */
    @JvmStatic
    fun edgePlane(e1: Vector3d, e2: Vector3d, normal: Vector3d, plane: Vector4d) {
        val n = Stack.newVec3d()
        e2.sub(e1, n).cross(normal).normalize()
        plane.set(n.x, n.y, n.z, e2.dot(n))
        Stack.subVec3d(1)
    }
}
