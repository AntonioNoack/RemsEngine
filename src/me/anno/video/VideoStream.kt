package me.anno.video

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.streams.AudioFileStreamOpenAL
import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.utils.ShutdownException
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.InputStreams.skipN
import me.anno.video.ffmpeg.FFMPEGMetaParser
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.ffmpeg.MediaMetadata
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream
import kotlin.concurrent.thread

class VideoStream(
    val file: FileReference, val meta: MediaMetadata,
    var playAudio: Boolean, var looping: LoopingState
) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(VideoStream::class)
    }

    var isPlaying = false
        private set

    var audio: AudioFileStreamOpenAL? = null

    private var startTime = 0L
    private var standTime = 0L

    private val capacity = 16
    private val sortedFrames = ArrayList<Pair<Int, GPUFrame>>()
    private var lastRequestedFrame = 0
    private var workerId = 0

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
        thread(name = "Stream/$workerId/${file.name}") {
            val signature = Signature.findNameSync(file)
            val process = object : FFMPEGStream(file, false) {
                val parser = FFMPEGMetaParser()
                val oldFrames = ArrayList<GPUFrame>()
                var nextReadIndex = frameIndex0
                override fun process(process: Process, vararg arguments: String) {
                    parseAsync(parser, process.errorStream)
                    try {
                        waitForMetadata(parser)
                        if (codec.isNotEmpty() && codec != FFMPEGMetaParser.invalidCodec) {
                            val input = process.inputStream
                            input.use {
                                readFrame(it)
                                while (id == workerId) {
                                    loadNextFrameMaybe(it)
                                }
                            }
                        } else LOGGER.debug("${file?.absolutePath?.shorten(200)} cannot be read as image(s) by FFMPEG")
                    } catch (e: OutOfMemoryError) {
                        LOGGER.warn("Engine has run out of memory!!")
                    } catch (ignored: EOFException) {
                    } catch (ignored: ShutdownException) {
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // clear old frames
                    for (frame in oldFrames) {
                        frame.destroy()
                    }
                    oldFrames.clear()
                }

                var frameSize = 0L
                fun readFrame(input: InputStream) {
                    val currentIndex = nextReadIndex++
                    val w = width
                    val h = height
                    val frame = oldFrames.removeLastOrNull() ?: run {
                        GPUFrameReader.createGPUFrame(w, h, codec, file)
                    }
                    frameSize = frame.getByteSize()
                    try {
                        frame.load(input)
                    } catch (e: Exception) {
                        oldFrames.add(frame)
                        throw e
                    }
                    synchronized(sortedFrames) {
                        if (id == workerId) {
                            // remove everything that is too new
                            val tooNew = sortedFrames.count { it.first >= nextReadIndex }
                            val oldFrames = sortedFrames.subList(sortedFrames.size - tooNew, sortedFrames.size)
                            for (oldFrame in oldFrames) this.oldFrames.add(oldFrame.second)
                            oldFrames.clear()
                            // then append the new frame
                            sortedFrames.add(currentIndex to frame)
                        } else oldFrames.add(frame)
                    }
                }

                fun skipFrame(input: InputStream) {
                    input.skipN(frameSize)
                    nextReadIndex++
                }

                override fun destroy() {}
                fun loadNextFrameMaybe(input: InputStream) {
                    // pop old frames
                    synchronized(sortedFrames) {
                        // throw away everything that is too old
                        val goodFrames = sortedFrames.count { it.first <= lastRequestedFrame }
                        val oldFrames = goodFrames - 1
                        if (oldFrames > 0) {
                            val toRemove = sortedFrames.subList(0, oldFrames)
                            for (frame in toRemove) this.oldFrames.add(frame.second)
                            toRemove.clear()
                        }
                    }
                    // if we're too slow, trash a few frames
                    for (i in nextReadIndex until lastRequestedFrame) {
                        skipFrame(input)
                    }
                    if (nextReadIndex < lastRequestedFrame + capacity) {
                        readFrame(input)
                    } else {
                        // we're too fast
                        Thread.sleep(0)
                    }
                }
            }
            process.run(
                *FFMPEGStream.getImageSequenceArguments(
                    file, signature, meta.videoWidth, meta.videoHeight,
                    frameIndex0 / meta.videoFPS,
                    maxNumFrames, meta.videoFPS,
                    meta.videoWidth, meta.videoFPS,
                    meta.videoFrameCount
                ).toTypedArray()
            )
        }
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
