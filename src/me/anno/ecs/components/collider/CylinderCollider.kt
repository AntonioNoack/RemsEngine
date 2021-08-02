package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.CylinderShapeX
import com.bulletphysics.collision.shapes.CylinderShapeZ
import me.anno.ecs.annotations.Range
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d

class CylinderCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @Range(-0.4, 2.4)
    @SerializedProperty
    var axis = 0

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override val className get() = "CylinderCollider"

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> CylinderShapeX(javax.vecmath.Vector3d(height * 0.5 * scale.x, radius * scale.y, radius * scale.z))
            1 -> CylinderShape(javax.vecmath.Vector3d(radius * scale.x, height * 0.5 * scale.y, radius * scale.z))
            2 -> CylinderShapeZ(javax.vecmath.Vector3d(radius * scale.x, radius * scale.y, height * 0.5 * scale.z))
            else -> throw RuntimeException()
        }
    }

    override fun drawShape() {
        // todo draw cylinder
    }

}