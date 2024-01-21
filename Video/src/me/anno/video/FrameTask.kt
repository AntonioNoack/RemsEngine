package me.anno.video

import me.anno.Time
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.ByteImage
import me.anno.io.files.FileReference
import me.anno.utils.pooling.ByteBufferPool
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C
import kotlin.concurrent.thread
import kotlin.math.abs

abstract class FrameTask(
    val width: Int,
    val height: Int,
    private val fps: Double,
    private val motionBlurSteps: Int,
    private val shutterPercentage: Float,
    private val time: Double,
    private val dst: FileReference
) {

    var isCancelled = false

    abstract fun renderScene(
        time: Double,
        flipY: Boolean, renderer: Renderer
    )

    private val partialFrame = Framebuffer(
        "VideoBackgroundTask-partial", width, height, 1, 1,
        false, DepthBufferType.TEXTURE
    )

    private val averageFrame = Framebuffer(
        "VideoBackgroundTask-sum", width, height, 1, 1,
        true, DepthBufferType.TEXTURE
    )

    fun start(callback: () -> Unit) {
        GFX.addGPUTask("FrameTask", width, height) {
            start1(callback)
        }
    }

    private fun start1(callback: () -> Unit) {
        if (isCancelled) {
            callback()
            return
        }
        if (renderFrame(time)) {
            writeFrame(averageFrame)
            destroy()
            callback()
        } else {
            // waiting
            GFX.addNextGPUTask("FrameTask::start", 1) {
                start1(callback)
            }
        }
    }

    fun writeFrame(frame: Framebuffer) {

        val pixelByteCount = 3 * width * height
        val pixels = ByteBufferPool.allocateDirect(pixelByteCount)

        GFX.check()

        frame.bindDirectly()
        Frame.invalidate()

        // val t0 = Clock()
        pixels.position(0)
        Texture2D.setReadAlignment(width)
        GL46C.glReadPixels(0, 0, width, height, GL46C.GL_RGB, GL46C.GL_UNSIGNED_BYTE, pixels)
        pixels.position(0)
        // t0.stop("read pixels"), 0.03s on RX 580, 1080p

        GFX.check()

        thread(name = "FrameTask::writeFrame") {// offload to other thread
            // val c1 = Clock()
            val image = ByteImage(width, height, ByteImage.Format.RGB)
            pixels.get(image.data, 0, image.data.size)
            // c1.stop("wrote to buffered image"), 0.025s on R5 2600, 1080p
            if (dst.exists) dst.delete()
            image.write(dst)
            // c1.stop("saved to file"), 0.07s on NVME SSD
            LOGGER.info("Wrote frame to $dst")
            ByteBufferPool.free(pixels)
        }
    }

    private fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        // is this correct??? mmh...
        val renderer = Renderer.colorRenderer

        var needsMoreSources = false

        if (motionBlurSteps < 2 || shutterPercentage <= 1e-3f) {
            GFXState.useFrame(0, 0, width, height, averageFrame) {
                try {
                    renderScene(time, true, renderer)
                    if (!GFX.isFinalRendering) throw IllegalStateException()
                } catch (e: MissingFrameException) {
                    // e.printStackTrace()
                    missingResource = e.message ?: ""
                    needsMoreSources = true
                }
            }
        } else {
            GFXState.useFrame(averageFrame) {

                averageFrame.clearColor(0)

                var i = 0
                while (i++ < motionBlurSteps && !needsMoreSources) {
                    FBStack.reset(width, height)
                    GFXState.useFrame(partialFrame, renderer) {
                        try {
                            renderScene(
                                time + (i - motionBlurSteps / 2f) * shutterPercentage / (fps * motionBlurSteps),
                                true,
                                renderer
                            )
                            if (!GFX.isFinalRendering) throw IllegalStateException()
                        } catch (e: MissingFrameException) {
                            // e.printStackTrace()
                            missingResource = e.message ?: ""
                            needsMoreSources = true
                        }
                    }
                    if (!needsMoreSources) {
                        partialFrame.bindTrulyNearest(0)
                        GFXState.blendMode.use(BlendMode.PURE_ADD) {
                            GFXState.depthMode.use(DepthMode.ALWAYS) {
                                // write with alpha 1/motionBlurSteps
                                GFX.copy(1f / motionBlurSteps)
                            }
                        }
                    }
                }

            }
        }

        GFX.isFinalRendering = false

        if (needsMoreSources) return false

        GFX.check()

        return true
    }

    fun destroy() {
        GFX.addGPUTask("FrameTask.destroy()", width, height) {
            partialFrame.destroy()
            averageFrame.destroy()
        }
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(FrameTask::class)

        @JvmField
        var lastPrinted = -1L

        @JvmStatic
        var missingResource = ""
            set(value) {
                if (value.isNotEmpty() && (lastPrinted == -1L || abs(Time.nanoTime - lastPrinted) > 1000_000_000L)) {
                    lastPrinted = Time.nanoTime
                    LOGGER.info("Waiting for $value")
                }
                field = value
            }
    }
}