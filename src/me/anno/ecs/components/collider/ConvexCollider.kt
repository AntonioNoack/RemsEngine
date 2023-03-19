package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConvexHullShape3
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.io.serialization.SerializedProperty
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

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return ConvexHullShape3(points!!)
    }

    override fun drawShape() {
        // currently drawn as a point cloud
        // triangles or polygons would be better, but we don't have them
        val points = points ?: return
        for (i in points.indices step 3) {
            LineShapes.drawPoint(entity, points[i], points[i + 1], points[i + 2], 0.1)
        }
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ConvexCollider
        clone.points = points
    }

    override val className get() = "ConvexCollider"

}