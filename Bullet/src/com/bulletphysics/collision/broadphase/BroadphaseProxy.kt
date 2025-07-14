package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.dispatch.CollisionObject

/**
 * BroadphaseProxy is the main class that can be used with the Bullet broadphases.
 * It stores collision shape type information, collision filter information and
 * a client object, typically a [com.bulletphysics.collision.dispatch.CollisionObject] or [com.bulletphysics.dynamics.RigidBody].
 *
 * @author jezek2
 */
open class BroadphaseProxy {
    /**
     * Usually the client CollisionObject or Rigidbody class
     */
    @JvmField
    var clientObject: CollisionObject

    @JvmField
    var collisionFilter = 0

    /**
     * uniqueId is introduced for HashedOverlappingPairCache.
     */
    var uid: Int = 0

    constructor(
        userPtr: CollisionObject,
        collisionFilter: Int,
    ) {
        this.clientObject = userPtr
        this.collisionFilter = collisionFilter
    }
}
