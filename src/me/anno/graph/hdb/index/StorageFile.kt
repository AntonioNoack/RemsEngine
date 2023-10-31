package me.anno.graph.hdb.index

class StorageFile(val index: Int) {
    val folders = HashSet<Folder>()
    var size = 0
    var isDirty = false
    val files: List<File> get() = folders.map { it.files.values }.flatten()
    override fun toString(): String {
        return "StorageFile#$index { size: $size}"
    }
}