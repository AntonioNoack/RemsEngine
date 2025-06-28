package com.bulletphysics

import me.anno.ecs.components.collider.CollisionFilters.ALL_MASK
import me.anno.ecs.components.collider.CollisionFilters.DEFAULT_ALL
import me.anno.ecs.components.collider.CollisionFilters.KINEMATIC_GROUP_ID
import me.anno.ecs.components.collider.CollisionFilters.KINEMATIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.NUM_GROUPS
import me.anno.ecs.components.collider.CollisionFilters.STATIC_GROUP_ID
import me.anno.ecs.components.collider.CollisionFilters.STATIC_MASK
import me.anno.ecs.components.collider.CollisionFilters.createFilter
import me.anno.ecs.components.collider.CollisionFilters.collides
import me.anno.ecs.components.collider.CollisionFilters.getGroupId
import me.anno.ecs.components.collider.CollisionFilters.getMask
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
                val filter = createFilter(groupId, mask)
                assertEquals(groupId, getGroupId(filter))
                assertEquals(mask, getMask(filter))
            }
        }
    }

    @Test
    fun testDynamicStaticCollisions() {
        val dynamic = DEFAULT_ALL
        val static = createFilter(STATIC_GROUP_ID, ALL_MASK and (STATIC_MASK or KINEMATIC_MASK).inv())
        val kinematic = createFilter(KINEMATIC_GROUP_ID, ALL_MASK and (STATIC_MASK or KINEMATIC_MASK).inv())

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