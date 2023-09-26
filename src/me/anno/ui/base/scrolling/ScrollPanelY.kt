package me.anno.ui.base.scrolling

import me.anno.Time.deltaTime
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.minWeight
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.utils.types.Booleans.toInt
import kotlin.math.max
import kotlin.math.round

open class ScrollPanelY(
    child: Panel, padding: Padding,
    style: Style,
    alignX: AxisAlignment
) : PanelContainer(child, padding, style), ScrollableY {

    constructor(style: Style) : this(PanelListY(style), style)
    constructor(child: Panel, style: Style) : this(child, Padding(), style, AxisAlignment.MIN)
    constructor(child: Panel, padding: Padding, style: Style) : this(child, padding, style, AxisAlignment.MIN)
    constructor(padding: Padding, align: AxisAlignment, style: Style) : this(PanelListY(style), padding, style, align)

    init {
        child += WrapAlign(alignX, AxisAlignment.MIN)
        weight = minWeight
    }

    @NotSerializedProperty
    var lastScrollPosY = -1.0

    @NotSerializedProperty
    var lastMaxScrollPosY = -1L

    override var scrollHardnessY = 25.0

    override var scrollPositionY = 0.0
    override var targetScrollPositionY = 0.0

    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionWidth = scrollbarWidth + 2 * interactionPadding

    val hasScrollbar get() = maxScrollPositionY > 0

    override val childSizeY: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeY else child.minH.toLong()
        }

    override val maxScrollPositionY: Long
        get() {
            val child = child
            val childH = if (child is LongScrollable) child.sizeY else child.minH.toLong()
            return max(0, childH + padding.height - height)
        }

    override fun scrollY(delta: Double): Double {
        if (delta == 0.0) return 0.0
        val old = targetScrollPositionY
        val new = clamp(old + delta, 0.0, maxScrollPositionY.toDouble())
        targetScrollPositionY = new
        return new - (old + delta) // remaining scroll amount
    }

    override fun onUpdate() {
        super.onUpdate()
        val window = window
        if (window != null) {
            val mx = window.mouseXi
            val my = window.mouseYi
            scrollbar.isBeingHovered = capturesChildEvents(mx, my)
        }
        scrollPositionY = mix(scrollPositionY, targetScrollPositionY, dtTo01(deltaTime * scrollHardnessY))
        if (scrollbar.updateAlpha()) invalidateDrawing()
        if (round(scrollPositionY) != lastScrollPosY || maxScrollPositionY != lastMaxScrollPosY) {
            lastScrollPosY = round(scrollPositionY)
            lastMaxScrollPosY = maxScrollPositionY
            placeChild()
            invalidateDrawing()
        }
    }

    fun placeChild() {
        val child = child
        val padding = padding
        val scroll0 = round(scrollPositionY).toLong()
        val scroll = clamp(scroll0, 0L, max(0, child.minH + padding.height - height).toLong()).toInt()
        child.setPosition(x + padding.left, y + padding.top - scroll)
        if (child is LongScrollable) {
            child.setExtraScrolling(0L, scroll0 - scroll)
        }
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverY(
            this.lx0, this.ly0, this.lx1, this.ly1,
            sbWidth, lx0, ly0, lx1, ly1
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val child = child
        val padding = padding
        child.calculateSize(w - padding.width, maxLength - padding.height)

        minW = child.minW + padding.width + hasScrollbar.toInt(scrollbarWidth)
        minH = child.minH + padding.height
    }

    override fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        placeChild()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        if (hasScrollbar) {
            val scrollbar = scrollbar
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.width = scrollbarWidth
            scrollbar.height = height - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
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

    @NotSerializedProperty
    private var isDownOnScrollbar = 0

    override fun onMouseDown(x: Float, y: Float, button: Key) {
        if (button == Key.BUTTON_LEFT) isDownOnScrollbar = if (capturesChildEvents(x.toInt(), y.toInt())) 1 else -1
        else super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: Key) {
        isDownOnScrollbar = 0
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar != 0 && Input.isLeftDown && StudioBase.dragged == null) {
            // todo test this remainder using scroll panels inside scroll panels
            val dy2 = scrollY(if (isDownOnScrollbar > 0) dy / relativeSizeY else -dy.toDouble()).toFloat()
            if (dx != 0f || dy2 != 0f) super.onMouseMoved(x, y, dx, dy2)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): ScrollPanelY {
        val clone = ScrollPanelY(child.clone(), padding, style, alignmentX)
        copyInto(clone)
        return clone
    }

    override val className: String get() = "ScrollPanelY"
}