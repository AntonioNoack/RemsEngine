package me.anno.ui.base.scrolling

import me.anno.Time.deltaTime
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Input
import me.anno.input.Key
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.engine.EngineBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawShadowX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.utils.types.Booleans.toInt
import kotlin.math.max
import kotlin.math.round

/**
 * Android: HorizontalScrollView,
 * Unity: ScrollView
 * */
open class ScrollPanelX(
    child: Panel, padding: Padding,
    style: Style
) : PanelContainer(child, padding, style), ScrollableX {

    constructor(style: Style) : this(PanelListX(style), style)
    constructor(child: Panel, style: Style) : this(child, Padding(), style)
    constructor(padding: Padding, style: Style) : this(PanelListX(style), padding, style)

    @NotSerializedProperty
    var lastScrollPosX = -1.0

    @NotSerializedProperty
    var lastMaxScrollPosX = -1L

    override var scrollHardnessX = 25.0

    override var scrollPositionX = 0.0
    override var targetScrollPositionX = 0.0

    var alwaysScroll = false

    val scrollbar = ScrollbarX(this, style)
    val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionHeight = scrollbarHeight + 2 * interactionPadding

    val hasScrollbar get() = maxScrollPositionX > 0f

    override val childSizeX: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeX else child.minW.toLong()
        }

    override val maxScrollPositionX: Long
        get() {
            val child = child
            val childW = if (child is LongScrollable) child.sizeX else child.minW.toLong()
            return max(0, childW + padding.width - width)
        }

    override fun scrollX(delta: Double): Double {
        val prev = targetScrollPositionX
        targetScrollPositionX += delta
        clampScrollPosition()
        val moved = targetScrollPositionX - prev
        return moved - delta
    }

    override fun onUpdate() {
        super.onUpdate()
        val window = window
        if (window != null) {
            val mx = window.mouseXi
            val my = window.mouseYi
            scrollbar.isBeingHovered = capturesChildEvents(mx, my)
        }
        scrollPositionX = mix(scrollPositionX, targetScrollPositionX, dtTo01(deltaTime * scrollHardnessX))
        if (scrollbar.updateAlpha()) invalidateDrawing()
        if (round(scrollPositionX) != lastScrollPosX || maxScrollPositionX != lastMaxScrollPosX) {
            lastScrollPosX = round(scrollPositionX)
            lastMaxScrollPosX = maxScrollPositionX
            placeChild()
            invalidateDrawing()
        }
    }

    fun placeChild() {
        val child = child
        val padding = padding
        val scroll0 = round(scrollPositionX).toLong()
        val scroll = clamp(scroll0, 0L, max(0, child.minW + padding.width - width).toLong()).toInt()
        child.setPosition(x + padding.left - scroll, y + padding.top)
        if (child is LongScrollable) {
            child.setExtraScrolling(scroll0 - scroll, 0L)
        }
    }

    override fun setSize(w: Int, h: Int) {
        super.setSize(w, h)
        child.setSize(max(child.minW, w - padding.width), h - padding.height)
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverX(
            this.lx0, this.ly0, this.lx1, this.ly1,
            sbHeight, lx0, ly0, lx1, ly1
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        val child = child
        val padding = padding
        child.calculateSize(maxLength - padding.width, h - padding.height)
        minW = min(child.minW + padding.width, w)
        minH = child.minH + padding.height + hasScrollbar.toInt(scrollbarHeight)
    }

    override fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        placeChild()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        val batch = DrawRectangles.startBatch()
        if (alwaysShowShadowX) {
            drawShadowX(x0, y0, x1, y1, shadowRadius)
        }
        if (hasScrollbar) {
            if (!alwaysShowShadowX) {
                val shadowRadius = min(maxScrollPositionX, shadowRadius.toLong()).toInt()
                drawShadowX(x0, y0, x1, y1, shadowRadius)
            }
            val scrollbar = scrollbar
            scrollbar.x = x + scrollbarPadding
            scrollbar.y = y1 - scrollbarHeight - scrollbarPadding
            scrollbar.width = width - 2 * scrollbarPadding
            scrollbar.height = scrollbarHeight
            drawChild(scrollbar, x0, y0, x1, y1)
        }
        DrawRectangles.finishBatch(batch)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val dxi = scrollX((dx * scrollSpeed).toDouble()).toFloat() / scrollSpeed
        val dyi = if (alwaysScroll) {
            -scrollX((-dy * scrollSpeed).toDouble()).toFloat() / scrollSpeed
        } else dy
        super.onMouseWheel(x, y, dxi, dyi, byMouse)
    }

    fun clampScrollPosition() {
        scrollPositionX = clamp(scrollPositionX, 0.0, maxScrollPositionX.toDouble())
        targetScrollPositionX = clamp(targetScrollPositionX, 0.0, maxScrollPositionX.toDouble())
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return hasScrollbar && ly1 >= y + height - scrollbarHeight
    }

    @NotSerializedProperty
    private var isDownOnScrollbar = 0

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) isDownOnScrollbar = if (capturesChildEvents(x.toInt(), y.toInt())) 1 else -1
        else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) isDownOnScrollbar = 0
        super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar != 0 && Input.isLeftDown && EngineBase.dragged == null) {
            val dx2 = scrollX(if (isDownOnScrollbar > 0) dx / relativeSizeX else -dx.toDouble()).toFloat()
            if (dx2 != 0f || dy != 0f) super.onMouseMoved(x, y, dx2, dy)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): ScrollPanelX {
        val clone = ScrollPanelX(child.clone(), padding, style)
        copyInto(clone)
        return clone
    }

    override val className: String get() = "ScrollPanelX"
}