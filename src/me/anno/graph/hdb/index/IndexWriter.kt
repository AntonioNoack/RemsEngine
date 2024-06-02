package me.anno.graph.hdb.index

import me.anno.utils.types.size
import me.anno.io.json.generic.JsonWriter
import me.anno.utils.InternalAPI
import java.io.OutputStream

@InternalAPI
class IndexWriter(stream: OutputStream) : JsonWriter(stream) {

    fun writeFolder(folder: Folder) {
        // todo why is this crashing????
        writeObject {
            attr("n")
            write(folder.name)
            attr("c")
            writeArray(folder.children.values.toList(), ::writeFolder)
            val sf = folder.storageFile
            if (sf != null) {
                writeStorageFile(sf, folder)
            }
        }
    }

    private fun writeStorageFile(sf: StorageFile, folder: Folder) {
        attr("i")
        write(sf.index)
        attr("f")
        writeObject {
            for ((hash, file) in folder.files) {
                attr(hash.toString())
                writeFile(file)
            }
        }
    }

    private fun writeFile(file: File) {
        writeObject {
            attr("a")
            write(file.lastAccessedMillis)
            attr("s")
            write(file.range.first)
            attr("l")
            write(file.range.size)
        }
    }
}