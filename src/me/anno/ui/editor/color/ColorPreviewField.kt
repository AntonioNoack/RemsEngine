package me.anno.ui.editor.color

import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.shader.ShaderLib
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b01
import me.anno.utils.Color.black
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.Color.white

class ColorPreviewField(private val refSize: Panel, val padding: Int, style: Style) : Panel(style) {

    var color = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    // transparent background can draw over borders
    override val canDrawOverBorders get() = true

    override fun calculateSize(w: Int, h: Int) {
        val size = refSize.minH
        minW = size
        minH = size
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val bgColor = if (ShaderLib.y.dot(color.r01(), color.g01(), color.b01(), 0f) > 0.5f) black else white
        DrawRectangles.drawRect(x, y, width, height, bgColor)
        var x2 = x + padding
        var y2 = y + padding
        var x3 = x + width - padding
        var y3 = y + height - padding
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