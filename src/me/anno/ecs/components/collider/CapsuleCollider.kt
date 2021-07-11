package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CapsuleShapeX
import com.bulletphysics.collision.shapes.CapsuleShapeZ
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import kotlin.math.max

class CapsuleCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @SerializedProperty
    var axis = 0

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        deltaPosition.absolute()
        val halfExtends = height * 0.5
        deltaPosition[axis] = max(deltaPosition[axis] - halfExtends, 0.0)
        return deltaPosition.length() - radius
    }

    override fun createBulletShape(): CollisionShape {
        return when (axis) {
            0 -> CapsuleShapeX(radius, height)
            1 -> CapsuleShape(radius, height, axis)
            else -> CapsuleShapeZ(radius, height)
        }
    }

    override fun drawShape() {
        // todo draw a capsule
    }

    override val className get() = "CapsuleCollider"

    companion object {
        private operator fun Vector3d.set(axis: Int, value: Double) {
            when (axis) {
                0 -> x = value
                1 -> y = value
                2 -> z = value
            }
        }
    }

}
