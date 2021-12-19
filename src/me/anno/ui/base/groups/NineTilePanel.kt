package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.style.Style

// nine tile panel, which solves all constraints, and aligns the items appropriately
// when there are multiple panels per tile, they will get stacked
open class NineTilePanel(style: Style) : PanelGroup(style) {

    override val children = ArrayList<Panel>()

    override fun remove(child: Panel) {
        children.remove(child)
    }

    fun add(panel: Panel) {
        children.add(panel)
        panel.uiParent?.remove(panel)
        panel.parent = this
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        // todo calculate how much space is available for the children
        for (child in children) {
            child.calculateSize(w / 3, h / 3)
            child.w = child.minW
            child.h = child.minH
        }

        minW = w
        minH = h
        this.w = minW
        this.h = minH

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        for (child in children) {
            val cx = x + child.alignmentX.getOffset(w, child.w)
            val cy = y + child.alignmentY.getOffset(h, child.h)
            child.placeInParent(cx, cy)
        }

    }

}