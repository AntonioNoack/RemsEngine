package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.maxAxis
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setSub
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * Single triangle shape.
 *
 * @author jezek2
 */
open class TriangleShape : PolyhedralConvexShape {
    val vertices = arrayOf(Vector3d(), Vector3d(), Vector3d())

    // JAVA NOTE: added
    constructor()

    constructor(p0: Vector3d, p1: Vector3d, p2: Vector3d) {
        vertices[0].set(p0)
        vertices[1].set(p1)
        vertices[2].set(p2)
    }

    // JAVA NOTE: added
    fun init(p0: Vector3d, p1: Vector3d, p2: Vector3d) {
        vertices[0].set(p0)
        vertices[1].set(p1)
        vertices[2].set(p2)
    }

    override val numVertices
        get(): Int {
            return 3
        }

    override fun getVertex(i: Int, vtx: Vector3d) {
        vtx.set(vertices[i])
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.TRIANGLE_SHAPE_PROXYTYPE

    override val numEdges
        get(): Int {
            return 3
        }

    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
        getVertex(i, pa)
        getVertex((i + 1) % 3, pb)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbSlow(t, aabbMin, aabbMax)
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        val dots = Stack.newVec()
        dots.set(dir.dot(vertices[0]), dir.dot(vertices[1]), dir.dot(vertices[2]))
        out.set(vertices[maxAxis(dots)])
        return out
    }

    override fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int) {
        getPlaneEquation(planeNormal, planeSupport)
    }

    override val numPlanes get() = 1

    fun calcNormal(normal: Vector3d) {
        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        tmp1.setSub(vertices[1], vertices[0])
        tmp2.setSub(vertices[2], vertices[0])

        tmp1.cross(tmp2, normal).normalize()
    }

    fun getPlaneEquation(planeNormal: Vector3d, planeSupport: Vector3d) {
        calcNormal(planeNormal)
        planeSupport.set(vertices[0])
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        // moving convex shapes is not supported
        // todo this is convex, so... approximate this as a mix of its three corners...
        return inertia.set(0.0)
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        val normal = Stack.newVec()
        calcNormal(normal)
        // distance to plane
        var dist = pt.dot(normal)
        val planeConst = vertices[0].dot(normal)
        dist -= planeConst
        if (dist >= -tolerance && dist <= tolerance) {
            // inside check on edge-planes
            val pa = Stack.newVec()
            val pb = Stack.newVec()
            val edge = Stack.newVec()
            val edgeNormal = Stack.newVec()
            for (i in 0 until 3) {
                getEdge(i, pa, pb)
                edge.setSub(pb, pa)
                edgeNormal.setCross(edge, normal)
                edgeNormal.normalize()
                /*double*/
                dist = pt.dot(edgeNormal)
                val edgeConst = pa.dot(edgeNormal)
                dist -= edgeConst
                if (dist < -tolerance) {
                    Stack.subVec(5)
                    return false
                }
            }
            Stack.subVec(5)
            return true
        }
        Stack.subVec(1) // normal
        return false
    }

    override val numPreferredPenetrationDirections: Int
        get() = 2

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3d) {
        calcNormal(penetrationVector)
        if (index != 0) {
            penetrationVector.mul(-1.0)
        }
    }
}
