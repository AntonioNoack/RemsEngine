package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.Time
import me.anno.audio.openal.SoundBuffer
import me.anno.cache.AsyncCacheData
import me.anno.cache.IgnoredException
import me.anno.image.Image
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep
import me.anno.utils.hpc.HeavyProcessing.numThreads
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.formats.cpu.CPUFrameReader
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.IOException
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
        @JvmField
        val processLimiter = Semaphore(max(2, numThreads), true)

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FFMPEGStream::class)

        @JvmField
        val frameCountByFile = HashMap<FileReference, Int>()

        @JvmField
        val waitingQueue = ProcessingQueue("WaitingQueue")

        @JvmStatic
        fun getInfo(input: FileReference) = (FFMPEGMeta(null)
            .run("-i", input.absolutePath) as FFMPEGMeta).stringData

        @JvmStatic
        fun getSupportedFormats() = (FFMPEGMeta(null)
            .run("-formats") as FFMPEGMeta).stringData

        @JvmStatic
        fun getImageSequence(
            input: FileReference, signature: String?,
            w: Int, h: Int, startFrame: Int, frameCount: Int, fps: Double,
            originalWidth: Int, // meta?.videoWidth
            originalFPS: Double, // meta?.videoFPS ?: 0.0001
            totalFrameCount: Int,
            nextFrameCallback: (GPUFrame) -> Unit,
            finishedCallback: (List<GPUFrame>) -> Unit
        ) {
            getImageSequence(
                input, signature, w, h, startFrame / fps, frameCount, fps,
                originalWidth, originalFPS, totalFrameCount,
                nextFrameCallback, finishedCallback
            )
        }

        // ffmpeg needs to fetch hardware decoded frames (-hwaccel auto) from gpu memory;
        // if we use hardware decoding, we need to use it on the gpu...
        @JvmStatic
        fun getImageSequence(
            input: FileReference, signature: String?,
            w: Int, h: Int, startTime: Double, frameCount: Int, fps: Double,
            originalWidth: Int, // meta?.videoWidth
            originalFPS: Double, // meta?.videoFPS ?: 0.0001
            totalFrameCount: Int,
            nextFrameCallback: (GPUFrame) -> Unit,
            finishedCallback: (List<GPUFrame>) -> Unit
        ) {
            thread(name = "$input/${w}x${h}/$startTime") {
                try {
                    GPUFrameReader(
                        input, (startTime * fps).roundToInt(),
                        frameCount, nextFrameCallback, finishedCallback
                    ).run(
                        *getImageSequenceArguments(
                            input, signature, w, h, startTime, frameCount, fps,
                            originalWidth, originalFPS, totalFrameCount
                        ).toTypedArray()
                    )
                } catch (_: ShutdownException) {
                } catch (e: IOException) {
                    finishedCallback(emptyList())
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun getImageSequenceCPU(
            input: FileReference, signature: String?,
            w: Int, h: Int, frameIndex: Int, frameCount: Int, fps: Double,
            originalWidth: Int, // meta?.videoWidth
            originalFPS: Double, // meta?.videoFPS ?: 0.0001
            totalFrameCount: Int,
            nextFrameCallback: (Image) -> Unit,
            finishedCallback: (List<Image>) -> Unit
        ) {
            thread(name = "$input/${w}x${h}/$frameIndex") {
                try {
                    CPUFrameReader(input, frameIndex, frameCount, nextFrameCallback, finishedCallback).run(
                        *getImageSequenceArguments(
                            input, signature, w, h,
                            frameIndex / max(fps, 1e-3), frameCount, fps,
                            originalWidth, originalFPS,
                            totalFrameCount
                        ).toTypedArray()
                    )
                } catch (_: ShutdownException) {
                } catch (e: IOException) {
                    finishedCallback(emptyList())
                    e.printStackTrace()
                }
            }
        }

        @JvmStatic
        fun getImageSequenceGPU(
            input: FileReference, signature: String?,
            w: Int, h: Int, frameIndex: Int, frameCount: Int, fps: Double,
            originalWidth: Int, // meta?.videoWidth
            originalFPS: Double, // meta?.videoFPS ?: 0.0001
            totalFrameCount: Int,
            nextFrameCallback: (GPUFrame) -> Unit,
            finishedCallback: (List<GPUFrame>) -> Unit
        ) {
            thread(name = "$input/${w}x${h}/$frameIndex") {
                try {
                    GPUFrameReader(input, frameIndex, frameCount, nextFrameCallback, finishedCallback).run(
                        *getImageSequenceArguments(
                            input, signature, w, h,
                            frameIndex / max(fps, 1e-3), frameCount, fps,
                            originalWidth, originalFPS,
                            totalFrameCount
                        ).toTypedArray()
                    )
                } catch (_: ShutdownException) {
                }
            }
        }

        @JvmStatic
        fun getImageSequenceArguments(
            input: FileReference,
            signature: String?,
            w: Int, h: Int, startTime: Double,
            frameCount: Int, fps: Double,
            originalWidth: Int, // meta?.videoWidth
            originalFPS: Double, // meta?.videoFPS ?: 0.0001
            totalFrameCount: Int
        ): List<String> {
            val args = ArrayList<String>()
            if (totalFrameCount > 1 && startTime > 0) {
                args.add("-ss")
                args.add(startTime.toString())
            }
            args.add("-i")
            args.add(input.absolutePath)
            if (abs(fps - originalFPS) > max(fps, originalFPS) * 0.01f) {
                args.add("-r")
                args.add(fps.toString())
            }
            if (originalWidth > 0 && originalWidth != w) {
                args.add("-vf")
                args.add("scale=$w:$h")
            }

            // todo support HDR color?
            args.add("-pix_fmt")
            args.add(
                if (signature == "dds" || signature == "webp" ||
                    (signature == "media" && input.lcExtension == "webm")
                ) "bgra" else "bgr24"
            )

            args.add("-vframes")
            args.add(frameCount.toString())
            // didn't have noticeable effect, maybe it does now (??...)
            // args.add("-movflags")
            // args.add("faststart")
            args.add("-f") // format
            args.add("rawvideo")
            args.add("-")
            return args
        }

        @JvmStatic
        fun getAudioSequence(
            input: FileReference,
            startTime: Double,
            duration: Double,
            sampleRate: Int
        ): AsyncCacheData<SoundBuffer> {
            val loader = FFMPEGAudio(input, MediaMetadata.getMeta(input, false)!!.audioChannels, sampleRate, duration)
            loader.run(
                "-ss", "$startTime", // important!!!
                "-i", input.absolutePath,
                "-t", "$duration", // duration
                "-ar", "$sampleRate",
                // -aq quality, codec specific
                "-f", "s16le", "-acodec", "pcm_s16le",
                // the -bitexact tag doesn't exist on my Linux ffmpeg :(, and ffmpeg just still adds the info block
                // -> we need to remove it
                // "-bitexact", // don't add a LIST-INFO chunk; we don't care
                // wav is exported with length -1, which slick does not support
                // ogg reports "error 34", and ffmpeg is slow
                // "-c:a", "pcm_s16le", "-ac", "2",
                "-"
                // "pipe:1" // 1 = stdout, 2 = stdout
            )
            return loader.result
        }

        @JvmStatic
        fun logOutput(prefix: String?, name: String, stream: InputStream, warn: Boolean) {
            val reader = stream.bufferedReader()
            thread(name = "LogOutput<$name>") {
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

        @JvmStatic
        fun devNull(name: String, stream: InputStream) {
            thread(name = "devNull-$name") {
                try {
                    stream.use { stream1: InputStream ->
                        Sleep.waitUntil(true) {// wait until we are done
                            stream1.available() > 0 && stream1.read() < 0
                        }
                    }
                } catch (_: IgnoredException) {
                }
            }
        }

        @JvmStatic
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

    var srcFPS = -1.0
    var srcDuration = 0.0

    var codec = ""

    var width = 0
    var height = 0

    abstract fun process(process: Process, vararg arguments: String)

    abstract fun destroy()

    fun waitForMetadata(parser: FFMPEGMetaParser) {
        var lt = Time.nanoTime
        Sleep.waitUntil(true) {
            // if the last line is too long ago, e.g., because the source is not readable as an image, return
            val timeLimit = 30e9
            if (codec == FFMPEGMetaParser.invalidCodec) true
            else if (parser.lastLineTime != 0L && Time.nanoTime - parser.lastLineTime > timeLimit) true
            else {
                val t = Time.nanoTime
                if (abs(t - lt) > 1e9) {
                    LOGGER.debug("Waiting for metadata on {}, {} x {}, {}", file, width, height, codec)
                    lt = t
                }
                width != 0 && height != 0 && codec.isNotEmpty()
            }
        }
    }

    fun parseAsync(parser: FFMPEGMetaParser, stream: InputStream) {
        thread(name = "${file?.name}:error-stream") {
            val out = stream.bufferedReader()
            try {
                while (true) {
                    val line = out.readLine() ?: break
                    // if('!' in line || "Error" in line) LOGGER.warn("ffmpeg $frame0 ${arguments.joinToString(" ")}: $line")
                    parser.parseLine(line, this)
                }
            } catch (_: IgnoredException) {
            }
        }
    }

    fun run(vararg arguments: String): FFMPEGStream {

        if (isProcessCountLimited) Sleep.acquire(true, processLimiter)

        LOGGER.info(arguments.joinToString(" "))

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, arguments.size + 1, true)
        if (arguments.isNotEmpty()) builder += "-hide_banner"
        builder.args += arguments

        val process = builder.start()
        process(process, *arguments)
        if (isProcessCountLimited) {
            waitForRelease(process)
        }
        return this
    }

    fun waitForRelease(process: Process) {
        if (Engine.shutdown) {
            LOGGER.warn("Shutting down before child process")
            waitingQueue.stop()
            process.destroyForcibly() // ^^
        } else {
            waitingQueue += {
                if (process.waitFor(1, TimeUnit.MILLISECONDS)) {
                    processLimiter.release()
                } else {
                    waitForRelease(process)
                }
            }
        }
    }
}