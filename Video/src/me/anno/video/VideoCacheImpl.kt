package me.anno.video

import me.anno.cache.AsyncCacheData
import me.anno.io.MediaMetadata
import me.anno.io.files.SignatureCache
import me.anno.maths.Maths
import me.anno.utils.types.Strings.shorten
import me.anno.video.ffmpeg.FFMPEGStream
import org.apache.logging.log4j.LogManager

object VideoCacheImpl {

    private val LOGGER = LogManager.getLogger(VideoCacheImpl::class)

    fun generateVideoFrames(key: VideoFramesKey, result: AsyncCacheData<VideoSlice>) {

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
        if (frame0 <= -key.bufferLength || frame0 >= Maths.max(1, slice.numTotalFramesInSrc))
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
}