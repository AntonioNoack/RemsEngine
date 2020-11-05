package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.studio.Logging
import me.anno.studio.RemsStudio
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.console.COFullscreen
import me.anno.ui.debug.console.COLine
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import java.util.logging.Level
import kotlin.math.min

class ConsoleOutputPanel(style: Style): TextPanel("", style) {

    // still sometimes flickering and idk why...
    init { instantTextLoading = true }

    override val effectiveTextColor: Int get() = textColor
    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            // open console in large with scrollbar
            val listPanel = COFullscreen(style)
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
                val panel = COLine(list, msg, style)
                panel.focusTextColor = color
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            RemsStudio.windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}