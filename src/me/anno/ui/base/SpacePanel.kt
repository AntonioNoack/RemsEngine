package me.anno.ui.base

import me.anno.ui.style.Style

open class SpacePanel(sizeX: Int, sizeY: Int, style: Style): Panel(style.getChild("spacer")){

    init {
        minW = sizeX
        minH = sizeY
    }

    fun setColor(color: Int): SpacePanel {
        backgroundColor = color
        return this
    }

}