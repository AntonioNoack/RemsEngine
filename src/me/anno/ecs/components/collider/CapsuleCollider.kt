package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.engine.ui.LineShapes.drawPartialSphere
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.TAU
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.max

class CapsuleCollider : Collider() {

    @Range(0.0, 2.0)
    @SerializedProperty
    @Docs("which axis the height is for")
    var axis = Axis.Y

    @SerializedProperty
    var halfHeight = 1f

    @SerializedProperty
    var radius = 1f

    @SerializedProperty
    var margin = 0.04f

    override fun union(globalTransform: Matrix4x3, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        // union the two rings, and the top and bottom peak
        val r = radius.toDouble()
        val h = halfHeight.toDouble()
        unionRing(globalTransform, aabb, tmp, axis, r, +h, preferExact)
        unionRing(globalTransform, aabb, tmp, axis, r, -h, preferExact)
        val s = h + r
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setComponent(axis.id, +s)))
        aabb.union(globalTransform.transformPosition(tmp.set(0.0).setComponent(axis.id, -s)))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // roundness is ignored, because a capsule is already perfectly round
        val halfExtends = halfHeight
        deltaPos.absolute()
        deltaPos.setComponent(axis.id, max(deltaPos[axis.id] - halfExtends, 0f))
        return deltaPos.length() - radius
    }

    override fun drawShape(pipeline: Pipeline) {
        val h = halfHeight.toDouble()
        val r = radius.toDouble()
        val xi = PI / 2
        val zi = xi * 3
        val color = getLineColor(hasPhysics)
        when (axis) {
            Axis.X -> {
                drawLine(entity, -h, -r, 0.0, +h, -r, 0.0, color)
                drawLine(entity, -h, +r, 0.0, +h, +r, 0.0, color)
                drawLine(entity, -h, 0.0, -r, +h, 0.0, -r, color)
                drawLine(entity, -h, 0.0, +r, +h, 0.0, +r, color)
                drawPartialSphere(entity, r, Vector3d(-h, 0.0, 0.0), 0.0, TAU, PI, PI, xi, PI, color)
                drawPartialSphere(entity, r, Vector3d(+h, 0.0, 0.0), 0.0, TAU, 0.0, PI, zi, PI, color)
            }
            Axis.Y -> {
                drawLine(entity, -r, -h, 0.0, -r, +h, 0.0, color)
                drawLine(entity, +r, -h, 0.0, +r, +h, 0.0, color)
                drawLine(entity, 0.0, -h, -r, 0.0, +h, -r, color)
                drawLine(entity, 0.0, -h, +r, 0.0, +h, +r, color)
                drawPartialSphere(entity, r, Vector3d(0.0, -h, 0.0), xi, PI, 0.0, TAU, PI, PI, color)
                drawPartialSphere(entity, r, Vector3d(0.0, +h, 0.0), zi, PI, 0.0, TAU, 0.0, PI, color)
            }
            Axis.Z -> {
                drawLine(entity, -r, 0.0, -h, -r, 0.0, +h, color)
                drawLine(entity, +r, 0.0, -h, +r, 0.0, +h, color)
                drawLine(entity, 0.0, -r, -h, 0.0, -r, +h, color)
                drawLine(entity, 0.0, +r, -h, 0.0, +r, +h, color)
                drawPartialSphere(entity, r, Vector3d(0.0, 0.0, -h), PI, PI, xi, PI, 0.0, TAU, color)
                drawPartialSphere(entity, r, Vector3d(0.0, 0.0, +h), 0.0, PI, zi, PI, 0.0, TAU, color)
            }
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CapsuleCollider) return
        dst.axis = axis
        dst.halfHeight = halfHeight
        dst.radius = radius
        dst.margin = margin
    }
}
