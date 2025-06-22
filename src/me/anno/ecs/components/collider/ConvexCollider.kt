package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
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
        val color = getLineColor(hasPhysics)
        forLoopSafely(points.size, 3) { i ->
            LineShapes.drawPoint(entity, points[i], points[i + 1], points[i + 2], 0.1, color)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConvexCollider) return
        dst.points = points
    }
}