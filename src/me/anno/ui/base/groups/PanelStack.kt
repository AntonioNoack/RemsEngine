package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.max

open class PanelStack(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    constructor(style: Style) : this(null, style)

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW = 0
        minH = 0
        children.forEach { child ->
            child.calculateSize(w, h)
            // child.applyPlacement(w, h)
            // child.applyConstraints()
            minW = max(minW, child.minW)
            minH = max(minH, child.minH)
        }
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        children.forEach { child ->
            child.place(x, y, w, h)
        }
    }

    // if they are overlapping, we need to redraw the others as well
    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return children.count { it.visibility == Visibility.VISIBLE } > 1
    }

    override fun clone(): PanelStack {
        val clone = PanelStack(sorter, style)
        copy(clone)
        return clone
    }

    // todo if they are overlapping, and the lower one can be placed inside the upper one,
    // todo draw only the necessary parts for it

}