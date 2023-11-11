package me.anno.tests.ui

import me.anno.Engine
import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.maths.Maths
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.progress.ProgressBar
import me.anno.ui.base.progress.ProgressBarPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.debug.TestStudio.Companion.testUI2
import kotlin.concurrent.thread

fun main() {
    disableRenderDoc()
    if (true) {
        testUI("Progress Bar") {
            var offset = 0f
            // testing indeterminate mode
            val window = GFX.someWindow!!
            window.addProgressBar("Something", "Bytes", Double.NaN).progress = 123456789.0
            window.addProgressBar(object : ProgressBar("Sample", "Bytes", 1e6) {
                override fun formatProgress(): String {
                    progress = Maths.clamp(window.mouseX * 1e6 / window.width, 0.0, 0.999e6)
                    return super.formatProgress()
                }
            })
            TextButton("Start", style)
                .addLeftClickListener {
                    val bar = window.addProgressBar("Sample", "Bytes", 1e6)
                    thread {
                        val offsetI = offset++
                        while (!Engine.shutdown && !bar.isCancelled) {
                            bar.progress = (((Time.gameTime + offsetI) * 0.1) % 1.0) * 0.99 * 1e6
                            Thread.sleep(10)
                        }
                        bar.finish()
                    }
                }
        }
    } else {
        // test progress bar panel
        val p = ProgressBarPanel("Sample", "", 1.0, 15, style)
        val t = SpyPanel { p.progress = (Time.gameTime * 5.0 / p.width) % 1.0 }
        testUI2("Progress Bar") { listOf(t, p) }
    }
}