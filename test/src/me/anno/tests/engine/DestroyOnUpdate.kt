package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.UpdateSystem
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import org.junit.jupiter.api.Test

class DestroyOnUpdateTest : Component(), OnUpdate {

    @Test
    fun testRemoval() {
        val components = (0 until 4).map {
            DestroyOnUpdateTest()
        }
        val scene = Entity()
        val sub = Entity(scene)
        for (c in components) sub.add(c)
        Systems.world = scene // all components get registered
        assertEquals(components.size, UpdateSystem.numRegisteredInstances)
        Systems.onUpdate() // all components get removed, and unregistered
        assertEquals(0, UpdateSystem.numRegisteredInstances)
        assertNull(scene.getComponentInChildren(DestroyOnUpdateTest::class))
    }

    override fun onUpdate() {
        entity?.destroy()
    }
}

