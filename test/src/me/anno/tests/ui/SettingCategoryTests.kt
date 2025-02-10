package me.anno.tests.ui

import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.editor.SettingCategory
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class SettingCategoryTests : UITests() {
    @Test // todo why do we then still get lag??
    @Execution(ExecutionMode.SAME_THREAD)
    fun testCollapseCallsInvalidateLayout() {
        for (withScrollbar in listOf(true, false)) {
            var ctr = 0
            val tested = SettingCategory(NameDesc("Test"), withScrollbar, style)
            val container = object : PanelContainer(tested, Padding.Zero, style) {
                override fun invalidateLayout() {
                    super.invalidateLayout()
                    ctr++
                }
            }
            prepareUI(container)
            updateUI()

            ctr = 0
            moveMouseTo(tested.titlePanel)
            click(Key.BUTTON_LEFT)

            assertEquals(1, ctr)
        }
    }
}