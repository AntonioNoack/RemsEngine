package me.anno.engine.ui.control

import org.joml.Matrix4x3
import org.joml.Vector3f

interface DCMovable {
    fun move(
        self: DraggingControls, camTransform: Matrix4x3, offset: Vector3f, dir: Vector3f,
        rotationAngle: Float, dx: Float, dy: Float
    )

    fun getGlobalTransform(dst: Matrix4x3): Matrix4x3
}