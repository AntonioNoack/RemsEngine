package me.anno.image.thumbs

import me.anno.Time
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.ImageScale
import me.anno.image.hdr.HDRReader
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.utils.InternalAPI
import me.anno.utils.Sleep
import me.anno.utils.structures.Callback
import me.anno.utils.types.Strings.getImportTypeByExtension
import me.anno.video.VideoCache
import org.apache.logging.log4j.LogManager
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@InternalAPI
object ImageThumbnails {

    private val LOGGER = LogManager.getLogger(ImageThumbnails::class)

    @JvmStatic
    @InternalAPI
    fun register() {
        Thumbs.registerSignatures("png", ::generateImage)
        Thumbs.registerSignatures("bmp", ::generateImage)
        Thumbs.registerSignatures("psd", ::generateImage)
        Thumbs.registerSignatures("dds", ::generateVideoFrame)
        Thumbs.registerFileExtensions("dds", ::generateVideoFrame)
        Thumbs.registerFileExtensions("webp", ::generateVideoFrame)
        Thumbs.registerSignatures("media", ::generateVideoFrame)
        Thumbs.registerSignatures("hdr", ::generateHDRImage)
    }

    private fun generateHDRImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.inputStream { stream, exc ->
            if (stream != null) {
                val image = stream.use(HDRReader::readHDR)
                Thumbs.findScale(image, srcFile, size, callback) { dst ->
                    Thumbs.saveNUpload(srcFile, false, dstFile, dst, callback)
                }
            } else callback.err(exc)
        }
    }

    @JvmStatic
    fun generateImage(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        // a small timeout, because we need that image shortly only
        val totalNanos = 30_000_000_000L
        val timeout = 50L
        var image: Image? = null
        val startTime = Time.nanoTime
        Sleep.waitUntil(true, {
            if (Time.nanoTime < startTime + totalNanos) {
                image = ImageCache[srcFile, timeout, true]
                image != null || ImageCache.hasFileEntry(srcFile, timeout)
            } else true
        }, {
            if (image == null) {
                val ext = srcFile.lcExtension
                when (val importType = getImportTypeByExtension(ext)) {
                    "Video" -> {
                        LOGGER.info("Generating frame for $srcFile")
                        generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                    }
                    // else nothing to do
                    else -> {
                        LOGGER.info("ImageCache failed, importType '$importType' != getImportType for $srcFile")
                        TextThumbnails.generateTextImage(srcFile, dstFile, size, callback)
                    }
                }
            } else Thumbs.transformNSaveNUpload(srcFile, true, image!!, dstFile, size, callback)
        })
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
            return Thumbs.generate(srcFile, sizeI, callback)
        }

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = ImageScale.scaleMax(sw, sh, size)
        if (w < 2 || h < 2) return

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = max(min((time * fps).roundToInt(), meta.videoFrameCount - 1), 0)

        Sleep.waitUntilDefined(true, {
            val frame = VideoCache.getVideoFrame(srcFile, scale, index, 1, fps, 1000L, true)
            if (frame != null && (frame.isCreated || frame.isDestroyed)) frame
            else null
        }, { frame ->
            val texture = frame.toTexture()
            if (Thumbs.useCacheFolder) {
                val dst = texture.createImage(flipY = false, withAlpha = true)
                Thumbs.saveNUpload(srcFile, false, dstFile, dst, callback)
            } else callback.ok(texture)
        })
    }

    @JvmStatic
    fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>,
        wantedTime: Double
    ) {
        MediaMetadata.getMetaAsync(srcFile) { meta, err ->
            if (meta != null) {
                generateVideoFrame(srcFile, dstFile, size, callback, wantedTime, meta)
            } else callback.err(err)
        }
    }
}