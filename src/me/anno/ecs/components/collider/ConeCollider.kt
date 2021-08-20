package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.ConeShapeX
import com.bulletphysics.collision.shapes.ConeShapeZ
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.Maths.length
import me.anno.utils.types.Vectors.setAxis
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class ConeCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @Range(0.0, 2.0)
    @SerializedProperty
    var axis = 1

    @SerializedProperty
    var height = 2.0

    @SerializedProperty
    var radius = 1.0

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the peak and the bottom ring
        val h = height * 0.5
        val r = radius
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
        tmp.setAxis(axis, +h)
        aabb.union(globalTransform.transformPosition(tmp))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {

        val radius = radius.toFloat()
        val height = height.toFloat()

        val invScale = height
        val scale = 1f / height
        deltaPos.mul(scale)

        val localY = height * scale * 0.5f - deltaPos[axis]

        val dist2D = when (axis) {
            0 -> length(deltaPos.y, deltaPos.z)
            1 -> length(deltaPos.x, deltaPos.z)
            else -> length(deltaPos.x, deltaPos.y)
        }

        // theoretically, it's just a triangle, which has been rotated
        // we don't even need the side on the middle axis
        deltaPos.x = -localY // we could inverse the sign later
        deltaPos.y = ((localY - 1f) * radius + dist2D * height) / length(radius, height)

        return and2SDFs(deltaPos, roundness.toFloat() * 0.5f) * invScale

    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> ConeShapeX(radius * scale.y, height * scale.x)
            1 -> ConeShape(radius * scale.x, height * scale.y)
            2 -> ConeShapeZ(radius * scale.x, height * scale.z)
            else -> throw RuntimeException()
        }
    }

    override fun drawShape() {
        // todo draw cone shape
    }

    override fun clone(): ConeCollider {
        val clone = ConeCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ConeCollider
        clone.axis = axis
        clone.height = height
        clone.radius = radius
    }

    override val className get() = "ConeCollider"

}