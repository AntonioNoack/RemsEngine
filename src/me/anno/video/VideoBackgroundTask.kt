package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.studio.Scene
import me.anno.studio.Studio

class VideoBackgroundTask(val video: VideoCreator){

    // todo show the progress somehow
    // (percent, time used, expected time remaining)

    val framebuffer = Framebuffer(video.w, video.h, 1, 1, false, Framebuffer.DepthBufferType.TEXTURE)

    var frameIndex = 0
    val totalFrameCount = 200

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

        } else video.close()

    }

    fun renderFrame(time: Double): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        try {
            Scene.draw(framebuffer, Studio.selectedCamera, 0, 0, video.w, video.h, time, flipY = true, useFakeColors = false)
        } catch (e: MissingFrameException){
            GFX.isFinalRendering = false
            return false
        }

        GFX.isFinalRendering = false

        GFX.check()

        return true

    }

}