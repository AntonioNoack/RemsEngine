package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Tabs
import kotlin.math.max
import kotlin.math.min

abstract class PanelGroup(style: Style) : Panel(style) {

    abstract override val children: List<Panel>
    abstract fun remove(child: Panel)

    override fun listChildTypes(): String = "p"
    override fun getChildListByType(type: Char) = children
    override fun getOptionsByType(type: Char) = getPanelOptions(this)
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        val children = children
        if (child is Panel && children is MutableList) children.add(index, child)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
    }

    open fun drawChildren(x0: Int, y0: Int, x1: Int, y1: Int) {
        val children = children
        for (index in children.indices) {
            val child = children[index]
            if (child.isVisible) {
                drawChild(child, x0, y0, x1, y1)
            }
        }
    }

    open fun drawChild(child: Panel, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val x02 = max(child.x, x0)
        val y02 = max(child.y, y0)
        val x12 = min(child.x + child.width, x1)
        val y12 = min(child.y + child.height, y1)
        return if (x12 > x02 && y12 > y02) {
            if (child.canDrawOverBorders) {
                GFX.clip2(x02, y02, x12, y12) {
                    child.draw(x02, y02, x12, y12)
                }
            } else {
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

    override fun updateVisibility(mx: Int, my: Int, canBeHovered: Boolean) {
        super.updateVisibility(mx, my, canBeHovered)
        updateChildrenVisibility(mx, my, canBeHovered)
    }

    open fun updateChildrenVisibility(mx: Int, my: Int, canBeHovered: Boolean) {
        val children = children
        for (i in children.indices) {
            children[i].updateVisibility(mx, my, canBeHovered)
        }
    }

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        if (isVisible) {
            for (child in children) {
                child.printLayout(tabDepth + 1)
            }
        } else {
            val cs = children.size
            print(Tabs.spaces((tabDepth + 1) * 2))
            if (cs == 1) println("... (1 child)")
            else println("... (${children.size} children)")
        }
    }

    override fun onSelectAll(x: Float, y: Float) {
        if (children.any { it.isVisible && it.getMultiSelectablePanel() == it }) {
            // select all panels, which are multi-selectable
            val children = children
            var first = true
            for (index in children.indices) {
                val panel = children[index]
                if (panel.getMultiSelectablePanel() == panel) {
                    panel.requestFocus(first)
                    first = false
                }
            }
        } else super.onSelectAll(x, y)
    }

    override fun destroy() {
        super.destroy()
        for (child in children) {
            child.destroy()
        }
    }

    override fun getPanelAt(x: Int, y: Int): Panel? {
        return if (canBeSeen && contains(x, y)) {
            if (!capturesChildEvents(x, y)) {
                val panelAt = getChildPanelAt(x, y)
                if (panelAt != null) return panelAt
            }
            this
        } else null
    }

    open fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        for (i in children.size - 1 downTo 0) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null) return panelAt
        }
        return null
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(null, "children", children)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "children" -> {
                val children = children
                if (children is MutableList) {
                    children.clear()
                    children.addAll(values.filterIsInstance<Panel>())
                }
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override fun forAllVisiblePanels(callback: (Panel) -> Unit) {
        if (canBeSeen) {
            callback(this)
            val children = children
            for (i in children.indices) {
                val child = children[i]
                child.parent = this
                child.forAllVisiblePanels(callback)
            }
        }
    }

    companion object {
        fun getPanelOptions(self: Panel?) = getOptionsByClass(self, Panel::class)
    }
}