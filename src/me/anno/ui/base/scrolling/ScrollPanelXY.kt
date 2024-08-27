package me.anno.ui.base.scrolling


import me.anno.Time.uiDeltaTime
import me.anno.config.ConfigRef
import me.anno.engine.EngineBase
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.Color.mulAlpha
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

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
        scrollbarX.isHovered = drawsOverX(mx, my)
        scrollbarY.isHovered = drawsOverY(mx, my)
        scrollPositionX = mix(scrollPositionX, targetScrollPositionX, dtTo01(uiDeltaTime * scrollHardnessX))
        scrollPositionY = mix(scrollPositionY, targetScrollPositionY, dtTo01(uiDeltaTime * scrollHardnessY))
        if (scrollbarX.updateAlpha()) invalidateDrawing()
        if (scrollbarY.updateAlpha()) invalidateDrawing()
        if (
            round(scrollPositionX) != lastScrollPosX ||
            round(scrollPositionY) != lastScrollPosY ||
            maxScrollPositionX != lastMaxScrollPosX ||
            maxScrollPositionY != lastMaxScrollPosY
        ) {
            lastScrollPosX = round(scrollPositionX)
            lastScrollPosY = round(scrollPositionY)
            lastMaxScrollPosX = maxScrollPositionX
            lastMaxScrollPosY = maxScrollPositionY
            setPosition(x, y)
            invalidateDrawing()
        }
    }

    override var scrollPositionX = 0.0
    override var scrollPositionY = 0.0

    override var targetScrollPositionX = 0.0
    override var targetScrollPositionY = 0.0

    override var scrollHardnessX = 25.0
    override var scrollHardnessY = 25.0

    override val maxScrollPositionX get() = max(0, maxScrollPositionXRaw)
    override val maxScrollPositionY get() = max(0, maxScrollPositionYRaw)
    val maxScrollPositionXRaw get() = (child.minW + padding.width - width).toLong()
    val maxScrollPositionYRaw get() = (child.minH + padding.height - height).toLong()

    private val scrollbarX = ScrollbarX(this, style)
    private val scrollbarY = ScrollbarY(this, style)

    private val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    private val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    private val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    private val interactionWidth = scrollbarWidth + 2 * interactionPadding
    private val interactionHeight = scrollbarHeight + 2 * interactionPadding

    private val hasScrollbarX: Boolean get() = maxScrollPositionX > 0
    private val hasScrollbarY: Boolean get() = maxScrollPositionY > 0
    private val hasScrollbarXF: Float get() = clamp(maxScrollPositionXRaw / (scrollbarHeight * 3f) + 1f)
    private val hasScrollbarYF: Float get() = clamp(maxScrollPositionYRaw / (scrollbarWidth * 3f) + 1f)

    override val childSizeX: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeX else child.minW.toLong()
        }

    override val childSizeY: Long
        get() {
            val child = child
            return if (child is LongScrollable) child.sizeY else child.minH.toLong()
        }

    override fun scrollX(delta: Double): Double {
        val prev = targetScrollPositionX
        targetScrollPositionX += delta
        clampScrollPosition()
        val moved = targetScrollPositionX - prev
        return delta - moved
    }

    override fun scrollY(delta: Double): Double {
        val prev = targetScrollPositionY
        targetScrollPositionY += delta
        clampScrollPosition()
        val moved = targetScrollPositionY - prev
        return delta - moved
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return drawsOverX(lx0, ly0, lx1, ly1) || drawsOverY(lx0, ly0, lx1, ly1)
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return (hasScrollbarX && ly1 >= y + height - scrollbarHeight) ||
                (hasScrollbarY && lx1 >= x + width - scrollbarWidth)
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
        // calculation must not depend on hasScrollbar, or we get flickering
        val paddingX = padding.width + (hasScrollbarXF * scrollbarWidth).toInt()
        val paddingY = padding.height + (hasScrollbarYF * scrollbarHeight).toInt()
        child.calculateSize(maxLength - paddingX, maxLength - paddingY)
        minW = min(w, child.minW + paddingX)
        minH = min(h, child.minH + paddingY)
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChild()
    }

    fun placeChild() {
        val child = child
        val padding = padding

        val scrollX0 = scrollPositionX.toLong()
        val scrollY0 = scrollPositionY.toLong()
        val scrollX1 = clamp(scrollX0, 0L, max(0, child.minW + padding.width - width).toLong()).toInt()
        val scrollY1 = clamp(scrollY0, 0L, max(0, child.minH + padding.height - height).toLong()).toInt()

        val cx = x + padding.left - scrollX1
        val cy = y + padding.top - scrollY1
        val paddingX = padding.width + (hasScrollbarXF * scrollbarWidth).toInt()
        val paddingY = padding.height + (hasScrollbarYF * scrollbarHeight).toInt()
        val cw = max(child.minW, width - paddingX)
        val ch = max(child.minH, height - paddingY)
        child.setPosSize(cx, cy, cw, ch)

        if (child is LongScrollable) {
            child.setExtraScrolling(scrollX0 - scrollX1, scrollY0 - scrollY1)
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        clampScrollPosition()
        super.onDraw(x0, y0, x1, y1)
        val batch = DrawRectangles.startBatch()
        if (alwaysShowShadowX) {
            drawShadowX(x0, y0, x1, y1, shadowRadius)
        }
        if (alwaysShowShadowY) {
            drawShadowY(x0, y0, x1, y1, shadowRadius)
        }
        if (hasScrollbarX) {
            if (!alwaysShowShadowX) {
                val shadowRadius = min(maxScrollPositionX, shadowRadius.toLong()).toInt()
                drawShadowX(x0, y0, x1, y1, shadowRadius)
            }
            val scrollbarX = scrollbarX
            scrollbarX.x = x + scrollbarPadding
            scrollbarX.y = y + height - scrollbarHeight - scrollbarPadding
            scrollbarX.width = width - 2 * scrollbarPadding
            scrollbarX.height = scrollbarHeight
            drawChild(scrollbarX, x0, y0, x1, y1)
        }
        if (hasScrollbarY) {
            if (!alwaysShowShadowY) {
                val shadowRadius = min(maxScrollPositionY, shadowRadius.toLong()).toInt()
                drawShadowY(x0, y0, x1, y1, shadowRadius)
            }
            val scrollbarY = scrollbarY
            scrollbarY.x = x + width - scrollbarWidth - scrollbarPadding
            scrollbarY.y = y + scrollbarPadding
            scrollbarY.width = scrollbarWidth
            scrollbarY.height = height - 2 * scrollbarPadding
            drawChild(scrollbarY, x0, y0, x1, y1)
        }
        DrawRectangles.finishBatch(batch)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {

        var consumedX = false
        var consumedY = false

        val dx0 = dx * scrollSpeed
        if ((dx0 > 0f && scrollPositionX >= maxScrollPositionX) ||
            (dx0 < 0f && scrollPositionX <= 0f)
        ) {// if done scrolling go up the hierarchy one
        } else {
            scrollX(dx0.toDouble())
            clampScrollPosition()
            consumedX = true
        }

        val dy0 = -dy * scrollSpeed
        if ((dy0 > 0f && scrollPositionY >= maxScrollPositionY) ||
            (dy0 < 0f && scrollPositionY <= 0f)
        ) {// if done scrolling go up the hierarchy one
        } else {
            scrollY(dy0.toDouble())
            clampScrollPosition()
            consumedY = true
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
        targetScrollPositionX = clamp(targetScrollPositionX, 0.0, maxScrollPositionX.toDouble())
        targetScrollPositionY = clamp(targetScrollPositionY, 0.0, maxScrollPositionY.toDouble())
    }

    @NotSerializedProperty
    private var isDownOnScrollbarX = 0

    @NotSerializedProperty
    private var isDownOnScrollbarY = 0

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            val xi = x.toInt()
            val yi = y.toInt()
            isDownOnScrollbarX = if (hasScrollbarX && drawsOverX(xi, yi)) 1 else -1
            isDownOnScrollbarY = if (hasScrollbarY && drawsOverY(xi, yi)) 1 else -1
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            isDownOnScrollbarX = 0
            isDownOnScrollbarY = 0
        }
        super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (isDownOnScrollbarX != 0 && rx != 0f && EngineBase.dragged == null) {
            rx = scrollX(if (isDownOnScrollbarX > 0) rx / relativeSizeX else -rx.toDouble()).toFloat()
        }
        if (isDownOnScrollbarY != 0 && ry != 0f && EngineBase.dragged == null) {
            ry = scrollY(if (isDownOnScrollbarY > 0) ry / relativeSizeY else -ry.toDouble()).toFloat()
        }
        if (rx != 0f || ry != 0f) {
            super.onMouseMoved(x, y, rx, ry)
        }
    }

    override fun clone(): PanelContainer {
        val clone = ScrollPanelXY(child.clone(), padding, style)
        copyInto(clone)
        return clone
    }

    companion object {

        val scrollSpeed by ConfigRef("ui.scroll.speed", 30f)

        @Suppress("unused_parameter")
        fun drawsOverX(
            lx0: Int, ly0: Int, lx1: Int, ly1: Int,
            sbHeight: Int,
            x0: Int, y0: Int, x1: Int = x0 + 1, y1: Int = y0 + 1
        ) = overlaps(
            lx0, ly1 - sbHeight, lx1, ly1,
            x0, y0, x1, y1
        )

        @Suppress("unused_parameter")
        fun drawsOverY(
            lx0: Int, ly0: Int, lx1: Int, ly1: Int,
            sbWidth: Int,
            x0: Int, y0: Int, x1: Int = x0 + 1, y1: Int = y0 + 1
        ) = overlaps(lx1 - sbWidth, ly0, lx1, ly1, x0, y0, x1, y1)

        fun overlaps(
            x0: Int, y0: Int, x1: Int, y1: Int,
            x2: Int, y2: Int, x3: Int, y3: Int
        ): Boolean {
            return abs((x0 + x1) - (x2 + x3)) < (x1 - x0) + (x3 - x2) &&
                    abs((y0 + y1) - (y2 + y3)) < (y1 - y0) + (y3 - y2)
        }

        fun PanelContainer.drawShadowX(x0: Int, y0: Int, x1: Int, y1: Int, shadowRadius: Int) {
            drawShadowX(x0, y0, x1, y1, shadowColor, shadowRadius)
        }

        fun PanelContainer.drawShadowY(x0: Int, y0: Int, x1: Int, y1: Int, shadowRadius: Int) {
            drawShadowY(x0, y0, x1, y1, shadowColor, shadowRadius)
        }

        fun Panel.drawShadowX(x0: Int, y0: Int, x1: Int, y1: Int, shadowColor: Int, shadowRadius: Int) {
            // draw left shadow
            for (x in max(x0, x) until min(x1, x + shadowRadius)) {
                val alpha = Maths.sq(1f - (x - this.x).toFloat() / shadowRadius)
                DrawRectangles.drawRect(x, y0, 1, y1 - y0, shadowColor.mulAlpha(alpha))
            }

            // draw right shadow
            for (x in max(x0, x + width - shadowRadius) until min(x1, x + width)) {
                val alpha = Maths.sq(1f - ((this.x + width) - x).toFloat() / shadowRadius)
                DrawRectangles.drawRect(x, y0, 1, y1 - y0, shadowColor.mulAlpha(alpha))
            }
        }

        fun Panel.drawShadowY(x0: Int, y0: Int, x1: Int, y1: Int, shadowColor: Int, shadowRadius: Int) {
            // draw top shadow
            for (y in max(y0, y) until min(y1, y + shadowRadius)) {
                val alpha = Maths.sq(1f - (y - this.y).toFloat() / shadowRadius)
                DrawRectangles.drawRect(x0, y, x1 - x0, 1, shadowColor.mulAlpha(alpha))
            }

            // draw bottom shadow
            for (y in max(y0, y + height - shadowRadius) until min(y1, y + height)) {
                val alpha = Maths.sq(1f - ((this.y + height) - y).toFloat() / shadowRadius)
                DrawRectangles.drawRect(x0, y, x1 - x0, 1, shadowColor.mulAlpha(alpha))
            }
        }
    }
}
