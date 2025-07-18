package me.anno.bullet.bodies

import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.STATIC_MASK

/**
 * A rigidbody that never moves or rotates.
 * Not by scripts not by interactions. Usually used for undestructible things like the ground and walls.
 * */
class StaticBody : PhysicalBody() {
    init {
        collisionGroup = STATIC_MASK
        collisionMask = ANY_DYNAMIC_MASK
    }
}