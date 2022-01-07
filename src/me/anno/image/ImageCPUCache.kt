package me.anno.image

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.gimp.GimpImage
import me.anno.image.hdr.HDRImage
import me.anno.image.raw.BIImage
import me.anno.image.tar.TGAImage
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.Signature
import me.anno.io.zip.SignatureFile
import me.anno.utils.Sleep
import me.anno.utils.maths.Maths.min
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGStream
import net.sf.image4j.codec.ico.ICODecoder
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO

object ImageCPUCache : CacheSection("BufferedImages") {

    private val LOGGER = LogManager.getLogger(ImageCPUCache::class)

    private val byteReaders = HashMap<String, (ByteArray) -> Image?>()
    private val fileReaders = HashMap<String, (FileReference) -> Image?>()
    private val streamReaders = HashMap<String, (InputStream) -> Image?>()

    fun registerReader(
        signature: String,
        byteReader: (ByteArray) -> Image?,
        fileReader: (FileReference) -> Image?,
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
        fileReaders[signature] = { it.inputStream().use(streamReader) }
        streamReaders[signature] = streamReader
    }

    init {
        registerStreamReader("hdr") { HDRImage(it) }
        registerStreamReader("tga") { TGAImage.read(it, false) }
        registerStreamReader("ico") { tryIco(it) }
        registerStreamReader("gimp") { GimpImage.createThumbnail(it) }
    }

    // eps: like svg, we could implement it, but we don't really need it that dearly...

    fun getImage(file: FileReference, async: Boolean): Image? {
        return getImage(file, 50, async)
    }

    fun shouldUseFFMPEG(signature: String?, file: FileReference): Boolean {
        return signature == "dds" || signature == "media" || file.lcExtension == "webp"
    }

    fun getImage(file0: FileReference, timeout: Long, async: Boolean): Image? {
        if (file0 is ImageReadable) return file0.readImage()
        val data = getFileEntry(file0, false, timeout, async) { file, _ ->
            if (file !is SignatureFile && file.length() < 10_000_000L) { // < 10MB -> read directly
                val bytes = file.readBytes()
                val signature = Signature.findName(bytes)
                if (shouldUseFFMPEG(signature, file)) {
                    tryFFMPEG(file)
                } else {
                    val reader = byteReaders[signature] ?: byteReaders[file.lcExtension]
                    if (reader != null) reader(bytes) else tryGeneric(file, bytes)
                }
            } else {
                val signature = Signature.findName(file)
                if (shouldUseFFMPEG(signature, file)) {
                    tryFFMPEG(file)
                } else {
                    val reader = fileReaders[signature] ?: fileReaders[file.lcExtension]
                    if (reader != null) reader(file) else tryGeneric(file)
                }
            }
        }
        return when (data) {
            is Image -> data
            is CacheData<*> -> data.value as? Image
            else -> null
        }
    }

    private fun tryFFMPEG(file: FileReference): Image {
        return if (file is FileFileRef) {
            val meta = FFMPEGMetadata.getMeta(file, false)!!
            val sequence = FFMPEGStream.getImageSequenceCPU(
                file, meta.videoWidth, meta.videoHeight, min(20, (meta.videoFrameCount - 1) / 3), 1, meta.videoFPS
            )
            Sleep.waitUntil(true) { sequence.frames.size > 0 }
            sequence.frames.first()
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = File.createTempFile("4ffmpeg", file.extension)
            tmp.writeBytes(file.readBytes())
            val image = tryFFMPEG(getReference(tmp))
            tmp.delete()
            image
        }
    }

    private fun tryIco(input: InputStream): Image? {
        val images = ICODecoder.read(input)
        // image array? it happens for better scalable icons
        if (images.size > 1) LOGGER.warn("Should implement array texture for ico files")
        if (images.isEmpty()) return null
        return images.maxByOrNull { it.width * it.height }!! // was blue ... why ever...
    }

    private fun tryGeneric(file: FileReference): Image? {
        var image = try {
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
        return if (image == null) null else BIImage(image)
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
        return if (image == null) null else BIImage(image)
    }

}