package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.studio.Logging
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.console.COLine
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.mixARGB
import kotlin.math.min

// todo second console output panel, which has the default font?

open class ConsoleOutputPanel(style: Style) : SimpleTextPanel(style) {

    // todo open path, if clicked on

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
                val color = if (msg.startsWith('[')) {
                    when (msg.substring(0, min(4, msg.length))) {
                        "[INF" -> 0xffffff
                        "[WAR" -> 0xffff00
                        "[ERR" -> 0xff0000
                        "[DEB",
                        "[FIN" -> 0x77ff77
                        else -> -1
                    } or DefaultStyle.black
                } else -1
                val panel = COLine(list, msg, style)
                panel.focusTextColor = color
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}