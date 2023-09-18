package me.anno.ui.base.groups

import me.anno.Engine
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
            val child = children[min(visibleIndex0, children.lastIndex)]
            val count = children.count2 { it.isVisible }
            child.calculateSize(availableW, availableH)
            // apply constraints?
            constantSum += count * child.minW
            maxY = max(maxY, child.y + child.minH)
            weightSum += count * max(0f, child.weight)
            // assign child measurements to all visible children
            for (i in visibleIndex0 until visibleIndex1) {
                val child2 = children[i]
                child2.width = child.width
                child2.height = child.height
                child2.minW = child.minW
                child2.minH = child.minH
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

    // todo these, and in PanelListY, make things unstable when scrolling
    override val visibleIndex0
        get(): Int {
            return 0
            // val idx = children.binarySearch { it.x.compareTo(lx0) }
            // return max(0, (if (idx < 0) -1 - idx else idx) - 1)
        }
    override val visibleIndex1
        get(): Int {
            /* val idx = children.binarySearch { it.x.compareTo(lx1) }
             return min(children.size, if (idx < 0) -1 - idx else idx)*/
            return children.size
        }

    /*override fun getChildPanelAt(x: Int, y: Int): Panel? {
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
    }*/

    override fun setPosition(x: Int, y: Int) {
        if (true || needsPosUpdate(x, y)) {
            lastPosTime = Engine.gameTime

            super.setPosition(x, y)

            val availableW = width - padding.width
            val availableH = height - padding.height

            var perWeight = 0f
            val sumWeight = sumWeight
            val sumConst = sumConst
            if (availableW > sumConst && sumWeight > 1e-7f) {
                val extraAvailable = availableW - sumConst
                perWeight = extraAvailable / sumWeight
            }

            // todo if all children have same size, update like PanelList2d
            // todo same for panel list y

            var currentX = x + padding.left
            val childY = y + padding.top

            val children = children
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