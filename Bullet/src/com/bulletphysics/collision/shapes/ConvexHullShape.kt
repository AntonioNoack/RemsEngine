package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.MeshCollider
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.showDebugPoint
import me.anno.engine.debug.DebugShapes.showDebugTriangle
import me.anno.engine.debug.DebugTriangle
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

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

    override var localScaling: Vector3f
        get() = super.localScaling
        set(value) {
            super.localScaling = value
            recalculateLocalAabb()
        }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {

        val dx = dir.x
        val dy = dir.y
        val dz = dir.z

        // default aka center
        out.set(0f)

        val points = points
        val localScaling = localScaling
        var maxDot = Float.NEGATIVE_INFINITY
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

    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        val supVertex = localGetSupportingVertexWithoutMargin(dir, out)

        if (margin != 0f) {
            val vecNorm = Stack.newVec3f(dir)
            if (vecNorm.lengthSquared() < BulletGlobals.FLT_EPSILON_SQ) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.fma(margin, vecNorm)
            Stack.subVec3f(1)
        }
        return out
    }

    override val numVertices get(): Int = numPoints
    override val numEdges get(): Int = numPoints

    /**
     * Currently just for debugging (drawing), perhaps future support for algebraic continuous collision detection.
     * Please note that you can debug-draw ConvexHullShape with the Raytracer Demo.
     */
    override fun getEdge(i: Int, pa: Vector3f, pb: Vector3f) {
        val index0 = (i % this.numPoints) * 3
        val index1 = ((i + 1) % this.numPoints) * 3
        pa.set(points, index0).mul(localScaling)
        pb.set(points, index1).mul(localScaling)
    }

    override fun getVertex(i: Int, vtx: Vector3f) {
        vtx.set(points, i * 3).mul(localScaling)
    }

    override fun isInside(pt: Vector3d, tolerance: Double): Boolean {
        throw NotImplementedError()
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONVEX_HULL

    override fun draw(drawMatrix: Matrix4x3?, color: Int) {
        val vertices = List(numVertices) { idx ->
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
                showDebugTriangle(DebugTriangle(a, b, c, color, 0f))
            }
        } else {
            for (i in vertices.indices) {
                val v = vertices[i]
                showDebugPoint(DebugPoint(v, color, 0f))
            }
        }
    }
}
