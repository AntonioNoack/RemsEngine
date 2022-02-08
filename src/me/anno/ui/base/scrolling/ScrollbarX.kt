package me.anno.ui.base.scrolling

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style

open class ScrollbarX(val scrollable: ScrollableX, style: Style): Scrollbar(style){

    init {
        parent = scrollable as PanelGroup
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val relativePosition = scrollable.scrollPositionX / scrollable.maxScrollPositionX
        val barW = relativeSize * w
        val barX = x + relativePosition * w * (1f - relativeSize)

        drawRect(barX.toInt(), y0, barW.toInt(), y1-y0, multiplyAlpha(scrollColor, scrollColorAlpha + activeAlpha * alpha))

    }

    val relativeSize get() = scrollable.w.toFloat() / scrollable.child.minW

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(Input.isLeftDown){
            scrollable.scrollPositionX += dx / relativeSize
        }// else super.onMouseMoved(x, y, dx, dy)
    }

}