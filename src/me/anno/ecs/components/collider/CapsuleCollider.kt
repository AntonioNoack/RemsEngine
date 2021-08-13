package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.types.Vectors.setAxis
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

class CapsuleCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @SerializedProperty
    var axis = 0

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the two rings and the top and bottom peak
        val r = radius
        val h = height * 0.5
        unionRing(globalTransform, aabb, tmp, axis, r, +h, preferExact)
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
        val s = h + r
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setAxis(axis, +s)))
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setAxis(axis, -s)))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // roundness is ignored, because a capsule is already perfectly round
        val halfExtends = (height * 0.5).toFloat()
        deltaPos.absolute()
        deltaPos.setAxis(axis, max(deltaPos[axis] - halfExtends, 0f))
        return deltaPos.length() - radius.toFloat()
    }

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

}
