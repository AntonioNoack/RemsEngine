package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.Window
import me.anno.input.MouseButton
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
                    RemsStudio.windowStack.pop()
                }
            }
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.child as PanelList
            RemsStudio.lastConsoleLines.reversed().forEach {
                val level = if (it.startsWith('[')) {
                    when (it.substring(0, min(4, it.length))) {
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
                    Level.WARNING -> 0xff7777
                    Level.INFO -> 0xffffff
                    else -> -1
                } or DefaultStyle.black
                val panel = object : TextPanel(it, style) {
                    // multiselect to copy multiple lines -> use a single text editor instead xD
                    // todo copy from multiple elements...
                    override fun getMultiSelectablePanel(): Panel? = this
                }
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            RemsStudio.windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}