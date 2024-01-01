package me.anno.video

import me.anno.Time
import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.maths.Maths
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

// todo play audio
class VideoStream(val file: FileReference, val meta: MediaMetadata, val playAudio: Boolean) : ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(VideoStream::class)
    }

    var isPlaying = false
        private set

    private var startTime = 0L
    private var standTime = 0L

    private val capacity = 16
    private val sortedFrames = ArrayList<Pair<Int, GPUFrame>>()
    private var lastRequestedFrame = 0
    private var workerId = 0

    fun start(time: Double = getTime()) {
        if (isPlaying) return
        isPlaying = true
        startTime = (Time.nanoTime - time * 1e9).toLong()
        val frameIndex0 = getFrameIndex()
        lastRequestedFrame = frameIndex0
        startWorker(frameIndex0, meta.videoFrameCount - frameIndex0)
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
                                    waitForNextFrame(it)
                                }
                            }
                        } else LOGGER.debug("${file?.absolutePath?.shorten(200)} cannot be read as image(s) by FFMPEG")
                    } catch (e: OutOfMemoryError) {
                        LOGGER.warn("Engine has run out of memory!!")
                    } catch (e: EOFException) {
                        if (id == workerId) stop()
                    } catch (e: ShutdownException) {
                        // ...
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
                    val frame = oldFrames.removeLastOrNull() ?: GPUFrameReader.createGPUFrame(w, h, codec, file)
                    frameSize = frame.getByteSize()
                    frame.load(input)
                    synchronized(sortedFrames) {
                        if (id == workerId) {
                            // remove everything that is too new
                            val tooNew = sortedFrames.count { it.first >= nextReadIndex }
                            val oldFrames = sortedFrames.subList(sortedFrames.size - tooNew, sortedFrames.size)
                            for (oldFrame in oldFrames) this.oldFrames.add(oldFrame.second)
                            oldFrames.clear()
                            // then append the new frame
                            sortedFrames.add(currentIndex to frame)
                        }
                    }
                }

                fun skipFrame(input: InputStream) {
                    input.skipN(frameSize)
                    nextReadIndex++
                }

                override fun destroy() {}
                fun waitForNextFrame(input: InputStream) {
                    // pop old frames
                    synchronized(sortedFrames) {
                        // throw away everything that is too old
                        val goodFrames = sortedFrames.count { it.first <= lastRequestedFrame }
                        val oldFrames = goodFrames - 1
                        if (oldFrames > 0) {
                            sortedFrames.subList(0, oldFrames).clear()
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
    }

    fun skipTo(time: Double) {
        if (isPlaying) {
            stop()
            start(time)
        } else {
            standTime = (time * 1e9).toLong()
            startWorker(getFrameIndex(), 1)
        }
    }

    fun getTime(): Double {
        return if (isPlaying) {
            (Time.nanoTime - startTime)
        } else {
            standTime
        } * 1e-9
    }

    private fun getFrameIndex(): Int {
        return Maths.clamp((getTime() * meta.videoFPS).toInt(), 0, meta.videoFrameCount - 1)
    }

    fun getFrame(): GPUFrame? {
        val index = getFrameIndex()
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
    }
}
