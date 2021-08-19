package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.io.files.FileReference
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.Rendering.isRendering
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.Codecs.videoCodecByExtension
import me.anno.video.FFMPEGStream.Companion.logOutput
import me.anno.video.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class VideoCreator(
    val w: Int, val h: Int,
    val fps: Double,
    val totalFrameCount: Long,
    val balance: FFMPEGEncodingBalance,
    val type: FFMPEGEncodingType,
    val output: FileReference
) {

    init {
        if (w % 2 != 0 || h % 2 != 0) throw RuntimeException("width and height must be divisible by 2")
    }

    val startTime = GFX.gameTime

    private lateinit var videoOut: OutputStream
    private lateinit var process: Process

    fun init() {

        if (output.exists) output.delete()
        else output.getParent()?.mkdirs()

        val extension = output.extension.lowercase(Locale.getDefault())
        val isGIF = extension == "gif"

        /**
         * first create the video,
         * then add audio later;
         * because I don't know how to send audio and video data to ffmpeg
         * at the same time with only one output stream
         * */
        val size = "${w}x${h}"
        val rawFormat = "rgb24"
        val encoding = videoCodecByExtension(extension) // "libx264"
        val constantRateFactor = project?.targetVideoQuality?.toString() ?: "23"

        // Incompatible pixel format 'yuv420p' for codec 'gif', auto-selecting format 'bgr8'
        val dstFormat = if (isGIF) "bgr8" else "yuv420p"
        val fpsString = fps.toString()

        val videoEncodingArguments = arrayListOf(
            "-f", "rawvideo",
            "-s", size,
            "-r", fpsString,
            "-pix_fmt", rawFormat,
            "-i", "pipe:0", // output buffer
        )

        if (!isGIF && encoding != null) {
            videoEncodingArguments += listOf(
                "-c:v", encoding
            )
        }

        videoEncodingArguments += listOf(
            "-an", // no audio
            "-r", fpsString,
            // "-qp", "0", // constant quality
        )

        if (!isGIF) {
            videoEncodingArguments += listOf(
                "-crf", constantRateFactor
            )
        }

        videoEncodingArguments += listOf(
            "-pix_fmt", dstFormat
        )

        if (encoding == "libx264") {
            // other codecs support other values
            videoEncodingArguments += listOf(
                "-preset", balance.internalName
            )
        }

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, videoEncodingArguments.size + 4, true)
        if (videoEncodingArguments.isNotEmpty()) builder += "-hide_banner"

        builder += videoEncodingArguments
        if (type.internalName != null) {
            builder += "-tune"
            builder += type.internalName
        }
        builder += output.absolutePath

        process = builder.start()
        logOutput(null, process.inputStream, true)
        thread(name = "VideoCreatorOutput") {
            processOutput(LOGGER, "Video", startTime, fps, totalFrameCount, process.errorStream)
        }

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

        thread(name = "FrameDataCopy[$frameIndex]") {// offload to other thread
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
        if (output.exists) LOGGER.info("Saved video without audio to $output")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VideoCreator::class)
    }

}