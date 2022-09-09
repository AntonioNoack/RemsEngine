package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.Panel
import me.anno.ui.style.Style
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

    fun drawChild(child: Panel, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val x02 = max(child.x, x0)
        val y02 = max(child.y, y0)
        val x12 = min(child.x + child.w, x1)
        val y12 = min(child.y + child.h, y1)
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

    override fun updateVisibility(mx: Int, my: Int) {
        super.updateVisibility(mx, my)
        updateChildrenVisibility(mx, my)
    }

    open fun updateChildrenVisibility(mx: Int, my: Int) {
        val children = children
        for (i in children.indices) {
            children[i].updateVisibility(mx, my)
        }
    }

    override fun printLayout(tabDepth: Int) {
        super.printLayout(tabDepth)
        if (isVisible) {
            for (child in children) {
                child.printLayout(tabDepth + 1)
            }
        } else println("${Tabs.spaces((tabDepth + 1) * 2)}...")
    }

    override fun onSelectAll(x: Float, y: Float) {
        if (children.any { it.isVisible && it.getMultiSelectablePanel() == it }) {
            // select all panels, which are multi-selectable
            children
                .filter { it.getMultiSelectablePanel() == it }
                .forEachIndexed { index, panel ->
                    panel.requestFocus(index == 0)
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
                val child = getChildPanelAt(x, y)
                if (child != null) return child
            }
            this
        } else null
    }

    open fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        for (i in children.size - 1 downTo 0) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null && panelAt.isOpaqueAt(x, y)) {
                return panelAt
            }
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