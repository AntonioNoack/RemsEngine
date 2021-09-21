package me.anno.ui.base

import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style

open class SpacePanel(val sizeX: Int, val sizeY: Int, style: Style) : Panel(style.getChild("spacer")) {

    init {
        when {
            sizeX == 0 -> layoutConstraints += WrapAlign.TopFill
            sizeY == 0 -> layoutConstraints += WrapAlign.Left
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        minW = sizeX
        minH = sizeY
    }

    fun setColor(color: Int): SpacePanel {
        backgroundColor = color
        return this
    }

}