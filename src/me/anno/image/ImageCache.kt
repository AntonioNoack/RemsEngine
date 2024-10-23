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

    fun registerStreamReader(
        signatures: String,
        streamReader: AsyncImageReader<InputStream>
    ) {
        registerReader(signatures, { bytes, callback ->
            streamReader.read(ByteArrayInputStream(bytes), callback)
        }, { fileRef, callback ->
            fileRef.inputStream { input, e ->
                if (input != null) streamReader.read(input, callback)
                else callback.err(e)
            }
        }, streamReader)
    }

    /**
     * reader shall return image or exception
     * */
    fun registerDirectStreamReader(
        signatures: String,
        streamReader: (InputStream) -> Any
    ) {
        registerStreamReader(signatures) { stream, callback ->
            val result = streamReader(stream)
            callback.call(result as? Image, result as? Exception)
        }
    }

    init {
        registerStreamReader("hdr") { it, callback ->
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
    operator fun get(file: FileReference, async: Boolean): Image? {
        return get(file, timeoutMillis, async)
    }

    fun getImageWithoutGenerator(file: FileReference): Image? {
        if (file is ImageReadable && file.hasInstantCPUImage()) return file.readCPUImage()
        return when (val data = getDualEntryWithoutGenerator(file, file.lastModified, 0)) {
            is Image -> data
            is CacheData<*> -> data.value as? Image
            else -> null
        }
    }

    operator fun get(file0: FileReference, timeout: Long, async: Boolean): Image? {
        if (file0 is ImageReadable) return file0.readCPUImage()
        val data = getFileEntry(file0, false, timeout, async) { file, _ ->
            ImageAsFolder.readImage(file, false)
        } ?: return null
        if (!async) data.waitFor()
        return data.value
    }

    fun getAsync(file0: FileReference, timeout: Long, async: Boolean, callback: Callback<Image>) {
        if (file0 is ImageReadable) {
            callback.ok(file0.readCPUImage())
        } else {
            getFileEntryAsync(file0, false, timeout, async, { file, _ ->
                ImageAsFolder.readImage(file, false)
            }, callback.wait())
        }
    }
}