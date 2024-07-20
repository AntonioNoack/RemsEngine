package me.anno.video

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.cache.ICacheData
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.maths.Maths.max
import me.anno.utils.structures.lists.Lists.any2
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicInteger

// todo configurable number of kept frames for Rem's Studio, so we can do blank-frame filtering and frame-interpolation
open class VideoStream(
    val file: FileReference, val meta: MediaMetadata,
    var playAudio: Boolean, var looping: LoopingState,
    val fps: Double, var maxSize: Int,
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

    private var startTime = 0L
    private var standTime = 0L

    val sortedFrames = ArrayList<Pair<Int, GPUFrame>>()
    var lastRequestedFrame = 0
    val workerId = AtomicInteger(0)

    fun togglePlaying() {
        val time = getTime()
        if (isPlaying) {
            stop()
            skipTo(time)
        } else {
            stop()
            val isAtEnd = time >= meta.videoDuration * 0.999
            start(if (isAtEnd) 0.0 else time)
        }
    }

    fun start(time: Double = getTime()) {
        if (isPlaying) return
        val frameIndex0 = (time * fps).toInt()
        LOGGER.info("Starting $this at #$frameIndex0/${meta.videoFrameCount}")
        isPlaying = true
        startTime = (Time.nanoTime - time * 1e9).toLong()
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
        standTime = (getTime() * 1e9).toLong()
        isPlaying = false
        stopAudio()
    }

    fun startAudio() {
        val audio = AudioFileStreamOpenAL(
            file, looping, getTime(), false, meta, 1.0,
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
            standTime = (time * 1e9).toLong()
            // only start worker, if current frame cannot be found
            if (!hasCurrentFrame()) {
                startWorker(getFrameIndex(), 1)
            }
        }
    }

    fun hasCurrentFrame(): Boolean {
        val frameIndex = getFrameIndex()
        return synchronized(sortedFrames) {
            sortedFrames.any2 { it.first == frameIndex && !it.second.isDestroyed }
        }
    }

    fun getTime(): Double {
        val time0 = max(
            if (isPlaying) {
                (Time.nanoTime - startTime)
            } else {
                standTime
            }, 0L
        ) * 1e-9
        return looping[time0, meta.videoDuration]
    }

    open fun getFrameIndex(): Int {
        return looping[(getTime() * fps).toInt(), meta.videoFrameCount - 1]
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
            val goodFrames = sortedFrames
                .filter { it.first <= frameIndex && it.second.isCreated }
                .maxByOrNull { it.first }
            goodFrames ?: sortedFrames.firstOrNull { it.second.isCreated }
        }?.second
    }

    override fun destroy() {
        stop()
        synchronized(sortedFrames) {
            for (frame in sortedFrames) {
                frame.second.destroy()
            }
            sortedFrames.clear()
        }
    }

    override fun toString(): String {
        return "VideoStream { $file, $fps fps, $looping, $maxSize size, audio? $audio }"
    }
}
