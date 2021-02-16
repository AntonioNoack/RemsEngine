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

    private fun getVideoFrames(
        file: File, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean
    ): VideoData? {
        return getEntry(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps), timeout, async) {
            val meta = getMeta(file, false)!!
            VideoData(
                file, meta.videoWidth / scale, meta.videoHeight / scale,
                bufferIndex, bufferLength, fps
            )
        } as? VideoData
    }

    private fun getVideoFramesDontUpdate(
        file: File, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double
    ): VideoData? {
        return getEntryDontUpdate(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps)) as? VideoData
    }

    /**
     * returned frames are guaranteed to be created
     * */
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
        val frame = videoData.frames.getOrNull(index % bufferLength)
        return if (frame?.isCreated == true) frame else null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrameDontUpdate(
        meta: FFMPEGMetadata,
        index: Int,
        bufferLength0: Int,
        fps: Double
    ): VFrame? {
        if (index < 0) return null
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val async = true
        for (scale in 1..4) {
            // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
            if (bufferLength0 > 0 && scale >= 4 && meta.run {
                    min(videoWidth, videoHeight) >= VideoProxyCreator.minSizeForScaling
                }) {
                val file2 = VideoProxyCreator.getProxyFileDontUpdate(meta.file)
                if (file2 != null) {
                    val meta2 = getMeta(file2, async)
                    if (meta2 != null) return getVideoFrameDontUpdate(meta2, index, bufferLength0, fps)
                }
            }
            val videoData = getVideoFramesDontUpdate(meta.file, scale, bufferIndex, bufferLength, fps) ?: return null
            val frame = videoData.frames.getOrNull(index % bufferLength)
            return if (frame?.isCreated == true) frame else null
        }
        return null
    }

    /**
     * returned frames are guaranteed to be created
     * */
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
        if (scale < 1) throw IllegalArgumentException("Scale must not be < 1")
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        if (bufferLength0 > 0 && scale >= 4 && (getMeta(file, async)?.run {
                min(videoWidth, videoHeight) >= VideoProxyCreator.minSizeForScaling
            } == true)) {
            val file2 = VideoProxyCreator.getProxyFile(file)
            if (file2 != null) {
                return getVideoFrame(file2, (scale + 2) / 4, index, bufferLength0, fps, timeout, async)
            }
        }
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        val frame = videoData.frames.getOrNull(index % bufferLength)
        return if (frame?.isCreated == true) frame else null
    }


}