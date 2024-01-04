package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomPanelType
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    disableRenderDoc()
    testUI("Custom Container") {
        val options = UITypeLibrary(arrayListOf(
            CustomPanelType("A") { TextPanel("A", DefaultConfig.style) },
            CustomPanelType("B") { TextPanel("B", DefaultConfig.style) },
            CustomPanelType("C") { TextPanel("C", DefaultConfig.style) }
        ))
        CustomContainer(options.createDefault(), options, DefaultConfig.style)
    }
}