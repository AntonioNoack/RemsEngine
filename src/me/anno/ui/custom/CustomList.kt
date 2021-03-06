package me.anno.ui.custom

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.Scrollbar
import me.anno.ui.style.Style
import me.anno.utils.bugs.SumOf
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class CustomList(val isY: Boolean, style: Style) : PanelList(style) {

    init {
        spacing = style.getSize("customList.spacing", 4)
        setWeight(1f)
    }

    override fun invalidateLayout() {
        window?.needsLayout?.add(this)
    }

    val minSize get() = if (isY) 10f / h else 10f / w

    fun change(p: Panel, delta: Float) {
        p.weight = clamp(p.weight + delta, minSize, 1f)
    }

    fun remove(index: Int) {
        if (children.size > 1) {
            children.removeAt(index)
        } else {
            val parent = parent
            if (parent is CustomList) {
                parent.remove(indexInParent)
            } else {
                LOGGER.warn("Cannot remove root of custom UI hierarchy")
            }
        }
        invalidateLayout()
    }

    fun move(index: Int, delta: Float) {
        if (index in 1 until children.size - 1) {
            val c1 = children[index - 1]
            val c2 = children[index + 1]
            val deltaS = delta / (if (isY) h else w)
            change(c1, +deltaS)
            change(c2, -deltaS)
        }
    }

    fun move2(index: Int, delta: Float) {
        val c1 = children[index]
        val c2 = children[index + 1]
        val deltaS = delta / (if (isY) h else w)
        change(c1, +deltaS)
        change(c2, -deltaS)
        invalidateLayout()
    }

    override fun add(child: Panel): CustomList {
        if (child.weight <= 0f) child.weight = 1f
        super.add(child)
        ensureScrollbars()
        return this
    }

    override fun addChild(index: Int, child: PrefabSaveable) {
        if (child is Panel) {
            if (child.weight <= 0f) child.weight = 1f
            super.add(child)
            ensureScrollbars()
        } else super.addChild(index, child)
    }

    override operator fun plusAssign(child: Panel) {
        add(child)
    }

    fun add(child: Panel, weight: Float): CustomList {
        add(child)
        child.weight = weight
        return this
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = 10
        minW = 10
    }

    override fun setPosition(x: Int, y: Int) {

        this.x = x
        this.y = y

        if (children.isEmpty()) return
        if (children.size == 1) {

            val child = children.first()
            child.setPosition(x, y)
            child.setSize(w, h)

        } else {

            val minWeight = 0.0001f
            val available = (if (isY) h else w) - (children.size - 1) * spacing
            val sumWeight = SumOf.sumOf(children) { max(minWeight, it.weight) }
            val weightScale = 1f / sumWeight

            var childPos = if (isY) y else x
            val children = children
            for (index in children.indices) {
                val child = children[index]
                val weight = max(minWeight, child.weight)
                val betterWeight = max(weight * weightScale, minSize)
                if (betterWeight != weight) child.weight = betterWeight
                val childSize = (betterWeight * weightScale * available).roundToInt()
                childPos += min(
                    childSize, if (isY) {
                        child.calculateSize(w, childSize)
                        child.setPosSize(x, childPos, w, childSize)
                        child.h
                    } else {
                        child.calculateSize(childSize, h)
                        child.setPosSize(childPos, y, childSize, h)
                        child.w
                    }
                )
                childPos += spacing
            }
        }
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        for (i in 1 until children.size) {
            if (touchesBar(i, lx0, ly0, lx1, ly1)) {
                return true
            }
        }
        return super.capturesChildEvents(lx0, ly0, lx1, ly1)
    }

    var isDownIndex = -1

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        // find where the mouse went down
        for (index in scrollbars.indices) {
            val scrollbar = scrollbars[index]
            if (scrollbar.isBeingHovered) {
                isDownIndex = index
                return
            }
        }
        super.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        isDownIndex = -1
        super.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (isDownIndex >= 0) {
            if (isY) {
                move2(isDownIndex, dy)
                ry = 0f
            } else {
                move2(isDownIndex, dx)
                rx = 0f
            }
        }
        super.onMouseMoved(x, y, rx, ry)
    }

    override fun onUpdate() {
        super.onUpdate()
        ensureScrollbars()
        for (index in scrollbars.indices) {
            val scrollbar = scrollbars[index]
            updateScrollbar(scrollbar, index)
        }
    }

    fun updateScrollbar(scrollbar: Scrollbar, index: Int) {
        val window = window!!
        val mx = window.mouseXi
        val my = window.mouseYi
        scrollbar.isBeingHovered = touchesBar(index + 1, mx, my)
        if (scrollbar.updateAlpha()) {
            invalidateDrawing()
        }
    }

    private val hoverColor = style.getColor("customList.color", Maths.mixARGB(0x77ffb783, originalBGColor, 0.8f))

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        backgroundColor = hoverColor
        drawBackground(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
        ensureScrollbars()
        for (i in 0 until min(scrollbars.size, children.size - 1)) {
            val scrollbar = scrollbars[i]
            val child = children[i + 1]
            if (isY) {
                scrollbar.x = x
                scrollbar.y = child.y - spacing
                scrollbar.w = w
                scrollbar.h = spacing
            } else {
                scrollbar.x = child.x - spacing
                scrollbar.y = y
                scrollbar.w = spacing
                scrollbar.h = h
            }
            updateScrollbar(scrollbar, i)
            drawChild(scrollbar, x0, y0, x1, y1)
        }
    }

    private val interactionHeight get() = spacing + 2 * interactionPadding
    private fun touchesBar(index: Int, lx0: Int, ly0: Int, lx1: Int = lx0 + 1, ly1: Int = ly0 + 1): Boolean {
        if (index !in children.indices) return false
        val sbSize = interactionHeight
        val child = children[index]
        val x: Int
        val y: Int
        val w: Int
        val h: Int
        val sbWidth: Int
        val sbHeight: Int
        if (isY) {
            x = this.x
            y = child.y - spacing
            w = this.w
            h = spacing
            sbWidth = w
            sbHeight = sbSize
        } else {
            x = child.x - spacing
            y = this.y
            w = spacing
            h = this.h
            sbWidth = sbSize
            sbHeight = h
        }
        val centerX = x * 2 + w
        val centerY = y * 2 + h
        return abs((lx0 + lx1) - centerX) < sbWidth && abs((ly0 + ly1) - centerY) < sbHeight
    }

    private fun ensureScrollbars() {
        val size = children.size - 1
        while (scrollbars.size < size) {
            scrollbars.add(Scrollbar(style))
        }
        while (size > scrollbars.size) {
            scrollbars.removeAt(scrollbars.lastIndex)
        }
    }

    override fun getCursor(): Long? {
        val scrollbars = scrollbars
        for (index in scrollbars.indices) {
            val scrollbar = scrollbars[index]
            if (scrollbar.isBeingHovered) {
                return if (isY) Cursor.vResize else Cursor.hResize
            }
        }
        return super.getCursor()
    }

    private val scrollbars = ArrayList<Scrollbar>()

    override val className: String = "CustomList"

    companion object {
        private val LOGGER = LogManager.getLogger(CustomList::class)
    }

}