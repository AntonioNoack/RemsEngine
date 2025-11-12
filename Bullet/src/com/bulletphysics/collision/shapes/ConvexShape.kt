package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * ConvexShape is an abstract shape class. It describes general convex shapes
 * using the [localGetSupportingVertex][.localGetSupportingVertex] interface
 * used in combination with GJK or ConvexCast.
 *
 * @author jezek2
 */
abstract class ConvexShape : CollisionShape() {

    abstract fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f
    abstract fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f

    //notice that the vectors should be unit length
    open fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3f>, outs: Array<Vector3f>, numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            localGetSupportingVertexWithoutMargin(dirs[i], outs[i])
        }
    }

    abstract fun getAabbSlow(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d)

    abstract val numPreferredPenetrationDirections: Int

    abstract fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f)
}
