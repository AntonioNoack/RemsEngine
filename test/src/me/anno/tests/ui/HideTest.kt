package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.input.TextInput
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class HideTest {
    @Test
    fun testHide() {
        val panel = TextInput(style)
        assertTrue(panel.isVisible)
        assertTrue(panel.isEnabled)
        panel.hide()
        assertFalse(panel.isVisible)
        assertFalse(panel.isEnabled)
        panel.show()
        assertTrue(panel.isVisible)
        assertTrue(panel.isVisible)
    }
}