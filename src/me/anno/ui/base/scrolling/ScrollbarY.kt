package me.anno.ui.base.scrolling

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.mulAlpha
import kotlin.math.max

open class ScrollbarY(val scrollable: ScrollableY, style: Style) : Scrollbar(scrollable as Panel, style) {

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val relativePosition = scrollable.scrollPositionY / scrollable.maxScrollPositionY
        val barHeight = max(minSize.toDouble(), scrollable.relativeSizeY * height)
        val barY = y + relativePosition * (height - barHeight)

        val color = scrollColor.mulAlpha(scrollColorAlpha + activeAlpha * alpha)
        drawRect(x0, barY.toInt(), x1 - x0, barHeight.toInt(), color)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            scrollable.scrollY(dy / scrollable.relativeSizeY)
        }// else super.onMouseMoved(x, y, dx, dy)
    }
}