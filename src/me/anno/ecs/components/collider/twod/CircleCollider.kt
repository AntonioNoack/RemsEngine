package me.anno.ecs.components.collider.twod

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawCircle
import me.anno.io.serialization.SerializedProperty
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.Shape
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.abs

class CircleCollider : Collider2d() {

    @SerializedProperty
    var radius = 1f

    override fun createBox2dShape(scale: Vector2f): Shape {
        val shape = CircleShape()
        shape.radius = radius * (abs(scale.x) + abs(scale.y)) * 0.5f
        return shape
    }

    override fun drawShape() {
        drawCircle(entity, radius.toDouble(), 0, 1, 0.0)
    }

    override fun clone(): CircleCollider {
        val clone = CircleCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CircleCollider
        clone.radius = radius
    }

}