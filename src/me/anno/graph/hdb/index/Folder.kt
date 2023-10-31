package me.anno.graph.hdb.index

class Folder(var name: String) {

    val files = HashMap<Long, File>()
    val children = HashMap<String, Folder>()
    var storageFile: StorageFile? = null

    override fun toString(): String {
        return "Folder@${System.identityHashCode(this)}($name) { files: $files, children: $children, sf: ${storageFile?.index ?: -1} }"
    }
}