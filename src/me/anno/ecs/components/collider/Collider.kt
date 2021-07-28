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

    override var isEnabled: Boolean = true
        set(value) {
            if(field != value){
                field = value
                entity?.rigidbody?.invalidateRigidbody()
            }
        }

    abstract fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double

    // todo test that
    fun createBulletCollider(base: Entity, scale: Vector3d): Pair<Transform, CollisionShape> {
        val transform0 = entity!!.fromLocalToOtherLocal(base)
        // there may be extra scale hidden in there
        val extraScale = transform0.getScale(Vector3d())
        val totalScale = Vector3d(scale).mul(extraScale)
        val shape = createBulletShape(totalScale)
        val transform = Transform(convertMatrix(transform0, extraScale))
        return transform to shape
    }

    abstract fun createBulletShape(scale: Vector3d): CollisionShape

    // a collider needs to be drawn
    override fun onDrawGUI() {
        // draw shape
        drawShape()
        // todo draw transformation gizmos for easy transforms
    }

    abstract fun drawShape()

}