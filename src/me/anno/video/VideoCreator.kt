package me.anno.video

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.io.OutputStream
import java.lang.RuntimeException
import java.nio.channels.Channel
import kotlin.concurrent.thread
import kotlin.math.sin

class VideoCreator(val w: Int, val h: Int, val fps: Float, val output: File){

    val videoQualities = arrayListOf(
        "ultrafast", "superfast", "veryfast", "faster",
        "fast", "medium", "slow", "slower", "**veryslow**", "placebo"
    )

    val out: OutputStream

    init {

        if(output.exists()) output.delete()
        else if(!output.parentFile.exists()){
            output.parentFile?.mkdirs()
        }

        val arguments = arrayListOf(
            "-f", "rawvideo",
            "-s", "${w}x$h",
            "-r", "$fps",
            "-pix_fmt", "rgb24",
            "-i", "pipe:0",
            "-c:v", "libx264", // encoding
            "-an", // no audio
            "-r", "$fps",
            "-crf", "22",
            "-pix_fmt", "yuv420p",
            "-preset", "ultrafast",
            // "-qp", "0", // constant quality
            output.absolutePath
        )

        val ffmpeg = File(DefaultConfig["ffmpegPath", "lib/ffmpeg/ffmpeg.exe"])
        if(!ffmpeg.exists()) throw RuntimeException("FFmpeg not found! (path: $ffmpeg), can't use videos, nor webp!")
        val args = ArrayList<String>(arguments.size+2)
        args += ffmpeg.absolutePath
        if(arguments.isNotEmpty()) args += "-hide_banner"
        args += arguments
        val process = ProcessBuilder(args).start()
        thread {
            val out = process.errorStream.bufferedReader()
            while(true){
                val line = out.readLine() ?: break
                println("[FFMPEG-Debug]: $line")
            }
        }

        out = process.outputStream.buffered()

    }

    fun debugWrite(){
        val frameCount = 200
        for(f in 0 until frameCount){
            val pixelCount = w * h * 3
            val color = (sin(f * 0.1f)*127 + 127).toInt()
            for(i in 0 until pixelCount) out.write(color)
            // val pixelCountYUV = pixelCount / 4
            // for(i in 0 until pixelCountYUV * 2) out.write(127)
        }

        out.flush()
        out.close()
    }

    val buffer = BufferUtils.createByteBuffer(w * h * 3)

    fun writeFrame(frame: Framebuffer, callback: () -> Unit){

        GFX.check()

        if(frame.w != w || frame.h != h) throw RuntimeException("Resolution does not match!")
        frame.bind()

        buffer.position(0)
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)

        GFX.check()

        thread {
            // buffer.get(byteBuffer)
            // use a buffer instead for better performance?
            val pixelCount = w * h * 3
            for(i in 0 until pixelCount){
                out.write(buffer.get().toInt())
            }
            callback()
        }

    }

    fun close(){
        out.flush()
        out.close()
        println("[INFO] Saved file to $output")
    }

}

fun main(){
    // VideoCreator()
}