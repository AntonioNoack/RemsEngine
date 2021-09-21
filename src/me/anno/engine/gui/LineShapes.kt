package me.anno.engine.gui

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.gpu.buffer.LineBuffer.putRelativeLine
import me.anno.gpu.pipeline.PipelineStage.Companion.getDrawMatrix
import me.anno.utils.types.Vectors.setAxis
import org.joml.Matrix4x3d
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin

object LineShapes {

    private val guiPositionsTmp = Array(16) { Vector3d() }

    val zToX = Matrix4x3d()
        .rotateY(Math.PI / 2)
    val zToY = Matrix4x3d()
        .rotateX(Math.PI / 2)

    fun drawCone(
        entity: Entity?,
        widthX: Double,
        widthY: Double = widthX,
        tipZ: Double = -1.0,
        baseZ: Double = 0.0,
        imm: Matrix4x3d? = null,
        color: Int = Collider.guiLineColor
    ) {

        // from +z to -z

        val positions = guiPositionsTmp
        positions[0].set(+widthX, +widthY, tipZ)
        positions[1].set(+widthX, -widthY, tipZ)
        positions[2].set(-widthX, -widthY, tipZ)
        positions[3].set(-widthX, +widthY, tipZ)
        positions[4].set(0.0, 0.0, baseZ)

        val transform = getDrawMatrix(entity)
        if (transform != null || imm != null) {
            for (i in 0 until 5) {
                val position = positions[i]
                imm?.transformPosition(position)
                transform?.transformPosition(position)
            }
        }

        for (i in 0 until 4) {
            // from center to frame
            putRelativeLine(positions[i], positions[4], color)
            // frame
            putRelativeLine(positions[i], positions[(i + 1) and 3], color)
        }

    }

    fun drawArrowZ(entity: Entity?, z0: Double, z1: Double, color: Int = Collider.guiLineColor) {

        // from z0 to z1; used for lights

        val x = (z1 - z0) * 0.15
        val z = 2.0 * x + z1

        val positions = guiPositionsTmp
        positions[0].set(0.0, 0.0, z0)
        positions[1].set(0.0, 0.0, z1)
        positions[2].set(+x, +x, z)
        positions[3].set(+x, -x, z)
        positions[4].set(-x, -x, z)
        positions[5].set(-x, +x, z)

        val transform = getDrawMatrix(entity)
        if (transform != null) {
            for (i in 0 until 6) {
                val position = positions[i]
                transform.transformPosition(position)
            }
        }

        // body
        putRelativeLine(positions[0], positions[1], color)

        // arrow tip
        for (i in 2 until 6) {
            putRelativeLine(positions[1], positions[i], color)
        }
    }

    fun drawCross(entity: Entity?, halfExtends: Vector3d? = null) {
        drawCross(entity, Collider.guiLineColor, halfExtends)
    }

    fun drawCross(entity: Entity?, color: Int = Collider.guiLineColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        positions[0].set(-1.0, 0.0, 0.0)
        positions[1].set(+1.0, 0.0, 0.0)
        positions[2].set(0.0, -1.0, 0.0)
        positions[3].set(0.0, +1.0, 0.0)
        positions[4].set(0.0, 0.0, -1.0)
        positions[5].set(0.0, 0.0, +1.0)

        for (i in 0 until 6) {
            val position = positions[i]
            if (halfExtends != null) position.mul(halfExtends)
            transform?.transformPosition(position)
        }

        putRelativeLine(positions[0], positions[1], color)
        putRelativeLine(positions[2], positions[3], color)
        putRelativeLine(positions[4], positions[5], color)

    }


    fun drawBox(entity: Entity?, halfExtends: Vector3d?) {
        drawBox(entity, Collider.guiLineColor, halfExtends)
    }

    fun drawBox(entity: Entity?, color: Int = Collider.guiLineColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        for (i in 0 until 8) {
            val position = positions[i]
            position.set(
                if ((i.and(1) != 0)) -1.0 else +1.0,
                if ((i.and(2) != 0)) -1.0 else +1.0,
                if ((i.and(4) != 0)) -1.0 else +1.0
            )
            if (halfExtends != null) position.mul(halfExtends)
            transform?.transformPosition(position)
        }

        for (base in 0 until 7) {
            for (bitIndex in 0 until 3) {
                val bit = 1 shl bitIndex
                if (base.and(bit) == 0) {
                    val other = base or bit
                    // line from base to other
                    putRelativeLine(positions[base], positions[other], color)
                }
            }
        }
    }

    fun drawXYPlane(entity: Entity?, z: Double, color: Int = Collider.guiLineColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        for (i in 0 until 4) {
            val position = positions[i]
            position.set(
                if ((i.and(1) != 0)) -1.0 else +1.0,
                if ((i.and(2) != 0)) -1.0 else +1.0,
                z
            )
            if (halfExtends != null) position.mul(halfExtends)
            transform?.transformPosition(position)
        }

        putRelativeLine(positions[0], positions[1], color)
        putRelativeLine(positions[1], positions[3], color)
        putRelativeLine(positions[3], positions[2], color)
        putRelativeLine(positions[2], positions[0], color)
    }

    fun drawPoint(entity: Entity?, center: Vector3d, sideLength: Double, color: Int = Collider.guiLineColor) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        positions[0].set(-1.0, 0.0, 0.0)
        positions[1].set(+1.0, 0.0, 0.0)
        positions[2].set(0.0, -1.0, 0.0)
        positions[3].set(0.0, +1.0, 0.0)
        positions[4].set(0.0, 0.0, -1.0)
        positions[5].set(0.0, 0.0, +1.0)

        for (i in 0 until 6) {
            val posI = positions[i]
            posI.mul(sideLength)
            posI.add(center)
            transform?.transformPosition(posI)
        }

        putRelativeLine(positions[0], positions[1], color)
        putRelativeLine(positions[2], positions[3], color)
        putRelativeLine(positions[4], positions[5], color)

    }

    fun drawLine(
        entity: Entity?,
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int = Collider.guiLineColor
    ) {
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        positions[0].set(x0, y0, z0)
        positions[1].set(x1, y1, z1)
        if (transform != null) {
            for (i in 0 until 2) {
                transform.transformPosition(positions[i])
            }
        }
        putRelativeLine(positions[0], positions[1], color)
    }

    fun drawLine(
        entity: Entity?,
        p0: javax.vecmath.Vector3d,
        p1: javax.vecmath.Vector3d,
        color: Int = Collider.guiLineColor
    ) {
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        positions[0].set(p0.x, p0.y, p0.z)
        positions[1].set(p1.x, p1.y, p1.z)
        if (transform != null) {
            for (i in 0 until 2) {
                transform.transformPosition(positions[i])
            }
        }
        putRelativeLine(positions[0], positions[1], color)
    }

    fun drawSphere(
        entity: Entity?,
        radius: Double,
        offset: Vector3d? = null,
        color: Int = Collider.guiLineColor
    ) {
        drawCircle(entity, radius, 0, 1, 0.0, offset, color)
        drawCircle(entity, radius, 1, 2, 0.0, offset, color)
        drawCircle(entity, radius, 2, 0, 0.0, offset, color)
    }

    fun drawCircle(
        entity: Entity?,
        radius: Double,
        cosAxis: Int,
        sinAxis: Int,
        otherAxis: Double,
        offset: Vector3d? = null,
        color: Int = Collider.guiLineColor
    ) {
        val segments = 11
        val transform = getDrawMatrix(entity)
        val positions = guiPositionsTmp
        for (i in 0 until segments) {
            val angle = i * 6.2830 / segments
            val position = positions[i]
            position.set(otherAxis)
            position.setAxis(cosAxis, cos(angle) * radius)
            position.setAxis(sinAxis, sin(angle) * radius)
            if (offset != null) position.add(offset)
            transform?.transformPosition(position)
        }
        for (i in 0 until segments) {
            putRelativeLine(positions[i], positions[(i + 1) % segments], color)
        }
    }

}
