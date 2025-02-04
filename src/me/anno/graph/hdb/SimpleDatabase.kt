package me.anno.graph.hdb

import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.async.UnitCallback

/**
 * non-hierarchical database, where keys are hashes,
 * and values are ByteArrays
 *
 * intended for many small files, which shouldn't clobber the disk
 * */
class SimpleDatabase(
    name: String,
    storage: FileReference,
    targetFileSize: Int,
    cacheTimeoutMillis: Long,
    deletionTimeMillis: Long,
    dataExtension: String = "bin", // idk why you would want to change it
) {
    val database = HierarchicalDatabase(
        name, storage, targetFileSize,
        cacheTimeoutMillis, deletionTimeMillis, dataExtension
    )

    private fun getPath(hash0: Long): List<String> {
        return listOf(hash0.toString())
    }

    private fun getPath(hash0: Long, hash1: Long): List<String> {
        return listOf(hash0.toString(), hash1.toString())
    }

    fun get(hash0: Long, callback: Callback<ByteSlice>) {
        return database.get(getPath(hash0), hash0, callback)
    }

    fun put(hash0: Long, value: ByteSlice, callback: UnitCallback? = null) {
        database.put(getPath(hash0), hash0, value, callback)
    }

    fun get(hash0: Long, hash1: Long, callback: Callback<ByteSlice>) {
        return database.get(getPath(hash0, hash1), hash1, callback)
    }

    fun put(hash0: Long, hash1: Long, value: ByteSlice, callback: UnitCallback? = null) {
        database.put(getPath(hash0, hash1), hash1, value, callback)
    }

    fun clear() {
        database.clear()
    }
}