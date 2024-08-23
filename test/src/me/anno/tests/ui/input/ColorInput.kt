package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.ColorInput
import me.anno.utils.Color.black4

fun main() {
    disableRenderDoc()
    testUI("Color Input") {
        val list = PanelListY(style)
        list.add(ColorInput(NameDesc("Enabled"), "", black4, false, style))
        list.add(ColorInput(NameDesc("Disabled"), "", black4, false, style).apply { isInputAllowed = false })
        list
    }
}