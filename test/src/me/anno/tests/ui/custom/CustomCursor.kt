package me.anno.tests.ui.custom

import me.anno.config.DefaultConfig.style
import me.anno.gpu.Cursor
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.ui.Panel
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    val image = ImageCache[getReference("res://icon.png"), false]!!
    val cursor = Cursor(image.resized(32, 32, true))
    val ui = object : Panel(style) {
        override fun getCursor() = cursor
    }
    testUI3("Custom Cursor", ui)
}