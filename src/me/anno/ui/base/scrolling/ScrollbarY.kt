package me.anno.ui.base.scrolling

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style
import kotlin.math.max

class ScrollbarY(val scrollable: ScrollableY, style: Style) : Scrollbar(style) {

    init {
        parent = scrollable as PanelGroup
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val relativePosition = scrollable.scrollPositionY / scrollable.maxScrollPositionY
        val barHeight = max(minSize.toDouble(), relativeSize * h)
        val barY = y + relativePosition * (h - barHeight)

        val color = multiplyAlpha(scrollColor, scrollColorAlpha + activeAlpha * alpha)
        drawRect(x0, barY.toInt(), x1 - x0, barHeight.toInt(), color)

    }

    val relativeSize
        get(): Double {
            val child = scrollable.child
            val minW = if (child is LongScrollable) child.sizeY else child.minH.toLong()
            return scrollable.h.toDouble() / minW
        }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            scrollable.scrollPositionY += dy / relativeSize
        }// else super.onMouseMoved(x, y, dx, dy)
    }

}