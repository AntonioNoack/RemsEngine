package me.anno.graph.hdb.index

import me.anno.engine.ui.input.ComponentUI.toLong
import me.anno.io.json.JsonScanner
import me.anno.utils.types.Ints.toLongOrDefault
import me.anno.utils.types.Strings.toInt
import java.io.InputStream

class IndexReader(input: InputStream, val lookupStorageFile: (Int) -> StorageFile?) : JsonScanner(input) {

    fun readFolder(folder: Folder) {
        scanObject { name ->
            when (name) {
                "f" -> scanObject { key ->
                    val hash = key.toLongOrDefault(-1L)
                    folder.files[hash] = readFile()
                }
                "c" -> scanArray {
                    val child = Folder("?")
                    readFolder(child)
                    folder.children[child.name] = child
                }
                "n" -> folder.name = readString()
                "i" -> folder.storageFile = lookupStorageFile(readNumber().toInt())
                else -> skipSomething()
            }
        }
        val sf = folder.storageFile
        if (sf != null) {
            sf.size += folder.files.values.size
        }
    }

    private fun readFile(): File {
        var lastAccessed = 0L
        var start = 0
        var length = 0
        scanObject {
            when (it) {
                "a" -> lastAccessed = readNumber().toLong()
                "s" -> start = readNumber().toInt()
                "l" -> length = readNumber().toInt()
                else -> skipSomething()
            }
        }
        return File(lastAccessed, start until (start + length))
    }
}