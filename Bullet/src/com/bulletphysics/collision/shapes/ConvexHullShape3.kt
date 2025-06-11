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
class ConvexHullShape3(private val points: FloatArray) : PolyhedralConvexShape() {

    val numPoints: Int = points.size / 3

    init {
        recalculateLocalAabb()
    }

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
        recalculateLocalAabb()
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, supVec: Vector3d): Vector3d {
        supVec.set(0.0, 0.0, 0.0)
        var newDot: Double
        var maxDot = Double.Companion.NEGATIVE_INFINITY
        var i = 0
        val l = points.size
        while (i < l) {
            val x = points[i] * localScaling.x
            val y = points[i + 1] * localScaling.y
            val z = points[i + 2] * localScaling.z
            newDot = dir.x * x + dir.y * y + dir.z * z
            if (newDot > maxDot) {
                maxDot = newDot
                supVec.set(x, y, z)
            }
            i += 3
        }
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

        var i = 0
        val l = points.size
        while (i < l) {
            val x = points[i] * localScaling.x
            val y = points[i + 1] * localScaling.y
            val z = points[i + 2] * localScaling.z

            for (j in 0 until numVectors) {
                val vec = dirs[j]
                newDot = vec.x * x + vec.y * y + vec.z * z
                if (newDot > wCoords[j]) {
                    // WARNING: don't swap next lines, the w component would get overwritten!
                    outs[j].set(x, y, z)
                    wCoords[j] = newDot
                }
            }
            i += 3
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
        return this.numPoints
    }

    override val numEdges get(): Int {
        return this.numPoints
    }

    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
        val index0 = (i % this.numPoints) * 3
        val index1 = ((i + 1) % this.numPoints) * 3
        pa.set(points[index0].toDouble(), points[index0 + 1].toDouble(), points[index0 + 2].toDouble())
        pb.set(points[index1].toDouble(), points[index1 + 1].toDouble(), points[index1 + 2].toDouble())
        mul(pa, pa, localScaling)
        mul(pb, pb, localScaling)
    }

    override fun getVertex(i: Int, vtx: Vector3d) {
        val j = i * 3
        vtx.x = points[j] * localScaling.x
        vtx.y = points[j + 1] * localScaling.y
        vtx.z = points[j + 2] * localScaling.z
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
