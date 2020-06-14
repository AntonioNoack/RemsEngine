package me.anno.video

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.studio.Scene

class VideoBackgroundTask(val video: VideoCreator){

    // todo show the progress somehow

    val framebuffer = Framebuffer(video.w, video.h, 1, false, Framebuffer.DepthBufferType.TEXTURE)

    var frameIndex = 0
    val totalFrameCount = 200

    fun start(){

        if(frameIndex < totalFrameCount) addNextTask()
        else video.close()

    }

    fun addNextTask(){

        if(frameIndex < totalFrameCount){

            GFX.addTask {

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

    fun renderFrame(time: Float): Boolean {

        GFX.check()

        GFX.isFinalRendering = true

        try {
            Scene.draw(framebuffer, GFX.selectedCamera, 0, 0, video.w, video.h, time, flipY = true)
        } catch (e: MissingFrameException){
            GFX.isFinalRendering = false
            return false
        }

        GFX.isFinalRendering = false

        GFX.check()

        return true

    }

}