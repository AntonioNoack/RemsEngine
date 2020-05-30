package me.anno.ui.base

import me.anno.gpu.GFX
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.style.Style
import me.anno.utils.warn

class MenuBase(child: Panel, style: Style): PanelContainer(child, Padding(0), style){

    init {
        this += WrapAlign.LeftTop
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        GFX.windowStack.removeAll { it.panel == this }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        warn("MenuBase ${child.x},${child.y} += ${child.w},${child.h}")
    }

}