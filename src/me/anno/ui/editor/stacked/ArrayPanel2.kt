package me.anno.ui.editor.stacked

import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Clipboard.getClipboardContent
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.ITreeViewEntryPanel
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.utils.structures.Collections.setContains
import org.apache.logging.log4j.LogManager
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WIP, don't use!
 *
 * when there is a lot of values, unfold them like in Chrome Dev Console:
 * 100 at a time, every power of 10 is grouped
 *
 * todo fix everything wrong with this class...
 * */
abstract class ArrayPanel2<EntryType, PanelType : Panel>(
    title: String, // todo add title
    val visibilityKey: String,
    val newValue: () -> EntryType, style: Style
) : TreeView<IntRange>(
    object : FileContentImporter<IntRange>() {},
    true,
    style
) {

    companion object {
        private val LOGGER = LogManager.getLogger(ArrayPanel2::class)
    }

    val values = ArrayList<EntryType>()

    // todo drag
    // todo move around (like scene views)

    abstract fun createPanel(value: EntryType): PanelType
    abstract fun updatePanel(value: EntryType, panel: PanelType)
    open fun onChange() {}

    var buttonWidth = 10
    var buttonPadding = 2

    init {
        padding.left += buttonWidth
    }

    override val canDrawOverBorders get() = true

    val buttonX get() = x + (padding.left - buttonWidth) / 2 + buttonPadding
    val buttonW get() = buttonWidth - 2 * buttonPadding

    fun getButtonIndex(x: Int, y: Int): Int {
        if (x - buttonX in 0 until buttonW) {
            var lastY = this.y
            for (index in 1 until list.children.size) { // skip first = title
                val child = list.children[index]
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

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
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

    override fun selectElements(elements: List<IntRange>) {
        // ...
    }

    override fun openAddMenu(parent: IntRange) {
        LOGGER.warn("Add menu not really supported")
    }

    override fun listRoots(): List<IntRange> {
        return listOf(0 until Int.MAX_VALUE)
    }

    @Range(2.0, 4e9)
    var base = 10

    override fun getChildren(element: IntRange): List<IntRange> {
        val last = min(element.last, values.lastIndex)
        val size = last + 1 - element.first
        return if (size > base * base) {
            val base = base.toDouble()
            val step = base.pow(floor(log(size - 1.0, base))).toInt()
            val ret = ((element.first..last) step step).map {
                it until min(last + 1, it + step)
            }
            return ret
        } else if (size > 1) {
            // list all children
            (element.first..last).map { it..it }
        } else emptyList()
    }

    fun isSingle(element: IntRange): Boolean {
        return element.first == element.last
    }

    class EditPanel(style: Style) : PanelListX(style), ITreeViewEntryPanel {
        override fun setEntrySymbol(symbol: String) {}
        override fun setEntryName(name: String) {}
        override fun setEntryTooltip(ttt: String) {}
    }

    override fun getOrCreateChildPanel(index: Int, element: IntRange): ITreeViewEntryPanel {

        // todo why is this called every single frame?

        if (!isSingle(element) && (index >= list.children.size || list.children[index] is TreeViewEntryPanel<*>)) {
            return super.getOrCreateChildPanel(index, element)
        }

        val panel = if (isSingle(element)) {
            val editPanel = list.children.getOrNull(index) as? EditPanel
            val title = "${element.first}: "
            if (editPanel != null) {
                (editPanel.children[0] as TextPanel).text = title
                @Suppress("UNCHECKED_CAST")
                updatePanel(values[element.first], editPanel.children[1] as PanelType)
                editPanel
            } else {
                EditPanel(style).apply {
                    add(TextPanel(title, style))
                    add(createPanel(values[element.first]))
                }
            }
        } else {
            list.children.getOrNull(index) as? TreeViewEntryPanel<*>
                ?: createChildPanel(index)
        }

        if (index < list.children.size) {
            list.children[index].parent = null
            list.children[index] = panel
            panel.parent = list
        } else list.add(panel)

        return panel
    }

    override fun canBeRemoved(element: IntRange): Boolean {
        return true
    }

    override fun canBeInserted(parent: IntRange, element: IntRange, index: Int): Boolean {
        return true
    }

    override fun isValidElement(element: Any?): Boolean {
        return element is IntRange
    }

    override fun getDragType(element: IntRange): String = "IndexRange"

    override fun stringifyForCopy(element: IntRange): String {
        return values.subList(element.first, element.last + 1).toString()
    }

    val notCollapsed = HashSet<IntRange>()
    override fun isCollapsed(element: IntRange): Boolean {
        return !isSingle(element) && element !in notCollapsed
    }

    override fun setCollapsed(element: IntRange, collapsed: Boolean) {
        notCollapsed.setContains(element, !collapsed)
    }

    override fun addChild(element: IntRange, child: Any, type: Char, index: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeChild(parent: IntRange, child: IntRange) {
        val element = child
        val last = min(element.last, values.lastIndex)
        values.subList(element.first, last + 1).clear()
        onChange()
        requestTreeUpdate()
    }

    override fun removeRoot(root: IntRange) {
        values.clear()
        onChange()
        requestTreeUpdate()
    }

    override fun getTooltipText(element: IntRange): String? = null
    override fun getParent(element: IntRange): IntRange? {
        if (element.first == 0 && element.last == Int.MAX_VALUE) return null
        val base1 = base.toDouble()
        val step = element.last + 1 - element.first
        val parentStep = max(base1.pow(ceil(log(step.toDouble(), base1)) + 1.0).toInt(), base * base)
        val parentFloor = element.first / parentStep * parentStep
        var parentCeil = min(parentFloor + parentStep, values.size)
        if (parentFloor == 0 && parentCeil >= values.size) parentCeil = Int.MAX_VALUE
        return parentFloor until parentCeil
    }

    override fun destroy(element: IntRange) {
        // todo destroy all children at these indices...
    }

    override fun getName(element: IntRange): String {
        return when {
            isSingle(element) -> element.first.toString()
            element.isEmpty() -> "[ empty ]"
            else -> "[${element.first} .. ${min(element.last, values.lastIndex)}]"
        }
    }

    override fun setName(element: IntRange, name: String) {
        LOGGER.warn("Setting name is not supported")
    }

    fun setValues(values: List<EntryType>) {
        this.values.clear()
        this.values.addAll(values)
        requestTreeUpdate()
    }

    fun set(panel: Panel, value: Any?) {
        val index = list.children.indexOfFirst { panel in it.listOfAll }
        val index1 = elementByIndex[index]
        @Suppress("unchecked_cast")
        values[index1.first] = value as EntryType
        onChange()
    }

    fun getValues(): List<EntryType> = values

    fun copy(index: Int) {
        // hopefully good enough...
        val panel = list.children[index]
        val window = GFX.someWindow
        Input.copy(window, panel.getPanelAt((panel.lx0 + panel.lx1) / 2, (panel.ly0 + panel.ly1) / 2) ?: panel)
    }

    fun remove(index: Int) {
        values.removeAt(index - 1)
        list.children.removeAt(index)
        onChange()
        requestTreeUpdate()
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
        onChange()
        requestTreeUpdate()
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        val index = getButtonIndex(x.toInt(), y.toInt())
        if (index >= 0 && button == Key.BUTTON_RIGHT) {
            // item specific actions
            openMenu(
                windowStack, listOf(
                    MenuOption(NameDesc("Insert Before")) {
                        insert(index, newValue())
                    },
                    MenuOption(NameDesc("Insert After")) {
                        insert(index + 1, newValue())
                    },
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
                    },
                    MenuOption(NameDesc("Remove")) {
                        remove(index)
                    },
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
                    },
                )
            )
        } else if (button == Key.BUTTON_RIGHT) {
            // todo when everything is working, use actions instead
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("Add Entry")) {
                    val value = newValue()
                    values.add(value)
                    onChange()
                    requestTreeUpdate()
                },
                MenuOption(NameDesc("Clear")) {
                    values.clear()
                    onChange()
                    requestTreeUpdate()
                }
            ))
        } else super.onMouseClicked(x, y, button, long)
    }
}