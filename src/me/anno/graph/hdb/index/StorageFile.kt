package me.anno.graph.hdb.index

import me.anno.graph.hdb.allocator.FileAllocation.calculateSortedRanges
import me.anno.utils.InternalAPI

@InternalAPI
class StorageFile(val index: Int) {

    val sortedFiles = ArrayList<File>()
    val sortedRanges = ArrayList<IntRange>()
    val folders = HashSet<Folder>()
    var size = 0
    var isDirty = false

    fun clear() {
        sortedFiles.clear()
        sortedRanges.clear()
        folders.clear()
        size = 0
        isDirty = false
    }

    fun rebuildSortedFiles() {
        sortedFiles.clear()
        for (folder in folders) {
            sortedFiles.addAll(folder.files.values)
        }
        sortedFiles.sortBy { it.range.first }
    }

    fun rebuildSortedRanges() {
        sortedRanges.clear()
        calculateSortedRanges(sortedFiles, sortedRanges)
    }

    override fun toString(): String {
        return "StorageFile#$index { size: $size }"
    }
}