package me.anno.ui.base.groups

import me.anno.engine.serialization.NotSerializedProperty
import me.anno.ui.Panel
import me.anno.ui.Style

abstract class PanelList2(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    open val visibleIndex0: Int get() = 0
    open val visibleIndex1: Int get() = children.size

    override fun drawChildren(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in visibleIndex0 until visibleIndex1) {
            val child = children[index]
            if (child.canBeSeen) {
                drawChild(child, x0, y0, x1, y1)
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

    @NotSerializedProperty
    private var lpx = 0

    @NotSerializedProperty
    private var lpy = Int.MAX_VALUE
}