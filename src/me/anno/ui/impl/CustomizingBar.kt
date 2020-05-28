package me.anno.ui.impl

import me.anno.gpu.Cursor
import me.anno.ui.base.SpacePanel
import me.anno.ui.style.Style

class CustomizingBar(size: Int, style: Style): SpacePanel(size, size, style){

    override fun getCursor(): Long? = Cursor.drag

}