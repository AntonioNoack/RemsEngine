package me.anno.tests.ui

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

fun main() {
    // todo check how tall text-textures are
    HiddenOpenGLContext.createOpenGL()
    OfficialExtensions.initForTests()

    val font = Font("Verdana", 20f)
    fun test(text: String) {
        val texture = FontManager.getTexture(font, text, -1, -1, false)!!
        println("'$text': $texture")
    }

    test("a")
    test("_")
    test("g")
    test("y")
    test("ᴛᴇsᴛ")
    test("ᵗᵉˢᵗ")
    test("ₜₑₛₜ")

    // test size ratio
    val frc = FontRenderContext(null, true, true)
    val layout = TextLayout("Test", java.awt.Font("Verdana", 0, 20), frc)
    println("${font.size}, ${layout.ascent} + ${layout.descent}")
}