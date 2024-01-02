package me.anno.ui.debug

import me.anno.gpu.texture.Texture2D
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.Events.addEvent
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.debug.console.ConsoleOutputLine
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Logging.lastConsoleLines
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.max

/**
 * displays all recent logging messages, and opens an overview on double click
 * */
open class ConsoleOutputPanel(style: Style) : SimpleTextPanel(style) {

    override fun onUpdate() {
        val text = lastConsoleLines.lastOrNull() ?: ""
        this.text = text
        tooltip = text.ifBlank2("Double-click to open history")
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
        } or black
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        if (button == Key.BUTTON_LEFT) {
            // open console in large with scrollbar
            val listPanel = ConsoleLogFullscreen(style)
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.content as PanelList
            list += TextButton(Dict["Close", "ui.general.close"], false, style).addLeftClickListener {
                windowStack.pop().destroy()
            }
            val lcl = lastConsoleLines.toList()
            for (i in lcl.indices) {
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

    override val className: String get() = "ConsoleOutputPanel"

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ConsoleOutputPanel::class)

        @JvmStatic
        fun formatFilePath(file: FileReference) = formatFilePath(file.absolutePath)

        @JvmStatic
        fun formatFilePath(file: String): String {
            return "file://${file.replace(" ", "%20")}"
        }

        @JvmStatic
        fun createConsole(style: Style): ConsoleOutputPanel {
            val console = ConsoleOutputPanel(style.getChild("small"))
            // console.fontName = "Segoe UI"
            console.tooltip = "Double-click to open history"
            return console
        }

        class RuntimeInfoPlaceholder(style: Style) : Panel(style) {
            override fun calculateSize(w: Int, h: Int) {
                val window = window
                val showFPS = StudioBase.showFPS
                val ws = window?.windowStack
                minW = if (ws != null && showFPS) {
                    // todo respect height for this calculation, too: we don't need to move out the way,
                    //  if we're above (e.g. on welcome screen)
                    val gap = ws.width - (window.panel.x + window.panel.width)
                    max(FrameTimings.width - gap, 0)
                } else {
                    if (StudioBase.showFPS) FrameTimings.width else 0
                }
                minH = 1
            }
        }

        @JvmStatic
        fun createConsoleWithStats(bottom: Boolean = true, style: Style): Panel {

            val group = PanelStack(style)
            val console = createConsole(style)
            group.alignmentX = AxisAlignment.FILL
            group += console

            val info = RuntimeInfoPanel(style)
            info.alignmentX = AxisAlignment.MAX
            info.tooltip = "Click to invoke garbage collector"
            info.addLeftClickListener {
                val runtime = Runtime.getRuntime()
                val oldMemory = runtime.totalMemory() - runtime.freeMemory()
                Texture2D.gc()
                System.gc()
                addEvent {// System.gc() is just a hint, so we wait a short moment, and then see whether sth changed
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

            if (bottom) {
                // adds conditional spacing for FPS panel
                val right = object : PanelListX(style) {
                    override fun onUpdate() {
                        super.onUpdate()
                        tooltip = console.text
                    }
                }
                right.add(info)
                right.alignmentX = AxisAlignment.MAX
                right.add(RuntimeInfoPlaceholder(style))
                group.add(right)
            } else {
                group.add(info)
            }

            return group
        }
    }
}