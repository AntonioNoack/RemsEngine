package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.VideoData
import me.anno.cache.keys.VideoFramesKey
import me.anno.gpu.drawing.DrawRectangles
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.black
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.video.BlankFrameDetector
import me.anno.video.VideoProxyCreator
import me.anno.video.VideoProxyCreator.framesPerSlice
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import me.anno.video.formats.gpu.GPUFrame
import kotlin.math.max
import kotlin.math.min

object VideoCache : CacheSection("Videos") {

    enum class Status(val color: Int) {
        FULL_SCALE_READY(0x24ff2c),
        READY(0xbbe961),
        BLANK(0x000000),
        WAIT_FOR_GPU_UPLOAD(0x61e9e3),
        BUFFER_LOADING(0xe9e561),
        DESTROYED(0xca55e7),
        NO_FRAME_NEEDED(0xa09da1),
        MISSING(0x9b634e),
        INVALID_INDEX(0xe73838),
        INVALID_SCALE(0xe73845),
        META_LOADING(0x442d24)
    }

    var videoGenLimit = 16

    private fun drawStatus(x: Int, y: Int, w: Int, h: Int, status: Status) {
        DrawRectangles.drawRect(x, y, w, h, status.color or black)
    }

    @Suppress("unused")
    fun drawLoadingStatus(
        x0: Int, y0: Int, x1: Int, y1: Int,
        file: FileReference, fps: Double = 0.0,
        blankFrameThreshold: Float,
        timeMapper: (x: Int) -> Double,
        bufferLength: Int = VideoData.framesPerContainer
    ) {
        val meta = getMeta(file, true)
        val maxScale = 32
        if (meta != null) {
            if (meta.videoFrameCount < 2) {
                var bestStatus = Status.MISSING
                for (scale in 1 until maxScale) {
                    val status = getFrameStatus(file, scale, 0, 1, 1.0, blankFrameThreshold)
                    if (status.ordinal < bestStatus.ordinal) bestStatus = status
                }
                drawStatus(x0, y0, x1 - x0, y1 - y0, bestStatus)
            } else {
                val fps2 = if (fps > 0.0) fps else meta.videoFPS
                var lastFrameIndex = -1
                var lastX = x0
                var lastStatus = Status.INVALID_SCALE
                // from left to right query all video data
                for (xi in x0 until x1) {
                    val time = timeMapper(xi)
                    var bestStatus = Status.MISSING
                    if (time >= 0.0) {
                        val frameIndex = (time * meta.videoFrameCount / meta.videoDuration)
                            .toInt() % meta.videoFrameCount
                        // if frame index is same as previously, don't request again
                        if (frameIndex == lastFrameIndex) {
                            bestStatus = lastStatus
                        } else {
                            lastFrameIndex = frameIndex
                            for (scale in 1 until maxScale) {
                                var status =
                                    getFrameStatus(file, scale, frameIndex, bufferLength, fps2, blankFrameThreshold)
                                if (status == Status.READY && scale == 1) status = Status.FULL_SCALE_READY
                                if (status.ordinal < bestStatus.ordinal) bestStatus = status
                            }
                        }
                    } else bestStatus = Status.NO_FRAME_NEEDED
                    // optimize drawing routine: draw blocks as one
                    if (bestStatus != lastStatus && xi > x0) {
                        // draw previous stripe
                        drawStatus(lastX, y0, xi - lastX, y1 - y0, lastStatus)
                        lastX = xi
                    }
                    lastStatus = bestStatus
                }
                // draw last stripe
                drawStatus(lastX, y0, x1 - lastX, y1 - y0, lastStatus)
            }
        } else drawStatus(x0, y0, x1 - x0, y1 - y0, Status.META_LOADING)
    }

    fun getFrameStatus(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferLength: Int, fps: Double,
        blankFrameThreshold: Float
    ): Status {
        if (frameIndex < 0) return Status.INVALID_INDEX
        if (scale < 1) return Status.INVALID_SCALE
        val bufferIndex = frameIndex / bufferLength
        val localIndex = frameIndex % bufferLength
        val videoData = getVideoFramesWithoutGenerator(file, scale, bufferIndex, bufferLength, fps)
            ?: return Status.MISSING
        val frame = videoData.frames.getOrNull(localIndex)
        return when {
            frame == null -> Status.BUFFER_LOADING
            frame.isDestroyed -> Status.DESTROYED
            frame.isCreated -> {
                if (BlankFrameDetector.isBlankFrame2(file, scale, frameIndex, bufferLength, fps, blankFrameThreshold))
                    Status.BLANK else Status.READY
            }
            else -> Status.WAIT_FOR_GPU_UPLOAD
        }
    }

    fun getVideoFrames(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean
    ): VideoData? {
        if (!file.exists) return null
        val meta = getMeta(file, async) ?: return null
        val bufferLength2 = clamp(bufferLength, 1, max(1, meta.videoFrameCount))
        val fps2 = if (meta.videoFrameCount < 2) 1.0 else fps
        val key = VideoFramesKey(file, scale, bufferIndex, bufferLength2, fps2)
        return getEntryLimited(key, timeout, async, videoGenLimit, ::generateVideoFrames) as? VideoData
    }

    fun generateVideoFrames(key: VideoFramesKey): VideoData {
        val file = key.file
        val scale = key.scale
        val signature = Signature.findNameSync(file)
        val meta = getMeta(file, signature, false) ?: throw RuntimeException("Meta was not found for $key!")
        return VideoData(
            file, signature, meta.videoWidth / scale, meta.videoHeight / scale, scale,
            key.bufferIndex, key.frameLength, key.fps,
            meta.videoWidth, meta.videoFPS,
            meta.videoFrameCount
        )
    }

    private fun getVideoFramesWithoutGenerator(
        file: FileReference, scale: Int,
        bufferIndex: Int, bufferLength: Int,
        fps: Double
    ) = getEntryWithoutGenerator(VideoFramesKey(file, scale, bufferIndex, bufferLength, fps)) as? VideoData

    fun getFrame(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double, timeout: Long, async: Boolean,
        needsToBeCreated: Boolean = true
    ): GPUFrame? {
        val localIndex = frameIndex % bufferLength
        val videoData = getVideoFrames(file, scale, bufferIndex, bufferLength, fps, timeout, async) ?: return null
        return if (frameIndex < videoData.numTotalFramesInSrc && !async) {
            waitForGFXThreadUntilDefined(true) {
                videoData.getFrame(localIndex, needsToBeCreated)
            }
        } else videoData.getFrame(localIndex, needsToBeCreated)
    }

    fun getFrameWithoutGenerator(
        file: FileReference, scale: Int,
        frameIndex: Int, bufferIndex: Int, bufferLength: Int,
        fps: Double
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
        meta: FFMPEGMetadata, async: Boolean
    ): GPUFrame? {
        if (index < 0) return null
        if (scale < 1) throw RuntimeException()
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        if (useProxy(scale, bufferLength0, meta)) {
            val slice0 = index / framesPerSlice
            val file2 = VideoProxyCreator.getProxyFile(file, slice0)
            if (file2 != null) {
                val sliceI = (index % framesPerSlice).toInt()
                return getVideoFrame(file2, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout, meta, async)
            }
        }
        return getFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }

    fun useProxy(scale: Int, bufferLength0: Int, meta: FFMPEGMetadata?): Boolean {
        return scale >= 4 && bufferLength0 > 1 && framesPerSlice % bufferLength0 == 0L &&
                (meta != null && min(meta.videoWidth, meta.videoHeight) >= VideoProxyCreator.minSizeForScaling)
    }

    /**
     * returned frames are guaranteed to be created
     * */
    @Suppress("unused")
    fun getVideoFrameWithoutGenerator(
        meta: FFMPEGMetadata,
        index: Int,
        bufferLength0: Int,
        fps: Double
    ): GPUFrame? {
        if (index < 0) return null
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        val async = true
        for (scale in 1..4) {
            if (useProxy(scale, bufferLength0, meta)) {
                val slice0 = index / framesPerSlice
                val file2 = VideoProxyCreator.getProxyFileDontUpdate(meta.file, slice0)
                if (file2 != null) {
                    val meta2 = getMeta(file2, async)
                    if (meta2 != null) {
                        val sliceI = (index % framesPerSlice).toInt()
                        return getVideoFrameWithoutGenerator(meta2, sliceI, bufferLength0, fps)
                    }
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
    ): GPUFrame? {
        if (index < 0) return null
        if (scale < 1) throw IllegalArgumentException("Scale must not be < 1")
        val bufferLength = max(1, bufferLength0)
        val bufferIndex = index / bufferLength
        // if scale >= 4 && width >= 200 create a smaller version in case using ffmpeg
        if (useProxy(scale, bufferLength0, getMeta(file, async))) {
            val slice0 = index / framesPerSlice
            val file2 = VideoProxyCreator.getProxyFile(file, slice0)
            if (file2 != null) {
                val sliceI = (index % framesPerSlice).toInt()
                return getVideoFrame(file2, (scale + 2) / 4, sliceI, bufferLength0, fps, timeout, async)
            }
        }
        return getFrame(file, scale, index, bufferIndex, bufferLength, fps, timeout, async)
    }


}