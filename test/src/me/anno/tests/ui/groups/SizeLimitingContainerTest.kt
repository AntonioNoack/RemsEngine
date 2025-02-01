package me.anno.tests.ui.groups

import me.anno.config.DefaultConfig.style
import me.anno.ui.base.groups.SizeLimitingContainer
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class SizeLimitingContainerTest {
    @Test
    fun testSmallEnough() {
        val child = ListAlignmentTest.ExactSizePanel(50, 52)
        val container = SizeLimitingContainer(child, 60, 70, style)
        container.calculateSize(100,100)
        assertEquals(50, container.minW)
        assertEquals(52, container.minH)
    }

    @Test
    fun testTooBig() {
        val child = ListAlignmentTest.ExactSizePanel(100, 100)
        val container = SizeLimitingContainer(child, 60, 70, style)
        container.calculateSize(100,100)
        assertEquals(60, container.minW)
        assertEquals(70, container.minH)
    }
}