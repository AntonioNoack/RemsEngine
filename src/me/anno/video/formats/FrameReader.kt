package me.anno.video.formats

import me.anno.Engine
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.strings.StringHelper.shorten
import me.anno.video.ffmpeg.FFMPEGMetaParser
import me.anno.video.ffmpeg.FFMPEGMetaParser.Companion.invalidCodec
import me.anno.video.ffmpeg.FFMPEGStream
import me.anno.video.ffmpeg.IsFFMPEGOnly.isFFMPEGOnlyExtension
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream
import kotlin.concurrent.thread
import kotlin.math.abs

abstract class FrameReader<FrameType>(
    file: FileReference,
    val frame0: Int,
    val bufferLength: Int,
) : FFMPEGStream(file, isProcessCountLimited = !file.extension.isFFMPEGOnlyExtension()) {

    val frames = ArrayList<FrameType>(bufferLength)
    val parser = FFMPEGMetaParser()

    override fun process(process: Process, arguments: List<String>) {
        thread(name = "${file?.name}:error-stream") {
            val out = process.errorStream.bufferedReader()
            try {
                while (true) {
                    val line = out.readLine() ?: break
                    // if('!' in line || "Error" in line) LOGGER.warn("ffmpeg $frame0 ${arguments.joinToString(" ")}: $line")
                    parser.parseLine(line, this)
                }
            } catch (e: ShutdownException) {
                // ...
            }
        }

        try {
            val frameCount = bufferLength
            waitForMetadata()
            if (codec.isNotEmpty() && codec != invalidCodec) {
                val input = process.inputStream
                input.use {
                    readFrame(input)
                    if (!isFinished) for (i in 1 until frameCount) {
                        readFrame(input)
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
    }

    fun waitForMetadata() {
        var lt = System.nanoTime()
        waitUntil(true) {
            // if the last line is too long ago, e.g., because the source is not readable as an image, return
            val timeLimit = 30e9
            if (codec == invalidCodec) true
            else if (parser.lastLineTime != 0L && Engine.nanoTime - parser.lastLineTime > timeLimit) true
            else {
                val t = System.nanoTime()
                if (abs(t - lt) > 1e9) {
                    LOGGER.debug("Waiting for metadata on $file, $w x $h, $codec")
                    lt = t
                }
                w != 0 && h != 0 && codec.isNotEmpty()
            }
        }
    }

    // todo what do we do, if we run out of memory?
    private fun readFrame(input: InputStream) {
        synchronized(foundCodecs) {
            if (foundCodecs.add(codec)) {
                LOGGER.info("Found codec '$codec' in $file")
            }
        }
        if (!isDestroyed && !isFinished) {
            val frame = readFrame(w, h, input)
            if (frame != null) {
                synchronized(frames) {
                    frames.add(frame)
                }
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
        private val LOGGER = LogManager.getLogger(FrameReader::class.java)
    }

}