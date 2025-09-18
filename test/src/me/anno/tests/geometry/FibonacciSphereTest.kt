package me.anno.tests.geometry

import me.anno.maths.MinMax.max
import me.anno.maths.MinMax.min
import me.anno.maths.geometry.FibonacciSphere
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class FibonacciSphereTest {
    @Test
    fun testMinMaxDistance() {
        assertEquals(Vector2f(2f), minMaxDistance(2))
        assertEquals(Vector2f(sqrt(2f)), minMaxDistance(3), 1e-3)

        val (min100, max100) = minMaxDistance(100)
        assertTrue(min100 > 0.19f)
        assertTrue(max100 < 0.36f)

        val (min1k, max1k) = minMaxDistance(1000)
        assertTrue(min1k > 0.05f)
        assertTrue(max1k < 0.12f)
    }

    fun minMaxDistance(n: Int): Vector2f {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        val positions = FibonacciSphere.create(n)
        for (i in positions.indices) {
            var minDistance = Float.POSITIVE_INFINITY
            for (j in positions.indices) {
                if (i == j) continue
                val a = positions[i]
                val b = positions[j]
                val distance = a.distance(b)
                minDistance = min(distance, minDistance)
            }
            min = min(min, minDistance)
            max = max(max, minDistance)
        }
        return Vector2f(min, max)
    }
}