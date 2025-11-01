package me.anno.ecs.components.camera.control

open class FirstPersonController : OrbitControls() {
    init {
        radius = -0.1
        minRadius = 0.0
        maxRadius = 0.0
        mouseWheelSpeed = 0.0
    }
}