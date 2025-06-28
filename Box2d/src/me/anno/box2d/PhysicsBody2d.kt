package me.anno.box2d

import me.anno.ecs.Entity
import me.anno.ecs.EntityPhysics.getPhysics
import me.anno.ecs.components.physics.PhysicsBodyBase
import org.jbox2d.dynamics.Body

abstract class PhysicsBody2d : PhysicsBodyBase<Body>() {
    override fun invalidatePhysics(entity: Entity) {
        getPhysics(Box2dPhysics::class)
            ?.invalidate(entity)
    }
}