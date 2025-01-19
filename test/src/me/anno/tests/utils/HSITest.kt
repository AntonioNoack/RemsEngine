package me.anno.tests.utils

import me.anno.ui.editor.color.spaces.HSI
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class HSITest {
    @Test
    fun testHSIInvertibility() {
        val space = HSI
        val random = Random(1234)
        val srcRGB = Vector3f()
        val tmp0 = Vector3f()
        val tmp1 = Vector3f()
        for (i in 0 until 1000) {
            srcRGB.set(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
            )
            val internal = space.fromRGB(srcRGB, tmp0)
            val dstRGB = space.toRGB(internal, tmp1)
            val error = srcRGB.distance(dstRGB)
            assertTrue(error < 1e-3f)
        }
    }
}