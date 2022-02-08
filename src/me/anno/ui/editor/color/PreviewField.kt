package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.ui.Panel
import me.anno.ui.style.Style

class PreviewField(private val refSize: Panel, val padding: Int, style: Style) : Panel(style) {

    var color = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override fun calculateSize(w: Int, h: Int) {
        val size = refSize.minH
        minW = size
        minH = size
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val x2 = x + padding
        val y2 = y + padding
        val w2 = w - 2 * padding
        val h2 = h - 2 * padding
        drawTransparentBackground(x2, y2, w2, h2)
        DrawRectangles.drawRect(x2, y2, w2, h2, color)
    }
}