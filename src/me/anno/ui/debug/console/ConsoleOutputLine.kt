package me.anno.ui.debug.console

import me.anno.input.MouseButton
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style

class ConsoleOutputLine(val list: PanelList, msg: String, style: Style) : TextPanel(msg, style) {
    override fun getMultiSelectablePanel(): Panel = this
    override fun onCopyRequested(x: Float, y: Float): String {
        return list.children
            .filterIsInstance<TextPanel>()
            .filter { it.isInFocus }
            .joinToString("\n") { it.text }
    }

    override fun onSelectAll(x: Float, y: Float) {
        list.children.forEachIndexed { index, panel ->
            panel.requestFocus(index == 0)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        onSelectAll(x, y)
    }

    override val className get() = "ConsoleOutputLine"
}