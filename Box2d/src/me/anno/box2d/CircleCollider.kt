package me.anno.box2d

import me.anno.ecs.components.collider.UnionUtils.unionCube
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths
import org.jbox2d.collision.shapes.CircleShape
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f

class CircleCollider : Collider2d() {

    @SerializedProperty
    var radius = 1f
        set(value) {
            field = value
            (nativeInstance?.shape as? CircleShape)?.radius = value
        }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return Maths.length(deltaPos.x, deltaPos.y) - radius
    }

    override fun drawShape(pipeline: Pipeline) {
        LineShapes.drawCircle(entity, radius.toDouble(), 0, 1, 0.0, null, colliderLineColor)
    }

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        val r = radius.toDouble()
        unionCube(globalTransform, dstUnion, r, r, 1.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CircleCollider) return
        dst.radius = radius
    }
}