package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.exr.EXRReader
import me.anno.image.gimp.GimpImage
import me.anno.image.hdr.HDRImage
import me.anno.image.raw.toImage
import me.anno.image.tar.TGAImage
import me.anno.io.files.BundledRef
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.Signature
import me.anno.io.zip.SignatureFile
import me.anno.maths.Maths.min
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream
import me.saharnooby.qoi.QOIImage
import net.sf.image4j.codec.ico.ICOReader
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

object ImageCPUCache : CacheSection("BufferedImages") {

    private val LOGGER = LogManager.getLogger(ImageCPUCache::class)

    private val byteReaders = HashMap<String, (ByteArray) -> Image?>()
    private val fileReaders = HashMap<String, (FileReference, ImageCallback) -> Unit>()
    private val streamReaders = HashMap<String, (InputStream) -> Image?>()

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
        byteReaders[signature] = { it.inputStream().use(streamReader) }
        fileReaders[signature] = { it, c ->
            it.inputStream { input, e ->
                c(input?.use {
                    streamReader(it)
                }, e)
            }
        }
        streamReaders[signature] = streamReader
    }

    init {
        registerStreamReader("hdr") { HDRImage(it) }
        registerStreamReader("tga") { TGAImage.read(it, false) }
        registerStreamReader("ico") { ICOReader.read(it) }
        registerStreamReader("gimp") { GimpImage.read(it) }
        registerStreamReader("exr") { EXRReader.read(it) }
        registerStreamReader("qoi") { QOIImage.read(it) }
    }

    // eps: like svg, we could implement it, but we don't really need it that dearly...

    @JvmStatic
    operator fun get(file: FileReference, async: Boolean): Image? {
        return get(file, 50, async)
    }

    fun getImageWithoutGenerator(file: FileReference): Image? {
        if (file is ImageReadable) return file.readImage()
        return when (val data = getDualEntryWithoutGenerator(file, file.lastModified, 0)) {
            is Image -> data
            is CacheData<*> -> data.value as? Image
            else -> null
        }
    }

    fun shouldUseFFMPEG(signature: String?, file: FileReference): Boolean {
        if (OS.isWeb) return false // uncomment, when we support FFMPEG in the browser XD
        return signature == "dds" || signature == "media" || file.lcExtension == "webp"
    }

    fun shouldIgnore(signature: String?): Boolean {
        return when (signature) {
            "rar", "bz2", "zip", "tar", "gzip", "xz", "lz4", "7z", "xar", "oar", "java", "text",
            "wasm", "ttf", "woff1", "woff2", "shell", "xml", "svg", "exe",
            "vox", "fbx", "gltf", "obj", "blend", "mesh-draco", "md2", "md5mesh", "dae",
            "yaml" -> true
            else -> false
        }
    }

    operator fun get(file0: FileReference, timeout: Long, async: Boolean): Image? {
        if (file0 is ImageReadable) return file0.readImage()
        val data = getFileEntry(file0, false, timeout, async) { file, _ ->
            val data = AsyncCacheData<Image?>()
            if (file is BundledRef || (file !is SignatureFile && file.length() < 10_000_000L)) { // < 10MB -> read directly
                file.readBytes { bytes, exc ->
                    exc?.printStackTrace()
                    if (bytes != null) {
                        val signature = Signature.findName(bytes)
                        if (shouldIgnore(signature)) {
                            data.value = null
                        } else if (shouldUseFFMPEG(signature, file)) {
                            tryFFMPEG(file) { it, e ->
                                data.value = it
                                e?.printStackTrace()
                            }
                        } else {
                            val reader = byteReaders[signature] ?: byteReaders[file.lcExtension]
                            data.value = if (reader != null) reader(bytes) else tryGeneric(file, bytes)
                        }
                    } else data.value = null
                }
            } else {
                Signature.findName(file) { signature ->
                    if (shouldIgnore(signature)) {
                        data.value = null
                    } else if (shouldUseFFMPEG(signature, file)) {
                        tryFFMPEG(file) { it, e ->
                            data.value = it
                            e?.printStackTrace()
                        }
                    } else {
                        val reader = fileReaders[signature] ?: fileReaders[file.lcExtension]
                        if (reader != null) reader(file) { it, e ->
                            e?.printStackTrace()
                            data.value = it
                        } else tryGeneric(file) { it, e ->
                            e?.printStackTrace()
                            data.value = it
                        }
                    }
                }

            }
            data
        }
        return if (data is AsyncCacheData<*>) data.value as? Image else null
    }

    private fun tryFFMPEG(file: FileReference, callback: ImageCallback) {
        return if (file is FileFileRef) {
            val meta = FFMPEGMetadata.getMeta(file, false)!!
            val sequence = FFMPEGStream.getImageSequenceCPU(
                file, meta.videoWidth, meta.videoHeight, min(20, (meta.videoFrameCount - 1) / 3), 1, meta.videoFPS,
                meta.videoFrameCount
            )
            Sleep.waitUntil(true) { sequence.frames.size > 0 || sequence.isFinished }
            callback(sequence.frames.first(), null)
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = FileFileRef.createTempFile("4ffmpeg", file.extension)
            file.readBytes { bytes, e ->
                if (bytes != null) {
                    tmp.writeBytes(bytes)
                    tryFFMPEG(getReference(tmp), callback)
                    tmp.delete()
                } else callback(null, e)
            }
        }
    }

    private fun tryGeneric(file: FileReference, callback: ImageCallback) {
        file.inputStream { it, exc ->
            if (it != null) {
                try {
                    val img = ImageIO.read(it) ?: throw IOException(file.toString())
                    it.close()
                    callback(img.toImage(), null)
                } catch (e: Exception) {
                    it.close()
                    file.inputStream { it2, exc2 ->
                        if (it2 != null) {
                            try {
                                callback(Imaging.getBufferedImage(it2).toImage(), null)
                            } catch (e: Exception) {
                                callback(null, e)
                            } finally {
                                it2.close()
                            }
                        } else callback(null, exc2)
                    }
                }
            } else callback(null, exc)
        }
        /*var image = try {
            ImageIO.read(file.inputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (image == null) {
            // LOGGER.debug("ImageIO failed for $file")
            try {
                image = Imaging.getBufferedImage(file.inputStream())
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
        if (image == null) LOGGER.debug("Imaging & ImageIO failed for $file, ${Signature.find(file)?.name}")
        return if (image == null) null else BIImage(image)*/
    }

    private fun tryGeneric(file: FileReference, bytes: ByteArray): Image? {
        var image = try {
            ImageIO.read(bytes.inputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (image == null) {
            LOGGER.debug("ImageIO failed for $file")
            try {
                image = Imaging.getBufferedImage(bytes)
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
        if (image == null) {
            LOGGER.debug("Imaging failed for $file")
        }
        return image?.toImage()
    }

}