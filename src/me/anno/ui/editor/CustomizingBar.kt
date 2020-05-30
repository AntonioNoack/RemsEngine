package me.anno.ui.editor

import me.anno.gpu.Cursor
import me.anno.input.Input
import me.anno.ui.base.SpacePanel
import me.anno.ui.custom.CustomList
import me.anno.ui.style.Style

class CustomizingBar(val index: Int, sizeX: Int, sizeY: Int, style: Style): SpacePanel(sizeX, sizeY, style){

    override fun getCursor(): Long? = if(minW < minH) Cursor.vResize else Cursor.hResize

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(isInFocus && 0 in Input.mouseKeysDown){
            val delta = if(minW == 0) dy else dx
            (parent as CustomList).move(index, delta)
        }
    }

}