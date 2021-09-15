package me.anno.engine.gui

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.gpu.buffer.LineBuffer.putRelativeLine
import org.joml.Vector3d
import kotlin.math.abs

object LineShapes {

    private val guiPositionsTmp = Array(8) { Vector3d() }

    fun drawCone(entity: Entity?, x: Double, color: Int = Collider.guiLineColor) {

        // from +z to -z

        val positions = guiPositionsTmp
        positions[0].set(+x, +x, -1.0)
        positions[1].set(+x, -x, -1.0)
        positions[2].set(-x, -x, -1.0)
        positions[3].set(-x, +x, -1.0)
        positions[4].set(0.0, 0.0, 0.0)

        val transform = entity?.transform?.drawTransform
        if (transform != null) {
            for (i in 0 until 5) {
                val position = positions[i]
                transform.transformPosition(position)
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

        val transform = entity?.transform?.drawTransform
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
        val transform = entity?.transform?.drawTransform
        val positions = guiPositionsTmp
        positions[0].set(-1.0,0.0,0.0)
        positions[1].set(+1.0,0.0,0.0)
        positions[2].set(0.0,-1.0,0.0)
        positions[3].set(0.0,+1.0,0.0)
        positions[4].set(0.0,0.0,-1.0)
        positions[5].set(0.0,0.0,+1.0)

        for (i in 0 until 6) {
            val position = positions[i]
            if (halfExtends != null) position.mul(halfExtends)
            transform?.transformPosition(position)
        }

        putRelativeLine(positions[0], positions[1], color)
        putRelativeLine(positions[2], positions[3], color)
        putRelativeLine(positions[4], positions[5], color)

    }

    fun drawBox(entity: Entity?, color: Int = Collider.guiLineColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = entity?.transform?.drawTransform
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
        val transform = entity?.transform?.drawTransform
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

}