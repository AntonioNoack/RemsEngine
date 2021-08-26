package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.VideoData
import me.anno.cache.keys.VideoFramesKey
import me.anno.io.files.FileReference
import me.anno.utils.maths.Maths.clamp
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.VFrame
import me.anno.video.VideoProxyCreator
import kotlin.math.max
import kotlin.math.min

// todo visualize, what is loaded (resolution, audio & video) for future debugging

object VideoCache : CacheSection("Videos") {

    private val videoGenLimit = 16

    private fun getVideoFrames(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean
    ): VideoData? {
        if (!LastModifiedCache[file].exists) return null
        val meta = getMeta(file, async) ?: return null
        val bufferLength2 = clamp(bufferLength, 1, max(1, meta.videoFrameCount))
        val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps
        val key = VideoFramesKey(file, scale, bufferIndex, bufferLength2, fps2)
        return getEntryLimited(key, timeout, async, videoGenLimit, ::generateVideoFrames) as? VideoData
    }

    private fun generateVideoFrames(key: VideoFramesKey): VideoData {
        val file = key.file
        val scale = key.scale
        val bufferIndex = key.bufferIndex
        val bufferLength = key.frameLength
        val fps = key.fps
        val meta = getMeta(file, false) ?: throw RuntimeException("Meta was not found for $key!")
        return VideoData(
            file, meta.videoWidth / scale, meta.videoHeight / scale, scale,
            bufferIndex, bufferLength, fps
        )
    }

    private fun getVideoFramesWithoutGenerator(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double
    ): VideoData? {
        return getEntryWithoutGenerator(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps)) as? VideoData
    }

    fun getFrame(
        file: FileReference, scale: Int,
        index: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean,
        needsToBeCreated: Boolean = true
    ): VFrame? {
        val localIndex = index % bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        val frame = videoData.frames.getOrNull(localIndex)
        return if (!needsToBeCreated || frame?.isCreated == true) frame else null
    }

    fun getFrameWithoutGenerator(
        file: FileReference, scale: Int,
        index: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double
    ): VFrame? {
        val videoData = getVideoFramesWithoutGenerator(file, scale, bufferIndex, bufferLength, fps) ?: return null
        val frame = videoData.frames.getOrNull(index % bufferLength)
        return if (frame?.isCreated == true) frame else null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference, scale: Int, index: Int,
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
        return getFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrameWithoutGenerator(
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
                    if (meta2 != null) return getVideoFrameWithoutGenerator(meta2, index, bufferLength0, fps)
                }
            }
            val frame = getFrameWithoutGenerator(meta.file, scale, index, bufferIndex, bufferLength, fps)
            if (frame != null) return frame
        }
        return null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference,
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
        if (bufferLength0 > 1 && scale >= 4 && (getMeta(file, async)?.run {
                min(videoWidth, videoHeight) >= VideoProxyCreator.minSizeForScaling
            } == true)) {
            val file2 = VideoProxyCreator.getProxyFile(file)
            if (file2 != null) {
                return getVideoFrame(file2, (scale + 2) / 4, index, bufferLength0, fps, timeout, async)
            }
        }
        return getFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }


}