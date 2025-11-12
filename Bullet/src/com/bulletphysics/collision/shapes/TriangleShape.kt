package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.maxAxis
import cz.advel.stack.Stack
import me.anno.utils.types.Triangles.getTriangleArea
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.pow

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

    override fun getVertex(i: Int, vtx: Vector3f) {
        vtx.set(vertices[i])
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.TRIANGLE

    override val numEdges
        get(): Int {
            return 3
        }

    override fun getEdge(i: Int, pa: Vector3f, pb: Vector3f) {
        getVertex(i, pa)
        getVertex((i + 1) % 3, pb)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbSlow(t, aabbMin, aabbMax)
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        val dots = Stack.newVec3d()
        dots.set(dir.dot(vertices[0]), dir.dot(vertices[1]), dir.dot(vertices[2]))
        out.set(vertices[maxAxis(dots)])
        Stack.subVec3d(1)
        return out
    }

    fun calcNormal(normal: Vector3f) {
        val tmp = Stack.newVec3d()
        subCross(vertices[0], vertices[1], vertices[2], tmp).normalize()
        normal.set(tmp)
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // moving convex shapes is not supported
        val area = getTriangleArea(vertices[0],vertices[1],vertices[2]).toFloat()
        return inertia.set(abs(area).pow(1.5f))
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        val normal = Stack.newVec3f()
        calcNormal(normal)
        // distance to plane
        var dist = pt.dot(normal)
        val planeConst = vertices[0].dot(normal)
        dist -= planeConst
        if (dist >= -tolerance && dist <= tolerance) {
            // inside check on edge-planes
            val pa = Stack.newVec3f()
            val pb = Stack.newVec3f()
            val edge = Stack.newVec3f()
            val edgeNormal = Stack.newVec3f()
            for (i in 0 until 3) {
                getEdge(i, pa, pb)
                pb.sub(pa, edge)
                edge.cross(normal, edgeNormal)
                edgeNormal.normalize()
                /*double*/
                dist = pt.dot(edgeNormal)
                val edgeConst = pa.dot(edgeNormal)
                dist -= edgeConst
                if (dist < -tolerance) {
                    Stack.subVec3f(5)
                    return false
                }
            }
            Stack.subVec3f(5)
            return true
        }
        Stack.subVec3f(1) // normal
        return false
    }

    override val numPreferredPenetrationDirections: Int
        get() = 2

    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f) {
        calcNormal(penetrationVector)
        if (index != 0) {
            penetrationVector.negate()
        }
    }
}
