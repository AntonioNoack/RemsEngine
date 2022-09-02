package me.anno.tests

import me.anno.config.DefaultConfig
import me.anno.gpu.GFXBase
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio
import me.anno.utils.OS

fun main() { // small test for custom fonts; works :)
    GFXBase.disableRenderDoc()
    TestStudio.testUI {
        val panel = TextPanel("This is some sample text", DefaultConfig.style)
        val link = OS.downloads.getChild("fonts/kids-alphabet.zip/KidsAlphabet.ttf")
        panel.font = panel.font
            .withName(link.absolutePath)
            .withSize(50f)
        panel
    }
}