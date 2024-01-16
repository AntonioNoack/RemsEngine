package me.anno.ecs.components.collider

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.engine.serialization.SerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

/**
 * uses a convex point cloud for collisions
 * */
class ConvexCollider : Collider() {

    @SerializedProperty
    var points: FloatArray? = null

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val points = points ?: return
        for (i in points.indices step 3) {
            tmp.set(
                points[i].toDouble(),
                points[i + 1].toDouble(),
                points[i + 2].toDouble()
            )
            aabb.union(globalTransform.transformPosition(tmp))
        }
    }

    override fun drawShape() {
        // currently drawn as a point cloud
        // triangles or polygons would be better, but we don't have them
        val points = points ?: return
        for (i in points.indices step 3) {
            LineShapes.drawPoint(entity, points[i], points[i + 1], points[i + 2], 0.1)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ConvexCollider
        dst.points = points
    }

    override val className: String get() = "ConvexCollider"

}