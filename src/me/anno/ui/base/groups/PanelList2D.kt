package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.structures.lists.Lists.count2
import kotlin.math.max
import kotlin.math.min

/**
 * Related Classes:
 *  - Android: GridLayout
 * */
open class PanelList2D(var isY: Boolean, style: Style) : PanelList2(style) {

    constructor(style: Style) : this(true, style)

    constructor(base: PanelList2D) : this(true, base.style) {
        base.copyInto(this)
    }

    override val children = ArrayList<Panel>(256)

    val defaultSize = 100

    var listAlignmentX = ListAlignment.SCALE_CHILDREN
    var listAlignmentY = ListAlignment.SCALE_CHILDREN

    private fun getI0Min(childIndex: Int, childSize: Int, spacing: Int): Int {
        return childIndex * (childSize + spacing) + spacing
    }

    private fun getI1Min(childIndex: Int, childSize: Int, spacing: Int): Int {
        return getI0Min(childIndex, childSize, spacing) + childSize
    }

    private fun getI0Max(childIndex: Int, numChildren: Int, childSize: Int, totalSize: Int, spacing: Int): Int {
        return totalSize - getI1Min(numChildren - 1 - childIndex, childSize, spacing)
    }

    private fun getI0ScaleChildren(childIndex: Int, numChildren: Int, totalSize: Int, spacing: Int): Int {
        return spacing * (childIndex + 1) + WorkSplitter.partition(
            totalSize - (numChildren + 1) * spacing,
            childIndex, max(numChildren, 1)
        )
    }

    private fun getI0ScaleSpaces(childIndex: Int, numChildren: Int, childSize: Int, totalSize: Int): Int {
        return WorkSplitter.partition(
            totalSize - numChildren * childSize,
            childIndex + 1, numChildren + 1
        ) + childIndex * childSize
    }

    private fun getI0(
        childIndex: Int, numChildren: Int, childSize: Int, totalSize: Int,
        spacing: Int, scaleLogic: ListAlignment
    ): Int {
        return when (scaleLogic) {
            ListAlignment.ALIGN_MIN -> getI0Min(childIndex, childSize, spacing)
            ListAlignment.ALIGN_MAX -> getI0Max(childIndex, numChildren, childSize, totalSize, spacing)
            // average of left and right
            ListAlignment.ALIGN_CENTER -> (getI0Min(childIndex, childSize, spacing) +
                    getI0Max(childIndex, numChildren, childSize, totalSize, spacing)).shr(1)
            ListAlignment.SCALE_CHILDREN -> getI0ScaleChildren(childIndex, numChildren, totalSize, spacing)
            ListAlignment.SCALE_SPACES -> getI0ScaleSpaces(childIndex, numChildren, childSize, totalSize)
        }
    }

    private fun getI1(
        childIndex: Int, numChildren: Int, childSize: Int, totalSize: Int,
        spacing: Int, scaleLogic: ListAlignment, i0: Int
    ): Int = if (scaleLogic == ListAlignment.SCALE_CHILDREN) {
        // where the next child would be, minus spacing
        getI0ScaleChildren(childIndex + 1, numChildren, totalSize, spacing) - spacing
    } else {
        // easy
        i0 + childSize
    }

    private fun getIInv(
        valueInI01: Int, numChildren: Int, childSize: Int, totalSize: Int,
        spacing: Int, scaleLogic: ListAlignment
    ): Int = clamp(
        when (scaleLogic) {
            ListAlignment.ALIGN_MIN -> (valueInI01 - spacing) / max(childSize, 1)
            ListAlignment.ALIGN_MAX -> (numChildren - 1) - (totalSize - valueInI01) / max(childSize, 1)
            ListAlignment.ALIGN_CENTER ->
                (valueInI01 - (totalSize - childSize * numChildren).shr(1)) / max(childSize, 1)
            else -> WorkSplitter.partition(valueInI01, childSize, max(totalSize, 1))
        }, 0, numChildren - 1
    )

    private fun isInfinite(total: Int): Boolean {
        return total >= 1_000_000_000
    }

    private fun getMinSize(
        numChildren: Int, childSize: Int, spacing: Int, totalSize: Int,
        scaleLogic: ListAlignment
    ): Int = when (scaleLogic) {
        ListAlignment.ALIGN_MIN, ListAlignment.ALIGN_CENTER, ListAlignment.ALIGN_MAX -> numChildren * (childSize + spacing) + spacing
        ListAlignment.SCALE_CHILDREN ->
            if (isInfinite(totalSize)) numChildren * (childSize + spacing) + spacing
            else max(totalSize, spacing * (numChildren + 1))
        ListAlignment.SCALE_SPACES ->
            if (isInfinite(totalSize)) numChildren * (childSize + spacing) + spacing
            else max(totalSize, childSize * numChildren)
    }

    private fun getChildSize(
        numChildren: Int, childSize: Int, spacing: Int, totalSize: Int,
        scaleLogic: ListAlignment
    ): Int = when (scaleLogic) {
        ListAlignment.ALIGN_MIN, ListAlignment.ALIGN_CENTER, ListAlignment.ALIGN_MAX -> childSize
        ListAlignment.SCALE_CHILDREN ->
            if (isInfinite(totalSize)) childSize
            else max(0, totalSize - spacing * (numChildren + 1)) / max(numChildren, 1)
        ListAlignment.SCALE_SPACES -> childSize
    }

    override val canDrawOverBorders: Boolean get() = true

    var childWidth: Int = style.getSize("childWidth", defaultSize)
    var childHeight: Int = style.getSize("childHeight", defaultSize)

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        val dxi = if (isY) 1 else numTilesY
        val dyi = if (isY) numTilesX else 1
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

    var numTilesX = 1
    var numTilesY = 1

    var maxTilesX = Int.MAX_VALUE
    var maxTilesY = Int.MAX_VALUE

    override fun calculateSize(w: Int, h: Int) {

        val children = children
        val wi = w - padding.width
        val hi = h - padding.height
        val numChildren = children.count2 { it.isVisible }
        if (isY || isInfinite(hi)) {
            val w2 = if (isInfinite(wi)) childWidth * numChildren else wi
            numTilesX = min(max(1, (w2 + spacing) / max(1, childWidth + spacing)), maxTilesX)
            numTilesY = ceilDiv(numChildren, numTilesX)
        } else {
            val h2 = if (isInfinite(hi)) childHeight * numChildren else hi
            numTilesY = min(max(1, (h2 + spacing) / max(1, childHeight + spacing)), maxTilesY)
            numTilesX = ceilDiv(numChildren, numTilesY)
        }

        minW = getMinSize(numTilesX, childWidth, spacing, wi, listAlignmentX)
        minH = getMinSize(numTilesY, childHeight, spacing, hi, listAlignmentY)
        val calcChildWidth = getChildSize(numTilesX, childWidth, spacing, wi, listAlignmentX)
        val calcChildHeight = getChildSize(numTilesY, childHeight, spacing, hi, listAlignmentY)

        for (i in children.indices) {
            val child = children[i]
            if (child.isVisible) {
                child.calculateSize(calcChildWidth, calcChildHeight)
            }
        }
    }

    fun getItemIndexAt(cx: Int, cy: Int): Int {
        val itemX = getIInv(cx - (x + padding.left), numTilesX, childWidth, width, spacing, listAlignmentX)
        val itemY = getIInv(cy - (y + padding.top), numTilesY, childHeight, height, spacing, listAlignmentY)
        val index = if (isY) itemX + itemY * numTilesX else itemX * numTilesY + itemY
        var visIndex = 0
        val children = children
        for (i in children.indices) {
            if (children[i].isVisible) {
                if (visIndex++ == index) {
                    return i
                }
            }
        }
        return 0
    }

    private fun placeChild(child: Panel, ix: Int, iy: Int, x: Int, y: Int, width: Int, height: Int) {
        val x0 = getI0(ix, numTilesX, childWidth, width, spacing, listAlignmentX)
        val x1 = getI1(ix, numTilesX, childWidth, width, spacing, listAlignmentX, x0)
        val y0 = getI0(iy, numTilesY, childHeight, height, spacing, listAlignmentY)
        val y1 = getI1(iy, numTilesY, childHeight, height, spacing, listAlignmentY, y0)
        child.setPosSizeAligned(x + x0, y + y0, x1 - x0, y1 - y0)
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChildrenWithoutPadding(
            x + padding.left,
            y + padding.top,
            width - padding.width,
            height - padding.height
        )
    }

    fun placeChildrenWithoutPadding(x: Int, y: Int, width: Int, height: Int) {
        if (isY) {
            var i = 0
            var iy = 0
            children@ while (true) {
                for (ix in 0 until numTilesX) {
                    var child: Panel
                    do {
                        child = children.getOrNull(i++) ?: break@children
                    } while (!child.isVisible)
                    placeChild(child, ix, iy, x, y, width, height)
                }
                iy++
            }
        } else {
            var i = 0
            var ix = 0
            children@ while (true) {
                for (iy in 0 until numTilesY) {
                    var child: Panel
                    do {
                        child = children.getOrNull(i++) ?: break@children
                    } while (!child.isVisible)
                    placeChild(child, ix, iy, x, y, width, height)
                }
                ix++
            }
        }
    }

    override fun clone(): PanelList2D {
        val clone = PanelList2D(this)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PanelList2D) return
        dst.childWidth = childWidth
        dst.childHeight = childHeight
        dst.listAlignmentX = listAlignmentX
        dst.listAlignmentY = listAlignmentY
        dst.spacing = spacing
        dst.maxTilesX = maxTilesX
        dst.maxTilesY = maxTilesY
        dst.isY = isY
    }
}