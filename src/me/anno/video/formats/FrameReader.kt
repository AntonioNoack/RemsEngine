package me.anno.video.formats

import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.waitUntil
import me.anno.video.FFMPEGMetaParser
import me.anno.video.FFMPEGStream
import me.anno.video.IsFFMPEGOnly.isFFMPEGOnlyExtension
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import kotlin.concurrent.thread

abstract class FrameReader<FrameType>(
    file: FileReference,
    val frame0: Int,
    bufferLength: Int,
) : FFMPEGStream(file, isProcessCountLimited = !file.extension.isFFMPEGOnlyExtension()) {

    val frames = ArrayList<FrameType>(bufferLength)

    override fun process(process: Process, arguments: List<String>) {
        thread(name = "${file?.name}:error-stream") {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
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
        thread(name = "${file?.name}:input-stream") {
            try {
                val frameCount = arguments[arguments.indexOf("-vframes") + 1].toInt()
                val input = process.inputStream
                input.use {
                    readFrame(input)
                    for (i in 1 until frameCount) {
                        readFrame(input)
                        if (isFinished) break
                    }
                }
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Engine has run out of memory!!")
            } catch (e: ShutdownException) {
                // ...
            }
        }
    }

    // todo what do we do, if we run out of memory?
    private fun readFrame(input: InputStream) {
        waitUntil(true) { w != 0 && h != 0 && codec.isNotEmpty() }
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
        private val foundCodecs = HashSet<String>()
        private val LOGGER = LogManager.getLogger(FrameReader::class.java)
    }

}