package me.anno.ui.base.groups

import me.anno.ui.Style
import me.anno.ui.base.groups.PanelContainer.Companion.setPosSizeWithPadding
import kotlin.math.max

open class PanelStack( style: Style) : PanelList(style) {

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
    override fun capturesChildEvents(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return false
    }

    override fun drawsOverlayOverChildren(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return children.size > 1
    }

    override fun clone(): PanelStack {
        val clone = PanelStack(style)
        copyInto(clone)
        return clone
    }
}