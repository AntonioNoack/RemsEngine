package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.GFXBase
import me.anno.ui.base.Font
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio
import me.anno.utils.OS.downloads

fun main() { // small test for custom fonts; works :)
    GFXBase.disableRenderDoc()
    TestStudio.testUI {
        val panel = TextPanel("This is some sample text", DefaultConfig.style)
        panel.font = Font(downloads.getChild("fonts/kids-alphabet.zip/KidsAlphabet.ttf"), 50f)
        panel
    }
}