package me.anno.ecs.components.camera.control

import me.anno.ecs.Transform
import me.anno.ecs.components.camera.Camera
import me.anno.gpu.GFX
import me.anno.maths.Maths.pow
import kotlin.math.cos
import kotlin.math.sin

class OrbitControls : CameraController() {

    var radius = 10.0
    var mouseWheelSpeed = 0.2f

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        radius *= pow(1f + mouseWheelSpeed, dy / GFX.someWindow.height)
        return true
    }

    override fun computeTransform(baseTransform: Transform, camTransform: Transform, camera: Camera) {
        baseTransform.localPosition.add(position)
        position.set(0f)
        baseTransform.localRotation.identity()
            .rotateY(rotation.y.toDouble())
        baseTransform.invalidateLocal()
        camTransform.localPosition.set(sin(rotation.x).toDouble(), 0.0, cos(rotation.x).toDouble())
        camTransform.localRotation.identity()
            .rotateX(rotation.x.toDouble())
            .rotateZ(rotation.z.toDouble()) // correct place? probably :)
        camTransform.invalidateLocal()
    }

    override fun clone(): OrbitControls {
        val clone = OrbitControls()
        copy(clone)
        return clone
    }

    override val className = "OrbitControls"

}