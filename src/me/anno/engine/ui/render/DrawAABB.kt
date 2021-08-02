package me.anno.engine.ui.render

import me.anno.ecs.Entity
import me.anno.gpu.buffer.LineBuffer
import me.anno.utils.types.AABBs.isEmpty
import org.joml.AABBd
import org.joml.Vector3f
import javax.vecmath.Vector3d

object DrawAABB {

    fun drawAABB(entity: Entity, worldScale: Double) {
        drawAABB(entity.aabb, worldScale, 1.0, 0.0, 0.0)
    }

    fun transform(a: Vector3d, worldScale: Double): Vector3f {
        val camPosition = RenderView.camPosition
        return Vector3f(
            ((a.x - camPosition.x) * worldScale).toFloat(),
            ((a.y - camPosition.y) * worldScale).toFloat(),
            ((a.z - camPosition.z) * worldScale).toFloat()
        )
    }

    fun drawLine(a: Vector3d, b: Vector3d, worldScale: Double, cr: Double, cg: Double, cb: Double) {
        LineBuffer.addLine(
            transform(a, worldScale),
            transform(b, worldScale),
            cr, cg, cb
        )
    }

    fun drawAABB(aabb: AABBd, worldScale: Double, cr: Double, cg: Double, cb: Double) {

        if (aabb.isEmpty()) return

        // to do draw 3 main axis like bullet?

        val camPosition = RenderView.camPosition

        val x = ((aabb.minX - camPosition.x) * worldScale).toFloat()
        val y = ((aabb.minY - camPosition.y) * worldScale).toFloat()
        val z = ((aabb.minZ - camPosition.z) * worldScale).toFloat()

        val dx = ((aabb.maxX - aabb.minX) * worldScale).toFloat()
        val dy = ((aabb.maxY - aabb.minY) * worldScale).toFloat()
        val dz = ((aabb.maxZ - aabb.minZ) * worldScale).toFloat()

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dx
            val b = ((i shr 1) and 1).toFloat() * dy
            LineBuffer.addLine(
                Vector3f(x + a, y + b, z + 0f),
                Vector3f(x + a, y + b, z + dz),
                cr, cg, cb
            )
        }

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dx
            val b = ((i shr 1) and 1).toFloat() * dz
            LineBuffer.addLine(
                Vector3f(x + a, y + 0f, z + b),
                Vector3f(x + a, y + dy, z + b),
                cr, cg, cb
            )
        }

        for (i in 0 until 4) {
            val a = ((i shr 0) and 1).toFloat() * dy
            val b = ((i shr 1) and 1).toFloat() * dz
            LineBuffer.addLine(
                Vector3f(x + 0f, y + a, z + b),
                Vector3f(x + dx, y + a, z + b),
                cr, cg, cb
            )
        }

    }

}