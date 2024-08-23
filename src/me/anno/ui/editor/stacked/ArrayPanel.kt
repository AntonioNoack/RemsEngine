package me.anno.ui.editor.stacked

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Clipboard.getClipboardContent
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.InputPanel
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

abstract class ArrayPanel<EntryType, PanelType : Panel>(
    nameDesc: NameDesc,
    visibilityKey: String,
    val newValue: () -> EntryType, style: Style
) : TitledListY(nameDesc, visibilityKey, style), InputPanel<List<EntryType>> {

    companion object {
        private val LOGGER = LogManager.getLogger(ArrayPanel::class)
    }

    // todo when there is a lot of values, unfold them like in Chrome Dev Console:
    //  100 at a time, every power of 10 is grouped

    val values = ArrayList<EntryType>()

    // todo drag
    // todo move around (like scene views)

    abstract fun createPanel(value: EntryType): PanelType

    var buttonWidth = 10
    var buttonPadding = 2

    init {
        padding.left += buttonWidth
    }

    override val canDrawOverBorders get() = true
    override val value: List<EntryType>
        get() = values

    override var isInputAllowed = true

    val buttonX get() = x + (padding.left - buttonWidth) / 2 + buttonPadding
    val buttonW get() = buttonWidth - 2 * buttonPadding

    fun getButtonIndex(x: Int, y: Int): Int {
        if (x - buttonX in 0 until buttonW) {
            var lastY = this.y
            for (index in 1 until children.size) { // skip first = title
                val child = children[index]
                // draw button left to it
                val y2 = max(child.y + buttonPadding, lastY)
                val y3 = child.y + child.height - buttonPadding
                if (y3 > y2 && y in y2 until y3) {
                    return index
                }
                if (y < y3) break
                lastY = y2 + buttonPadding
            }
        }
        return -1
    }

    override fun getCursor(): Cursor? {
        return if (getButtonIndex(x, y) < 0) null
        else Cursor.hand
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // draw buttons for add/remove/drag
        val children = children
        val buttonX = buttonX
        val buttonW = buttonW
        var lastY = this.y
        for (index in 1 until children.size) { // skip first = title
            val child = children[index]
            // draw button left to it
            // todo nice button border & change appearance on hover/click...
            val y2 = max(child.y + buttonPadding, lastY)
            val y3 = child.y + child.height - buttonPadding
            if (y3 > y2) {
                drawRect(buttonX, y2, buttonW, y3 - y2, -1)
            }
            lastY = y2 + buttonPadding
        }
    }

    override fun clear() {
        super.clear()
        values.clear()
    }

    fun setValues(values: List<EntryType>) {
        clear()
        for (i in 0 until min(values.size, 100)) {
            add(createPanel(values[i]))
        }
        if (values.size > 100) {
            add(TextPanel("...", style).apply {
                tooltip = "More elements are not yet supported"
            })
        }
        this.values.addAll(values)
    }

    override fun setValue(newValue: List<EntryType>, mask: Int, notify: Boolean): ArrayPanel<EntryType, PanelType> {
        setValues(newValue)
        if (notify) onChange()
        return this
    }

    fun set(panel: Panel, value: Any?) {
        var index = children.indexOf(panel) - 1
        if (index !in values.indices) {
            LOGGER.warn("Invalid index!!", index)
            index = 0
        }
        @Suppress("unchecked_cast")
        values[index] = value as EntryType
        onChange()
    }

    fun getValues(): List<EntryType> = values

    open fun onChange() {}

    fun copy(index: Int) {
        // hopefully good enough...
        val panel = children[index]
        val window = GFX.someWindow
        Input.copy(window, panel.getPanelAt((panel.lx0 + panel.lx1) / 2, (panel.ly0 + panel.ly1) / 2) ?: panel)
    }

    fun remove(index: Int) {
        values.removeAt(index - 1)
        children.removeAt(index)
        onChange()
        invalidateLayout()
    }

    fun paste(index: Int) {
        // todo parse, then insert
        // todo when working, add paste options into general + item options
        val clipboard = getClipboardContent()
        val parsed = TODO()
        insert(index, parsed)
    }

    fun insert(index: Int, value: EntryType) {
        values.add(index - 1, value)
        addChild(index, createPanel(value))
        onChange()
        invalidateLayout()
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        val index = getButtonIndex(x.toInt(), y.toInt())
        if (index >= 0 && button == Key.BUTTON_RIGHT) {
            // item specific actions
            openMenu(
                windowStack, listOf(
                    MenuOption(NameDesc("Insert Before")) {
                        insert(index, newValue())
                    }.setEnabled(isInputAllowed, "Readonly"),
                    MenuOption(NameDesc("Insert After")) {
                        insert(index + 1, newValue())
                    }.setEnabled(isInputAllowed, "Readonly"),
                    /*MenuOption(NameDesc("Paste Before")) {
                        paste(index)
                    },
                    MenuOption(NameDesc("Paste After")) {
                        paste(index + 1)
                    },*/
                    MenuOption(NameDesc("Copy")) {
                        copy(index)
                    },
                    MenuOption(NameDesc("Cut")) {
                        copy(index)
                        remove(index)
                    }.setEnabled(isInputAllowed, "Readonly"),
                    MenuOption(NameDesc("Remove")) {
                        remove(index)
                    }.setEnabled(isInputAllowed, "Readonly"),
                    MenuOption(NameDesc("Duplicate")) {
                        val clone: Any? = when (val value = values[index - 1]) {
                            is String, is Prefab, is FileReference -> value
                            is PrefabSaveable -> value.clone()
                            is Saveable -> JsonStringReader.read(
                                JsonStringWriter.toText(value, InvalidRef),
                                InvalidRef,
                                true
                            )
                            else -> value // may be incorrect
                        }
                        @Suppress("unchecked_cast")
                        insert(index + 1, clone as EntryType)
                    }.setEnabled(isInputAllowed, "Readonly"),
                )
            )
        } else if (button == Key.BUTTON_RIGHT) {
            // todo when everything is working, use actions instead
            openMenu(
                windowStack, listOf(
                    MenuOption(NameDesc("Add Entry")) {
                        val value = newValue()
                        addChild(createPanel(value))
                        values.add(value)
                        onChange()
                        invalidateLayout()
                    }.setEnabled(isInputAllowed, "Readonly"),
                    MenuOption(NameDesc("Clear")) {
                        values.clear()
                        clear()
                        onChange()
                        invalidateLayout()
                    }.setEnabled(isInputAllowed, "Readonly")
                )
            )
        } else super.onMouseClicked(x, y, button, long)
    }
}