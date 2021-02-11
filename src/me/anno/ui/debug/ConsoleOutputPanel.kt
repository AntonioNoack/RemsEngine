package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.studio.Logging
import me.anno.studio.StudioBase
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.debug.console.COLine
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import java.util.logging.Level
import kotlin.math.min

class ConsoleOutputPanel(style: Style): TextPanel("", style) {

    // still sometimes flickering and idk why...
    init { instantTextLoading = true }

    // todo open path, if clicked on

    override val effectiveTextColor: Int get() = textColor
    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            // open console in large with scrollbar
            val listPanel = ConsoleLogFullscreen(style)
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.content as PanelList
            list += TextButton(Dict["Close", "ui.general.close"], false, style).setSimpleClickListener {
                windowStack.pop().destroy()
            }
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
            StudioBase.instance.windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}