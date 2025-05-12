package me.anno.export.ui

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.UIColors.fireBrick
import me.anno.ui.UIColors.greenYellow
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.ITreeViewEntryPanel
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.lists.Lists.mod

class FileInclusionPanel(val roots: List<FileReference>, style: Style) :
    TreeView<FileReference>(
        FileContentImporter(),
        true, style
    ) {

    // todo serialize this state somehow: included + excluded files
    val fileStates = HashMap<FileReference, Boolean>()

    val stateValues = listOf(true, false, null)
    val stateLabels = listOf(
        NameDesc("✔"),
        NameDesc("❌"),
        NameDesc("➕")
    )

    val notCollapsed = HashSet<FileReference>()

    init {
        notCollapsed.addAll(roots)
    }

    fun getIncludedFiles(): Set<FileReference> {
        val dst = HashSet<FileReference>(fileStates.size)
        for ((file, isIncluded) in fileStates) {
            if (isIncluded) addIncludedFiles(file, dst)
        }
        return dst
    }

    private fun addIncludedFiles(file: FileReference, dst: HashSet<FileReference>) {
        when {
            isExcluded(file) -> return
            isFullyIncluded(file) -> dst.add(file)
            isPartiallyIncluded(file) -> {
                val children = getChildren(file)
                for (child in children) {
                    addIncludedFiles(child, dst)
                }
            }
        }
    }

    override fun getOrCreateChildPanel(index: Int, element: FileReference): ITreeViewEntryPanel {
        return if (index < list.children.size) {
            val panel = list.children[index] as TreeViewEntryPanel<*>
            updateLabel(panel.children[0] as TextPanel, index)
            panel
        } else {
            val panel = createChildPanel(index)
            val textPanel = object : TextPanel(getStateLabel(element), style) {
                override fun calculateSize(w: Int, h: Int) {
                    super.calculateSize(w, h)
                    minW = minH
                }
            }
            textPanel.textAlignmentX = AxisAlignment.CENTER
            textPanel.addLeftClickListener { button ->
                nextState(elementByIndex[index])
                updateLabel(button as TextPanel, index)
            }
            panel.add(0, textPanel)
            list += panel
            panel
        }
    }

    private fun updateLabel(button: TextPanel, index: Int) {
        val label = getStateLabel(elementByIndex[index])
        button.text = label.name
        button.tooltip = label.desc
    }

    private fun getStateLabel(file: FileReference): NameDesc {
        val oldState = fileStates[file]
        return stateLabels[stateValues.indexOf(oldState)]
    }

    private fun nextState(file: FileReference): NameDesc {
        val oldState = fileStates[file]
        val newState = stateValues.mod(stateValues.indexOf(oldState) + 1)
        if (newState == null) fileStates.remove(file)
        else fileStates[file] = newState
        return getStateLabel(file)
    }

    override fun listRoots(): List<FileReference> = roots
    override fun selectElements(elements: List<FileReference>) {}
    override fun openAddMenu(parent: FileReference) {}
    override fun getChildren(element: FileReference): List<FileReference> {
        return element.listChildren()
            .sortedBy { it.name.lowercase() }
            .sortedByDescending { it.isDirectory }
    }

    override fun getSymbol(element: FileReference): String {
        return when {
            !isCollapsed(element) -> "▶"
            element.isDirectory -> "📂"
            else -> when (getImportType(element)) {
                "Image" -> "🖼"
                "Video" -> "📽"
                "Audio" -> "🎶"
                "URL" -> "🔗"
                "Container" -> "📦"
                "Executable" -> "📚"
                "Metadata" -> "🌍"
                "Text" -> "✉"
                "Mesh" -> "🐒"
                else -> "🗄"
            }
        }
    }

    private fun getImportType(element: FileReference): String? {
        return DefaultConfig["import.mapping.${element.lcExtension}"] as? String
    }

    private fun isFullyIncluded(element: FileReference): Boolean {
        return isPartiallyIncluded(element) &&
                !hasChildrenWithState(element, false)
    }

    private fun isPartiallyIncluded(element: FileReference): Boolean {
        return !hasParentWithState(element, false) &&
                hasParentWithState(element, true)
    }

    private fun hasChildrenWithState(element: FileReference, state: Boolean): Boolean {
        return fileStates.any { (maybeChild, childState) ->
            childState == state && maybeChild.isSameOrSubFolderOf(element)
        }
    }

    private fun hasParentWithState(element: FileReference, state: Boolean): Boolean {
        return fileStates.any { (maybeParent, childState) ->
            childState == state && element.isSameOrSubFolderOf(maybeParent)
        }
    }

    private fun isExcluded(element: FileReference): Boolean {
        return hasParentWithState(element, false)
    }

    override fun getLocalColor(element: FileReference, isHovered: Boolean, isInFocus: Boolean): Int {
        // change color based on is-included
        // mixed states: [excluded, partially, included]
        return when {
            isExcluded(element) -> fireBrick
            isPartiallyIncluded(element) -> greenYellow
            isFullyIncluded(element) -> 0x76ff00 or black
            else -> white
        }
    }

    override fun isCollapsed(element: FileReference): Boolean {
        return element !in notCollapsed
    }

    override fun setCollapsed(element: FileReference, collapsed: Boolean) {
        notCollapsed.setContains(element, !collapsed)
    }

    override fun addChild(element: FileReference, child: Any, type: Char, index: Int): Boolean {
        return false
    }

    override fun removeRoot(root: FileReference) {}
    override fun removeChild(parent: FileReference, child: FileReference) {}
    override fun getParent(element: FileReference) = element.getParent()
    override fun destroy(element: FileReference) {}
    override fun getName(element: FileReference): String {
        return element.name
    }

    override fun setName(element: FileReference, name: String) {
        element.renameTo(element.getSibling(name))
    }

    override fun stringifyForCopy(element: FileReference): String {
        return element.absolutePath
    }

    override fun canBeRemoved(element: FileReference) = false
    override fun canBeInserted(parent: FileReference, element: FileReference, index: Int): Boolean = false
    override fun getDragType(element: FileReference): String = "File"
    override fun isValidElement(element: Any?): Boolean {
        return element is FileReference && element.exists
    }
}