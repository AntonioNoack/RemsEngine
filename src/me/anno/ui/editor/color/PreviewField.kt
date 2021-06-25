package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles
import me.anno.ui.base.Panel
import me.anno.ui.style.Style

class PreviewField(private val refSize: Panel, val padding: Int, style: Style) : Panel(style) {

    // todo show alpha with chess field

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
        DrawRectangles.drawRect(x + padding, y + padding, w - 2 * padding, h - 2 * padding, color)
    }
}