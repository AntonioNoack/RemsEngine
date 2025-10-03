package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max
import kotlin.math.min

/**
 * Related Classes:
 *  - Android: LinearLayout, orientation=vertical
 * */
open class PanelListY(style: Style) : PanelList2(style) {

    private var sumConst = 0
    private var sumConstWW = 0
    private var sumWeight = 0f

    override fun clone(): PanelListY {
        val clone = PanelListY(style)
        copyInto(clone)
        return clone
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val calculator = ListSizeCalculator.push()
        calculator.init(this, w, h)

        val children = children
        val count = children.count2 { it.isVisible }
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child = children[0]
            calculator.addChildSizeY(child, count)
            // assign child measurements to all visible children
            calculator.copySizeOntoChildren(child, children)
        } else {
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    calculator.addChildSizeY(child, 1)
                }
            }
        }

        calculator.addSpacing(spacing, count)
        sumConstWW = calculator.constantSumWW
        sumConst = calculator.constantSum
        sumWeight = calculator.weightSum

        minW = (calculator.maxX - x) + padding.width
        minH = calculator.constantSum + padding.height
        ListSizeCalculator.pop()
    }

    override val visibleIndex0
        get(): Int {
            val idx = children.binarySearch { it.y.compareTo(ly0) }
            return max(0, (if (idx < 0) -1 - idx else idx) - 1)
        }

    override val visibleIndex1
        get(): Int {
            val idx = children.binarySearch { it.y.compareTo(ly1) }
            return min(children.size, if (idx < 0) -1 - idx else idx)
        }

    override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        var idx = children.binarySearch { it.y.compareTo(y) }
        if (idx < 0) idx = -1 - idx
        val i1 = min(children.size - 1, idx)
        val i0 = max(0, idx - 1)
        for (i in i1 downTo i0) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null && panelAt.isVisible) {
                return panelAt
            }
        }
        return null
    }

    override fun placeChildrenWithoutPadding(x: Int, y: Int, width: Int, height: Int) {

        var currentY = y
        val currentY0 = currentY

        val children = children
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            val idealH = height / max(1, children.count2 { it.isVisible })
            val childH = if (children[0].weight > 0f) idealH else min(children[0].minH, idealH)
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    child.setPosSize(x, currentY, width, childH)
                    currentY += childH + spacing
                } else child.setPosSize(x, currentY, 0, 0)
            }
        } else {
            var perWeight = 0f
            var shrinkingFactor = 1f
            if (height > sumConst && sumWeight > 1e-7f) {
                val availableForWeighted = height - sumConstWW
                perWeight = availableForWeighted / sumWeight
            } else if (height < sumConst) {
                shrinkingFactor = height.toFloat() / sumConst.toFloat()
            }
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    var childH = if (perWeight > 0f && child.weight > 0f) {
                        (perWeight * child.weight)
                    } else {
                        (shrinkingFactor * child.minH)
                    }.roundToIntOr()
                    val currentH = currentY - currentY0
                    val remainingH = height - currentH
                    childH = min(childH, remainingH)
                    if (child.minW != width || child.minH != childH) {
                        // update the children, if they need to be updated
                        child.calculateSize(width, childH)
                    }
                    //if (child.x != childX || child.y != currentY || child.w != availableW || child.h != childH) {
                    // something changes, or constraints are used
                    child.setPosSizeAligned(x, currentY, width, childH)
                    //}
                    currentY += childH + spacing
                } else child.setPosSize(x, currentY, 0, 0)
            }
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Previous", "Up" -> selectNext(-1)
            "Next", "Down" -> selectNext(+1)
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }
}