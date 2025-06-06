package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature.Companion.sampleSize
import me.anno.io.files.SignatureCache.generate
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.async.Callback

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {

    var timeoutMillis = 10_000L

    private fun generate(key: FileKey): AsyncCacheData<Signature?> {
        val value = AsyncCacheData<Signature?>()
        generate(key.file, value)
        return value
    }

    private fun generate(file: FileReference, callback: Callback<Signature?>) {
        return when (file) {
            is SignatureFile -> callback.ok(file.signature)
            else -> {
                // reads the bytes, or 255 if at end of file
                // how much do we read? 🤔
                // some formats are easy, others require more effort
                // maybe we could read them piece by piece...
                file.inputStream(sampleSize.toLong()) { input, err ->
                    if (input != null) {
                        val bytes = input.readNBytes2(sampleSize, false)
                        val sign = Signature.find(bytes)
                        callback.ok(sign)
                    } else callback.err(err)
                }
            }
        }
    }

    operator fun get(file: FileReference, async: Boolean): Signature? {
        return getFileEntry(file, false, timeoutMillis, async, ::generate)?.value
    }

    fun getAsync(file: FileReference, callback: (Signature?) -> Unit) {
        return getFileEntryAsync(file, false, timeoutMillis, true, ::generate) { sig, _ ->
            callback(sig?.value)
        }
    }
}