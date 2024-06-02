package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {
    operator fun get(file: FileReference, async: Boolean): Signature? {
        return getFileEntry(file, false, 10_000, async) { _, _ ->
            val value = AsyncCacheData<Signature?>()
            Signature.find(file, value)
            value
        }?.value
    }
}