package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.TransformUtil
import com.bulletphysics.linearmath.VectorUtil
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
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

    fun getPlaneNormal(out: Vector3d): Vector3d {
        out.set(planeNormal)
        return out
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val tmp = Stack.newVec()
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        val halfExtents = Stack.newVec()
        halfExtents.setSub(aabbMax, aabbMin)
        halfExtents.mul(0.5)

        val radius = halfExtents.length()
        val center = Stack.newVec()
        center.setAdd(aabbMax, aabbMin)
        center.mul(0.5)

        // this is where the triangles are generated, given AABB and plane equation (normal/constant)
        val tangentDir0 = Stack.newVec()
        val tangentDir1 = Stack.newVec()

        // tangentDir0/tangentDir1 can be precalculated
        TransformUtil.planeSpace1(planeNormal, tangentDir0, tangentDir1)

        val projectedCenter = Stack.newVec()
        tmp.setScale(planeNormal.dot(center) - planeConstant, planeNormal)
        projectedCenter.setSub(center, tmp)

        val triangle: Array<Vector3d> = arrayOf(Stack.newVec(), Stack.newVec(), Stack.newVec())

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        VectorUtil.add(triangle[0], projectedCenter, tmp1, tmp2)

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        tmp.setSub(tmp1, tmp2)
        VectorUtil.add(triangle[1], projectedCenter, tmp)

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        tmp.setSub(tmp1, tmp2)
        triangle[2].setSub(projectedCenter, tmp)

        callback.processTriangle(triangle, 0, 0)

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        tmp.setSub(tmp1, tmp2)
        triangle[0].setSub(projectedCenter, tmp)

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        tmp.setAdd(tmp1, tmp2)
        triangle[1].setSub(projectedCenter, tmp)

        tmp1.setScale(radius, tangentDir0)
        tmp2.setScale(radius, tangentDir1)
        VectorUtil.add(triangle[2], projectedCenter, tmp1, tmp2)

        callback.processTriangle(triangle, 0, 1)

        Stack.subVec(11)
    }

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
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

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        //moving concave objects not supported
        inertia.set(0.0, 0.0, 0.0)
    }
}
