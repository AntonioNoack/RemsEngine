package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * StaticPlaneShape simulates an infinite non-moving (static) collision plane.
 *
 * @author jezek2
 */
class StaticPlaneShape(val planeNormal: Vector3f, var planeConstant: Double) : ConcaveShape() {

    init {
        planeNormal.normalize()
    }

    override fun getVolume(): Float = 1e308f

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        val tmp = Stack.newVec3d()
        val tmp1 = Stack.newVec3d()
        val tmp2 = Stack.newVec3d()

        val halfExtents = Stack.newVec3d()
        aabbMax.sub(aabbMin, halfExtents)
        halfExtents.mul(0.5)

        val radius = halfExtents.length()
        val center = Stack.newVec3d()
        aabbMax.add(aabbMin, center)
        center.mul(0.5)

        // this is where the triangles are generated, given AABB and plane equation (normal/constant)
        val tangentDir0 = Stack.newVec3f()
        val tangentDir1 = Stack.newVec3f()

        // tangentDir0/tangentDir1 can be precalculated
        planeNormal.findSystem(tangentDir0, tangentDir1, false)

        val projectedCenter = Stack.newVec3d()
        planeNormal.mul(planeNormal.dot(center) - planeConstant, tmp)
        center.sub(tmp, projectedCenter)

        val a = Stack.newVec3d()
        val b = Stack.newVec3d()
        val c = Stack.newVec3d()

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        VectorUtil.add(a, projectedCenter, tmp1, tmp2)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        VectorUtil.add(b, projectedCenter, tmp)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        projectedCenter.sub(tmp, c)

        callback.processTriangle(a, b, c, 0, 0)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.sub(tmp2, tmp)
        projectedCenter.sub(tmp, a)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        tmp1.add(tmp2, tmp)
        projectedCenter.sub(tmp, b)

        tangentDir0.mul(radius, tmp1)
        tangentDir1.mul(radius, tmp2)
        VectorUtil.add(c, projectedCenter, tmp1, tmp2)

        callback.processTriangle(a, b, c, 0, 1)
        Stack.subVec3d(9)
        Stack.subVec3f(2)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        aabbMin.set(-1e308, -1e308, -1e308)
        aabbMax.set(1e308, 1e308, 1e308)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.STATIC_PLANE

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // moving concave objects is not supported
        return inertia.set(0f)
    }
}
