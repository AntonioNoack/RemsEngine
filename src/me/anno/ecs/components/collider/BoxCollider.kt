package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.Maths.length
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

class BoxCollider : Collider() {

    @SerializedProperty
    var cornerRoundness = 0.0

    @SerializedProperty
    var halfExtends = Vector3d(1.0)

    override val className get() = "BoxCollider"

    // we could use this for our own physics engine...
    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        deltaPosition.absolute()
        deltaPosition.sub(halfExtends)
        deltaPosition.add(cornerRoundness, cornerRoundness, cornerRoundness)
        val outside = length(max(deltaPosition.x, 0.0), max(deltaPosition.y, 0.0), max(deltaPosition.z, 0.0))
        val inside = min(max(deltaPosition.x, max(deltaPosition.y, deltaPosition.z)), 0.0)
        return outside + inside - cornerRoundness
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return BoxShape(
            javax.vecmath.Vector3d(
                halfExtends.x * scale.x,
                halfExtends.y * scale.y,
                halfExtends.z * scale.z
            )
        )
    }

    override fun drawShape() {
        // todo draw box
    }

}