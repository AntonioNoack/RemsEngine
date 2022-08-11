package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import kotlin.math.max

/**
 * nine tile panel, which solves all constraints, and aligns the items appropriately
 * when there are multiple panels per tile, they will get stacked
 * */
open class NineTilePanel(style: Style) : PanelGroup(style) {

    constructor(base: NineTilePanel) : this(base.style) {
        base.copy(this)
    }

    override val children = ArrayList<Panel>()

    override fun remove(child: Panel) {
        children.remove(child)
    }

    fun add(panel: Panel) {
        children.add(panel)
        panel.uiParent?.remove(panel)
        panel.parent = this
    }

    fun add(index: Int, panel: Panel) {
        children.add(index, panel)
        panel.uiParent?.remove(panel)
        panel.parent = this
    }

    override fun drawsOverlayOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int) = true

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        var minW = 0
        var minH = 0

        // todo calculate how much space is available for the children
        for (child in children) {
            var aw = w / 3
            var ah = h / 3
            if (child.alignmentX == AxisAlignment.FILL) aw = w
            if (child.alignmentY == AxisAlignment.FILL) ah = h
            child.calculateSize(aw, ah)
            if (child.alignmentX != AxisAlignment.FILL) child.w = child.minW
            if (child.alignmentY != AxisAlignment.FILL) child.h = child.minH
            minW = max(minW, if (child.alignmentX == AxisAlignment.FILL) w else child.w)
            minH = max(minH, if (child.alignmentY == AxisAlignment.FILL) h else child.h)
        }

        this.minW = minW
        this.minH = minH

    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val children = children
        for (index in children.indices) {
            val child = children[index]
            val cx = x + child.alignmentX.getOffset(w, child.w)
            val cy = y + child.alignmentY.getOffset(h, child.h)
            child.setPosition(cx, cy)
        }
    }

    override fun clone() = NineTilePanel(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as NineTilePanel
        clone.children.clear()
        clone.children.addAll(children.map { it.clone() })
    }

    override val className: String = "NineTilePanel"

}