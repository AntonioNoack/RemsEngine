package me.anno.image

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.deferredToCallback
import me.anno.utils.async.deferredToValue
import me.anno.utils.async.mapSuccess
import me.anno.utils.async.pack
import java.io.ByteArrayInputStream
import java.io.InputStream

object ImageCache : CacheSection("Image") {

    var timeoutMillis = 10_000L

    val byteReaders = HashMap<String, AsyncImageReader<ByteArray>>()
    val fileReaders = HashMap<String, AsyncImageReader<FileReference>>()
    val streamReaders = HashMap<String, AsyncImageReader<InputStream>>()

    fun registerReader(
        signatures: String,
        byteReader: AsyncImageReader<ByteArray>,
        fileReader: AsyncImageReader<FileReference>,
        streamReader: AsyncImageReader<InputStream>
    ) {
        // todo keep lists instead, and try all until one succeeds
        synchronized(this) {
            for (signature in signatures.split(',')) {
                byteReaders[signature] = byteReader
                fileReaders[signature] = fileReader
                streamReaders[signature] = streamReader
            }
        }
    }

    fun registerStreamReader(signatures: String, streamReader: AsyncImageReader<InputStream>) {
        registerReader(signatures, { src, bytes ->
            streamReader.read(src, ByteArrayInputStream(bytes))
        }, { src, fileRef ->
            fileRef.inputStream().mapSuccess { input ->
                streamReader.read(src, input)
            }
        }, streamReader)
    }

    fun registerByteArrayReader(signatures: String, byteReader: AsyncImageReader<ByteArray>) {
        registerReader(signatures, byteReader, { src, fileRef ->
            fileRef.readBytes().mapSuccess { bytes ->
                byteReader.read(src, bytes)
            }
        }, { src, stream ->
            byteReader.read(src, stream.readBytes())
        })
    }

    /**
     * streamReader shall return image or exception
     * */
    fun registerDirectStreamReader(
        signatures: String,
        streamReader: (InputStream) -> Any
    ) {
        registerStreamReader(signatures) { _, stream ->
            val result = streamReader(stream)
            pack(result as? Image, result as? Exception)
        }
    }

    /**
     * streamReader shall return image or exception
     * */
    fun registerDirectStreamReader(
        signatures: String,
        streamReader: (FileReference, InputStream) -> Any
    ) {
        registerStreamReader(signatures) { src, stream ->
            val result = streamReader(src, stream)
            pack(result as? Image, result as? Exception)
        }
    }

    init {
        registerStreamReader("hdr") { _, it ->
            Result.success(HDRReader.readHDR(it))
        }
    }

    fun unregister(signatures: String) {
        synchronized(this) {
            for (signature in signatures.split(',')) {
                byteReaders.remove(signature)
                fileReaders.remove(signature)
                streamReaders.remove(signature)
            }
        }
    }

    @JvmStatic
    @Deprecated(USE_COROUTINES_INSTEAD)
    operator fun get(source: FileReference, async: Boolean): Image? {
        return get(source, timeoutMillis, async)
    }

    fun getImageWithoutGenerator(source: FileReference): Image? {
        if (source is ImageReadable && source.hasInstantCPUImage()) return source.readCPUImage()
        return when (val data = getDualEntryWithoutGenerator(source, source.lastModified, 0)) {
            is Image -> data
            is CacheData<*> -> data.value as? Image
            else -> null
        }
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    operator fun get(source: FileReference, timeout: Long, async: Boolean): Image? {
        if (source is ImageReadable && source.hasInstantCPUImage()) {
            return source.readCPUImage()
        }
        return deferredToValue(get(source, timeout), async)
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun getAsync(source: FileReference, timeout: Long, callback: Callback<Image>) {
        if (source is ImageReadable && source.hasInstantCPUImage()) {
            callback.ok(source.readCPUImage())
        } else {
            deferredToCallback(get(source, timeout), callback)
        }
    }

    operator fun get(source: FileReference, timeout: Long): Deferred<Result<Image>> {
        if (source is ImageReadable && source.hasInstantCPUImage()) {
            return CompletableDeferred(Result.success(source.readCPUImage()))
        }
        return getFileEntryX(source, false, timeout, { file1, _ ->
            ImageAsFolder.readImage(file1, false)
        })
    }
}