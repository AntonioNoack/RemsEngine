package me.anno.ui.base.groups

import me.anno.Time.uiDeltaTime
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.drawing.DrawRectangles
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.ui.Style
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawsOverX
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.drawsOverY
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.scrolling.ScrollbarX
import me.anno.ui.base.scrolling.ScrollbarY
import org.joml.Vector2d
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln

/** basis for simple 2d map controls with zoom-in/out and drag to move */
abstract class MapPanel(style: Style) : PanelList(style), ScrollableX, ScrollableY {

    val target = Vector2d()
    val center = Vector2d()

    val targetScale = Vector2d(1.0)
    val scale = Vector2d(1.0)

    val isZooming get() = targetScale != scale

    val centerX get() = x + width / 2
    val centerY get() = y + height / 2

    val minScale = Vector2d(0.5)
    val maxScale = Vector2d(1000.0)

    // most implementations will do so
    override val canDrawOverBorders: Boolean get() = true

    open fun onChangeSize() {
    }

    fun windowToCoordsDirX(wx: Double) = wx / scale.x
    fun windowToCoordsDirY(wy: Double) = wy / scale.y

    fun coordsToWindowDirX(cx: Double) = cx * scale.x
    fun coordsToWindowDirY(cy: Double) = cy * scale.y

    fun windowToCoordsX(wx: Double) = (wx - centerX) / scale.x + center.x
    fun windowToCoordsY(wy: Double) = (wy - centerY) / scale.y + center.y

    fun windowToCoordsX(wx: Float) = windowToCoordsX(wx.toDouble()).toFloat()
    fun windowToCoordsY(wy: Float) = windowToCoordsY(wy.toDouble()).toFloat()

    fun coordsToWindowX(cx: Double) = (cx - center.x) * scale.x + centerX
    fun coordsToWindowY(cy: Double) = (cy - center.y) * scale.y + centerY

    fun coordsToWindowX(cx: Float) = coordsToWindowX(cx.toDouble()).toFloat()
    fun coordsToWindowY(cy: Float) = coordsToWindowY(cy.toDouble()).toFloat()

    override val childSizeX: Long get() = width.toLong()
    override val childSizeY: Long get() = height.toLong()

    private val hasScrollbarX get() = maxScrollPositionX > 0
    private val hasScrollbarY get() = maxScrollPositionY > 0

    override var scrollPositionX = 0.0
    override var scrollPositionY = 0.0

    // todo use these for smoothed movement
    override var scrollHardnessX = 25.0
    override var scrollHardnessY = 25.0
    override var targetScrollPositionX = 0.0
    override var targetScrollPositionY = 0.0

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

    override fun scrollX(delta: Double): Double {
        scrollPositionX += delta
        moveMap(-delta, 0.0)
        return 0.0
    }

    override fun scrollY(delta: Double): Double {
        scrollPositionY += delta
        moveMap(0.0, -delta)
        return 0.0
    }

    override fun onUpdate() {
        val dtx = dtTo01(5.0 * uiDeltaTime)
        if (abs(1.0 - targetScale.y / scale.y) > 0.01) {
            scale.x = exp(mix(ln(scale.x), ln(targetScale.x), dtx))
            scale.y = exp(mix(ln(scale.y), ln(targetScale.y), dtx))
            onChangeSize()
            invalidateLayout()
        } else if (scale != targetScale) {
            scale.set(targetScale)
            onChangeSize()
            invalidateLayout()
        }
        if (target.distanceSquared(center) > 1e-5) {
            invalidateLayout()
        }
        center.mix(target, dtx)
        super.onUpdate()
    }

    override fun invalidateLayout() {
        window?.addNeedsLayout(this)
    }

    open fun shallMoveMap(): Boolean = Input.isLeftDown

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
        if (shallMoveMap() && (rx != 0f || ry != 0f)) moveMap(rx.toDouble(), ry.toDouble())
        else super.onMouseMoved(x, y, rx, ry)
    }

    /** move map in pixel space */
    open fun moveMap(dx: Double, dy: Double) {
        // moving around
        center.sub(dx / scale.x, dy / scale.y)
        target.set(center)
        invalidateLayout()
    }

    fun moveMapTo(position: Vector2d) {
        target.set(position)
        invalidateLayout()
    }

    fun teleportMapTo(position: Vector2d) {
        center.set(position)
        target.set(position)
        invalidateLayout()
    }

    fun teleportScaleTo(newScale: Vector2d) {
        scale.set(newScale)
        targetScale.set(newScale)
        invalidateLayout()
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        // zoom in on the mouse pointer
        val oldInvX = 1.0 / targetScale.x
        val oldInvY = 1.0 / targetScale.y
        val multiplier = Maths.pow(1.05, dy.toDouble())
        targetScale.mul(multiplier)
        targetScale.max(minScale)
        targetScale.min(maxScale)
        val dxi = (x - centerX) * (oldInvY - 1.0 / targetScale.y)
        val dyi = (y - centerY) * (oldInvX - 1.0 / targetScale.x)
        target.add(dxi, dyi)
        invalidateLayout()
    }

    fun mapMouseDown(x: Float, y: Float) {
        val xi = x.toInt()
        val yi = y.toInt()
        isDownOnScrollbarX = hasScrollbarX && drawsOverX(xi, yi)
        isDownOnScrollbarY = hasScrollbarY && drawsOverY(xi, yi)
    }

    fun draw2DLineGrid(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, gridSize: Double) {
        val gridX0 = windowToCoordsX(x0.toDouble())
        val gridX1 = windowToCoordsX(x1.toDouble())
        val gridY0 = windowToCoordsY(y0.toDouble())
        val gridY1 = windowToCoordsY(y1.toDouble())
        val i0 = floor(gridX0 / gridSize).toLong()
        val i1 = ceil(gridX1 / gridSize).toLong()
        val j0 = floor(gridY0 / gridSize).toLong()
        val j1 = ceil(gridY1 / gridSize).toLong()
        for (i in i0 until i1) {
            val gridX = i * gridSize
            val windowX = coordsToWindowX(gridX).toInt()
            if (windowX in x0 until x1) DrawRectangles.drawRect(windowX, y0, 1, y1 - y0, color)
        }
        for (j in j0 until j1) {
            val gridY = j * gridSize
            val windowY = coordsToWindowY(gridY).toInt()
            if (windowY in y0 until y1) DrawRectangles.drawRect(x0, windowY, x1 - x0, 1, color)
        }
    }

    fun layoutScrollbars(x1: Int, y1: Int) {
        val scrollbarX = scrollbarX
        scrollbarX.x = x + scrollbarPadding
        scrollbarX.y = y1 - scrollbarHeight - scrollbarPadding
        scrollbarX.width = width - 2 * scrollbarPadding
        scrollbarX.height = scrollbarHeight
        val scrollbarY = scrollbarY
        scrollbarY.x = x1 - scrollbarWidth - scrollbarPadding
        scrollbarY.y = y + scrollbarPadding
        scrollbarY.width = scrollbarWidth
        scrollbarY.height = height - 2 * scrollbarPadding
    }

    fun drawScrollbars(x0: Int, y0: Int, x1: Int, y1: Int) {
        layoutScrollbars(x1, y1)
        if (hasScrollbarX) drawChild(scrollbarX, x0, y0, x1, y1)
        if (hasScrollbarY) drawChild(scrollbarY, x0, y0, x1, y1)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT || key == Key.BUTTON_MIDDLE || key == Key.BUTTON_RIGHT) {
            mapMouseDown(x, y)
            if (!isDownOnScrollbarX && !isDownOnScrollbarY) super.onKeyDown(x, y, key)
        } else super.onKeyDown(x, y, key)
    }

    fun mapMouseUp() {
        isDownOnScrollbarX = false
        isDownOnScrollbarY = false
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT || key == Key.BUTTON_MIDDLE || key == Key.BUTTON_RIGHT) mapMouseUp()
        super.onKeyUp(x, y, key)
    }

    private fun drawsOverX(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbHeight = interactionHeight + 2 * scrollbarPadding
        return hasScrollbarX && drawsOverX(this.lx0, this.ly0, this.lx1, this.ly1, sbHeight, lx0, ly0, lx1, ly1)
    }

    private fun drawsOverY(lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbarY && drawsOverY(this.lx0, this.ly0, this.lx1, this.ly1, sbWidth, lx0, ly0, lx1, ly1)
    }

    fun getCursorPosition(x: Float, y: Float, dst: Vector3d = Vector3d()): Vector3d {
        dst.x = windowToCoordsX(x.toDouble())
        dst.y = windowToCoordsY(y.toDouble())
        dst.z = 0.0
        return dst
    }
}