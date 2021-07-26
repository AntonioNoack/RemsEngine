package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.gpu.GFX
import me.anno.utils.Maths.clamp

class CameraState: Component() {

    var currentCamera: CameraComponent? = null
    var previousCamera: CameraComponent? = null
    var cameraBlendingTime = 0.0
    var cameraBlendingProgress = 0.0

    override fun onUpdate() {
        cameraBlendingProgress += GFX.deltaTime / clamp(cameraBlendingTime, 1e-6, 1e3)
    }

    // todo draw: first cam 1, then cam 2, and then blend them together


    override val className get() = "CameraStateComponent"

    companion object {



    }

}