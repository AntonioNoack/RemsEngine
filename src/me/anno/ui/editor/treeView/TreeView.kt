package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.language.translation.Dict
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.files.Search
import me.anno.ui.input.TextInput
import me.anno.utils.Color.white
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
    val searchPanel = TextInput(
        Dict["Search Term", "ui.general.searchTerm"], "",
        false, style.getChild("deep")
    )

    init {
        alwaysShowShadowY = true
        list.add(searchPanel)
        list.makeBackgroundTransparent()
        val padLR = 4
        searchPanel.padding.add(padLR, 0, padLR, 0)
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

    fun removeFromParent(original: V) {
        val parent = getParent(original)
        if (parent != null) removeChild(parent, original)
    }

    fun addBefore(sibling: V, added: V, type: Char): Boolean {
        return addRelative(sibling, added, type, 0)
    }

    fun addAfter(sibling: V, added: V, type: Char): Boolean {
        return addRelative(sibling, added, type, 1)
    }

    open fun paste(hovered: V, original: V?, relativeY: Float, data: String) {
        val type1 = findType(original, hovered) ?: ' '
        if (original != null && canBeMoved(hovered, original)) {
            moveChange {
                val parent = getParent(original)
                if (parent != null) removeChild(parent, original)
                insertElement(relativeY, hovered, original, type1)
            }
        } else {
            // if not, create a copy
            @Suppress("unchecked_cast")
            val clone = JsonStringReader.read(data, EngineBase.workspace, true)
                .firstOrNull() as? V ?: return
            moveChange {
                insertElement(relativeY, hovered, clone, type1)
            }
        }
    }

    fun findType(original: V?): Char? {
        if (original !is PrefabSaveable) return null
        val parent = getParent(original) as? PrefabSaveable ?: return null
        val types = parent.getValidTypesForChild(original)
        if (types.length == 1) return types.first()
        for (type in types) {
            if (parent.getChildListByType(type).contains(original)) {
                return type
            }
        }
        return null
    }

    fun findType(original: V?, hovered: V): Char? {
        val type0 = findType(original)
        if (type0 != null) return type0
        if (original !is PrefabSaveable) return null
        val parent = hovered as? PrefabSaveable ?: return null
        val types = parent.getValidTypesForChild(original)
        return types.firstOrNull()
    }

    fun canBeMoved(hovered: V, original: V): Boolean {
        var canBeMoved = true
        var ancestor = hovered
        while (true) {
            if (original === ancestor) {
                canBeMoved = false
                break
            }
            ancestor = getParent(ancestor) ?: break
        }
        return canBeMoved
    }

    enum class InsertMode {
        BEFORE,
        AFTER,
        LAST,
    }

    fun insertElement(relativeY: Float, hovered: V, clone: V, type: Char) {
        val success = when (getInsertMode(relativeY, hovered)) {
            InsertMode.BEFORE -> addBefore(hovered, clone, type)
            InsertMode.AFTER -> addAfter(hovered, clone, type)
            InsertMode.LAST -> insertElementLast(hovered, clone, type)
        }
        if (success) selectElements(listOf(clone))
    }

    fun getInsertMode(relativeY: Float, hovered: V): InsertMode {
        return when {
            relativeY < 0.33f -> {
                // paste on top
                if (getParent(hovered) != null) {
                    InsertMode.BEFORE
                } else {
                    InsertMode.LAST
                }
            }
            relativeY < 0.67f -> {
                // paste as child
                InsertMode.LAST
            }
            else -> {
                // paste below
                if (getParent(hovered) != null) {
                    InsertMode.AFTER
                } else {
                    InsertMode.LAST
                }
            }
        }
    }

    fun insertElementLast(hovered: V, clone: V, type: Char): Boolean {
        val index = getChildren(hovered).size
        return if (canBeInserted(hovered, clone, index)) {
            addChild(hovered, clone, type, index)
        } else {
            LOGGER.warn("Cannot add child")
            false
        }
    }

    open fun addRelative(sibling: V, added: V, type: Char, delta: Int): Boolean {
        removeFromParent(added)
        val parent = getParent(sibling)!!
        val index = getIndexInParent(parent, sibling) + delta
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

    open fun getLocalColor(element: V, isHovered: Boolean, isInFocus: Boolean): Int = white

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
        // todo go down some levels for search, if not all are searched
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