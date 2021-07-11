package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class SphereCollider : Collider() {

    @SerializedProperty
    var radius = 1.0

    override val className get() = "SphereCollider"

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        return deltaPosition.length() - radius
    }

    override fun createBulletShape(): CollisionShape {
        return SphereShape(radius)
    }

    override fun drawShape() {
        // todo draw sphere
    }

}