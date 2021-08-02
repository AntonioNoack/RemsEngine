package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CapsuleShapeX
import com.bulletphysics.collision.shapes.CapsuleShapeZ
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import java.lang.RuntimeException
import kotlin.math.max

class CapsuleCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @SerializedProperty
    var axis = 0

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    /*override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        deltaPosition.absolute()
        val halfExtends = height * 0.5
        deltaPosition[axis] = max(deltaPosition[axis] - halfExtends, 0.0)
        return deltaPosition.length() - radius
    }*/

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> CapsuleShape(radius * scale.y, height * scale.x, axis) // x
            1 -> CapsuleShape(radius * scale.x, height * scale.y, axis) // y
            2 -> CapsuleShape(radius * scale.x, height * scale.z, axis) // z
            else -> throw RuntimeException()
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
