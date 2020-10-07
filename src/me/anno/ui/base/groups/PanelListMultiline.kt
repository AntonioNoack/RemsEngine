package me.anno.ui.base.groups

import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.scrolling.ScrollbarY
import me.anno.ui.style.Style
import me.anno.utils.Quad
import me.anno.utils.clamp
import kotlin.math.max

class PanelListMultiline(style: Style): PanelGroup(style), ScrollableY {

    override val children = ArrayList<Panel>(256)
    override val child: Panel
        get() = this

    var scaleChildren = false
    var childWidth: Int
    var childHeight: Int

    init {
        val defaultSize = 100//style.getSize("textSize", 12) * 10
        childWidth = style.getSize("childWidth", defaultSize)
        childHeight = style.getSize("childHeight", defaultSize)
    }

    override fun invalidateLayout() {
        window!!.needsLayout += this
    }

    override fun getLayoutState() =
        Triple(
            super.getLayoutState(),
            children.size,
            Quad(childWidth, childHeight, scrollPosition, maxScrollPosition)
        )

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0
    var minH2 = 0

    val spacing = 1

    override fun calculateSize(w: Int, h: Int) {

        val children = children

        updateSize(w, h)

        for(child in children){
            child.calculateSize(calcChildWidth, calcChildHeight)
            // child.applyConstraints()
        }

    }

    fun clear() = children.clear()
    operator fun plusAssign(child: Panel){
        children.plusAssign(child)
        child.parent = this
    }

    override fun remove(child: Panel){ children.remove(child) }

    fun updateSize(w: Int, h: Int){
        columns = max(1, (w+spacing)/(childWidth+spacing))
        rows = max(1, (children.size + columns - 1) / columns)
        val childScale = if(scaleChildren) max(1f, ((w+spacing)/columns - spacing)*1f/childWidth) else 1f
        calcChildWidth = if(scaleChildren) (childWidth * childScale).toInt() else childWidth
        calcChildHeight = if(scaleChildren) (childHeight * childScale).toInt() else childHeight
        minW = max(w, calcChildWidth)
        minH = max((calcChildHeight + spacing) * rows - spacing, h)
        minH += childHeight / 2 /* Reserve, because somehow it's not enough... */
        minH2 = minH
    }

    override fun applyPlacement(w: Int, h: Int) {
        updateSize(w, h)
        super.applyPlacement(w, h)
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scroll = scrollPosition.toInt()
        for((i, child) in children.withIndex()){
            val ix = i % columns
            val iy = i / columns
            val cx = x + ix * (calcChildWidth + spacing) - spacing
            val cy = y + iy * (calcChildHeight + spacing) - spacing - scroll
            child.placeInParent(cx, cy)
        }

    }

    override var scrollPosition = 0f
    var isDownOnScrollbar = false

    override val maxScrollPosition get() = max(0, minH2 - h)
    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbar.width", 8)
    val scrollbarPadding = style.getSize("scrollbar.padding", 1)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        clampScrollPosition()
        if(maxScrollPosition > 0f){
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.w = scrollbarWidth
            scrollbar.h = h - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if(!Input.isShiftDown && !Input.isControlDown){
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