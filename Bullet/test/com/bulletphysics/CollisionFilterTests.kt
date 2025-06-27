package com.bulletphysics

import com.bulletphysics.collision.broadphase.CollisionFilterGroups.ALL_MASK
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.DEFAULT_ALL
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.KINEMATIC_GROUP_ID
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.KINEMATIC_MASK
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.NUM_GROUPS
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.STATIC_GROUP_ID
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.STATIC_MASK
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.buildFilter
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.collides
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.getGroupId
import com.bulletphysics.collision.broadphase.CollisionFilterGroups.getMask
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CollisionFilterTests {
    @Test
    fun testCreatingFilters() {
        val random = Random(1324)
        for (groupId in 0 until NUM_GROUPS) {
            repeat(10) {
                val mask = random.nextInt() and ALL_MASK
                val filter = buildFilter(groupId, mask)
                assertEquals(groupId, getGroupId(filter))
                assertEquals(mask, getMask(filter))
            }
        }
    }

    @Test
    fun testDynamicStaticCollisions() {
        val dynamic = DEFAULT_ALL
        val static = buildFilter(STATIC_GROUP_ID, ALL_MASK and (STATIC_MASK or KINEMATIC_MASK).inv())
        val kinematic = buildFilter(KINEMATIC_GROUP_ID, ALL_MASK and (STATIC_MASK or KINEMATIC_MASK).inv())

        val filters = intArrayOf(dynamic, static, kinematic)
        for (i in filters.indices) {
            for (j in filters.indices) {
                val filterI = filters[i]
                val filterJ = filters[j]
                val expected = filterI == dynamic || filterJ == dynamic
                assertEquals(expected, collides(filterI, filterJ))
            }
        }
    }
}