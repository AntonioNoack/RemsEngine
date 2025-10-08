package me.anno.experiments.emojitext

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager.getTexture
import me.anno.fonts.GlyphLayout
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Strings.joinChars

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val folder = desktop.getChild("Spacing")
    folder.tryMkdirs()

    val font = Font("Verdana", 20f, true, isItalic = false)
    assertEquals(2, GlyphLayout(font, "a\nb", 0f, 10).numLines)
    assertEquals(3, GlyphLayout(font, "a b", 1e-6f, 10).numLines)
    assertEquals(1, GlyphLayout(font, "a b", 0f, 10).numLines)

    fun glToLines(str: String, rwl: Float): List<String> {
        val layout = GlyphLayout(font, str, rwl, 100)
        return (0 until layout.numLines).map { lineIndex ->
            layout.indices.filter { glyphIndex ->
                layout.getLineIndex(glyphIndex) == lineIndex
            }.map { glyphIndex ->
                layout.getCodepoint(glyphIndex)
            }.joinChars().toString()
        }
    }

    assertEquals(listOf("Hello", "World"), glToLines("Hello World", 5f))
    assertEquals(listOf("H-", "ello", "Wor"), glToLines("H-ello Wor", 2.5f))

    fun test(text: String) {
        // fixed: somewhere, there is an inconsistency...
        // (png was cut off on long texts, because text was initially measured using Java Graphics, then by OffsetCache)
        val size = getTextSize(font, text, -1, -1).waitFor()!!
        println("${getSizeX(size)} for '$text'")
        val texture = getTexture(font, text, 100, -1).waitFor()!!
        texture.write(folder.getChild(text.replace(" ", "")))
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