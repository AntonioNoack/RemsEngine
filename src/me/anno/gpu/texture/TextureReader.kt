package me.anno.gpu.texture

import me.anno.cache.AsyncCacheData
import me.anno.config.DefaultConfig
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.image.Image
import me.anno.image.ImageAsFolder
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.ImageTransform
import me.anno.image.raw.GPUImage
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.SignatureCache
import me.anno.utils.InternalAPI
import me.anno.utils.OSFeatures
import me.anno.utils.async.Callback
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager

@InternalAPI
class TextureReader(val file: FileReference, val result: AsyncCacheData<ITexture2D>) {

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
        if (result.hasValue) {
            texture?.destroy()
            LOGGER.warn("Destroying $texture for $file before it was used")
        } else {
            result.value = texture
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
                cpuImage.createTexture(texture, checkRedundancy = true, ::callback)
            } else loadTexture()
        }
    }

    private fun loadTexture() {
        if (OSFeatures.fileAccessIsHorriblySlow) { // skip loading the signature
            loadTexture1()
        } else {
            SignatureCache[file].waitFor(::loadTexture0)
        }
    }

    private fun loadTexture0(signature: Signature?) {
        when (signature?.name) {
            "dds", "media" -> tryUsingVideoCache(file)
            else -> loadTexture1()
        }
    }

    private fun loadTexture1() {
        ImageAsFolder.readImage(file, true).waitFor(::loadImage)
    }

    private fun loadImage(image: Image?) {
        when (image) {
            is GPUImage -> {
                val texture = Texture2D("copyOf/${image.texture.name}", image.width, image.height, 1)
                texture.rotation = (image.texture as? Texture2D)?.rotation
                texture.create(image, true, ::callback)
            }
            null -> {
                LOGGER.warn("Failed reading '$file' using ImageReader")
                result.value = null
            }
            else -> {
                getRotation(file) { rot, _ ->
                    val texture = Texture2D("i2t/?/$file", image.width, image.height, 1)
                    texture.rotation = rot
                    texture.create(image, true, ::callback)
                }
            }
        }
    }

    private fun tryUsingVideoCache(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        getMeta(file).waitFor { meta ->
            if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
                LOGGER.warn("Cannot load $file using VideoCache")
                result.value = null
            } else {
                VideoCache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout)
                    .waitFor({ frame ->
                        frame == null || frame.isCreated || frame.isDestroyed
                    }) { frame ->
                        if (frame != null) {
                            addGPUTask("ImageData.useFFMPEG", frame.width, frame.height) {
                                result.value = frame.toTexture()
                            }
                        } else result.value = null
                    }
            }
        }
    }
}
