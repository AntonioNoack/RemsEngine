package me.anno.graph.hdb.index

import me.anno.utils.InternalAPI

@InternalAPI
class StorageFile(val index: Int) {

    val folders = HashSet<Folder>()
    var size = 0
    var isDirty = false

    val files: ArrayList<File>
        get() = ArrayList(
            folders.map { it.files.values }
                .flatten()
                .sortedBy { it.range.first }
        )

    override fun toString(): String {
        return "StorageFile#$index { size: $size}"
    }
}