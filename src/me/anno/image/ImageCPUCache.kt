package me.anno.image

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.raw.BIImage
import me.anno.image.tar.TGAImage
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.Signature
import me.anno.utils.Sleep
import me.anno.utils.maths.Maths.clamp
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

    fun getImage(file: FileReference, async: Boolean): Image? {
        val data = getEntry(file, 100_000, async) {
            if (file is ImageReadable) {
                CacheData(file.readImage())
            } else {
                if (file.length() < 1e7) { // < 10MB -> read directly
                    val bytes = file.readBytes()
                    CacheData(
                        when (Signature.findName(bytes)) {
                            "hdr" -> HDRImage(bytes.inputStream())
                            "tga" -> TGAImage.read(bytes.inputStream(), false)
                            "ico" -> tryIco(bytes.inputStream())
                            "dds", "media" -> tryFFMPEG(file)
                            null -> when (file.lcExtension) {
                                "tga" -> TGAImage.read(bytes.inputStream(), false)
                                "webp" -> tryFFMPEG(file)
                                else -> tryGeneric(bytes)
                            }
                            else -> tryGeneric(bytes)
                        }
                    )
                } else {
                    CacheData(
                        when (Signature.findName(file)) {
                            "hdr" -> HDRImage(file.inputStream())
                            "tga" -> TGAImage.read(file.inputStream(), false)
                            "ico" -> tryIco(file.inputStream())
                            "dds", "media" -> tryFFMPEG(file)
                            null -> when (file.lcExtension) {
                                "tga" -> TGAImage.read(file.inputStream(), false)
                                "webp" -> tryFFMPEG(file)
                                else -> tryGeneric(file)
                            }
                            else -> tryGeneric(file)
                        }
                    )
                }
            }
        } as? CacheData<*>
        return data?.value as? Image
    }

    private fun tryFFMPEG(file: FileReference): Image {
        return if (file is FileFileRef) {
            val meta = FFMPEGMetadata.getMeta(file, false)!!
            val sequence = FFMPEGStream.getImageSequenceCPU(
                file, meta.videoWidth, meta.videoHeight, min(20, (meta.videoFrameCount-1)/3), 1, meta.videoFPS
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
        // image array?
        if (images.size > 1) LOGGER.warn("Should implement array texture for ico files")
        if (images.isEmpty()) return null
        return images.maxByOrNull { it.width * it.height }!! // was blue ... why ever...
    }

    private fun tryGeneric(bytes: FileReference): Image? {
        var image = try {
            ImageIO.read(bytes.inputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (image == null) {
            try {
                image = Imaging.getBufferedImage(bytes.inputStream())
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
        return if (image == null) null else BIImage(image)
    }

    private fun tryGeneric(bytes: ByteArray): Image? {
        var image = try {
            ImageIO.read(bytes.inputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (image == null) {
            try {
                image = Imaging.getBufferedImage(bytes)
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
        return if (image == null) null else BIImage(image)
    }

}