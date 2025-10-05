package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val text = "Folder \uD83D\uDCC1 123"
    val image = FontManager.getTexture(Font("Verdana", 50f), text, -1, -1)
        .waitFor()!!
    image.write(desktop.getChild("Emoji Text.png"))
}