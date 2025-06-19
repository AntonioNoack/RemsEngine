package me.anno.cache

import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager

object FileCacheSection {

    private val LOGGER = LogManager.getLogger(FileCacheSection::class)

    fun <V : Any> CacheSection<FileKey, V>.removeFileEntry(file: FileReference) = removeEntry(file.getFileKey())
    fun <V : Any> CacheSection<FileKey, V>.removeFileEntry(file: FileReference, lastModified: Long) =
        removeEntry(FileKey(file, lastModified))

    fun <V : Any> CacheSection<FileKey, V>.getFileEntry(
        file: FileReference, allowDirectories: Boolean, timeoutMillis: Long,
        generator: (FileKey, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> {
        val validFile = getValidFile(file, allowDirectories)
        if (validFile == null) return NullCacheData.get()
        return getEntry(validFile.getFileKey(), timeoutMillis, generator)
    }

    @Deprecated("Please get rid of all -Async functions, we don't need them")
    fun <V : Any> CacheSection<FileKey, V>.getFileEntryAsync(
        file: FileReference, allowDirectories: Boolean,
        timeoutMillis: Long, generator: (FileKey, AsyncCacheData<V>) -> Unit, callback: Callback<V>
    ) {
        getFileEntry(file, allowDirectories, timeoutMillis, generator)
            .waitFor(callback)
    }

    fun <V : Any> CacheSection<FileKey, V>.getValidFile(
        file: FileReference,
        allowDirectories: Boolean
    ): FileReference? {
        return when {
            !allowDirectories && file is InnerFolder -> {
                val alias = file.alias ?: return null
                getValidFile(alias, false)
            }
            file == InvalidRef -> null
            !file.exists -> {
                LOGGER.warn("[$name] Skipped loading $file, is missing")
                null
            }
            !allowDirectories && file.isDirectory -> {
                LOGGER.warn("[$name] Skipped loading $file, is a folder")
                null
            }
            else -> file
        }
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun <V : Any> CacheSection<FileKey, V>.getFileEntryWithoutGenerator(
        key1: FileReference, delta: Long = 1L
    ): AsyncCacheData<V>? {
        return getEntryWithoutGenerator(key1.getFileKey(), delta)
    }

    /**
     * returns whether a value is present
     * */
    fun <V : Any> CacheSection<FileKey, V>.hasFileEntry(key: FileReference, delta: Long = 1L): Boolean =
        hasEntry(key.getFileKey(), delta)

    fun <V : Any> CacheSection<FileKey, V>.overrideFileEntry(key: FileReference, newValue: V, timeoutMillis: Long) {
        override(key.getFileKey(), newValue, timeoutMillis)
    }
}