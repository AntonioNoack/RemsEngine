package me.anno.graph.hdb.index

import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import speiger.primitivecollections.LongToObjectHashMap

@InternalAPI
class Folder(var name: String) {

    val files = LongToObjectHashMap<File>()
    val children = HashMap<String, Folder>()
    var storageFile: StorageFile? = null

    override fun toString(): String {
        return "Folder@${hash32(this)}($name) { files: #${files.size}, children: $children, sf: ${storageFile?.index ?: -1} }"
    }
}