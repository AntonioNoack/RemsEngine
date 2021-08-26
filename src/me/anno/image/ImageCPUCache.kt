package me.anno.image

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.image.raw.BIImage
import me.anno.image.tar.TGAImage
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import net.sf.image4j.codec.ico.ICODecoder
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import javax.imageio.ImageIO

object ImageCPUCache : CacheSection("BufferedImages") {

    private val LOGGER = LogManager.getLogger(ImageCPUCache::class)

    // todo ffmpeg-only formats like webp

    fun getImage(file: FileReference, async: Boolean): Image? {
        val data = getEntry(file, 100_000, async) {
            if (file is ImageReadable) {
                CacheData(file.readImage())
            } else {
                val bytes = file.readBytes()
                val signature = Signature.find(bytes)
                CacheData(
                    when (signature?.name) {
                        "hdr" -> HDRImage(bytes.inputStream())
                        "tga" -> TGAImage.read(bytes.inputStream(), false)
                        "ico" -> tryIco(bytes.inputStream())
                        null -> {
                            when (file.lcExtension) {
                                "tga" -> TGAImage.read(bytes.inputStream(), false)
                                else -> tryGeneric(bytes)
                            }
                        }
                        else -> tryGeneric(bytes)
                    }
                )
            }
        } as? CacheData<*>
        return data?.value as? Image
    }

    private fun tryIco(input: InputStream): Image? {
        val images = ICODecoder.read(input)
        // image array?
        if (images.size > 1) LOGGER.warn("Should implement array texture for ico files")
        if (images.isEmpty()) return null
        return images.maxByOrNull { it.width * it.height }!! // was blue ... why ever...
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