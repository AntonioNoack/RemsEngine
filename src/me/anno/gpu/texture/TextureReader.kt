package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.ImageReader
import me.anno.image.ImageTransform
import me.anno.image.raw.GPUImage
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.utils.InternalAPI
import me.anno.utils.Sleep
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager

@InternalAPI
class TextureReader(file: FileReference) : AsyncCacheData<ITexture2D>() {

    companion object {

        @JvmStatic
        val imageTimeout get() = DefaultConfig["ui.image.frameTimeout", 5000L]

        @JvmStatic
        private val LOGGER = LogManager.getLogger(TextureReader::class)

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

    fun callback(texture: ITexture2D?, error: Exception?) {
        value = texture
        error?.printStackTrace()
    }

    init {
        if (file is ImageReadable) {
            val texture = Texture2D("i2t/ir/${file.name}", 1024, 1024, 1)
            texture.create(file.readGPUImage(), true, ::callback)
        } else {
            val cpuImage = ImageCache.getImageWithoutGenerator(file)
            if (cpuImage != null) {
                val texture = Texture2D("i2t/ci/${file.name}", cpuImage.width, cpuImage.height, 1)
                cpuImage.createTexture(texture, sync = true, checkRedundancy = true, ::callback)
            } else when (Signature.findNameSync(file)) {
                "dds", "media" -> tryUsingVideoCache(file)
                else -> {
                    when (val image = ImageReader.readImage(file, true).waitForGFX()) {
                        is GPUImage -> {
                            val texture = Texture2D("copyOf/${image.texture.name}", image.width, image.height, 1)
                            texture.rotation = (image.texture as? Texture2D)?.rotation
                            texture.create(image, true, ::callback)
                        }
                        null -> {
                            LOGGER.warn("Null from ImageReader for $file")
                            value = null
                        }
                        else -> {
                            val texture = Texture2D("i2t/?/${file.name}", image.width, image.height, 1)
                            texture.rotation = getRotation(file)
                            texture.create(image, true, ::callback)
                        }
                    }
                }
            }
        }
    }

    fun tryUsingVideoCache(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val meta = getMeta(file, false)
        if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
            LOGGER.warn("Cannot load $file using VideoCache")
            value = null
        } else {
            val frame = Sleep.waitForGFXThreadUntilDefined(true) {
                VideoCache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, false)
            }
            frame.waitToLoad()
            GFX.addGPUTask("ImageData.useFFMPEG", frame.width, frame.height) {
                value = frame.toTexture()
            }
        }
    }
}
