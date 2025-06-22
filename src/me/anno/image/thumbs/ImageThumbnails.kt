package me.anno.image.thumbs

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.ImageAsFolder
import me.anno.image.ImageScale
import me.anno.image.hdr.HDRReader
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.utils.InternalAPI
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.mapAsync
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.getImportTypeByExtension
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@InternalAPI
object ImageThumbnails {

    private val LOGGER = LogManager.getLogger(ImageThumbnails::class)

    @JvmStatic
    @InternalAPI
    fun register() {
        ThumbnailCache.registerSignatures("png,bmp,psd", ::generateImage)
        ThumbnailCache.registerSignatures("dds,media", ::generateVideoFrame)
        ThumbnailCache.registerFileExtensions("dds,webp", ::generateVideoFrame)
        ThumbnailCache.registerSignatures("hdr", ::generateHDRImage)
    }

    private fun generateHDRImage(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.inputStream { stream, exc ->
            if (stream != null) {
                val image = stream.use(HDRReader::readHDR)
                ThumbnailCache.findScale(image, srcFile, size, callback) { dst ->
                    ThumbnailCache.saveNUpload(srcFile, false, dstFile, dst, callback)
                }
            } else callback.err(exc)
        }
    }

    @JvmStatic
    fun generateImage(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        ImageAsFolder.readImage(srcFile, true).waitFor { image ->
            if (image != null) {
                ThumbnailCache.transformNSaveNUpload(srcFile, true, image, dstFile, size, callback)
            } else {
                generateIfReadImageFailed(srcFile, dstFile, size, callback)
            }
        }
    }

    @JvmStatic
    private fun generateIfReadImageFailed(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        val ext = srcFile.lcExtension
        when (getImportTypeByExtension(ext)) {
            "Video", "Audio" -> { // audio can have thumbnail, too
                LOGGER.info("Generating frame for $srcFile")
                generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
            }
            // else nothing to do
            else -> {
                when (srcFile.lcExtension) {
                    "txt", "md" -> TextThumbnails.generateTextImage(srcFile, dstFile, size, callback)
                    else -> {
                        // LOGGER.warn("No thumbnail generator found for $srcFile")
                        callback.err(null)
                    }
                }
            }
        }
    }

    private fun generateVideoFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) = generateVideoFrame(srcFile, dstFile, size, callback, 1.0)

    @JvmStatic
    fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>,
        wantedTime: Double, meta: MediaMetadata
    ) {

        val mx = max(meta.videoWidth, meta.videoHeight)
        if (mx < size) {
            var sizeI = size shr 1
            while (mx < sizeI) sizeI = sizeI shr 1
            return ThumbnailCache.generate(srcFile, sizeI, callback)
        }

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = ImageScale.scaleMax(sw, sh, size)
        if (w < 2 || h < 2) return

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = max(min((time * fps).roundToIntOr(), meta.videoFrameCount - 1), 0)

        VideoCache.getVideoFrame(srcFile, scale, index, 1, fps, 1000L)
            .waitFor(callback.mapAsync { frame, cb2 ->
                val texture = frame.toTexture()
                if (ThumbnailCache.useCacheFolder) {
                    val dst = texture.createImage(flipY = false, withAlpha = true)
                    ThumbnailCache.saveNUpload(srcFile, false, dstFile, dst, cb2)
                } else cb2.ok(texture)
            })
    }

    @JvmStatic
    fun generateVideoFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>, wantedTime: Double
    ) {
        MediaMetadata.getMeta(srcFile).waitFor { meta, err ->
            if (meta != null) generateVideoFrame(srcFile, dstFile, size, callback, wantedTime, meta)
            else callback.err(err)
        }
    }
}