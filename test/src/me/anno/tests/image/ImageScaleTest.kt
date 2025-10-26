package me.anno.tests.image

import me.anno.image.ImageScale.scaleMax
import me.anno.image.ImageScale.scaleMaxPreview
import me.anno.image.ImageScale.scaleMin
import me.anno.utils.assertions.assertEquals
import org.joml.Vector2i
import org.junit.jupiter.api.Test

object ImageScaleTest {
    @Test
    fun testScaleMin() {
        assertEquals(Vector2i(64, 64), scaleMin(100, 100, 64))
        assertEquals(Vector2i(64, 64), scaleMin(10, 10, 64))
        assertEquals(Vector2i(640, 64), scaleMin(100, 10, 64))
        assertEquals(Vector2i(64, 640), scaleMin(10, 100, 64))
    }

    @Test
    fun testScaleMax() {
        assertEquals(Vector2i(64, 64), scaleMax(100, 100, 64))
        assertEquals(Vector2i(64, 64), scaleMax(10, 10, 64))
        assertEquals(Vector2i(64, 6), scaleMax(100, 10, 64))
        assertEquals(Vector2i(64, 1), scaleMax(100, 1, 64))
        assertEquals(Vector2i(6, 64), scaleMax(10, 100, 64))
        assertEquals(Vector2i(1, 64), scaleMax(1, 100, 64))
    }

    @Test
    fun testScaleMaxPreview() {
        assertEquals(Vector2i(64, 64), scaleMaxPreview(100, 100, 64, 64, 5))
        assertEquals(Vector2i(64, 64), scaleMaxPreview(10, 10, 64, 64, 5))
        assertEquals(Vector2i(64, 13), scaleMaxPreview(100, 10, 64, 64, 5))
        assertEquals(Vector2i(13, 64), scaleMaxPreview(10, 100, 64, 64, 5))
    }
}