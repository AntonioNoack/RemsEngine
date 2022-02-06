package me.anno.video

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.packAlignment
import me.anno.io.files.FileReference
import me.anno.remsstudio.RemsStudio.project
import me.anno.remsstudio.Rendering.isRendering
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.Codecs.videoCodecByExtension
import me.anno.video.ffmpeg.FFMPEGStream.Companion.logOutput
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEGUtils.processOutput
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min

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

    private val buffer1 = BufferUtils.createByteBuffer(pixelByteCount)
    private val buffer2 = BufferUtils.createByteBuffer(pixelByteCount)

    fun writeFrame(frame: Framebuffer, frameIndex: Long, callback: () -> Unit) {

        GFX.check()

        if (frame.w != w || frame.h != h) throw RuntimeException("Resolution does not match!")
        frame.bindDirectly()
        Frame.invalidate()

        val buffer = if (frameIndex % 2 == 0L) buffer1 else buffer2

        buffer.position(0)
        packAlignment(w * 3)
        glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)

        GFX.check()

        thread(name = "FrameDataCopy[$frameIndex]") {// offload to other thread
            try {
                synchronized(videoOut) {
                    write(videoOut, buffer)
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

    private val byteArrayBuffer = ByteArray(min(pixelByteCount, 2048))
    fun write(output: OutputStream, data: ByteBuffer) {
        var i = 0
        val tmp = byteArrayBuffer
        val n = pixelByteCount
        while (i < n) {
            val di = min(n - i, tmp.size)
            data.get(tmp, 0, di)
            output.write(tmp, 0, di)
            i += di
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

        /**
         * render a video from a framebuffer
         * */
        fun renderVideo(
            w: Int,
            h: Int,
            fps: Double,
            dst: FileReference,
            numUpdates: Int,
            fb: Framebuffer,
            update: (callback: () -> Unit) -> Unit
        ) {
            val creator = VideoCreator(
                w, h, fps, numUpdates + 1L, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, dst
            )
            creator.init()
            var frameCount = 0
            fun writeFrame() {
                creator.writeFrame(fb, frameCount.toLong()) {
                    if (++frameCount <= numUpdates) {
                        GFX.addGPUTask(1) {
                            update(::writeFrame)
                        }
                    } else {
                        creator.close()
                        Engine.requestShutdown()
                    }
                }
            }
            GFX.addGPUTask(1) { writeFrame() }
            GFX.workGPUTasksUntilShutdown()
        }

        /**
         * render a video from a set of textures
         * */
        fun renderVideo(
            w: Int,
            h: Int,
            fps: Double,
            dst: FileReference,
            numFrames: Long,
            getNextFrame: (callback: (Texture2D) -> Unit) -> Unit
        ) {
            val creator = VideoCreator(
                w, h, fps, numFrames, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, dst
            )
            creator.init()
            var frameCount = 0
            val fb = Framebuffer("frame", w, h, 1, 1, false, DepthBufferType.NONE)
            fun writeFrame() {
                getNextFrame { texture ->
                    useFrame(fb) {
                        texture.bind(0)
                        GFX.copyNoAlpha()
                    }
                    creator.writeFrame(fb, frameCount.toLong()) {
                        if (++frameCount <= numFrames) {
                            GFX.addGPUTask(1) {
                                writeFrame()
                            }
                        } else {
                            creator.close()
                            Engine.requestShutdown()
                        }
                    }
                }
            }
            GFX.addGPUTask(1) { writeFrame() }
            GFX.workGPUTasksUntilShutdown()
        }

    }

}