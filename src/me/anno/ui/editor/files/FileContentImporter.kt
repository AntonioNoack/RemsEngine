package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.engine.Events.addEvent
import me.anno.io.files.FileReference

open class FileContentImporter<V> {

    enum class SoftLinkMode {
        ASK,
        CREATE_LINK,
        COPY_CONTENT
    }

    open fun setName(element: V, name: String) {}

    open fun import(
        parent: V?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (V) -> Unit
    ) {
    }

    open fun createNode(parent: V?): V? = null

    fun addChildFromFile(
        parent: V?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        callback: (V) -> Unit
    ) = addChildFromFile(parent, file, useSoftLink, doSelect, 0, callback)

    fun addChildFromFile(
        parent: V?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (V) -> Unit
    ) {
        if (file.isDirectory) {
            val directory = createNode(parent) ?: return
            setName(directory, file.name)
            if (depth < DefaultConfig["import.depth.max", 3]) {
                // isn't the import order more important than speed?
                for (child in file.listChildren()) {
                    if (!child.name.startsWith('.')) {
                        addEvent {
                            addChildFromFile(directory, child, useSoftLink, doSelect, depth + 1, callback)
                        }
                    }
                }
            }
        } else import(parent, file, useSoftLink, doSelect, depth, callback)
    }
}