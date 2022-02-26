package me.anno.ecs.components.camera

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp

class CameraState : Component() {

    var currentCamera: CameraComponent? = null
    var previousCamera: CameraComponent? = null
    var cameraBlendingTime = 0.0
    var cameraBlendingProgress = 0.0

    override fun onUpdate(): Int {
        cameraBlendingProgress += Engine.deltaTime / clamp(cameraBlendingTime, 1e-6, 1e3)
        return 1
    }

    // todo draw: first cam 1, then cam 2, and then blend them together

    override val className get() = "CameraStateComponent"

    override fun clone(): CameraState {
        val clone = CameraState()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CameraState
        // mmh... may create issues:
        clone.currentCamera = currentCamera
        clone.previousCamera = previousCamera
        clone.cameraBlendingTime = cameraBlendingTime
        clone.cameraBlendingProgress = cameraBlendingProgress
    }

}