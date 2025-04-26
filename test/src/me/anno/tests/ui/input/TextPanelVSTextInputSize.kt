package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.TextInput

fun main() {
    // find out why TextPanel and TextInput have different heights and make them the same
    // if not well possible, add a work-around/option to make them the same size, e.g., fillY should work

    // -> because the implementation of ScrollPanelX/Y/XY depended on data from the previous frame for calculating
    // whether it needs a scrollbar, and that data - rightfully so - might be completely wrong for the current frame

    disableRenderDoc()

    val list = PanelListX(style)
    val name = NameDesc("Sample Text")
    list.add(TextPanel(name, style))
    list.add(TextInput(name, "", "Sample Value", style))

    testUI3("TextPanel VS TextInput", PanelListY(style).add(list))
}