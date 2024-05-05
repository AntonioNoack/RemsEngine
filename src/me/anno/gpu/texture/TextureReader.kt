package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.image.Image
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
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.utils.structures.Callback
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager

@InternalAPI
class TextureReader(val file: FileReference) : AsyncCacheData<ITexture2D>() {

    companion object {

        @JvmStatic
        val imageTimeout get() = DefaultConfig["ui.image.frameTimeout", 5000L]

        @JvmStatic
        private val LOGGER = LogManager.getLogger(TextureReader::class)

        // injected by ImagePlugin
        @JvmField
        var findExifRotation: ((FileReference, Callback<ImageTransform?>) -> Unit)? = null

        @JvmStatic
        fun getRotation(src: FileReference, callback: Callback<ImageTransform?>) {
            if (src == InvalidRef || src.isDirectory) return callback.ok(null)
            // which files can contain exif metadata?
            // according to https://exiftool.org/TagNames/EXIF.html,
            // JPG, TIFF, PNG, JP2, PGF, MIFF, HDP, PSP and XC, AVI and MOV
            val findRotation = findExifRotation ?: return callback.ok(null)
            findRotation(src, callback)
        }
    }

    private fun callback(texture: ITexture2D?, error: Exception?) {
        if (hasValue) {
            texture?.destroy()
            LOGGER.warn("Destroying $texture for $file before it was used")
        } else {
            value = texture
            error?.printStackTrace()
        }
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
            } else loadTexture()
        }
    }

    private fun loadTexture() {
        when (if (OS.isWeb) null else Signature.findNameSync(file)) {
            "dds", "media" -> tryUsingVideoCache(file)
            else -> ImageReader.readImage(file, true).waitForGFX(::loadImage)
        }
    }

    private fun loadImage(image: Image?) {
        when (image) {
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
                getRotation(file) { rot, _ ->
                    val texture = Texture2D("i2t/?/${file.name}", image.width, image.height, 1)
                    texture.rotation = rot
                    texture.create(image, true, ::callback)
                }
            }
        }
    }

    private fun tryUsingVideoCache(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val meta = getMeta(file, false)
        if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
            LOGGER.warn("Cannot load $file using VideoCache")
            value = null
        } else {
            Sleep.waitUntilDefined(true, {
                val frame = VideoCache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, true)
                if (frame != null && (frame.isCreated || frame.isDestroyed)) frame
                else null
            }, { frame ->
                GFX.addGPUTask("ImageData.useFFMPEG", frame.width, frame.height) {
                    value = frame.toTexture()
                }
            })
        }
    }
}
