package me.anno.tests.gfx

import me.anno.engine.ui.render.Frustum
import me.anno.image.ImageWriter.writeImageInt
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Test

class CullTest {
    @Test
    fun simpleTest() {
        val frustum = Frustum()
        val res = 100

        frustum.definePerspective(
            0.001, 100.0, (90.0).toRadians(), res, res, 1.0,
            Vector3d(0.0, 0.0, -1.0), Quaterniond()
        )

        val aabb1 = AABBd()
        aabb1.union(0.0, 0.0, 0.0)

        val aabb2 = AABBd()
        aabb2.union(0.0, 0.0, 0.9)

        assertFalse(aabb1 in frustum)
        assertFalse(aabb2 in frustum)

        frustum.definePerspective(
            0.001, 100.0, (90.0).toRadians(), res, res, 1.0,
            Vector3d(0.0, 0.0, 1.0), Quaterniond()
        )

        assertTrue(aabb1 in frustum)
        assertTrue(aabb2 in frustum)
    }
}

fun main() {
    val frustum = Frustum()
    imageTest(frustum)
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
