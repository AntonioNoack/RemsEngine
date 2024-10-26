package me.anno.tests.maths.noise

import me.anno.maths.Maths.mix
import me.anno.maths.Maths.smoothStepUnsafe
import me.anno.maths.noise.FullNoise
import me.anno.tests.LOGGER
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.createList
import org.junit.jupiter.api.Test
import kotlin.math.min
import kotlin.test.assertEquals

// todo test gradients
class FullNoiseTest {
    @Test
    fun testEqualDistribution1d() {
        val noise = FullNoise(1234)
        val history = IntArray(10)
        for (x in 0 until 10000) {
            val hi = min((noise[x] * history.size).toInt(), history.lastIndex)
            history[hi]++
        }
        LOGGER.info("History: ${history.toList()}")
        assertTrue(history.all { it in 930..1070 })
    }

    @Test
    fun testInterpolation1d() {
        val noise = FullNoise(1234)
        val sx = 30
        val grid = FloatArray(sx + 1) { x -> noise[x] }
        val dxi = 7
        for (x in 0 until sx) {
            val v00 = grid[x]
            val v10 = grid[x + 1]
            for (dx in 0..dxi) {
                val vx = dx.toFloat() / dxi
                val expected = mix(v00, v10, vx)
                assertEquals(
                    expected, noise[x + vx],
                    1e-5f
                )
            }
        }
    }

    @Test
    fun testSmoothInterpolation1d() {
        val noise = FullNoise(1234)
        val sx = 30
        val grid = FloatArray(sx + 1) { x -> noise[x] }
        val dxi = 7
        for (x in 0 until sx) {
            val v00 = grid[x]
            val v10 = grid[x + 1]
            for (dx in 0..dxi) {
                val vx = smoothStepUnsafe(dx.toFloat() / dxi)
                val expected = mix(v00, v10, vx)
                assertEquals(expected, noise.getSmooth(x + dx.toFloat() / dxi), 1e-5f)
            }
        }
    }

    @Test
    fun testEqualDistribution2d() {
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
    fun testInterpolation2d() {
        val noise = FullNoise(1234)
        val sy = 5
        val sx = 6
        val grid = createList(sx + 1) { x ->
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
    fun testSmoothInterpolation2d() {
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

    @Test
    fun testEqualDistribution3d() {
        val noise = FullNoise(1234)
        val history = IntArray(10)
        for (z in 0 until 21) {
            for (y in 0 until 22) {
                for (x in 0 until 22) {
                    val hi = min((noise[x, y, z] * history.size).toInt(), history.lastIndex)
                    history[hi]++
                }
            }
        }
        LOGGER.info("History: ${history.toList()}")
        assertTrue(history.all { it in 930..1070 })
    }

    @Test
    fun testInterpolation3d() {
        val noise = FullNoise(1234)
        val sz = 4
        val sy = 4
        val sx = 4
        val grid = createList(sx + 1) { x ->
            createList(sy + 1) { y ->
                FloatArray(sz + 1) { z ->
                    noise[x, y, z]
                }
            }
        }
        val dzi = 3
        val dyi = 3
        val dxi = 3
        for (z in 0 until sz) {
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    val v000 = grid[x][y][z]
                    val v010 = grid[x][y + 1][z]
                    val v100 = grid[x + 1][y][z]
                    val v110 = grid[x + 1][y + 1][z]
                    val v001 = grid[x][y][z + 1]
                    val v011 = grid[x][y + 1][z + 1]
                    val v101 = grid[x + 1][y][z + 1]
                    val v111 = grid[x + 1][y + 1][z + 1]
                    for (dz in 0 until dzi) {
                        val vz = dz.toFloat() / dzi
                        for (dy in 0 until dyi) {
                            val vy = dy.toFloat() / dyi
                            for (dx in 0..dxi) {
                                val vx = dx.toFloat() / dxi
                                val expected = mix(
                                    mix(
                                        mix(v000, v010, vy),
                                        mix(v100, v110, vy),
                                        vx
                                    ),
                                    mix(
                                        mix(v001, v011, vy),
                                        mix(v101, v111, vy),
                                        vx
                                    ), vz
                                )
                                assertEquals(
                                    expected, noise[x + vx, y + vy, z + vz],
                                    1e-5f
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testSmoothInterpolation3d() {
        val noise = FullNoise(1234)
        val sz = 4
        val sy = 4
        val sx = 4
        val grid = createList(sx + 1) { x ->
            createList(sy + 1) { y ->
                FloatArray(sz + 1) { z ->
                    noise[x, y, z]
                }
            }
        }
        val dzi = 3
        val dyi = 3
        val dxi = 3
        for (z in 0 until sz) {
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    val v000 = grid[x][y][z]
                    val v010 = grid[x][y + 1][z]
                    val v100 = grid[x + 1][y][z]
                    val v110 = grid[x + 1][y + 1][z]
                    val v001 = grid[x][y][z + 1]
                    val v011 = grid[x][y + 1][z + 1]
                    val v101 = grid[x + 1][y][z + 1]
                    val v111 = grid[x + 1][y + 1][z + 1]
                    for (dz in 0 until dzi) {
                        val vz = smoothStepUnsafe(dz.toFloat() / dzi)
                        for (dy in 0 until dyi) {
                            val vy = smoothStepUnsafe(dy.toFloat() / dyi)
                            for (dx in 0..dxi) {
                                val vx = smoothStepUnsafe(dx.toFloat() / dxi)
                                val expected = mix(
                                    mix(
                                        mix(v000, v010, vy),
                                        mix(v100, v110, vy),
                                        vx
                                    ),
                                    mix(
                                        mix(v001, v011, vy),
                                        mix(v101, v111, vy),
                                        vx
                                    ),
                                    vz
                                )
                                assertEquals(
                                    expected, noise.getSmooth(
                                        x + dx.toFloat() / dxi,
                                        y + dy.toFloat() / dyi,
                                        z + dz.toFloat() / dzi
                                    ), 1e-5f
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testEqualDistribution4d() {
        val noise = FullNoise(1234)
        val history = IntArray(10)
        for (w in 0 until 10) {
            for (z in 0 until 10) {
                for (y in 0 until 10) {
                    for (x in 0 until 10) {
                        val hi = min((noise[x, y, z, w] * history.size).toInt(), history.lastIndex)
                        history[hi]++
                    }
                }
            }
        }
        LOGGER.info("History: ${history.toList()}")
        assertTrue(history.all { it in 930..1070 })
    }

    @Test
    fun testInterpolation4d() {
        val noise = FullNoise(1234)
        val sw = 3
        val sz = 3
        val sy = 3
        val sx = 3
        val grid = createList(sx + 1) { x ->
            createList(sy + 1) { y ->
                createList(sz + 1) { z ->
                    FloatArray(sw + 1) { w ->
                        noise[x, y, z, w]
                    }
                }
            }
        }
        val dwi = 3
        val dzi = 3
        val dyi = 3
        val dxi = 3
        for (w in 0 until sw) {
            for (z in 0 until sz) {
                for (y in 0 until sy) {
                    for (x in 0 until sx) {
                        val v0000 = grid[x][y][z][w]
                        val v0100 = grid[x][y + 1][z][w]
                        val v1000 = grid[x + 1][y][z][w]
                        val v1100 = grid[x + 1][y + 1][z][w]
                        val v0010 = grid[x][y][z + 1][w]
                        val v0110 = grid[x][y + 1][z + 1][w]
                        val v1010 = grid[x + 1][y][z + 1][w]
                        val v1110 = grid[x + 1][y + 1][z + 1][w]
                        val v0001 = grid[x][y][z][w + 1]
                        val v0101 = grid[x][y + 1][z][w + 1]
                        val v1001 = grid[x + 1][y][z][w + 1]
                        val v1101 = grid[x + 1][y + 1][z][w + 1]
                        val v0011 = grid[x][y][z + 1][w + 1]
                        val v0111 = grid[x][y + 1][z + 1][w + 1]
                        val v1011 = grid[x + 1][y][z + 1][w + 1]
                        val v1111 = grid[x + 1][y + 1][z + 1][w + 1]
                        for (dw in 0 until dwi) {
                            val vw = dw.toFloat() / dwi
                            for (dz in 0 until dzi) {
                                val vz = dz.toFloat() / dzi
                                for (dy in 0 until dyi) {
                                    val vy = dy.toFloat() / dyi
                                    for (dx in 0..dxi) {
                                        val vx = dx.toFloat() / dxi
                                        val expected = mix(
                                            mix(
                                                mix(
                                                    mix(v0000, v0100, vy),
                                                    mix(v1000, v1100, vy),
                                                    vx
                                                ),
                                                mix(
                                                    mix(v0010, v0110, vy),
                                                    mix(v1010, v1110, vy),
                                                    vx
                                                ), vz
                                            ),
                                            mix(
                                                mix(
                                                    mix(v0001, v0101, vy),
                                                    mix(v1001, v1101, vy),
                                                    vx
                                                ),
                                                mix(
                                                    mix(v0011, v0111, vy),
                                                    mix(v1011, v1111, vy),
                                                    vx
                                                ), vz
                                            ),
                                            vw
                                        )
                                        assertEquals(
                                            expected, noise[x + vx, y + vy, z + vz, w + vw],
                                            1e-5f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testSmoothInterpolation4d() {
        val noise = FullNoise(1234)
        val sw = 3
        val sz = 3
        val sy = 3
        val sx = 3
        val grid = createList(sx + 1) { x ->
            createList(sy + 1) { y ->
                createList(sz + 1) { z ->
                    FloatArray(sw + 1) { w ->
                        noise[x, y, z, w]
                    }
                }
            }
        }
        val dwi = 3
        val dzi = 3
        val dyi = 3
        val dxi = 3
        for (w in 0 until sw) {
            for (z in 0 until sz) {
                for (y in 0 until sy) {
                    for (x in 0 until sx) {
                        val v0000 = grid[x][y][z][w]
                        val v0100 = grid[x][y + 1][z][w]
                        val v1000 = grid[x + 1][y][z][w]
                        val v1100 = grid[x + 1][y + 1][z][w]
                        val v0010 = grid[x][y][z + 1][w]
                        val v0110 = grid[x][y + 1][z + 1][w]
                        val v1010 = grid[x + 1][y][z + 1][w]
                        val v1110 = grid[x + 1][y + 1][z + 1][w]
                        val v0001 = grid[x][y][z][w + 1]
                        val v0101 = grid[x][y + 1][z][w + 1]
                        val v1001 = grid[x + 1][y][z][w + 1]
                        val v1101 = grid[x + 1][y + 1][z][w + 1]
                        val v0011 = grid[x][y][z + 1][w + 1]
                        val v0111 = grid[x][y + 1][z + 1][w + 1]
                        val v1011 = grid[x + 1][y][z + 1][w + 1]
                        val v1111 = grid[x + 1][y + 1][z + 1][w + 1]
                        for (dw in 0 until dwi) {
                            val vw = smoothStepUnsafe(dw.toFloat() / dwi)
                            for (dz in 0 until dzi) {
                                val vz = smoothStepUnsafe(dz.toFloat() / dzi)
                                for (dy in 0 until dyi) {
                                    val vy = smoothStepUnsafe(dy.toFloat() / dyi)
                                    for (dx in 0..dxi) {
                                        val vx = smoothStepUnsafe(dx.toFloat() / dxi)
                                        val expected = mix(
                                            mix(
                                                mix(
                                                    mix(v0000, v0100, vy),
                                                    mix(v1000, v1100, vy),
                                                    vx
                                                ),
                                                mix(
                                                    mix(v0010, v0110, vy),
                                                    mix(v1010, v1110, vy),
                                                    vx
                                                ), vz
                                            ),
                                            mix(
                                                mix(
                                                    mix(v0001, v0101, vy),
                                                    mix(v1001, v1101, vy),
                                                    vx
                                                ),
                                                mix(
                                                    mix(v0011, v0111, vy),
                                                    mix(v1011, v1111, vy),
                                                    vx
                                                ), vz
                                            ),
                                            vw
                                        )
                                        assertEquals(
                                            expected, noise.getSmooth(
                                                x + dx.toFloat() / dxi,
                                                y + dy.toFloat() / dyi,
                                                z + dz.toFloat() / dzi,
                                                w + dw.toFloat() / dwi
                                            ), 1e-5f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}