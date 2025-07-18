package me.anno.video

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.video.formats.gpu.GPUFrame
import kotlin.math.max
import kotlin.math.min

object VideoCache : CacheSection<VideoFramesKey, VideoSlice>("Videos") {

    const val framesPerSlice = 512
    const val scale = 4
    const val minSize = 16
    const val minSizeForScaling = scale * minSize
    var videoGenLimit = 16

    var getProxyFile: ((file: FileReference, sliceIndex: Int) -> AsyncCacheData<FileReference>)? = null
    var getProxyFileDontUpdate: ((file: FileReference, sliceIndex: Int) -> FileReference?)? = null
    var generateVideoFrames: ((key: VideoFramesKey, result: AsyncCacheData<VideoSlice>) -> Unit)? = null

    private fun getVideoFramesWithoutGenerator(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int, fps: Double
    ): VideoSlice? {
        return getEntryWithoutGenerator(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps))?.value
    }

    private fun getVideoFrames(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long,
    ): AsyncCacheData<VideoSlice> {
        return MediaMetadata.getMeta(file).mapNext2 { meta ->
            val bufferLength2 = Maths.clamp(bufferLength, 1, max(1, meta.videoFrameCount))
            val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps
            val key = VideoFramesKey(file, scale, bufferIndex, bufferLength2, fps2)
            val generator = generateVideoFrames
            if (generator != null) getEntryLimitedWithRetry(key, timeout, videoGenLimit, generator)
            else AsyncCacheData.empty()
        }
    }

    fun getVideoFrame(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long,
        needsToBeCreated: Boolean = true
    ): AsyncCacheData<GPUFrame> {
        val localIndex = frameIndex % bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout)
        val result = AsyncCacheData<GPUFrame>()
        videoData.waitUntilDefined({ data ->
            data.getFrame(localIndex, needsToBeCreated)
        }, { frame ->
            result.value = frame
        })
        return result
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
        meta: MediaMetadata
    ): AsyncCacheData<GPUFrame> {
        if (index < 0 || scale < 1) return AsyncCacheData.empty()
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val getProxyFile = getProxyFile
        if (getProxyFile != null && useProxy(scale, bufferLength0, meta)) {
            val slice0 = index / framesPerSlice
            val proxyFile = getProxyFile(file, slice0).value
            if (proxyFile != null) {
                val sliceI = index % framesPerSlice
                val proxyFrame = getVideoFrame(
                    proxyFile, (scale + 2) / 4,
                    sliceI, bufferLength0, fps, timeout, meta
                )
                if (proxyFrame.hasValue) return proxyFrame
            }
        }
        return getVideoFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout)
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
        val getProxyFile = getProxyFileDontUpdate
        for (scaleI in 1..4) {
            val scale = 1 shl (scaleI * 2)
            if (getProxyFile != null && useProxy(scale, bufferLength0, meta)) {
                val slice0 = index / framesPerSlice
                val file2 = getProxyFile(meta.file, slice0)
                if (file2 != null) {
                    val meta2 = MediaMetadata.getMeta(file2).value
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
        bufferLength0: Int, fps: Double, timeout: Long
    ): AsyncCacheData<GPUFrame> {
        if (index < 0 || scale < 1) return AsyncCacheData.empty()
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        val getProxyFile = getProxyFile
        val maybeMeta = MediaMetadata.getMeta(file).value
        if (getProxyFile != null && useProxy(scale, bufferLength0, maybeMeta)) {
            val slice0 = index / framesPerSlice
            val proxyFile = getProxyFile(file, slice0).value
            if (proxyFile != null) {
                val sliceI = index % framesPerSlice
                val proxyFrame = getVideoFrame(proxyFile, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout)
                if (proxyFrame.hasValue) return proxyFrame
            }
        }
        return getVideoFrameImpl(file, scale, index, bufferLength0, fps, timeout)
    }

    fun getVideoFrameImpl(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double, timeout: Long
    ): AsyncCacheData<GPUFrame> {
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        return getVideoFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout)
    }
}