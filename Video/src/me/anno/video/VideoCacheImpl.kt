package me.anno.video

import me.anno.cache.Promise
import me.anno.gpu.texture.ITexture2D
import me.anno.io.MediaMetadata
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.SignatureCache
import me.anno.utils.types.Strings.shorten
import me.anno.video.ffmpeg.FFMPEGStream
import org.apache.logging.log4j.LogManager
import java.io.IOException
import kotlin.math.max

object VideoCacheImpl {

    private val LOGGER = LogManager.getLogger(VideoCacheImpl::class)

    fun generateVideoFrames(key: VideoFramesKey, result: Promise<VideoSlice>) {

        val file = key.file
        val scale = key.scale

        val signature = SignatureCache[file].waitFor()?.name
        val meta = MediaMetadata.getMeta(file, signature).waitFor()
        if (meta == null) {
            RuntimeException("Meta was not found for $key!").printStackTrace()
            result.value = null
            return
        }

        if (key.bufferIndex > 0) {
            val startTimeSeconds = key.bufferIndex * key.bufferLength / key.fps
            if (meta.videoDuration < startTimeSeconds) {
                result.value = null
                return
            }
        }

        val slice = VideoSlice(
            key, meta.videoWidth / scale, meta.videoHeight / scale,
            meta.videoWidth, meta.videoFPS, meta.videoFrameCount,
            result
        )
        result.value = slice // set this as early as possible to prevent waiting

        val frame0 = key.bufferIndex * key.bufferLength
        if (frame0 <= -key.bufferLength || frame0 >= max(1, slice.numTotalFramesInSrc))
            LOGGER.warn(
                "Access of frames is out of bounds: $frame0/${key.bufferLength}/${slice.numTotalFramesInSrc} by ${
                    file.absolutePath.shorten(200)
                }"
            )
        // what about video webp? I think it's pretty rare...
        var localIndex = 0 // is there a cleaner way?
        FFMPEGStream.getImageSequenceGPU(
            file, signature, slice.w, slice.h, key.bufferIndex * key.bufferLength,
            if (file.lcExtension == "webp" || file.lcExtension == "wav") 1 else key.bufferLength,
            key.fps, slice.originalWidth, slice.originalFPS,
            slice.numTotalFramesInSrc, { frame ->
                val index = localIndex++
                if (index in slice.indices) slice.frames[index] = frame
                if (result.hasBeenDestroyed || index !in slice.indices) frame.destroy()
            }, {
                slice.hasFinished = true
            }
        )
    }

    fun generateImageTexture(file: FileReference, result: Promise<ITexture2D>) {
        val file = file.resolved()
        if (file is FileFileRef) {
            FFMPEGStream.getImageGPU(file) { result.call(it, err(it)) }
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = FileFileRef.createTempFile("4ffmpeg", file.extension)
            file.copyTo(tmp) { e ->
                if (e == null) {
                    FFMPEGStream.getImageGPU(tmp) {
                        result.call(it, err(it))
                        tmp.delete()
                    }
                } else result.err(e)
            }
        }
    }

    private fun err(result: ITexture2D?): Exception? {
        if (result != null) return null
        return IOException("FFMPEG failed")
    }
}