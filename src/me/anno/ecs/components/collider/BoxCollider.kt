package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.io.serialization.SerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class BoxCollider : Collider() {

    @SerializedProperty
    var halfExtends = Vector3d(1.0)

    override val className get() = "BoxCollider"

    // we could use this for our own physics engine...
    /*override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        deltaPosition.absolute()
        deltaPosition.sub(halfExtends)
        deltaPosition.add(cornerRoundness, cornerRoundness, cornerRoundness)
        val outside = length(max(deltaPosition.x, 0.0), max(deltaPosition.y, 0.0), max(deltaPosition.z, 0.0))
        val inside = min(max(deltaPosition.x, max(deltaPosition.y, deltaPosition.z)), 0.0)
        return outside + inside - cornerRoundness
    }*/

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val halfExtends = halfExtends
        unionCube(globalTransform, aabb, tmp, halfExtends.x, halfExtends.y, halfExtends.z)
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfExtends = halfExtends
        deltaPos.absolute()
        deltaPos.sub(halfExtends.x.toFloat(), halfExtends.y.toFloat(), halfExtends.z.toFloat())
        return and3SDFs(deltaPos)
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