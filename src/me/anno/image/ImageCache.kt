package me.anno.image

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.wait
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
        registerReader(signatures, { src, bytes, callback ->
            streamReader.read(src, ByteArrayInputStream(bytes), callback)
        }, { src, fileRef, callback ->
            fileRef.inputStream { input, e ->
                if (input != null) streamReader.read(src, input, callback)
                else callback.err(e)
            }
        }, streamReader)
    }

    fun registerByteArrayReader(signatures: String, byteReader: AsyncImageReader<ByteArray>) {
        registerReader(signatures, byteReader, { src, fileRef, callback ->
            fileRef.readBytes { bytes, e ->
                if (bytes != null) byteReader.read(src, bytes, callback)
                else callback.err(e)
            }
        }, { src, stream, callback ->
            byteReader.read(src, stream.readBytes(), callback)
        })
    }

    /**
     * streamReader shall return image or exception
     * */
    fun registerDirectStreamReader(
        signatures: String,
        streamReader: (InputStream) -> Any
    ) {
        registerStreamReader(signatures) { _, stream, callback ->
            val result = streamReader(stream)
            callback.call(result as? Image, result as? Exception)
        }
    }

    /**
     * streamReader shall return image or exception
     * */
    fun registerDirectStreamReader(
        signatures: String,
        streamReader: (FileReference, InputStream) -> Any
    ) {
        registerStreamReader(signatures) { src, stream, callback ->
            val result = streamReader(src, stream)
            callback.call(result as? Image, result as? Exception)
        }
    }

    init {
        registerStreamReader("hdr") { _, it, callback ->
            callback.ok(HDRReader.readHDR(it))
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
    operator fun get(source: FileReference, async: Boolean): Image? {
        return get(source, timeoutMillis, async)
    }

    fun getImageWithoutGenerator(source: FileReference): Image? {
        if (source is ImageReadable && source.hasInstantCPUImage()) return source.readCPUImage()
        return when (val data = getEntryWithoutGenerator(source.getFileKey(), 0)) {
            is Image -> data
            is CacheData<*> -> data.value as? Image
            else -> null
        }
    }

    operator fun get(source: FileReference, timeout: Long, async: Boolean): Image? {
        if (source is ImageReadable && source.hasInstantCPUImage()) {
            return source.readCPUImage()
        }
        val data = getFileEntry(source, false, timeout, async) { key ->
            ImageAsFolder.readImage(key.file, false)
        } ?: return null
        if (!async) data.waitFor()
        return data.value
    }

    fun getAsync(source: FileReference, timeout: Long, async: Boolean, callback: Callback<Image>) {
        if (source is ImageReadable && source.hasInstantCPUImage()) {
            callback.ok(source.readCPUImage())
        } else {
            getFileEntryAsync(source, false, timeout, async, { key ->
                ImageAsFolder.readImage(key.file, false)
            }, callback.wait())
        }
    }
}