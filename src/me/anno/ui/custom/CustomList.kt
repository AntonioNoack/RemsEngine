package me.anno.ui.custom

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.Scrollbar
import me.anno.utils.Color.mixARGB
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.sumOfFloat
import me.anno.utils.types.Floats.roundToIntOr
import org.apache.logging.log4j.LogManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Horizontal/Vertical (isY) list, where the user can decide the width/height of each element,
 * so they can focus on what is most important to them.
 * */
open class CustomList(val isY: Boolean, style: Style) : PanelList(style) {

    init {
        spacing = style.getSize("customList.spacing", 4)
        alignmentX = AxisAlignment.FILL
        alignmentY = AxisAlignment.FILL
        weight = 1f
    }

    val minSize get() = if (isY) 10f / height else 10f / width

    fun change(p: Panel, delta: Float, minSize: Float): Float {
        val target = p.weight + delta
        p.weight = clamp(target, minSize, 1f)
        return target - p.weight // remaining value
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
    }

    fun move(index: Int, delta: Float) {
        val available = if (isY) height - padding.height else width - padding.width
        val w = delta / max(available, 1)
        var li = index
        val minSize = minSize
        var w0 = change(children[li--], +w, minSize)
        while (abs(w0) > 1e-5f && li > 0) {
            w0 = change(children[li--], w0, minSize)
        }
        var ri = index + 1
        var w1 = change(children[ri++], -w, minSize)
        while (abs(w1) > 1e-5f && ri < children.size) {
            w1 = change(children[ri++], w1, minSize)
        }
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
        val minWeight = 0.0001f
        val available0 = (if (isY) h - padding.height else w - padding.width)
        val available = available0 - (children.size - 1) * spacing
        val sumWeight = children.sumOf { max(minWeight, it.weight).toDouble() }.toFloat()
        val weightScale = 1f / sumWeight
        val children = children
        for (index in children.indices) {
            val child = children[index]
            val weight = max(minWeight, child.weight)
            val betterWeight = max(weight * weightScale, minSize)
            if (betterWeight != weight) child.weight = betterWeight
            val childSize = (betterWeight * weightScale * available).roundToIntOr()
            if (isY) child.calculateSize(w, childSize)
            else child.calculateSize(childSize, h)
        }
        minW = w
        minH = h
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChildrenWithoutPadding(
            x + padding.left,
            y + padding.top,
            width - padding.width,
            height - padding.height
        )
    }

    fun placeChildrenWithoutPadding(x: Int, y: Int, width: Int, height: Int) {
        if (children.size == 1) {
            val child = children.first()
            child.setPosSize(x, y, width, height)
            return
        }

        val minWeight = 0.0001f
        val available = (if (isY) height else width) - (children.size - 1) * spacing
        val sumWeight = children.sumOfFloat { it.weight }
        val weightScale = 1f / max(1e-38f, sumWeight)
        var childPos = if (isY) y else x
        val children = children
        for (index in children.indices) {
            val child = children[index]
            val weight = max(minWeight, child.weight)
            val betterWeight = max(weight * weightScale, minSize)
            if (betterWeight != weight) child.weight = betterWeight
            val childSize = (betterWeight * weightScale * available).roundToIntOr()
            val usedSize = if (isY) {
                child.calculateSize(width, childSize)
                child.setPosSize(x, childPos, width, childSize)
                child.height
            } else {
                child.calculateSize(childSize, height)
                child.setPosSize(childPos, y, childSize, height)
                child.width
            }
            childPos += min(childSize, usedSize) + spacing
        }
    }

    override fun capturesChildEvents(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        for (i in 1 until children.size) {
            if (touchesBar(i, x0, y0, x1, y1)) {
                return true
            }
        }
        return super.capturesChildEvents(x0, y0, x1, y1)
    }

    var isDownIndex = -1

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            isDownIndex = scrollbars.indexOfFirst { it.isHovered }
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        isDownIndex = -1
        super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        var rx = dx
        var ry = dy
        if (isDownIndex >= 0) {
            if (isY) {
                move(isDownIndex, dy)
                ry = 0f
            } else {
                move(isDownIndex, dx)
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
        scrollbar.isHovered = touchesBar(index + 1, mx, my)
        scrollbar.updateAlpha()
    }

    private val hoverColor = style.getColor("customList.hoverColor", mixARGB(0x77ffb783, background.originalColor, 0.8f))

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        background.color = hoverColor
        drawBackground(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
        ensureScrollbars()
        for (i in 0 until min(scrollbars.size, children.size - 1)) {
            val scrollbar = scrollbars[i]
            val child = children[i + 1]
            if (isY) {
                scrollbar.x = x
                scrollbar.y = child.y - spacing
                scrollbar.width = width
                scrollbar.height = spacing
            } else {
                scrollbar.x = child.x - spacing
                scrollbar.y = y
                scrollbar.width = spacing
                scrollbar.height = height
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
            w = this.width
            h = spacing
            sbWidth = w
            sbHeight = sbSize
        } else {
            x = child.x - spacing
            y = this.y
            w = spacing
            h = this.height
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
            scrollbars.add(Scrollbar(this, style))
        }
        while (scrollbars.isNotEmpty() && scrollbars.size > size) {
            scrollbars.removeAt(scrollbars.lastIndex)
        }
    }

    override fun updateChildrenVisibility(mx: Int, my: Int, canBeHovered: Boolean, x0: Int, y0: Int, x1: Int, y1: Int) {
        super.updateChildrenVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        for (i in scrollbars.indices) {
            scrollbars[i].updateVisibility(mx, my, canBeHovered, x0, y0, x1, y1)
        }
    }

    override fun getCursor(): Cursor? {
        return if (scrollbars.any2 { it.isHovered }) {
            if (isY) Cursor.vResize else Cursor.hResize
        } else super.getCursor()
    }

    private val scrollbars = ArrayList<Scrollbar>()

    companion object {
        private val LOGGER = LogManager.getLogger(CustomList::class)
    }
}