package me.anno.ui.base

import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style

open class SpacePanel(sizeX: Int, sizeY: Int, style: Style): Panel(style.getChild("spacer")){

    init {
        minW = sizeX
        minH = sizeY
        when {
            sizeX == 0 -> {
                layoutConstraints += WrapAlign.TopFill
            }
            sizeY == 0 -> {
                layoutConstraints += WrapAlign.Left
            }
        }
    }

    fun setColor(color: Int): SpacePanel {
        backgroundColor = color
        return this
    }

}