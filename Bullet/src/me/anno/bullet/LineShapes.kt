package me.anno.bullet

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.engine.ui.LineShapes
import me.anno.gpu.buffer.LineBuffer
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import javax.vecmath.Vector3d

fun drawLine(
    entity: Entity?,
    p0: Vector3d,
    p1: Vector3d,
    color: Int = Collider.guiLineColor
) {
    val transform = LineShapes.getDrawMatrix(entity)
    val positions = LineShapes.tmpVec3d
    positions[0].set(p0.x, p0.y, p0.z)
    positions[1].set(p1.x, p1.y, p1.z)
    if (transform != null) {
        for (i in 0 until 2) {
            transform.transformPosition(positions[i])
        }
    }
    LineBuffer.putRelativeLine(positions[0], positions[1], color)
}

fun putRelativeLine(
    v0: Vector3d, v1: Vector3d,
    cam: org.joml.Vector3d,
    worldScale: Double,
    r: Double, g: Double, b: Double, a: Double = 1.0
) {
    LineBuffer.putRelativeLine(
        v0.x, v0.y, v0.z,
        v1.x, v1.y, v1.z,
        cam, worldScale,
        LineBuffer.vToByte(r),
        LineBuffer.vToByte(g),
        LineBuffer.vToByte(b),
        LineBuffer.vToByte(a)
    )
}

@Suppress("unused")
fun putRelativeLine(
    v0: Vector3d, v1: Vector3d,
    cam: org.joml.Vector3d,
    worldScale: Double,
    color: Int
) {
    putRelativeLine(
        v0, v1,
        cam, worldScale,
        color.r().toByte(),
        color.g().toByte(),
        color.b().toByte(),
        color.a().toByte()
    )
}

fun putRelativeLine(
    v0: Vector3d, v1: Vector3d,
    cam: org.joml.Vector3d,
    worldScale: Double,
    r: Byte, g: Byte, b: Byte, a: Byte = -1
) {
    LineBuffer.putRelativeLine(
        v0.x, v0.y, v0.z,
        v1.x, v1.y, v1.z,
        cam, worldScale,
        r, g, b, a
    )
}