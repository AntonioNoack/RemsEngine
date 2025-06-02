package me.anno.gpu.texture

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
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.mapSuccess
import me.anno.utils.async.waitForCallback
import me.anno.video.VideoCache
import java.io.IOException

@InternalAPI
object TextureReader {

    @JvmStatic
    val imageTimeout get() = DefaultConfig["ui.image.frameTimeout", 5000L]

    // injected by ImagePlugin
    @JvmField
    var findExifRotation: ((FileReference, Callback<ImageTransform?>) -> Unit)? = null

    @JvmStatic
    @Deprecated(USE_COROUTINES_INSTEAD)
    fun getRotation(src: FileReference, callback: Callback<ImageTransform?>) {
        if (src == InvalidRef || src.isDirectory) return callback.ok(null)
        // which files can contain exif metadata?
        // according to https://exiftool.org/TagNames/EXIF.html,
        // JPG, TIFF, PNG, JP2, PGF, MIFF, HDP, PSP and XC, AVI and MOV
        val findRotation = findExifRotation ?: return callback.ok(null)
        findRotation(src, callback)
    }

    suspend fun read(file: FileReference): Result<ITexture2D> {
        if (file is ImageReadable) {
            val texture = Texture2D("i2t/ir/${file.name}", 1024, 1024, 1)
            return waitForCallback { callback ->
                texture.create(file.readGPUImage(), true, callback)
            }
        } else {
            val cpuImage = ImageCache.getImageWithoutGenerator(file)
            return if (cpuImage != null) {
                val texture = Texture2D("i2t/ci/${file.name}", cpuImage.width, cpuImage.height, 1)
                waitForCallback { callback ->
                    cpuImage.createTexture(texture, checkRedundancy = true, callback)
                }
            } else loadTexture(file)
        }
    }

    private suspend fun loadTexture(file: FileReference): Result<ITexture2D> {
        return if (OSFeatures.fileAccessIsHorriblySlow) { // skip loading the signature
            loadTexture1(file)
        } else {
            val signature = SignatureCache.getX(file).await().getOrNull()
            loadTexture0(file, signature)
        }
    }

    private suspend fun loadTexture0(file: FileReference, signature: Signature?): Result<ITexture2D> {
        return when (signature?.name) {
            "dds", "media" -> tryUsingVideoCache(file)
            else -> loadTexture1(file)
        }
    }

    private suspend fun loadTexture1(file: FileReference): Result<ITexture2D> {
        return ImageAsFolder.readImage(file, true).mapSuccess { image ->
            loadImage(file, image)
        }
    }

    private suspend fun loadImage(file: FileReference, image: Image?): Result<ITexture2D> {
        return when (image) {
            is GPUImage -> {
                val texture = Texture2D("copyOf/${image.texture.name}", image.width, image.height, 1)
                texture.rotation = (image.texture as? Texture2D)?.rotation
                return waitForCallback { callback ->
                    texture.create(image, true, callback)
                }
            }
            null -> Result.failure(IOException("Failed reading '$file' using ImageReader"))
            else -> {
                return waitForCallback { callback ->
                    getRotation(file) { rot, _ ->
                        val texture = Texture2D("i2t/?/$file", image.width, image.height, 1)
                        texture.rotation = rot
                        texture.create(image, true, callback)
                    }
                }
            }
        }
    }

    private suspend fun tryUsingVideoCache(file: FileReference): Result<ITexture2D> {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val meta = getMeta(file, false)
        return if (meta == null || !meta.hasVideo || meta.videoFrameCount < 1) {
            Result.failure(IOException("Cannot load $file using VideoCache"))
        } else {
            waitForCallback { callback ->
                Sleep.waitUntilDefined(true, {
                    val frame = VideoCache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, true)
                    if (frame != null && (frame.isCreated || frame.isDestroyed)) frame
                    else null
                }, { frame ->
                    addGPUTask("ImageData.useFFMPEG", frame.width, frame.height) {
                        callback.ok(frame.toTexture())
                    }
                })
            }
        }
    }
}
