package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import org.joml.Quaternionf
import kotlin.math.abs
import kotlin.math.sign

open class OrbitControls : CameraController() {

    var radius = 10.0
    var mouseWheelSpeed = 0.1
    var useGlobalSpace = false

    var maxRadius = 1e+3
    var minRadius = 1e-3

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        if (radius == 0.0) return false
        val newRadius = clamp(pow(2.0, -dy * mouseWheelSpeed) * abs(radius), minRadius, maxRadius)
        movementSpeed *= newRadius / abs(radius)
        radius = newRadius * sign(radius)
        return true
    }

    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        if (baseTransform === camTransform) {
            lastWarning = "Base and cam transform are the same"
            return
        }
        if (useGlobalSpace) {
            baseTransform.globalPosition = baseTransform.globalPosition.add(position)
            position.set(0.0)
            baseTransform.globalRotation = Quaternionf()
                .rotateY(rotation.y)
                .rotateX(rotation.x)
        } else {
            baseTransform.localPosition = baseTransform.localPosition.add(position)
            position.set(0.0)
            baseTransform.localRotation = baseTransform.localRotation.identity()
                .rotateY(rotation.y)
                .rotateX(rotation.x)
        }
        camTransform.localPosition = camTransform.localPosition
            .set(0.0, 0.0, +radius)
        camTransform.localRotation = camTransform.localRotation.identity()
            .rotateZ(rotation.z) // correct place? probably :)
    }
}