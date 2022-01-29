package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.editor.stacked.Option
import me.anno.ui.style.Style
import me.anno.utils.Tabs
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import kotlin.math.max
import kotlin.math.min

abstract class PanelGroup(style: Style) : Panel(style) {

    abstract override val children: List<Panel>
    abstract fun remove(child: Panel)

    override fun listChildTypes(): String = "p"
    override fun getChildListByType(type: Char) = children
    override fun getOptionsByType(type: Char) = getPanelOptions()
    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        val children = children
        if (child is Panel && children is MutableList) children.add(index, child)
    }

    // override fun getLayoutState(): Any? = children.count { it.visibility == Visibility.VISIBLE }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        drawChildren(x0, y0, x1, y1)
    }

    fun drawChildren(x0: Int, y0: Int, x1: Int, y1: Int) {
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
        if (children.any { it.visibility == Visibility.VISIBLE && it.getMultiSelectablePanel() == it }) {
            // select all panels, which are multi-selectable
            GFX.inFocus.clear()
            GFX.inFocus.addAll(children.filter { it.getMultiSelectablePanel() == it })
        } else super.onSelectAll(x, y)
    }

    override fun destroy() {
        super.destroy()
        for (child in children) {
            child.destroy()
        }
    }

    companion object {
        fun getPanelOptions() = ISaveable.objectTypeRegistry
            .filterValues { it.sampleInstance is Panel }
            .map { (key, value) ->
                Option(key.camelCaseToTitle(), "") { value.generator() as Panel }
            }
    }

}