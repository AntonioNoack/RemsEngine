package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Codepoints.codepoints
import me.anno.jvm.HiddenOpenGLContext
import me.anno.jvm.emojis.EmojiCache.getEmojiImage
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val text = "\uD83D\uDCC1"
    val codepoints = text.codepoints()
    val image = getEmojiImage(codepoints.toList(), 64)
        .waitFor()!!
    image.write(desktop.getChild("Emoji.png"))
}