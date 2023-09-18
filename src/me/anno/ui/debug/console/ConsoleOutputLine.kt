package me.anno.ui.debug.console

import me.anno.input.Key
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.Style

class ConsoleOutputLine(val list: PanelList, msg: String, style: Style) : TextPanel(msg, style) {
    override fun getMultiSelectablePanel(): Panel = this
    override fun onCopyRequested(x: Float, y: Float): String {
        return list.children
            .filterIsInstance<TextPanel>()
            .filter { it.isInFocus }
            .joinToString("\n") { it.text }
    }

    override fun onSelectAll(x: Float, y: Float) {
        val children = list.children
        for (index in children.indices) {
            val panel = children[index]
            panel.requestFocus(index == 0)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        onSelectAll(x, y)
    }

    override val className: String get() = "ConsoleOutputLine"
}