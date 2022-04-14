package me.anno.ecs.components.collider.twod

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawLine
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.Shape
import org.joml.Vector2f

class RectCollider : Collider2d() {

    var halfExtends = Vector2f(1f)

    override fun createBox2dShape(scale: Vector2f): Shape {
        val shape = PolygonShape()
        shape.setAsBox(halfExtends.x, halfExtends.y)
        return shape
    }

    override fun drawShape() {
        val x = halfExtends.x.toDouble()
        val y = halfExtends.y.toDouble()
        drawLine(entity, +x, +y, 0.0, -x, +y, 0.0)
        drawLine(entity, +x, -y, 0.0, -x, -y, 0.0)
        drawLine(entity, +x, +y, 0.0, +x, -y, 0.0)
        drawLine(entity, +x, +y, 0.0, -x, -y, 0.0)
    }

    override fun clone(): RectCollider {
        val clone = RectCollider()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as RectCollider
        clone.halfExtends.set(halfExtends)
    }

}