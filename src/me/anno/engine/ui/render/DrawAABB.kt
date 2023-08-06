package me.anno.engine.ui.render

import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.gpu.buffer.LineBuffer.ensureSize
import me.anno.gpu.buffer.LineBuffer.lineSize
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f
import javax.vecmath.Vector3d

object DrawAABB {

    fun transform(a: Vector3d, worldScale: Double, dst: Vector3f = Vector3f()): Vector3f {
        val pos = RenderState.cameraPosition
        return dst.set(
            ((a.x - pos.x) * worldScale).toFloat(),
            ((a.y - pos.y) * worldScale).toFloat(),
            ((a.z - pos.z) * worldScale).toFloat()
        )
    }

    fun drawLine(a: Vector3d, b: Vector3d, worldScale: Double, color: Int) {
        val t0 = JomlPools.vec3f.create()
        val t1 = JomlPools.vec3f.create()
        addLine(
            transform(a, worldScale, t0),
            transform(b, worldScale, t1),
            color
        )
        JomlPools.vec3f.sub(2)
    }

    fun drawAABB(aabb: AABBd, color: Int) {

        if (aabb.isEmpty()) return

        val pos = RenderState.cameraPosition
        val worldScale = RenderState.worldScale

        val x0 = ((aabb.minX - pos.x) * worldScale)
        val y0 = ((aabb.minY - pos.y) * worldScale)
        val z0 = ((aabb.minZ - pos.z) * worldScale)

        val x1 = ((aabb.maxX - pos.x) * worldScale)
        val y1 = ((aabb.maxY - pos.y) * worldScale)
        val z1 = ((aabb.maxZ - pos.z) * worldScale)

        drawAABB(x0, y0, z0, x1, y1, z1, color)

    }

    fun drawAABB(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int
    ) {

        ensureSize(lineSize * 12)

        // loops simplified, so we potentially gain a little performance

        // dx
        addLine(x0, y0, z0, x1, y0, z0, color)
        addLine(x0, y1, z0, x1, y1, z0, color)
        addLine(x0, y0, z1, x1, y0, z1, color)
        addLine(x0, y1, z1, x1, y1, z1, color)

        // dy
        addLine(x0, y0, z0, x0, y1, z0, color)
        addLine(x1, y0, z0, x1, y1, z0, color)
        addLine(x0, y0, z1, x0, y1, z1, color)
        addLine(x1, y0, z1, x1, y1, z1, color)

        // dz
        addLine(x0, y0, z0, x0, y0, z1, color)
        addLine(x1, y0, z0, x1, y0, z1, color)
        addLine(x0, y1, z0, x0, y1, z1, color)
        addLine(x1, y1, z0, x1, y1, z1, color)

    }

    fun drawAABB(transform: Matrix4x3d?, aabb: AABBd, worldScale: Double, color: Int) {

        if (aabb.isEmpty()) return
        if (transform == null) return drawAABB(aabb, color)

        val pos = RenderState.cameraPosition
        val min = transform.transformPosition(aabb.getMin(JomlPools.vec3d.create()))
        val max = transform.transformPosition(aabb.getMax(JomlPools.vec3d.create()))

        min.sub(pos).mul(worldScale)
        max.sub(pos).mul(worldScale)

        drawAABB(min.x, min.y, min.z, max.x, max.y, max.z, color)

        JomlPools.vec3d.sub(2)

    }

}