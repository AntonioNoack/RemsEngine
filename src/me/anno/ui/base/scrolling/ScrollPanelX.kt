package me.anno.ui.base.scrolling

import me.anno.Time.uiDeltaTime
import me.anno.engine.EngineBase
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawShadowX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import kotlin.math.max
import kotlin.math.round

/**
 * Android: HorizontalScrollView,
 * Unity: ScrollView
 * */
open class ScrollPanelX(child: Panel, padding: Padding, style: Style) :
    PanelContainer(child, padding, style), ScrollableX {

    constructor(style: Style) : this(PanelListX(style), style)
    constructor(child: Panel, style: Style) : this(child, Padding(), style)
    constructor(padding: Padding, style: Style) : this(PanelListX(style), padding, style)

    override var scrollHardnessX = 25.0

    override var scrollPositionX = 0.0
    override var targetScrollPositionX = 0.0

    var alwaysScroll = false

    val scrollbar = ScrollbarX(this, style)
    val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionHeight = scrollbarHeight + 2 * interactionPadding

    val hasScrollbar: Boolean get() = maxScrollPositionX > 0f

    override val childSizeX: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeX else child.minW.toLong()
        }

    override var maxScrollPositionX: Long = 0L

    private var hasScrollbarF: Float = 1f

    fun updateMaxScrollPosition(availableWidth: Int) {
        val child = child
        val childW = if (child is LongScrollable) child.sizeX else child.minW.toLong()
        maxScrollPositionX = childW + padding.width - availableWidth
        hasScrollbarF = clamp(maxScrollPositionX / (scrollbarHeight * 3f) + 1f)
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
        scrollbar.isHovered = window != null && capturesChildEvents(window.mouseXi, window.mouseYi)
        scrollPositionX = mix(scrollPositionX, targetScrollPositionX, dtTo01(uiDeltaTime * scrollHardnessX))
        scrollbar.updateAlpha()
    }

    override fun updateChildrenVisibility(mx: Int, my: Int, canBeHovered: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        super.updateChildrenVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        scrollbar.updateVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
    }

    override fun capturesChildEvents(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverX(
            lx0, ly0, lx1, ly1,
            sbHeight, x0, y0, x1, y1
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        // calculation must not depend on hasScrollbar, or we get flickering
        val child = child
        val padding = padding
        val paddingY0 = padding.height + scrollbarHeight
        child.calculateSize(MAX_LENGTH - padding.width, h - paddingY0)
        updateMaxScrollPosition(w)
        // these must follow child.calculateSize and updateMaxScrollPosition(), because they use their results as values
        val paddingY1 = padding.height + (hasScrollbarF * scrollbarHeight).toInt()
        minW = min(child.minW + padding.width, w)
        minH = min(child.minH + paddingY1, h)
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        val child = child
        val padding = padding
        val scroll0 = round(scrollPositionX).toLong()
        val scroll = clamp(scroll0, 0L, max(0, child.minW + padding.width - width).toLong()).toInt()
        val paddingY = padding.height + (hasScrollbarF * scrollbarHeight).toInt()
        child.setPosSize(
            x + padding.left - scroll, y + padding.top,
            max(child.minW, width - padding.width), height - paddingY
        )
        if (child is LongScrollable) {
            child.setExtraScrolling(scroll0 - scroll, 0L)
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.draw(x0, y0, x1, y1)
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
            scrollbar.y = y + height - scrollbarHeight - scrollbarPadding
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

    override fun drawsOverlayOverChildren(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return hasScrollbar && y1 >= y + height - scrollbarHeight
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
}