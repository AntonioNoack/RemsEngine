package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import java.util.*
import org.joml.Vector3d
import com.bulletphysics.util.setScaleAdd

/**
 * ConvexHullShape implements an implicit convex hull of an array of vertices.
 * Bullet provides a general and fast collision detector for convex shapes based
 * on GJK and EPA using localGetSupportingVertex.
 *
 * @author Antonio, jezek2
 */
class ConvexHullShape2(private val points: Array<Vector3d>) : PolyhedralConvexShape() {

    init {
        recalculateLocalAabb()
    }

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
        recalculateLocalAabb()
    }

    val numPoints: Int
        get() = points.size

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, supVec: Vector3d): Vector3d {
        supVec.set(0.0, 0.0, 0.0)
        var newDot: Double
        var maxDot = Double.Companion.NEGATIVE_INFINITY

        val vtx = Stack.newVec()
        for (point in points) {
            mul(vtx, point, localScaling)

            newDot = dir.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                supVec.set(vtx)
            }
        }

        Stack.subVec(1)

        return supVec
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        var newDot: Double

        // JAVA NOTE: rewritten as code used W coord for temporary usage in Vector3
        // TODO: optimize it
        val wCoords = DoubleArray(numVectors)

        // use 'w' component of supportVerticesOut?
        Arrays.fill(wCoords, Double.Companion.NEGATIVE_INFINITY)

        val vtx = Stack.newVec()
        for (point in points) {
            mul(vtx, point, localScaling)

            for (j in 0 until numVectors) {
                val vec = dirs[j]
                newDot = vec.dot(vtx)
                //if (newDot > supportVerticesOut[j][3])
                if (newDot > wCoords[j]) {
                    // WARNING: don't swap next lines, the w component would get overwritten!
                    outs[j]!!.set(vtx)
                    //supportVerticesOut[j][3] = newDot;
                    wCoords[j] = newDot
                }
            }
        }
    }

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        val supVertex = localGetSupportingVertexWithoutMargin(dir, out)

        if (margin != 0.0) {
            val vecNorm = Stack.newVec(dir)
            if (vecNorm.lengthSquared() < (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.setScaleAdd(margin, vecNorm, supVertex)
            Stack.subVec(1)
        }
        return out
    }

    /**
     * Currently just for debugging (drawing), perhaps future support for algebraic continuous collision detection.
     * Please note that you can debug-draw ConvexHullShape with the Raytracer Demo.
     */
    override val numVertices get(): Int {
        return points.size
    }

    override val numEdges get(): Int {
        return points.size
    }

    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
        val index0 = i % points.size
        val index1 = (i + 1) % points.size
        mul(pa, points[index0], localScaling)
        mul(pb, points[index1], localScaling)
    }

    override fun getVertex(i: Int, vtx: Vector3d) {
        mul(vtx, points[i], localScaling)
    }

    override val numPlanes get(): Int {
        return 0
    }

    override fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int) {
        throw NotImplementedError()
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        throw NotImplementedError()
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONVEX_HULL_SHAPE_PROXYTYPE
}
