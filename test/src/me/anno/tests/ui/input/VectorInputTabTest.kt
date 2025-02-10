package me.anno.tests.ui.input

import me.anno.config.DefaultConfig
import me.anno.engine.OfficialExtensions
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.tests.ui.UITests
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.NumberType
import me.anno.ui.input.components.PureTextInputML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * automated test, whether pressing tab switches to the next input field
 * */
class VectorInputTabTest : UITests() {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testTabPresses() {
        OfficialExtensions.initForTests() // for text generator, which is needed for layout
        val ui = FloatVectorInput(NameDesc.EMPTY, "", NumberType.VEC4, DefaultConfig.style)
        prepareUI(ui)
        updateUI()
        val inputPanels = ui.listOfVisible
            .filterIsInstance<PureTextInputML>()
        assertEquals(ui.type.numComponents, inputPanels.size)
        inputPanels.first().requestFocus()
        for (i in 0 until 10) {
            updateUI()
            assertEquals(listOf(i % ui.type.numComponents), inputPanels.indices
                .filter { inputPanels[it].isInFocus })
            type(Key.KEY_TAB, '\t')
        }
    }
}

// manual testing:
fun main() {
    disableRenderDoc()
    testUI("VectorInputTab", FloatVectorInput(DefaultConfig.style))
}
