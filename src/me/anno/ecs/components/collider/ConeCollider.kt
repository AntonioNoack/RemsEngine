package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.LineShapes.drawCone
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector2f.Companion.lengthSquared
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sign
import kotlin.math.sqrt

class ConeCollider : Collider() {

    @Range(0.0, 2.0)
    @SerializedProperty
    var axis = Axis.Y

    @SerializedProperty
    var height = 2f

    @SerializedProperty
    var radius = 1f

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        // union the peak and the bottom ring
        val h = height * 0.5
        val r = radius.toDouble()
        unionRing(globalTransform, dstUnion, tmp, axis, r, -h)
        tmp[axis.id] = +h
        dstUnion.union(globalTransform.transformPosition(tmp))
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {

        val roundness = roundness
        val h = -height + roundness * 2f
        val dist1D = deltaPos[axis.id] + h * 0.5f // centering
        val dist2D = when (axis) {
            Axis.X -> length(deltaPos.y, deltaPos.z)
            Axis.Y -> length(deltaPos.x, deltaPos.z)
            Axis.Z -> length(deltaPos.x, deltaPos.y)
        }

        // todo how can we include roundness here?

        val r = radius - roundness
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

    override fun drawShape(pipeline: Pipeline) {
        // todo check whether they are correct (the same as the physics behaviour)
        val matrix = when (axis) {
            Axis.X -> LineShapes.zToX
            Axis.Y -> LineShapes.zToY
            Axis.Z -> null
        }
        val color = getLineColor(hasPhysics)
        val radius = radius.toDouble()
        drawCone(entity, radius, radius, height.toDouble(), 0.0, matrix, color)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ConeCollider) return
        dst.axis = axis
        dst.height = height
        dst.radius = radius
    }
}