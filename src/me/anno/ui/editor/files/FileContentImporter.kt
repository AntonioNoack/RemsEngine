package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.io.FileReference
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.structures.Hierarchical
import java.io.File

abstract class FileContentImporter<V: Hierarchical<V>> {

    enum class SoftLinkMode {
        ASK,
        CREATE_LINK,
        COPY_CONTENT
    }

    abstract fun import(
        parent: V?,
        file: File,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (V) -> Unit
    )

    abstract fun createNode(parent: V?): V

    fun addChildFromFile(
        parent: V?,
        file: File,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        callback: (V) -> Unit
    ) = addChildFromFile(parent, file, useSoftLink, doSelect, 0, callback)

    fun addChildFromFile(
        parent: V?,
        file: FileReference,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        callback: (V) -> Unit
    ) = addChildFromFile(parent, file.file, useSoftLink, doSelect, 0, callback)

    fun addChildFromFile(
        parent: V?,
        file: File,
        useSoftLink: SoftLinkMode,
        doSelect: Boolean,
        depth: Int,
        callback: (V) -> Unit
    ) {
        if (file.isDirectory) {
            val directory = createNode(parent)
            directory.name = file.name
            if (depth < DefaultConfig["import.depth.max", 3]) {
                // isn't the import order more important than speed?
                val fileList = file.listFiles()
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