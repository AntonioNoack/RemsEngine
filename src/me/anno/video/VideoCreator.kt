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

/**
 * todo write at the same time as rendering (does it work?)
 * todo why are we limited to 11 fps?
 * */
class VideoCreator(val w: Int, val h: Int, val fps: Double, val totalFrameCount: Int, val output: File){

    init {
        if(w % 2 != 0 || h % 2 != 0) throw RuntimeException("width and height must be divisible by 2")
    }

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

        val args = ArrayList<String>(videoEncodingArguments.size+2)
        args += FFMPEG.ffmpegPathString
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

    private val buffer1 = BufferUtils.createByteBuffer(w * h * 3)
    private val buffer2 = BufferUtils.createByteBuffer(w * h * 3)

    fun writeFrame(frame: Framebuffer, frameIndex: Int, callback: () -> Unit){

        GFX.check()

        if(frame.w != w || frame.h != h) throw RuntimeException("Resolution does not match!")
        frame.bind()

        val buffer = if(frameIndex % 2 == 0) buffer1 else buffer2

        buffer.position(0)
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)

        GFX.check()

        thread {
            synchronized(videoOut){
                // buffer.get(byteBuffer)
                // use a buffer instead for better performance?
                val pixelCount = w * h * 3
                for(i in 0 until pixelCount){
                    videoOut.write(buffer.get().toInt())
                }
                callback()
            }
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