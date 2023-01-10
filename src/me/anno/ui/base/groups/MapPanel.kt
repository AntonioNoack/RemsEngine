package me.anno.ui.base.groups

import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawsOverX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawsOverY
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.scrolling.ScrollbarX
import me.anno.ui.base.scrolling.ScrollbarY
import me.anno.ui.style.Style
import org.joml.Vector2d

/** basis for simple 2d map controls with zoom-in/out and drag to move */
abstract class MapPanel(style: Style) : PanelList(style), ScrollableX, ScrollableY {

    val target = Vector2d()
    val center = Vector2d()

    open var scale = 1.0

    val centerX get() = x + w / 2
    val centerY get() = y + h / 2

    var minScale = 0.001
    var maxScale = 2.0

    fun windowToCoordsDirX(wx: Double) = wx / scale
    fun windowToCoordsDirY(wy: Double) = wy / scale

    fun coordsToWindowDirX(cx: Double) = cx * scale
    fun coordsToWindowDirY(cy: Double) = cy * scale

    fun windowToCoordsX(wx: Double) = (wx - centerX) / scale + center.x
    fun windowToCoordsY(wy: Double) = (wy - centerY) / scale + center.y

    fun coordsToWindowX(cx: Double) = (cx - center.x) * scale + centerX
    fun coordsToWindowY(cy: Double) = (cy - center.y) * scale + centerY

    override val childSizeX: Long get() = w.toLong()
    override val childSizeY: Long get() = h.toLong()

    private val hasScrollbarX get() = maxScrollPositionX > 0
    private val hasScrollbarY get() = maxScrollPositionY > 0

    override var scrollPositionX = 0.0
    override var scrollPositionY = 0.0

    private val scrollbarWidth = style.getSize("scrollbarWidth", 8)
    private val scrollbarHeight = style.getSize("scrollbarHeight", 8)
    private val scrollbarPadding = style.getSize("scrollbarPadding", 1)

    private val interactionWidth = scrollbarWidth + 2 * interactionPadding
    private val interactionHeight = scrollbarHeight + 2 * interactionPadding

    val scrollbarX = ScrollbarX(this, style)
    val scrollbarY = ScrollbarY(this, style)

    @NotSerializedProperty
    var isDownOnScrollbarX = false

    @NotSerializedProperty
    var isDownOnScrollbarY = false

    override fun scrollX(delta: Double) {
        scrollPositionX += delta
        moveMap(-delta, 0.0)
    }

    override fun scrollY(delta: Double) {
        scrollPositionY += delta
        moveMap(0.0, -delta)
    }

    abstract fun shallMoveMap(): Boolean

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (Input.isLeftDown) {
            if (isDownOnScrollbarX && rx != 0f) {
                scrollX(rx / relativeSizeX)
                rx = 0f
            }
            if (isDownOnScrollbarY && ry != 0f) {
                scrollY(ry / relativeSizeY)
                ry = 0f
            }
        }
        if (shallMoveMap()) moveMap(rx.toDouble(), ry.toDouble())
        else super.onMouseMoved(x, y, rx, ry)
    }

    /** move map in pixel space */
    open fun moveMap(dx: Double, dy: Double) {
        // moving around
        center.sub(dx / scale, dy / scale)
        target.set(center)
        window?.needsLayout?.add(this)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val oldX = windowToCoordsX(x.toDouble())
        val oldY = windowToCoordsY(y.toDouble())
        val multiplier = Maths.pow(1.05, dy.toDouble())
        scale = Maths.clamp(scale * multiplier, minScale, maxScale)
        val newX = windowToCoordsX(x.toDouble())
        val newY = windowToCoordsY(y.toDouble())
        // zoom in on the mouse pointer
        center.add(oldX - newX, oldY - newY)
        target.add(oldX - newX, oldY - newY)
        invalidateLayout()
    }

    fun mapMouseDown(x: Float, y: Float) {
        val xi = x.toInt()
        val yi = y.toInt()
        isDownOnScrollbarX = hasScrollbarX && drawsOverX(xi, yi)
        isDownOnScrollbarY = hasScrollbarY && drawsOverY(xi, yi)
    }

    fun layoutScrollbars(x1: Int, y1: Int) {
        val scrollbarX = scrollbarX
        scrollbarX.x = x + scrollbarPadding
        scrollbarX.y = y1 - scrollbarHeight - scrollbarPadding
        scrollbarX.w = w - 2 * scrollbarPadding
        scrollbarX.h = scrollbarHeight
        val scrollbarY = scrollbarY
        scrollbarY.x = x1 - scrollbarWidth - scrollbarPadding
        scrollbarY.y = y + scrollbarPadding
        scrollbarY.w = scrollbarWidth
        scrollbarY.h = h - 2 * scrollbarPadding
    }

    fun drawScrollbars(x0: Int, y0: Int, x1: Int, y1: Int) {
        layoutScrollbars(x1, y1)
        if (hasScrollbarX) drawChild(scrollbarX, x0, y0, x1, y1)
        if (hasScrollbarY) drawChild(scrollbarY, x0, y0, x1, y1)
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        mapMouseDown(x, y)
        if (!isDownOnScrollbarX && !isDownOnScrollbarY) super.onMouseDown(x, y, button)
    }

    fun mapMouseUp() {
        isDownOnScrollbarX = false
        isDownOnScrollbarY = false
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        mapMouseUp()
        super.onMouseUp(x, y, button)
    }

    private fun drawsOverX(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbarX && drawsOverX(this.lx0, this.ly0, this.lx1, this.ly1, sbHeight, lx0, ly0, lx1, ly1)
    }

    private fun drawsOverY(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbarY && drawsOverY(this.lx0, this.ly0, this.lx1, this.ly1, sbWidth, lx0, ly0, lx1, ly1)
    }

}