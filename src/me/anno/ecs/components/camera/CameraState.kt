package me.anno.ecs.components.camera

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.utils.Maths.clamp

class CameraState : Component() {

    var currentCamera: CameraComponent? = null
    var previousCamera: CameraComponent? = null
    var cameraBlendingTime = 0.0
    var cameraBlendingProgress = 0.0

    override fun onUpdate() {
        cameraBlendingProgress += GFX.deltaTime / clamp(cameraBlendingTime, 1e-6, 1e3)
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
        // todo when these are in the tree being cloned, we need the clones here
        clone.currentCamera = currentCamera
        clone.previousCamera = previousCamera
        clone.cameraBlendingTime = cameraBlendingTime
        clone.cameraBlendingProgress = cameraBlendingProgress
    }

}