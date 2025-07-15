package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d

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

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        val tmp = Stack.newVec()
        val supVertexA = Stack.newVec()
        val supVertexB = Stack.newVec()

        dir.negate(tmp)
        transA.basis.transformTranspose(tmp)
        shapeA.localGetSupportingVertexWithoutMargin(tmp, supVertexA)
        transA.transform(supVertexA)

        transB.basis.transformTranspose(dir, tmp)
        shapeB.localGetSupportingVertexWithoutMargin(tmp, supVertexB)
        transB.transform(supVertexB)

        supVertexA.sub(supVertexB, out)
        Stack.subVec(3)
        return out
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.MINKOWSKI_SUM_SHAPE_PROXYTYPE

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        throw NotImplementedError()
    }

    override var margin: Double
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
