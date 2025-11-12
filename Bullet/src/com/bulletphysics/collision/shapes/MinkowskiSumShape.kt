package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * MinkowskiSumShape is only for advanced users. This shape represents implicit
 * based minkowski sum of two convex implicit shapes.
 *
 * @author jezek2
 */
class MinkowskiSumShape @Suppress("unused") constructor(
    private val shapeA: ConvexShape,
    private val shapeB: ConvexShape
) : ConvexInternalShape() {

    private val transA = Transform()
    private val transB = Transform()

    init {
        this.transA.setIdentity()
        this.transB.setIdentity()
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        val tmp = Stack.newVec3f()
        val supVertexA = Stack.newVec3f()
        val supVertexB = Stack.newVec3f()

        dir.negate(tmp)
        transA.basis.transformTranspose(tmp)
        shapeA.localGetSupportingVertexWithoutMargin(tmp, supVertexA)
        transA.transformPosition(supVertexA)

        transB.basis.transformTranspose(dir, tmp)
        shapeB.localGetSupportingVertexWithoutMargin(tmp, supVertexB)
        transB.transformPosition(supVertexB)

        supVertexA.sub(supVertexB, out)
        Stack.subVec3f(3)
        return out
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.MINKOWSKI_SUM

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        throw NotImplementedError()
    }

    override var margin: Float
        get() = shapeA.margin + shapeB.margin

    @Suppress("unused")
    fun setTransformA(transA: Transform) {
        this.transA.set(transA)
    }

    @Suppress("unused")
    fun setTransformB(transB: Transform) {
        this.transB.set(transB)
    }

    @Suppress("unused")
    fun getTransformA(dst: Transform) {
        dst.set(transA)
    }

    @Suppress("unused")
    fun getTransformB(dst: Transform) {
        dst.set(transB)
    }
}
