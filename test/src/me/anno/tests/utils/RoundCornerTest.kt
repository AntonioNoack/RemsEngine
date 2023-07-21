package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFXBase
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.Panel
import me.anno.ui.Panel.Companion.CORNER_TOP_LEFT
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.types.Booleans.toInt

fun main() {
    GFXBase.disableRenderDoc()
    val panel = Panel(style)
    panel.x = 10
    panel.y = 10
    panel.width = 210
    panel.height = 60
    panel.backgroundRadius = 15f
    panel.backgroundRadiusCorners = CORNER_TOP_LEFT
    testDrawing("Round Corners") {
        for (y in 0 until panel.height + panel.y * 2) {
            for (x in 0 until panel.width + panel.x * 2) {
                val c = -panel.isOpaqueAt(x, y).toInt()
                drawRect(x + it.x, y + it.y, 1, 1, c)
            }
        }
    }
}