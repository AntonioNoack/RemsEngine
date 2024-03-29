package me.anno.tests.ui.custom

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.fonts.Font
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.downloads

fun main() { // small test for custom fonts; works :)
    disableRenderDoc()
    testUI("Custom Fonts") {
        val panel = TextPanel("This is some sample text", DefaultConfig.style)
        panel.font = Font(downloads.getChild("fonts/kids-alphabet.zip/KidsAlphabet.ttf"), 50f)
        panel
    }
}