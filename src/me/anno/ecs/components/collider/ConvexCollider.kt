package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
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

    override fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val points = points ?: return
        for (i in 0 until points.size - 2 step 3) {
            tmp.set(
                points[i].toDouble(),
                points[i + 1].toDouble(),
                points[i + 2].toDouble()
            )
            aabb.union(globalTransform.transformPosition(tmp))
        }
    }

    override fun drawShape(pipeline: Pipeline) {
        // currently drawn as a point cloud
        // triangles or polygons would be better, but we don't have them
        val points = points ?: return
        val color = getLineColor(hasPhysics)
        for (i in 0 until points.size - 2 step 3) {
            LineShapes.drawPoint(entity, points[i], points[i + 1], points[i + 2], 0.1, color)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConvexCollider) return
        dst.points = points
    }
}