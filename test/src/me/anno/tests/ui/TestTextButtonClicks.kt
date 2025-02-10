package me.anno.tests.ui

import me.anno.input.Key
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

// todo why do sometimes buttons need two clicks??
//  - probably first for focus, then for click, but why is the focus elsewhere???
class TestTextButtonClicks : UITests() {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun needsOnlySingleClick() {
        val ui = PanelListY(style)
        val clickCounter = IntArray(3)
        val expectedCounter = IntArray(clickCounter.size)
        for (i in clickCounter.indices) {
            ui.add(TextButton(style).fill(1f).addLeftClickListener { clickCounter[i]++ })
        }
        prepareUI(ui)
        updateUI()
        for (i in 0 until 10) {
            // prepare
            val pi = i % clickCounter.size
            val expected = ui.children[pi]
            expectedCounter[pi]++

            // execute
            moveMouseTo(expected)
            click(Key.BUTTON_LEFT)

            // check
            assertEquals(i + 1, clickCounter.sum())
            assertContentEquals(expectedCounter, clickCounter)
        }
    }
}