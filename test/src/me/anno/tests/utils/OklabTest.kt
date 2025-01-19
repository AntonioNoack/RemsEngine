package me.anno.tests.utils

import me.anno.ui.editor.color.spaces.Oklab
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OklabTest {
    @Test
    fun testInvertibility() {
        val n = 10000
        val rgb = Vector3f()
        val lab = Vector3f()
        val dst = Vector3f()
        val rnd = Random(1234L)
        for (i in 0 until n) {
            rgb.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            Oklab.fromRGB(rgb, lab)
            Oklab.toRGB(lab, dst)
            assertEquals(0f, rgb.distanceSquared(dst), 1e-7f)
        }
    }
}