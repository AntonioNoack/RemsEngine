package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.maths.Maths.TAU
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.max

class CapsuleCollider : Collider() {

    // which axis the height is for, x = 0, y = 1, z = 2
    @Range(0.0, 2.0)
    @SerializedProperty
    var axis = 1

    @SerializedProperty
    var halfHeight = 1.0

    @SerializedProperty
    var radius = 1.0

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the two rings, and the top and bottom peak
        val r = radius
        val h = halfHeight
        unionRing(globalTransform, aabb, tmp, axis, r, +h, preferExact)
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
        val s = h + r
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setComponent(axis, +s)))
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setComponent(axis, -s)))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // roundness is ignored, because a capsule is already perfectly round
        val halfExtends = halfHeight.toFloat()
        deltaPos.absolute()
        deltaPos.setComponent(axis, max(deltaPos[axis] - halfExtends, 0f))
        return deltaPos.length() - radius.toFloat()
    }

    override fun drawShape() {
        val h = halfHeight
        val r = radius
        val xi = PI / 2
        val zi = xi * 3
        when (axis) {
            0 -> {
                LineShapes.drawLine(entity, -h, -r, 0.0, +h, -r, 0.0)
                LineShapes.drawLine(entity, -h, +r, 0.0, +h, +r, 0.0)
                LineShapes.drawLine(entity, -h, 0.0, -r, +h, 0.0, -r)
                LineShapes.drawLine(entity, -h, 0.0, +r, +h, 0.0, +r)
                LineShapes.drawPartialSphere(entity, r, Vector3d(-h, 0.0, 0.0), 0.0, TAU, PI, PI, xi, PI)
                LineShapes.drawPartialSphere(entity, r, Vector3d(+h, 0.0, 0.0), 0.0, TAU, 0.0, PI, zi, PI)
            }
            1 -> {
                LineShapes.drawLine(entity, -r, -h, 0.0, -r, +h, 0.0)
                LineShapes.drawLine(entity, +r, -h, 0.0, +r, +h, 0.0)
                LineShapes.drawLine(entity, 0.0, -h, -r, 0.0, +h, -r)
                LineShapes.drawLine(entity, 0.0, -h, +r, 0.0, +h, +r)
                LineShapes.drawPartialSphere(entity, r, Vector3d(0.0, -h, 0.0), xi, PI, 0.0, TAU, PI, PI)
                LineShapes.drawPartialSphere(entity, r, Vector3d(0.0, +h, 0.0), zi, PI, 0.0, TAU, 0.0, PI)
            }
            2 -> {
                LineShapes.drawLine(entity, -r, 0.0, -h, -r, 0.0, +h)
                LineShapes.drawLine(entity, +r, 0.0, -h, +r, 0.0, +h)
                LineShapes.drawLine(entity, 0.0, -r, -h, 0.0, -r, +h)
                LineShapes.drawLine(entity, 0.0, +r, -h, 0.0, +r, +h)
                LineShapes.drawPartialSphere(entity, r, Vector3d(0.0, 0.0, -h), PI, PI, xi, PI, 0.0, TAU)
                LineShapes.drawPartialSphere(entity, r, Vector3d(0.0, 0.0, +h), 0.0, PI, zi, PI, 0.0, TAU)
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CapsuleCollider
        dst.axis = axis
        dst.halfHeight = halfHeight
        dst.radius = radius
    }
}
