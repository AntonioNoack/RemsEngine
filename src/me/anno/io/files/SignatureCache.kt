package me.anno.io.files

import me.anno.cache.CacheData
import me.anno.cache.CacheSection

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {
    operator fun get(file: FileReference, async: Boolean): Signature? {
        val data = getFileEntry(file, false, 10_000, async) { _, _ ->
            CacheData(Signature.findSync(file))
        } as? CacheData<*>
        return data?.value as? Signature
    }
}