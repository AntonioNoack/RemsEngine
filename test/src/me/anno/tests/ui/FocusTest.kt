package me.anno.tests.ui

import me.anno.input.Key
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.junit.jupiter.api.Test

class FocusTest : UITests() {
    @Test
    fun testIsFocussed() {

        val panel0 = SpacerPanel(10, 10, style)
        val panel1 = SpacerPanel(10, 10, style)

        val ui = PanelListY(style)
        ui.add(panel0)
        ui.add(panel1)
        prepareUI(ui)
        updateUI()

        for (i in 0 until 5) {
            val hovered = if (i.hasFlag(1)) panel1 else panel0
            val notHovered = if (i.hasFlag(1)) panel0 else panel1

            moveMouseTo(hovered)
            click(Key.BUTTON_LEFT)
            callMouseMove()
            updateUI()

            assertTrue(hovered.isHovered)
            assertTrue(hovered.isInFocus)
            assertFalse(notHovered.isHovered)
            assertFalse(notHovered.isInFocus)
        }
    }
}