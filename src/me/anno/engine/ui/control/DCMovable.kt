package me.anno.engine.ui.control

import org.joml.Matrix4x3d
import org.joml.Vector3d

interface DCMovable {
    fun move(
        self: DraggingControls, camTransform: Matrix4x3d, offset: Vector3d, dir: Vector3d,
        rotationAngle: Double, dx: Float, dy: Float
    )
}