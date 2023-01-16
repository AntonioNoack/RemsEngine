package me.anno.tests.ui

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.progress.ProgressBarPanel
import me.anno.ui.debug.TestStudio.Companion.testUI2

fun main() {
    GFXBase.disableRenderDoc()
    val p = ProgressBarPanel("", 1.0, 15, style)
    val t = SpyPanel { p.progress = (Engine.gameTimeD * 5.0 / p.w) % 1.0 }
    testUI2 { listOf(t, p) }
}