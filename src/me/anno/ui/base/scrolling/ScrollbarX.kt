package me.anno.ui.base.scrolling

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.mulAlpha
import kotlin.math.max

open class ScrollbarX(val scrollable: ScrollableX, style: Style) : Scrollbar(scrollable as Panel, style) {

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val relativePosition = scrollable.scrollPositionX / scrollable.maxScrollPositionX
        val barW = max(minSize.toDouble(), scrollable.relativeSizeX * width)
        val barX = x + relativePosition * (width - barW)

        val color = scrollColor.mulAlpha(scrollColorAlpha + activeAlpha * alpha)
        drawRect(barX.toInt(), y0, barW.toInt(), y1 - y0, color)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            scrollable.scrollX(dx / scrollable.relativeSizeX)
        }// else super.onMouseMoved(x, y, dx, dy)
    }
}