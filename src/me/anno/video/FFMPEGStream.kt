package me.anno.video

import me.anno.gpu.GFX
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread
import kotlin.math.roundToInt

abstract class FFMPEGStream(val file: File?){

    var sourceFPS = -1.0
    var sourceLength = 0.0

    companion object {
        private val LOGGER = LogManager.getLogger(FFMPEGStream::class)
        val frameCountByFile = HashMap<File, Int>()
        fun getInfo(input: File) = (FFMPEGMeta(null).run(listOf(
            "-i", input.absolutePath
        )) as FFMPEGMeta).stringData
        fun getSupportedFormats() = (FFMPEGMeta(null).run(listOf(
            "-formats"
        )) as FFMPEGMeta).stringData
        fun getImageSequence(input: File, w: Int, h: Int, startFrame: Int, frameCount: Int, fps: Double = 10.0) =
            getImageSequence(input, w, h, startFrame / fps, frameCount, fps)
        // ffmpeg needs to fetch hardware decoded frames (-hwaccel auto) from gpu memory;
        // if we use hardware decoding, we need to use it on the gpu...
        fun getImageSequence(input: File, w: Int, h: Int, startTime: Double, frameCount: Int, fps: Double = 10.0) = FFMPEGVideo(
            input, (startTime * fps).roundToInt(), frameCount).run(
            if(getMeta(input, false)?.videoWidth == w){
                listOf(
                    "-i", input.absolutePath,
                    "-ss", "$startTime",
                    "-r", "$fps",
                    "-vframes", "$frameCount",
                    // "-movflags", "faststart", // has no effect :(
                    "-f", "rawvideo", "-" // format
                )
            } else {
                listOf(
                    "-i", input.absolutePath,
                    "-ss", "$startTime",
                    "-vf", "scale=$w:$h",
                    "-r", "$fps",
                    "-vframes", "$frameCount",
                    // "-movflags", "faststart", // has no effect :(
                    "-f", "rawvideo", "-" // format
                )
            }
        ) as FFMPEGVideo
        fun getAudioSequence(input: File, startTime: Double, duration: Double, sampleRate: Int) = FFMPEGAudio(
            input, sampleRate, duration).run(listOf(
            "-i", input.absolutePath,
            "-ss", "$startTime",
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
        )) as FFMPEGAudio
    }

    abstract fun destroy()

    fun run(arguments: List<String>): FFMPEGStream {
        // LOGGER.info("${(GFX.lastTime/1e9).toInt()} ${arguments.joinToString(" ")}")
        val args = ArrayList<String>(arguments.size+2)
        args += FFMPEG.ffmpegPathString
        if(arguments.isNotEmpty()) args += "-hide_banner"
        args += arguments
        val process = ProcessBuilder(args).start()
        process(process, arguments)
        return this
    }

    abstract fun process(process: Process, arguments: List<String>)

    var codec = ""

    var w = 0
    var h = 0

    var srcW = 0
    var srcH = 0

    fun getOutput(prefix: String, stream: InputStream){
        val reader = stream.bufferedReader()
        thread {
            while(true){
                val line = reader.readLine() ?: break
                LOGGER.info("[$prefix] $line")
            }
        }
    }

}