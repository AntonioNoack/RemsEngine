package me.anno.remsstudio.video

import me.anno.gpu.shader.Renderer
import me.anno.io.files.FileReference
import me.anno.remsstudio.Scene
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Transform
import me.anno.video.FrameTask

class FrameTaskV2(
    width: Int,
    height: Int,
    fps: Double,
    val scene: Transform,
    val camera: Camera,
    motionBlurSteps: Int,
    shutterPercentage: Float,
    time: Double,
    dst: FileReference
) : FrameTask(width, height, fps, motionBlurSteps, shutterPercentage, time, dst) {
    override fun renderScene(time: Double, flipY: Boolean, renderer: Renderer) {
        Scene.draw(
            camera, scene, 0, 0, width, height, time,
            flipY, renderer, null
        )
    }
}