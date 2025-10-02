package me.anno.ecs.components.collider

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.SDFUtils.and2SDFs
import me.anno.ecs.components.collider.UnionUtils.unionRing
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths.length
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

class CylinderCollider : Collider() {

    @Range(0.0, 2.0)
    @SerializedProperty
    @Docs("which axis the height is for")
    var axis = Axis.Y

    @SerializedProperty
    var halfHeight = 1f

    @SerializedProperty
    var radius = 1f

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        // union the two rings
        val h = halfHeight.toDouble()
        val r = radius.toDouble()
        unionRing(globalTransform, dstUnion, axis, r, +h)
        unionRing(globalTransform, dstUnion, axis, r, -h)
    }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        val halfHeight = halfHeight
        val radius = radius
        val circle = when (axis) {
            Axis.X -> length(deltaPos.y, deltaPos.z)
            Axis.Y -> length(deltaPos.x, deltaPos.z)
            Axis.Z -> length(deltaPos.x, deltaPos.y)
        } - radius
        val box = abs(deltaPos[axis.id]) - halfHeight
        deltaPos.x = circle
        deltaPos.y = box
        return and2SDFs(deltaPos, roundness)
    }

    override fun drawShape(pipeline: Pipeline) {
        LineShapes.drawCylinder(entity, radius.toDouble(), halfHeight.toDouble(), axis, colliderLineColor)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CylinderCollider) return
        dst.axis = axis
        dst.halfHeight = halfHeight
        dst.radius = radius
    }
}