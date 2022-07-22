package me.anno.ui.base.scrolling

import me.anno.config.DefaultConfig
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import kotlin.math.abs
import kotlin.math.max

open class ScrollPanelXY(child: Panel, padding: Padding, style: Style) :
    PanelContainer(child, padding, style), ScrollableX, ScrollableY {

    constructor(style: Style) : this(Panel(style), style)
    constructor(child: Panel, style: Style) : this(child, Padding(), style)
    constructor(padding: Padding, style: Style) : this(PanelListY(style), padding, style)

    open val content get() = child

    @NotSerializedProperty
    private var lastScrollPosX = -1.0

    @NotSerializedProperty
    private var lastScrollPosY = -1.0

    @NotSerializedProperty
    private var lastMaxScrollPosX = -1L

    @NotSerializedProperty
    private var lastMaxScrollPosY = -1L

    override fun onUpdate() {
        super.onUpdate()
        val window = window!!
        val mx = window.mouseXi
        val my = window.mouseYi
        scrollbarX.isBeingHovered = drawsOverX(mx, my)
        scrollbarY.isBeingHovered = drawsOverY(mx, my)
        if (scrollbarX.updateAlpha()) invalidateDrawing()
        if (scrollbarY.updateAlpha()) invalidateDrawing()
        if (
            scrollPositionX != lastScrollPosX ||
            scrollPositionY != lastScrollPosY ||
            maxScrollPositionX != lastMaxScrollPosX ||
            maxScrollPositionY != lastMaxScrollPosY
        ) {
            lastScrollPosX = scrollPositionX
            lastScrollPosY = scrollPositionY
            lastMaxScrollPosX = maxScrollPositionX
            lastMaxScrollPosY = maxScrollPositionY
            window.needsLayout += this
        }
    }

    override var scrollPositionX = 0.0
    override var scrollPositionY = 0.0

    override val maxScrollPositionX get() = max(0, child.minW + padding.width - w).toLong()
    override val maxScrollPositionY get() = max(0, child.minH + padding.height - h).toLong()

    @NotSerializedProperty
    private var isDownOnScrollbarX = false

    @NotSerializedProperty
    private var isDownOnScrollbarY = false

    private val scrollbarX = ScrollbarX(this, style)
    private val scrollbarY = ScrollbarY(this, style)

    private val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    private val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    private val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    private val interactionWidth = scrollbarWidth + 2 * interactionPadding
    private val interactionHeight = scrollbarHeight + 2 * interactionPadding

    private val hasScrollbarX get() = maxScrollPositionX > 0
    private val hasScrollbarY get() = maxScrollPositionY > 0

    override fun scrollX(delta: Double) {
        scrollPositionX += delta
        clampScrollPosition()
    }

    override fun scrollY(delta: Double) {
        scrollPositionY += delta
        clampScrollPosition()
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return drawsOverX(lx0, ly0, lx1, ly1) || drawsOverY(lx0, ly0, lx1, ly1)
    }

    private fun drawsOverX(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbarX && drawsOverX(this.lx0, this.ly0, this.lx1, this.ly1, sbHeight, lx0, ly0, lx1, ly1)
    }

    private fun drawsOverY(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbarY && drawsOverY(this.lx0, this.ly0, this.lx1, this.ly1, sbWidth, lx0, ly0, lx1, ly1)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(maxLength - padding.width, maxLength - padding.height)

        minW = child.minW + padding.width
        minH = child.minH + padding.height

        if (hasScrollbarX) minH += scrollbarHeight
        if (hasScrollbarY) minW += scrollbarWidth

    }

    override fun setPosition(x: Int, y: Int) {

        this.x = x
        this.y = y

        val child = child
        val padding = padding

        val scrollX0 = scrollPositionX.toLong()
        val scrollY0 = scrollPositionY.toLong()
        val scrollX1 = clamp(scrollX0, 0L, max(0, child.minW + padding.width - w).toLong()).toInt()
        val scrollY1 = clamp(scrollY0, 0L, max(0, child.minH + padding.height - h).toLong()).toInt()

        child.setPosition(x + padding.left - scrollX1, y + padding.top - scrollY1)

        if (child is LongScrollable) {
            child.setExtraScrolling(scrollX0 - scrollX1, scrollY0 - scrollY1)
        }

    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        if (hasScrollbarX) {
            val scrollbarX = scrollbarX
            scrollbarX.x = x + scrollbarPadding
            scrollbarX.y = y1 - scrollbarHeight - scrollbarPadding
            scrollbarX.w = w - 2 * scrollbarPadding
            scrollbarX.h = scrollbarHeight
            drawChild(scrollbarX, x0, y0, x1, y1)
        }
        if (hasScrollbarY) {
            val scrollbarY = scrollbarY
            scrollbarY.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbarY.y = y + scrollbarPadding
            scrollbarY.w = scrollbarWidth
            scrollbarY.h = h - 2 * scrollbarPadding
            drawChild(scrollbarY, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {

        var consumedX = false
        var consumedY = false

        val dx0 = dx * scrollSpeed
        if ((dx0 > 0f && scrollPositionX >= maxScrollPositionX) ||
            (dx0 < 0f && scrollPositionX <= 0f)
        ) {// if done scrolling go up the hierarchy one

        } else {
            scrollPositionX += dx0
            clampScrollPosition()
            consumedX = true
        }

        val dy0 = -dy * scrollSpeed
        if ((dy0 > 0f && scrollPositionY >= maxScrollPositionY) ||
            (dy0 < 0f && scrollPositionY <= 0f)
        ) {// if done scrolling go up the hierarchy one

        } else {
            scrollPositionY += dy0
            clampScrollPosition()
            consumedY = true
        }

        if (consumedX || consumedY) {
            invalidateLayout()
        }

        if (!consumedX || !consumedY) {
            val dx2 = if (consumedX) 0f else dx
            val dy2 = if (consumedY) 0f else dy
            super.onMouseWheel(x, y, dx2, dy2, byMouse)
        }

    }

    private fun clampScrollPosition() {
        scrollPositionX = clamp(scrollPositionX, 0.0, maxScrollPositionX.toDouble())
        scrollPositionY = clamp(scrollPositionY, 0.0, maxScrollPositionY.toDouble())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        val xi = x.toInt()
        val yi = y.toInt()
        isDownOnScrollbarX = hasScrollbarX && drawsOverX(xi, yi)
        isDownOnScrollbarY = hasScrollbarY && drawsOverY(xi, yi)
        if (!isDownOnScrollbarX && !isDownOnScrollbarY) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbarX = false
        isDownOnScrollbarY = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (isDownOnScrollbarX && rx != 0f) {
            scrollbarX.onMouseMoved(x, y, rx, 0f)
            clampScrollPosition()
            invalidateLayout()
            // consume rx
            rx = 0f
        }
        if (isDownOnScrollbarY && ry != 0f) {
            scrollbarY.onMouseMoved(x, y, 0f, ry)
            clampScrollPosition()
            invalidateLayout()
            // consume ry
            ry = 0f
        }
        if (rx != 0f || ry != 0f) {
            super.onMouseMoved(x, y, rx, ry)
        }
    }

    override fun clone(): PanelContainer {
        val clone = ScrollPanelXY(child.clone(), padding, style)
        copy(clone)
        return clone
    }

    override val className: String = "ScrollPanelXY"

    companion object {

        const val minWeight = 0.0001f

        val scrollSpeed get() = DefaultConfig["ui.scroll.speed", 30f]

        @Suppress("unused_parameter")
        fun drawsOverX(
            lx0: Int, ly0: Int, lx1: Int, ly1: Int,
            sbHeight: Int,
            x0: Int, y0: Int, x1: Int = x0 + 1, y1: Int = y0 + 1
        ): Boolean {
            val sbWidth = lx1 - lx0
            // val sbHeight = touchScrollbarHeight + 2 * scrollbarPadding
            val centerX = lx0 + lx1
            val centerY = ly1 * 2 - sbHeight
            return abs((x0 + x1) - centerX) < sbWidth && // hasScrollbarY &&
                    abs((y0 + y1) - centerY) < sbHeight
        }

        @Suppress("unused_parameter")
        fun drawsOverY(
            lx0: Int, ly0: Int, lx1: Int, ly1: Int,
            sbWidth: Int,
            x0: Int, y0: Int, x1: Int = x0 + 1, y1: Int = y0 + 1
        ): Boolean {
            // val sbWidth = touchScrollbarWidth + 2 * scrollbarPadding
            val sbHeight = ly1 - ly0
            val centerX = lx1 * 2 - sbWidth
            val centerY = ly0 + ly1
            return abs((x0 + x1) - centerX) < sbWidth && // hasScrollbarY &&
                    abs((y0 + y1) - centerY) < sbHeight
        }

    }

}
