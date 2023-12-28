package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.sq
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.structures.lists.Lists.count2
import kotlin.math.max
import kotlin.math.min

/**
 * Related Classes:
 *  - Android: GridLayout
 * */
open class PanelList2D(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {

    constructor(style: Style) : this(null, style)

    constructor(base: PanelList2D) : this(base.sorter, base.style) {
        base.copyInto(this)
    }

    override val children = ArrayList<Panel>(256)

    val defaultSize = 100
    var scaleChildren = false
    var scaleSpaces = false

    override val canDrawOverBorders: Boolean get() = true

    var childWidth: Int = style.getSize("childWidth", defaultSize)
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var childHeight: Int = style.getSize("childHeight", defaultSize)
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    override fun invalidateLayout() {
        window?.addNeedsLayout(this)
    }

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0

    var maxColumns = Int.MAX_VALUE

    override fun calculateSize(w: Int, h: Int) {

        val children = children
        if (sorter != null) {
            children.sortWith(sorter)
        }

        val numChildren = children.count2 { it.isVisible }
        val w2 = min(w, childWidth * numChildren)
        columns = min(max(1, (w2 + spacing) / (childWidth + spacing)), maxColumns)
        rows = max(1, (numChildren + columns - 1) / columns)
        val childScale = if (scaleChildren) max(1f, ((w2 + spacing) / columns - spacing) * 1f / childWidth) else 1f
        calcChildWidth = if (scaleChildren) (childWidth * childScale).toInt() else childWidth
        calcChildHeight = if (scaleChildren) (childHeight * childScale).toInt() else childHeight
        minW = min(w2, numChildren * (calcChildWidth + spacing) + spacing)
        minH = (calcChildHeight + spacing) * rows - spacing

        // only execute for visible children
        for (i in visibleIndex0 until visibleIndex1) {
            val child = children[i]
            if (child.isVisible) {
                child.calculateSize(calcChildWidth, calcChildHeight)
            }
        }
    }

    override val visibleIndex0
        get() = 0 // max((ly0 - (y + spacing - scrollPositionY.toInt())) / childHeight, 0) * columns
    override val visibleIndex1
        get() = children.size /*min(
            (ly1 - (y + spacing - scrollPositionY.toInt()) + childHeight - 1) / childHeight * columns,
            children.size
        )*/

    /*override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        if (children.isEmpty()) return null
        val i = getItemIndexAt(x, y)
        val panelAt = children[i].getPanelAt(x, y)
        return if (panelAt != null && panelAt.isOpaqueAt(x, y)) panelAt else null
    }*/

    fun getItemIndexAt(cx: Int, cy: Int): Int {
        val lw = (calcChildWidth + spacing)
        val lh = (calcChildHeight + spacing)
        // todo skip invisible children less costly
        val children = children.filter { it.isVisible }
        if (lw < 1 || lh < 1 || children.size < 2) return children.lastIndex
        // cx = x + ix * (calcChildWidth + spacing) + spacing
        val itemX = (cx - x - spacing) / lw
        val itemY = (cy - y - spacing) / lh
        val ci = min(max(itemX + itemY * columns, 0), children.lastIndex)
        return if (ci > 0 && itemX > 0) {
            val p0 = children[ci]
            val p1 = children[ci - 1]
            val d1 = sq(cx - (p1.x + p1.width / 2f), cy - (p1.y + p1.height / 2f))
            val d0 = sq(cx - (p0.x + p0.width / 2f), cy - (p0.y + p0.height / 2f))
            if (d1 < d0) ci - 1 else ci
        } else ci
    }

    fun getItemFractionY(y: Float): Float {
        val ly = y - this.y - spacing
        val itemY = ly * rows / height
        return fract(itemY)
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val sch = scaleChildren
        val ssp = scaleSpaces && !sch
        var iy = 0
        var i = 0
        children@ while (true) {
            var x0 = x + spacing
            val cy = y + iy * (calcChildHeight + spacing) + spacing
            for (ix in 0 until columns) {
                var child: Panel
                do {
                    child = children.getOrNull(i++) ?: break@children
                } while (!child.isVisible)
                val x1 = if (ssp) x + width * (ix + 1) / columns else x0 + calcChildWidth + spacing
                val aw = x1 - x0
                val cw = if (sch) aw else min(max(childWidth, child.minW), aw)
                val cx = x0 + child.alignmentX.getOffset(aw, cw)
                val cw1 = child.alignmentX.getWidth(aw, cw)
                child.setPosSize(cx, cy, cw1, calcChildHeight)
                x0 = x1
            }
            iy++
        }
    }

    override fun clone() = PanelList2D(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PanelList2D
        dst.childWidth = childWidth
        dst.childHeight = childHeight
        dst.scaleChildren = scaleChildren
        dst.rows = rows
        dst.columns = columns
        dst.spacing = spacing
        dst.maxColumns = maxColumns
    }

    override val className: String
        get() = "PanelList2D"
}