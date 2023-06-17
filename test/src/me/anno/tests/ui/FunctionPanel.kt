package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase.disableRenderDoc
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.utils.FunctionPanel
import kotlin.math.sin

fun main() {
    disableRenderDoc()
    testUI3 {
        FunctionPanel({ x -> sin(x * x) / x }, style)
    }
}