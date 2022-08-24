package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.ConeShapeX
import com.bulletphysics.collision.shapes.ConeShapeZ
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawCone
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector2f.Companion.lengthSquared
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sign
import kotlin.math.sqrt

class ConeCollider : Collider() {

    /** which axis the height is for, x = 0, y = 1, z = 2 */
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
        tmp.setComponent(axis, +h)
        aabb.union(globalTransform.transformPosition(tmp))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {

        val roundness = roundness.toFloat()
        val h = -height.toFloat() + roundness * 2f
        val dist1D = deltaPos[axis] + h * 0.5f // centering
        val dist2D = when (axis) {
            0 -> length(deltaPos.y, deltaPos.z)
            1 -> length(deltaPos.x, deltaPos.z)
            else -> length(deltaPos.x, deltaPos.y)
        }

        // todo how can we include roundness here?

        val r = radius.toFloat() - roundness
        val t = clamp((dist2D * r + dist1D * h) / (r * r + h * h))
        val a2 = lengthSquared(r * t - dist2D, h * t - dist1D)
        val b2 = lengthSquared(clamp(dist2D, 0f, r) - dist2D, h - dist1D)
        val k = sign(h)
        deltaPos.x = a2
        deltaPos.y = b2
        val d = min(a2, b2)
        val s = max(k * (dist2D * h - dist1D * r), k * (dist1D - h))
        return sqrt(d) * sign(s) - roundness

    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return when (axis) {
            0 -> ConeShapeX(radius * scale.y, height * scale.x)
            2 -> ConeShapeZ(radius * scale.x, height * scale.z)
            else -> ConeShape(radius * scale.x, height * scale.y)
        }
    }

    override fun drawShape() {
        // todo check whether they are correct (the same as the physics behaviour)
        when (axis) {
            0 -> drawCone(entity, radius, radius, height, 0.0, LineShapes.zToX)
            1 -> drawCone(entity, radius, radius, height, 0.0, LineShapes.zToY)
            2 -> drawCone(entity, radius, radius, height, 0.0, null)
        }
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