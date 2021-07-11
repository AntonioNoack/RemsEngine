package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.CylinderShapeX
import com.bulletphysics.collision.shapes.CylinderShapeZ
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class CylinderCollider : Collider() {

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
            0 -> CylinderShapeX(javax.vecmath.Vector3d(height * 0.5, radius, radius))
            1 -> CylinderShape(javax.vecmath.Vector3d(radius, height * 0.5, radius))
            else -> CylinderShapeZ(javax.vecmath.Vector3d(radius, radius, height * 0.5))
        }
    }

    override fun drawShape() {
        // todo draw cylinder
    }

}