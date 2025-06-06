package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.waitFor

object HasChildrenCache : CacheSection("FileHasChildren") {

    private val TIMEOUT_MILLIS = 3600_000L // pretty much never

    fun hasChildren(file: FileReference, async: Boolean, callback: Callback<Boolean>) {
        getFileEntryAsync(
            file, true, TIMEOUT_MILLIS,
            async, generator, callback.waitFor()
        )
    }

    fun hasChildren(file: FileReference, async: Boolean): Boolean? {
        return getFileEntry(
            file, true, TIMEOUT_MILLIS,
            async, generator
        )?.value
    }

    private val generator = { key: FileKey ->
        val result = AsyncCacheData<Boolean>()
        key.file.listChildren(result.map(List<FileReference>::isNotEmpty))
        result
    }
}