package com.bulletphysics.collision.broadphase

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
	var clientObject: Any? = null

    // TODO: mask
	@JvmField
	var collisionFilterGroup: Short = 0
    @JvmField
	var collisionFilterMask: Short = 0

    @JvmField
	var multiSapParentProxy: Any? = null

    /**
     * uniqueId is introduced for paircache. could get rid of this, by calculating the address offset etc.
     */
    var uid: Int = 0

    constructor()

    @JvmOverloads
    constructor(
        userPtr: Any?,
        collisionFilterGroup: Short,
        collisionFilterMask: Short,
        multiSapParentProxy: Any? = null
    ) {
        this.clientObject = userPtr
        this.collisionFilterGroup = collisionFilterGroup
        this.collisionFilterMask = collisionFilterMask
        this.multiSapParentProxy = multiSapParentProxy
    }
}
