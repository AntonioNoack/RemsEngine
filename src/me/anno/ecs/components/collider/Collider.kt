package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.linearmath.Transform
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.engine.physics.BulletPhysics.Companion.convertMatrix
import org.joml.Vector3d

// todo collision-effect mappings:
//  - which listener is used
//  - whether a collision happens
//  - whether a can push b
//  - whether b can push a

abstract class Collider : Component() {

    abstract fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double

    fun createBulletCollider(base: Entity): Pair<Transform, CollisionShape> {
        val shape = createBulletShape()
        val transform = Transform(convertMatrix(entity!!.fromLocalToOtherLocal(base)))
        return transform to shape
    }

    abstract fun createBulletShape(): CollisionShape

    // a collider needs to be drawn
    override fun onDrawGUI() {
        // draw shape
        drawShape()
        // todo draw transformation gizmos for easy transforms
    }

    abstract fun drawShape()

}