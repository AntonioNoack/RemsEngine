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

        val data = VideoSlice(
            key, meta.videoWidth / scale, meta.videoHeight / scale,
            meta.videoWidth, meta.videoFPS, meta.videoFrameCount
        )

        val frame0 = key.bufferIndex * key.bufferLength
        if (frame0 <= -key.bufferLength || frame0 >= Maths.max(1, data.numTotalFramesInSrc))
            LOGGER.warn(
                "Access of frames is out of bounds: $frame0/${key.bufferLength}/${data.numTotalFramesInSrc} by ${
                    file.absolutePath.shorten(200)
                }"
            )
        // what about video webp? I think it's pretty rare...
        FFMPEGStream.getImageSequenceGPU(
            file, signature, data.w, data.h, key.bufferIndex * key.bufferLength,
            if (file.lcExtension == "webp" || file.lcExtension == "wav") 1 else key.bufferLength,
            key.fps, data.originalWidth, data.originalFPS,
            data.numTotalFramesInSrc, {
                if (data.isDestroyed) it.destroy()
                else data.frames.add(it)
            }, {
                data.finished = true
            }
        )
        result.value = data
    }
}