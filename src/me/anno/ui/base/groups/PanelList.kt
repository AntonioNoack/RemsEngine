package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.style.Style

abstract class PanelList(val sorter: Comparator<Panel>?, style: Style) : PanelGroup(style) {

    override val children = ArrayList<Panel>()
    var spacing = style.getSize("spacer.width", 0)
    var disableConstantSpaceForWeightedChildren = false

    val padding = Padding(0)

    open fun clear() {
        children.clear()
        invalidateLayout()
    }

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

    fun selectPrevious(): Boolean {
        if (!isAnyChildInFocus) return false
        val childIndex = children.indexOfFirst { it.visibility == Visibility.VISIBLE && it.isInFocus }
        val newChild = if (childIndex >= 0) {
            children.subList(0, childIndex)
                .lastOrNull { it.visibility == Visibility.VISIBLE }
        } else null
        if (newChild != null) {
            // todo call click event?
            children[childIndex].invalidateDrawing()
            newChild.requestFocus()
            newChild.invalidateDrawing()
        }
        return newChild != null
    }

    fun selectNext(): Boolean {
        if (!isAnyChildInFocus) return false
        val childIndex = children.indexOfFirst { it.visibility == Visibility.VISIBLE && it.isAnyChildInFocus }
        val newChild = if (childIndex >= 0) {
            children.subList(childIndex + 1, children.size)
                .firstOrNull { it.visibility == Visibility.VISIBLE }
        } else null
        if (newChild != null) {
            // todo call click event?
            children[childIndex].invalidateDrawing()
            newChild.requestFocus()
            newChild.invalidateDrawing()
        }
        return newChild != null
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PanelList
        clone.spacing = spacing
        clone.disableConstantSpaceForWeightedChildren = disableConstantSpaceForWeightedChildren
        clone.children.clear()
        clone.children.addAll(children.map { it.clone() })
    }

}