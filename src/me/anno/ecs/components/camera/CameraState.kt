package me.anno.ecs.components.camera

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp

class CameraState : Component() {

    var currentCamera: Camera? = null
    var previousCamera: Camera? = null
    var cameraBlendingTime = 0f
    var cameraBlendingProgress = 0f

    override fun onUpdate(): Int {
        cameraBlendingProgress += Engine.deltaTime / clamp(cameraBlendingTime, 1e-6f, 1e3f)
        return 1
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CameraState
        // mmh... may create issues:
        dst.currentCamera = currentCamera
        dst.previousCamera = previousCamera
        dst.cameraBlendingTime = cameraBlendingTime
        dst.cameraBlendingProgress = cameraBlendingProgress
    }

    override val className get() = "CameraStateComponent"

}