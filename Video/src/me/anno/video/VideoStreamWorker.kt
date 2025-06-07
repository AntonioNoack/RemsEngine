package me.anno.video

import me.anno.Engine
import me.anno.cache.IgnoredException
import me.anno.io.Streams.skipN
import me.anno.io.files.FileReference
import me.anno.io.files.SignatureCache
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.count2
import me.anno.video.ffmpeg.FFMPEGMetaParser
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

class VideoStreamWorker(file: FileReference, frameIndex0: Int, val id: Int, val self: VideoStream) :
    FFMPEGStream(file, false) {

    private val parser = FFMPEGMetaParser()
    private val reusableFrames = ArrayList<GPUFrame>()
    private var nextReadIndex = frameIndex0
    private var frameSizeForSkipping = 0L

    override fun process(process: Process, arguments: List<String>, callback: () -> Unit) {
        parseAsync(parser, process.errorStream)
        waitForMetadata(parser) {
            try {
                if (codec.isNotEmpty() && codec != FFMPEGMetaParser.invalidCodec) {
                    process.inputStream.use { stream ->
                        while (id == self.workerId.get() && !Engine.shutdown) {
                            loadNextFrameMaybe(stream)
                        }
                    }
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} cannot be read as image(s) by FFMPEG", file)
                }
            } catch (_: OutOfMemoryError) {
                LOGGER.warn("Engine has run out of memory!!")
            } catch (_: EOFException) {
            } catch (_: IgnoredException) {
            } catch (e: Exception) {
                e.printStackTrace()
            }
            destroy()
            callback()
        }
    }

    private fun readFrame(input: InputStream) {
        val currentIndex = nextReadIndex++
        val w = width
        val h = height
        val frame = reusableFrames.removeLastOrNull()
            ?: GPUFrameReader.createGPUFrame(w, h, currentIndex, codec, file)
        assertEquals(w, frame.width)
        assertEquals(h, frame.height)
        frameSizeForSkipping = frame.getByteSize()
        try {
            frame.load(input) {}
        } catch (e: Exception) {
            reusableFrames.add(frame)
            throw e
        }
        frame.frameIndex = currentIndex
        val sortedFrames = self.sortedFrames
        synchronized(sortedFrames) {
            if (id == self.workerId.get()) {
                // remove everything that is too new
                val tooNew = sortedFrames.count2 { it.frameIndex >= currentIndex }
                val tooOldIndex = nextReadIndex - self.capacity
                val tooOld = sortedFrames.count2 { it.frameIndex < tooOldIndex }
                if (tooNew > 0) removeFrames(sortedFrames.subList(sortedFrames.size - tooNew, sortedFrames.size))
                if (tooOld > 0) removeFrames(sortedFrames.subList(0, tooOld))
                // then append the new frame
                sortedFrames.add(frame)
            } else frame.destroy()
        }
    }

    private fun removeFrames(oldFrames: MutableList<GPUFrame>) {
        for (oldFrame in oldFrames) {
            if (oldFrame.width == width && oldFrame.height == height) {
                reusableFrames.add(oldFrame)
            } else oldFrame.destroy()
        }
        oldFrames.clear()
    }

    private fun skipFrame(input: InputStream) {
        input.skipN(frameSizeForSkipping)
        nextReadIndex++
    }

    override fun destroy() {
        // clear old frames
        for (frame in reusableFrames) {
            frame.destroy()
        }
        reusableFrames.clear()
    }

    private fun loadNextFrameMaybe(input: InputStream) {
        // if we're too slow, trash a few frames
        if (frameSizeForSkipping > 0) {
            for (i in nextReadIndex until self.lastRequestedFrame) {
                skipFrame(input)
            }
        }
        if (nextReadIndex < self.lastRequestedFrame + self.capacity) {
            readFrame(input)
        } else {
            // we're too fast
            Thread.sleep(0)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoStreamWorker::class)
        fun runVideoStreamWorker(
            self: VideoStream, id: Int,
            frameIndex0: Int, maxNumFrames: Int,
            fps: Double, maxSize: Int
        ) {
            val file = self.file
            val meta = self.meta
            SignatureCache.getAsync(file) { signature ->
                // scale video as needed
                val scale = clamp(maxSize.toDouble() / max(meta.videoWidth, meta.videoHeight).toDouble())
                val w0 = max((scale * meta.videoWidth).toInt(), 2)
                val h0 = max((scale * meta.videoHeight).toInt(), 2)
                val w1 = w0 - w0.and(1)
                val h1 = h0 - h0.and(1)
                val threadName = "Stream/$id/${file.name}"
                val args = getImageSequenceArguments(
                    file, signature?.name, w1, h1,
                    frameIndex0 / fps,
                    maxNumFrames, fps,
                    meta.videoWidth, meta.videoFPS,
                    meta.videoFrameCount
                )
                VideoStreamWorker(file, frameIndex0, id, self).runAsync(threadName, args)
            }
        }
    }
}