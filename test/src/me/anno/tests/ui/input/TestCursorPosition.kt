package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.input.components.PureTextInput

fun main() {
    // the cursor was incorrectly positioned, because
    // - our cache access was async
    // - our cache key was created from a StringBuilder
    // - StringBuilder isn't cacheable -> key wasn't cacheable
    // -> our cache never returned a value
    disableRenderDoc()
    OfficialExtensions.initForTests()
    val ui = PanelListY(style).add(PureTextInput(style))
    println(getSizeY(getTextSize(Font("Verdana", 20), "x",-1,-1)))
    testUI3("Incorrect Cursor", ui)
}