package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera
import me.anno.maths.Maths.pow

open class OrbitControls : CameraController() {

    var radius = 10f
    var mouseWheelSpeed = 0.2f

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        val factor = pow(2f, -dy * mouseWheelSpeed)
        radius *= factor
        movementSpeed *= factor
        return true
    }

    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        baseTransform.localPosition = baseTransform.localPosition
            .add(position)
        position.set(0f)
        baseTransform.localRotation = baseTransform.localRotation.identity()
            .rotateY(rotation.y.toDouble())
            .rotateX(rotation.x.toDouble())
        baseTransform.invalidateGlobal()
        camTransform.localPosition = camTransform.localPosition
            .set(0.0, 0.0, +radius.toDouble())
        camTransform.localRotation = camTransform.localRotation.identity()
            .rotateZ(rotation.z.toDouble()) // correct place? probably :)
        camTransform.invalidateGlobal()
    }

    override fun clone(): OrbitControls {
        val clone = OrbitControls()
        copy(clone)
        return clone
    }

    override val className get() = "OrbitControls"

}