package me.anno.ui.base.scrolling

import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.utils.clamp
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListMultiline
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min

open class ScrollPanelY(child: Panel, padding: Padding,
                        style: Style,
                        alignX: AxisAlignment): PanelContainer(child, padding, style), ScrollableY {

    constructor(child: Panel, style: Style): this(child, Padding(), style, AxisAlignment.MIN)
    constructor(child: Panel, padding: Padding, style: Style): this(child, padding, style, AxisAlignment.MIN)
    constructor(padding: Padding, align: AxisAlignment, style: Style): this(PanelListY(style), padding, style, align)

    init {
        child += WrapAlign(alignX, AxisAlignment.MIN)
        weight = 0.0001f
    }

    var lsp = -1f
    var lmsp = -1
    override fun tickUpdate() {
        super.tickUpdate()
        if(scrollPosition != lsp || maxScrollPosition != lmsp){
            lsp = scrollPosition
            lmsp = maxScrollPosition
            window!!.needsLayout += this
        }
    }

    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return maxScrollPosition > 0 && lx1 > this.lx1 - scrollbarWidth // overlaps on the right
    }

    override var scrollPosition = 0f
    val maxLength = 100_000
    var isDownOnScrollbar = false

    override val maxScrollPosition get() = max(0, child.minH + padding.height - h)
    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbar.width", 8)
    val scrollbarPadding = style.getSize("scrollbar.padding", 1)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(w-padding.width, maxLength-padding.height)

        minW = child.minW + padding.width
        minH = child.minH + padding.height
        if(maxScrollPosition > 0) minW += scrollbarWidth
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scroll = scrollPosition.toInt()
        child.placeInParent(x+padding.left,y+padding.top-scroll)

    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        if(maxScrollPosition > 0f){
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.w = scrollbarWidth
            scrollbar.h = h - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if(!Input.isShiftDown){
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

}