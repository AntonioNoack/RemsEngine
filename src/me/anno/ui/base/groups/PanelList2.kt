package me.anno.ui.base.groups

import me.anno.io.serialization.NotSerializedProperty
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min

abstract class PanelList2(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {

    abstract val visibleIndex0: Int
    abstract val visibleIndex1: Int

    override fun drawChildren(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in visibleIndex0 until visibleIndex1) {
            val child = children[index]
            if (child.visibility == Visibility.VISIBLE) {
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
        val idx0 = max(min(vi0, lpi2), 0)
        val idx1 = min(max(vi1, lpi3), children.size)
        lpi2 = vi0
        lpi3 = vi1

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
    var lpi0 = 0

    @NotSerializedProperty
    var lpi1 = Int.MAX_VALUE

    @NotSerializedProperty
    var lpi2 = 0

    @NotSerializedProperty
    var lpi3 = Int.MAX_VALUE

}