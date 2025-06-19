package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.FileCacheSection.getFileEntryAsync
import me.anno.utils.async.Callback

object IsDirectoryCache : CacheSection<FileKey, Boolean>("FileHasChildren") {

    private val TIMEOUT_MILLIS = 3600_000L // pretty much never

    fun isDirectory(file: FileReference, async: Boolean, callback: Callback<Boolean>) {
        getFileEntryAsync(
            file, true, TIMEOUT_MILLIS,
            async, generator, callback
        )
    }

    fun isDirectory(file: FileReference, async: Boolean): Boolean? {
        return getFileEntry(
            file, true, TIMEOUT_MILLIS,
            async, generator
        ).value
    }

    private val generator = { key: FileKey, result: AsyncCacheData<Boolean> ->
        result.value = key.file.isDirectory
    }
}