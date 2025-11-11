package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import org.joml.Vector3d

// JAVA NOTE: ScaledBvhTriangleMeshShape from 2.73 SP1
/**
 * The ScaledBvhTriangleMeshShape allows to instance a scaled version of an existing
 * [BvhTriangleMeshShape]. Note that each [BvhTriangleMeshShape] still can
 * have its own local scaling, independent of ScaledBvhTriangleMeshShape 'localScaling'.
 *
 * @author jezek2
 */
class ScaledBvhTriangleMeshShape @Suppress("unused") constructor(
    var childShape: BvhTriangleMeshShape,
    localScaling: Vector3d
) : ConcaveShape() {

    val localScaling: Vector3d = Vector3d()

    init {
        this.localScaling.set(localScaling)
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val scaledCallback = ScaledTriangleCallback(callback, localScaling)

        val invLocalScaling = Stack.newVec()
        invLocalScaling.set(1.0 / localScaling.x, 1.0 / localScaling.y, 1.0 / localScaling.z)

        val scaledAabbMin = Stack.newVec()
        val scaledAabbMax = Stack.newVec()

        // support negative scaling
        scaledAabbMin.x = if (localScaling.x >= 0.0) aabbMin.x * invLocalScaling.x else aabbMax.x * invLocalScaling.x
        scaledAabbMin.y = if (localScaling.y >= 0.0) aabbMin.y * invLocalScaling.y else aabbMax.y * invLocalScaling.y
        scaledAabbMin.z = if (localScaling.z >= 0.0) aabbMin.z * invLocalScaling.z else aabbMax.z * invLocalScaling.z

        scaledAabbMax.x = if (localScaling.x <= 0.0) aabbMin.x * invLocalScaling.x else aabbMax.x * invLocalScaling.x
        scaledAabbMax.y = if (localScaling.y <= 0.0) aabbMin.y * invLocalScaling.y else aabbMax.y * invLocalScaling.y
        scaledAabbMax.z = if (localScaling.z <= 0.0) aabbMin.z * invLocalScaling.z else aabbMax.z * invLocalScaling.z

        childShape.processAllTriangles(scaledCallback, scaledAabbMin, scaledAabbMax)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val localAabbMin = childShape.getLocalAabbMin(Stack.newVec())
        val localAabbMax = childShape.getLocalAabbMax(Stack.newVec())

        val tmpLocalAabbMin = Stack.newVec()
        val tmpLocalAabbMax = Stack.newVec()
        mul(tmpLocalAabbMin, localAabbMin, localScaling)
        mul(tmpLocalAabbMax, localAabbMax, localScaling)

        localAabbMin.x = if (localScaling.x >= 0.0) tmpLocalAabbMin.x else tmpLocalAabbMax.x
        localAabbMin.y = if (localScaling.y >= 0.0) tmpLocalAabbMin.y else tmpLocalAabbMax.y
        localAabbMin.z = if (localScaling.z >= 0.0) tmpLocalAabbMin.z else tmpLocalAabbMax.z
        localAabbMax.x = if (localScaling.x <= 0.0) tmpLocalAabbMin.x else tmpLocalAabbMax.x
        localAabbMax.y = if (localScaling.y <= 0.0) tmpLocalAabbMin.y else tmpLocalAabbMax.y
        localAabbMax.z = if (localScaling.z <= 0.0) tmpLocalAabbMin.z else tmpLocalAabbMax.z

        AabbUtil.transformAabb(
            localAabbMin, localAabbMax, margin,
            t, aabbMin, aabbMax
        )
        Stack.subVec(4)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_SCALED_TRIANGLE_MESH

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        out.set(localScaling)
        return out
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        return childShape.calculateLocalInertia(mass, inertia).mul(localScaling) // correct?
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private class ScaledTriangleCallback(
        private val originalCallback: TriangleCallback,
        private val localScaling: Vector3d
    ) : TriangleCallback {
        private val newA = Vector3d()
        private val newB = Vector3d()
        private val newC = Vector3d()

        override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
            mul(newA, a, localScaling)
            mul(newB, b, localScaling)
            mul(newC, c, localScaling)
            originalCallback.processTriangle(newA, newB, newC, partId, triangleIndex)
        }
    }
}
