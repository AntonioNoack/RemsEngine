package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.utils.Sleep

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {

    var timeoutMillis = 10_000L

    @Suppress("UNUSED_PARAMETER")
    private fun generate(file: FileReference, modified: Long): AsyncCacheData<Signature?> {
        val value = AsyncCacheData<Signature?>()
        Signature.find(file, value)
        return value
    }

    operator fun get(file: FileReference, async: Boolean): Signature? {
        return getFileEntry(file, false, timeoutMillis, async, ::generate)?.value
    }

    fun getAsync(file: FileReference, callback: (Signature?) -> Unit) {
        // a little more complicated than anticipated
        val file1 = getValidFile(file, false)
        if (file1 != null) {
            getDualEntryAsync(file1, file1.lastModified, timeoutMillis, true, ::generate) { sig, _ ->
                if (sig != null) sig.waitForGFX(callback)
                else callback(null)
            }
        } else callback(null)
    }
}