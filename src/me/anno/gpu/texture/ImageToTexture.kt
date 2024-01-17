package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.image.*
import me.anno.image.raw.GPUImage
import me.anno.image.raw.toImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.utils.Sleep
import me.anno.utils.types.Strings.getImportType
import me.anno.video.VideoCache
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.ImagingException
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

class ImageToTexture(file: FileReference) : AsyncCacheData<ITexture2D>() {

    companion object {

        @JvmStatic
        val imageTimeout get() = DefaultConfig["ui.image.frameTimeout", 5000L]

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ImageToTexture::class)

        // injected by ImagePlugin
        @JvmField
        var findExifRotation: ((FileReference) -> ImageTransform?)? = null

        @JvmStatic
        fun getRotation(src: FileReference): ImageTransform? {
            if (src == InvalidRef || src.isDirectory) return null
            // which files can contain exif metadata?
            // according to https://exiftool.org/TagNames/EXIF.html,
            // JPG, TIFF, PNG, JP2, PGF, MIFF, HDP, PSP and XC, AVI and MOV
            val findRotation = findExifRotation ?: return null
            return findRotation(src)
        }
    }

    var texture: ITexture2D? = null
    val hasFailed get() = hasValue && texture == null

    fun callback(texture: ITexture2D?, error: Exception?) {
        this.texture = texture
        error?.printStackTrace()
    }

    init {
        if (file is ImageReadable) {
            val texture = Texture2D("i2t/ir/${file.name}", 1024, 1024, 1)
            this.texture = texture
            texture.create(file.readGPUImage(), true, ::callback)
        } else {
            val cpuImage = ImageCache.getImageWithoutGenerator(file)
            if (cpuImage != null) {
                val texture = Texture2D("i2t/ci/${file.name}", cpuImage.width, cpuImage.height, 1)
                this.texture = texture
                cpuImage.createTexture(texture, sync = true, checkRedundancy = true, ::callback)
            } else when (Signature.findNameSync(file)) {
                "dds", "media" -> useFFMPEG(file)
                else -> {
                    val async = AsyncCacheData<Image?>()
                    ImageReader.readImage(file, async, true)
                    Sleep.waitForGFXThread(true) { async.hasValue }
                    when (val image = async.value) {
                        is GPUImage -> {
                            val texture = Texture2D("copyOf/${image.texture.name}", image.width, image.height, 1)
                            texture.rotation = (image.texture as? Texture2D)?.rotation
                            this.texture = texture
                            texture.create(image, true, ::callback)
                        }
                        null -> tryGetImage0(file, file.lcExtension)
                        else -> {
                            val texture = Texture2D("i2t/?/${file.name}", image.width, image.height, 1)
                            texture.rotation = getRotation(file)
                            this.texture = texture
                            texture.create(image, true, ::callback)
                        }
                    }
                }
            }
        }
    }

    fun useFFMPEG(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val meta = getMeta(file, false)
        if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
            LOGGER.warn("Cannot load $file using FFMPEG")
            texture = null
        } else {
            val frame = Sleep.waitForGFXThreadUntilDefined(true) {
                VideoCache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, false)
            }
            frame.waitToLoad()
            GFX.addGPUTask("ImageData.useFFMPEG", frame.width, frame.height) {
                texture = frame.toTexture()
            }
        }
    }

    private fun tryGetImage0(file: FileReference, fileExtension: String) {
        // read metadata information from jpegs
        // read the exif rotation header
        // because some camera images are rotated incorrectly
        if (fileExtension.getImportType() == "Video") {
            useFFMPEG(file)
        } else tryGetImage1(file)
    }

    private fun tryGetImage1(file: FileReference) {
        val image = tryGetImage(file)
        if (image != null) {
            val texture = Texture2D("i2t/bi/${file.name}", 1024, 1024, 1)
            texture.rotation = getRotation(file)
            this.texture = texture
            texture.create(image, checkRedundancy = true, ::callback)
        } else {
            useFFMPEG(file)
        }
    }

    private fun tryGetImage(file: FileReference): Image? {
        if (file is ImageReadable) return file.readGPUImage()
        return tryGetImage(file, file.inputStreamSync())
    }

    private fun tryGetImage(file: FileReference, stream: InputStream): Image? {
        LOGGER.warn("tryGetImage($file)")
        if (file is ImageReadable) return file.readGPUImage()
        // try ImageIO first, then Imaging, then give up (we could try FFMPEG, but idk, whether it supports sth useful)
        val image = try {
            ImageIO.read(stream)
        } catch (e: Exception) {
            null
        } ?: try {
            Imaging.getBufferedImage(stream)
        } catch (e: ImagingException) {
            onError(file, e)
        } catch (e: IOException) {
            onError(file, e)
        } as? BufferedImage
        return image?.toImage()
    }

    private fun onError(file: FileReference, e: Throwable) {
        LOGGER.warn("Cannot read image from input $file, $e")
    }

    override fun destroy() {
        texture?.destroy()
    }
}