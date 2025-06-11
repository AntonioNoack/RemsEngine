package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphasePair
import com.bulletphysics.collision.broadphase.DispatcherInfo

/**
 * Callback for overriding collision filtering and more fine-grained control over
 * collision detection.
 *
 * @see CollisionDispatcher.setNearCallback
 * @see CollisionDispatcher.getNearCallback
 *
 * @author jezek2
 */
interface NearCallback {
    fun handleCollision(collisionPair: BroadphasePair, dispatcher: CollisionDispatcher, dispatchInfo: DispatcherInfo)
}
