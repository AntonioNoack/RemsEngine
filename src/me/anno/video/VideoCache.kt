package me.anno.video

import me.anno.cache.CacheSection
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.Sleep
import me.anno.utils.async.Callback
import me.anno.video.formats.gpu.GPUFrame
import kotlin.math.max
import kotlin.math.min

object VideoCache : CacheSection("Videos") {

    const val framesPerSlice = 512
    const val scale = 4
    const val minSize = 16
    const val minSizeForScaling = scale * minSize
    var videoGenLimit = 16

    var getProxyFile: ((file: FileReference, sliceIndex: Int, async: Boolean) -> FileReference?)? = null
    var getProxyFileDontUpdate: ((file: FileReference, sliceIndex: Int) -> FileReference?)? = null
    var generateVideoFrames: ((key: VideoFramesKey) -> VideoSlice)? = null

    private fun getVideoFramesWithoutGenerator(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int, fps: Double
    ): VideoSlice? {
        return getEntryWithoutGenerator(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps)) as? VideoSlice
    }

    private fun getVideoFrames(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean
    ): VideoSlice? {
        if (!file.exists) return null
        val meta = MediaMetadata.getMeta(file, async) ?: return null
        val bufferLength2 = Maths.clamp(bufferLength, 1, max(1, meta.videoFrameCount))
        val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps
        val key = VideoFramesKey(file, scale, bufferIndex, bufferLength2, fps2)
        return getEntryLimited(key, timeout, async, videoGenLimit, generateVideoFrames ?: return null)
    }

    fun getVideoFrame(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean,
        needsToBeCreated: Boolean = true
    ): GPUFrame? {
        val localIndex = frameIndex % bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        return if (frameIndex < videoData.numTotalFramesInSrc && !async) {
            Sleep.waitUntilDefined(true) {
                videoData.getFrame(localIndex, needsToBeCreated)
            }
        } else videoData.getFrame(localIndex, needsToBeCreated)
    }

    fun getVideoFrameAsync(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long,
        needsToBeCreated: Boolean,
        callback: Callback<GPUFrame>,
    ) {
        val localIndex = frameIndex % bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, false)
        if (videoData != null && frameIndex < videoData.numTotalFramesInSrc) {
            Sleep.waitUntilDefined(true, {
                val frame = videoData.getFrame(localIndex, needsToBeCreated)
                if (frame != null && (!needsToBeCreated || frame.isCreated || frame.isDestroyed)) frame
                else null
            }) { frame ->
                if (needsToBeCreated && frame.isDestroyed) callback.err(RuntimeException("Frame has been destroyed"))
                else callback.ok(frame)
            }
        } else callback.err(null)
    }

    fun getVideoFrameWithoutGenerator(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int,
        bufferLength: Int, fps: Double
    ): GPUFrame? {
        val videoData = getVideoFramesWithoutGenerator(file, scale, bufferIndex, bufferLength, fps) ?: return null
        val frame = videoData.frames.getOrNull(frameIndex % bufferLength)
        return if (frame?.isCreated == true) frame else null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double, timeout: Long,
        meta: MediaMetadata, async: Boolean
    ): GPUFrame? {
        if (index < 0 || scale < 1) return null
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val getProxyFile = getProxyFile
        if (getProxyFile != null && useProxy(scale, bufferLength0, meta)) {
            val slice0 = index / framesPerSlice
            val file2 = getProxyFile(file, slice0, true)
            if (file2 != null) {
                val sliceI = index % framesPerSlice
                return getVideoFrame(file2, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout, meta, async)
            }
        }
        return getVideoFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }

    fun useProxy(scale: Int, bufferLength0: Int, meta: MediaMetadata?): Boolean {
        return scale >= 4 && bufferLength0 > 1 && framesPerSlice % bufferLength0 == 0 &&
                (meta != null && min(meta.videoWidth, meta.videoHeight) >= minSizeForScaling)
    }

    /**
     * returned frames are guaranteed to be created
     * */
    @Suppress("unused")
    fun getVideoFrameWithoutGenerator(
        meta: MediaMetadata, index: Int,
        bufferLength0: Int, fps: Double
    ): GPUFrame? {
        if (index < 0) return null
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val async = true
        val getProxyFile = getProxyFileDontUpdate
        for (scaleI in 1..4) {
            val scale = 1 shl (scaleI * 2)
            if (getProxyFile != null && useProxy(scale, bufferLength0, meta)) {
                val slice0 = index / framesPerSlice
                val file2 = getProxyFile(meta.file, slice0)
                if (file2 != null) {
                    val meta2 = MediaMetadata.getMeta(file2, async)
                    if (meta2 != null) {
                        val sliceI = index % framesPerSlice
                        return getVideoFrameWithoutGenerator(meta2, sliceI, bufferLength0, fps)
                    }
                }
            }
            return getVideoFrameWithoutGenerator(
                meta.file, scale, index,
                bufferIndex, bufferLength, fps
            ) ?: continue
        }
        return null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double,
        timeout: Long, async: Boolean
    ): GPUFrame? {
        if (index < 0 || scale < 1) return null
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        val getProxyFile = getProxyFile
        if (getProxyFile != null && useProxy(scale, bufferLength0, MediaMetadata.getMeta(file, async))) {
            val slice0 = index / framesPerSlice
            val file2 = getProxyFile(file, slice0, true)
            if (file2 != null) {
                val sliceI = index % framesPerSlice
                return getVideoFrame(file2, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout, async)
            }
        }
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        return getVideoFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrameAsync(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double,
        timeout: Long, callback: Callback<GPUFrame>
    ) {
        if (index < 0 || scale < 1) {
            val msg = if (index < 0) "Index must be >= 0"
            else "Scale must not be < 1"
            callback.err(IllegalArgumentException(msg))
            return
        }
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        val getProxyFile = getProxyFile
        if (getProxyFile != null && useProxy(scale, bufferLength0, MediaMetadata.getMeta(file, false))) {
            val slice0 = index / framesPerSlice
            val file2 = getProxyFile(file, slice0, true)
            if (file2 != null) {
                val sliceI = index % framesPerSlice
                getVideoFrameAsync(file2, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout, callback)
                return
            }
        }
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        getVideoFrameAsync(file, scale, index, bufferIndex, bufferLength, fps, timeout, true, callback)
    }
}