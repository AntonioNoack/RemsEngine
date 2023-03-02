package me.anno.tests.ui

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.GFXBase
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.progress.ProgressBarPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.debug.TestStudio.Companion.testUI2
import kotlin.concurrent.thread

fun main() {
    GFXBase.disableRenderDoc()
    if (true) {
        testUI {
            TextButton("Start", false, style)
                .addLeftClickListener {
                    val bar = GFX.someWindow.addProgressBar("Sample", "Infinity", 1.0)
                    thread {
                        while (!Engine.shutdown && !bar.isCancelled) {
                            bar.progress = (Engine.gameTimeF % 1f) * 0.99
                            Thread.sleep(10)
                        }
                        bar.finish()
                        println("Finished :)")
                    }
                }
        }
    } else {
        // test progress bar panel
        val p = ProgressBarPanel("Sample", "", 1.0, 15, style)
        val t = SpyPanel { p.progress = (Engine.gameTimeD * 5.0 / p.w) % 1.0 }
        testUI2 { listOf(t, p) }
    }
}