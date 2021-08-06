package me.anno.ui.base.groups

import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.scrolling.ScrollbarY
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.structures.tuples.Quad
import kotlin.math.max

class PanelListMultiline(val sorter: Comparator<Panel>?, style: Style) : PanelGroup(style), ScrollableY {

    override val children = ArrayList<Panel>(256)
    override val child: Panel
        get() = this

    // different modes for left/right alignment
    var childAlignmentX = AxisAlignment.CENTER

    var scaleChildren = false
    var childWidth: Int
    var childHeight: Int

    init {
        val defaultSize = 100
        childWidth = style.getSize("childWidth", defaultSize)
        childHeight = style.getSize("childHeight", defaultSize)
    }

    override fun invalidateLayout() {
        window?.needsLayout?.add(this)
    }

    override fun getLayoutState() =
        Pair(
            children.count { it.visibility == Visibility.VISIBLE },
            Quad(
                childWidth,
                childHeight,
                scrollPosition,
                maxScrollPosition
            )
        )

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0
    var minH2 = 0

    var spacing = style.getSize("childSpacing", 1)

    override fun calculateSize(w: Int, h: Int) {

        val children = children
        if(sorter != null){
            children.sortWith(sorter)
        }

        updateSize(w, h)
        for (i in children.indices) {
            val child = children[i]
            if (child.visibility != Visibility.GONE) {
                child.calculateSize(calcChildWidth, calcChildHeight)
                // child.applyConstraints()
            }
        }

    }

    fun clear() = children.clear()
    operator fun plusAssign(child: Panel) {
        children.plusAssign(child)
        child.parent = this
        invalidateLayout()
    }

    override fun remove(child: Panel) {
        children.remove(child)
        invalidateLayout()
    }

    private fun updateCount() {
        val childCount = children.count { it.visibility == Visibility.VISIBLE }
        columns = max(1, (w + spacing) / (childWidth + spacing))
        rows = max(1, (childCount + columns - 1) / columns)
    }

    private fun updateScale() {
        val childScale = if (scaleChildren) max(1f, ((w + spacing) / columns - spacing) * 1f / childWidth) else 1f
        calcChildWidth = if (scaleChildren) (childWidth * childScale).toInt() else childWidth
        calcChildHeight = if (scaleChildren) (childHeight * childScale).toInt() else childHeight
    }

    private fun updateSize(w: Int, h: Int) {
        updateCount()
        updateScale()
        minW = max(w, calcChildWidth)
        minH = max((calcChildHeight + spacing) * rows - spacing, h)
        minH += childHeight / 6 /* Reserve, because somehow it's not enough... */
        minH2 = minH
    }

    override fun applyPlacement(w: Int, h: Int) {
        updateSize(w, h)
        super.applyPlacement(w, h)
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val w = w - scrollbarWidth
        val contentW = columns * childWidth

        val scroll = scrollPosition.toInt()
        var i = 0
        for (j in children.indices) {
            val child = children[j]
            if (child.visibility != Visibility.GONE) {
                val ix = i % columns
                val iy = i / columns
                val cx = x + when (childAlignmentX) {
                    AxisAlignment.MIN -> ix * (calcChildWidth + spacing) + spacing
                    AxisAlignment.CENTER -> ix * calcChildWidth + max(0, w - contentW) * (ix + 1) / (columns + 1)
                    AxisAlignment.MAX -> w - (columns - ix) * (calcChildWidth + spacing)
                }
                val cy = y + iy * (calcChildHeight + spacing) + spacing - scroll
                child.placeInParent(cx, cy)
                i++
            }
        }

    }

    override var scrollPosition = 0f
    var isDownOnScrollbar = false

    override val maxScrollPosition get() = max(0, minH2 - h)
    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        clampScrollPosition()
        if (maxScrollPosition > 0f) {
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.w = scrollbarWidth
            scrollbar.h = h - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        if (!Input.isShiftDown && !Input.isControlDown) {
            val delta = dx - dy
            val scale = 20f
            if ((delta > 0f && scrollPosition >= maxScrollPosition) ||
                (delta < 0f && scrollPosition <= 0f)
            ) {// if done scrolling go up the hierarchy one
                super.onMouseWheel(x, y, dx, dy)
            } else {
                scrollPosition += scale * delta
                clampScrollPosition()
            }
        } else super.onMouseWheel(x, y, dx, dy)
    }

    private fun clampScrollPosition() {
        scrollPosition = clamp(scrollPosition, 0f, maxScrollPosition.toFloat())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = scrollbar.contains(x, y, scrollbarPadding * 2)
        if (!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar) {
            scrollbar.onMouseMoved(x, y, dx, dy)
            clampScrollPosition()
        } else super.onMouseMoved(x, y, dx, dy)
    }

}