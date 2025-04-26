package me.anno.ui.base.scrolling

import me.anno.Time.uiDeltaTime
import me.anno.engine.EngineBase
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawShadowY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.utils.types.Booleans.toInt
import kotlin.math.max
import kotlin.math.round

/**
 * Related Classes:
 *  - Android: ScrollView,
 *  - Unity: ScrollView,
 *  - Unreal Engine: UScrollBox,
 * */
open class ScrollPanelY(child: Panel, padding: Padding, style: Style) :
    PanelContainer(child, padding, style), ScrollableY {

    constructor(style: Style) : this(PanelListY(style), style)
    constructor(child: Panel, style: Style) : this(child, Padding(), style)
    constructor(padding: Padding, style: Style) : this(PanelListY(style), padding, style)

    override var scrollHardnessY = 25.0

    override var scrollPositionY = 0.0
    override var targetScrollPositionY = 0.0

    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionWidth = scrollbarWidth + 2 * interactionPadding

    val hasScrollbar: Boolean get() = maxScrollPositionY > 0

    override val childSizeY: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeY else child.minH.toLong()
        }

    override val maxScrollPositionY: Long
        get() = max(0, maxScrollPositionYRaw)

    val maxScrollPositionYRaw: Long
        get() {
            val child = child
            val childH = if (child is LongScrollable) child.sizeY else child.minH.toLong()
            return childH + padding.height - height
        }

    override fun scrollY(delta: Double): Double {
        val prev = targetScrollPositionY
        targetScrollPositionY += delta
        clampScrollPosition()
        val moved = targetScrollPositionY - prev
        return moved - delta
    }

    override fun onUpdate() {
        super.onUpdate()
        val window = window
        scrollbar.isHovered = window != null && capturesChildEvents(window.mouseXi, window.mouseYi)
        scrollPositionY = mix(scrollPositionY, targetScrollPositionY, dtTo01(uiDeltaTime * scrollHardnessY))
        scrollbar.updateAlpha()
    }

    fun placeChild() {
        val child = child
        val padding = padding
        val scroll0 = round(scrollPositionY).toLong()
        val scroll = clamp(scroll0, 0L, max(0, child.minH + padding.height - height).toLong()).toInt()
        val paddingX = padding.width + hasScrollbar.toInt(scrollbarWidth)
        child.setPosSize(
            x + padding.left, y + padding.top - scroll,
            width - paddingX, max(child.minH, height - padding.height)
        )
        if (child is LongScrollable) {
            child.setExtraScrolling(0L, scroll0 - scroll)
        }
    }

    override fun updateChildrenVisibility(mx: Int, my: Int, canBeHovered: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        super.updateChildrenVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        scrollbar.updateVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverY(
            this.lx0, this.ly0, this.lx1, this.ly1,
            sbWidth, lx0, ly0, lx1, ly1
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        val child = child
        val padding = padding
        // calculation must not depend on hasScrollbar, or we get flickering
        val paddingX = padding.width + hasScrollbar.toInt(scrollbarWidth)
        child.calculateSize(w - paddingX, MAX_LENGTH - padding.height)
        minW = min(child.minW + paddingX, w)
        minH = min(child.minH + padding.height, h)
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChild()
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.draw(x0, y0, x1, y1)
        val batch = DrawRectangles.startBatch()
        if (alwaysShowShadowY) {
            drawShadowY(x0, y0, x1, y1, shadowRadius)
        }
        if (hasScrollbar) {
            if (!alwaysShowShadowY) {
                val shadowRadius = min(maxScrollPositionY, shadowRadius.toLong()).toInt()
                drawShadowY(x0, y0, x1, y1, shadowRadius)
            }
            val scrollbar = scrollbar
            scrollbar.x = x + width - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.width = scrollbarWidth
            scrollbar.height = height - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
        DrawRectangles.finishBatch(batch)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val delta = -dy * scrollSpeed
        if ((delta > 0f && targetScrollPositionY >= maxScrollPositionY) ||
            (delta < 0f && targetScrollPositionY <= 0f)
        ) {// if done scrolling go up the hierarchy one
            super.onMouseWheel(x, y, dx, dy, byMouse)
        } else {
            scrollY(delta.toDouble())
            super.onMouseWheel(x, y, dx, 0f, byMouse)
        }
    }

    fun clampScrollPosition() {
        scrollPositionY = clamp(scrollPositionY, 0.0, maxScrollPositionY.toDouble())
        targetScrollPositionY = clamp(targetScrollPositionY, 0.0, maxScrollPositionY.toDouble())
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return hasScrollbar && lx1 >= x + width - scrollbarWidth
    }

    @NotSerializedProperty
    private var isDownOnScrollbar = 0

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) isDownOnScrollbar = if (capturesChildEvents(x.toInt(), y.toInt())) 1 else -1
        else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) isDownOnScrollbar = 0
        else super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar != 0 && Input.isLeftDown && EngineBase.dragged == null) {
            val dy2 = scrollY(if (isDownOnScrollbar > 0) dy / relativeSizeY else -dy.toDouble()).toFloat()
            if (dx != 0f || dy2 != 0f) super.onMouseMoved(x, y, dx, dy2)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): ScrollPanelY {
        val clone = ScrollPanelY(child.clone(), padding, style)
        copyInto(clone)
        return clone
    }
}