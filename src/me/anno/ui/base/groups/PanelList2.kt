package me.anno.ui.base.groups

import me.anno.io.serialization.NotSerializedProperty
import me.anno.ui.Panel
import me.anno.ui.Style
import kotlin.math.max
import kotlin.math.min

abstract class PanelList2(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    abstract val visibleIndex0: Int
    abstract val visibleIndex1: Int

    override fun drawChildren(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in visibleIndex0 until visibleIndex1) {
            val child = children[index]
            if (child.isVisible) {
                drawChild(child, x0, y0, x1, y1)
            }
        }
    }

    override fun forAllVisiblePanels(callback: (Panel) -> Unit) {
        if (canBeSeen) {
            callback(this)
            val children = children
            for (i in visibleIndex0 until visibleIndex1) {
                val child = children[i]
                child.parent = this
                child.forAllVisiblePanels(callback)
            }
        }
    }

    override fun updateChildrenVisibility(mx: Int, my: Int) {

        val vi0 = visibleIndex0
        val vi1 = visibleIndex1
        val idx0 = max(min(vi0, lpx), 0)
        val idx1 = min(max(vi1, lpy), children.size)
        lpx = vi0
        lpy = vi1

        val children = children
        for (i in idx0 until idx1) {
            children[i].updateVisibility(mx, my)
        }
    }

    override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        for (i in visibleIndex1 - 1 downTo visibleIndex0) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null && panelAt.isOpaqueAt(x, y)) {
                return panelAt
            }
        }
        return null
    }

    @NotSerializedProperty
    private var lpx = 0

    @NotSerializedProperty
    private var lpy = Int.MAX_VALUE
}