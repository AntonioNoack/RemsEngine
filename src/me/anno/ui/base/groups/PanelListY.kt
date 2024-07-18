package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.structures.lists.Lists.count2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Related Classes:
 *  - Android: LinearLayout, orientation=vertical
 * */
open class PanelListY(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {
    constructor(style: Style) : this(null, style)

    private var sumConst = 0
    private var sumConstWW = 0
    private var sumWeight = 0f

    override fun clone(): PanelListY {
        val clone = PanelListY(sorter, style)
        copyInto(clone)
        return clone
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val x = x
        var maxX = x
        var constantSum = 0
        var constantSumWW = 0
        var weightSum = 0f

        val availableW = w - padding.width
        var availableH = h - padding.height

        fun addChildSize(child: Panel, childCount: Int) {
            val dy = child.minH * childCount
            constantSum += dy
            maxX = max(maxX, child.x + child.minW)
            availableH = max(0, availableH - dy)
            if (child.weight > 0f) {
                weightSum += child.weight * childCount
            } else {
                constantSumWW += dy
            }
        }

        val children = children
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child = children[0]
            val count = children.count2 { it.isVisible }
            child.calculateSize(availableW, availableH)
            addChildSize(child, count)
            // assign child measurements to all visible children
            for (i in 1 until children.size) {
                val childI = children[i]
                childI.width = child.width
                childI.height = child.height
                childI.minW = child.minW
                childI.minH = child.minH
            }
        } else {
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    child.calculateSize(availableW, availableH)
                    addChildSize(child, 1)
                }
            }
        }

        val spaceCount = max(0, children.size - 1)
        val totalSpace = spacing * spaceCount
        constantSum += totalSpace
        constantSumWW += totalSpace
        sumConstWW = constantSumWW
        sumConst = constantSum
        sumWeight = weightSum

        minW = (maxX - x) + padding.width
        minH = constantSumWW + padding.height
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
            if (panelAt != null) return panelAt
        }
        return null
    }

    override fun setPosition(x: Int, y: Int) {
        // todo some elements don't like this shortcut...
        if (true || needsPosUpdate(x, y)) {
            lastPosTime = Time.frameTimeNanos

            super.setPosition(x, y)

            val availableW = width - padding.width
            val availableH = height - padding.height

            val childX = x + padding.left
            var currentY = y + padding.top
            val currentY0 = currentY

            val children = children
            if (allChildrenHaveSameSize && children.isNotEmpty()) {
                val idealH = availableH / max(1, children.count2 { it.isVisible })
                val childH = if (children[0].weight > 0f) idealH else min(children[0].minH, idealH)
                for (i in children.indices) {
                    val child = children[i]
                    if (child.isVisible) {
                        child.setPosSize(childX, currentY, availableW, childH)
                        currentY += childH + spacing
                    } else child.setPosSize(childX, currentY, 1, 1)
                }
            } else {
                var perWeight = 0f
                var minWFactor = 1f
                if (availableH > sumConst && sumWeight > 1e-7f) {
                    val availableForWeighted = availableH - sumConstWW
                    perWeight = availableForWeighted / sumWeight
                } else if (availableH < sumConst) {
                    minWFactor = availableH.toFloat() / sumConst.toFloat()
                }
                for (i in children.indices) {
                    val child = children[i]
                    if (child.isVisible) {
                        var childH = if (perWeight > 0f && child.weight > 0f) {
                            (perWeight * child.weight)
                        } else {
                            (minWFactor * child.minH)
                        }.roundToInt()
                        val currentH = currentY - currentY0
                        val remainingH = availableH - currentH
                        childH = min(childH, remainingH)
                        if (child.minW != availableW || child.minH != childH) {
                            // update the children, if they need to be updated
                            child.calculateSize(availableW, childH)
                        }
                        //if (child.x != childX || child.y != currentY || child.w != availableW || child.h != childH) {
                        // something changes, or constraints are used
                        val alignment = child.alignmentX
                        val minW = min(availableW, child.minW)
                        val offset = alignment.getOffset(availableW, minW)
                        val childW = alignment.getSize(availableW, minW)
                        child.setPosSize(childX + offset, currentY, childW, childH)
                        //}
                        currentY += childH + spacing
                    } else child.setPosSize(childX, currentY, 1, 1)
                }
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