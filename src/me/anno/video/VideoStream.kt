package me.anno.video

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.cache.ICacheData
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.maths.MinMax.max
import me.anno.utils.structures.lists.Lists.any2
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger

open class VideoStream(
    val file: FileReference, val meta: MediaMetadata,
    var playAudio: Boolean, var looping: LoopingState,
    val fps: Double, var maxSize: Int,
    // configurable number of kept frames for Rem's Studio, so we can do blank-frame filtering and frame-interpolation
    val capacity: Int = 16,
) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(VideoStream::class)
        var runVideoStreamWorker: ((self: VideoStream, id: Int, frameIndex0: Int, maxNumFrames: Int, fps: Double, maxSize: Int) -> Unit)? =
            null
    }

    var isPlaying = false
        private set

    var audio: AudioFileStreamOpenAL? = null
        private set

    private var startTimeNanos = 0L
    private var pausedTimeNanos = 0L

    val sortedFrames = ArrayList<GPUFrame>()
    val workerId = AtomicInteger(0)
    var lastRequestedFrame = 0

    fun togglePlaying() {
        val time = getLoopingTimeSeconds()
        if (isPlaying) { // regular stop
            stop()
            skipTo(time) // reset time just in case
        } else {
            stop() // kill last worker just in case
            val isAtEnd = time >= meta.videoDuration * (1.0 - 1e-8)
            start(if (isAtEnd) 0.0 else time)
        }
    }

    fun start(time: Double = getLoopingTimeSeconds()) {
        if (isPlaying) return
        val frameIndex0 = (time * fps).toInt()
        LOGGER.info("Starting $this at #$frameIndex0/${meta.videoFrameCount}")
        isPlaying = true
        startTimeNanos = (Time.nanoTime - time * 1e9).toLong()
        lastRequestedFrame = frameIndex0
        startWorker(frameIndex0, meta.videoFrameCount - frameIndex0)
        if (playAudio) {
            startAudio()
        }
    }

    fun startWorker(frameIndex0: Int, maxNumFrames: Int) {
        val id = workerId.incrementAndGet()
        runVideoStreamWorker?.invoke(this, id, frameIndex0, maxNumFrames, fps, maxSize)
    }

    fun stop() {
        LOGGER.info("Stopping $this at #$lastRequestedFrame")
        workerId.incrementAndGet() // just in case execute this always
        if (!isPlaying) return
        pausedTimeNanos = (getLoopingTimeSeconds() * 1e9).toLong()
        isPlaying = false
        stopAudio()
    }

    fun startAudio() {
        val audio = AudioFileStreamOpenAL(
            file, looping, getLoopingTimeSeconds(), false, meta, 1.0,
            left = true, center = false, right = true
        )
        this.audio?.stop()
        this.audio = audio
        audio.start()
    }

    fun stopAudio() {
        audio?.stop()
        audio = null
    }

    fun skipTo(time: Double) {
        if (isPlaying) {
            stop()
            start(time)
        } else {
            pausedTimeNanos = (time * 1e9).toLong()
            // only start worker, if current frame cannot be found
            if (!hasCurrentFrame()) {
                startWorker(getFrameIndex(), 1)
            }
        }
    }

    fun hasCurrentFrame(): Boolean {
        val frameIndex = getFrameIndex()
        return synchronized(sortedFrames) {
            sortedFrames.any2 { it.frameIndex == frameIndex && !it.isDestroyed }
        }
    }

    fun getLoopingTimeSeconds(): Double {
        val rawTimeSeconds = max(
            if (isPlaying) {
                (Time.nanoTime - startTimeNanos)
            } else {
                pausedTimeNanos
            }, 0L
        ) * 1e-9
        return looping[rawTimeSeconds, meta.videoDuration]
    }

    open fun getFrameIndex(): Int {
        return looping[(getLoopingTimeSeconds() * fps).toInt(), meta.videoFrameCount - 1]
    }

    fun getFrame(): GPUFrame? {
        return getFrame(getFrameIndex())
    }

    fun getFrame(frameIndex: Int, numExtraImages: Int = 0): GPUFrame? {
        if (isPlaying) {
            if (frameIndex == meta.videoFrameCount - 1 && looping == LoopingState.PLAY_ONCE) {
                stop()
            }
            if (frameIndex < lastRequestedFrame && looping != LoopingState.PLAY_ONCE) {
                // restart is needed
                // todo don't discard audio?
                stop()
                val startIndex = max(frameIndex - numExtraImages, 0)
                start(startIndex / fps)
                return getFrame(frameIndex)
            }
        }
        lastRequestedFrame = max(frameIndex - numExtraImages, 0)
        return synchronized(sortedFrames) {
            findBestFrame(frameIndex)
        }
    }

    private fun findBestFrame(idealFrameIndex: Int): GPUFrame? {
        val sorted = sortedFrames
        var bestFrame: GPUFrame? = null
        // could theoretically be binary search, but that doesn't matter here,
        // because |sortedFrames| is typically just 7 or 16 or so, so binary search is not worth it
        for (i in sorted.indices) {
            val frame = sorted[i]
            if (!frame.isCreated) continue
            if (frame.frameIndex <= idealFrameIndex) {
                bestFrame = frame
            } else if (bestFrame != null) {
                // if a frame from the past is available, use it
                return bestFrame
            } else {
                // this is the first frame after that's created -> use it
                return frame
            }
        }
        return bestFrame
    }

    override fun destroy() {
        stop()
        synchronized(sortedFrames) {
            for (fi in sortedFrames.indices) {
                sortedFrames[fi].destroy()
            }
            sortedFrames.clear()
        }
    }

    override fun toString(): String {
        return "VideoStream { $file, $fps fps, $looping, $maxSize size, audio? $audio }"
    }
}
