package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val text = "\uD83D\uDCC1"
    val font = Font("Verdana", 50f)
    val image = FontManager.getTexture(font, text, 220, -1).waitFor()
    image!!.write(desktop.getChild("Emoji Text2.png"))
}