package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.FunctionPanelNd
import kotlin.math.sin

fun main() {
    disableRenderDoc()
    testUI3("Function Panel") {
        FunctionPanelNd({ x -> sin(x * x) / x }, style)
    }
}