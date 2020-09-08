package me.anno.ui.base.scrolling

import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.utils.clamp
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min

open class ScrollPanelX(child: Panel, padding: Padding,
                        style: Style,
                        alignY: AxisAlignment
): PanelContainer(child, padding, style){

    init {
        child += WrapAlign(AxisAlignment.MIN, alignY)
        weight = 0.0001f
    }

    var scrollPosition = 0f
    val maxLength = 100_000
    var wasActive = 1f
    var isDownOnScrollbar = false

    val maxScrollPosition get() = max(0, child.minW + padding.width - w)
    val scrollbar = ScrollbarX(this, style)
    val scrollbarHeight = style.getSize("scrollbar.height", 8)
    val scrollbarPadding = style.getSize("scrollbar.padding", 1)
    init {
        padding.bottom += scrollbarHeight
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(maxLength, h-padding.height)
        // child.applyConstraints()

        minW = child.minW + padding.width
        minH = child.minH + padding.height

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scroll = scrollPosition.toInt()
        child.placeInParent(x+padding.left-scroll,y+padding.top)

    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.draw(x0, y0, x1, y1)
        // draw the scrollbar
        if(maxScrollPosition > 0f){
            scrollbar.x = x + scrollbarPadding
            scrollbar.y = y1 - scrollbarHeight - scrollbarPadding
            scrollbar.w = w - 2 * scrollbarPadding
            scrollbar.h = scrollbarHeight
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if(Input.isShiftDown){
            val delta = dx-dy
            val scale = 20f
            if((delta > 0f && scrollPosition >= maxScrollPosition) ||
                (delta < 0f && scrollPosition <= 0f)){// if done scrolling go up the hierarchy one
                super.onMouseWheel(x, y, dx, dy)
            } else {
                scrollPosition += scale * delta
                clampScrollPosition()
            }
        } else super.onMouseWheel(x, y, dx, dy)
    }

    fun clampScrollPosition(){
        scrollPosition = clamp(scrollPosition, 0f, maxScrollPosition.toFloat())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = scrollbar.contains(x,y,scrollbarPadding*2)
        if(!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(isDownOnScrollbar){
            scrollbar.onMouseMoved(x, y, dx, dy)
            clampScrollPosition()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun getClassName(): String = "ScrollPanelX"

}