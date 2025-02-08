package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.RemsEngine.Companion.openConfigWindow
import me.anno.engine.RemsEngine.Companion.openKeymapWindow
import me.anno.engine.RemsEngine.Companion.openStylingWindow
import me.anno.gpu.RenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    RenderDoc.disableRenderDoc()
    testUI3("Config Panel") {
        val list = PanelListY(style)
        list.add(
            TextButton(NameDesc("Config"), style)
                .addLeftClickListener { openConfigWindow(it.windowStack) })
        list.add(
            TextButton(NameDesc("Style"), style)
                .addLeftClickListener { openStylingWindow(it.windowStack) })
        list.add(
            TextButton(NameDesc("KeyMap"), style)
                .addLeftClickListener { openKeymapWindow(it.windowStack) })
        list
    }
}