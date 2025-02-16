package me.anno.engine.ui.render

import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.gpu.buffer.LineBuffer.bytesPerLine
import me.anno.gpu.buffer.LineBuffer.ensureSize
import me.anno.utils.pooling.JomlPools
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3d

object DrawAABB {

    fun drawAABB(aabb: AABBd, color: Int) {
        val pos = RenderState.cameraPosition
        drawAABB(aabb, color, pos)
    }

    fun drawAABB(aabb: AABBd, color: Int, pos: Vector3d) {

        if (aabb.isEmpty()) return

        val x0 = aabb.minX - pos.x
        val y0 = aabb.minY - pos.y
        val z0 = aabb.minZ - pos.z

        val x1 = aabb.maxX - pos.x
        val y1 = aabb.maxY - pos.y
        val z1 = aabb.maxZ - pos.z

        drawAABB(x0, y0, z0, x1, y1, z1, color)
    }

    fun drawAABB(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Int
    ) {
        drawAABB(
            x0.toFloat(), y0.toFloat(), z0.toFloat(),
            x1.toFloat(), y1.toFloat(), z1.toFloat(), color
        )
    }

    fun drawAABB(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        color: Int
    ) {
        ensureSize(bytesPerLine * 12)

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

    fun drawAABB(transform: Matrix4x3?, aabb: AABBd, color: Int) {

        if (aabb.isEmpty()) return
        if (transform == null) return drawAABB(aabb, color)

        val min = transform.transformPosition(aabb.getMin(JomlPools.vec3d.create()))
        val max = transform.transformPosition(aabb.getMax(JomlPools.vec3d.create()))

        val pos = RenderState.cameraPosition
        min.sub(pos)
        max.sub(pos)

        drawAABB(min.x, min.y, min.z, max.x, max.y, max.z, color)

        JomlPools.vec3d.sub(2)
    }
}