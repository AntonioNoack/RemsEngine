package me.anno.video

import me.anno.Engine
import me.anno.Time
import me.anno.utils.Threads
import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.GPUTasks.workGPUTasksUntilShutdown
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D.Companion.setReadAlignment
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.image.Image
import me.anno.image.raw.GPUImage
import me.anno.io.BufferedIO.useBuffered
import me.anno.io.Streams.writeBE24
import me.anno.io.Streams.writeBE32
import me.anno.io.files.FileReference
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.maths.Maths.min
import me.anno.utils.async.Callback
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Booleans.hasFlag
import me.anno.video.Codecs.videoCodecByExtension
import me.anno.video.ffmpeg.FFMPEG
import me.anno.video.ffmpeg.FFMPEGEncodingBalance
import me.anno.video.ffmpeg.FFMPEGEncodingType
import me.anno.video.ffmpeg.FFMPEGStream.Companion.logOutput
import me.anno.video.ffmpeg.FFMPEGUtils.processOutput
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

// todo support HDR video, too
open class VideoCreator(
    val width: Int, val height: Int,
    val fps: Double,
    val totalFrameCount: Int,
    val balance: FFMPEGEncodingBalance,
    val type: FFMPEGEncodingType,
    val quality: Int,
    val withAlpha: Boolean, // use .gif or .webm for this
    var output: FileReference
) {

    init {
        if (width % 2 != 0 || height % 2 != 0)
            throw RuntimeException("width and height must be divisible by 2")
    }

    val startTime = Time.nanoTime

    private lateinit var videoOut: OutputStream
    private lateinit var process: Process

    val extension = output.lcExtension
    val isGIF = extension == "gif"

    fun init() {

        // output must not exist
        var i = 0
        while (output.exists) {
            output.delete()
            output.getParent().tryMkdirs()
            output.invalidate()
            if (output.exists) {
                // change output
                output = output.getSibling(output.nameWithoutExtension + "-${i++}." + output.extension)
                LOGGER.warn("Changing output, because it still exists after deleting it")
            }
        }

        /**
         * first create the video,
         * then add audio later;
         * because I don't know how to send audio and video data to ffmpeg
         * at the same time with only one output stream
         * */
        val rawFormat = if (withAlpha) "rgba"
        else "rgb24"

        // Incompatible pixel format 'yuv420p' for codec 'gif', auto-selecting format 'bgr8'
        // todo "alpha isn't supported by hevc and nvenc" -> can we prevent using them?
        val dstFormat = if (isGIF) {
            if (withAlpha) "rgba"
            else "bgr8"
        } else {
            if (withAlpha) "yuva420p"
            else "yuv420p"
        }
        val fpsString = fps.toString()

        val args = arrayListOf(
            "-f", "rawvideo",
            "-s", "${width}x${height}",
            "-r", fpsString,
            "-pix_fmt", rawFormat,
            // completely wrong:
            // "-color_primaries", "bt709", "-color_trc", "bt709", "-colorspace", "bt709",
            "-i", "pipe:0", // output buffer
        )

        val encoding = if (!isGIF) videoCodecByExtension(extension, withAlpha) else null
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

        if (isGIF && withAlpha) {
            // todo transparent gifs aren't working yet, probably need some stupid flags
        }

        val builder = BetterProcessBuilder(FFMPEG.ffmpeg, args.size + 4, true)
        if (args.isNotEmpty()) builder += "-hide_banner"

        builder += args
        if (type.internalName != null) {
            builder += "-tune"
            builder += type.internalName
        }
        builder += output.absolutePath

        process = builder.start()
        logOutput(null, output.absolutePath, process.inputStream, true)
        Threads.runTaskThread("VideoCreator:updates") {
            processOutput(LOGGER, "Video", startTime, fps, totalFrameCount, process.errorStream) {
                close()
            }
        }

        videoOut = process.outputStream.useBuffered()

        LOGGER.info("Total frame count: $totalFrameCount")
    }

    private val pixelByteCount = width * height * (if (withAlpha) 4 else 3)
    private val buffer1 = ByteBufferPool.allocateDirect(pixelByteCount)
    private val buffer2 = ByteBufferPool.allocateDirect(pixelByteCount)

    fun writeFrame(frame: Framebuffer, frameIndex: Int, callback: () -> Unit) {

        GFX.check()

        if (frame.width != width || frame.height != height) throw IllegalArgumentException("Resolution does not match!")
        frame.bindDirectly()
        Frame.invalidate()

        val buffer = if (frameIndex.hasFlag(1)) buffer1 else buffer2

        buffer.position(0)
        setReadAlignment(width * 3)
        GL46C.glReadPixels(
            0, 0, width, height,
            if (withAlpha) GL46C.GL_RGBA else GL46C.GL_RGB,
            GL46C.GL_UNSIGNED_BYTE, buffer
        )
        buffer.position(0)

        GFX.check()

        copyQueue += {// offload to other thread
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
        if (frame.width != width || frame.height != height) throw IllegalArgumentException("Resolution does not match!")
        val frame = (frame as? GPUImage)?.asIntImage() ?: frame
        val output = videoOut
        synchronized(output) {
            val withAlpha = withAlpha
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = frame.getRGB(x, y)
                    if (withAlpha) output.writeBE32(color)
                    else output.writeBE24(color)
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
        LOGGER.debug("Closing stream")
        try {
            synchronized(videoOut) {
                videoOut.flush()
                videoOut.close()
            }
        } catch (_: IOException) {

        }
        process.waitFor()
        if (output.exists) LOGGER.info("Saved video without audio to $output")
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(VideoCreator::class)

        @JvmField
        val defaultQuality = 23

        private val copyQueue = ProcessingQueue("VideoCreator:copy")

        /**
         * render a video from a framebuffer
         * */
        @JvmStatic
        fun renderVideo(
            w: Int,
            h: Int,
            fps: Double,
            dst: FileReference,
            numUpdates: Int,
            fb: Framebuffer,
            update: (frameIndex: Int, callback: () -> Unit) -> Unit,
            callback: ((FileReference) -> Unit)? = null
        ) {
            val creator = VideoCreator(
                w, h, fps, numUpdates + 1, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, false, dst
            )
            creator.init()
            var frameIndex = 0
            fun writeFrame() {
                creator.writeFrame(fb, frameIndex) {
                    if (++frameIndex <= numUpdates) {
                        addGPUTask("VideoCreator", 1) {
                            update(frameIndex, ::writeFrame)
                        }
                    } else {
                        creator.close()
                        if (callback == null) Engine.requestShutdown()
                        else callback(creator.output)
                    }
                }
            }
            addGPUTask("VideoCreator", 1) { writeFrame() }
            workGPUTasksUntilShutdown()
        }

        /**
         * render a video from a set of textures
         * */
        @JvmStatic
        fun renderVideo(
            w: Int, h: Int, fps: Double, dst: FileReference,
            numFrames: Int, shutdown: Boolean,
            getNextFrame: (callback: Callback<ITexture2D>) -> Unit,
            callback: (() -> Unit)? = null
        ) {
            val creator = VideoCreator(
                w, h, fps, numFrames, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, false, dst
            )
            creator.init()
            var frameCount = 0
            val fb = Framebuffer("frame", w, h, 1, TargetType.UInt8x4, DepthBufferType.NONE)
            fun writeFrame() {
                getNextFrame { texture0, _ ->
                    val texture = texture0 ?: blackTexture
                    useFrame(fb) {
                        Blitting.copyNoAlpha(texture, texture0 != null)
                    }
                    creator.writeFrame(fb, frameCount) {
                        if (++frameCount <= numFrames) {
                            if (GFX.isGFXThread()) {
                                writeFrame()
                            } else {
                                addGPUTask("VideoCreator", 1) {
                                    writeFrame()
                                }
                            }
                        } else {
                            creator.close()
                            if (callback == null) Engine.requestShutdown()
                            else callback()
                        }
                    }
                }
            }
            if (GFX.isGFXThread()) {
                writeFrame()
            } else {
                addGPUTask("VideoCreator", 1) { writeFrame() }
            }
            if (shutdown) workGPUTasksUntilShutdown()
        }

        /**
         * render a video from a set of textures
         * */
        @JvmStatic
        fun renderVideo2(
            w: Int,
            h: Int,
            fps: Double,
            dst: FileReference,
            numFrames: Int,
            getNextFrame: (Int) -> Image?
        ) {
            val creator = VideoCreator(
                w, h, fps, numFrames, FFMPEGEncodingBalance.S1,
                FFMPEGEncodingType.DEFAULT, defaultQuality, false, dst
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