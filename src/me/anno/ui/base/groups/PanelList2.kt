package me.anno.ui.base.groups

import me.anno.ui.canvas.Canvas
import me.anno.ui.Panel
import me.anno.ui.Style

abstract class PanelList2(style: Style) : PanelList(style) {

    open val visibleIndex0: Int get() = 0
    open val visibleIndex1: Int get() = children.size

    override fun drawChildren(canvas: Canvas) {
        val children = children
        for (index in visibleIndex0 until visibleIndex1) {
            val child = children[index]
            if (child.canBeSeen) {
                drawChild(child, canvas)
            }
        }
    }

    override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        for (i in visibleIndex1 - 1 downTo visibleIndex0) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null && panelAt.isVisible) return panelAt
        }
        return null
    }
}