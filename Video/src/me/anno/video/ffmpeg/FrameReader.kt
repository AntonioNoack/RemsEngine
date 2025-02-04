package me.anno.video.ffmpeg

import me.anno.cache.IgnoredException
import me.anno.io.files.FileReference
import me.anno.utils.types.Strings.shorten
import me.anno.video.ffmpeg.FFMPEGMetaParser.Companion.invalidCodec
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

abstract class FrameReader<FrameType>(
    file: FileReference,
    val frame0: Int,
    val bufferLength: Int,
    val nextFrameCallback: (FrameType) -> Unit,
    val finishedCallback: (List<FrameType>) -> Unit
) : FFMPEGStream(file, isProcessCountLimited = !file.extension.isFFMPEGOnlyExtension()) {

    val frames = ArrayList<FrameType>(bufferLength)
    val parser = FFMPEGMetaParser()

    override fun process(process: Process, arguments: List<String>, callback: () -> Unit) {
        parseAsync(parser, process.errorStream)
        waitForMetadata(parser) {
            try {
                val frameCount = bufferLength
                if (codec.isNotEmpty() && codec != invalidCodec) {
                    val input = process.inputStream
                    var frameIndex = frame0
                    input.use { input1: InputStream ->
                        readFrame(frameIndex++, input1)
                        if (!isFinished) {
                            for (i in 1 until frameCount) {
                                readFrame(frameIndex++, input1)
                                if (isFinished) break
                            }
                            isFinished = true
                        }
                    }
                } else LOGGER.debug("${file?.absolutePath?.shorten(200)} cannot be read as image(s) by FFMPEG")
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Engine has run out of memory!!")
            } catch (e: EOFException) {
                isFinished = true
            } catch (_: IgnoredException) {
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finishedCallback(frames)
            callback()
        }
    }

    // what do we do, if we run out of memory?
    // - from the start, we reuse memory as well as possible,
    // - if we're just streaming, use a class like VideoPanel
    // - if we're out of memory anyway, we'll just be unlucky... memory is cheap today

    private fun readFrame(frameIndex: Int, input: InputStream) {
        synchronized(foundCodecs) {
            if (foundCodecs.add(codec)) {
                LOGGER.info("Found codec '$codec' in $file")
            }
        }
        if (!isDestroyed && !isFinished) {
            val frame = readFrame(width, height, frameIndex, input)
            if (frame != null) {
                synchronized(frames) {
                    frames.add(frame)
                }
                nextFrameCallback(frame)
            } else onError()
        }
        if (isDestroyed) destroy()
    }

    abstract fun readFrame(w: Int, h: Int, frameIndex: Int, input: InputStream): FrameType?

    private fun onError() {
        frameCountByFile[file!!] = frames.size + frame0
        isFinished = true
    }

    var isFinished = false
    var isDestroyed = false

    companion object {
        @JvmStatic
        private val foundCodecs = HashSet<String>()

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FrameReader::class)

        fun String.isFFMPEGOnlyExtension() = equals("webp", true)// || equals("jp2", true)
    }
}