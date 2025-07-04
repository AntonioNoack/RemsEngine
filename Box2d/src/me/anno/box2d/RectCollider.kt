package me.anno.box2d

import me.anno.ecs.components.collider.UnionUtils.unionCube
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.jbox2d.collision.shapes.PolygonShape
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RectCollider : Collider2d() {

    var halfExtents = Vector2f(1f)
        set(value) {
            field.set(value)
            (nativeInstance?.shape as? PolygonShape)?.setAsBox(value.x, value.y)
        }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // from SDFBox()
        val b = halfExtents
        val qx = abs(deltaPos.x) - b.x
        val qy = abs(deltaPos.y) - b.y
        val outer = Maths.length(max(0f, qx), max(0f, qy))
        val inner = min(max(qx, qy), 0f)
        return outer + inner
    }

    override fun drawShape(pipeline: Pipeline) {
        val halfExtents = halfExtents
        val x = halfExtents.x
        val y = halfExtents.y
        val v3 = JomlPools.vec3f
        val p0 = v3.create().set(+x, +y, 0f)
        val p1 = v3.create().set(+x, -y, 0f)
        val p2 = v3.create().set(-x, -y, 0f)
        val p3 = v3.create().set(-x, +y, 0f)
        LineShapes.drawRect(entity, p0, p1, p2, p3, colliderLineColor)
        v3.sub(4)
    }

    override fun union(globalTransform: Matrix4x3, dstUnion: AABBd, tmp: Vector3d) {
        val halfExtents = halfExtents
        unionCube(globalTransform, dstUnion, halfExtents.x.toDouble(), halfExtents.y.toDouble(), 1.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is RectCollider) return
        dst.halfExtents.set(halfExtents)
    }
}