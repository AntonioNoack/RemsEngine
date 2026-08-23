package me.anno.ui.base.scrolling

import me.anno.input.Input
import me.anno.ui.Canvas
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.mulAlpha
import kotlin.math.max

open class ScrollbarX(val scrollable: ScrollableX, style: Style) : Scrollbar(scrollable as Panel, style) {

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val relativePosition = scrollable.scrollPositionX / scrollable.maxScrollPositionX
        val barW = max(minSize.toDouble(), scrollable.relativeSizeX * width)
        val barX = x + relativePosition * (width - barW)

        val color = scrollColor.mulAlpha(scrollColorAlpha + activeAlpha * alpha)
        canvas.drawRect(barX.toInt(), canvas.y0, barW.toInt(), canvas.dy, color)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            scrollable.scrollX(dx / scrollable.relativeSizeX)
        }// else super.onMouseMoved(x, y, dx, dy)
    }
}