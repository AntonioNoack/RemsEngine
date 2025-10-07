package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    // why is the text grayscale -> because Java doesn't support ARGB for subpixel-text
    // todo bug: why do the exported textures have an alpha channel???

    val boldText = "Some Bold Text"
    val boldFont = Font("Verdana", 50f, isBold = true, isItalic = false)
    val boldImage = FontManager.getTexture(boldFont, boldText, 220, -1).waitFor()
    boldImage!!.write(desktop.getChild("BoldTexture.png"), false, withAlpha = false)

    val normalText = "Some Normal Text"
    val normalFont = boldFont.withBold(false)
    val normalImage = FontManager.getTexture(normalFont, normalText, 220, -1).waitFor()
    normalImage!!.write(desktop.getChild("NormalTexture.png"),false, withAlpha = false)
}