package me.anno.ecs.components.collider

import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import kotlin.math.max

class CapsuleCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @SerializedProperty
    var axis = 0

    @SerializedProperty
    var halfExtends = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun getClassName(): String = "CapsuleCollider"

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        deltaPosition.absolute()
        deltaPosition[axis] = max(deltaPosition[axis] - halfExtends, 0.0)
        return deltaPosition.length() - radius
    }

    companion object {
        private operator fun Vector3d.set(axis: Int, value: Double) {
            when(axis){
                0 -> x = value
                1 -> y = value
                2 -> z = value
            }
        }
    }

}
