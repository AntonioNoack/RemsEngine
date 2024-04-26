package me.anno.tests.ui.custom

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomPanelType
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.debug.TestEngine.Companion.testUI

fun main() {
    disableRenderDoc()
    testUI("Custom Container") {
        val options = UITypeLibrary(arrayListOf(
            CustomPanelType(NameDesc("A")) { TextPanel("A", DefaultConfig.style) },
            CustomPanelType(NameDesc("B")) { TextPanel("B", DefaultConfig.style) },
            CustomPanelType(NameDesc("C")) { TextPanel("C", DefaultConfig.style) }
        ))
        CustomContainer(options.createDefault(), options, DefaultConfig.style)
    }
}