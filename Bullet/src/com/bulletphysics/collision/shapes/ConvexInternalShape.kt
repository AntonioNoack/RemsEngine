package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * ConvexInternalShape is an internal base class, shared by most convex shape implementations.
 * collisionMargin is not scaled!
 *
 * @author jezek2
 */
abstract class ConvexInternalShape : ConvexShape() {

    val implicitShapeDimensions = Vector3f()

    /**
     * getAabb's default implementation is brute force, expected derived classes to implement a fast dedicated version.
     */
    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbSlow(t, aabbMin, aabbMax)
    }

    override fun getAabbSlow(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val margin = margin
        val dir = Stack.newVec3f()
        val globalDir = Stack.newVec3f()
        val globalPos = Stack.newVec3f()

        for (i in 0..2) {
            dir.set(0f)
            dir[i] = 1f

            dir.mulTranspose(t.basis, globalDir)
            localGetSupportingVertex(globalDir, globalPos)

            t.transformPosition(globalPos)

            aabbMax[i] = (globalPos[i] + margin).toDouble()
            dir[i] = -1f

            dir.mulTranspose(t.basis, globalDir)
            localGetSupportingVertex(globalDir, globalPos)
            t.transformPosition(globalPos)

            aabbMin[i] = (globalPos[i] - margin).toDouble()
        }
        Stack.subVec3f(3)
    }

    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        val supVertex = localGetSupportingVertexWithoutMargin(dir, out)
        if (margin != 0f) {
            /*val offset = margin / max(dir.length(), 1e-308)
            supVertex.fma(offset, dir)*/
            val vecNorm = Stack.newVec3f(dir)
            if (vecNorm.lengthSquared() < BulletGlobals.FLT_EPSILON_SQ) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.fma(margin, vecNorm)
            Stack.subVec3f(1)
        }
        return out
    }

    override var localScaling: Vector3f = Vector3f(1f)
        set(value) {
            field.set(value).absolute()
        }

    override val numPreferredPenetrationDirections: Int get() = 0

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f) {
        throw NotImplementedError()
    }
}
