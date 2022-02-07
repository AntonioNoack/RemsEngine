package me.anno.remsstudio.video

import me.anno.gpu.shader.Renderer
import me.anno.remsstudio.Scene
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Transform
import me.anno.video.VideoBackgroundTask
import me.anno.video.VideoCreator

class VideoBackgroundTaskV2(
    video: VideoCreator,
    val scene: Transform,
    val camera: Camera,
    val motionBlurSteps: AnimatedProperty<Int>,
    val shutterPercentage: AnimatedProperty<Float>
) : VideoBackgroundTask(video) {

    override fun getMotionBlurSteps(time: Double): Int {
        return motionBlurSteps[time]
    }

    override fun getShutterPercentage(time: Double): Float {
        return shutterPercentage[time]
    }

    override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
        Scene.draw(
            camera, scene, 0, 0, video.w, video.h, time,
            true, renderer, null
        )
    }

}