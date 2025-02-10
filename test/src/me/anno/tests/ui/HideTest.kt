package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.input.TextInput
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class HideTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
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