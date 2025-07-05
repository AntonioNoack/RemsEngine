package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.Time
import me.anno.audio.openal.SoundBuffer
import me.anno.cache.AsyncCacheData
import me.anno.cache.IgnoredException
import me.anno.image.Image
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.utils.Sleep
import me.anno.utils.hpc.HeavyProcessing
import me.anno.video.formats.cpu.CPUFrameReader
import me.anno.video.formats.gpu.GPUFrame
import me.anno.video.formats.gpu.GPUFrameReader
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

// ffmpeg requires 100 MB RAM per instance -> do we really need multiple instances, or does one work fine
// done keep only a certain amount of ffmpeg instances running
abstract class FFMPEGStream(val file: FileReference?, val isProcessCountLimited: Boolean) {

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FFMPEGStream::class)

        // could be limited by memory as well...
        // to help to keep the memory and cpu-usage below 100%
        // 5 GB = 50 processes, at 6 cores / 12 threads = 4 ratio
        @JvmField
        val processLimiter = Semaphore(max(2, HeavyProcessing.numThreads), true)

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
            val threadName = "$input/${w}x${h}/$frameIndex"
            val args = getImageSequenceArguments(
                input, signature, w, h,
                frameIndex / max(fps, 1e-3), frameCount, fps,
                originalWidth, originalFPS,
                totalFrameCount
            )
            CPUFrameReader(input, frameIndex, frameCount, nextFrameCallback, finishedCallback)
                .runAsync(threadName, args)
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
            val threadName = "$input/${w}x${h}/$frameIndex"
            val args = getImageSequenceArguments(
                input, signature, w, h,
                frameIndex / max(fps, 1e-3), frameCount, fps,
                originalWidth, originalFPS,
                totalFrameCount
            )
            GPUFrameReader(input, frameIndex, frameCount, nextFrameCallback, finishedCallback)
                .runAsync(threadName, args)
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
            input: FileReference, startTime: Double, duration: Double, sampleRate: Int,
            result: AsyncCacheData<SoundBuffer>
        ) {
            FFMPEGAudio(
                input, MediaMetadata.getMeta(input).waitFor()!!.audioChannels,
                sampleRate, duration, result
            ).runAsync(
                "$input/$startTime/$duration/$sampleRate", listOf(
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
            )
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
            val name1 = "devNull-$name"
            Sleep.waitUntil(name1, true, {// wait until we are done
                stream.available() > 0 && stream.read() < 0
            }, { stream.close() })
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

    var codec = ""

    var width = 0
    var height = 0

    var isFinished = false
    var isDestroyed = false

    abstract fun process(process: Process, arguments: List<String>, callback: () -> Unit)

    abstract fun destroy()

    fun waitForMetadata(parser: FFMPEGMetaParser, callback: () -> Unit) {
        var lt = Time.nanoTime
        Sleep.waitUntil("waitForMetadata(${file?.name})", true, {
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
        }, { thread(name = toString(), block = callback) })
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

    fun runAsync(threadName: String, arguments: List<String>, isLimited: Boolean = isProcessCountLimited) {
        if (isLimited) {
            var acquired = 0
            Sleep.waitUntil("FFMPEGStream:runAsync",true, { // wait for running permission
                if (processLimiter.tryAcquire()) acquired++
                acquired > 0 || isDestroyed
            }, {
                if (isDestroyed) {
                    processLimiter.release(acquired)
                } else {
                    if (acquired > 1) processLimiter.release(acquired - 1)
                    runAsync(threadName, arguments, false)
                }
            })
        } else {
            thread(name = threadName) {
                runUnlimited(arguments)
            }
        }
    }

    private fun runUnlimited(arguments: List<String>) {
        if (isDestroyed) return
        LOGGER.info(arguments.joinToString(" "))

        val builder = BetterProcessBuilder(FFMPEG.ffmpeg, arguments.size + 1, true)
        if (arguments.isNotEmpty()) builder += "-hide_banner"
        builder.addAll(arguments)

        val process = builder.start()
        process(process, arguments) {
            if (isProcessCountLimited) {
                waitForRelease(process)
            }
        }
    }

    private fun waitForRelease(process: Process) {
        Sleep.waitUntil("FFMPEGStream:waitForRelease",false, {
            if (process.waitFor(0, TimeUnit.MILLISECONDS)) {
                true
            } else if (Engine.shutdown) {
                process.destroyForcibly() // forced shutdown
                true
            } else false // continue waiting
        }, {
            processLimiter.release()
        })
    }
}