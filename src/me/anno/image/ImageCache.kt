package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileReference
import java.io.ByteArrayInputStream
import java.io.InputStream

object ImageCache : CacheSection("Image") {

    val byteReaders = HashMap<String, AsyncImageReader<ByteArray>>()
    val fileReaders = HashMap<String, AsyncImageReader<FileReference>>()
    val streamReaders = HashMap<String, AsyncImageReader<InputStream>>()

    fun registerReader(
        signature: String,
        byteReader: AsyncImageReader<ByteArray>,
        fileReader: AsyncImageReader<FileReference>,
        streamReader: AsyncImageReader<InputStream>
    ) {
        // todo keep lists instead, and try all until one succeeds
        synchronized(this) {
            byteReaders[signature] = byteReader
            fileReaders[signature] = fileReader
            streamReaders[signature] = streamReader
        }
    }

    fun registerStreamReader(
        signature: String,
        streamReader: AsyncImageReader<InputStream>
    ) {
        registerReader(signature, { bytes, callback ->
            streamReader.read(ByteArrayInputStream(bytes), callback)
        }, { fileRef, callback ->
            fileRef.inputStream { input, e ->
                if (input != null) streamReader.read(input, callback)
                else callback.err(e)
            }
        }, streamReader)
    }

    fun registerDirectStreamReader(
        signature: String,
        streamReader: (InputStream) -> Image
    ) {
        registerStreamReader(signature) { stream, callback ->
            callback.ok(streamReader(stream))
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
        return get(file, 10_000, async)
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
            ImageReader.readImage(file, false)
        } as? AsyncCacheData<*> ?: return null
        if (!async) data.waitForGFX()
        return data.value as? Image
    }
}