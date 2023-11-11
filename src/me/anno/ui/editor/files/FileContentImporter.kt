package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.studio.Events.addEvent

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
                val fileList = file.listChildren()
                if (fileList != null) {
                    for (it in fileList) {
                        if (!it.name.startsWith('.')) {
                            addEvent {
                                addChildFromFile(directory, it, useSoftLink, doSelect, depth + 1, callback)
                            }
                        }
                    }
                }
                /*threadWithName("ImportFromFile") {
                    file.listFiles()?.filter { !it.name.startsWith(".") }?.forEach {
                        addEvent {
                            addChildFromFile(directory, it, useSoftLink, doSelect, depth + 1, callback)
                        }
                    }
                }*/
            }
        } else import(parent, file, useSoftLink, doSelect, depth, callback)
    }

}