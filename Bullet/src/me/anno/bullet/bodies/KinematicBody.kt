package me.anno.bullet.bodies

import com.bulletphysics.collision.broadphase.CollisionFilterGroups.ANY_DYNAMIC_MASK
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.KINEMATIC_MASK

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