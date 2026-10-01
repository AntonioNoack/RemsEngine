package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.VerticalTextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.NineTilePanel
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    testUI3("Vertical Button") {
        val ui = NineTilePanel(style)
        ui.add(
            VerticalTextButton(NameDesc("Click Me"), style)
            .addLeftClickListener {
                it as VerticalTextButton
                it.rotatedLeft = !it.rotatedLeft
            }.apply {
                alignmentX = AxisAlignment.MIN
                alignmentY = AxisAlignment.CENTER
            })
        ui
    }
}