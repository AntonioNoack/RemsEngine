package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.VideoData
import me.anno.cache.keys.VideoFramesKey
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.VFrame
import me.anno.video.VideoProxyCreator
import java.io.File
import kotlin.math.max
import kotlin.math.min

object VideoCache : CacheSection("Videos") {

    fun getVideoFrames(
        file: File, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean
    ) : VideoData? {
        return getEntry(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps), timeout, async) {
            val meta = getMeta(file, false)!!
            VideoData(
                file, meta.videoWidth / scale, meta.videoHeight / scale,
                bufferIndex, bufferLength, fps
            )
        } as? VideoData
    }

    fun getVideoFrame(
        file: File, scale: Int, index: Int,
        bufferLength0: Int, fps: Double, timeout: Long,
        meta: FFMPEGMetadata, async: Boolean
    ): VFrame? {
        if (index < 0) return null
        if (scale < 1) throw RuntimeException()
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        if (scale >= 4 && meta.run { min(videoWidth, videoHeight) >= VideoProxyCreator.minSizeForScaling }) {
            val file2 = VideoProxyCreator.getProxyFile(file)
            if (file2 != null) {
                return getVideoFrame(file2, (scale + 2) / 4, index, bufferLength0, fps, timeout, meta, async)
            }
        }
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        return videoData.frames.getOrNull(index % bufferLength)
    }

    fun getVideoFrame(
        file: File,
        scale: Int,
        index: Int,
        bufferLength0: Int,
        fps: Double,
        timeout: Long,
        async: Boolean
    ): VFrame? {
        if (index < 0) return null
        if (scale < 1) throw RuntimeException()
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        if (scale >= 4 && (getMeta(file, async)?.run { min(videoWidth, videoHeight) >= VideoProxyCreator.minSizeForScaling } == true)) {
            val file2 = VideoProxyCreator.getProxyFile(file)
            if (file2 != null) {
                return getVideoFrame(file2, (scale + 2) / 4, index, bufferLength0, fps, timeout, async)
            }
        }
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        return videoData.frames.getOrNull(index % bufferLength)
    }


}