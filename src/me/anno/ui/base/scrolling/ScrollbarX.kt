package me.anno.ui.base.scrolling

import me.anno.gpu.GFX
import me.anno.gpu.GFX.deltaTime
import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.mix
import kotlin.math.min

open class ScrollbarX(val scrollbar: ScrollPanelX, style: Style): Scrollbar(style){

    init {
        parent = scrollbar
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val relativePosition = scrollbar.scrollPosition / scrollbar.maxScrollPosition
        val barW = relativeSize * w
        val barX = x + relativePosition * w * (1f - relativeSize)

        GFX.drawRect(barX.toInt(), y0, barW.toInt(), y1-y0, multiplyAlpha(scrollColor, scrollColorAlpha + activeAlpha * wasActive))

    }

    val relativeSize get() = scrollbar.w.toFloat() / scrollbar.child.minW

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(0 in Input.mouseKeysDown){
            scrollbar.scrollPosition += dx / relativeSize
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun getClassName() = "ScrollbarX"

}