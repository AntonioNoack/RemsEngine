package me.anno.ui.debug.console

import me.anno.input.Key
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.scrolling.ScrollPanelXY

class ConsoleLogFullscreen(style: Style) : ScrollPanelXY(Padding(5), style) {
    override fun onBackSpaceKey(x: Float, y: Float) {
        Menu.close(this)
    }

    override fun onSelectAll(x: Float, y: Float) {
        val child = child as? PanelList ?: return
        windowStack.requestFocus(child.children, exclusive = true)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        onSelectAll(x, y)
    }
}