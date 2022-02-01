package me.anno.ecs.components.collider.twod

import com.bulletphysics.collision.shapes.CollisionShape
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.collider.Collider
import me.anno.io.serialization.SerializedProperty
import org.jbox2d.collision.shapes.Shape
import org.joml.Matrix4x3d
import org.joml.Vector2f
import org.joml.Vector3d

// todo colliders by meshes in 2d,
// todo colliders by points in 2d
// todo colliders by images?

abstract class Collider2d : Collider() {

    @Range(0.0, 1e100)
    @SerializedProperty
    var density = 1f

    @Range(0.0, 1.0)
    @SerializedProperty
    var friction = 0.2f

    fun createBox2dCollider(base: Entity, scale: Vector2f): Pair<Matrix4x3d, Shape> {
        val transform0 = entity!!.fromLocalToOtherLocal(base)
        // there may be extra scale hidden in there
        val extraScale = transform0.getScale(Vector3d())
        val totalScale = Vector2f(scale).mul(extraScale.x.toFloat(), extraScale.y.toFloat())
        val shape = createBox2dShape(totalScale)
        transform0.scale(1.0 / extraScale.x, 1.0 / extraScale.y, 1.0 / extraScale.z)
        return transform0 to shape
    }

    abstract fun createBox2dShape(scale: Vector2f): Shape

    override fun createBulletShape(scale: Vector3d): CollisionShape {
        TODO("2d colliders are not yet supported for 3d physics")
    }

}