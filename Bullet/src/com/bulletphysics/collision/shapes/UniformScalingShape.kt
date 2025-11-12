package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * UniformScalingShape allows to re-use uniform scaled instances of [ConvexShape]
 * in a memory efficient way. Instead of using [UniformScalingShape], it is better
 * to use the non-uniform setLocalScaling method on convex shapes that implement it.
 *
 * @author jezek2
 */
class UniformScalingShape(val childShape: ConvexShape, val uniformScalingFactor: Float) : ConvexShape() {
    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        childShape.localGetSupportingVertex(dir, out)
        out.mul(uniformScalingFactor)
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        childShape.localGetSupportingVertexWithoutMargin(dir, out)
        out.mul(uniformScalingFactor)
        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3f>, outs: Array<Vector3f>, numVectors: Int
    ) {
        childShape.batchedUnitVectorGetSupportingVertexWithoutMargin(dirs, outs, numVectors)
        for (i in 0 until numVectors) {
            outs[i].mul(uniformScalingFactor)
        }
    }

    override fun getAabbSlow(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        childShape.getAabbSlow(t, aabbMin, aabbMax)
        val aabbCenter = Stack.newVec3d()
        aabbMax.add(aabbMin, aabbCenter)
            .mul(0.5)

        val scaledAabbHalfExtents = Stack.newVec3d()
        aabbMax.sub(aabbMin, scaledAabbHalfExtents)
            .mul(0.5 * uniformScalingFactor)

        aabbCenter.sub(scaledAabbHalfExtents, aabbMin)
        aabbCenter.add(scaledAabbHalfExtents, aabbMax)
    }

    override var localScaling: Vector3f
        get() = childShape.localScaling
        set(value) {
            childShape.localScaling = value
        }

    override var margin: Float
        get() = childShape.margin * uniformScalingFactor
        set(margin) {
            childShape.margin = margin
        }

    override val numPreferredPenetrationDirections: Int
        get() = childShape.numPreferredPenetrationDirections

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f) {
        childShape.getPreferredPenetrationDirection(index, penetrationVector)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        childShape.getBounds(t, aabbMin, aabbMax)

        val aabbCenter = Stack.newVec3d()
        aabbMax.add(aabbMin, aabbCenter)
            .mul(0.5)

        val scaledAabbHalfExtents = Stack.newVec3d()
        aabbMax.sub(aabbMin, scaledAabbHalfExtents)
            .mul(0.5 * uniformScalingFactor)

        aabbCenter.sub(scaledAabbHalfExtents, aabbMin)
        aabbCenter.add(scaledAabbHalfExtents, aabbMax)
        Stack.subVec3d(2)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.UNIFORM_SCALING

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // this linear upscaling is not realistic, but we don't deal with large mass ratios...
        return childShape.calculateLocalInertia(mass, inertia).mul(uniformScalingFactor)
    }
}
