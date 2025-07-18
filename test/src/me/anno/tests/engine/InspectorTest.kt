package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class InspectorTest {
    @Test
    @FlakyTest
    fun testIsCollapsed() {
        val instances = listOf(Entity(), MeshComponent())
        for (instance in instances) {
            val reflections = instance.getReflections()
            assertNotNull(reflections.allProperties["isCollapsed"])
            assertNotNull(reflections.allProperties["isEnabled"])
            assertTrue(reflections.editorFields.any { it.name == "isEnabled" })
        }
    }
}