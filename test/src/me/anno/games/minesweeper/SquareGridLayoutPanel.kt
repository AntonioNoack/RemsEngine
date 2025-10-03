package me.anno.games.minesweeper

import me.anno.config.DefaultConfig.style
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import kotlin.math.min

/**
 * Calculates layout for child elements, such that they are grouped together, are all square,
 * all have the same size, same width and height, and are centered
 * */
class SquareGridLayoutPanel(val sx: Int, val sy: Int, createPanel: (xi: Int, yi: Int) -> Panel) :
    PanelList(style) {

    init {
        for (yi in 0 until sy) {
            for (xi in 0 until sx) {
                add(createPanel(xi, yi))
            }
        }
    }

    override fun placeChildrenWithoutPadding(x: Int, y: Int, width: Int, height: Int) {
        val childSize = min(width / sx, height / sy)
        val usedWidth = childSize * sx
        val usedHeight = childSize * sy
        val paddingX = (width - usedWidth) shr 1
        val paddingY = (height - usedHeight) shr 1
        for (yi in 0 until sy) {
            for (xi in 0 until sx) {
                val cx = x + paddingX + xi * childSize
                val cy = y + paddingY + yi * childSize
                children[xi + sx * yi]
                    .setPosSize(cx, cy, childSize, childSize)
            }
        }
    }
}