package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.util.ObjectArrayList
import me.anno.io.serialization.SerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d

/**
 * uses a point cloud for collisions
 * */
class ConvexCollider : Collider() {

    @SerializedProperty
    val points = ArrayList<Vector3d>()

    // todo signed distance...

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        for (pt in points) {
            aabb.union(globalTransform.transformPosition(tmp.set(pt)))
        }
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        val pointList = ObjectArrayList<javax.vecmath.Vector3d>(points.size)
        pointList.addAll(points.map { javax.vecmath.Vector3d(it.x * scale.x, it.y * scale.y, it.z * scale.z) })
        return ConvexHullShape(pointList)
    }

    override fun drawShape() {
        // todo draw the convex hull
    }

    override val className get() = "ConvexCollider"

}