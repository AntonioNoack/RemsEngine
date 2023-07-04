package me.anno.ui.base.groups

import me.anno.Engine
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.scrolling.ScrollableX
import me.anno.ui.base.scrolling.ScrollableY
import me.anno.ui.style.Style

abstract class PanelList(val sorter: Comparator<Panel>?, style: Style) : PanelGroup(style) {

    constructor(style: Style) : this(null, style)

    override val children = ArrayList<Panel>()
    var spacing = style.getSize("spacer.width", 0)
    var disableConstantSpaceForWeightedChildren = false

    val padding = Padding(0)
    var lastPosTime = 0L
    var allChildrenHaveSameSize = false

    fun needsPosUpdate(x: Int, y: Int): Boolean {
        return this.x != x || this.y != y || lastPosTime != Engine.gameTime
    }

    open fun clear() {
        children.clear()
        invalidateLayout()
    }

    fun invalidateSorting() {
        invalidateLayout() // causes calculateSize() to be called
    }

    fun isEmpty() = children.isEmpty()

    fun add(index: Int, child: Panel) {
        if (children.size < index || children.getOrNull(index) != child)
            invalidateLayout()
        children.add(index, child)
        child.parent = this
    }

    fun addAll(children: Collection<Panel>) {
        val oldSize = this.children.size
        this.children.addAll(children)
        for (index in oldSize until this.children.size) {
            this.children[index].parent = this
        }
    }

    override fun addChild(child: PrefabSaveable) {
        if (child is Panel) add(child)
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
        if (sorter != null) children.sortWith(sorter)
        super.calculateSize(w, h)
    }

    fun selectPrevious(): Boolean {
        if (!isAnyChildInFocus) return false
        val childIndex = children.indexOfFirst { it.isVisible && it.isInFocus }
        val newChild = if (childIndex >= 0) {
            children.subList(0, childIndex)
                .lastOrNull { it.isVisible }
        } else null
        if (newChild != null) {
            // todo call click event?
            children[childIndex].invalidateDrawing()
            newChild.requestFocus()
            newChild.invalidateDrawing()
            onSelectNext(children[childIndex], newChild)
        }
        return newChild != null
    }

    fun selectNext(): Boolean {
        if (!isAnyChildInFocus) return false
        val childIndex = children.indexOfFirst { it.isVisible && it.isAnyChildInFocus }
        val newChild = if (childIndex >= 0) {
            children.subList(childIndex + 1, children.size).firstOrNull { it.isVisible }
        } else null
        if (newChild != null) {
            // todo call click event?
            children[childIndex].invalidateDrawing()
            newChild.requestFocus()
            newChild.invalidateDrawing()
            onSelectNext(children[childIndex], newChild)
        }
        return newChild != null
    }

    fun onSelectNext(prev: Panel, next: Panel) {
        // todo test this, correct sign?
        (firstInHierarchy { it is ScrollableX } as? ScrollableX)?.scrollX((next.x + next.width / 2) - (prev.x + prev.width / 2))
        (firstInHierarchy { it is ScrollableY } as? ScrollableY)?.scrollY((next.y + next.height / 2) - (prev.y + prev.height / 2))
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PanelList
        dst.spacing = spacing
        dst.disableConstantSpaceForWeightedChildren = disableConstantSpaceForWeightedChildren
        dst.children.clear()
        dst.children.addAll(children.map { it.clone() })
    }

}