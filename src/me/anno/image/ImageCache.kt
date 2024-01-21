package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback
import java.io.ByteArrayInputStream
import java.io.InputStream

object ImageCache : CacheSection("Image") {

    val byteReaders = HashMap<String, (ByteArray, Callback<Image>) -> Unit>()
    val fileReaders = HashMap<String, (FileReference, Callback<Image>) -> Unit>()
    val streamReaders = HashMap<String, (InputStream, Callback<Image>) -> Unit>()

    fun registerReader(
        signature: String,
        byteReader: (ByteArray, Callback<Image>) -> Unit,
        fileReader: (FileReference, Callback<Image>) -> Unit,
        streamReader: (InputStream, Callback<Image>) -> Unit
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
        streamReader: (InputStream, Callback<Image>) -> Unit
    ) {
        registerReader(signature, { bytes, callback ->
            streamReader(ByteArrayInputStream(bytes), callback)
        }, { fileRef, callback ->
            fileRef.inputStream { input, e ->
                if (input != null) streamReader(input, callback)
                else callback.err(e)
            }
        }, streamReader)
    }

    init {
        registerStreamReader("hdr") { it, callback ->
            callback.ok(HDRReader.read(it))
        }
    }

    fun unregister(vararg signatures: String) {
        synchronized(this) {
            for (signature in signatures) {
                byteReaders.remove(signature)
                fileReaders.remove(signature)
                streamReaders.remove(signature)
            }
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
            ImageReader.readImage(file, false)
        } as? AsyncCacheData<*> ?: return null
        if (!async) data.waitForGFX()
        return data.value as? Image
    }
}