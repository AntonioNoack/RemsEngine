package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.studio.StudioBase
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager

// todo select multiple elements, filter for common properties, and apply them all together :)

// todo search elements
// todo search with tags

abstract class TreeView<V>(
    val sources: List<V>,
    private val fileContentImporter: FileContentImporter<V>,
    private val showSymbols: Boolean,
    style: Style
) : ScrollPanelXY(Padding(5), style.getChild("treeView")) {

    val list = child as PanelList
    val sample get() = list.children.first() as TreeViewPanel<*>

    init {
        padding.top = 16
    }

    private val elementByIndex = ArrayList<V>()

    var inset = style.getSize("fontSize", 12) / 3
    var collapsedSymbol = DefaultConfig["ui.symbol.collapsed", "\uD83D\uDDBF"]

    var needsTreeUpdate = true

    // Selection.select(element, null)
    abstract fun selectElement(element: V?)

    // zoomToObject
    abstract fun focusOnElement(element: V)

    abstract fun openAddMenu(parent: V)

    abstract fun getChildren(element: V): List<V>

    abstract fun isCollapsed(element: V): Boolean

    abstract fun setCollapsed(element: V, collapsed: Boolean)

    abstract fun addChild(element: V, child: Any, index: Int)

    abstract fun removeChild(parent: V, child: V)

    abstract fun getSymbol(element: V): String

    abstract fun getTooltipText(element: V): String?

    abstract fun getParent(element: V): V?

    abstract fun destroy(element: V)

    // element.name.ifBlank { element.defaultDisplayName })
    abstract fun getName(element: V): String

    abstract fun setName(element: V, name: String)

    open fun addBefore(self: V, sibling: V) {
        val parent = getParent(self)!!
        addChild(parent, self!!, getIndexInParent(parent, sibling))
    }

    open fun addAfter(self: V, sibling: V) {
        val parent = getParent(self)!!
        addChild(parent, self!!, getIndexInParent(parent, sibling) + 1)
    }

    abstract fun getIndexInParent(parent: V, child: V): Int

    abstract fun stringifyForCopy(element: V): String

    // todo use these functions to show indicator colors
    // todo use these functions to actually forbid the action
    abstract fun canBeRemoved(element: V): Boolean
    abstract fun canBeInserted(parent: V, element: V, index: Int): Boolean

    abstract fun getDragType(element: V): String

    open fun selectElementMaybe(element: V?) {
        selectElement(element)
    }

    open fun getLocalColor(element: V, isHovered: Boolean, isInFocus: Boolean): Int {
        return -1
    }

    override fun onPropertiesChanged() {
        invalidateLayout()
    }

    private fun addToTreeList(element: V, depth: Int, index0: Int): Int {
        var index = index0
        val panel = getOrCreateChildPanel(index++, element)
        val isCollapsed = isCollapsed(element)
        //(panel.parent!!.children[0] as SpacePanel).minW = inset * depth + panel.padding.right
        val symbol = if (isCollapsed) collapsedSymbol else getSymbol(element)
        panel.setText(symbol.trim(), getName(element))
        val padding = panel.padding
        val left = inset * depth + padding.right
        if (padding.left != left) {
            padding.left = left
            invalidateLayout()
        }
        if (!isCollapsed) {
            val children = getChildren(element)
            for (i in children.indices) {
                val child = children[i]
                if (getParent(child) != element) {
                    LOGGER.warn("${className}.getParent($child) is incorrect, $element")
                }
                index = addToTreeList(child, depth + 1, index)
            }
        }// todo else show that it's collapsed, if there is no symbol
        // invalidateLayout()
        return index
    }

    // if the size of the tree is large, this can use up
    // quite a lot of time -> only update when necessary
    private fun updateTree() {
        try {
            var index = 0
            val sources = sources
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
            lastWarning = e.message ?: lastWarning
        }
    }

    override fun invalidateLayout() {
        super.invalidateLayout()
        needsTreeUpdate = true
    }

    override fun onUpdate() {
        super.onUpdate()
        if (needsTreeUpdate) {
            updateTree()
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        updateTree()
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isRight && sources.isNotEmpty()) {
            // correct? maybe 😄
            openAddMenu(sources.last())
        } else super.onMouseClicked(x, y, button, long)
    }

    private fun getOrCreateChildPanel(index: Int, element: V): TreeViewPanel<*> {
        if (index < list.children.size) {
            elementByIndex[index] = element
            val panel = list.children[index] as TreeViewPanel<*>
            panel.isVisible = true
            return panel
        }
        elementByIndex += element
        val child = TreeViewPanel(
            { elementByIndex[index] }, ::isValidElement, ::toggleCollapsed, ::moveChange,
            ::getName, ::setName, this::openAddMenu,
            fileContentImporter, showSymbols, this, style
        )
        child.padding.left = 4
        // todo checkbox with custom icons
        list += child
        return child
    }

    open fun moveChange(run: () -> Unit) {
        run()
    }

    abstract fun isValidElement(element: Any?): Boolean

    abstract fun toggleCollapsed(element: V)

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "Delete" -> {
                moveChange {
                    var ctr = 0
                    val studioBase = StudioBase.instance!!
                    for (child in list.children) {
                        if (child is TreeViewPanel<*>) {
                            @Suppress("unchecked_cast")
                            val element = child.getElement() as V
                            val parent = getParent(element)
                            if (parent != null && studioBase.isSelected(element)) {
                                if (!(element is PrefabSaveable && element.root.prefab?.isWritable == false)) {
                                    removeChild(parent, element)
                                    ctr++
                                } else {
                                    LOGGER.warn("Cannot remove element, because prefab is not writable!")
                                }
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
                sources.lastOrNull(),
                file,
                FileContentImporter.SoftLinkMode.ASK,
                true
            ) {}
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(TreeView::class)
    }

}