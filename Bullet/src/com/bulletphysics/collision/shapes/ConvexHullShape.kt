package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.util.setScaleAdd
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.MeshCollider
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.debug.DebugTriangle
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.Arrays

/**
 * ConvexHullShape implements an implicit convex hull of an array of vertices.
 * Bullet provides a general and fast collision detector for convex shapes based
 * on GJK and EPA using localGetSupportingVertex.
 *
 * @author Antonio, jezek2
 */
class ConvexHullShape(val points: FloatArray, val triangles: IntArray?) :
    PolyhedralConvexShape(), MeshCollider.BulletShapeDrawable {

    val numPoints: Int = points.size / 3

    init {
        recalculateLocalAabb()
    }

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
        recalculateLocalAabb()
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {

        val dx = dir.x
        val dy = dir.y
        val dz = dir.z

        // default aka center
        out.set(0.0)

        val points = points
        val localScaling = localScaling
        var maxDot = Double.NEGATIVE_INFINITY
        forLoopSafely(points.size, 3) { i ->
            val x = points[i] * localScaling.x
            val y = points[i + 1] * localScaling.y
            val z = points[i + 2] * localScaling.z
            val newDot = x * dx + y * dy + z * dz
            if (newDot > maxDot) {
                maxDot = newDot
                out.set(x, y, z)
            }
        }
        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        var newDot: Double

        // JAVA NOTE: rewritten as code used W coord for temporary usage in Vector3
        // TODO: optimize it
        val wCoords = DoubleArray(numVectors)

        // use 'w' component of supportVerticesOut?
        Arrays.fill(wCoords, Double.NEGATIVE_INFINITY)

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

    override val numVertices get(): Int = numPoints
    override val numEdges get(): Int = numPoints

    /**
     * Currently just for debugging (drawing), perhaps future support for algebraic continuous collision detection.
     * Please note that you can debug-draw ConvexHullShape with the Raytracer Demo.
     */
    override fun getEdge(i: Int, pa: Vector3d, pb: Vector3d) {
        val index0 = (i % this.numPoints) * 3
        val index1 = ((i + 1) % this.numPoints) * 3
        pa.set(points, index0).mul(localScaling)
        pb.set(points, index1).mul(localScaling)
    }

    override fun getVertex(i: Int, vtx: Vector3d) {
        vtx.set(points, i * 3).mul(localScaling)
    }

    override val numPlanes get(): Int = 0

    override fun getPlane(planeNormal: Vector3d, planeSupport: Vector3d, i: Int) {
        throw NotImplementedError()
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        throw NotImplementedError()
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONVEX_HULL_SHAPE_PROXYTYPE

    override fun draw(drawMatrix: Matrix4x3?, color: Int) {
        val vertices = createList(numVertices) { idx ->
            Vector3d(points, idx * 3)
        }
        if (drawMatrix != null) {
            val pos = drawMatrix.getTranslation(Vector3d())
            val rot = drawMatrix.getUnnormalizedRotation(Quaternionf()).normalize()
            for (i in vertices.indices) {
                val v = vertices[i]
                v.rotate(rot)
                v.add(pos)
            }
        }
        val triangles = triangles
        if (triangles != null) {
            val color = color.withAlpha(127)
            forLoopSafely(triangles.size, 3) { i ->
                val a = vertices[triangles[i]]
                val b = vertices[triangles[i + 1]]
                val c = vertices[triangles[i + 2]]
                DebugShapes.debugTriangles.add(DebugTriangle(a, b, c, color, 0f))
            }
        } else {
            for (i in vertices.indices) {
                val v = vertices[i]
                DebugShapes.debugPoints.add(DebugPoint(v, color, 0f))
            }
        }
    }
}
