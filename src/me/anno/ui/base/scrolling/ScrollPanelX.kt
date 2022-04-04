package me.anno.ui.base.scrolling

import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.minWeight
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.ui.style.Style
import kotlin.math.max

open class ScrollPanelX(
    child: Panel, padding: Padding,
    style: Style,
    alignY: AxisAlignment
) : PanelContainer(child, padding, style), ScrollableX {

    constructor(style: Style) : this(PanelListX(style), Padding(), style, AxisAlignment.MIN)

    init {
        child += WrapAlign(AxisAlignment.MIN, alignY)
        setWeight(minWeight)
    }

    @NotSerializedProperty
    var lsp = -1.0

    @NotSerializedProperty
    var lmsp = -1L

    override var scrollPositionX = 0.0

    @NotSerializedProperty
    private var isDownOnScrollbar = false

    override val maxScrollPositionX get() = max(0, child.minW + padding.width - w).toLong()
    val scrollbar = ScrollbarX(this, style)

    // todo these two properties need to be updated, when the style changes
    val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionHeight = scrollbarHeight + 2 * interactionPadding

    val hasScrollbar get() = maxScrollPositionX > 0f

    override fun tickUpdate() {
        super.tickUpdate()
        val window = window!!
        val mx = window.mouseXi
        val my = window.mouseYi
        scrollbar.isBeingHovered = capturesChildEvents(mx, my)
        if (scrollbar.updateAlpha()) invalidateDrawing()
        if (scrollPositionX != lsp || maxScrollPositionX != lmsp) {
            lsp = scrollPositionX
            lmsp = maxScrollPositionX
            window.needsLayout += this
        }
    }

    override fun scrollX(delta: Double) {
        scrollPositionX += delta
        clampScrollPosition()
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverX(
            this.lx0, this.ly0, this.lx1, this.ly1, sbHeight,
            lx0, ly0, lx1, ly1
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val child = child
        val padding = padding
        child.calculateSize(maxLength, h - padding.height)

        minW = child.minW + padding.width
        minH = child.minH + padding.height
        if (hasScrollbar) minH += scrollbarHeight

    }

    override fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        val child = child
        val padding = padding
        val scroll0 = scrollPositionX.toLong()
        val scroll = clamp(scroll0, 0L, max(0, child.minW + padding.width - w).toLong()).toInt()
        child.setPosition(x + padding.left - scroll, y + padding.top)
        if (child is LongScrollable) {
            child.setExtraScrolling(scroll0 - scroll, 0L)
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        // draw the scrollbar
        if (hasScrollbar) {
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
            invalidateLayout()
            // we consumed dx
            if (dy != 0f) {
                super.onMouseWheel(x, y, 0f, dy, byMouse)
            }
        }
    }

    private fun clampScrollPosition() {
        scrollPositionX = clamp(scrollPositionX, 0.0, maxScrollPositionX.toDouble())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = capturesChildEvents(x.toInt(), y.toInt())
        if (!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar) {
            if (dx != 0f) {
                scrollbar.onMouseMoved(x, y, dx, 0f)
                clampScrollPosition()
                invalidateLayout()
            }
            // dx was consumed
            if (dy != 0f) super.onMouseMoved(x, y, 0f, dy)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): ScrollPanelX {
        val clone = ScrollPanelX(child.clone(), padding, style, alignmentX)
        copy(clone)
        return clone
    }

    override val className: String = "ScrollPanelX"

}