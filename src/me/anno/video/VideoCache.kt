package me.anno.video

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.video.formats.gpu.GPUFrame
import kotlin.math.max
import kotlin.math.min

object VideoCache {

    private val slices = CacheSection<VideoFramesKey, VideoSlice>("Videos")
    private val frames = CacheSection<VideoFrameKey, GPUFrame>("Frames")

    fun clear() {
        slices.clear()
        frames.clear()
    }

    const val framesPerSlice = 512
    const val scale = 4
    const val minSize = 16
    const val minSizeForScaling = scale * minSize

    var getProxyFile: ((file: FileReference, sliceIndex: Int) -> Promise<FileReference>)? = null
    var getProxyFileDontUpdate: ((file: FileReference, sliceIndex: Int) -> FileReference?)? = null
    var generateVideoFrames: ((key: VideoFramesKey, result: Promise<VideoSlice>) -> Unit)? = null

    fun getVideoFramesWithoutGenerator(key: VideoFramesKey, delta: Long = 0L): Promise<VideoSlice>? =
        slices.getEntryWithoutGenerator(key, delta)

    fun getVideoFramesWithoutGenerator(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int, fps: Double,
        delta: Long = 0L
    ): Promise<VideoSlice>? =
        getVideoFramesWithoutGenerator(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps), delta)

    fun getVideoFrames(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long,
    ): Promise<VideoSlice> {

        val meta = MediaMetadata.getMeta(file).waitFor()
            ?: return Promise.empty()
        val bufferLength2 = Maths.clamp(bufferLength, 1, max(1, meta.videoFrameCount))
        val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps

        val key = VideoFramesKey(file, scale, bufferIndex, bufferLength2, fps2)
        return getVideoFrames(key, timeout)
    }

    private fun getVideoFrames(key: VideoFramesKey, timeout: Long): Promise<VideoSlice> {
        val generator = generateVideoFrames
        return if (generator != null) slices.getEntry(key, timeout, generator)
        else Promise.empty()
    }

    fun getVideoFrameImpl(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long
    ): Promise<GPUFrame> {

        val localIndex = frameIndex % bufferLength
        val meta = MediaMetadata.getMeta(file).waitFor()
            ?: return Promise.empty()
        val bufferLength2 = Maths.clamp(bufferLength, 1, max(1, meta.videoFrameCount))
        val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps

        val key0 = VideoFrameKey(file, scale, bufferIndex, bufferLength2, fps2, localIndex)
        return frames.getEntry(key0, timeout) { key, result ->
            val key2 = VideoFramesKey(key.file, key.scale, key.bufferIndex, key.bufferLength, key.fps)
            val frames = getVideoFrames(key2, timeout)
            frames.waitFor { slice ->
                if (slice != null) result.content = slice[key.localIndex].content
                else result.value = null
            }
        }
    }

    fun getVideoFrameWithoutGenerator(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int,
        bufferLength: Int, fps: Double
    ): Promise<GPUFrame> {
        val videoData = getVideoFramesWithoutGenerator(file, scale, bufferIndex, bufferLength, fps)
            ?.value ?: return Promise.empty()
        val localIndex = frameIndex % bufferLength
        return videoData[localIndex]
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double, timeout: Long,
        meta: MediaMetadata
    ): Promise<GPUFrame> {
        if (index < 0 || scale < 1) return Promise.empty()
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
        return getVideoFrameImpl(file, scale, index, bufferIndex, bufferLength, fps, timeout)
    }

    fun useProxy(scale: Int, bufferLength0: Int, meta: MediaMetadata?): Boolean {
        return scale >= 4 && bufferLength0 > 1 && framesPerSlice % bufferLength0 == 0 &&
                (meta != null && min(meta.videoWidth, meta.videoHeight) >= minSizeForScaling)
    }

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
            val frame = getVideoFrameWithoutGenerator(
                meta.file, scale, index,
                bufferIndex, bufferLength, fps
            ).value
            if (frame?.isCreated == true) return frame
        }
        return null
    }

    /**
     * returned frames are guaranteed to be created
     * */
    fun getVideoFrame(
        file: FileReference, scale: Int, index: Int,
        bufferLength0: Int, fps: Double, timeout: Long
    ): Promise<GPUFrame> {
        if (index < 0 || scale < 1) return Promise.empty()
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
    ): Promise<GPUFrame> {
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        return getVideoFrameImpl(file, scale, index, bufferIndex, bufferLength, fps, timeout)
    }
}