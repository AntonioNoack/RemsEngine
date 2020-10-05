package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.objects.Camera
import me.anno.studio.Scene
import me.anno.studio.Studio.nullCamera
import me.anno.studio.Studio.root
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.min

class VideoBackgroundTask(val video: VideoCreator) {

    val cameras = root.listOfAll.filter { it is Camera }.toList() as List<Camera>

    val camera = cameras.firstOrNull() ?: nullCamera

    val framebuffer =
        Framebuffer("VideoBackgroundTask", video.w, video.h, 1, 1, false, Framebuffer.DepthBufferType.TEXTURE)

    val renderingIndex = AtomicInteger(0)
    val savingIndex = AtomicInteger(0)

    val totalFrameCount = video.totalFrameCount

    var isDone = false

    fun start() {

        if (renderingIndex.get() < totalFrameCount) addNextTask()
        else video.close()

    }

    fun addNextTask() {

        if (min(renderingIndex.get(), savingIndex.get()) < totalFrameCount) {// not done yet

            /**
             * runs on GPU thread
             * */
            if (renderingIndex.get() < savingIndex.get() + 2) {
                GFX.addGPUTask(1000, 1000) {

                    val frameIndex = renderingIndex.get()
                    if (renderFrame(frameIndex / video.fps)) {
                        video.writeFrame(framebuffer, frameIndex) {
                            // it was saved -> everything works well :)
                            savingIndex.incrementAndGet()
                        }
                        renderingIndex.incrementAndGet()
                        addNextTask()
                    } else {
                        // ("waiting for frame (data missing)")
                        // waiting
                        thread {
                            Thread.sleep(1)
                            addNextTask()
                        }
                    }

                }
            } else {
                // ("waiting for frame (writing is slow)")
                // waiting for saving to ffmpeg
                thread {
                    Thread.sleep(100)
                    addNextTask()
                }
            }

        } else {
            video.close()
            isDone = true
        }

    }

    fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true
        Frame(0, 0, video.w, video.h, framebuffer) {
            try {
                Scene.draw(camera, 0, 0, video.w, video.h, time, true, ShaderPlus.DrawMode.COLOR_SQUARED, null)
            } catch (e: MissingFrameException) {
                GFX.isFinalRendering = false
            }
        }

        if(!GFX.isFinalRendering) return false

        GFX.isFinalRendering = false

        GFX.check()

        return true

    }

}