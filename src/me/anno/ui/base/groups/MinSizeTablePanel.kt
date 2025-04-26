package me.anno.ui.base.groups

import me.anno.ui.Style
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max

/**
 * Table layout where children are take up their minimum size (or scaled).
 * */
open class MinSizeTablePanel(sizeX: Int, sizeY: Int, style: Style) :
    TablePanel(sizeX, sizeY, style) {

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)

        val sizeX = sizeX
        val sizeY = sizeY

        val minWs = minWs
        val minHs = minHs
        minWs.size = sizeX + 1
        minHs.size = sizeY + 1
        minWs.fill(0)
        minHs.fill(0)

        val perWeightX = (w - totalSpacingX)
        val perWeightY = (h - totalSpacingY)
        for (y in 0 until sizeY) {
            val sizeYGuess = perWeightY
            for (x in 0 until sizeX) {
                val child = children[getIndex(x, y)]
                if (!child.isVisible) continue
                val sizeXGuess = perWeightX
                child.calculateSize(sizeXGuess, sizeYGuess) // for its children
                minWs[x] = max(minWs[x], child.minW)
                minHs[y] = max(minHs[y], child.minH)
            }
        }

        minW = minWs.sum().toInt() + totalSpacingX
        minH = minHs.sum().toInt() + totalSpacingY
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        placeChildrenAxis(width, sizeX, xs, minWs, padding.left, padding.right, true)
        placeChildrenAxis(height, sizeY, ys, minHs, padding.top, padding.bottom, false)
        super.placeChildren(x, y, width, height)
    }

    open fun placeChildrenAxis(
        totalSize: Int, sizeI: Int,
        dst: IntArrayList, minSize: IntArrayList,
        paddingLeft: Int, paddingRight: Int, isX: Boolean
    ) {

        // ensure we don't access out of bounds
        dst.size = sizeI + 1

        val spacing = spacing
        val availableW = totalSize - (sizeI - 1) * spacing - (paddingLeft + paddingRight)
        val shrinkFactor = availableW.toFloat() / max(minSize.sum(), 1).toFloat()

        var offset = 0f
        for (i in 0..sizeI) {
            dst[i] = paddingLeft + i * spacing + offset.toInt()
            if (i < sizeI) offset += minSize[i] * shrinkFactor
        }
    }
}