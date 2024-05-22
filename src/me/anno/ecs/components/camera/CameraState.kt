package me.anno.ecs.components.camera

import me.anno.Time
import me.anno.io.saveable.Saveable
import me.anno.maths.Maths.clamp

class CameraState : Saveable() {

    var currentCamera: Camera? = null
    var previousCamera: Camera? = null
    var cameraBlendingTime = 0f
    var cameraBlendingProgress = 0f

    fun update() {
        cameraBlendingProgress += Time.deltaTime.toFloat() / clamp(cameraBlendingTime, 1e-6f, 1e3f)
    }

    fun copyInto(dst: CameraState) {
        dst.currentCamera = currentCamera
        dst.previousCamera = previousCamera
        dst.cameraBlendingTime = cameraBlendingTime
        dst.cameraBlendingProgress = cameraBlendingProgress
    }
}