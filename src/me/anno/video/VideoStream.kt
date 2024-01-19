package me.anno.video

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.MediaMetadata
import me.anno.video.formats.gpu.GPUFrame

class VideoStream(
    val file: FileReference, val meta: MediaMetadata,
    var playAudio: Boolean, var looping: LoopingState
) : ICacheData {

    companion object {
        var runVideoStreamWorker: ((self: VideoStream, id: Int, frameIndex0: Int, maxNumFrames: Int) -> Unit)? = null
    }

    var isPlaying = false
        private set

    var audio: AudioFileStreamOpenAL? = null

    private var startTime = 0L
    private var standTime = 0L

    val capacity = 16
    val sortedFrames = ArrayList<Pair<Int, GPUFrame>>()
    var lastRequestedFrame = 0
    var workerId = 0

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
        isPlaying = true
        startTime = (Time.nanoTime - time * 1e9).toLong()
        val frameIndex0 = getFrameIndex()
        lastRequestedFrame = frameIndex0
        startWorker(frameIndex0, meta.videoFrameCount - frameIndex0)
        if (playAudio) {
            startAudio()
        }
    }

    fun startWorker(frameIndex0: Int, maxNumFrames: Int) {
        val id = ++workerId
        runVideoStreamWorker?.invoke(this, id, frameIndex0, maxNumFrames)
    }

    fun stop() {
        if (!isPlaying) return
        ++workerId
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
            sortedFrames.any { it.first == frameIndex && !it.second.isDestroyed }
        }
    }

    fun getTime(): Double {
        val time0 = if (isPlaying) {
            (Time.nanoTime - startTime)
        } else {
            standTime
        } * 1e-9
        return looping[time0, meta.videoDuration]
    }

    private fun getFrameIndex(): Int {
        return looping[(getTime() * meta.videoFPS).toInt(), meta.videoFrameCount - 1]
    }

    fun getFrame(): GPUFrame? {
        val index = getFrameIndex()
        if (isPlaying) {
            if (index == meta.videoFrameCount - 1 && looping == LoopingState.PLAY_ONCE) {
                stop()
            }
            if (index < lastRequestedFrame && looping != LoopingState.PLAY_ONCE) {
                stop()
                start() // todo don't discard audio?
                lastRequestedFrame = 0
                return getFrame()
            }
        }
        lastRequestedFrame = index
        return synchronized(sortedFrames) {
            val goodFrames = sortedFrames
                .filter { it.first <= index && it.second.isCreated }
                .maxByOrNull { it.first }
            goodFrames ?: sortedFrames.firstOrNull { it.second.isCreated }
        }?.second
    }

    init {
        skipTo(0.0)
    }

    override fun destroy() {
        workerId++
        synchronized(sortedFrames) {
            for (frame in sortedFrames) {
                frame.second.destroy()
            }
            sortedFrames.clear()
        }
    }
}
