package com.bulletphysics

/**
 * Called when contact has been destroyed between two collision objects.
 *
 * @see BulletGlobals.contactDestroyedCallback
 *
 * @author jezek2
 */
fun interface ContactDestroyedCallback {
    fun contactDestroyed(userPersistentData: Any): Boolean
}
