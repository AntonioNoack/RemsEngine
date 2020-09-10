package me.anno.ui.base.scrolling

import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.ui.style.Style

class ScrollbarY(val scrollbar: ScrollPanelY, style: Style): Scrollbar(style){

    init {
        parent = scrollbar
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val relativePosition = scrollbar.scrollPosition / scrollbar.maxScrollPosition
        val barHeight = relativeSize * h
        val barY = y + relativePosition * h * (1f - relativeSize)

        GFX.drawRect(x0, barY.toInt(), x1-x0, barHeight.toInt(), multiplyAlpha(scrollColor, scrollColorAlpha + activeAlpha * wasActive))

    }

    val relativeSize get() = scrollbar.h.toFloat() / scrollbar.child.minH

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(0 in Input.mouseKeysDown){
            scrollbar.scrollPosition += dy / relativeSize
        }// else super.onMouseMoved(x, y, dx, dy)
    }

    override fun getClassName() = "ScrollbarY"

}