package me.anno.bugs.done

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.input.TextInputML

fun main() {
    // bug: entering tabs into text inputs is broken:
    //  - cursor is always left of it
    // test multi-line text input for tabs -> yes, works, too :)

    OfficialExtensions.initForTests()

    val font = Font("Verdana", 12f)
    val w0 = getSizeX(getTextSize(font, "text", -1, -1).waitFor()!!)
    val w1 = getSizeX(getTextSize(font, "text\t", -1, -1).waitFor()!!)
    val w2 = getSizeX(getTextSize(font, "text\t\t", -1, -1).waitFor()!!)

    println("ws: $w0, $w1, $w2")

    testUI("Tabs in TextInput", TextInputML(style))
}