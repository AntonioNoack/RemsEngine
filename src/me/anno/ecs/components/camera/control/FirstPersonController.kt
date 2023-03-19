package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera

open class FirstPersonController : CameraController() {

    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        lastWarning = "hasn't been implemented yet"
    }

    override val className get() = "FirstPersonControls"

}