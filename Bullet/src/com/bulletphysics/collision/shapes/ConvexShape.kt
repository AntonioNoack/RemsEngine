package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * ConvexShape is an abstract shape class. It describes general convex shapes
 * using the [localGetSupportingVertex][.localGetSupportingVertex] interface
 * used in combination with GJK or ConvexCast.
 *
 * @author jezek2
 */
abstract class ConvexShape : CollisionShape() {

    abstract fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d
    abstract fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d

    //notice that the vectors should be unit length
    open fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            localGetSupportingVertexWithoutMargin(dirs[i], outs[i])
        }
    }

    abstract fun getAabbSlow(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d)

    abstract override fun setLocalScaling(scaling: Vector3d)

    abstract override fun getLocalScaling(out: Vector3d): Vector3d

    abstract val numPreferredPenetrationDirections: Int

    abstract fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3d)

    companion object {
        const val MAX_PREFERRED_PENETRATION_DIRECTIONS: Int = 10
    }
}
