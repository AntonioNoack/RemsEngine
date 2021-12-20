package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class PanelListX(sorter: Comparator<Panel>?, style: Style) : PanelList(sorter, style) {
    constructor(style: Style) : this(null, style)

    private var sumConst = 0
    private var sumWeight = 0f

    override fun clone(): PanelListX {
        val clone = PanelListX(sorter, style)
        copy(clone)
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
        for (i in children.indices) {
            val child = children[i]
            if (child.visibility != Visibility.GONE) {
                child.calculateSize(availableW, availableH)
                // apply constraints?
                constantSum += child.minW
                maxY = max(maxY, child.y + child.minH)
                weightSum += max(0f, child.weight)
            }
        }

        val spaceCount = children.size - 1
        constantSum += spacing * spaceCount
        sumConst = constantSum
        sumWeight = weightSum

        minW = constantSum + padding.width
        minH = (maxY - y) + padding.height

    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)

        var perWeight = 0f
        val perConst = 1f // could be used to force elements into the available space

        val availableW = w - padding.width
        val availableH = h - padding.height

        if (availableW > sumConst && sumWeight > 1e-16f) {
            val extraAvailable = availableW - sumConst
            perWeight = extraAvailable / sumWeight
        }

        var currentX = x + padding.left
        val childY = y + padding.top
        val children = children
        for (i in children.indices) {
            val child = children[i]
            if (child.visibility != Visibility.GONE) {
                var childW = (perConst * child.minW + perWeight * max(0f, child.weight)).roundToInt()
                val currentW = currentX - x
                val remainingW = availableW - currentW
                childW = min(childW, remainingW)
                child.calculateSize(childW, availableH)
                child.placeInParent(currentX, childY)
                child.applyPlacement(childW, availableH)
                currentX += childW + spacing
            }
        }

    }

}