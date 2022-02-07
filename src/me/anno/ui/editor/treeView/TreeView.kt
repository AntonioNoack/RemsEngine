package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.style.Style

// todo select multiple elements, filter for common properties, and apply them all together :)

// todo search elements
// todo search with tags

abstract class TreeView<V>(
    val sources: List<V>,
    val fileContentImporter: FileContentImporter<V>,
    val showSymbols: Boolean,
    style: Style
) : ScrollPanelXY(Padding(5), style.getChild("treeView")) {

    val list = content as PanelList
    val sample get() = list.children.first() as TreeViewPanel<*>

    init {
        padding.top = 16
    }

    val elementByIndex = ArrayList<V>()

    var inset = style.getSize("fontSize", 12) / 3
    var collapsedSymbol = DefaultConfig["ui.symbol.collapsed", "\uD83D\uDDBF"]

    var needsTreeUpdate = true
    var focused: Panel? = null

    // Selection.select(element, null)
    abstract fun selectElement(element: V?)

    // zoomToObject
    abstract fun focusOnElement(element: V)

    abstract fun openAddMenu(parent: V)

    abstract fun getChildren(element: V): List<V>

    abstract fun isCollapsed(element: V): Boolean

    abstract fun setCollapsed(element: V, collapsed: Boolean)

    abstract fun addChild(element: V, child: Any)

    abstract fun removeChild(element: V, child: V)

    abstract fun getSymbol(element: V): String

    abstract fun getTooltipText(element: V): String?

    abstract fun getParent(element: V): V?

    abstract fun destroy(element: V)

    // element.name.ifBlank { element.defaultDisplayName })
    abstract fun getName(element: V): String

    abstract fun setName(element: V, name: String)

    // val index = parentChildren.indexOf(self)
    // parentChildren.add(index, child)
    // child.parent = p
    abstract fun addBefore(self: V, sibling: Any)

    // val index = parentChildren.indexOf(self)
    // parentChildren.add(index + 1, child)
    // child.parent = p
    abstract fun addAfter(self: V, sibling: Any)

    abstract fun stringifyForCopy(element: V): String

    // todo define these functions
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
                index = addToTreeList(child, depth + 1, index)
            }
        }// todo else show that it's collapsed, if there is no symbol
        // invalidateLayout()
        return index
    }

    // if the size of the tree is large, this can use up
    // quite a lot of time -> only update when necessary
    private fun updateTree() {
        needsTreeUpdate = false
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
            child.visibility = Visibility.GONE
        }
    }

    override fun invalidateLayout() {
        super.invalidateLayout()
        needsTreeUpdate = true
    }

    override fun tickUpdate() {
        super.tickUpdate()
        if (needsTreeUpdate) {
            updateTree()
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        updateTree()
        super.onDraw(x0, y0, x1, y1)
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isRight) {
            // correct? maybe 😄
            openAddMenu(sources.last())
        } else super.onMouseClicked(x, y, button, long)
    }

    private fun getOrCreateChildPanel(index: Int, element: V): TreeViewPanel<*> {
        if (index < list.children.size) {
            elementByIndex[index] = element
            val panel = list.children[index] as TreeViewPanel<*>
            panel.visibility = Visibility.VISIBLE
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

}