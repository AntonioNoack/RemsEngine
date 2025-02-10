package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityUtils.setContains
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.UpdateSystem
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.random.Random

class OnUpdateTest {

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testOnlyAddedInstancesAreUpdated() {
        val entity = Entity()
        Systems.world = entity

        val wasUpdated = HashSet<Int>()
        val components = (0 until 10).map { id ->
            object : Component(), OnUpdate {
                override fun onUpdate() {
                    assertTrue(wasUpdated.add(id))
                }
            }
        }

        val random = Random(2134)
        val isAdded = BooleanArray(components.size)
        for (i in 0 until 100) {
            // add/remove random component
            val id = random.nextInt(components.size)
            entity.setContains(components[id], !isAdded[id])
            isAdded[id] = !isAdded[id]

            // calculate what we expect
            val toBeUpdated = isAdded.toList()
                .mapIndexedNotNull { index, value -> if (value) index else null }
                .toHashSet()

            // execute update
            wasUpdated.clear()
            Systems.onUpdate()

            // compare
            assertEquals(toBeUpdated, wasUpdated)
        }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testOnUpdateIsExecutedAndSorted() {

        val entity = Entity()
        Systems.world = entity

        var ctr = 0
        // these cannot be put in a loop, because the classes need to be different from each other
        entity.add(object : Component(), OnUpdate {
            override fun priority(): Int = 3
            override fun onUpdate() {
                assertEquals(3, ctr++)
            }
        })
        entity.add(object : Component(), OnUpdate {
            override fun priority(): Int = 1
            override fun onUpdate() {
                assertEquals(1, ctr++)
            }
        })
        entity.add(object : Component(), OnUpdate {
            override fun priority(): Int = 0
            override fun onUpdate() {
                assertEquals(0, ctr++)
            }
        })
        entity.add(object : Component(), OnUpdate {
            override fun priority(): Int = 2
            override fun onUpdate() {
                assertEquals(2, ctr++)
            }
        })

        // everything should run once
        Systems.onUpdate()
        assertEquals(4, ctr)

        // clear -> nothing more should run
        Systems.world = Entity()
        Systems.onUpdate()
        assertEquals(4, ctr)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testRegisterAndUnregisterSystem() {

        val entity = Entity()
        Systems.world = entity

        var ctr = 0
        entity.add(object : Component(), OnUpdate {
            override fun onUpdate() {
                assertEquals(0, ctr++)
            }
        })

        fun checkIsUpdated() {
            // check that ctr is incremented
            for (i in 0 until 7) {
                assertEquals(0, ctr)
                Systems.onUpdate()
                assertEquals(1, ctr)
                ctr = 0
            }
        }

        fun checkIsNotUpdated() {
            // ctr must not be updated
            for (i in 0 until 7) {
                Systems.onUpdate()
                assertEquals(0, ctr)
            }
        }

        checkIsUpdated()

        val removed = Systems.unregisterSystem(UpdateSystem)
        assertSame(UpdateSystem, removed)

        checkIsNotUpdated()

        // register system again (for this test and other tests)
        Systems.registerSystem(UpdateSystem)
        checkIsUpdated()
    }
}