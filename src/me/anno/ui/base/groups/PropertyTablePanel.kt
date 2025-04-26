package me.anno.ui.base.groups

import me.anno.ui.Style
import me.anno.utils.structures.arrays.IntArrayList

/**
 * Table layout where children are take up their minimum size (or scaled).
 * Left column always will use minimum size, right side will use remainder.
 * */
open class PropertyTablePanel(sizeX: Int, sizeY: Int, style: Style) :
    MinSizeTablePanel(sizeX, sizeY, style) {

    override fun placeChildrenAxis(
        totalSize: Int,
        sizeI: Int,
        dst: IntArrayList,
        minSize: IntArrayList,
        paddingLeft: Int,
        paddingRight: Int,
        isX: Boolean
    ) {
        if (!isX) {
            super.placeChildrenAxis(totalSize, sizeI, dst, minSize, paddingLeft, paddingRight, false)
            return
        }

        dst.size = sizeI + 1

        val spacing = spacing
        val minSize0 = minSize[0]
        val availableW = totalSize - (sizeI - 1) * spacing - (paddingLeft + paddingRight) - minSize0
        val shrinkFactor = availableW.toFloat() / (minSize.sum() - minSize0).toFloat()

        var offset = 0f
        for (i in 0..sizeI) {
            dst[i] = paddingLeft + i * spacing + offset.toInt()
            if (i == 0) offset += minSize0
            else if (i < sizeI) offset += minSize[i] * shrinkFactor
        }
    }
}