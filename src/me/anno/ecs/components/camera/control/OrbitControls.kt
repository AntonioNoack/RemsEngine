package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow

open class OrbitControls : CameraController() {

    var radius = 10f
    var mouseWheelSpeed = 0.1f
    var useGlobalSpace = false

    var maxRadius = 1e+3f
    var minRadius = 1e-3f

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        val newRadius = clamp(pow(2f, -dy * mouseWheelSpeed) * radius, minRadius, maxRadius)
        movementSpeed *= newRadius / radius
        radius = newRadius
        return true
    }

    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        if (baseTransform === camTransform) {
            lastWarning = "Base and cam transform are the same"
            return
        }
        if (useGlobalSpace) {
            baseTransform.globalPosition = baseTransform.globalPosition.add(position)
            position.set(0f)
            baseTransform.globalRotation = baseTransform.globalRotation.identity()
                .rotateY(rotation.y.toDouble())
                .rotateX(rotation.x.toDouble())
        } else {
            baseTransform.localPosition = baseTransform.localPosition.add(position)
            position.set(0f)
            baseTransform.localRotation = baseTransform.localRotation.identity()
                .rotateY(rotation.y.toDouble())
                .rotateX(rotation.x.toDouble())
        }
        camTransform.localPosition = camTransform.localPosition
            .set(0.0, 0.0, +radius.toDouble())
        camTransform.localRotation = camTransform.localRotation.identity()
            .rotateZ(rotation.z.toDouble()) // correct place? probably :)
    }
}