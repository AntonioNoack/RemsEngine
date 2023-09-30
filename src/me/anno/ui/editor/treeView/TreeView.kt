package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.files.Search
import me.anno.ui.input.TextInput
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

// todo select multiple elements, filter for common properties, and apply them all together :)

abstract class TreeView<V : Any>(
    val fileContentImporter: FileContentImporter<V>,
    val showSymbols: Boolean,
    style: Style
) : ScrollPanelXY(Padding(5), style.getChild("treeView")) {

    val list = child as PanelListY
    val sample get() = list.children.getOrNull(1) as TreeViewEntryPanel<*>
    val searchPanel = TextInput("Search Term", "", false, style)

    init {
        list.add(searchPanel)
        searchPanel.addChangeListener {
            search = if (it.isBlank2()) null else Search(it)
            needsTreeUpdate = true
        }
    }

    val elementByIndex = ArrayList<V>()

    var inset = style.getSize("fontSize", 12) / 3
    var collapsedSymbol = DefaultConfig["ui.symbol.collapsed", "\uD83D\uDDBF"]

    var needsTreeUpdate = true

    abstract fun listSources(): List<V>

    // Selection.select(element, null)
    abstract fun selectElements(elements: List<V>)

    // zoomToObject
    abstract fun focusOnElement(element: V)

    abstract fun openAddMenu(parent: V)

    abstract fun getChildren(element: V): List<V>

    abstract fun isCollapsed(element: V): Boolean

    abstract fun setCollapsed(element: V, collapsed: Boolean)

    /**
     * returns true on success
     * */
    abstract fun addChild(element: V, child: Any, type: Char, index: Int): Boolean

    abstract fun removeChild(parent: V, child: V)
    abstract fun removeRoot(root: V)

    open fun getSymbol(element: V): String = if (isCollapsed(element)) "▶" else "▼"
    open fun getTooltipText(element: V): String? = null

    abstract fun getParent(element: V): V?

    abstract fun destroy(element: V)

    // element.name.ifBlank { element.defaultDisplayName })
    abstract fun getName(element: V): String

    abstract fun setName(element: V, name: String)

    open fun addBefore(sibling: V, added: V, type: Char): Boolean {
        val parent = getParent(sibling)!!
        if (getParent(added) == parent) removeChild(parent, added)
        val index = getIndexInParent(parent, sibling)
        return if (canBeInserted(parent, added, index)) {
            addChild(parent, added, type, index)
        } else {
            LOGGER.warn("Cannot add child")
            false
        }
    }

    open fun addAfter(sibling: V, added: V, type: Char): Boolean {
        val parent = getParent(sibling)!!
        if (getParent(added) == parent) {
            removeChild(parent, added)
        }
        val index = getIndexInParent(parent, sibling) + 1
        return if (canBeInserted(parent, added, index)) {
            addChild(parent, added, type, index)
        } else {
            LOGGER.warn("Cannot add child")
            false
        }
    }

    fun getIndexInParent(parent: V, child: V): Int = getChildren(parent).indexOf(child)

    abstract fun stringifyForCopy(element: V): String

    // todo use these functions to show indicator colors
    abstract fun canBeRemoved(element: V): Boolean

    // todo use this functions to actually forbid the action
    abstract fun canBeInserted(parent: V, element: V, index: Int): Boolean

    abstract fun getDragType(element: V): String

    open fun selectElementsMaybe(elements: List<V>) {
        selectElements(elements)
    }

    open fun getLocalColor(element: V, isHovered: Boolean, isInFocus: Boolean): Int {
        return -1
    }

    override fun onPropertiesChanged() {
        invalidateLayout()
    }

    open fun fulfillsSearch(element: V, name: String, ttt: String?, search: Search): Boolean {
        return if (ttt == null) search.matches(name)
        else search.matches("$name $ttt")
    }

    var search: Search? = null
    private fun addToTreeList(element: V, depth: Int, index0: Int): Int {
        var index = index0
        val name = getName(element)
        val ttt = lazy { getTooltipText(element) }

        while (index >= elementByIndex.size) elementByIndex.add(element)
        elementByIndex[index] = element

        val panel = getOrCreateChildPanel(index++, element)

        (panel as Panel).isVisible = true
        val isCollapsed = isCollapsed(element)
        val search = search
        var isIncludedInSearch = search == null || fulfillsSearch(element, name, ttt.value, search)
        if (!isCollapsed) {
            val children = getChildren(element)
            for (i in children.indices) {
                val child = children[i]
                val testParent = getParent(child)
                if (testParent != element) {
                    LOGGER.warn("${className}.getParent($child) is incorrect, $testParent != $element")
                }
                index = addToTreeList(child, depth + 1, index)
                if (index > index0 + 1) isIncludedInSearch = true
            }
        }// todo else show that it's collapsed, if there is no symbol
        return if (isIncludedInSearch) {
            panel.setEntrySymbol(getSymbol(element))
            panel.setEntryName(name)
            panel.setEntryTooltip(ttt.value)
            val padding = panel.padding
            val left = inset * depth + padding.right
            if (padding.left != left) {
                padding.left = left
                invalidateLayout()
            }
            index
        } else index0
    }

    // if the size of the tree is large, this can use up
    // quite a lot of time -> only update when necessary
    private fun updateTree() {
        try {
            var index = 1
            val sources = listSources()
            for (i in sources.indices) {
                val element = sources[i]
                index = addToTreeList(element, 0, index)
            }
            // make the rest invisible (instead of deleting them)
            val children = list.children
            for (i in index until children.size) {
                val child = children[i]
                child.isVisible = false
            }
            needsTreeUpdate = false
        } catch (e: Exception) {
            e.printStackTrace()
            lastWarning = e.message ?: lastWarning
        }
    }

    override fun invalidateLayout() {
        super.invalidateLayout()
        needsTreeUpdate = true
    }

    override fun onUpdate() {
        super.onUpdate()
        if (needsTreeUpdate) updateTree()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (needsTreeUpdate) updateTree()
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        val sources = listSources()
        if (button == Key.BUTTON_RIGHT && sources.isNotEmpty()) {
            // correct? maybe 😄
            openAddMenu(sources.last())
        } else super.onMouseClicked(x, y, button, long)
    }

    open fun getOrCreateChildPanel(index: Int, element: V): ITreeViewEntryPanel {
        return if (index < list.children.size) {
            list.children[index] as TreeViewEntryPanel<*>
        } else {
            val panel = createChildPanel(index)
            list += panel
            panel
        }
    }

    fun createChildPanel(index: Int): TreeViewEntryPanel<*> {
        val child = TreeViewEntryPanel(index, this, style)
        child.padding.left = 4
        // todo checkbox with custom icons
        return child
    }

    open fun moveChange(run: () -> Unit) {
        run()
    }

    abstract fun isValidElement(element: Any?): Boolean

    fun toggleCollapsed(element: V) {
        setCollapsed(element, !isCollapsed(element))
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Delete" -> {
                moveChange {
                    for (child in list.children) {
                        if (child is TreeViewEntryPanel<*>) {
                            @Suppress("unchecked_cast")
                            val element = child.getElement() as V
                            val parent = getParent(element)
                            if (child.isAnyChildInFocus) {
                                if (canBeRemoved(element)) {
                                    if (parent == null) removeRoot(element)
                                    else removeChild(parent, element)
                                } else LOGGER.info("Cannot remove element")
                            }
                        }
                    }
                    invalidateLayout()
                }
                true
            }
            else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
    }

    // done display, where we'd move it
    // done between vs at the end vs at the start
    // todo we'd need a selection mode with the arrow keys, too...

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        for (file in files) {
            fileContentImporter.addChildFromFile(
                listSources().lastOrNull(),
                file,
                FileContentImporter.SoftLinkMode.ASK,
                true
            ) {}
        }
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        // probably should be an action instead...
        if (key == Key.KEY_F && Input.isControlDown) {
            searchPanel.requestFocus()
        } else super.onKeyTyped(x, y, key)
    }

    override val className: String get() = "TreeView"

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(TreeView::class)
    }
}