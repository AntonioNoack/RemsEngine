package me.anno.ui.base.scrolling

import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelY.Companion.scrollSpeed
import me.anno.ui.style.Style
import me.anno.maths.Maths.clamp
import kotlin.math.max

// todo if the mouse is over a scrollbar, change the cursor
open class ScrollPanelX(
    child: Panel, padding: Padding,
    style: Style,
    alignY: AxisAlignment
) : PanelContainer(child, padding, style), ScrollableX {

    constructor(style: Style) : this(PanelListX(style), Padding(), style, AxisAlignment.MIN)

    init {
        child += WrapAlign(AxisAlignment.MIN, alignY)
        weight = 0.0001f
    }

    @NotSerializedProperty
    var lsp = -1f

    @NotSerializedProperty
    var lmsp = -1

    override fun tickUpdate() {
        super.tickUpdate()
        if (scrollPositionX != lsp || maxScrollPositionX != lmsp) {
            lsp = scrollPositionX
            lmsp = maxScrollPositionX
            window!!.needsLayout += this
        }
    }

    override var scrollPositionX = 0f
    var isDownOnScrollbar = false

    override val maxScrollPositionX get() = max(0, child.minW + padding.width - w)
    val scrollbar = ScrollbarX(this, style)

    // todo these two properties need to be updated, when the style changes
    @NotSerializedProperty
    var scrollbarHeight = style.getSize("scrollbarHeight", 8)

    @NotSerializedProperty
    var scrollbarPadding = style.getSize("scrollbarPadding", 1)

    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return maxScrollPositionX > 0 && ly1 > this.ly1 - scrollbarHeight // overlaps on the bottom
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(maxLength, h - padding.height)
        // child.applyConstraints()

        minW = child.minW + padding.width
        minH = child.minH + padding.height

        if (maxScrollPositionX > 0) minH += scrollbarHeight

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scroll = scrollPositionX.toInt()
        child.placeInParent(x + padding.left - scroll, y + padding.top)

    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        // draw the scrollbar
        if (maxScrollPositionX > 0f) {
            scrollbar.x = x + scrollbarPadding
            scrollbar.y = y1 - scrollbarHeight - scrollbarPadding
            scrollbar.w = w - 2 * scrollbarPadding
            scrollbar.h = scrollbarHeight
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val scale = scrollSpeed
        if ((dx > 0f && scrollPositionX >= maxScrollPositionX) ||
            (dx < 0f && scrollPositionX <= 0f)
        ) {// if done scrolling go up the hierarchy one
            super.onMouseWheel(x, y, dx, dy, byMouse)
        } else {
            scrollPositionX += scale * dx
            clampScrollPosition()
            // we consumed dx
            if (dy != 0f) {
                super.onMouseWheel(x, y, 0f, dy, byMouse)
            }
        }
    }

    private fun clampScrollPosition() {
        scrollPositionX = clamp(scrollPositionX, 0f, maxScrollPositionX.toFloat())
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

    override fun clone(): ScrollPanelX {
        val clone = ScrollPanelX(child.clone(), padding, style, alignmentX)
        copy(clone)
        return clone
    }

    override val className: String = "ScrollPanelX"

}