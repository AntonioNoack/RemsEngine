package me.anno.ui.debug

import me.anno.Logging.lastConsoleLines
import me.anno.config.DefaultStyle
import me.anno.engine.RemsEngine
import me.anno.gpu.texture.Texture2D
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.debug.console.ConsoleOutputLine
import me.anno.ui.style.Style
import me.anno.utils.files.Files.formatFileSize
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread
import kotlin.math.max

// todo second console output panel, which has the default font?

/**
 * displays all recent logging messages, and opens an overview on double click
 * */
open class ConsoleOutputPanel(style: Style) : SimpleTextPanel(style) {

    override fun onUpdate() {
        val text = lastConsoleLines.peek() ?: ""
        this.text = text
        tooltip = text.ifBlank { "Double-click to open history" }
        textColor = getTextColor(text) and 0x77ffffff
        super.onUpdate()
    }

    fun getTextColor(msg: String): Int {
        return when {
            msg.startsWith("[INF") -> textColor
            msg.startsWith("[WAR") -> 0xffff00
            msg.startsWith("[ERR") -> 0xff0000
            msg.startsWith("[DEB") || msg.startsWith("[FIN") -> 0x77ff77
            else -> -1
        } or DefaultStyle.black
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
            val lcl = lastConsoleLines.toList()
            for (i in lcl.indices.reversed()) {
                val msg = lcl.getOrNull(i) ?: continue
                val panel = ConsoleOutputLine(list, msg, style)
                val color = getTextColor(msg)
                // todo if line contains file, then add a section for that
                // todo styled simple text panel: colors, and actions for sections of text
                panel.focusTextColor = color
                panel.textColor = mixARGB(panel.textColor, color, 0.5f)
                list += panel
            }
            windowStack.add(Window(listPanel, isTransparent = false, windowStack))
        }
    }

    override val className: String = "ConsoleOutputPanel"

    companion object {

        private val LOGGER = LogManager.getLogger(ConsoleOutputPanel::class)

        fun formatFilePath(file: FileReference) = formatFilePath(file.absolutePath)
        fun formatFilePath(file: String): String {
            return "file://${file.replace(" ", "%20")}"
        }

        fun createConsole(style: Style): ConsoleOutputPanel {
            val console = ConsoleOutputPanel(style.getChild("small"))
            // console.fontName = "Segoe UI"
            console.tooltip = "Double-click to open history"
            return console
        }

        fun createConsoleWithStats(bottom: Boolean = true, style: Style): Panel {

            val group = PanelStack(style)
            val console = createConsole(style)
            group += console

            val right = object : PanelListX(style) {
                override fun onUpdate() {
                    super.onUpdate()
                    tooltip = console.text
                }
            }
            val rip = RuntimeInfoPanel(style)
            rip.alignmentX = AxisAlignment.MAX
            rip.makeBackgroundOpaque()
            rip.setWeight(1f)
            rip.tooltip = "Click to invoke garbage collector"
            rip.addLeftClickListener {
                val runtime = Runtime.getRuntime()
                val oldMemory = runtime.totalMemory() - runtime.freeMemory()
                Texture2D.gc()
                System.gc()
                thread {// System.gc() is just a hint, so we wait a short moment, and then see whether sth changed
                    Thread.sleep(10)
                    val newMemory = runtime.totalMemory() - runtime.freeMemory()
                    LOGGER.info(
                        "" +
                                "Called Garbage Collector:\n" +
                                "  old:   ${oldMemory.formatFileSize()}\n" +
                                "  new:   ${newMemory.formatFileSize()}\n" +
                                "  freed: ${max(0L, oldMemory - newMemory).formatFileSize()}"
                    )
                }
            }
            right.add(rip)
            right.makeBackgroundTransparent()
            if (bottom) right.add(RemsEngine.RuntimeInfoPlaceholder())
            group.add(right)

            return group

        }

    }

}