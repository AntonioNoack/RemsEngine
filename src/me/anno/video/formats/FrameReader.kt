package me.anno.video.formats

import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.strings.StringHelper.shorten
import me.anno.video.ffmpeg.FFMPEGMetaParser
import me.anno.video.ffmpeg.FFMPEGMetaParser.Companion.invalidCodec
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.ffmpeg.IsFFMPEGOnly.isFFMPEGOnlyExtension
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

    override fun process(process: Process, vararg arguments: String) {
        parseAsync(parser, process.errorStream)
        try {
            val frameCount = bufferLength
            waitForMetadata(parser)
            if (codec.isNotEmpty() && codec != invalidCodec) {
                val input = process.inputStream
                input.use { input1: InputStream ->
                    readFrame(input1)
                    if (!isFinished) for (i in 1 until frameCount) {
                        readFrame(input1)
                        if (isFinished) break
                    }
                }
            } else LOGGER.debug("${file?.absolutePath?.shorten(200)} cannot be read as image(s) by FFMPEG")
        } catch (e: OutOfMemoryError) {
            LOGGER.warn("Engine has run out of memory!!")
        } catch (e: EOFException) {
            isFinished = true
        } catch (e: ShutdownException) {
            // ...
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finishedCallback(frames)
    }

    // what do we do, if we run out of memory?
    // - from the start, we reuse memory as well as possible,
    // - if we're just streaming, use a class like VideoPanel
    // - if we're out of memory anyway, we'll just be unlucky... memory is cheap today

    private fun readFrame(input: InputStream) {
        synchronized(foundCodecs) {
            if (foundCodecs.add(codec)) {
                LOGGER.info("Found codec '$codec' in $file")
            }
        }
        if (!isDestroyed && !isFinished) {
            val frame = readFrame(width, height, input)
            if (frame != null) {
                synchronized(frames) {
                    frames.add(frame)
                }
                nextFrameCallback(frame)
            } else onError()
        }
        if (isDestroyed) destroy()
    }

    abstract fun readFrame(w: Int, h: Int, input: InputStream): FrameType?

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
    }
}