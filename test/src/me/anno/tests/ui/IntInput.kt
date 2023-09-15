package me.anno.tests.ui

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.input.Key
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.input.IntInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

fun main() {
    GFXBase.disableRenderDoc()
    testUI("IntInput", IntInput("Quality", "", Type.VIDEO_QUALITY_CRF, style))
}

class IntInputTests : UITests() {
    @Test
    fun typingChangesValue() {

        val test = IntInput("Quality", "", Type.VIDEO_QUALITY_CRF, style)
        prepareUI(test)

        test.setValue(20, false)
        assertEquals(20L, test.value)
        assertEquals("20", test.inputPanel.value)
        test.inputPanel.requestFocus()

        press(Key.KEY_KP_ADD, '+')
        assertEquals("20+", test.inputPanel.value)

        press(Key.KEY_5, '5')
        assertEquals("20+5", test.inputPanel.value)
        assertEquals(25L, test.value)

        press(Key.KEY_ENTER)
        assertEquals("25", test.inputPanel.value)
        assertEquals(25L, test.value)
    }
}