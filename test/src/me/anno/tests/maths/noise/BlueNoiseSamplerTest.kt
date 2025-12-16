package me.anno.tests.maths.noise

import me.anno.maths.noise.BlueNoiseSampler.sampleBlueNoise2f
import me.anno.maths.noise.BlueNoiseSampler.sampleBlueNoise3f
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.abs

class BlueNoiseSamplerTest {

    @Test
    fun generatesNonEmptyPointSet2f() {
        val points = sampleBlueNoise2f(
            size = Vector2f(0f),
            minDist = 5f,
            seed = 1L
        )
        assertEquals(listOf(Vector2f()), points)
    }

    @Test
    fun generatesNonEmptyPointSet3f() {
        val points = sampleBlueNoise3f(
            size = Vector3f(0f),
            minDist = 5f,
            seed = 1L
        )
        assertEquals(listOf(Vector3f()), points)
    }

    @Test
    fun allPointsInsideBounds2f() {
        val size = Vector2f(100f, 80f)
        val points = sampleBlueNoise2f(size, 4f, 2L)
        for (p in points) {
            assertTrue(p.x >= 0f && p.x < size.x)
            assertTrue(p.y >= 0f && p.y < size.y)
        }
    }

    @Test
    fun allPointsInsideBounds3f() {
        val size = Vector3f(30f, 40f, 50f)
        val points = sampleBlueNoise3f(size, 5f, 2L)
        for (p in points) {
            assertTrue(p.x >= 0f && p.x < size.x)
            assertTrue(p.y >= 0f && p.y < size.y)
            assertTrue(p.z >= 0f && p.z < size.z)
        }
    }

    @Test
    fun minimumDistanceRespected2f() {
        val minDist = 6f
        val points = sampleBlueNoise2f(
            Vector2f(120f), minDist,
            3L
        )
        val r2 = minDist * minDist
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                assertGreaterThanEquals(points[i].distanceSquared(points[j]), r2)
            }
        }
    }

    @Test
    fun minimumDistanceRespected3d() {
        val minDist = 7f
        val points = sampleBlueNoise3f(
            Vector3f(60f), minDist,
            3L
        )
        val r2 = minDist * minDist
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                assertGreaterThanEquals(points[i].distanceSquared(points[j]), r2)
            }
        }
    }

    @Test
    fun deterministicWithSameSeed2f() {
        val pa = sampleBlueNoise2f(Vector2f(80f), 5f, 42L)
        val pb = sampleBlueNoise2f(Vector2f(80f), 5f, 42L)
        assertEquals(pa, pb)
    }

    @Test
    fun deterministicWithSameSeed3f() {
        val pa = sampleBlueNoise3f(Vector3f(80f), 5f, 42L)
        val pb = sampleBlueNoise3f(Vector3f(80f), 5f, 42L)
        assertEquals(pa, pb)
    }

    @Test
    fun differentSeedsDiffer2f() {
        val pa = sampleBlueNoise2f(Vector2f(100f), 6f, 1L)
        val pb = sampleBlueNoise2f(Vector2f(100f), 6f, 2L)
        // Extremely unlikely to be identical
        assertNotEquals(pa, pb)
    }

    @Test
    fun differentSeedsDiffer3f() {
        val pa = sampleBlueNoise3f(Vector3f(100f), 6f, 1L)
        val pb = sampleBlueNoise3f(Vector3f(100f), 6f, 2L)
        // Extremely unlikely to be identical
        assertNotEquals(pa, pb)
    }

    @Test
    fun pointCountWithinExpectedRange2f() {
        val size = Vector2f(100f)
        val minDist = 5f

        val points = sampleBlueNoise2f(size, minDist, 5L)
        val area = size.x * size.y
        val theoreticalMax = area / (minDist * minDist)

        assertTrue(points.size > theoreticalMax * 0.3f)
        assertTrue(points.size < theoreticalMax * 1.2f)
    }

    @Test
    fun pointCountWithinExpectedRange3f() {
        val size = Vector3f(100f)
        val minDist = 10f

        val points = sampleBlueNoise3f(size, minDist, 5L)
        val area = size.x * size.y * size.z
        val theoreticalMax = area / (minDist * minDist * minDist)

        assertTrue(points.size > theoreticalMax * 0.3f)
        assertTrue(points.size < theoreticalMax * 1.2f)
    }

    @Test
    fun noInvalidValues2f() {
        val points = sampleBlueNoise2f(Vector2f(100f), 6f, 99L)
        for (p in points) {
            assertTrue(p.isFinite)
        }
    }

    @Test
    fun noInvalidValues3f() {
        val points = sampleBlueNoise3f(Vector3f(100f), 6f, 99L)
        for (p in points) {
            assertTrue(p.isFinite)
        }
    }

    @Test
    fun flat3fBehavesLike2f() {
        val size = Vector3f(100f, 100f, 0.001f)
        val minDist = 5f
        val points = sampleBlueNoise3f(size, minDist, 123L)
        for (p in points) {
            assertTrue(abs(p.z) < 0.01f)
        }

        val area = size.x * size.y
        val theoreticalMax = area / (minDist * minDist)
        assertTrue(points.size > theoreticalMax * 0.3f)
        assertTrue(points.size < theoreticalMax * 1.2f)
    }
}