package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

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
    localScaling: Vector3f
) : ConcaveShape() {

    init {
        this.localScaling = localScaling
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val scaledCallback = ScaledTriangleCallback(callback, localScaling)

        val invLocalScaling = Stack.newVec3d()
        invLocalScaling.set(1.0 / localScaling.x, 1.0 / localScaling.y, 1.0 / localScaling.z)

        val scaledAabbMin = Stack.newVec3d()
        val scaledAabbMax = Stack.newVec3d()

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
        val localAabbMin = Stack.newVec3f(childShape.localAabbMin)
        val localAabbMax = Stack.newVec3f(childShape.localAabbMax)

        val tmpLocalAabbMin = Stack.newVec3f()
        val tmpLocalAabbMax = Stack.newVec3f()
        localAabbMin.mul(localScaling, tmpLocalAabbMin)
        localAabbMax.mul(localScaling, tmpLocalAabbMax)

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
        Stack.subVec3f(4)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_SCALED_TRIANGLE_MESH

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        return childShape.calculateLocalInertia(mass, inertia).mul(localScaling) // correct?
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private class ScaledTriangleCallback(
        private val originalCallback: TriangleCallback,
        private val localScaling: Vector3f
    ) : TriangleCallback {
        private val newA = Vector3d()
        private val newB = Vector3d()
        private val newC = Vector3d()

        override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
            a.mul(localScaling, newA)
            b.mul(localScaling, newB)
            c.mul(localScaling, newC)
            originalCallback.processTriangle(newA, newB, newC, partId, triangleIndex)
        }
    }
}
