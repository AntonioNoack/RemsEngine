package me.anno.tests.engine

import me.anno.ecs.components.camera.Camera
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraPropertyTest {
    @Test
    fun testPerspectivePropertyIsSerialized() {
        val sample = Camera()
        val reflect = Saveable.getReflections(sample)
        val property = assertNotNull(reflect["isPerspective"])
        assertTrue(property.serialize)
        assertEquals(true, property.forceSaving) // is a bool
    }
}