package me.anno.video

import me.anno.cache.CacheSection
import me.anno.io.files.Signature
import me.anno.maths.Maths
import me.anno.utils.types.Strings.shorten
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.io.MediaMetadata
import org.apache.logging.log4j.LogManager

object VideoCacheImpl : CacheSection("Videos") {

    private val LOGGER = LogManager.getLogger(VideoCacheImpl::class)

    fun generateVideoFrames(key: VideoFramesKey): VideoSlice {
        val file = key.file
        val scale = key.scale
        val signature = Signature.findNameSync(file)
        val meta =
            MediaMetadata.getMeta(file, signature, false) ?: throw RuntimeException("Meta was not found for $key!")
        val data = VideoSlice(
            file, meta.videoWidth / scale, meta.videoHeight / scale, scale, key.bufferIndex,
            key.frameLength, key.fps, meta.videoWidth,
            meta.videoFPS, meta.videoFrameCount
        )

        val frame0 = data.bufferIndex * data.bufferLength
        if (frame0 <= -data.bufferLength || frame0 >= Maths.max(1, data.numTotalFramesInSrc))
            LOGGER.warn(
                "Access of frames is out of bounds: $frame0/${data.bufferLength}/${data.numTotalFramesInSrc} by ${
                    file.absolutePath.shorten(200)
                }"
            )
        // what about video webp? I think it's pretty rare...
        FFMPEGStream.getImageSequence(
            file, signature, data.w, data.h, data.bufferIndex * data.bufferLength,
            if (file.name.endsWith(".webp", true)) 1 else data.bufferLength, data.fps,
            data.originalWidth, data.originalFPS,
            data.numTotalFramesInSrc, {
                if (data.isDestroyed) it.destroy()
                else data.frames.add(it)
            }, {}
        )
        return data
    }

}