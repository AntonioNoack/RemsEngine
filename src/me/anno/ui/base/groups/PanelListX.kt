package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.structures.lists.Lists.count2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListX(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {
    constructor(style: Style) : this(null, style)

    private var sumConst = 0
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
        var weightSum = 0f

        val availableW = w - padding.width
        val availableH = h - padding.height

        val children = children
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child0 = children[min(visibleIndex0, children.lastIndex)]
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
                    weightSum += max(0f, child.weight)
                }
            }
        }

        val spaceCount = children.size - 1
        constantSum += spacing * spaceCount
        sumConst = constantSum
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
        for (i in min(children.size - 1, idx) downTo max(0, idx - 1)) {
            val panelAt = children[i].getPanelAt(x, y)
            if (panelAt != null && panelAt.isOpaqueAt(x, y)) {
                return panelAt
            }
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
                val sumWeight = sumWeight
                val sumConst = sumConst
                if (availableW > sumConst && sumWeight > 1e-7f) {
                    val extraAvailable = availableW - sumConst
                    perWeight = extraAvailable / sumWeight
                }
                for (i in children.indices) {
                    val child = children[i]
                    if (child.isVisible) {
                        var childW = (child.minW + perWeight * max(0f, child.weight)).roundToInt()
                        val currentW = currentX - x
                        val remainingW = availableW - currentW
                        childW = min(childW, remainingW)
                        if (child.width != childW || child.height != availableH) {
                            // update the children, if they need to be updated
                            child.calculateSize(childW, availableH)
                        }
                        //if (child.x != currentX || child.y != childY || child.w != childW || child.h != availableH) {
                        // something changes, or constraints are used
                        child.setPosSize(currentX, childY, childW, availableH)
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