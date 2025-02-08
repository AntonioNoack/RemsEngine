package me.anno.ui.editor.color

import me.anno.gpu.Cursor
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures.drawTransparentBackground
import me.anno.gpu.shader.ShaderLib
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b01
import me.anno.utils.Color.black
import me.anno.utils.Color.g01
import me.anno.utils.Color.mixARGB
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

    override fun getCursor() = Cursor.hand
    override fun calculateSize(w: Int, h: Int) {
        val size = refSize.minH
        minW = size
        minH = size
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val bgColor = if (ShaderLib.y.dot(color.r01(), color.g01(), color.b01(), 0f) > 0.5f) black else white
        val size = min(width, height)
        DrawRectangles.drawRect(
            x + padding - 1, y + padding - 1,
            size - 2, size - 2,
            mixARGB(backgroundColor, bgColor, 0.5f)
        )
        val xi = x + padding
        val yi = y + padding
        val si = size - 2 * padding
        if (color.a() != 255) {
            drawTransparentBackground(xi, yi, si, si)
        }
        DrawRectangles.drawRect(xi, yi, si, si, color)
    }
}