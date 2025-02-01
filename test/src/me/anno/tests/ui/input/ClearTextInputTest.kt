package me.anno.tests.ui.input

import me.anno.engine.EngineActions
import me.anno.input.ActionManager
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.tests.ui.UITests
import me.anno.ui.input.FileInput
import me.anno.utils.OS.home
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class ClearTextInputTest : UITests() {
    @Test
    fun testClearText() {

        // register everything we need
        EngineActions.register()
        ActionManager.init()

        val root = home
        val input1 = FileInput(NameDesc("Source"), style, root, emptyList())
        prepareUI(input1)
        updateUI()

        assertEquals(home.toLocalPath(), input1.base2.value)

        moveMouseTo(input1)
        click(Key.BUTTON_LEFT)
        updateUI()

        hold(Key.KEY_LEFT_CONTROL)
        updateUI()
        skipTime(0.01)
        type(Key.KEY_A)
        release(Key.KEY_LEFT_CONTROL)
        type(Key.KEY_DELETE)

        assertEquals("", input1.base2.value)
    }
}