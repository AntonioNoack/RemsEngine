package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.buffer.LineBuffer.putRelativeLine
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.mix
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4x3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object LineShapes {

    private val tmpVec3f = createArrayList(16) { Vector3f() }
    val tmpVec3d = createArrayList(16) { Vector3d() }
    private val defaultColor = 0x77ffff or black

    fun getDrawMatrix(entity: Entity?): Matrix4x3d? {
        val entity1 = entity ?: return null
        entity1.validateTransform()
        return entity1.transform.getDrawMatrix()
    }

    val zToX: Matrix4x3d = Matrix4x3d()
        .rotateY(PI / 2)
    val zToY: Matrix4x3d = Matrix4x3d()
        .rotateX(PI / 2)

    fun drawCone(
        entity: Entity?,
        widthX: Double,
        widthY: Double = widthX,
        tipZ: Double = -1.0,
        baseZ: Double = 0.0,
        imm: Matrix4x3d? = null,
        color: Int = defaultColor
    ) {

        // from +z to -z

        val positions = tmpVec3d
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

    fun drawArrowZ(from: Vector3d, to: Vector3d, color: Int = defaultColor) {

        val positions = tmpVec3d
        positions[0].set(from)
        positions[1].set(to)

        val dirZ = Vector3d(to).sub(from)
        val length = dirZ.length()

        if (length == 0.0 || !length.isFinite()) return

        val dirX = JomlPools.vec3d.create()
        val dirY = JomlPools.vec3d.create()
        dirZ.findSystem(dirX, dirY)

        val to2 = from.mix(to, 0.7, JomlPools.vec3d.create())
        dirX.normalize(length * 0.15)
        dirY.normalize(length * 0.15)

        positions[2].set(to2).add(dirX).add(dirY)
        positions[3].set(to2).add(dirX).sub(dirY)
        positions[4].set(to2).sub(dirX).sub(dirY)
        positions[5].set(to2).sub(dirX).add(dirY)

        // body
        putRelativeLine(positions[0], positions[1], color)

        // arrow tip
        for (i in 2 until 6) {
            putRelativeLine(positions[1], positions[i], color)
        }

        JomlPools.vec3d.sub(3)
    }

    fun drawArrowZ(entity: Entity?, z0: Double, z1: Double, color: Int = defaultColor) {

        // from z0 to z1; used for lights

        val x = (z1 - z0) * 0.15
        val z = z1 - 2.0 * x // correct for directional lights

        val positions = tmpVec3d
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
        drawCross(entity, defaultColor, halfExtends)
    }

    fun drawCross(entity: Entity?, color: Int = defaultColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
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
        drawBox(entity, defaultColor, halfExtends)
    }

    fun drawBox(entity: Entity?, color: Int = defaultColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        val hx = halfExtends?.x ?: 1.0
        val hy = halfExtends?.y ?: 1.0
        val hz = halfExtends?.z ?: 1.0
        for (i in 0 until 8) {
            val position = positions[i].set(
                if ((i.and(1) != 0)) -hx else +hx,
                if ((i.and(2) != 0)) -hy else +hy,
                if ((i.and(4) != 0)) -hz else +hz
            )
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

    fun drawXYPlane(entity: Entity?, z: Double, color: Int = defaultColor, halfExtends: Vector3d? = null) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
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

    fun drawPoint(entity: Entity?, center: Vector3d, sideLength: Double, color: Int = defaultColor) =
        drawPoint(entity, center.x, center.y, center.z, sideLength, color)

    fun drawPoint(
        entity: Entity?,
        cx: Double,
        cy: Double,
        cz: Double,
        sideLength: Double,
        color: Int = defaultColor
    ) {
        // iterate over all lines:
        // all bits that can flip
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        positions[0].set(-1.0, 0.0, 0.0)
        positions[1].set(+1.0, 0.0, 0.0)
        positions[2].set(0.0, -1.0, 0.0)
        positions[3].set(0.0, +1.0, 0.0)
        positions[4].set(0.0, 0.0, -1.0)
        positions[5].set(0.0, 0.0, +1.0)

        for (i in 0 until 6) {
            val posI = positions[i]
            posI.mul(sideLength)
            posI.add(cx, cy, cz)
            transform?.transformPosition(posI)
        }

        putRelativeLine(positions[0], positions[1], color)
        putRelativeLine(positions[2], positions[3], color)
        putRelativeLine(positions[4], positions[5], color)
    }

    fun drawPoint(
        entity: Entity?, cx: Float, cy: Float, cz: Float, sideLength: Double, color: Int = defaultColor
    ) = drawPoint(entity, cx.toDouble(), cy.toDouble(), cz.toDouble(), sideLength, color)

    fun drawLine(
        entity: Entity?,
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int = defaultColor
    ) {
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        val p0 = positions[0]
        val p1 = positions[1]
        p0.set(x0, y0, z0)
        p1.set(x1, y1, z1)
        if (transform != null) {
            transform.transformPosition(p0)
            transform.transformPosition(p1)
        }
        val pi = positions[2]
        val pj = positions[3]
        pi.set(p0)
        // split the line in pieces for less flickering
        // alternatively, we could apply the transform with doubles, and clip in view-space on the cpu side
        // the most important thing with splitting is that the number is even, so the center is on the true center
        val pieces = 16
        for (i in 0 until pieces) {
            p0.mix(p1, (i + 1.0) / pieces, pj)
            putRelativeLine(pi, pj, color)
            pi.set(pj)
        }
    }

    fun drawLine(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        color: Int = defaultColor
    ) {
        val positions = tmpVec3f
        val p0 = positions[0]
        val p1 = positions[1]
        p0.set(x0, y0, z0)
        p1.set(x1, y1, z1)
        val pi = positions[2]
        val pj = positions[3]
        pi.set(p0)
        // split the line in pieces for less flickering
        // alternatively, we could apply the transform with doubles, and clip in view-space on the cpu side
        // the most important thing with splitting is that the number is even, so the center is on the true center
        val pieces = 16
        for (i in 0 until pieces) {
            p0.mix(p1, (i + 1f) / pieces, pj)
            putRelativeLine(pi, pj, color)
            pi.set(pj)
        }
    }

    fun drawLine(
        entity: Entity?,
        p0: Vector3f,
        p1: Vector3f,
        color: Int = defaultColor
    ) {
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        positions[0].set(p0)
        positions[1].set(p1)
        if (transform != null) {
            for (i in 0 until 2) {
                transform.transformPosition(positions[i])
            }
        }
        putRelativeLine(positions[0], positions[1], color)
    }

    fun drawLine(
        entity: Entity?,
        p0: Vector3d,
        p1: Vector3d,
        color: Int = defaultColor
    ) {
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        positions[0].set(p0)
        positions[1].set(p1)
        if (transform != null) {
            for (i in 0 until 2) {
                transform.transformPosition(positions[i])
            }
        }
        putRelativeLine(positions[0], positions[1], color)
    }

    fun drawRect(
        entity: Entity?, p0: Vector3f, p1: Vector3f, p2: Vector3f, p3: Vector3f,
        color: Int = defaultColor
    ) {
        val positions = tmpVec3d
        positions[0].set(p0)
        positions[1].set(p1)
        positions[2].set(p2)
        positions[3].set(p3)
        drawRect(entity, color)
    }

    fun drawRect(
        entity: Entity?, p0: Vector3d, p1: Vector3d, p2: Vector3d, p3: Vector3d,
        color: Int = defaultColor
    ) {
        val positions = tmpVec3d
        positions[0].set(p0)
        positions[1].set(p1)
        positions[2].set(p2)
        positions[3].set(p3)
        drawRect(entity, color)
    }

    private fun drawRect(entity: Entity?, color: Int) {
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        if (transform != null) {
            for (i in 0 until 4) {
                transform.transformPosition(positions[i])
            }
        }
        for (i in 0 until 4) {
            putRelativeLine(positions[i], positions[(i + 1) and 3], color)
        }
    }

    fun drawSphere(
        entity: Entity?,
        radius: Double,
        offset: Vector3d? = null,
        color: Int = defaultColor
    ) {
        drawCircle(entity, radius, 1, 2, 0.0, offset, color)
        drawCircle(entity, radius, 2, 0, 0.0, offset, color)
        drawCircle(entity, radius, 0, 1, 0.0, offset, color)
    }

    fun drawPartialSphere(
        entity: Entity?,
        radius: Double,
        offset: Vector3d?,
        ax: Double, dax: Double,
        ay: Double, day: Double,
        az: Double, daz: Double,
        color: Int = defaultColor
    ) {
        drawCircle(entity, radius, 1, 2, 0.0, offset, color, ax, dax)
        drawCircle(entity, radius, 2, 0, 0.0, offset, color, ay, day)
        drawCircle(entity, radius, 0, 1, 0.0, offset, color, az, daz)
    }

    fun drawCircle(
        entity: Entity?,
        radius: Double,
        cosAxis: Int,
        sinAxis: Int,
        otherAxis: Double,
        offset: Vector3d? = null,
        color: Int = defaultColor,
        angle0: Double = 0.0,
        deltaAngle: Double = TAU,
    ) {
        val segments = 16
        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        val fullCircle = deltaAngle >= TAU
        var segments1 = segments
        if (!fullCircle) segments1--
        val angleMultiplier = deltaAngle / segments1
        for (i in 0 until segments) {
            val angle = angle0 + i * angleMultiplier
            val position = positions[i]
            position.set(otherAxis)
            position[cosAxis] = cos(angle) * radius
            position[sinAxis] = sin(angle) * radius
            if (offset != null) position.add(offset)
            transform?.transformPosition(position)
        }
        var lastPosI = 0
        if (fullCircle) lastPosI = segments - 1
        var lastPos = positions[lastPosI]
        val i0 = fullCircle.toInt(0, 1)
        for (i in i0 until segments) {
            val pos = positions[i]
            putRelativeLine(lastPos, pos, color)
            lastPos = pos
        }
    }

    fun drawCone(
        entity: Entity?,
        start: Vector3f,
        end: Vector3f,
        radiusAtOrigin: Float,
        radiusPerUnit: Float,
        centralColor: Int = defaultColor,
        outerRayColor: Int = defaultColor,
        circleColor: Int = defaultColor
    ) {

        // only correct if the scale is uniform

        val transform = getDrawMatrix(entity)
        val positions = tmpVec3d
        val p0 = positions[0].set(start)
        val p1 = positions[1].set(end)

        if (transform != null) {
            transform.transformPosition(p0)
            transform.transformPosition(p1)
        }

        // find the two axes orthogonal to end-start
        val tmp = positions[2]
        val sysX = positions[3]
        val sysY = positions[4]
        tmp.set(p1).sub(p0).findSystem(sysX, sysY)

        // draw central line
        putRelativeLine(p0, p1, centralColor)

        val r0 = radiusAtOrigin.toDouble()
        val r1 = radiusAtOrigin.toDouble() + radiusPerUnit * p1.distance(p0)

        val cp = RenderState.cameraPosition
        val ws = RenderState.worldScale

        // draw borderlines
        val numBorderLines = 8
        for (i in 0 until numBorderLines) {
            val angle = i * TAU / numBorderLines
            val c = cos(angle)
            val s = sin(angle)
            val c0 = c * r0
            val c1 = c * r1
            val s0 = s * r0
            val s1 = s * r1
            putRelativeLine(
                p0.x + c0 * sysX.x + s0 * sysY.x, p0.y + c0 * sysX.y + s0 * sysY.y, p0.z + c0 * sysX.z + s0 * sysY.z,
                p1.x + c1 * sysX.x + s1 * sysY.x, p1.y + c1 * sysX.y + s1 * sysY.y, p1.z + c1 * sysX.z + s1 * sysY.z,
                cp, ws, outerRayColor
            )
        }

        fun drawRing(fraction: Double) {

            p0.mix(p1, fraction, tmp)
            val radius = mix(r0, r1, fraction)
            if (radius == 0.0) return

            val segments = 8
            val di = 5
            for (i in 0 until segments) {
                val angle = i * TAU / segments
                val position = positions[di + i]
                position.set(tmp)
                sysX.mulAdd(cos(angle) * radius, position, position)
                sysY.mulAdd(sin(angle) * radius, position, position)
            }
            var j = segments - 1
            for (i in 0 until segments) {
                putRelativeLine(positions[di + i], positions[di + j], circleColor)
                j = i
            }
        }

        drawRing(0.0)
        drawRing(1.0)
    }
}
