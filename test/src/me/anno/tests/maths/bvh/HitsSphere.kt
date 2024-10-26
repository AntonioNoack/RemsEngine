package me.anno.tests.maths.bvh

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3f
import org.junit.jupiter.api.Test

object HitsSphere {

    fun hitsSphere(pos: Vector3f, dir: Vector3f): Boolean {
        val t = -pos.dot(dir)
        val closest = pos + dir * t
        return t >= 0f && closest.length() <= 1f
    }

    @Test
    fun testRandomRayGenerator() {
        val gen = RandomRayGenerator()
        var ctr = 0
        for (i in 0 until 1000) {
            ctr += gen.next().toInt()
        }
        val expected = (1000 * 0.5).toInt()
        assertTrue(ctr in expected - 50..expected + 50) {
            "Expected $expected, got $ctr"
        }
    }

    @Test
    fun testHitsSphere() {
        val gen = RandomRayGenerator()
        for (i in 0 until 1000) {
            val shouldHitSphere = gen.next()
            assertEquals(shouldHitSphere, hitsSphere(gen.pos, gen.dir))
        }
    }
}