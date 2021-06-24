package me.anno.video

import me.anno.animation.AnimatedProperty
import me.anno.gpu.GFX
import me.anno.gpu.RenderSettings.blendMode
import me.anno.gpu.RenderSettings.depthMode
import me.anno.gpu.RenderSettings.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.rems.Scene
import me.anno.utils.Threads.threadWithName
import me.anno.video.FrameTask.Companion.missingResource
import org.lwjgl.opengl.GL11.*
import java.util.concurrent.atomic.AtomicLong

class VideoBackgroundTask(
    val video: VideoCreator,
    val scene: Transform,
    val camera: Camera,
    val motionBlurSteps: AnimatedProperty<Int>,
    val shutterPercentage: AnimatedProperty<Float>
) {

    private val partialFrame = Framebuffer(
        "VideoBackgroundTask-partial", video.w, video.h, 1, 1,
        false, Framebuffer.DepthBufferType.TEXTURE
    )

    private val averageFrame = Framebuffer(
        "VideoBackgroundTask-sum", video.w, video.h, 1, 1,
        true, Framebuffer.DepthBufferType.TEXTURE
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
        val drawMode = Renderer.colorRenderer

        var needsMoreSources = false
        val motionBlurSteps = motionBlurSteps[time]
        val shutterPercentage = shutterPercentage[time]

        if (motionBlurSteps < 2 || shutterPercentage <= 1e-3f) {
            useFrame(0, 0, video.w, video.h, false, averageFrame, drawMode) {
                try {
                    Scene.draw(
                        camera, scene, 0, 0, video.w, video.h,
                        time, true, drawMode, null
                    )
                    if (!GFX.isFinalRendering) throw RuntimeException()
                } catch (e: MissingFrameException) {
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
                    FBStack.clear(video.w, video.h)
                    useFrame(partialFrame, drawMode) {
                        try {
                            Scene.draw(
                                camera, scene, 0, 0, video.w, video.h,
                                time + (i - motionBlurSteps / 2f) * shutterPercentage / (video.fps * motionBlurSteps),
                                true, drawMode, null
                            )
                            if (!GFX.isFinalRendering) throw RuntimeException()
                        } catch (e: MissingFrameException) {
                            missingResource = e.message ?: ""
                            needsMoreSources = true
                        }
                    }
                    if (!needsMoreSources) {
                        partialFrame.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                        blendMode.use(BlendMode.PURE_ADD){
                            depthMode.use(false){
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

    private fun destroy() {
        GFX.addGPUTask(video.w, video.h) {
            partialFrame.destroy()
            averageFrame.destroy()
        }
    }

}