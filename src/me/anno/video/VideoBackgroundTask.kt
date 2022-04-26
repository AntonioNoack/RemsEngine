package me.anno.video

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.OpenGL.blendMode
import me.anno.gpu.OpenGL.depthMode
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.video.FrameTask.Companion.missingResource
import org.lwjgl.opengl.GL11C.*
import java.util.concurrent.atomic.AtomicLong

abstract class VideoBackgroundTask(
    val video: VideoCreator
) {

    abstract fun getMotionBlurSteps(time: Double): Int
    abstract fun getShutterPercentage(time: Double): Float

    private val partialFrame = Framebuffer(
        "VideoBackgroundTask-partial", video.w, video.h, 1, 1,
        false, DepthBufferType.TEXTURE
    )

    private val averageFrame = Framebuffer(
        "VideoBackgroundTask-sum", video.w, video.h, 1, 1,
        true, DepthBufferType.TEXTURE
    )

    private val renderingIndex = AtomicLong(0)
    private val savingIndex = AtomicLong(0)
    private val totalFrameCount = video.totalFrameCount

    var isDone = false

    fun start() {
        if (renderingIndex.get() < totalFrameCount) {
            addNextTask()
        } else video.close()
    }

    var isDoneRenderingAndSaving = false

    private fun addNextTask() {

        if (isDoneRenderingAndSaving) {
            video.close()
            destroy()
            isDone = true
            return
        }


        /**
         * runs on GPU thread
         * */
        val ri = renderingIndex.get()
        if (ri < totalFrameCount && ri < savingIndex.get() + 2) {
            GFX.addGPUTask(video.w, video.h, ::tryRenderingFrame)
        } else {
            // waiting for saving to ffmpeg
            threadWithName("VBT/2") { addNextTask() }
        }

    }

    private fun tryRenderingFrame() {
        val frameIndex = renderingIndex.get()
        if (renderFrame(frameIndex / video.fps)) {
            renderingIndex.incrementAndGet()
            video.writeFrame(averageFrame, frameIndex) {
                // it was saved -> everything works well :)
                val si = savingIndex.incrementAndGet()
                if (si == totalFrameCount) {
                    isDoneRenderingAndSaving = true
                } else if (si > totalFrameCount) throw RuntimeException("too many saves: $si, $totalFrameCount")
            }
            addNextTask()
        } else {
            // waiting
            threadWithName("VBT/1") { addNextTask() }
        }
    }

    private fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        // is this correct??? mmh...
        val renderer = Renderer.colorRenderer

        var needsMoreSources = false
        val motionBlurSteps = getMotionBlurSteps(time)
        val shutterPercentage = getShutterPercentage(time)

        if (motionBlurSteps < 2 || shutterPercentage <= 1e-3f) {
            useFrame(0, 0, video.w, video.h, false, averageFrame, renderer) {
                try {
                    renderScene(time, true, renderer)
                    if (!GFX.isFinalRendering) throw RuntimeException()
                } catch (e: MissingFrameException) {
                    // e.printStackTrace()
                    missingResource = e.message ?: ""
                    needsMoreSources = true
                }
            }
        } else {
            useFrame(averageFrame, Renderer.colorRenderer) {

                Frame.bind()
                glClearColor(0f, 0f, 0f, 0f)
                glClear(GL_COLOR_BUFFER_BIT)

                var i = 0
                while (i++ < motionBlurSteps && !needsMoreSources) {
                    FBStack.reset(video.w, video.h)
                    useFrame(partialFrame, renderer) {
                        try {
                            renderScene(
                                time + (i - motionBlurSteps / 2f) * shutterPercentage / (video.fps * motionBlurSteps),
                                true, renderer
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
                            depthMode.use(DepthMode.ALWAYS) {
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

    abstract fun renderScene(time: Double, flipY: Boolean, renderer: Renderer)

    private fun destroy() {
        GFX.addGPUTask(video.w, video.h) {
            partialFrame.destroy()
            averageFrame.destroy()
        }
    }

}