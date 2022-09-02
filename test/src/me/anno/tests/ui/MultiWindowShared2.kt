package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {

    GFXBase.disableRenderDoc()

    // open 3 windows using our engine, and make all of them work
    testUI {
        for (title in listOf("your", "grace", "is welcome")) {
            val ui = PanelListY(style)
            ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
            ui.add(TextPanel(title, style))
            ui.setWeight(1f)
            GFXBase.createWindow(title, ui)
        }
        // todo UI on first window is missing / not being drawn
        TextPanel("Hi :)", style)
    }

}
