package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.toHexColor

class ColorPreviewField(private val refSize: Panel, val padding: Int, style: Style) : Panel(style) {

    var color = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    // transparent background can draw over borders
    override val canDrawOverBorders get() = color.a() != 255

    override fun calculateSize(w: Int, h: Int) {
        val size = refSize.minH
        minW = size
        minH = size
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        var x2 = x + padding
        var y2 = y + padding
        var x3 = x + width - 2 * padding
        var y3 = y + height - 2 * padding
        if (color.a() != 255) {
            drawTransparentBackground(x2, y2, x3 - x2, y3 - y2)
        }
        x2 = max(x0, x2)
        y2 = max(y0, y2)
        x3 = min(x1, x3)
        y3 = min(y1, y3)
        DrawRectangles.drawRect(x2, y2, x3 - x2, y3 - y2, color)
    }
}