package me.anno.graph.hdb.index

import me.anno.graph.hdb.allocator.size
import me.anno.io.json.generic.JsonWriter
import me.anno.utils.InternalAPI
import java.io.OutputStream

@InternalAPI
class IndexWriter(stream: OutputStream) : JsonWriter(stream) {

    fun writeFolder(folder: Folder) {
        beginObject()
        attr("n")
        write(folder.name)
        attr("c")
        beginArray()
        for (child in folder.children.values) {
            writeFolder(child)
        }
        endArray()
        val sf = folder.storageFile
        if (sf != null) {
            attr("i")
            write(sf.index)
            attr("f")
            beginObject()
            for ((hash, file) in folder.files) {
                attr(hash.toString())
                writeFile(file)
            }
            endObject()
        }
        endObject()
    }

    private fun writeFile(file: File) {
        beginObject()
        attr("a")
        write(file.lastAccessedMillis)
        attr("s")
        write(file.range.first)
        attr("l")
        write(file.range.size)
        endObject()
    }
}