package me.anno.tests.gfx

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.Frustum
import me.anno.image.ImageWriter.writeImageInt
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBd
import org.joml.Quaterniond
import org.joml.Vector3d

fun main() {
    OfficialExtensions.initForTests()
    val frustum = Frustum()
    imageTest(frustum)
    Engine.requestShutdown()
}

fun imageTest(frustum: Frustum) {

    val w = 256
    val h = 256

    frustum.definePerspective(
        0.001, 100.0, (20.0).toRadians(), w, h, 1.0,
        Vector3d(0.0, 0.0, 0.0),
        Quaterniond().rotateX(0.3)
    )

    for (z in -5 until 0) {
        writeImageInt(w, h, false, "cull$z.png", 1024) { x, y, _ ->
            val aabb = AABBd()
            val xf = (x - w / 2) * 5.0 / w
            val yf = (y - h / 2) * 5.0 / h
            val zf = z * 1.0
            aabb.union(Vector3d(xf, yf, zf))
            val d = 0.5
            aabb.union(Vector3d(xf + d, yf + d, zf + d))
            if (frustum.contains(aabb)) 0 else 0xffffff
        }
    }
}
