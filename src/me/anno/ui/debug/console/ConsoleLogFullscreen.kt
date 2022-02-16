package me.anno.ui.debug.console

import me.anno.input.MouseButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.style.Style

class ConsoleLogFullscreen(style: Style) : ScrollPanelXY(Padding(5), style) {
    override fun onBackSpaceKey(x: Float, y: Float) {
        windowStack.pop().destroy()
    }

    override fun onSelectAll(x: Float, y: Float) {
        val child = child as? PanelList ?: return
        child.children.forEachIndexed { index, panel ->
            panel.requestFocus(index == 0)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        onSelectAll(x, y)
    }
}