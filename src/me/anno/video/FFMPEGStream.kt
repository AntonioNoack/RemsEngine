package me.anno.video

import me.anno.gpu.GFX
import me.anno.io.FileReference
import me.anno.utils.hpc.HeavyProcessing.threads
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.types.Floats.f3
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt

// ffmpeg requires 100MB RAM per instance -> do we really need multiple instances, or does one work fine
// done keep only a certain amount of ffmpeg instances running
abstract class FFMPEGStream(val file: FileReference?, val isProcessCountLimited: Boolean) {

    var sourceFPS = -1.0
    var sourceLength = 0.0

    companion object {

        // could be limited by memory as well...
        // to help to keep the memory and cpu-usage below 100%
        // 5GB = 50 processes, at 6 cores / 12 threads = 4 ratio
        val processLimiter = Semaphore(max(2, threads), true)
        private val LOGGER = LogManager.getLogger(FFMPEGStream::class)
        val frameCountByFile = HashMap<FileReference, Int>()
        val waitingQueue = ProcessingQueue("WaitingQueue")
        fun getInfo(input: File) = (FFMPEGMeta(null).run(
            listOf(
                "-i", input.absolutePath
            )
        ) as FFMPEGMeta).stringData

        fun getSupportedFormats() = (FFMPEGMeta(null).run(
            listOf(
                "-formats"
            )
        ) as FFMPEGMeta).stringData

        fun getImageSequence(
            input: FileReference,
            w: Int,
            h: Int,
            startFrame: Int,
            frameCount: Int,
            fps: Double,
            frameCallback: (VFrame, Int) -> Unit
        ) = getImageSequence(input, w, h, startFrame / fps, frameCount, fps, frameCallback)

        // ffmpeg needs to fetch hardware decoded frames (-hwaccel auto) from gpu memory;
        // if we use hardware decoding, we need to use it on the gpu...
        fun getImageSequence(
            input: FileReference,
            w: Int,
            h: Int,
            startTime: Double,
            frameCount: Int,
            fps: Double,
            frameCallback: (VFrame, Int) -> Unit
        ): FFMPEGVideo {
            val meta = getMeta(input, false)
            val args = arrayListOf(
                "-ss", "$startTime", // must be placed here!!!
                "-i", input.absolutePath
            )
            if (StrictMath.abs(fps - (meta?.videoFPS ?: 0.0001)) > 0.01) {
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
            val video = FFMPEGVideo(
                input, w, h, (startTime * fps).roundToInt(), frameCount,
                frameCallback
            )
            video.run(args)
            return video
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
            thread {
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

    abstract fun destroy()

    fun run(arguments: List<String>): FFMPEGStream {

        if (isProcessCountLimited) processLimiter.acquire()

        LOGGER.info("${(GFX.gameTime * 1e-9f).f3()} ${arguments.joinToString(" ")}")

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, arguments.size + 1, true)
        if (arguments.isNotEmpty()) builder += "-hide_banner"
        builder += arguments

        val process = builder.start()
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

    abstract fun process(process: Process, arguments: List<String>)

    var codec = ""

    var w = 0
    var h = 0

    var srcW = 0
    var srcH = 0

    fun devNull(prefix: String, stream: InputStream) {
        thread {
            while (true) {
                val read = stream.read()
                if (read < 0) break
            }
        }
    }

}