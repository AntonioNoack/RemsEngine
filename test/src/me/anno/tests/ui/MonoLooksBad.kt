package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.withAlpha

fun main() {
    // spacing is all messed up
    //  -> now it's fixed for triplets at least 😅,
    // more complex cases aren't implemented yet
    val text = "mmi mim imm\n" +
            "iim imi mii\n" +
            "miim iimm\n" +
            "mmii immi"
    val font = TextPanel(style).font
    testDrawing("Messed up spacing") {
        it.clear()
        drawText(
            it.x + it.width / 2, it.y + it.height / 2, font, text, -1,
            it.backgroundColor.withAlpha(0),
            AxisAlignment.CENTER, AxisAlignment.CENTER
        )
    }
}