package me.anno.ui.debug

import me.anno.engine.WindowRenderFlags
import me.anno.engine.Events.addEvent
import me.anno.engine.RemsEngine
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.console.ConsoleLogFullscreen
import me.anno.ui.debug.console.ConsoleOutputLine
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.Logging.lastConsoleLines
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.max

/**
 * displays all recent logging messages, and opens an overview on double click
 * */
open class ConsoleOutputPanel(style: Style) : SimpleTextPanel(style) {

    val textColor0 = textColor

    override fun onUpdate() {
        val text = lastConsoleLines.lastOrNull() ?: ""
        this.text = text
        tooltip = text.ifBlank2("Double-click to open history")
        textColor = getTextColor(text).withAlpha(127)
        super.onUpdate()
    }

    fun getTextColor(msg: String): Int {
        return when {
            msg.startsWith("[INF") -> textColor0
            msg.startsWith("[WAR") -> 0xffff00
            msg.startsWith("[ERR") -> 0xff0000
            msg.startsWith("[DEB") || msg.startsWith("[FIN") -> 0x77ff77
            else -> textColor0
        } or black
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        if (button == Key.BUTTON_LEFT) {
            // open console in large with scrollbar
            val wrapper = PanelListY(style)
            wrapper += TextButton(NameDesc("Close", "", "ui.general.close"), false, style)
                .addLeftClickListener(Menu::close)
            val listPanel = ConsoleLogFullscreen(style)
            wrapper.add(listPanel.fill(1f))
            // todo update, if there are new messages incoming
            // done select the text color based on the type of message
            val list = listPanel.content as PanelList
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
            windowStack.push(Window(wrapper, isTransparent = false, windowStack))
        }
    }

    override fun clone(): ConsoleOutputPanel {
        val clone = ConsoleOutputPanel(style)
        copyInto(clone)
        return clone
    }

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

        @JvmStatic
        fun createConsoleWithStats(bottom: Boolean = true, style: Style): Panel {

            val group = PanelStack(style)
            val console = createConsole(style)
            group += console

            val info = RuntimeInfoPanel(style)
            info.alignmentX = AxisAlignment.MAX
            info.tooltip = "Click to invoke garbage collector"
            fun runGC() {
                val runtime = Runtime.getRuntime()
                val oldMemory = runtime.totalMemory() - runtime.freeMemory()
                Pools.gc()
                System.gc()
                addEvent(16) {// System.gc() is just a hint, so we wait a short moment, and then see whether sth changed
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
            info.addLeftClickListener {
                runGC()
            }
            info.addRightClickListener {
                Menu.openMenu(
                    it.windowStack, listOf(
                        MenuOption(NameDesc("Edit Config")) {
                            RemsEngine.openConfigWindow(it.windowStack)
                        },
                        MenuOption(NameDesc("Edit Style")) {
                            RemsEngine.openStylingWindow(it.windowStack)
                        },
                        MenuOption(NameDesc("Edit Keymap")) {
                            RemsEngine.openKeymapWindow(it.windowStack)
                        },
                        MenuOption(NameDesc("Toggle VSync"), WindowRenderFlags::toggleVsync),
                        MenuOption(NameDesc("Run GC"), ::runGC)
                    )
                )
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
                right.add(FPSPanelSpacer(style))
                group.add(right)
            } else {
                group.add(info)
            }

            return group
        }
    }
}