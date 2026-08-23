package me.anno.ui.background

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawRounded.drawRoundedRectSquircle
import me.anno.ui.Canvas
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import kotlin.math.max
import kotlin.math.min

class Background(style: Style) {

    var outlineColor = 0
    var outlineThickness = 0f
    var radius = style.getSize("background.radius", 0f)
    var color = style.getColor("background", -1)
    val originalColor = color

    fun drawBackground(
        x: Int, y: Int, width: Int, height: Int,
        canvas: Canvas, dx: Int, dy: Int,
        hasRoundedCorners: Boolean, uiParent: Panel?
    ) {
        // if the children are overlapping, this is incorrect
        // this however, should rarely happen...
        if (color.a() > 0 || hasRoundedCorners &&
            outlineThickness > 0f &&
            outlineColor.a() > 0
        ) {
            if (hasRoundedCorners) {
                val bg = if (uiParent == null) black else uiParent.background.color and 0xffffff
                val th = outlineThickness
                val radius = radius + th
                drawRoundedRectSquircle(
                    x + dx, y + dy, width - 2 * dx, height - 2 * dy,
                    radius, th, color, outlineColor, bg, 1f
                )
            } else {
                val x2 = max(canvas.x0, x + dx)
                val y2 = max(canvas.y0, y + dy)
                val x3 = min(canvas.x1, x + width - dx)
                val y3 = min(canvas.y1, y + height - dy)
                drawRect(x2, y2, x3 - x2, y3 - y2, color)
            }
        }
    }
}