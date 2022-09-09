package me.anno.ui.base.groups

import me.anno.Engine
import me.anno.ui.Panel
import me.anno.ui.style.Style
import me.anno.utils.structures.lists.Lists.count2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListY(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {
    constructor(style: Style) : this(null, style)

    private var sumConst = 0
    private var sumWeight = 0f

    override fun clone(): PanelListY {
        val clone = PanelListY(sorter, style)
        copy(clone)
        return clone
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val x = x
        var maxX = x
        var constantSum = 0
        var weightSum = 0f

        val availableW = w - padding.width
        var availableH = h - padding.height

        val children = children
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child = children[min(visibleIndex0, children.lastIndex)]
            val count = children.count2 { it.isVisible }
            child.calculateSize(availableW, availableH)
            // apply constraints?
            constantSum += count * child.minH
            maxX = max(maxX, child.x + child.minW)
            weightSum += count * max(0f, child.weight)
            // assign child measurements to all visible children
            for (i in visibleIndex0 until visibleIndex1) {
                val child2 = children[i]
                child2.w = child.w
                child2.h = child.h
                child2.minW = child.minW
                child2.minH = child.minH
            }
        } else {
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    child.calculateSize(availableW, availableH)
                    // apply constraints?
                    constantSum += child.minH
                    maxX = max(maxX, child.x + child.minW)
                    weightSum += max(0f, child.weight)
                    availableH = max(0, availableH - child.minH)
                }
            }
        }

        val spaceCount = max(0, children.size - 1)
        constantSum += spacing * spaceCount
        sumConst = constantSum
        sumWeight = weightSum

        minW = (maxX - x) + padding.width
        minH = constantSum + padding.height

    }

    override val visibleIndex0
        get(): Int {
            return 0
            // val idx = children.binarySearch { it.y.compareTo(ly0) }
            // return max(0, (if (idx < 0) -1 - idx else idx) - 1)
        }
    override val visibleIndex1
        get(): Int {
            return children.size
            // val idx = children.binarySearch { it.y.compareTo(ly1) }
            // return min(children.size, if (idx < 0) -1 - idx else idx)
        }

    /*override fun getChildPanelAt(x: Int, y: Int): Panel? {
        val children = children
        var idx = children.binarySearch { it.y.compareTo(y) }
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
        // todo some elements don't like this shortcut...
        if (true || needsPosUpdate(x, y)) {
            lastPosTime = Engine.gameTime

            super.setPosition(x, y)

            val availableW = w - padding.width
            val availableH = h - padding.height

            var perWeight = 0f
            val sumWeight = sumWeight
            val sumConst = sumConst
            if (availableH > sumConst && sumWeight > 1e-7f) {
                val extraAvailable = availableH - sumConst
                perWeight = extraAvailable / sumWeight
            }

            val childX = x + padding.left
            var currentY = y + padding.top

            val children = children
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    var childH = (child.minH + perWeight * max(0f, child.weight)).roundToInt()
                    val currentH = currentY - y
                    val remainingH = availableH - currentH
                    childH = min(childH, remainingH)
                    if (child.w != availableW || child.h != childH) {
                        // update the children, if they need to be updated
                        child.calculateSize(availableW, childH)
                    }
                    //if (child.x != childX || child.y != currentY || child.w != availableW || child.h != childH) {
                    // something changes, or constraints are used
                    child.setPosSize(childX, currentY, availableW, childH)
                    //}
                    currentY += childH + spacing
                }
            }
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Previous", "Up" -> selectPrevious()
            "Next", "Down" -> selectNext()
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    override val className = "PanelListY"

}