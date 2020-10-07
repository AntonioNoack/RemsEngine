package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.studio.Logging
import me.anno.studio.RemsStudio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style
import me.anno.utils.mixARGB
import java.util.logging.Level
import kotlin.math.min

class ConsoleOutputPanel(style: Style): TextPanel("", style) {
    override val effectiveTextColor: Int get() = textColor
    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            // open console in large with scrollbar
            val listPanel = object : ScrollPanelY(Padding(5), AxisAlignment.CENTER, style) {
                override fun onBackSpaceKey(x: Float, y: Float) {
                    RemsStudio.windowStack.pop().destroy()
                }
                override fun onSelectAll(x: Float, y: Float) {
                    inFocus.clear()
                    inFocus.addAll((child as PanelList).children)
                }
                override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
                    onSelectAll(x,y)
                }
            }
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.child as PanelList
            Logging.lastConsoleLines.reversed().forEach { msg ->
                val level = if (msg.startsWith('[')) {
                    when (msg.substring(0, min(4, msg.length))) {
                        "[INF" -> Level.INFO
                        "[WAR" -> Level.WARNING
                        "[ERR" -> Level.SEVERE
                        "[DEB", "[FIN" -> Level.FINE
                        else -> Level.INFO
                    }
                } else Level.INFO
                val color = when (level) {
                    Level.FINE -> 0x77ff77
                    Level.SEVERE -> 0xff0000
                    Level.WARNING -> 0xffff00
                    Level.INFO -> 0xffffff
                    else -> -1
                } or DefaultStyle.black
                val panel = object : TextPanel(msg, style) {
                    override fun getMultiSelectablePanel(): Panel? = this
                    override fun onCopyRequested(x: Float, y: Float): String? {
                        val all = rootPanel.listOfAll.toList()
                        return inFocus
                            .filterIsInstance<TextPanel>()
                            .map { it.text to all.indexOf(it) }
                            .sortedBy { it.second }
                            .joinToString("\n"){ it.first }
                    }
                    override fun onSelectAll(x: Float, y: Float) {
                        inFocus.clear()
                        inFocus.addAll(list.children)
                    }
                    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
                        onSelectAll(x,y)
                    }
                }
                panel.focusTextColor = color
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            RemsStudio.windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}