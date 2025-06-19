package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.image.hdr.HDRReader
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import java.io.ByteArrayInputStream
import java.io.InputStream

object ImageCache : CacheSection<FileKey, Image>("Image") {

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
    operator fun get(source: FileReference): AsyncCacheData<Image> {
        return get(source, timeoutMillis)
    }

    fun getImageWithoutGenerator(source: FileReference): Image? {
        if (source is ImageReadable && source.hasInstantCPUImage()) return source.readCPUImage()
        return getEntryWithoutGenerator(source.getFileKey(), 0)?.value
    }

    operator fun get(source: FileReference, timeout: Long): AsyncCacheData<Image> {
        return if (source is ImageReadable && source.hasInstantCPUImage()) {
            AsyncCacheData(source.readCPUImage())
        } else {
            getFileEntry(source, false, timeout) { key, result ->
                ImageAsFolder.readImage(key.file, false, result)
            }
        }
    }
}