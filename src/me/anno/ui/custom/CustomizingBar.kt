package me.anno.ui.custom

import me.anno.gpu.Cursor
import me.anno.input.Input
import me.anno.ui.base.SpacePanel
import me.anno.ui.style.Style
import me.anno.utils.mixARGB

class CustomizingBar(var index: Int, sizeX: Int, sizeY: Int, style: Style): SpacePanel(sizeX, sizeY, style){

    override fun getCursor(): Long? = if(minW < minH) Cursor.vResize else Cursor.hResize

    val hoverColor = mixARGB(0x77ffb783, originalBGColor, 0.8f)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        backgroundColor = if(isHovered) hoverColor else originalBGColor
        drawBackground()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(isInFocus && 0 in Input.mouseKeysDown){
            val delta = if(minW == 0) dy else dx
            (parent as CustomList).move(index, delta)
        }
    }

}