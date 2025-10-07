package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val text = "Folder \uD83D\uDCC1 123 \uD83D\uDE0A/\uD83D\uDE0A/" +
            "\uD83D\uDE0A/\uD83D\uDE0A/\uD83D\uDE0A/" +
            "\uD83D\uDE0A/\uD83D\uDE0A"
    val font = Font("Verdana", 50f)
    val image = FontManager.getTexture(font, text, 220, -1).waitFor()
    image!!.write(desktop.getChild("Emoji Text.png"))
}