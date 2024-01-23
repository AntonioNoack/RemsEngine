package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    testUI3("Scrollbar Overdraw") {
        EngineBase.enableVSync = true
        val main = ScrollPanelY(style)
        val list = main.child as PanelList
        for (i in 0 until 100) {
            list.add(TextPanel("Text $i", style))
        }
        main
    }
}