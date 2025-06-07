package me.anno.video.ffmpeg

import me.anno.cache.IgnoredException
import me.anno.io.files.FileReference
import me.anno.utils.Sleep
import me.anno.video.ffmpeg.FFMPEGMetaParser.Companion.invalidCodec
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

abstract class FrameReader<FrameType>(
    file: FileReference,
    val frame0: Int,
    val bufferLength: Int,
    val nextFrameCallback: (FrameType) -> Unit,
    val finishedCallback: (List<FrameType>) -> Unit
) : FFMPEGStream(file, isProcessCountLimited = true) {

    val frames = ArrayList<FrameType>(bufferLength)
    val parser = FFMPEGMetaParser()

    override fun process(process: Process, arguments: List<String>, callback: () -> Unit) {
        parseAsync(parser, process.errorStream)
        waitForMetadata(parser) {
            try {
                if (codec.isNotEmpty() && codec != invalidCodec) {
                    process.inputStream.use { stream ->
                        for (frameIndex in frame0 until frame0 + bufferLength) {
                            readFrame(frameIndex, stream)
                            if (isDestroyed) break
                        }
                    }
                } else LOGGER.debug("{} cannot be read as image(s) by FFMPEG", file)
            } catch (_: OutOfMemoryError) {
                LOGGER.warn("Engine has run out of memory!!")
            } catch (_: EOFException) {
            } catch (_: IgnoredException) {
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFinished = true
            }
            finishedCallback(frames)
            callback()
        }
    }

    // this limiter shall prevent the CPU reading tons of images before the GPU can process them
    private val limiter = AtomicInteger(0)

    private fun readFrame(frameIndex: Int, input: InputStream) {
        // load 3 frames concurrently max
        val limit = max(maxFramesConcurrently, 1)
        Sleep.waitUntil(true) { limiter.get() < limit || isDestroyed || isFinished }
        if (isDestroyed || isFinished) return

        limiter.addAndGet(1)
        readFrame(width, height, frameIndex, input) { frame ->
            limiter.addAndGet(-1)
            if (frame != null) {
                synchronized(frames) {
                    frames.add(frame)
                }
                nextFrameCallback(frame)
            } else onError()
            if (isDestroyed) destroy()
        }
    }

    abstract fun readFrame(w: Int, h: Int, frameIndex: Int, input: InputStream, callback: (FrameType?) -> Unit)

    private fun onError() {
        frameCountByFile[file!!] = frames.size + frame0
        isFinished = true
    }

    companion object {

        var maxFramesConcurrently = 3

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FrameReader::class)
    }
}