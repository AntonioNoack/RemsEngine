package me.anno.tests.utils

import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.Panel
import me.anno.ui.Panel.Companion.CORNER_BOTTOM_RIGHT
import me.anno.ui.Panel.Companion.CORNER_TOP_LEFT
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.types.Booleans.toInt

fun main() {
    disableRenderDoc()
    val panel = Panel(style)
    panel.x = 10
    panel.y = 50
    panel.width = 410
    panel.height = 120
    panel.background.radius = 50f
    panel.background.color = -1
    testDrawing("Round Corners") {
        panel.drawBackground(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height)
        for (y in 0 until panel.height + panel.y * 2) {
            for (x in 0 until panel.width + panel.x * 2) {
                val c = -panel.isOpaqueAt(x, y).toInt()
                drawRect(x + it.x, y + it.y + panel.height, 1, 1, c)
            }
        }
    }
}