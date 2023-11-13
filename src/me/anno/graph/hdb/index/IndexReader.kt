package me.anno.graph.hdb.index

import me.anno.engine.ui.input.ComponentUI.toLong
import me.anno.io.json.generic.JsonScanner
import me.anno.utils.types.Ints.toLongOrDefault
import me.anno.utils.types.Strings.toInt
import java.io.InputStream
import kotlin.math.max

class IndexReader(input: InputStream, val lookupStorageFile: (sfIndex: Int) -> StorageFile?) : JsonScanner(input) {

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
                "i" -> {
                    val sf = lookupStorageFile(readNumber().toInt())
                    folder.storageFile = sf
                    sf?.folders?.add(folder)
                }
                else -> skipSomething()
            }
        }
        val sf = folder.storageFile
        if (sf != null) {
            sf.size = max(sf.size, folder.files.values.maxOfOrNull { it.range.last + 1 } ?: 0)
        }
    }

    private fun readFile(): File {
        var lastAccessed = 0L
        var start = 0
        var length = 0
        scanObject { key ->
            when (key) {
                "a" -> lastAccessed = readNumber().toLong()
                "s" -> start = readNumber().toInt()
                "l" -> length = readNumber().toInt()
                else -> skipSomething()
            }
        }
        return File(lastAccessed, start until (start + length))
    }
}