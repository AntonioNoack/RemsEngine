package me.anno.bullet.bodies

import com.bulletphysics.collision.broadphase.CollisionFilterGroups.ANY_DYNAMIC_MASK
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.STATIC_MASK

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