package me.anno.tests.maths.optimization

import me.anno.maths.Maths.PIf
import me.anno.maths.optimization.Bisection.bisect
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos

class BisectionTest {
    @Test
    fun testCosineBisectionF() {
        for (i in 0 until 1000) {
            val angle = PIf * i / 999f
            val cosI = cos(angle)
            val angleI = bisect(0f, PIf) { angleI ->
                // condition is reversed, because our function is descending
                cos(angleI) > cosI
            }
            assertEquals(angle, angleI, 1e-3f)
        }
    }

    @Test
    fun testCosineBisectionD() {
        for (i in 0 until 1000) {
            val angle = PI * i / 999.0
            val cosI = cos(angle)
            val angleI = bisect(0.0, PI) { angleI ->
                // condition is reversed, because our function is descending
                cos(angleI) > cosI
            }
            assertEquals(angle, angleI, 1e-7)
        }
    }
}