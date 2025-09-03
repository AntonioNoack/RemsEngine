package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature.Companion.sampleSize
import me.anno.io.files.inner.SignatureFile

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection<FileKey, Signature>("Signatures") {

    var timeoutMillis = 10_000L

    private val generate: (key: FileKey, result: AsyncCacheData<Signature>) -> Unit = { key, result ->
        val file = key.file
        when (file) {
            is SignatureFile -> result.value = file.signature
            else -> {
                // reads the bytes, or 255 if at end of file
                // how much do we read? 🤔
                // some formats are easy, others require more effort
                // maybe we could read them piece by piece...
                file.inputStream(sampleSize.toLong()) { input, err ->
                    if (input != null) {
                        val bytes = input.readNBytes2(sampleSize, false)
                        val sign = if (bytes != null) Signature.find(bytes) else null
                        result.value = sign
                    } else {
                        result.value = null
                        // todo can we store the exception somehow?
                        // result.err(err)
                    }
                }
            }
        }
    }

    operator fun get(file: FileReference): AsyncCacheData<Signature> {
        return getFileEntry(file, false, timeoutMillis, generate)
    }
}