package me.anno.video

import me.anno.cache.IgnoredException
import me.anno.io.files.Signature
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.utils.types.Strings.shorten
import me.anno.io.Streams.skipN
import me.anno.video.ffmpeg.FFMPEGMetaParser
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream
import kotlin.concurrent.thread

object VideoStreamWorker {
    private val LOGGER = LogManager.getLogger(VideoStreamWorker::class)
    fun runVideoStreamWorker(self: VideoStream, id: Int, frameIndex0: Int, maxNumFrames: Int, maxSize: Int) {
        val file = self.file
        val meta = self.meta
        thread(name = "Stream/$id/${file.name}") {
            val signature = Signature.findNameSync(file)
            val process = object : FFMPEGStream(file, false) {
                val parser = FFMPEGMetaParser()
                val oldFrames = ArrayList<GPUFrame>()
                var nextReadIndex = frameIndex0
                override fun process(process: Process, arguments: List<String>) {
                    parseAsync(parser, process.errorStream)
                    try {
                        waitForMetadata(parser)
                        if (codec.isNotEmpty() && codec != FFMPEGMetaParser.invalidCodec) {
                            val input = process.inputStream
                            input.use {
                                readFrame(it)
                                while (id == self.workerId.get()) {
                                    loadNextFrameMaybe(it)
                                }
                            }
                        } else LOGGER.debug("${file.absolutePath.shorten(200)} cannot be read as image(s) by FFMPEG")
                    } catch (e: OutOfMemoryError) {
                        LOGGER.warn("Engine has run out of memory!!")
                    } catch (_: EOFException) {
                    } catch (_: IgnoredException) {
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
                    val sortedFrames = self.sortedFrames
                    synchronized(sortedFrames) {
                        if (id == self.workerId.get()) {
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
                    val sortedFrames = self.sortedFrames
                    synchronized(sortedFrames) {
                        // throw away everything that is too old
                        val goodFrames = sortedFrames.count { it.first <= self.lastRequestedFrame }
                        val oldFrames = goodFrames - 1
                        if (oldFrames > 0) {
                            val toRemove = sortedFrames.subList(0, oldFrames)
                            for (frame in toRemove) this.oldFrames.add(frame.second)
                            toRemove.clear()
                        }
                    }
                    // if we're too slow, trash a few frames
                    for (i in nextReadIndex until self.lastRequestedFrame) {
                        skipFrame(input)
                    }
                    if (nextReadIndex < self.lastRequestedFrame + self.capacity) {
                        readFrame(input)
                    } else {
                        // we're too fast
                        Thread.sleep(0)
                    }
                }
            }
            // scale video as needed
            val scale = clamp(maxSize.toDouble() / max(meta.videoWidth, meta.videoHeight).toDouble() )
            val w0 = max((scale * meta.videoWidth).toInt(), 2)
            val h0 = max((scale * meta.videoHeight).toInt(), 2)
            val w1 = w0 - w0.and(1)
            val h1 = h0 - h0.and(1)
            process.run(
                FFMPEGStream.getImageSequenceArguments(
                    file, signature, w1, h1,
                    frameIndex0 / meta.videoFPS,
                    maxNumFrames, meta.videoFPS,
                    meta.videoWidth, meta.videoFPS,
                    meta.videoFrameCount
                )
            )
        }
    }
}