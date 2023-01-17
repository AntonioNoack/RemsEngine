package me.anno.ui.base.groups

import me.anno.Engine
import me.anno.Engine.deltaTime
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.MouseButton
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.scrolling.ScrollPanelXY.Companion.scrollSpeed
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.base.scrolling.ScrollbarY
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min

class PanelList2D(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style), ScrollableY {

    constructor(style: Style) : this(null, style)

    constructor(base: PanelList2D) : this(base.sorter, base.style) {
        base.copy(this)
    }

    override val children = ArrayList<Panel>(256)

    override val childSizeY: Long
        get() = minH.toLong()

    val defaultSize = 100
    var scaleChildren = false

    var childWidth: Int = style.getSize("childWidth", defaultSize)
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var childHeight: Int = style.getSize("childHeight", defaultSize)
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    override fun invalidateLayout() {
        window?.needsLayout?.add(this)
    }

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0
    var minH2 = 0

    var maxColumns = Int.MAX_VALUE

    override var scrollPositionY = 0.0
    override var targetScrollPositionY = 0.0
    override var scrollHardnessY = 25.0
    var isDownOnScrollbar = false

    override val maxScrollPositionY get(): Long = max(0, minH2 - h).toLong()

    override fun scrollY(delta: Double) {
        targetScrollPositionY += delta
        clampScrollPosition()
        window?.needsLayout?.add(this)
    }

    val scrollbar = ScrollbarY(this, style)
    var scrollbarWidth = style.getSize("scrollbarWidth", 8)
    var scrollbarPadding = style.getSize("scrollbarPadding", 1)

    override fun calculateSize(w: Int, h: Int) {

        val children = children
        if (sorter != null) {
            children.sortWith(sorter)
        }

        val w2 = min(w, childWidth * children.size)

        updateSize(w2)

        // only execute for visible children
        for (i in visibleIndex0 until visibleIndex1) {
            val child = children[i]
            if (child.isVisible) {
                child.calculateSize(calcChildWidth, calcChildHeight)
            }
        }

    }

    override val visibleIndex0
        get() = 0 // max((ly0 - (y + spacing - scrollPositionY.toInt())) / childHeight, 0) * columns
    override val visibleIndex1
        get() = children.size /*min(
            (ly1 - (y + spacing - scrollPositionY.toInt()) + childHeight - 1) / childHeight * columns,
            children.size
        )*/

    var autoScrollTargetPosition = 0.0
    var autoScrollEndTime = 0L
    var autoScrollPerNano = 1f
    var autoScrollLastUpdate = 0L

    fun scrollTo(itemIndex: Int, fractionY: Float) {
        val child = children.getOrNull(itemIndex) ?: return
        val currentY = child.y + fractionY * child.h
        val targetY = windowStack.mouseY
        val newScrollPosition = scrollPositionY + (currentY - targetY)
        smoothlyScrollTo(newScrollPosition, 0.25f)
    }

    override fun onUpdate() {
        super.onUpdate()
        val window = window
        if (window != null) {
            val mx = window.mouseXi
            val my = window.mouseYi
            scrollbar.isBeingHovered = capturesChildEvents(mx, my)
            if (scrollbar.updateAlpha()) invalidateDrawing()
        }
        if (autoScrollLastUpdate < autoScrollEndTime) {
            val delta = autoScrollPerNano * (Engine.gameTime - autoScrollLastUpdate)
            if (delta > 0L) {
                scrollPositionY = if (autoScrollLastUpdate < autoScrollEndTime) {
                    mix(scrollPositionY, autoScrollTargetPosition, dtTo01(delta.toDouble()))
                } else autoScrollTargetPosition
                targetScrollPositionY = scrollPositionY
                clampScrollPosition()
                invalidateLayout()
                autoScrollLastUpdate = Engine.gameTime
            }
        } else {
            scrollPositionY = mix(scrollPositionY, targetScrollPositionY, dtTo01(deltaTime * scrollHardnessY))
        }
    }

    val interactionWidth = scrollbarWidth + 2 * interactionPadding
    val hasScrollbar get() = maxScrollPositionY > 0

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val sbWidth = interactionWidth + 2 * scrollbarPadding
        return hasScrollbar && ScrollPanelXY.drawsOverY(
            this.lx0, this.ly0, this.lx1, this.ly1,
            sbWidth, lx0, ly0, lx1, ly1
        )
    }

    fun smoothlyScrollTo(y: Double, duration: Float = 1f) {
        autoScrollTargetPosition = clamp(y, 0.0, maxScrollPositionY.toDouble())
        autoScrollPerNano = 5e-9f / duration
        autoScrollEndTime = Engine.gameTime + (duration * 1e9f).toLong()
        autoScrollLastUpdate = Engine.gameTime
        if (duration <= 0f) scrollPositionY = autoScrollTargetPosition
        invalidateDrawing()
    }

    override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        if (children.isEmpty()) return null
        val i = getItemIndexAt(x, y)
        val panelAt = children[i].getPanelAt(x, y)
        return if (panelAt != null && panelAt.isOpaqueAt(x, y)) panelAt else null
    }

    fun getItemIndexAt(cx: Int, cy: Int): Int {
        val scroll = scrollPositionY.toInt()
        val lw = (calcChildWidth + spacing)
        val lh = (calcChildHeight + spacing)
        if (lw < 1 || lh < 1 || children.size < 2) return children.size - 1
        // cx = x + ix * (calcChildWidth + spacing) + spacing
        val itemX = (cx - x - spacing) / lw
        val itemY = (cy - y - spacing + scroll) / lh
        val ci = min(max(itemX + itemY * columns, 0), children.lastIndex)
        return if (ci > 0 && itemX > 0) {
            val p0 = children[ci]
            val p1 = children[ci - 1]
            val d1 = sq(cx - (p1.x + p1.w / 2f), cy - (p1.y + p1.h / 2f))
            val d0 = sq(cx - (p0.x + p0.w / 2f), cy - (p0.y + p0.h / 2f))
            if (d1 < d0) ci - 1 else ci
        } else ci
    }

    fun getItemFractionY(y: Float): Double {
        val ly = y - this.y + scrollPositionY - spacing
        val itemY = ly * rows / h
        return fract(itemY)
    }

    private fun updateCount() {
        columns = min(max(1, (w + spacing) / (childWidth + spacing)), maxColumns)
        rows = max(1, (children.size + columns - 1) / columns)
    }

    private fun updateScale() {
        val childScale = if (scaleChildren) max(1f, ((w + spacing) / columns - spacing) * 1f / childWidth) else 1f
        calcChildWidth = if (scaleChildren) (childWidth * childScale).toInt() else childWidth
        calcChildHeight = if (scaleChildren) (childHeight * childScale).toInt() else childHeight
    }

    private fun updateSize(w: Int) {
        updateCount()
        updateScale()
        minW = min(w, children.size * (calcChildWidth + spacing) + spacing)
        minH = (calcChildHeight + spacing) * rows - spacing
        minH2 = minH
    }

    override fun setSize(w: Int, h: Int) {
        updateSize(w)
        super.setSize(w, h)
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)

        val w = w - scrollbarWidth
        val contentW = columns * childWidth

        val scroll = scrollPositionY.toInt()

        // only place visible children + all that were previously visible
        val vi0 = visibleIndex0
        val vi1 = visibleIndex1
        val idx0 = max(min(vi0, lpi0), 0)
        val idx1 = min(max(vi1, lpi1), children.size)
        lpi0 = vi0
        lpi1 = vi1

        for (i in idx0 until idx1) {
            val child = children[i]
            if (child.isVisible) {
                val ix = i % columns
                val iy = i / columns
                val cx = x + when (child.alignmentX) {
                    AxisAlignment.MIN, AxisAlignment.FILL -> ix * (calcChildWidth + spacing) + spacing
                    AxisAlignment.CENTER -> ix * calcChildWidth + max(0, w - contentW) * (ix + 1) / (columns + 1)
                    AxisAlignment.MAX -> w - (columns - ix) * (calcChildWidth + spacing)
                }
                val cy = y + iy * (calcChildHeight + spacing) + spacing - scroll
                child.setPosSize(cx, cy, calcChildWidth, calcChildHeight)
            }
        }

    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        clampScrollPosition()
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
            scrollY(delta.toDouble())
            // we consumed dy
            if (dx != 0f) {
                super.onMouseWheel(x, y, dx, 0f, byMouse)
            }
        }
    }

    private fun clampScrollPosition() {
        val limit = maxScrollPositionY.toDouble()
        scrollPositionY = clamp(scrollPositionY, 0.0, limit)
        targetScrollPositionY = clamp(targetScrollPositionY, 0.0, limit)
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = scrollbar.contains(x, y, scrollbarPadding * 2)
        if (!isDownOnScrollbar) super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownOnScrollbar = false
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isDownOnScrollbar) {
            scrollbar.onMouseMoved(x, y, dx, dy)
            clampScrollPosition()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun clone() = PanelList2D(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PanelList2D
        clone.childWidth = childWidth
        clone.childHeight = childHeight
        clone.scaleChildren = scaleChildren
        clone.rows = rows
        clone.columns = columns
        clone.spacing = spacing
        clone.maxColumns = maxColumns
        clone.scrollPositionY = scrollPositionY
        clone.isDownOnScrollbar = isDownOnScrollbar
        clone.scrollbarWidth = scrollbarWidth
        clone.scrollbarPadding = scrollbarPadding
        clone.autoScrollEndTime = autoScrollEndTime
        clone.autoScrollPerNano = autoScrollPerNano
        clone.autoScrollLastUpdate = autoScrollLastUpdate
        clone.autoScrollTargetPosition = autoScrollTargetPosition
    }

}