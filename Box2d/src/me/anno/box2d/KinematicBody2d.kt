package me.anno.box2d

import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.KINEMATIC_MASK

class KinematicBody2d : PhysicalBody2d() {
    init {
        collisionGroup = KINEMATIC_MASK
        collisionMask = ANY_DYNAMIC_MASK
    }
}