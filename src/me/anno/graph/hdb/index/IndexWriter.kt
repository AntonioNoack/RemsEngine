package me.anno.graph.hdb.index

import me.anno.io.json.generic.JsonWriter
import me.anno.utils.InternalAPI
import me.anno.utils.types.Ranges.size
import java.io.Writer

@InternalAPI
class IndexWriter(stream: Writer) : JsonWriter(stream) {

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
            val files = folder.files
            synchronized(files) {
                files.forEach { hash, file ->
                    attr(hash.toString())
                    writeFile(file)
                }
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