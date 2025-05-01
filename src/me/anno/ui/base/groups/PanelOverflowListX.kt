package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max

open class PanelOverflowListX(sorter: Comparator<Panel>?, style: Style) : PanelList2(sorter, style) {
    constructor(style: Style) : this(null, style)

    // todo implement all children have same size as an optimization for thousands of entries

    /**
     * how extra space in each line is handled, e.g. left/right/center/fill
     * */
    var lineAlignmentX = AxisAlignment.CENTER

    private val lineWidths = IntArrayList()

    override fun calculateSize(w: Int, h: Int) {
        var maxWidth = 0
        var sumHeight = 0

        var rowWidth = 0
        var rowHeight = 0

        val spacing = spacing
        val children = children
        lineWidths.clear()
        for (i in children.indices) {
            val child = children[i]
            if (!child.isVisible) continue
            child.calculateSize(w, h)
            if (rowWidth > 0 && rowWidth + spacing + child.minW > w) {
                // begin new row
                maxWidth = max(maxWidth, rowWidth)
                sumHeight += rowHeight + spacing
                lineWidths.add(rowWidth)
                rowWidth = 0
                rowHeight = 0
            }

            // append to this row
            rowWidth += spacing + child.minW
            rowHeight = max(rowHeight, child.minH)
        }

        // add last row
        maxWidth = max(maxWidth, rowWidth)
        sumHeight += rowHeight
        lineWidths.add(rowWidth)

        minW = maxWidth + padding.width
        minH = sumHeight + padding.height
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {

        val w = width - padding.width

        var maxWidth = 0
        var yi = y + padding.top

        var rowWidth = 0
        var rowHeight = 0

        var lineIndex = 0
        for (i in children.indices) {
            val child = children[i]
            if (!child.isVisible) {
                child.setPosSize(rowWidth, yi, 1, 1)
                continue
            }

            if (rowWidth > 0 && rowWidth + spacing + child.minW > w) {
                // begin new row
                maxWidth = max(maxWidth, rowWidth)
                yi += rowHeight + spacing
                lineIndex++
                rowWidth = 0
                rowHeight = 0
            }

            val lineWidth = lineWidths.getOrDefault(lineIndex, w)
            val extraSpace = max(w - lineWidth, 0)
            val dx = lineAlignmentX.getOffset(extraSpace, 0)
            child.setPosSizeAligned(x + padding.left + rowWidth + dx, yi, child.minW, child.minH)

            // append to this row
            rowWidth += spacing + child.minW
            rowHeight = max(rowHeight, child.minH)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Previous", "Left" -> selectNext(-1)
            "Next", "Right" -> selectNext(+1)
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    override fun clone(): PanelOverflowListX {
        val clone = PanelOverflowListX(sorter, style)
        copyInto(clone)
        return clone
    }
}