package com.bulletphysics

import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.narrowphase.ManifoldPoint

/**
 * Called when existing contact between two collision objects has been processed.
 *
 * @see BulletGlobals.contactProcessedCallback
 *
 * @author jezek2
 */
interface ContactProcessedCallback {
    fun contactProcessed(cp: ManifoldPoint, body0: CollisionObject, body1: CollisionObject): Boolean
}
