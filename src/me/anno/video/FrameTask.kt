package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.RenderState.blendMode
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.files.FileReference
import me.anno.objects.Transform
import me.anno.studio.rems.Scene
import me.anno.utils.Color.rgba
import me.anno.utils.Threads.threadWithName
import org.apache.logging.log4j.LogManager
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs


class FrameTask(
    val width: Int,
    val height: Int,
    val fps: Double,
    scene: Transform,
    val motionBlurSteps: Int,
    val shutterPercentage: Float,
    val time: Double,
    val dst: FileReference
) : AudioCreator(scene, 0.0, 1, emptyList()) {

    val partialFrame = Framebuffer(
        "VideoBackgroundTask-partial", width, height, 1, 1,
        false, Framebuffer.DepthBufferType.TEXTURE
    )

    val averageFrame = Framebuffer(
        "VideoBackgroundTask-sum", width, height, 1, 1,
        true, Framebuffer.DepthBufferType.TEXTURE
    )

    fun start(callback: () -> Unit) {

        /**
         * runs on GPU thread
         * */
        GFX.addGPUTask(width, height) {
            if (renderFrame(time)) {
                writeFrame(averageFrame)
                destroy()
                callback()
            } else {
                // waiting
                threadWithName("FrameTask::start") { start(callback) }
            }
        }

    }

    fun writeFrame(frame: Framebuffer) {

        val pixelByteCount = 3 * width * height
        val pixels = BufferUtils.createByteBuffer(pixelByteCount)

        GFX.check()

        frame.bindDirectly(false)
        Frame.invalidate()

        // val t0 = Clock()
        pixels.position(0)
        glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixels)
        pixels.position(0)
        // t0.stop("read pixels"), 0.03s on RX 580, 1080p

        GFX.check()

        threadWithName("FrameTask::writeFrame") {// offload to other thread
            // val c1 = Clock()
            val image = BufferedImage(width, height, 1)
            val buffer2 = image.raster.dataBuffer
            for (i in 0 until width * height) {
                val j = i * 3
                buffer2.setElem(i, rgba(pixels[j], pixels[j + 1], pixels[j + 2], -1))
            }
            // c1.stop("wrote to buffered image"), 0.025s on R5 2600, 1080p
            if (dst.exists) dst.delete()
            if (!ImageIO.write(image, dst.extension, dst.unsafeFile)) {
                LOGGER.warn("Could not find writer for image format ${dst.extension}!")
            } else {
                // c1.stop("saved to file"), 0.07s on NVME SSD
                LOGGER.info("Wrote frame to $dst")
            }
        }

    }

    private fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        // is this correct??? mmh...
        val renderer = Renderer.colorRenderer

        var needsMoreSources = false

        if (motionBlurSteps < 2 || shutterPercentage <= 1e-3f) {
            useFrame(0, 0, width, height, false, averageFrame) {
                try {
                    Scene.draw(
                        camera, scene, 0, 0, width, height,
                        time, true, renderer, null
                    )
                    if (!GFX.isFinalRendering) throw RuntimeException()
                } catch (e: MissingFrameException) {
                    // e.printStackTrace()
                    missingResource = e.message ?: ""
                    needsMoreSources = true
                }
            }
        } else {
            useFrame(averageFrame) {

                Frame.bind()
                glClearColor(0f, 0f, 0f, 0f)
                glClear(GL_COLOR_BUFFER_BIT)

                var i = 0
                while (i++ < motionBlurSteps && !needsMoreSources) {
                    FBStack.clear(width, height)
                    useFrame(partialFrame, renderer) {
                        try {
                            Scene.draw(
                                camera, scene, 0, 0, width, height,
                                time + (i - motionBlurSteps / 2f) * shutterPercentage / (fps * motionBlurSteps),
                                true, renderer, null
                            )
                            if (!GFX.isFinalRendering) throw RuntimeException()
                        } catch (e: MissingFrameException) {
                            // e.printStackTrace()
                            missingResource = e.message ?: ""
                            needsMoreSources = true
                        }
                    }
                    if (!needsMoreSources) {
                        partialFrame.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                        blendMode.use(BlendMode.PURE_ADD) {
                            depthMode.use(false) {
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
        GFX.addGPUTask(width, height) {
            partialFrame.destroy()
            averageFrame.destroy()
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(FrameTask::class)
        var lastPrinted = 0L
        var missingResource = ""
            set(value) {
                if (value.isNotEmpty() && abs(GFX.gameTime - lastPrinted) > 1000_000_000) {
                    lastPrinted = GFX.gameTime
                    LOGGER.info("Waiting for $value")
                }
                field = value
            }
    }

}