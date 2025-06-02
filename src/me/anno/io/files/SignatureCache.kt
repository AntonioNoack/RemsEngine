package me.anno.io.files

import kotlinx.coroutines.Deferred
import me.anno.cache.CacheSection
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature.Companion.sampleSize
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.mapSuccess2
import me.anno.utils.async.deferredToCallback
import me.anno.utils.async.deferredToValue

/**
 * cache for signatures, so files don't have to be read all the time
 * */
object SignatureCache : CacheSection("Signatures") {

    var timeoutMillis = 10_000L

    private suspend fun generate(file: FileReference): Result<Signature?> {
        return when (file) {
            is SignatureFile -> Result.success(file.signature)
            else -> {
                // reads the bytes, or 255 if at end of file
                // how much do we read? 🤔
                // some formats are easy, others require more effort
                // maybe we could read them piece by piece...
                file.inputStream(sampleSize.toLong()).mapSuccess2 { input ->
                    val bytes = input.readNBytes2(sampleSize, false)
                    Signature.find(bytes)
                }
            }
        }
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    operator fun get(file: FileReference, async: Boolean): Signature? {
        return deferredToValue(getX(file), async)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun getAsync(file: FileReference, callback: (Signature?) -> Unit) {
        return deferredToCallback(getX(file)) { sig, _ ->
            callback(sig)
        }
    }

    fun getX(file: FileReference): Deferred<Result<Signature?>> {
        return getFileEntryX(file, false, timeoutMillis) { file, _ -> generate(file) }
    }
}