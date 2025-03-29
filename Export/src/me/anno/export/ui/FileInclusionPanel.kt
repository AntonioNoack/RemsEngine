package me.anno.export.ui

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.ui.Style
import me.anno.ui.UIColors.greenYellow
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.TreeView
import me.anno.utils.Color.white

class FileInclusionPanel(val roots: List<FileReference>, style: Style) :
    TreeView<FileReference>(
        FileContentImporter(),
        true, style
    ) {

    enum class FileState {
        INCLUDED,
        CLOSED,
        OPEN;

        val next by lazy {
            entries[(ordinal + 1) % entries.size]
        }
    }

    val states = HashMap<FileReference, FileState>()
    val includedFiles
        get() = states.entries
            .filter { it.value == FileState.INCLUDED }
            .map { it.key }

    init {
        for (root in roots) {
            states[root] = FileState.OPEN
        }
    }

    fun getState(element: FileReference): FileState {
        return states.getOrDefault(element, FileState.CLOSED)
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
            getState(element) == FileState.INCLUDED || isPartiallyIncluded(element) -> "✔"
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

    private fun isPartiallyIncluded(element: FileReference): Boolean {
        return states.any { (maybeChild, childState) ->
            childState == FileState.INCLUDED && maybeChild.isSubFolderOf(element)
        }
    }

    override fun getLocalColor(element: FileReference, isHovered: Boolean, isInFocus: Boolean): Int {
        // change color based on is-included
        // mixed states: [excluded, partially, included]
        return when (getState(element)) {
            FileState.INCLUDED -> greenYellow
            else -> white
        }
    }

    override fun isCollapsed(element: FileReference): Boolean {
        return getState(element) != FileState.OPEN
    }

    override fun toggleCollapsed(element: FileReference) {
        states[element] = getState(element).next
    }

    override fun setCollapsed(element: FileReference, collapsed: Boolean) {
        // todo implement?
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