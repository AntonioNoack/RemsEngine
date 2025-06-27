package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.MatrixUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import com.bulletphysics.util.setScaleAdd
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * ConvexInternalShape is an internal base class, shared by most convex shape implementations.
 *
 * @author jezek2
 */
abstract class ConvexInternalShape : ConvexShape() {

    // local scaling. collisionMargin is not scaled !
    val localScaling: Vector3d = Vector3d(1.0, 1.0, 1.0)
    val implicitShapeDimensions: Vector3d = Vector3d()

    /**
     * getAabb's default implementation is brute force, expected derived classes to implement a fast dedicated version.
     */
    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbSlow(t, aabbMin, aabbMax)
    }

    override fun getAabbSlow(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val margin = margin
        val vec = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        for (i in 0..2) {
            vec.set(0.0, 0.0, 0.0)
            setCoord(vec, i, 1.0)

            MatrixUtil.transposeTransform(tmp1, vec, t.basis)
            localGetSupportingVertex(tmp1, tmp2)

            t.transform(tmp2)

            setCoord(aabbMax, i, getCoord(tmp2, i) + margin)

            setCoord(vec, i, -1.0)

            MatrixUtil.transposeTransform(tmp1, vec, t.basis)
            localGetSupportingVertex(tmp1, tmp2)
            t.transform(tmp2)

            setCoord(aabbMin, i, getCoord(tmp2, i) - margin)
        }
        Stack.subVec(3)
    }

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        val supVertex = localGetSupportingVertexWithoutMargin(dir, out)
        if (margin != 0.0) {
            /*val offset = margin / max(dir.length(), 1e-308)
            supVertex.fma(offset, dir)*/
            val vecNorm = Stack.newVec(dir)
            if (vecNorm.lengthSquared() < (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.setScaleAdd(margin, vecNorm, supVertex)
            Stack.subVec(1)
        }
        return out
    }

    override fun setLocalScaling(scaling: Vector3d) {
        scaling.absolute(localScaling)
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        out.set(localScaling)
        return out
    }

    override val numPreferredPenetrationDirections: Int get() = 0

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3d) {
        throw NotImplementedError()
    }
}
