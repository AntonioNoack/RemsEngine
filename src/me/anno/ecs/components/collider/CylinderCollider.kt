package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.CylinderShapeX
import com.bulletphysics.collision.shapes.CylinderShapeZ
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.length
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

class CylinderCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @Range(0.0, 2.0)
    @SerializedProperty
    var axis = 1

    @SerializedProperty
    var height = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the two rings
        val h = height * 0.5
        val r = radius
        unionRing(globalTransform, aabb, tmp, axis, r, +h, preferExact)
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfHeight = (height * 0.5).toFloat()
        val radius = radius.toFloat()
        val circle = when (axis) {
            0 -> length(deltaPos.y, deltaPos.z)
            1 -> length(deltaPos.x, deltaPos.z)
            else -> length(deltaPos.x, deltaPos.y)
        } - radius
        val box = abs(deltaPos[axis]) - halfHeight
        deltaPos.x = circle
        deltaPos.y = box
        return and2SDFs(deltaPos, roundness.toFloat())
    }

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

    override fun clone(): CylinderCollider {
        val clone = CylinderCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CylinderCollider
        clone.axis = axis
        clone.height = height
        clone.radius = radius
    }

    override val className get() = "CylinderCollider"

}