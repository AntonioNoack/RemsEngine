package me.anno.cache

import me.anno.io.files.FileReference
import me.anno.io.files.LinkFileReference
import me.anno.utils.structures.lists.SimpleList

/**
 * Reduces cache lookups by storing the async value
 * */
class FileCacheList<V : Any>(
    files: List<FileReference>,
    val cache: (FileReference) -> AsyncCacheData<V>
) : SimpleList<FileReference>() {

    companion object {
        private val emptyList = FileCacheList(emptyList()) { AsyncCacheData.empty() }
        fun <V : Any> empty(): FileCacheList<V> {
            @Suppress("UNCHECKED_CAST")
            return emptyList as FileCacheList<V>
        }
    }

    override val size: Int get() = files.size

    private val files = files.toTypedArray()
    private var cachedValue = arrayOfNulls<AsyncCacheData<V>>(files.size)
    private var lastModified = LongArray(files.size)

    override fun get(index: Int): FileReference {
        return files[index]
    }

    fun getValue(index: Int): V? {
        return waitFor(index).value
    }

    fun waitFor(index: Int): AsyncCacheData<V> {

        val file = files[index]
        val cachedValue = cachedValue[index]
        if (cachedValue != null &&
            !cachedValue.hasBeenDestroyed &&
            file.lastModified == lastModified[index]
        ) {

            // if file was already resolved, but is a link, make it direct
            if (file is LinkFileReference) {
                files[index] = file.originalOrNull ?: file
            }

            cachedValue.update(10_000L)
            return cachedValue
        }

        val value = cache(file) // should not be synchronized to prevent deadlocks
        synchronized(this) {
            lastModified[index] = file.lastModified
            this.cachedValue[index] = value
        }
        return value
    }

    private val hash = files.fold(1) { v, file -> v * 31 + file.hashCode() }

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean {
        return other is List<*> && other.size == size &&
                (0 until size).all { this[it] == other[it] }
    }
}