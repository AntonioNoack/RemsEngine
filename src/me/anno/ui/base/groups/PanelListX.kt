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
 *  - Android: LinearLayout, orientation=horizontal
 * */
open class PanelListX(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {
    constructor(style: Style) : this(null, style)

    private var sumConst = 0
    private var sumConstWW = 0
    private var sumWeight = 0f

    override fun clone(): PanelListX {
        val clone = PanelListX(sorter, style)
        copyInto(clone)
        return clone
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val y = y
        var maxY = y
        var constantSum = 0
        var constantSumWW = 0
        var weightSum = 0f

        var availableW = w - padding.width
        val availableH = h - padding.height

        val children = children
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child0 = children[0]
            val count = children.count2 { it.isVisible }
            child0.calculateSize(availableW, availableH)
            // apply constraints?
            constantSum += count * child0.minW
            maxY = max(maxY, child0.y + child0.minH)
            weightSum += count * max(0f, child0.weight)
            // assign child measurements to all visible children
            for (i in children.indices) {
                val child = children[i]
                child.width = child0.width
                child.height = child0.height
                child.minW = child0.minW
                child.minH = child0.minH
            }
        } else {
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    child.calculateSize(availableW, availableH)
                    // apply constraints?
                    constantSum += child.minW
                    maxY = max(maxY, child.y + child.minH)
                    availableW = max(0, availableW - child.minW)
                    if (child.weight > 0f) {
                        weightSum += child.weight
                    } else {
                        constantSumWW += child.minW
                    }
                }
            }
        }

        val spaceCount = children.size - 1
        val totalSpace = spacing * spaceCount
        constantSum += totalSpace
        constantSumWW += totalSpace
        sumConst = constantSum
        sumConstWW = constantSumWW
        sumWeight = weightSum

        minW = constantSum + padding.width
        minH = (maxY - y) + padding.height
    }

    override val visibleIndex0
        get(): Int {
            val idx = children.binarySearch { it.x.compareTo(lx0) }
            return max(0, (if (idx < 0) -1 - idx else idx) - 1)
        }
    override val visibleIndex1
        get(): Int {
            val idx = children.binarySearch { it.x.compareTo(lx1) }
            return min(children.size, if (idx < 0) -1 - idx else idx)
        }

    override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        var idx = children.binarySearch { it.x.compareTo(x) }
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
        if (true || needsPosUpdate(x, y)) {
            lastPosTime = Time.nanoTime

            super.setPosition(x, y)

            val availableW = width - padding.width
            val availableH = height - padding.height

            var currentX = x + padding.left
            val childY = y + padding.top

            val children = children
            if (allChildrenHaveSameSize) {
                val childW = availableW / max(children.count2 { it.isVisible }, 1)
                for (i in children.indices) {
                    val child = children[i]
                    if (child.isVisible) {
                        child.setPosSize(currentX, childY, childW, availableH)
                        currentX += childW + spacing
                    } else child.setPosSize(currentX, childY, 1, 1)
                }
            } else {
                var perWeight = 0f
                var minWFactor = 1f
                if (availableW > sumConst && sumWeight > 1e-7f) {
                    val availableForWeighted = availableW - sumConstWW
                    perWeight = availableForWeighted / sumWeight
                } else if (availableW < sumConstWW) {
                    minWFactor = availableW.toFloat() / sumConstWW.toFloat()
                }
                for (i in children.indices) {
                    val child = children[i]
                    if (child.isVisible) {
                        var childW = if (perWeight > 0f && child.weight > 0f) {
                            (perWeight * child.weight)
                        } else {
                            (minWFactor * child.minW)
                        }.roundToInt()
                        val currentW = currentX - x
                        val remainingW = availableW - currentW
                        childW = min(childW, remainingW)
                        if (child.width != childW || child.height != availableH) {
                            // update the children, if they need to be updated
                            child.calculateSize(childW, availableH)
                        }
                        //if (child.x != currentX || child.y != childY || child.w != childW || child.h != availableH) {
                        // something changes, or constraints are used
                        val alignment = child.alignmentY
                        val minH = min(availableH, child.minH)
                        val offset = alignment.getOffset(availableH, minH)
                        val childH = alignment.getWidth(availableH, minH)
                        child.setPosSize(currentX, childY + offset, childW, childH)
                        //}
                        currentX += childW + spacing
                    } else child.setPosSize(currentX, childY, 1, 1)
                }
            }
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Previous", "Left" -> selectPrevious()
            "Next", "Right" -> selectNext()
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    override val className: String get() = "PanelListX"
}