package me.anno.bullet.bodies

import me.anno.ecs.components.collider.CollisionFilters.ANY_DYNAMIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.KINEMATIC_MASK

/**
 * Physics object, that should be controlled exclusively by scripts.
 * Any implementation should implement OnPhysicsUpdate to do these changes.
 *
 * A KinematicBody that doesn't move should just be replaced with a Rigidbody.
 * */
class KinematicBody : PhysicalBody() {
    init {
        collisionGroup = KINEMATIC_MASK
        collisionMask = ANY_DYNAMIC_MASK
    }
}