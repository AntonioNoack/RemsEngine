package me.anno.video

import me.anno.gpu.Blitting
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.alwaysDepthMode
import me.anno.gpu.GFXState.blendMode
import me.anno.gpu.GFXState.depthMode
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.renderer.Renderer
import me.anno.video.FrameTask.Companion.missingResource
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

abstract class VideoBackgroundTask(
    val creator: VideoCreator,
    val samples: Int,
) {

    var isDone = false
    var isCancelled = false

    abstract fun getMotionBlurSteps(time: Double): Int
    abstract fun getShutterPercentage(time: Double): Float

    private val partialFrame = Framebuffer(
        "VideoBackgroundTask-partial", creator.width, creator.height, samples,
        TargetType.UInt8x4, DepthBufferType.TEXTURE
    )

    private val averageFrame = Framebuffer(
        "VideoBackgroundTask-sum", creator.width, creator.height, 1,
        TargetType.UInt8x4, DepthBufferType.TEXTURE
    )

    private val renderingIndex = AtomicInteger(0)
    private val savingIndex = AtomicInteger(0)
    private val totalFrameCount = creator.totalFrameCount

    fun start() {
        if (renderingIndex.get() < totalFrameCount) {
            addNextTask()
        } else destroy()
    }

    var isDoneRenderingAndSaving = false

    private fun addNextTask() {

        if (isCancelled || creator.wasClosed) {
            isCancelled = true
            destroy()
            return
        }

        if (isDoneRenderingAndSaving) {
            destroy()
            isDone = true
            return
        }

        /**
         * runs on GPU thread
         * */
        val ri = renderingIndex.get()
        if (ri < totalFrameCount && ri < savingIndex.get() + 2) {
            addGPUTask("VideoBackgroundTask", creator.width, creator.height, ::tryRenderingFrame)
        } else {
            // waiting for saving to ffmpeg
            thread(name = "VBT/2") { addNextTask() }
        }
    }

    private fun tryRenderingFrame() {

        if (isCancelled || creator.wasClosed) {
            isCancelled = true
            destroy()
            return
        }

        val frameIndex = renderingIndex.get()
        if (renderFrame(frameIndex / creator.fps)) {
            renderingIndex.incrementAndGet()
            creator.writeFrame(averageFrame, frameIndex) {
                // it was saved -> everything works well :)
                val si = savingIndex.incrementAndGet()
                if (si == totalFrameCount) {
                    isDoneRenderingAndSaving = true
                } else if (si > totalFrameCount) throw RuntimeException("too many saves: $si, $totalFrameCount")
            }
            addNextTask()
        } else {
            // waiting
            thread(name = "VBT/1") { addNextTask() }
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
            useFrame(0, 0, creator.width, creator.height, averageFrame, renderer) {
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
            useFrame(averageFrame, Renderer.copyRenderer) {

                averageFrame.clearColor(0)

                var i = 0
                while (i++ < motionBlurSteps && !needsMoreSources) {
                    FBStack.reset(creator.width, creator.height)
                    useFrame(partialFrame, renderer) {
                        try {
                            renderScene(
                                time + (i - motionBlurSteps / 2f) * shutterPercentage / (creator.fps * motionBlurSteps),
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
                        partialFrame.bindTrulyNearest(0)
                        blendMode.use(BlendMode.PURE_ADD) {
                            depthMode.use(alwaysDepthMode) {
                                // write with alpha 1/motionBlurSteps
                                Blitting.copy(1f / motionBlurSteps, 1, true)
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
        creator.close()
        addGPUTask("VideoBackgroundTask.destroy()", creator.width, creator.height) {
            partialFrame.destroy()
            averageFrame.destroy()
        }
    }
}