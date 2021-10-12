package me.anno.ui.debug.console

import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style

class COLine(val list: PanelList, msg: String, style: Style): TextPanel(msg, style) {
    override fun getMultiSelectablePanel(): Panel = this
    override fun onCopyRequested(x: Float, y: Float): String {
        val all = rootPanel.listOfAll.toList()
        return GFX.inFocus
            .filterIsInstance<TextPanel>()
            .map { it.text to all.indexOf(it) }
            .sortedBy { it.second }
            .joinToString("\n"){ it.first }
    }
    override fun onSelectAll(x: Float, y: Float) {
        GFX.inFocus.clear()
        GFX.inFocus.addAll(list.children)
    }
    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        onSelectAll(x,y)
    }
}