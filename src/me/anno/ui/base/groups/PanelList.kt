package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.style.Style

abstract class PanelList(val sorter: Comparator<Panel>?, style: Style) : PanelGroup(style) {

    override val children = ArrayList<Panel>()
    var spacing = style.getSize("spacer.width", 0)
    var disableConstantSpaceForWeightedChildren = false

    fun clear() = children.clear()

    fun isEmpty() = children.isEmpty()

    fun add(index: Int, child: Panel) {
        if (children.size < index || children.getOrNull(index) != child)
            invalidateLayout()
        children.add(index, child)
        child.parent = this
    }

    override fun remove(child: Panel) {
        children.remove(child)
        child.parent = null
        invalidateLayout()
    }

    open operator fun plusAssign(child: Panel) {
        add(child)
    }

    open fun add(child: Panel): PanelList {
        children += child
        child.parent = this
        invalidateLayout()
        return this
    }

    override fun calculateSize(w: Int, h: Int) {
        if (sorter != null) {
            children.sortWith(sorter)
        }
        super.calculateSize(w, h)
    }

}