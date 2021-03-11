package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.Rendering.isRendering
import me.anno.video.FFMPEGStream.Companion.logOutput
import me.anno.video.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread

class VideoCreator(
    val w: Int, val h: Int,
    val fps: Double,
    val totalFrameCount: Long,
    balance: FFMPEGEncodingBalance,
    type: FFMPEGEncodingType,
    val output: File
) {

    init {
        if (w % 2 != 0 || h % 2 != 0) throw RuntimeException("width and height must be divisible by 2")
    }

    val startTime = GFX.gameTime

    private val videoOut: OutputStream
    private var process: Process

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
        val size = "${w}x${h}"
        val rawFormat = "rgb24"
        val encoding = "libx264"
        val constantRateFactor = project?.targetVideoQuality?.toString() ?: "23"
        val dstFormat = "yuv420p"
        val fps = fps.toString()
        val videoEncodingArguments = arrayListOf(
            "-f", "rawvideo",
            "-s", size,
            "-r", fps,
            "-pix_fmt", rawFormat,
            "-i", "pipe:0", // output buffer
            "-c:v", encoding, // encoding
            "-an", // no audio
            "-r", fps,
            "-crf", constantRateFactor,
            "-pix_fmt", dstFormat,
            "-preset", balance.internalName
            // "-qp", "0", // constant quality
        )

        if (type.internalName != null) {
            videoEncodingArguments += "-tune"
            videoEncodingArguments += type.internalName
        }

        videoEncodingArguments += output.absolutePath

        val args = ArrayList<String>(videoEncodingArguments.size + 2)
        args += FFMPEG.ffmpegPathString
        if (videoEncodingArguments.isNotEmpty()) args += "-hide_banner"
        args += videoEncodingArguments
        process = ProcessBuilder(args).start()
        thread { logOutput("", process.inputStream, true) }
        thread { processOutput(LOGGER, "Video", startTime, totalFrameCount, process.errorStream) }

        videoOut = process.outputStream.buffered()

        LOGGER.info("Total frame count: $totalFrameCount")

    }

    private val pixelByteCount = w * h * 3
    private val byteArrayBuffer = ByteArray(pixelByteCount)

    private val buffer1 = BufferUtils.createByteBuffer(pixelByteCount)
    private val buffer2 = BufferUtils.createByteBuffer(pixelByteCount)

    fun writeFrame(frame: Framebuffer, frameIndex: Long, callback: () -> Unit) {

        GFX.check()

        if (frame.w != w || frame.h != h) throw RuntimeException("Resolution does not match!")
        frame.bindDirectly(false)
        Frame.invalidate()

        val buffer = if (frameIndex % 2 == 0L) buffer1 else buffer2

        buffer.position(0)
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)

        GFX.check()

        thread {// offload to other thread
            try {
                synchronized(videoOut) {
                    buffer.get(byteArrayBuffer)
                    videoOut.write(byteArrayBuffer)
                    callback()
                }
            } catch (e: IOException) {
                if (!wasClosed) {
                    LOGGER.error("Closing because of ${e.message}")
                    isRendering = false
                    e.printStackTrace()
                    close()
                }
            }
        }

    }

    var wasClosed = false
    fun close() {
        wasClosed = true
        synchronized(videoOut) {
            videoOut.flush()
            videoOut.close()
        }
        process.waitFor()
        if (output.exists()) LOGGER.info("Saved video without audio to $output")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoCreator::class)
    }

}