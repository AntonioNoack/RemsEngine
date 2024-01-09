package me.anno.ui.custom

import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.scrolling.Scrollbar
import me.anno.utils.Color.mixARGB
import me.anno.utils.types.Booleans.toInt
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Horizontal/Vertical (isY) list, where the user can decide the width/height of each element,
 * so they can focus on what is most important to them.
 * */
open class CustomSizeContainer(val isX: Boolean, val isY: Boolean, child: Panel, style: Style) :
    PanelContainer(child, Padding.Zero, style) {

    private val spacing = style.getSize("customList.spacing", 4)
    private val scrollbars = ArrayList<Scrollbar>()

    private var customSizeX = 100
    private var customSizeY = 100

    init {
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.FILL
        weight = 1f
        if (isX) scrollbars.add(Scrollbar(style))
        if (isY) scrollbars.add(Scrollbar(style))
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        child.calculateSize(
            if (isX) customSizeX else w - padding.width,
            if (isY) customSizeY else h - padding.height
        )
        minW = (if (isX) customSizeX + spacing else child.minW) + padding.width
        minH = (if (isY) customSizeY + spacing else child.minH) + padding.height
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        child.setPosition(x + padding.left, y + padding.top)
    }

    override fun setSize(w: Int, h: Int) {
        super.setSize(w, h)
        child.setSize(
            if (isX) min(customSizeX, w - padding.width) else w - padding.width,
            if (isY) min(customSizeY, h - padding.height) else h - padding.height
        )
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        if (max(dx, dy) >= 0) return true
        return super.capturesChildEvents(lx0, ly0, lx1, ly1)
    }

    private var isDownIndex = -1

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            // find where the mouse went down
            for (index in scrollbars.indices) {
                val scrollbar = scrollbars[index]
                if (scrollbar.isBeingHovered) {
                    isDownIndex = index
                    return
                }
            }
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) isDownIndex = -1
        else super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (isDownIndex >= 0) {
            if (isX && abs(dx) >= 0.5f) {
                customSizeX = max(customSizeX + dx.roundToInt(), interactionPadding)
                invalidateLayout()
                rx = 0f
            }
            if (isY && abs(dy) >= 0.5f) {
                customSizeY = max(customSizeY + dy.roundToInt(), interactionPadding)
                invalidateLayout()
                ry = 0f
            }
        }
        super.onMouseMoved(x, y, rx, ry)
    }

    override fun onUpdate() {
        super.onUpdate()
        for (index in scrollbars.indices) {
            val scrollbar = scrollbars[index]
            updateScrollbar(scrollbar, !isX || index > 0)
        }
    }

    fun updateScrollbar(scrollbar: Scrollbar, isYBar: Boolean) {
        scrollbar.isBeingHovered = (if (isYBar) dy else dx) >= 0
        if (scrollbar.updateAlpha()) {
            invalidateDrawing()
        }
    }

    private val hoverColor = style.getColor("customList.hoverColor", mixARGB(0x77ffb783, originalBGColor, 0.8f))

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        backgroundColor = hoverColor
        drawBackground(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
        if (isX) {
            val scrollbar = scrollbars[0]
            scrollbar.x = child.x + child.width
            scrollbar.y = y
            scrollbar.width = spacing
            scrollbar.height = height
            updateScrollbar(scrollbar, false)
            drawChild(scrollbar, x0, y0, x1, y1)
        }
        if (isY) {
            val scrollbar = scrollbars[isX.toInt()]
            scrollbar.x = x
            scrollbar.y = child.y + child.height
            scrollbar.width = width
            scrollbar.height = spacing
            updateScrollbar(scrollbar, true)
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    val dx get() = window!!.mouseXi - (child.x + child.width) + interactionPadding
    val dy get() = window!!.mouseYi - (child.y + child.height) + interactionPadding

    override fun getCursor(): Cursor? {
        val dx = dx >= 0
        val dy = dy >= 0
        return when {
            dx && dy -> Cursor.resize
            dx -> Cursor.hResize
            dy -> Cursor.vResize
            else -> super.getCursor()
        }
    }

    override val className: String get() = "CustomSizeContainer"
}