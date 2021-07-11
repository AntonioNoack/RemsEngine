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
    var axis = 0

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override val className get() = "CylinderCollider"

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        TODO()
    }

    override fun createBulletShape(): CollisionShape {
        return when (axis) {
            0 -> ConeShapeX(radius, height)
            1 -> ConeShape(radius, height)
            else -> ConeShapeZ(radius, height)
        }
    }

    override fun drawShape() {
        // todo draw cone shape
    }

}