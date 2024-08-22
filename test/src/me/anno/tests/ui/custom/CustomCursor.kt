package me.anno.tests.ui.custom

import me.anno.config.DefaultConfig.style
import me.anno.gpu.Cursor
import me.anno.image.ImageCache
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.res

fun main() {
    val image = ImageCache[res.getChild("icon.png"), false]!!
    val cursor = Cursor(image.resized(32, 32, true))
    val ui = object : Panel(style) {
        override fun getCursor() = cursor
    }
    testUI3("Custom Cursor", ui)
}