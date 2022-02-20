package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.style.Style
import kotlin.math.max

open class PanelStack(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        var minW = 0
        var minH = 0
        for (child in children) {
            child.calculateSize(w, h)
            // child.applyPlacement(w, h)
            // child.applyConstraints()
            minW = max(minW, child.minW)
            minH = max(minH, child.minH)
        }
        this.minW = minW
        this.minH = minH
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        for (child in children) {
            child.setPosSize(x, y, w, h)
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
        copy(clone)
        return clone
    }

    // todo if they are overlapping, and the lower one can be placed inside the upper one,
    // todo draw only the necessary parts for it

    override val className: String = "PanelStack"

}