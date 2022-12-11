package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera

open class ThirdPersonController : CameraController() {

    // this is like orbit, isn't it?
    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        lastWarning = "hasn't been implemented yet"
    }

    override fun clone(): ThirdPersonController {
        val clone = ThirdPersonController()
        copy(clone)
        return clone
    }

    override val className get() = "ThirdPersonControls"

}