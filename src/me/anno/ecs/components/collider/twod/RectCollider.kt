package me.anno.ecs.components.collider.twod

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawRect
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.Shape
import org.joml.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RectCollider : Collider2d() {

    var halfExtends = Vector2f(1f)
        set(value) {
            field.set(value)
            (box2dInstance?.shape as? PolygonShape)
                ?.setAsBox(value.x, value.y)
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

    override fun createBox2dShape(): Shape {
        val shape = PolygonShape()
        val halfExtends = halfExtends
        shape.setAsBox(halfExtends.x, halfExtends.y)
        return shape
    }

    override fun drawShape() {
        val halfExtends = halfExtends
        val x = halfExtends.x
        val y = halfExtends.y
        val p0 = JomlPools.vec3f.create().set(+x, +y, 0f)
        val p1 = JomlPools.vec3f.create().set(+x, -y, 0f)
        val p2 = JomlPools.vec3f.create().set(-x, -y, 0f)
        val p3 = JomlPools.vec3f.create().set(-x, +y, 0f)
        drawRect(entity, p0, p1, p2, p3)
        JomlPools.vec3f.sub(4)
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val halfExtends = halfExtends
        unionCube(globalTransform, aabb, tmp, halfExtends.x.toDouble(), halfExtends.y.toDouble(), 1.0)
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        val halfExtends = halfExtends
        return BoxShape(
            javax.vecmath.Vector3d(
                halfExtends.x * scale.x,
                halfExtends.y * scale.y,
                scale.z
            )
        )
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as RectCollider
        dst.halfExtends.set(halfExtends)
    }

    override val className: String get() = "RectCollider"

}