package me.anno.tests.maths.noise

import me.anno.maths.Maths.mix
import me.anno.maths.Maths.smoothStepUnsafe
import me.anno.maths.noise.FullNoise
import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.createArrayList
import org.junit.jupiter.api.Test
import kotlin.math.min
import kotlin.test.assertEquals

class FullNoiseTest {
    @Test
    fun testEqualDistribution() {
        val noise = FullNoise(1234)
        val history = IntArray(10)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                val hi = min((noise[x, y] * history.size).toInt(), history.lastIndex)
                history[hi]++
            }
        }
        LOGGER.info("History: ${history.toList()}")
        assertTrue(history.all { it in 930..1070 })
    }

    @Test
    fun testInterpolation() {
        val noise = FullNoise(1234)
        val sy = 5
        val sx = 6
        val grid = createArrayList(sx + 1) { x ->
            FloatArray(sy + 1) { y ->
                noise[x, y]
            }
        }
        val dxi = 5
        val dyi = 7
        for (y in 0 until sy) {
            for (x in 0 until sx) {
                val v00 = grid[x][y]
                val v01 = grid[x][y + 1]
                val v10 = grid[x + 1][y]
                val v11 = grid[x + 1][y + 1]
                for (dy in 0 until dyi) {
                    val vy = dy.toFloat() / dyi
                    for (dx in 0..dxi) {
                        val vx = dx.toFloat() / dxi
                        val expected = mix(
                            mix(v00, v01, vy),
                            mix(v10, v11, vy),
                            vx
                        )
                        assertEquals(
                            expected, noise[x + vx, y + vy],
                            1e-5f
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testSmoothInterpolation() {
        val noise = FullNoise(1234)
        val sy = 5
        val sx = 6
        val grid = createArrayList(sx + 1) { x ->
            FloatArray(sy + 1) { y ->
                noise[x, y]
            }
        }
        val dxi = 5
        val dyi = 7
        for (y in 0 until sy) {
            for (x in 0 until sx) {
                val v00 = grid[x][y]
                val v01 = grid[x][y + 1]
                val v10 = grid[x + 1][y]
                val v11 = grid[x + 1][y + 1]
                for (dy in 0 until dyi) {
                    val vy = smoothStepUnsafe(dy.toFloat() / dyi)
                    for (dx in 0..dxi) {
                        val vx = smoothStepUnsafe(dx.toFloat() / dxi)
                        val expected = mix(
                            mix(v00, v01, vy),
                            mix(v10, v11, vy),
                            vx
                        )
                        assertEquals(
                            expected, noise.getSmooth(
                                x + dx.toFloat() / dxi,
                                y + dy.toFloat() / dyi
                            ), 1e-5f
                        )
                    }
                }
            }
        }
    }
}