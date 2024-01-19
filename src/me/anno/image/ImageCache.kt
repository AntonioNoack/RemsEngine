package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileReference
import me.anno.utils.Sleep.waitForGFXThread
import java.io.ByteArrayInputStream
import java.io.InputStream

object ImageCache : CacheSection("Image") {

    val byteReaders = HashMap<String, (ByteArray) -> Image?>()
    val fileReaders = HashMap<String, (FileReference, ImageCallback) -> Unit>()
    val streamReaders = HashMap<String, (InputStream) -> Image?>()

    fun registerReader(
        signature: String,
        byteReader: (ByteArray) -> Image?,
        fileReader: (FileReference, ImageCallback) -> Unit,
        streamReader: (InputStream) -> Image?
    ) {
        byteReaders[signature] = byteReader
        fileReaders[signature] = fileReader
        streamReaders[signature] = streamReader
    }

    fun registerStreamReader(
        signature: String,
        streamReader: (InputStream) -> Image?
    ) {
        registerReader(signature, { bytes ->
            ByteArrayInputStream(bytes).use(streamReader)
        }, { fileRef, c ->
            fileRef.inputStream { input, e ->
                c(input?.use { input1 ->
                    streamReader(input1)
                }, e)
            }
        }, streamReader)
    }

    init {
        registerStreamReader("hdr") { HDRReader.read(it) }
    }

    fun unregister(vararg signatures: String) {
        for(signature in signatures) {
            byteReaders.remove(signature)
            fileReaders.remove(signature)
            streamReaders.remove(signature)
        }
    }

    // eps: like svg, we could implement it, but we don't really need it that dearly...

    @JvmStatic
    operator fun get(file: FileReference, async: Boolean): Image? {
        return get(file, 50, async)
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
            val data = AsyncCacheData<Image?>()
            ImageReader.readImage(file, data, false)
            data
        } as? AsyncCacheData<*> ?: return null
        if (!async) waitForGFXThread(true) { data.hasValue }
        return data.value as? Image
    }
}