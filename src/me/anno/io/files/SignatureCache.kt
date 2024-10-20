package me.anno.io.files

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature.Companion.json
import me.anno.io.files.Signature.Companion.sampleSize
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.async.Callback

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {

    var timeoutMillis = 10_000L

    @Suppress("UNUSED_PARAMETER")
    private fun generate(file: FileReference, modified: Long): AsyncCacheData<Signature?> {
        val value = AsyncCacheData<Signature?>()
        generate(file, value)
        return value
    }

    private fun generate(file: FileReference, callback: Callback<Signature?>) {
        if (file is SignatureFile) return callback.ok(file.signature)
        if (!file.exists) return callback.ok(null)
        return when (file) {
            is PrefabReadable -> callback.ok(json)
            else -> {
                // reads the bytes, or 255 if at end of file
                // how much do we read? 🤔
                // some formats are easy, others require more effort
                // maybe we could read them piece by piece...
                file.inputStream(sampleSize.toLong()) { input, err ->
                    if (input != null) callback.ok(Signature.find(input.readNBytes2(sampleSize, false)))
                    else callback.err(err)
                }
            }
        }
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