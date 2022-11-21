package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.anim.AnimTextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.ConsoleOutputPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import kotlin.math.sin

/**
 * a test, where we open three extra windows
 * */
fun main() {
    GFXBase.disableRenderDoc()
    testUI {
        addEvent {
            for (title in listOf("your", "grace", "is welcome")) {
                val ui = PanelListY(style)
                ui.add(ConsoleOutputPanel.createConsoleWithStats(false, style))
                ui.add(AnimTextPanel.SimpleAnimTextPanel(title, style) { _, time, index, cx, cy ->
                    val s = time * 5f + index / 3f
                    AnimTextPanel.translate(0f, sin(s) * 5f)
                    AnimTextPanel.rotate(sin(s) * 0.1f, cx, cy)
                    AnimTextPanel.hsluv(time * 2f - index / 2f)
                })
                ui.setWeight2(1f)
                GFXBase.createWindow(title, ui)
            }
        }
        // UI on first window is missing / not being drawn ->
        // with addEvent{} it works :)
        AnimTextPanel.SimpleAnimTextPanel("Hi :)", style) { _, time, index, cx, cy ->
            val s = time * 5f + index / 3f
            AnimTextPanel.translate(0f, sin(s) * 5f)
            AnimTextPanel.rotate(sin(s) * 0.1f, cx, cy)
            AnimTextPanel.hsluv(time * 2f - index / 2f)
        }
    }
}
