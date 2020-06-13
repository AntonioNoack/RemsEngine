package me.anno.ui.base.scrolling

import me.anno.input.Input
import me.anno.ui.base.Panel
import me.anno.utils.clamp
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import kotlin.math.max

open class ScrollPanelX(child: Panel, padding: Padding,
                        style: Style,
                        alignY: AxisAlignment
): PanelContainer(child, padding, style){

    constructor(style: Style, padding: Padding, align: AxisAlignment): this(PanelListX(style), padding, style, align)

    init {
        child += WrapAlign(AxisAlignment.MIN, alignY)
        weight = 0.0001f
    }

    var scrollPosition = 0f
    val maxLength = 100_000
    var wasActive = 1f

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
        child.applyConstraints()

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
            scrollPosition += scale * delta
            clampScrollPosition()
        } else super.onMouseWheel(x, y, dx, dy)
    }

    fun clampScrollPosition(){
        scrollPosition = clamp(scrollPosition, 0f, maxScrollPosition.toFloat())
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(scrollbar.contains(x,y,scrollbarPadding*2)){
            scrollbar.onMouseMoved(x, y, dx, dy)
            clampScrollPosition()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun getClassName(): String = "ScrollPanelX"

}