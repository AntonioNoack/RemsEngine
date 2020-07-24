package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.io.OutputStream
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.sin

class VideoCreator(val w: Int, val h: Int, val fps: Double, val totalFrameCount: Int, val output: File){

    val videoQualities = arrayListOf(
        "ultrafast", "superfast", "veryfast", "faster",
        "fast", "medium", "slow", "slower", "**veryslow**", "placebo"
    )

    val videoOut: OutputStream

    init {

        if(output.exists()) output.delete()
        else if(!output.parentFile.exists()){
            output.parentFile?.mkdirs()
        }

        /**
         * first create the video,
         * then add audio later;
         * because I don't know how to send audio and video data to ffmpeg
         * at the same time with only one output stream
         * */
        val videoEncodingArguments = arrayListOf(
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

        val ffmpeg = FFMPEG.ffmpeg
        val args = ArrayList<String>(videoEncodingArguments.size+2)
        args += ffmpeg.absolutePath
        if(videoEncodingArguments.isNotEmpty()) args += "-hide_banner"
        args += videoEncodingArguments
        val process = ProcessBuilder(args).start()
        thread {
            val out = process.errorStream.bufferedReader()
            while(true){
                val line = out.readLine() ?: break
                println("[FFMPEG-Debug]: $line")
            }
        }

        videoOut = process.outputStream.buffered()

    }

    fun debugWrite(){
        val frameCount = 200
        for(f in 0 until frameCount){
            val pixelCount = w * h * 3
            val color = (sin(f * 0.1f)*127 + 127).toInt()
            for(i in 0 until pixelCount) videoOut.write(color)
            // val pixelCountYUV = pixelCount / 4
            // for(i in 0 until pixelCountYUV * 2) out.write(127)
        }

        videoOut.flush()
        videoOut.close()
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
                videoOut.write(buffer.get().toInt())
            }
            callback()
        }

    }

    fun close(){
        videoOut.flush()
        videoOut.close()
        LOGGER.info("Saved video without audio to $output")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoCreator::class)
    }

}