package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.debug.DebugShapes
import me.anno.engine.debug.DebugTriangle
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.geometry.convexhull.ConvexHull
import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d

/**
 * uses a convex point cloud for collisions
 * todo calculate SDF or at least raycasting
 * */
class ConvexCollider : Collider() {

    @SerializedProperty
    var points: FloatArray? = null

    @NotSerializedProperty
    var hull: ConvexHull? = null

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        val points = points ?: return
        forLoopSafely(points.size, 3) { i ->
            tmp.set(points, i)
            dstUnion.union(globalTransform.transformPosition(tmp))
        }
    }

    override fun drawShape(pipeline: Pipeline) {
        // currently drawn as a point cloud
        // triangles or polygons would be better, but we don't have them
        val points = points ?: return
        var hull = hull
        if (hull == null) {
            hull = ConvexHulls.calculateConvexHull(points)
            this.hull = hull
        }
        val color = colliderLineColor
        if (hull != null) {
            val drawMatrix = transform?.getDrawMatrix()
            // also draw lines with full alpha
            val triColor = color.withAlpha(127)
            var vertices: List<Vector3d> = hull.vertices
            if (drawMatrix != null) {
                // draw all lines relative to this entity
                vertices = vertices.map {
                    drawMatrix.transformPosition(it, Vector3d())
                }
            }
            val indices = hull.triangles
            forLoopSafely(indices.size, 3) { idx ->
                val a = vertices[indices[idx]]
                val b = vertices[indices[idx + 1]]
                val c = vertices[indices[idx + 2]]
                DebugShapes.debugTriangles.add(
                    DebugTriangle(
                        a, b, c,
                        triColor, 0f
                    )
                )
                LineBuffer.addLine(a, b, color)
                LineBuffer.addLine(b, c, color)
                LineBuffer.addLine(c, a, color)
            }
        } else {
            forLoopSafely(points.size, 3) { i ->
                LineShapes.drawPoint(entity, points[i], points[i + 1], points[i + 2], 0.1, color)
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConvexCollider) return
        dst.points = points
    }
}