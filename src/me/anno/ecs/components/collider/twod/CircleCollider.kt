package me.anno.ecs.components.collider.twod

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.SphereShape
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawCircle
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.length
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.Shape
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f

class CircleCollider : Collider2d() {

    @SerializedProperty
    var radius = 1f
        set(value) {
            field = value
            // could we update this at runtime? would need to update mass & inertia
            invalidateRigidbody()
        }

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        return length(deltaPos.x, deltaPos.y) - radius
    }

    override fun createBox2dShape(): Shape {
        val shape = CircleShape()
        shape.radius = radius
        return shape
    }

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        return SphereShape(radius * scale.dot(0.33, 0.34, 0.33))
    }

    override fun drawShape() {
        drawCircle(entity, radius.toDouble(), 0, 1, 0.0)
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val r = radius.toDouble()
        unionCube(globalTransform, aabb, tmp, r, r, 1.0)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CircleCollider
        dst.radius = radius
    }

    override val className: String get() = "CircleCollider"

}