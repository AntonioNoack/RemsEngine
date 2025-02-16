package me.anno.engine.ui.control

import org.joml.Matrix4x3m
import org.joml.Vector3f

interface DCMovable {
    fun move(
        self: DraggingControls, camTransform: Matrix4x3m, offset: Vector3f, dir: Vector3f,
        rotationAngle: Float, dx: Float, dy: Float
    )

    fun getGlobalTransform(dst: Matrix4x3m): Matrix4x3m
}