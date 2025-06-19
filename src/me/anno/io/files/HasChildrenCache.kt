package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.utils.async.Callback.Companion.map

object HasChildrenCache : CacheSection<FileKey, Boolean>("FileHasChildren") {

    private val TIMEOUT_MILLIS = 3600_000L // pretty much never

    fun hasChildren(file: FileReference): AsyncCacheData<Boolean> {
        return getFileEntry(file, true, TIMEOUT_MILLIS, generator)
    }

    private val generator = { key: FileKey, result: AsyncCacheData<Boolean> ->
        key.file.listChildren(result.map(List<FileReference>::isNotEmpty))
    }
}