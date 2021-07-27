package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.util.ObjectArrayList
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

/**
 * uses a point cloud for collisions
 * */
class ConvexCollider : Collider() {

    @SerializedProperty
    val points = ArrayList<Vector3d>()

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        TODO("Not yet implemented")
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