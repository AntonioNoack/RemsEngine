package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import com.bulletphysics.linearmath.VectorUtil
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * StaticPlaneShape simulates an infinite non-moving (static) collision plane.
 *
 * @author jezek2
 */
class StaticPlaneShape(val planeNormal: Vector3d, var planeConstant: Double) : ConcaveShape() {

    init {
        planeNormal.normalize()
    }

    val localScaling = Vector3d(0.0, 0.0, 0.0)

    override fun getVolume(): Double = 1e308

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        val halfExtents = Stack.newVec()
        aabbMax.sub(aabbMin, halfExtents)
        halfExtents.mul(0.5)

        val radius = halfExtents.length()
        val center = Stack.newVec()
        aabbMax.add(aabbMin, center)
        center.mul(0.5)

        // this is where the triangles are generated, given AABB and plane equation (normal/constant)
        val tangentDir0 = Stack.newVec()
        val tangentDir1 = Stack.newVec()

        // tangentDir0/tangentDir1 can be precalculated
        planeNormal.findSystem(tangentDir0, tangentDir1, false)

        val projectedCenter = Stack.newVec()
        planeNormal.mul(planeNormal.dot(center) - planeConstant, tmp)
        center.sub(tmp, projectedCenter)

        val triangle: Array<Vector3d> = arrayOf(Stack.newVec(), Stack.newVec(), Stack.newVec())

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        VectorUtil.add(triangle[0], projectedCenter, tmp1, tmp2)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        VectorUtil.add(triangle[1], projectedCenter, tmp)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        projectedCenter.sub(tmp, triangle[2])

        callback.processTriangle(triangle, 0, 0)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        projectedCenter.sub(tmp, triangle[0])

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.add(tmp2, tmp)
        projectedCenter.sub(tmp, triangle[1])

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        VectorUtil.add(triangle[2], projectedCenter, tmp1, tmp2)

        callback.processTriangle(triangle, 0, 1)

        Stack.subVec(11)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        aabbMin.set(-1e308, -1e308, -1e308)
        aabbMax.set(1e308, 1e308, 1e308)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.STATIC_PLANE_PROXYTYPE

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        out.set(localScaling)
        return out
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        // moving concave objects is not supported
        return inertia.set(0.0)
    }
}
