package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.waitFor

object IsDirectoryCache : CacheSection("FileHasChildren") {

    private val TIMEOUT_MILLIS = 3600_000L // pretty much never

    fun isDirectory(file: FileReference, async: Boolean, callback: Callback<Boolean>) {
        getFileEntryAsync(
            file, true, TIMEOUT_MILLIS,
            async, generator, callback.waitFor()
        )
    }

    fun isDirectory(file: FileReference, async: Boolean): Boolean? {
        return getFileEntry(
            file, true, TIMEOUT_MILLIS,
            async, generator
        )?.value
    }

    private val generator = { key: FileKey ->
        val result = AsyncCacheData<Boolean>()
        result.value = key.file.isDirectory
        result
    }
}