package me.anno.video

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.video.formats.ARGBFrame
import me.anno.video.formats.BGRAFrame
import me.anno.video.formats.I420Frame
import me.anno.video.formats.RGBFrame
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class FFMPEGStream(val file: File?, val frame0: Int, arguments: List<String>, waitToFinish: Boolean, interpretMeta: Boolean){

    var lastUsedTime = System.nanoTime()
    var sourceFPS = -1f
    var sourceLength = 0f

    companion object {
        val frameCountByFile = HashMap<File, Int>()
        fun getInfo(input: File) = FFMPEGStream(null, 0, listOf(
            "-i", input.absolutePath
        ), waitToFinish = true, interpretMeta = false).stringData
        fun getSupportedFormats() = FFMPEGStream(null, 0, listOf(
            "-formats"
        ), waitToFinish = true, interpretMeta = false).stringData
        fun getImageSequence(input: File, startFrame: Int, frameCount: Int, fps: Float = 10f) =
            getImageSequence(input, startFrame / fps, frameCount, fps)
        fun getImageSequence(input: File, startTime: Float, frameCount: Int, fps: Float = 10f) = FFMPEGStream(
            input, (startTime * fps).roundToInt(),
            listOf(
            "-i", input.absolutePath,
            "-ss", "$startTime",
            "-r", "$fps",
            "-vframes", "$frameCount",
            "-movflags", "faststart", // has no effect :(
            "-f", "rawvideo", "-"// format
            // "pipe:1" // 1 = stdout, 2 = stdout
        ), waitToFinish = false, interpretMeta = true)
    }

    fun destroy(){
        synchronized(frames){
            frames.forEach { GFX.addTask { it.destroy(); 3 } }
            frames.clear()
            isDestroyed = true
        }
    }

    var isDestroyed = false
    var stringData = ""

    init {
        if(waitToFinish){
            run(arguments, interpretMeta).waitFor()
        } else {
            thread {
                run(arguments, interpretMeta)
            }
        }
    }

    private fun run(arguments: List<String>, interpretMeta: Boolean): Process {
        // val time0 = System.nanoTime()
        // println(arguments)
        val ffmpeg = File(DefaultConfig["ffmpegPath", "lib/ffmpeg/ffmpeg.exe"])
        if(!ffmpeg.exists()) throw RuntimeException("FFmpeg not found! (path: $ffmpeg), can't use videos, nor webp!")
        val args = ArrayList<String>(arguments.size+2)
        args += ffmpeg.absolutePath
        if(arguments.isNotEmpty()) args += "-hide_banner"
        args += arguments
        val process = ProcessBuilder(args).start()
        if(interpretMeta){
            thread {
                if(interpretMeta){
                    val out = process.errorStream.bufferedReader()
                    val parser = FFMPEGMetaParser()
                    while(true){
                        val line = out.readLine() ?: break
                        parser.parseLine(line, this)
                    }
                } else {
                    val data = String(process.errorStream.readAllBytes())
                    stringData += "err[${data.length}]: $data \n"
                }
            }
            thread {
                val frameCount = arguments[arguments.indexOf("-vframes")+1].toInt()
                val input = process.inputStream
                readFrame(input)
                // val time1 = System.nanoTime()
                // println("used ${(time1-time0)*1e-9f}s for the first frame")
                for(i in 1 until frameCount){
                    readFrame(input)
                }
                input.close()
            }
        } else {
            getOutput("error", process.errorStream)
            getOutput("input", process.inputStream)
        }
        return process
    }

    var w = 0
    var h = 0
    var codec = ""

    val frames = ArrayList<Frame>()

    fun readFrame(input: InputStream){
        while(w == 0 || h == 0 || codec.isEmpty()){
            Thread.sleep(0,100_000)
        }
        if(!isDestroyed){
            synchronized(frames){
                try {
                    when(codec){
                        "I420" -> {
                            // doesn't work for webp somehow...
                            // looks like w is not correct, or similar
                            val frame = I420Frame(w, h)
                            frame.load(input)
                            frames.add(frame)
                        }
                        "ARGB" -> {
                            val frame = ARGBFrame(w, h)
                            frame.load(input)
                            frames.add(frame)
                        }
                        "BGRA" -> {
                            val frame = BGRAFrame(w, h)
                            frame.load(input)
                            frames.add(frame)
                        }
                        "RGB" -> {
                            val frame = RGBFrame(w, h)
                            frame.load(input)
                            frames.add(frame)
                        }
                        else -> throw RuntimeException("Unsupported Codec $codec!")
                    }
                } catch (e: LastFrame){
                    frameCountByFile[file!!] = frames.size + frame0
                } catch (e: Exception){
                    e.printStackTrace()
                    frameCountByFile[file!!] = frames.size + frame0
                }
            }
        }
        if(isDestroyed) destroy()
    }

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