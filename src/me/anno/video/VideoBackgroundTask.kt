package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.objects.Camera
import me.anno.studio.Scene
import me.anno.studio.Studio.nullCamera
import me.anno.studio.Studio.root

class VideoBackgroundTask(val video: VideoCreator){

    val cameras = root.listOfAll.filter { it is Camera }.toList() as List<Camera>

    val camera = cameras.firstOrNull() ?: nullCamera

    // todo show the progress somehow
    // (percent, time used, expected time remaining)

    val framebuffer = Framebuffer(video.w, video.h, 1, 1, false, Framebuffer.DepthBufferType.TEXTURE)

    var frameIndex = 0
    val totalFrameCount = video.totalFrameCount

    var isDone = false

    fun start(){

        if(frameIndex < totalFrameCount) addNextTask()
        else video.close()

    }

    fun addNextTask(){

        if(frameIndex < totalFrameCount){

            GFX.addGPUTask {

                if(renderFrame(frameIndex / video.fps)){
                    video.writeFrame(framebuffer){
                        frameIndex++
                        addNextTask()
                    }
                } else {
                    // waiting
                    addNextTask()
                }

                5 // 5 tokens for this frame ;)
            }

        } else {
            video.close()
            isDone = true
        }

    }

    fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        try {
            Scene.draw(framebuffer, camera, 0, 0, video.w, video.h, time, flipY = true, drawMode = ShaderPlus.DrawMode.COLOR)
        } catch (e: MissingFrameException){
            GFX.isFinalRendering = false
            return false
        }

        GFX.isFinalRendering = false

        GFX.check()

        return true

    }

}