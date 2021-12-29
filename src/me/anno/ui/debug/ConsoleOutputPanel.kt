package me.anno.ui.debug

import me.anno.Logging
import me.anno.config.DefaultStyle
import me.anno.engine.RemsEngine
import me.anno.gpu.Window
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.ui.base.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.console.COLine
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.mixARGB
import org.apache.logging.log4j.LogManager

// todo second console output panel, which has the default font?

open class ConsoleOutputPanel(style: Style) : SimpleTextPanel(style) {

    override fun tickUpdate() {
        invalidateDrawing()
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            // open console in large with scrollbar
            val listPanel = ConsoleLogFullscreen(style)
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.content as PanelList
            list += TextButton(Dict["Close", "ui.general.close"], false, style).addLeftClickListener {
                windowStack.pop().destroy()
            }
            val lcl = Logging.lastConsoleLines
            for (i in lcl.lastIndex downTo 0) {
                val msg = lcl[i]
                val panel = COLine(list, msg, style)
                val color = when {
                    msg.startsWith("[INF") -> panel.textColor
                    msg.startsWith("[WAR") -> 0xffff00
                    msg.startsWith("[ERR") -> 0xff0000
                    msg.startsWith("[DEB") || msg.startsWith("[FIN") -> 0x77ff77
                    else -> -1
                } or DefaultStyle.black
                // todo if line contains file, then add a section for that
                // todo styled simple text panel: colors, and actions for sections of text
                panel.focusTextColor = color
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            windowStack.add(Window(listPanel, true, windowStack, 0, 0))
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ConsoleOutputPanel::class)

        fun formatFilePath(file: FileReference) = formatFilePath(file.absolutePath)
        fun formatFilePath(file: String): String {
            return "file://${file.replace(" ", "%20")}"
        }

        fun createConsole(style: Style): ConsoleOutputPanel {
            val console = ConsoleOutputPanel(style.getChild("small"))
            // console.fontName = "Segoe UI"
            Logging.console = console
            console.setTooltip("Double-click to open history")
            console.text = Logging.lastConsoleLines.lastOrNull() ?: ""
            return console
        }

        fun createConsoleWithStats(bottom: Boolean = true, style: Style): Panel {

            val group = PanelStack(style)
            group += createConsole(style)

            val right = PanelListX(style)
            val rip = RuntimeInfoPanel(style)
            rip.alignmentX = AxisAlignment.MAX
            rip.makeBackgroundOpaque()
            rip.setWeight(1f)
            rip.addLeftClickListener {
                System.gc()
                LOGGER.info("Called Garbage Collector")
            }
            right.add(rip)
            right.makeBackgroundTransparent()
            if (bottom) right.add(RemsEngine.RuntimeInfoPlaceholder())
            group.add(right)

            return group

        }

    }

}