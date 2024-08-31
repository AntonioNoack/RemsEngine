package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding

abstract class PanelList(val sorter: Comparator<Panel>?, style: Style) : PanelGroup(style) {

    constructor(style: Style) : this(null, style)

    override val children = ArrayList<Panel>()
    var spacing = style.getSize("spacer.width", 0)
    var disableConstantSpaceForWeightedChildren = false

    val padding = Padding(0)
    var lastPosTime = 0L
    var allChildrenHaveSameSize = false

    fun needsPosUpdate(x: Int, y: Int): Boolean {
        return this.x != x || this.y != y || lastPosTime != Time.frameTimeNanos
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

    fun selectNext(di: Int = 1): Boolean {
        if (!isAnyChildInFocus) return false
        val children = children.filter { it.isVisible }
        val childIndex = children.indexOfFirst { it.isAnyChildInFocus }
        val newChild = when {
            childIndex >= 0 -> children.getOrNull(childIndex + di)
            di < 0 -> children.firstOrNull()
            else -> children.lastOrNull()
        }
        if (newChild != null) {
            selectNext(newChild)
        }
        return true
    }

    private fun selectNext(newChild: Panel) {
        newChild.requestFocus()
        newChild.scrollTo()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PanelList) return
        dst.padding.set(padding)
        dst.spacing = spacing
        dst.allChildrenHaveSameSize = allChildrenHaveSameSize
        dst.disableConstantSpaceForWeightedChildren = disableConstantSpaceForWeightedChildren
        dst.children.clear()
        dst.children.addAll(children.map { it.clone() })
    }
}