package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import kotlin.math.max
import kotlin.math.min

open class PanelStack(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        var minW = 0
        var minH = 0
        val children = children
        for (index in children.indices) {
            val child = children[index]
            child.calculateSize(w, h)
            minW = max(minW, child.minW)
            minH = max(minH, child.minH)
        }
        this.minW = minW
        this.minH = minH
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val w = width
        val h = height
        val children = children
        for (index in children.indices) {
            val child = children[index]
            val ax = child.alignmentX
            val ay = child.alignmentY
            val minW = min(w, child.minW)
            val minH = min(h, child.minH)
            val dx = ax.getOffset(w, minW)
            val cw = ax.getSize(w, minW)
            val dy = ay.getOffset(h, minH)
            val ch = ay.getSize(h, minH)
            child.setPosSize(x + dx, y + dy, cw, ch)
        }
    }

    // if they are overlapping, we need to redraw the others as well
    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return false
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return children.size > 1
    }

    override fun clone(): PanelStack {
        val clone = PanelStack(sorter, style)
        copyInto(clone)
        return clone
    }
}