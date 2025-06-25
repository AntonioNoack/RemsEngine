package com.bulletphysics.collision.broadphase

/**
 * Common collision filter groups,
 * and their respective logic.
 *
 * Up to 27 groups are supported, so the mask and groupId can be stored in one int32.
 */
object CollisionFilterGroups {

    const val NUM_MASK_BITS = 27
    const val ALL_MASK = (2 shl NUM_MASK_BITS) - 1

    const val DEFAULT_GROUP_ID = 0
    const val STATIC_GROUP_ID = 1
    const val KINEMATIC_GROUP_ID = 2
    const val DEBRIS_GROUP_ID = 3
    const val GHOST_GROUP_ID = 4
    const val CHARACTER_GROUP_ID = 5

    const val DEFAULT_MASK = 1 shl DEFAULT_GROUP_ID
    const val STATIC_MASK = 1 shl STATIC_GROUP_ID
    const val KINEMATIC_MASK = 1 shl KINEMATIC_GROUP_ID
    const val DEBRIS_MASK = 1 shl DEBRIS_GROUP_ID
    const val GHOST_MASK = 1 shl GHOST_GROUP_ID
    const val CHARACTER_MASK = 1 shl CHARACTER_GROUP_ID

    const val ANY_DYNAMIC_MASK = ALL_MASK and (STATIC_MASK or KINEMATIC_MASK or GHOST_MASK).inv()

    val DEFAULT_ALL = buildFilter(DEFAULT_GROUP_ID, ALL_MASK)

    fun buildFilter(group: Int, mask: Int): Int {
        return (mask and ALL_MASK) or (group shl NUM_MASK_BITS)
    }

    fun getGroupId(filter: Int): Int {
        return filter ushr NUM_MASK_BITS
    }

    fun getGroupMask(filter: Int): Int {
        return 1 shl getGroupId(filter)
    }

    fun getMask(filter: Int): Int {
        return filter and ALL_MASK
    }

    fun collides(filter0: Int, filter1: Int): Boolean {
        return getGroupMask(filter0).and(getMask(filter1)) != 0
    }

    fun collidesBidirectional(filter0: Int, filter1: Int): Boolean {
        return collides(filter0, filter1) and collides(filter1, filter0)
    }
}
