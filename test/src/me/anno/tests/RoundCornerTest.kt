package me.anno.tests

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.Panel
import me.anno.ui.Panel.Companion.CORNER_TOP_LEFT
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.types.Booleans.toInt

fun main() {
    val panel = Panel(style)
    panel.x = 10
    panel.y = 10
    panel.w = 210
    panel.h = 60
    panel.backgroundRadius = 15f
    panel.backgroundRadiusCorners = CORNER_TOP_LEFT
    testDrawing {
        for (y in 0 until panel.h + panel.y * 2) {
            for (x in 0 until panel.w + panel.x * 2) {
                val c = -panel.isOpaqueAt(x, y).toInt()
                drawRect(x + it.x, y + it.y, 1, 1, c)
            }
        }
    }
}