package me.anno.ui.base.scrolling

import me.anno.config.DefaultConfig
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawsOverY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.minWeight
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.ui.style.Style
import kotlin.math.max

// todo scroll smoothing
// todo for x as well

// todo UI animations for nice effects :)

// todo springs as well :) -> make the UI joyful
// todo layouts could have springs as well, so the elements move to their target position with lerp(pos,target,dt*10)

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
        setWeight(minWeight)
    }

    @NotSerializedProperty
    var lsp = -1f

    @NotSerializedProperty
    var lmsp = -1

    override var scrollPositionY = 0f

    @NotSerializedProperty
    private var isDownOnScrollbar = false

    val scrollbar = ScrollbarY(this, style)
    val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    val interactionWidth = scrollbarWidth + 2 * interactionPadding

    val hasScrollbar get() = maxScrollPositionY > 0

    override val maxScrollPositionY get() = max(0, child.minH + padding.height - h)

    override fun tickUpdate() {
        super.tickUpdate()
        val window = window!!
        val mx = window.mouseX.toInt()
        val my = window.mouseY.toInt()
        scrollbar.isBeingHovered = drawsOverlaysOverChildren(mx, my)
        if (scrollbar.updateAlpha()) invalidateDrawing()
        if (scrollPositionY != lsp || maxScrollPositionY != lmsp) {
            lsp = scrollPositionY
            lmsp = maxScrollPositionY
            window.needsLayout += this
        }
    }

    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbar && drawsOverY(this.lx0, this.ly0, this.lx1, this.ly1, sbWidth, lx0, ly0, lx1, ly1)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(w - padding.width, maxLength - padding.height)

        minW = child.minW + padding.width
        minH = child.minH + padding.height
        if (hasScrollbar) minW += scrollbarWidth
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        val scroll = scrollPositionY.toInt()
        child.placeInParent(x + padding.left, y + padding.top - scroll)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        if (hasScrollbar) {
            scrollbar.x = x1 - scrollbarWidth - scrollbarPadding
            scrollbar.y = y + scrollbarPadding
            scrollbar.w = scrollbarWidth
            scrollbar.h = h - 2 * scrollbarPadding
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val delta = -dy * scrollSpeed
        if ((delta > 0f && scrollPositionY >= maxScrollPositionY) ||
            (delta < 0f && scrollPositionY <= 0f)
        ) {// if done scrolling go up the hierarchy one
            super.onMouseWheel(x, y, dx, dy, byMouse)
        } else {
            scrollPositionY += delta
            clampScrollPosition()
            // we consumed dy
            if (dx != 0f) {
                super.onMouseWheel(x, y, dx, 0f, byMouse)
            }
        }
    }

    fun clampScrollPosition() {
        scrollPositionY = clamp(scrollPositionY, 0f, maxScrollPositionY.toFloat())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = drawsOverlaysOverChildren(x.toInt(), y.toInt())
        if (!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar) {
            if (dy != 0f) {
                scrollbar.onMouseMoved(x, y, 0f, dy)
                clampScrollPosition()
            }
            // y was consumed
            if (dx != 0f) super.onMouseMoved(x, y, dx, 0f)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): ScrollPanelY {
        val clone = ScrollPanelY(child.clone(), padding, style, alignmentX)
        copy(clone)
        return clone
    }

    override val className: String = "ScrollPanelY"

}