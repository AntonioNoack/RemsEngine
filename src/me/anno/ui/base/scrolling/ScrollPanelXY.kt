package me.anno.ui.base.scrolling

import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY.Companion.scrollSpeed
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.clamp
import kotlin.math.max

// todo make this class into its own class for less issues with the layout

open class ScrollPanelXY(
    child: Panel, padding: Padding,
    style: Style,
    alignX: AxisAlignment
) : PanelContainer(child, padding, style),
    ScrollableX,
    ScrollableY {

    constructor(child: Panel, style: Style) : this(child, Padding(), style, AxisAlignment.MIN)
    constructor(child: Panel, padding: Padding, style: Style) : this(child, padding, style, AxisAlignment.MIN)
    constructor(padding: Padding, align: AxisAlignment, style: Style) : this(PanelListY(style), padding, style, align)
    constructor(padding: Padding, style: Style) : this(
        PanelListY(style), padding, style,
        AxisAlignment.MIN
    )

    init {
        // child += WrapAlign(alignX, AxisAlignment.MIN)
        // weight = 0.0001f
    }

    open val content get() = child

    var lspX = -1f
    var lspY = -1f

    var lmspX = -1
    var lmspY = -1

    override fun tickUpdate() {
        super.tickUpdate()
        if (
            scrollPositionX != lspX ||
            scrollPositionY != lspY ||
            maxScrollPositionX != lmspX ||
            maxScrollPositionY != lmspY
        ) {
            lspX = scrollPositionX
            lspY = scrollPositionY
            lmspX = maxScrollPositionX
            lmspY = maxScrollPositionY
            window!!.needsLayout += this
        }
    }

    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return maxScrollPositionY > 0 && lx1 > this.lx1 - scrollbarWidth // overlaps on the right
    }

    override var scrollPositionX = 0f
    override var scrollPositionY = 0f

    override val maxScrollPositionX get() = max(0, child.minW + padding.width - w)
    override val maxScrollPositionY get() = max(0, child.minH + padding.height - h)

    var isDownOnScrollbarX = false
    var isDownOnScrollbarY = false

    val scrollbarX = ScrollbarX(this, style)
    val scrollbarY = ScrollbarY(this, style)

    val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    val scrollbarHeight = style.getSize("scrollbarWidth", 8)
    val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        child.calculateSize(maxLength - padding.width, maxLength - padding.height)

        minW = child.minW + padding.width
        minH = child.minH + padding.height

        if (maxScrollPositionY > 0) minW += scrollbarWidth
        if (maxScrollPositionX > 0) minH += scrollbarHeight

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        val scrollX = scrollPositionX.toInt()
        val scrollY = scrollPositionY.toInt()

        child.placeInParent(x + padding.left - scrollX, y + padding.top - scrollY)

    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        if (maxScrollPositionX > 0f) {
            val scrollbarX = scrollbarX
            scrollbarX.x = x + scrollbarPadding
            scrollbarX.y = y1 - scrollbarHeight - scrollbarPadding
            scrollbarX.w = w - 2 * scrollbarPadding
            scrollbarX.h = scrollbarHeight
            drawChild(scrollbarX, x0, y0, x1, y1)
        }
        if (maxScrollPositionY > 0f) {
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

        if (!consumedX || !consumedY) {
            val dx2 = if (consumedX) 0f else dx
            val dy2 = if (consumedY) 0f else dy
            super.onMouseWheel(x, y, dx2, dy2, byMouse)
        }

    }

    fun clampScrollPosition() {
        scrollPositionX = clamp(scrollPositionX, 0f, maxScrollPositionX.toFloat())
        scrollPositionY = clamp(scrollPositionY, 0f, maxScrollPositionY.toFloat())
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbarX = scrollbarX.contains(x, y, scrollbarPadding * 2)
        isDownOnScrollbarY = scrollbarY.contains(x, y, scrollbarPadding * 2)
        if (!isDownOnScrollbarX && !isDownOnScrollbarY) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbarX = false
        isDownOnScrollbarY = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        when {
            isDownOnScrollbarX -> {
                scrollbarX.onMouseMoved(x, y, dx, dy)
                clampScrollPosition()
            }
            isDownOnScrollbarY -> {
                scrollbarY.onMouseMoved(x, y, dx, dy)
                clampScrollPosition()
            }
            else -> super.onMouseMoved(x, y, dx, dy)
        }
    }

}


/*open class ScrollPanelXY(
    child: Panel, padding: Padding,
    style: Style,
    alignX: AxisAlignment,
    alignY: AxisAlignment
) :
    ScrollPanelX(
        ScrollPanelY(
            child,
            Padding(), style, alignY
        ),
        padding, style, alignX
    ) {

    constructor(child: Panel, style: Style) : this(
        child, Padding(), style, AxisAlignment.MIN, AxisAlignment.MIN
    )

    constructor(padding: Padding, style: Style) : this(
        PanelListY(style), padding, style,
        AxisAlignment.MIN,
        AxisAlignment.MIN
    )

    val content = (this.child as ScrollPanelY).child

}*/