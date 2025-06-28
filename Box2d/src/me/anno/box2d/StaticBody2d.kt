package me.anno.box2d

import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.STATIC_MASK

class StaticBody2d: PhysicalBody2d() {
    init {
        collisionGroup = STATIC_MASK
        collisionMask = ANY_DYNAMIC_MASK
    }
}