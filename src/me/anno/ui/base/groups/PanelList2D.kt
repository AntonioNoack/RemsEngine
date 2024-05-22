package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
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
open class PanelList2D(var isY: Boolean, sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {

    constructor(style: Style) : this(true, null, style)

    constructor(base: PanelList2D) : this(true, base.sorter, base.style) {
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

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        val dxi = if (isY) 1 else rows
        val dyi = if (isY) columns else 1
        return when (action) {
            "Left" -> selectNext(-dxi)
            "Right" -> selectNext(+dxi)
            "Up" -> selectNext(-dyi)
            "Down" -> selectNext(+dyi)
            "Previous" -> selectNext(-1)
            "Next" -> selectNext(+1)
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    var rows = 1
    var columns = 1
    var calcChildWidth = 0
    var calcChildHeight = 0

    var maxColumns = Int.MAX_VALUE
    var maxRows = Int.MAX_VALUE

    override fun calculateSize(w: Int, h: Int) {

        val children = children
        if (sorter != null) {
            children.sortWith(sorter)
        }

        val numChildren = children.count2 { it.isVisible }
        if (isY) {
            val w2 = min(w, childWidth * numChildren)
            columns = min(max(1, (w2 + spacing) / (childWidth + spacing)), maxColumns)
            rows = ceilDiv(numChildren, columns)
            val childScale = if (scaleChildren) max(1f, ((w2 + spacing) / columns - spacing) * 1f / childWidth) else 1f
            calcChildWidth = if (scaleChildren) (childWidth * childScale).toInt() else childWidth
            calcChildHeight = if (scaleChildren) (childHeight * childScale).toInt() else childHeight
            minW = min(w2, numChildren * (calcChildWidth + spacing) + spacing)
            minH = (calcChildHeight + spacing) * rows - spacing
        } else {
            val h2 = min(h, childHeight * numChildren)
            rows = min(max(1, (h2 + spacing) / (childHeight + spacing)), maxRows)
            columns = ceilDiv(numChildren, rows)
            val childScale = if (scaleChildren) max(1f, ((h2 + spacing) / rows - spacing) * 1f / childHeight) else 1f
            calcChildHeight = if (scaleChildren) (childHeight * childScale).toInt() else childHeight
            calcChildWidth = if (scaleChildren) (childWidth * childScale).toInt() else childWidth
            minH = min(h2, numChildren * (calcChildHeight + spacing) + spacing)
            minW = (calcChildWidth + spacing) * columns - spacing
        }

        // only execute for visible children
        for (i in visibleIndex0 until visibleIndex1) {
            val child = children[i]
            if (child.isVisible) {
                child.calculateSize(calcChildWidth, calcChildHeight)
            }
        }
    }

    fun getItemIndexAt(cx: Int, cy: Int): Int {
        val lw = (calcChildWidth + spacing)
        val lh = (calcChildHeight + spacing)
        // todo skip invisible children less costly
        val children = children.filter { it.isVisible }
        if (lw < 1 || lh < 1 || children.size < 2) return children.lastIndex
        val itemX = (cx - x - spacing) / lw
        val itemY = (cy - y - spacing) / lh
        val index = if (isY) itemX + itemY * columns else itemX * rows + itemY
        val ci = clamp(index, 0, children.lastIndex)
        return if (ci > 0 && itemX > 0) {
            val p0 = children[ci]
            val p1 = children[ci - 1]
            val d1 = sq(cx - (p1.x + p1.width / 2f), cy - (p1.y + p1.height / 2f))
            val d0 = sq(cx - (p0.x + p0.width / 2f), cy - (p0.y + p0.height / 2f))
            if (d1 < d0) ci - 1 else ci
        } else ci
    }

    fun getItemFractionY(y: Float): Float {
        if (isY) {
            val ly = y - this.y - spacing
            val itemY = ly * rows / height
            return fract(itemY)
        } else {
            val lx = y - this.x - spacing
            val itemX = lx * columns / width
            return fract(itemX)
        }
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val sch = scaleChildren
        val ssp = scaleSpaces && !sch
        if (isY) {
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
                    val cw1 = child.alignmentX.getSize(aw, cw)
                    child.setPosSize(cx, cy, cw1, calcChildHeight)
                    x0 = x1
                }
                iy++
            }
        } else {
            var ix = 0
            var i = 0
            children@ while (true) {
                var y0 = y + spacing
                val cx = x + ix * (calcChildWidth + spacing) + spacing
                for (iy in 0 until rows) {
                    var child: Panel
                    do {
                        child = children.getOrNull(i++) ?: break@children
                    } while (!child.isVisible)
                    val y1 = if (ssp) y + height * (iy + 1) / rows else y0 + calcChildHeight + spacing
                    val ah = y1 - y0
                    val ch = if (sch) ah else min(max(childHeight, child.minH), ah)
                    val cy = y0 + child.alignmentY.getOffset(ah, ch)
                    val ch1 = child.alignmentY.getSize(ah, ch)
                    child.setPosSize(cx, cy, calcChildWidth, ch1)
                    y0 = y1
                }
                ix++
            }
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
        dst.maxRows = maxRows
        dst.isY = isY
    }
}