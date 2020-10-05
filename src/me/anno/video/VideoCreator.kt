package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.utils.*
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.io.OutputStream
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.round

/**
 * todo write at the same time as rendering (does it work?)
 * todo why are we limited to 11 fps?
 * */
class VideoCreator(val w: Int, val h: Int, val fps: Double, val totalFrameCount: Int, val output: File) {

    init {
        if (w % 2 != 0 || h % 2 != 0) throw RuntimeException("width and height must be divisible by 2")
    }

    val videoQualities = arrayListOf(
        "ultrafast", "superfast", "veryfast", "faster",
        "fast", "medium", "slow", "slower", "**veryslow**", "placebo"
    )

    val videoOut: OutputStream

    init {

        if (output.exists()) output.delete()
        else if (!output.parentFile.exists()) {
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

        val args = ArrayList<String>(videoEncodingArguments.size + 2)
        args += FFMPEG.ffmpegPathString
        if (videoEncodingArguments.isNotEmpty()) args += "-hide_banner"
        args += videoEncodingArguments
        val process = ProcessBuilder(args).start()
        thread {
            val out = process.errorStream.bufferedReader()
            while (true) {
                val line = out.readLine() ?: break
                // parse the line
                if (line.indexOf('=') > 0) {
                    var frameIndex = 0
                    var fps = 0f
                    var quality = 0f
                    var size = 0
                    var elapsedTime = 0.0
                    var bitrate = 0
                    var speed = 0f
                    var remaining = line
                    while (remaining.isNotEmpty()) {
                        val firstIndex = remaining.indexOf('=')
                        if (firstIndex < 0) break
                        val key = remaining.substring(0, firstIndex).trim()
                        remaining = remaining.substring(firstIndex + 1).trim()
                        var secondIndex = remaining.indexOf(' ')
                        if (secondIndex < 0) secondIndex = remaining.length
                        val value = remaining.substring(0, secondIndex)
                        try {
                            when (key.toLowerCase()) {
                                "speed" -> speed = value.substring(0, value.length - 1).toFloat() // 0.15x
                                "bitrate" -> {
                                    // todo parse bitrate? or just display it?
                                }
                                "time" -> elapsedTime = value.parseTime()
                                "size", "lsize" -> {
                                }
                                "q" -> {
                                } // quality?
                                "frame" -> frameIndex = value.toInt()
                                "fps" -> fps = value.toFloat()
                            }
                        } catch (e: Exception) {
                            LOGGER.warn(e.message ?: "")
                        }
                        // LOGGER.info("$key: $value")
                        if (secondIndex == remaining.length) break
                        remaining = remaining.substring(secondIndex)
                    }
                    // update progress bar after this
                    // + log other statistics
                    val relativeProgress = frameIndex.toDouble() / totalFrameCount
                    // estimate remaining time
                    // round the value to not confuse artists (and to "give" 0.5s "extra" ;))
                    val remainingTime = round(
                        elapsedTime / relativeProgress *
                                (1.0 - relativeProgress)
                    ).formatTime()
                    LOGGER.info(
                        "Rendering-Progress: ${clamp((relativeProgress * 100).toFloat(), 0f, 100f).f1()}%, " +
                                "fps: $fps, " +
                                "elapsed: ${round(elapsedTime).formatTime()}, " +
                                "remaining: $remainingTime"
                    )
                }// else {
                    // the rest logged is only x264 statistics
                    // LOGGER.debug(line)
                // }
                // frame=  151 fps= 11 q=12.0 size=     256kB time=00:00:04.40 bitrate= 476.7kbits/s speed=0.314x
                // or [libx264 @ 000001c678804000] frame I:1     Avg QP:19.00  size:  2335
            }
        }

        videoOut = process.outputStream.buffered()

    }

    private val buffer1 = BufferUtils.createByteBuffer(w * h * 3)
    private val buffer2 = BufferUtils.createByteBuffer(w * h * 3)

    fun writeFrame(frame: Framebuffer, frameIndex: Int, callback: () -> Unit) {

        GFX.check()

        if (frame.w != w || frame.h != h) throw RuntimeException("Resolution does not match!")
        frame.bindDirectly(false)
        Frame.invalidate()

        val buffer = if (frameIndex % 2 == 0) buffer1 else buffer2

        buffer.position(0)
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)

        GFX.check()

        thread {
            synchronized(videoOut) {
                // buffer.get(byteBuffer)
                // use a buffer instead for better performance?
                val pixelCount = w * h * 3
                for (i in 0 until pixelCount) {
                    videoOut.write(buffer.get().toInt())
                }
                callback()
            }
        }

    }

    fun close() {
        videoOut.flush()
        videoOut.close()
        LOGGER.info("Saved video without audio to $output")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoCreator::class)
    }

}