package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max
import kotlin.math.min

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

    private val calculator = ListSizeCalculator()
    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        calculator.init(this, w, h)

        val children = children
        val count = children.count2 { it.isVisible }
        if (allChildrenHaveSameSize && children.isNotEmpty()) {
            // optimize for case that all children have same size
            val child = children[0]
            calculator.addChildSizeX(child, count)
            // assign child measurements to all visible children
            calculator.copySizeOntoChildren(child, children)
        } else {
            for (i in children.indices) {
                val child = children[i]
                if (child.isVisible) {
                    calculator.addChildSizeX(child, 1)
                }
            }
        }

        calculator.addSpacing(spacing, count)
        sumConst = calculator.constantSum
        sumConstWW = calculator.constantSumWW
        sumWeight = calculator.weightSum

        minW = calculator.constantSum + padding.width
        minH = (calculator.maxY - y) + padding.height
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

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        if (true || needsPosUpdate(x, y)) {
            lastPosTime = Time.frameTimeNanos

            val availableW = width - padding.width
            val availableH = height - padding.height

            var currentX = x + padding.left
            val currentX0 = currentX
            val childY = y + padding.top

            val children = children
            if (allChildrenHaveSameSize && children.isNotEmpty()) {
                val idealW = availableW / max(children.count2 { it.isVisible }, 1)
                val childW = if (children[0].weight > 0f) idealW else min(idealW, children[0].minW)
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
                        }.roundToIntOr()
                        val currentW = currentX - currentX0
                        val remainingW = availableW - currentW
                        childW = min(childW, remainingW)
                        if (child.minW != childW || child.minH != availableH) {
                            // update the children, if they need to be updated
                            child.calculateSize(childW, availableH)
                        }
                        //if (child.x != currentX || child.y != childY || child.w != childW || child.h != availableH) {
                        // something changes, or constraints are used
                        val alignment = child.alignmentY
                        val minH = min(availableH, child.minH)
                        val offset = alignment.getOffset(availableH, minH)
                        val childH = alignment.getSize(availableH, minH)
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
            "Previous", "Left" -> selectNext(-1)
            "Next", "Right" -> selectNext(+1)
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }
}