package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.ConeShapeX
import com.bulletphysics.collision.shapes.ConeShapeZ
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class ConeCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @SerializedProperty
    var axis = 1

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override val className get() = "ConeCollider"

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        TODO()
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> ConeShapeX(radius * scale.y, height * scale.x)
            1 -> ConeShape(radius * scale.x, height * scale.y)
            2 -> ConeShapeZ(radius * scale.x, height * scale.z)
            else -> throw RuntimeException()
        }
    }

    override fun drawShape() {
        // todo draw cone shape
    }

}