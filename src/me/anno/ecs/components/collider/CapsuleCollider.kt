package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes
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
        // todo test this
        // todo only draw if selected or collider debug mode
        // todo color based on physics / trigger (?)
        val h = height * 0.5
        val r = radius
        when (axis) {
            0 -> {
                LineShapes.drawLine(entity, -h, -r, 0.0, +h, -r, 0.0)
                LineShapes.drawLine(entity, -h, +r, 0.0, +h, +r, 0.0)
                LineShapes.drawLine(entity, -h, 0.0, -r, +h, 0.0, -r)
                LineShapes.drawLine(entity, -h, 0.0, +r, +h, 0.0, +r)
                LineShapes.drawSphere(entity, r, Vector3d(-h, 0.0, 0.0))
                LineShapes.drawSphere(entity, r, Vector3d(+h, 0.0, 0.0))
            }
            1 -> {
                LineShapes.drawLine(entity, -r, -h, 0.0, -r, +h, 0.0)
                LineShapes.drawLine(entity, +r, -h, 0.0, +r, +h, 0.0)
                LineShapes.drawLine(entity, 0.0, -h, -r, 0.0, +h, -r)
                LineShapes.drawLine(entity, 0.0, -h, +r, 0.0, +h, +r)
                LineShapes.drawSphere(entity, r, Vector3d(0.0, -h, 0.0))
                LineShapes.drawSphere(entity, r, Vector3d(0.0, +h, 0.0))
            }
            2 -> {
                LineShapes.drawLine(entity, -r, 0.0, -h, -r, 0.0, +h)
                LineShapes.drawLine(entity, +r, 0.0, -h, +r, 0.0, +h)
                LineShapes.drawLine(entity, 0.0, -r, -h, 0.0, -r, +h)
                LineShapes.drawLine(entity, 0.0, +r, -h, 0.0, +r, +h)
                LineShapes.drawSphere(entity, r, Vector3d(0.0, 0.0, -h))
                LineShapes.drawSphere(entity, r, Vector3d(0.0, 0.0, +h))
            }
        }
    }

    override fun clone(): CapsuleCollider {
        val clone = CapsuleCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CapsuleCollider
        clone.axis = axis
        clone.height = height
        clone.radius = radius
    }

    override val className get() = "CapsuleCollider"

}
