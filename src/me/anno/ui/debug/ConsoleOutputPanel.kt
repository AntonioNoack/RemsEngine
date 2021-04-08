package me.anno.ui.debug

import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.GFXx2D
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.studio.Logging
import me.anno.ui.base.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.debug.console.COLine
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min

class ConsoleOutputPanel(style: Style) : Panel(style) {

    var text = ""
        set(value) {
            field = value.trim()
            invalidateDrawing()
        }

    val font = GFXx2D.monospaceFont

    var textColor = style.getColor("textColor", DefaultStyle.iconGray)
    var focusTextColor = style.getColor("textColorFocused", -1)

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isEmpty()) "." else text
        super.calculateSize(w, h)
        val w2 = font.sampleWidth * text.length + 4
        val h2 = font.sampleHeight + 4
        minW = max(1, w2)
        minH = max(1, h2)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        GFX.loadTexturesSync.push(true)
        super.onDraw(x0, y0, x1, y1)
        GFXx2D.drawSimpleTextCharByChar(
            x + 1, y + 2, 2, // idk...
            text, textColor, backgroundColor
        )
        GFX.loadTexturesSync.pop()
    }

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
            windowStack.add(Window(listPanel, true, 0, 0))
        }
    }
}