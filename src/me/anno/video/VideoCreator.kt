package me.anno.video

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.readAlignment
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.process.BetterProcessBuilder
import me.anno.video.Codecs.videoCodecByExtension
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import me.anno.video.ffmpeg.FFMPEGStream.Companion.logOutput
import me.anno.video.ffmpeg.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11C.*
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.min

open class VideoCreator(
    val w: Int, val h: Int,
    val fps: Double,
    val totalFrameCount: Long,
    val balance: FFMPEGEncodingBalance,
    val type: FFMPEGEncodingType,
    val quality: Int,
    val output: FileReference
) {

    init {
        if (w % 2 != 0 || h % 2 != 0) throw RuntimeException("width and height must be divisible by 2")
    }

    val startTime = Engine.gameTime

    private lateinit var videoOut: OutputStream
    private lateinit var process: Process

    fun init() {

        if (output.exists) output.delete()
        else output.getParent()?.tryMkdirs()

        val extension = output.lcExtension
        val isGIF = extension == "gif"

        /**
         * first create the video,
         * then add audio later;
         * because I don't know how to send audio and video data to ffmpeg
         * at the same time with only one output stream
         * */
        val rawFormat = "rgb24"

        // Incompatible pixel format 'yuv420p' for codec 'gif', auto-selecting format 'bgr8'
        val dstFormat = if (isGIF) "bgr8" else "yuv420p"
        val fpsString = fps.toString()

        val args = arrayListOf(
            "-f", "rawvideo",
            "-s", "${w}x${h}",
            "-r", fpsString,
            "-pix_fmt", rawFormat,
            "-i", "pipe:0", // output buffer
        )

        val encoding = if (!isGIF) videoCodecByExtension(extension) else null
        if (encoding != null) {
            args += "-c:v"
            args += encoding // "libx264"
        }

        args += "-an" // no audio
        args += "-r"
        args += fpsString
        // "-qp", "0", // constant quality

        if (!isGIF) {
            // constant rate factor
            args += "-crf"
            args += quality.toString()
        }

        args += "-pix_fmt"
        args += dstFormat

        if (encoding == "libx264") {
            // other codecs support other values
            args += "-preset"
            args += balance.internalName
        }

        val builder = BetterProcessBuilder(FFMPEG.ffmpegPathString, args.size + 4, true)
        if (args.isNotEmpty()) builder += "-hide_banner"

        builder += args
        if (type.internalName != null) {
            builder += "-tune"
            builder += type.internalName
        }
        builder += output.absolutePath

        process = builder.start()
        logOutput(null, process.inputStream, true)
        thread(name = "VideoCreatorOutput") {
            processOutput(LOGGER, "Video", startTime, fps, totalFrameCount, process.errorStream) {
                close()
            }
        }

        videoOut = process.outputStream.buffered()

        LOGGER.info("Total frame count: $totalFrameCount")
    }

    private val pixelByteCount = w * h * 3

    private val buffer1 = ByteBufferPool.allocateDirect(pixelByteCount)
    private val buffer2 = ByteBufferPool.allocateDirect(pixelByteCount)

    fun writeFrame(frame: Framebuffer, frameIndex: Long, callback: () -> Unit) {

        GFX.check()

        if (frame.w != w || frame.h != h) throw IllegalArgumentException("Resolution does not match!")
        frame.bindDirectly()
        Frame.invalidate()

        val buffer = if (frameIndex % 2 == 0L) buffer1 else buffer2

        buffer.position(0)
        readAlignment(w * 3)
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
                    LOGGER.error("Closing", e)
                    close()
                }
            }
        }
    }

    fun writeFrame(frame: Image) {
        if (frame.width != w || frame.height != h) throw IllegalArgumentException("Resolution does not match!")
        val output = videoOut
        synchronized(output) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val color = frame.getRGB(x, y)
                    output.write(color.shr(16))
                    output.write(color.shr(8))
                    output.write(color)
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
    open fun close() {
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
        val defaultQuality = 23

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
            update: (frameIndex: Int, callback: () -> Unit) -> Unit
        ) {
            val creator = VideoCreator(
                w, h, fps, numUpdates + 1L, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, dst
            )
            creator.init()
            var frameIndex = 0
            fun writeFrame() {
                creator.writeFrame(fb, frameIndex.toLong()) {
                    if (++frameIndex <= numUpdates) {
                        GFX.addGPUTask("VideoCreator", 1) {
                            update(frameIndex, ::writeFrame)
                        }
                    } else {
                        creator.close()
                        Engine.requestShutdown()
                    }
                }
            }
            GFX.addGPUTask("VideoCreator", 1) { writeFrame() }
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
            shutdown: Boolean,
            getNextFrame: (callback: (Texture2D) -> Unit) -> Unit
        ) {
            val creator = VideoCreator(
                w, h, fps, numFrames, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, dst
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
                            if (GFX.isGFXThread()) {
                                writeFrame()
                            } else {
                                GFX.addGPUTask("VideoCreator", 1) {
                                    writeFrame()
                                }
                            }
                        } else {
                            creator.close()
                            Engine.requestShutdown()
                        }
                    }
                }
            }
            if (GFX.isGFXThread()) {
                writeFrame()
            } else {
                GFX.addGPUTask("VideoCreator", 1) { writeFrame() }
            }
            if (shutdown) GFX.workGPUTasksUntilShutdown()
        }

        /**
         * render a video from a set of textures
         * */
        fun renderVideo2(
            w: Int,
            h: Int,
            fps: Double,
            dst: FileReference,
            numFrames: Long,
            getNextFrame: (Long) -> Image?
        ) {
            val creator = VideoCreator(
                w, h, fps, numFrames, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, dst
            )
            creator.init()
            for (i in 0 until numFrames) {
                val frame = getNextFrame(i) ?: break
                creator.writeFrame(frame)
            }
            creator.close()
        }

    }

}