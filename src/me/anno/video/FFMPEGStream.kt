package me.anno.video

import me.anno.gpu.GFX
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class FFMPEGStream(arguments: List<String>, waitToFinish: Boolean, interpretMeta: Boolean){

    var lastUsedTime = System.nanoTime()
    var sourceFPS = 24f
    var sourceLength = 1f

    companion object {
        fun getInfo(input: File) = FFMPEGStream(listOf(
            "-i", input.absolutePath
        ), waitToFinish = true, interpretMeta = false).stringData
        fun getSupportedFormats() = FFMPEGStream(listOf(
            "-formats"
        ), waitToFinish = true, interpretMeta = false).stringData
        fun getImageSequence(input: File, startFrame: Int, frameCount: Int, fps: Float = 10f) =
            getImageSequence(input, startFrame / fps, frameCount, fps)
        fun getImageSequence(input: File, startTime: Float, frameCount: Int, fps: Float = 10f) = FFMPEGStream(listOf(
            "-i", input.absolutePath,
            "-ss", "$startTime",
            "-r", "$fps",
            "-vframes", "$frameCount",
            "-movflags", "faststart", // has no effect :(
            "-f", "rawvideo", "-"// format
            // "pipe:1" // 1 = stdout, 2 = stdout
        ), waitToFinish = false, interpretMeta = true)
        var testFrame: I420Frame? = null
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

    fun run(arguments: List<String>, interpretMeta: Boolean): Process {
        // val time0 = System.nanoTime()
        println(arguments)
        val ffmpeg = File("C:\\Users\\Antonio\\Downloads\\lib\\ffmpeg\\bin\\ffmpeg.exe")
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

    val frames = ArrayList<Frame>()

    fun readFrame(input: InputStream){
        while(w == 0 || h == 0){
            Thread.sleep(0,100_000)
        }
        if(!isDestroyed){
            synchronized(frames){
                val frame = I420Frame(w,h)
                frame.load(input)
                frames.add(frame)
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