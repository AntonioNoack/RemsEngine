package me.anno.tests.ui.input

import me.anno.ui.input.NumberType
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.input.Key
import me.anno.tests.ui.UITests
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.IntInput
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

fun main() {
    disableRenderDoc()
    testUI("IntInput", IntInput("Quality", "", NumberType.VIDEO_QUALITY_CRF, style))
}

class IntInputTests : UITests() {
    @Test
    fun typingChangesValue() {

        LogManager.disableLogger("SimpleExpressionParser")

        val test = IntInput("Quality", "", NumberType.VIDEO_QUALITY_CRF, style)
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