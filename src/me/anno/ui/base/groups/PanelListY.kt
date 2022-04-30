package me.anno.ui.base.groups

import me.anno.Engine
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListY(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {
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
        for (i in children.indices) {
            val child = children[i]
            if (child.visibility != Visibility.GONE) {
                child.calculateSize(availableW, availableH)
                // apply constraints?
                constantSum += child.minH
                maxX = max(maxX, child.x + child.minW)
                weightSum += max(0f, child.weight)
                availableH = max(0, availableH - child.minH)
            }
        }

        val spaceCount = max(0, children.size - 1)
        constantSum += spacing * spaceCount
        sumConst = constantSum
        sumWeight = weightSum

        minW = (maxX - x) + padding.width
        minH = constantSum + padding.height

    }

    override fun setPosition(x: Int, y: Int) {
        // todo some elements don't like this shortcut either...
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
                if (child.visibility != Visibility.GONE) {
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

    override val className: String = "PanelListY"

}