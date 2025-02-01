package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelContainer.Companion.setPosSizeWithPadding
import kotlin.math.max

open class PanelStack(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    override fun calculateSize(w: Int, h: Int) {
        var neededW = 0
        var neededH = 0
        val wi = w - padding.width
        val hi = h - padding.height
        val children = children
        for (index in children.indices) {
            val child = children[index]
            child.calculateSize(wi, hi)
            neededW = max(neededW, child.minW)
            neededH = max(neededH, child.minH)
        }
        minW = neededW + padding.width
        minH = neededH + padding.height
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        val children = children
        for (index in children.indices) {
            setPosSizeWithPadding(children[index], x, y, width, height, padding)
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