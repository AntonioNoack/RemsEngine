package me.anno.box2d

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.jbox2d.collision.shapes.PolygonShape
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RectCollider : Collider2d() {

    var halfExtends = Vector2f(1f)
        set(value) {
            field.set(value)
            (box2dInstance?.shape as? PolygonShape)?.setAsBox(value.x, value.y)
        }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // from SDFBox()
        val b = halfExtends
        val qx = abs(deltaPos.x) - b.x
        val qy = abs(deltaPos.y) - b.y
        val outer = Maths.length(max(0f, qx), max(0f, qy))
        val inner = min(max(qx, qy), 0f)
        return outer + inner
    }

    override fun drawShape() {
        val halfExtends = halfExtends
        val x = halfExtends.x
        val y = halfExtends.y
        val v3 = JomlPools.vec3f
        val p0 = v3.create().set(+x, +y, 0f)
        val p1 = v3.create().set(+x, -y, 0f)
        val p2 = v3.create().set(-x, -y, 0f)
        val p3 = v3.create().set(-x, +y, 0f)
        LineShapes.drawRect(entity, p0, p1, p2, p3)
        v3.sub(4)
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val halfExtends = halfExtends
        unionCube(globalTransform, aabb, tmp, halfExtends.x.toDouble(), halfExtends.y.toDouble(), 1.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as RectCollider
        dst.halfExtends.set(halfExtends)
    }

    override val className: String get() = "RectCollider"
}