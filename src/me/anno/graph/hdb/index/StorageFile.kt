package me.anno.graph.hdb.index

import me.anno.graph.hdb.allocator.FileAllocation
import me.anno.utils.InternalAPI
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Ranges.overlaps

@InternalAPI
class StorageFile(val index: Int) {

    val storage = FileAllocation(ArrayList(), null, 0)
    val files get() = storage.instances

    val folders = HashSet<Folder>()

    var size: Int
        get() = storage.storageSize
        set(value) {
            storage.storageSize = value
        }

    var isDirty = false

    fun clear() {
        storage.clear()
        folders.clear()
        size = 0
        isDirty = false
    }

    fun rebuildStorage() {
        files.clear()
        for (folder in folders) {
            val childFiles = folder.files
            synchronized(childFiles) {
                childFiles.forEach { _, file -> files.add(file) }
            }
        }
        ensureStorage()
    }

    override fun toString(): String {
        return "StorageFile#$index { size: $size }"
    }

    fun ensureStorage(): FileAllocation {
        while (!storage.validate()) {
            val allFiles = ArrayList(files)
            files.removeIf { file -> allFiles.any2 { other -> other !== file && other.range.overlaps(file.range) } }
        }
        return storage
    }
}