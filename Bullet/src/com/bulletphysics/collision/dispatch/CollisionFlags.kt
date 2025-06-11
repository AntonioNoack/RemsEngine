package com.bulletphysics.collision.dispatch

/**
 * Flags for collision objects.
 *
 * @author jezek2
 */
object CollisionFlags {
    /** Sets this collision object as static.  */
    const val STATIC_OBJECT: Int = 1

    /** Sets this collision object as kinematic.  */
    const val KINEMATIC_OBJECT: Int = 2

    /** Disables contact response.  */
    const val NO_CONTACT_RESPONSE: Int = 4

    /**
     * Enables calling [com.bulletphysics.ContactAddedCallback] for collision objects. This
     * allows per-triangle material (friction/restitution).
     */
    const val CUSTOM_MATERIAL_CALLBACK: Int = 8

    @Suppress("unused")
    const val CHARACTER_OBJECT: Int = 16
}
