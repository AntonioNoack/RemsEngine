package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.acquire
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.hpc.HeavyProcessing.numThreads
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.types.Floats.f3
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta
import me.anno.video.formats.cpu.CPUFrameReader
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// ffmpeg requires 100 MB RAM per instance -> do we really need multiple instances, or does one work fine
// done keep only a certain amount of ffmpeg instances running
abstract class FFMPEGStream(val file: FileReference?, val isProcessCountLimited: Boolean) {

    companion object {

        // could be limited by memory as well...
        // to help to keep the memory and cpu-usage below 100%
        // 5 GB = 50 processes, at 6 cores / 12 threads = 4 ratio
        val processLimiter = Semaphore(max(2, numThreads), true)
        private val LOGGER = LogManager.getLogger(FFMPEGStream::class)
        val frameCountByFile = HashMap<FileReference, Int>()
        val waitingQueue = ProcessingQueue("WaitingQueue")

        fun getInfo(input: FileReference) = (FFMPEGMeta(null)
            .run(listOf("-i", input.absolutePath)) as FFMPEGMeta).stringData

        fun getSupportedFormats() = (FFMPEGMeta(null)
            .run(listOf("-formats")) as FFMPEGMeta).stringData

        fun getImageSequence(
            input: FileReference, w: Int, h: Int, startFrame: Int, frameCount: Int, fps: Double
        ): GPUFrameReader {
            return getImageSequence(input, w, h, startFrame / fps, frameCount, fps)
        }

        // ffmpeg needs to fetch hardware decoded frames (-hwaccel auto) from gpu memory;
        // if we use hardware decoding, we need to use it on the gpu...
        fun getImageSequence(
            input: FileReference, w: Int, h: Int, startTime: Double, frameCount: Int, fps: Double
        ): GPUFrameReader {
            val video = GPUFrameReader(input, (startTime * fps).roundToInt(), frameCount)
            video.run(getImageSequenceArguments(input, w, h, startTime, frameCount, fps))
            return video
        }

        fun getImageSequenceCPU(
            input: FileReference, w: Int, h: Int, frameIndex: Int, frameCount: Int, fps: Double
        ): CPUFrameReader {
            val video = CPUFrameReader(input, frameIndex, frameCount)
            video.run(getImageSequenceArguments(input, w, h, frameIndex / max(fps, 1e-3), frameCount, fps))
            return video
        }

        fun getImageSequenceArguments(
            input: FileReference, w: Int, h: Int, startTime: Double, frameCount: Int, fps: Double
        ): List<String> {
            val meta = getMeta(input, false)
            val args = arrayListOf(
                "-ss", "$startTime", // must be placed here!!!
                "-i", input.absolutePath
            )
            if (abs(fps - (meta?.videoFPS ?: 0.0001)) > 0.01) {
                // 2x slower
                args += listOf("-r", "$fps")
            }
            if (meta?.videoWidth != w) {
                args += listOf("-vf", "scale=$w:$h")
            }
            args += listOf(
                "-vframes", "$frameCount",
                // "-movflags", "faststart", // didn't have noticeable effect, maybe it does now (??...)
                "-f", "rawvideo", "-" // format
            )
            return args
        }

        fun getAudioSequence(input: FileReference, startTime: Double, duration: Double, sampleRate: Int) =
            FFMPEGAudio(input, sampleRate, duration).run(
                listOf(
                    "-ss", "$startTime", // important!!!
                    "-i", input.absolutePath,
                    "-t", "$duration", // duration
                    "-ar", "$sampleRate",
                    // -aq quality, codec specific
                    "-f", "wav",
                    // the -bitexact tag doesn't exist on my Linux ffmpeg :(, and ffmpeg just still adds the info block
                    // -> we need to remove it
                    // "-bitexact", // don't add an additional LIST-INFO chunk; we don't care
                    // wav is exported with length -1, which slick does not support
                    // ogg reports "error 34", and ffmpeg is slow
                    // "-c:a", "pcm_s16le", "-ac", "2",
                    "-"
                    // "pipe:1" // 1 = stdout, 2 = stdout
                )
            ) as FFMPEGAudio

        fun logOutput(prefix: String?, stream: InputStream, warn: Boolean) {
            val reader = stream.bufferedReader()
            thread(name = "LogOutput") {
                while (true) {
                    val line = reader.readLine() ?: break
                    val lineWithPrefix = if (prefix == null) line else "[$prefix] $line"
                    if (warn) {
                        LOGGER.warn(lineWithPrefix)
                    } else {
                        LOGGER.info(lineWithPrefix)
                    }
                }
            }
        }

    }

    var sourceFPS = -1.0
    var sourceLength = 0.0

    var codec = ""

    var w = 0
    var h = 0

    var srcW = 0
    var srcH = 0

    abstract fun process(process: Process, arguments: List<String>)

    abstract fun destroy()

    fun run(arguments: List<String>): FFMPEGStream {

        if (isProcessCountLimited) acquire(true, processLimiter)

        LOGGER.info("${(Engine.gameTime * 1e-9f).f3()} ${arguments.joinToString(" ")}")

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, arguments.size + 1, true)
        if (arguments.isNotEmpty()) builder += "-hide_banner"
        builder += arguments

        val process = builder.start()
        LOGGER.debug("started process")
        process(process, arguments)
        if (isProcessCountLimited) {
            waitForRelease(process)
        }

        return this

    }

    fun waitForRelease(process: Process) {
        waitingQueue += {
            if (process.waitFor(1, TimeUnit.MILLISECONDS)) {
                processLimiter.release()
            } else {
                waitForRelease(process)
            }
        }
    }

    fun devNull(name: String, stream: InputStream) {
        thread(name = "devNull-$name") {
            try {
                stream.use {
                    waitUntil(true) {// wait until we are done
                        stream.available() > 0 && stream.read() < 0
                    }
                }
            } catch (_: ShutdownException) {
            }
        }
    }

    fun devLog(name: String, stream: InputStream) {
        thread(name = name) {
            val out = stream.bufferedReader()
            while (!Engine.shutdown) {
                val line = out.readLine() ?: break
                LOGGER.info(line)
            }
            out.close()
        }
    }

}