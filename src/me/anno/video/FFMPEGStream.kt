package me.anno.video

import me.anno.config.DefaultConfig
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.roundToInt

abstract class FFMPEGStream(val file: File?){

    var lastUsedTime = System.nanoTime()
    var sourceFPS = -1f
    var sourceLength = 0f

    companion object {
        val frameCountByFile = HashMap<File, Int>()
        fun getInfo(input: File) = (FFMPEGMeta(null).run(listOf(
            "-i", input.absolutePath
        )) as FFMPEGMeta).stringData
        fun getSupportedFormats() = (FFMPEGMeta(null).run(listOf(
            "-formats"
        )) as FFMPEGMeta).stringData
        fun getImageSequence(input: File, startFrame: Int, frameCount: Int, fps: Float = 10f) =
            getImageSequence(input, startFrame / fps, frameCount, fps)
        fun getImageSequence(input: File, startTime: Float, frameCount: Int, fps: Float = 10f) = FFMPEGVideo(
            input, (startTime * fps).roundToInt()).run(listOf(
            "-i", input.absolutePath,
            "-ss", "$startTime",
            "-r", "$fps",
            "-vframes", "$frameCount",
            "-movflags", "faststart", // has no effect :(
            "-f", "rawvideo", "-"// format
            // "pipe:1" // 1 = stdout, 2 = stdout
        )) as FFMPEGVideo
        fun getAudioSequence(input: File, startTime: Float, duration: Float, sampleRate: Int) = FFMPEGAudio(
            input, sampleRate, duration, (startTime * sampleRate).roundToInt()).run(listOf(
            "-i", input.absolutePath,
            "-ss", "$startTime",
            "-t", "$duration", // duration
            "-ar", "$sampleRate",
            // -aq quality, codec specific
            "-f", "wav",
            // wav is exported with length -1, which slick does not support
            // ogg reports "error 34", and ffmpeg is slow
            // "-c:a", "pcm_s16le", "-ac", "2",
            "-"
            // "pipe:1" // 1 = stdout, 2 = stdout
        )) as FFMPEGAudio
    }

    abstract fun destroy()

    fun run(arguments: List<String>): FFMPEGStream {
        val ffmpeg = File(DefaultConfig["ffmpegPath", "lib/ffmpeg/ffmpeg.exe"])
        if(!ffmpeg.exists()) throw RuntimeException("FFmpeg not found! (path: $ffmpeg), can't use videos, nor webp!")
        val args = ArrayList<String>(arguments.size+2)
        args += ffmpeg.absolutePath
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
                println("[$prefix] $line")
            }
        }
    }

}