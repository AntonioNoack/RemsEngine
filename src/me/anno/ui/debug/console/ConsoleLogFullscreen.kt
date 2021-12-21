package me.anno.ui.debug.console

import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.studio.StudioBase
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.style.Style

class ConsoleLogFullscreen(style: Style): ScrollPanelXY(Padding(5), style) {
    override fun onBackSpaceKey(x: Float, y: Float) {
        windowStack.pop().destroy()
    }
    override fun onSelectAll(x: Float, y: Float) {
        GFX.inFocus.clear()
        GFX.inFocus.addAll((child as PanelList).children)
    }
    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        onSelectAll(x,y)
    }
}