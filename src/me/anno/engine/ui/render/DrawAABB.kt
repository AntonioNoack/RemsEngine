package me.anno.engine.ui.render

import me.anno.gpu.buffer.LineBuffer.addLine
import me.anno.gpu.buffer.LineBuffer.ensureSize
import me.anno.gpu.buffer.LineBuffer.lineSize
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.isEmpty
import org.joml.AABBd
import org.joml.Vector3f
import javax.vecmath.Vector3d

object DrawAABB {

    fun transform(a: Vector3d, worldScale: Double, dst: Vector3f = Vector3f()): Vector3f {
        val pos = RenderView.camPosition
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

    fun drawAABB(aabb: AABBd, worldScale: Double, color: Int) {

        if (aabb.isEmpty()) return

        val pos = RenderView.camPosition

        val x0 = ((aabb.minX - pos.x) * worldScale).toFloat()
        val y0 = ((aabb.minY - pos.y) * worldScale).toFloat()
        val z0 = ((aabb.minZ - pos.z) * worldScale).toFloat()

        val x1 = ((aabb.maxX - pos.x) * worldScale).toFloat()
        val y1 = ((aabb.maxY - pos.y) * worldScale).toFloat()
        val z1 = ((aabb.maxZ - pos.z) * worldScale).toFloat()

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

}