package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager.getTexture
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val folder = desktop.getChild("Spacing")
    folder.tryMkdirs()

    val font = Font("Verdana", 20f, true, isItalic = false)
    fun test(text: String) {
        // todo somewhere, there is a inconsistency...
        val size = getTextSize(font, text, -1, -1).waitFor()!!
        println("${getSizeX(size)} for '$text'")
        val texture = getTexture(font,text,-1,-1).waitFor()!!
        texture.write(folder.getChild(text.replace(" ","")))
    }

    test("1.png")
    test("1 2.png")
    test("1 2 3.png")
    test("1 2 3 4.png")
    test("1 2 3 4 5.png")
    test("1 2 3 4 5 6.png")
    test("1 2 3 4 5 6 7.png")
    test("1 2 3 4 5 6 7 8.png")
    test("1 2 3 4 5 6 7 8 9.png")
}