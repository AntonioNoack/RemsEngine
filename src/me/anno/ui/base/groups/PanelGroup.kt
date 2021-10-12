package me.anno.ui.base.groups

import me.anno.gpu.GFX
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import kotlin.math.max
import kotlin.math.min

abstract class PanelGroup(style: Style) : Panel(style) {

    abstract val children: List<Panel>
    abstract fun remove(child: Panel)

    // override fun getLayoutState(): Any? = children.count { it.visibility == Visibility.VISIBLE }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val children = children
        children@ for (index in children.indices) {
            val child = children[index]
            if (child.visibility == Visibility.VISIBLE) {
                drawChild(child, x0, y0, x1, y1)
            }
        }
    }

    fun drawChild(child: Panel, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val x02 = max(child.x, x0)
        val y02 = max(child.y, y0)
        val x12 = min(child.x + child.w, x1)
        val y12 = min(child.y + child.h, y1)
        return if (x12 > x02 && y12 > y02) {
            GFX.clip2(x02, y02, x12, y12) {
                child.draw(x02, y02, x12, y12)
            }
            true
        } else {
            child.lx0 = x02
            child.ly0 = y02
            child.lx1 = x12
            child.ly1 = y12
            false
        }

    }

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        if (visibility == Visibility.VISIBLE) {
            for (child in children) {
                child.printLayout(tabDepth + 1)
            }
        } else println("${Tabs.spaces((tabDepth + 1) * 2)}...")
    }

    override fun onSelectAll(x: Float, y: Float) {
        if (children.any { it.getMultiSelectablePanel() == it }) {
            // select all panels, which are multi-selectable
            GFX.inFocus.clear()
            GFX.inFocus.addAll(children.filter { it.getMultiSelectablePanel() == it })
        } else super.onSelectAll(x, y)
    }

}