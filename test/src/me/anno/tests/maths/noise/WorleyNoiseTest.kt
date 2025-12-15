package me.anno.tests.maths.noise

import me.anno.maths.Maths.length
import me.anno.maths.Maths.posMod
import me.anno.maths.noise.WorleyCell
import me.anno.maths.noise.WorleyNoise
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class WorleyNoiseTest {

    private fun forEach1d(callback: (x: Float) -> Unit) {
        repeat(1000) {
            val x = it * 0.136f
            callback(x)
        }
    }

    private fun forEach2d(callback: (x: Float, y: Float) -> Unit) {
        repeat(1000) {
            val x = it * 0.137f
            val y = it * -0.271f
            callback(x, y)
        }
    }

    private fun forEach3d(callback: (x: Float, y: Float, z: Float) -> Unit) {
        repeat(1000) {
            val x = it * 0.137f
            val y = it * -0.271f
            val z = it * 0.412f
            callback(x, y, z)
        }
    }

    @Test
    fun distancesAreSmall1d() {
        val n1 = WorleyNoise(1234L)
        forEach1d { x ->
            val dist = n1.getDistanceSq(x)
            assertTrue(dist in 0f..2f) { "$dist !in 0 .. 2" }
        }
    }

    @Test
    fun distancesAreSmall2d() {
        val n1 = WorleyNoise(1234L)
        forEach2d { x, y ->
            val dist = n1.getDistanceSq(x, y)
            assertTrue(dist in 0f..2f) { "$dist !in 0 .. 2" }
        }
    }

    @Test
    fun distancesAreSmall3d() {
        val n1 = WorleyNoise(1234L)
        forEach3d { x, y, z ->
            val dist = n1.getDistanceSq(x, y, z)
            assertTrue(dist in 0f..2f) { "$dist !in 0 .. 2" }
        }
    }

    @Test
    fun deterministicForSameSeed1d() {
        val n1 = WorleyNoise(1234L)
        val n2 = WorleyNoise(1234L)
        forEach1d { x ->
            assertEquals(
                n1.getDistance(x),
                n2.getDistance(x),
                1e-6f
            )
        }
    }

    @Test
    fun deterministicForSameSeed2d() {
        val n1 = WorleyNoise(1234L)
        val n2 = WorleyNoise(1234L)
        forEach2d { x, y ->
            assertEquals(
                n1.getDistance(x, y),
                n2.getDistance(x, y),
                1e-6f
            )
        }
    }

    @Test
    fun deterministicForSameSeed3d() {
        val n1 = WorleyNoise(1234L)
        val n2 = WorleyNoise(1234L)
        forEach3d { x, y, z ->
            assertEquals(
                n1.getDistance(x, y, z),
                n2.getDistance(x, y, z),
                1e-6f
            )
        }
    }

    @Test
    fun distanceSquaredMatchesDistance1d() {
        val noise = WorleyNoise(42L)
        forEach1d { x ->
            val dist = noise.getDistance(x)
            val distSq = noise.getDistanceSq(x)
            assertEquals(dist * dist, distSq, 1e-4f)
        }
    }

    @Test
    fun distanceSquaredMatchesDistance2d() {
        val noise = WorleyNoise(42L)
        forEach2d { x, y ->
            val dist = noise.getDistance(x, y)
            val distSq = noise.getDistanceSq(x, y)
            assertEquals(dist * dist, distSq, 1e-4f)
        }
    }

    @Test
    fun distanceSquaredMatchesDistance3d() {
        val noise = WorleyNoise(42L)
        forEach3d { x, y, z ->
            val dist = noise.getDistance(x, y, z)
            val distSq = noise.getDistanceSq(x, y, z)
            assertEquals(dist * dist, distSq, 1e-4f)
        }
    }

    @Test
    fun returnedCellProducesCorrectDistance1d() {
        val noise = WorleyNoise(777L)
        val cell = WorleyCell()
        forEach1d { x ->
            val actual = noise.getDistance(x, cell)
            val dx = cell.xi + cell.cellX - x
            val expected = abs(dx)
            assertEquals(expected, actual, 1e-4f)
        }
    }

    @Test
    fun returnedCellProducesCorrectDistance2d() {
        val noise = WorleyNoise(777L)
        val cell = WorleyCell()
        forEach2d { x, y ->
            val actual = noise.getDistance(x, y, cell)
            val dx = cell.xi + cell.cellX - x
            val dy = cell.yi + cell.cellY - y
            val expected = hypot(dx, dy)
            assertEquals(expected, actual, 1e-4f)
        }
    }

    @Test
    fun returnedCellProducesCorrectDistance3d() {
        val noise = WorleyNoise(777L)
        val cell = WorleyCell()
        forEach3d { x, y, z ->
            val actual = noise.getDistance(x, y, z, cell)
            val dx = cell.xi + cell.cellX - x
            val dy = cell.yi + cell.cellY - y
            val dz = cell.zi + cell.cellZ - z
            val expected = length(dx, dy, dz)
            assertEquals(expected, actual, 1e-4f)
        }
    }

    @Test
    fun continuityAcrossCellBoundaries1d() {
        val noise = WorleyNoise(123L)
        for (i in -100..100) {
            val x1 = i.toFloat() + 0.9999f
            val x2 = i.toFloat() + 1.0001f
            val d1 = noise.getDistance(x1)
            val d2 = noise.getDistance(x2)
            assertEquals(d1, d2, 0.1f)
        }
    }

    @Test
    fun continuityAcrossCellBoundaries2d() {
        val noise = WorleyNoise(123L)
        for (i in -100..100) {
            val x1 = i.toFloat() + 0.9999f
            val x2 = i.toFloat() + 1.0001f
            val d1 = noise.getDistance(x1, 0.25f)
            val d2 = noise.getDistance(x2, 0.25f)
            assertEquals(d1, d2, 0.1f)
        }
    }

    @Test
    fun continuityAcrossCellBoundaries3d() {
        val noise = WorleyNoise(123L)
        for (i in -100..100) {
            val x1 = i.toFloat() + 0.9999f
            val x2 = i.toFloat() + 1.0001f
            val d1 = noise.getDistance(x1, 0.25f, 0.7f)
            val d2 = noise.getDistance(x2, 0.25f, 0.7f)
            assertEquals(d1, d2, 0.1f)
        }
    }

    @Test
    fun zeroRandomnessCentersCells1d() {
        val noise = WorleyNoise(1234L, randomness = 0f)
        val cell = WorleyCell()
        forEach1d { x ->
            noise.getDistance(x, cell)
            assertEquals(0.5f, cell.cellX)
        }
    }

    @Test
    fun zeroRandomnessCentersCells2d() {
        val noise = WorleyNoise(1234L, randomness = 0f)
        val cell = WorleyCell()
        forEach2d { x, y ->
            noise.getDistance(x, y, cell)
            assertEquals(0.5f, cell.cellX)
            assertEquals(0.5f, cell.cellY)
        }
    }

    @Test
    fun zeroRandomnessCentersCells3d() {
        val noise = WorleyNoise(1234L, randomness = 0f)
        val cell = WorleyCell()
        forEach3d { x, y, z ->
            noise.getDistance(x, y, z, cell)
            assertEquals(0.5f, cell.cellX)
            assertEquals(0.5f, cell.cellY)
            assertEquals(0.5f, cell.cellZ)
        }
    }

    @Test
    fun localSmoothness1d() {
        val noise = WorleyNoise(321L)
        val delta = 1e-3f
        forEach1d { x ->
            val d1 = noise.getDistance(x)
            val d2 = noise.getDistance(x + delta)
            assertEquals(d1, d2, 1.1f * delta)
        }
    }

    @Test
    fun localSmoothness2d() {
        val noise = WorleyNoise(321L)
        val delta = 1e-3f
        forEach2d { x, y ->
            val d1 = noise.getDistance(x, y)
            val d2 = noise.getDistance(x + delta, y)
            assertEquals(d1, d2, 1.1f * delta)
        }
    }

    @Test
    fun localSmoothness3d() {
        val noise = WorleyNoise(321L)
        val delta = 1e-3f
        forEach3d { x, y, z ->
            val d1 = noise.getDistance(x, y, z)
            val d2 = noise.getDistance(x + delta, y, z)
            assertEquals(d1, d2, 1.1f * delta)
        }
    }

    @Test
    fun differentSeedsDiffer1d() {
        val a = WorleyNoise(1L)
        val b = WorleyNoise(2L)
        var different = 0
        forEach1d { x ->
            if (abs(a.getDistance(x) - b.getDistance(x)) > 1e-4f) {
                different++
            }
        }
        assertGreaterThanEquals(different, 98)
    }

    @Test
    fun differentSeedsDiffer2d() {
        val a = WorleyNoise(1L)
        val b = WorleyNoise(2L)
        var different = 0
        forEach2d { x, y ->
            if (abs(a.getDistance(x, y) - b.getDistance(x, y)) > 1e-4f) {
                different++
            }
        }
        assertGreaterThanEquals(different, 98)
    }

    @Test
    fun differentSeedsDiffer3d() {
        val a = WorleyNoise(1L)
        val b = WorleyNoise(2L)
        var different = 0
        forEach3d { x, y, z ->
            if (abs(a.getDistance(x, y, z) - b.getDistance(x, y, z)) > 1e-4f) {
                different++
            }
        }
        assertGreaterThanEquals(different, 98)
    }

    @Test
    fun cellStability1d() {
        val noise = WorleyNoise(456L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach1d { x ->
            if (noise.getDistanceToEdge(x) > 1e-3f) {
                noise.getDistance(x, c1)
                noise.getDistance(x + 1e-4f, c2)
                assertEquals(c1.xi, c2.xi)
            }
        }
    }

    @Test
    fun cellStability2d() {
        val noise = WorleyNoise(456L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach2d { x, y ->
            noise.getDistance(x, y, c1)
            noise.getDistance(x + 1e-4f, y, c2)
            assertEquals(c1.xi, c2.xi)
            assertEquals(c1.yi, c2.yi)
        }
    }

    @Test
    fun cellStability3d() {
        val noise = WorleyNoise(456L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach3d { x, y, z ->
            noise.getDistance(x, y, z, c1)
            noise.getDistance(x + 1e-4f, y, z, c2)
            assertEquals(c1.xi, c2.xi)
            assertEquals(c1.yi, c2.yi)
            assertEquals(c1.zi, c2.zi)
        }
    }

    @Test
    fun largeCoordinateStability1d() {
        val noise = WorleyNoise(999L)
        for (x in coords) {
            val d = noise.getDistance(x)
            assertTrue(d in 0f..2f)
        }
    }

    @Test
    fun largeCoordinateStability2d() {
        val noise = WorleyNoise(999L)
        for (x in coords) {
            val d = noise.getDistance(x, x * 0.3f)
            assertTrue(d in 0f..2f)
        }
    }

    @Test
    fun largeCoordinateStability3d() {
        val noise = WorleyNoise(999L)
        for (x in coords) {
            val d = noise.getDistance(x, x * 0.3f, x * -0.2f)
            assertTrue(d in 0f..2f)
        }
    }

    @Test
    fun distanceToEdgeIsNonNegative1d() {
        val noise = WorleyNoise(123L)
        forEach1d { x ->
            val dist = noise.getDistanceToEdge(x)
            assertTrue(dist >= 0f)
        }
    }

    @Test
    fun distanceToEdgeIsNonNegative2d() {
        val noise = WorleyNoise(123L)
        forEach2d { x, y ->
            val dist = noise.getDistanceToEdge(x, y)
            assertTrue(dist >= 0f)
        }
    }

    @Test
    fun distanceToEdgeIsNonNegative3d() {
        val noise = WorleyNoise(123L)
        forEach3d { x, y, z ->
            val dist = noise.getDistanceToEdge(x, y, z)
            assertTrue(dist >= 0f)
        }
    }

    @Test
    fun distanceToEdgeZeroAtMidpointBetweenCells1d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        // With randomness = 0, feature points are at center coordinates
        val dist = noise.getDistanceToEdge(1f)
        assertEquals(0f, dist)
    }

    @Test
    fun distanceToEdgeZeroAtMidpointBetweenCells2d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        // With randomness = 0, feature points are at center coordinates
        val dist = noise.getDistanceToEdge(1f, 0.1f)
        assertEquals(0f, dist)
    }

    @Test
    fun distanceToEdgeZeroAtMidpointBetweenCells3d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        // With randomness = 0, feature points are at center coordinates
        val dist = noise.getDistanceToEdge(1f, 0.2f, 0.1f)
        assertEquals(0f, dist)
    }

    @Test
    fun distanceToEdgeMatchesF1F2Relation1d() {
        val noise = WorleyNoise(42L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach1d { x ->
            val edge = noise.getDistanceToEdge(x, c1, c2)
            val expected = (c2.distance - c1.distance) * 0.5f
            assertEquals(edge, expected)
        }
    }

    @Test
    fun distanceToEdgeMatchesF1F2Relation2d() {
        val noise = WorleyNoise(42L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach2d { x, y ->
            val edge = noise.getDistanceToEdge(x, y, c1, c2)
            val expected = (c2.distance - c1.distance) * 0.5f
            assertEquals(edge, expected)
        }
    }

    @Test
    fun distanceToEdgeMatchesF1F2Relation3d() {
        val noise = WorleyNoise(42L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach3d { x, y, z ->
            val edge = noise.getDistanceToEdge(x, y, z, c1, c2)
            val expected = (c2.distance - c1.distance) * 0.5f
            assertEquals(edge, expected)
        }
    }

    @Test
    fun returnedCellsAreOrderedByDistance1d() {
        val noise = WorleyNoise(77L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach1d { x ->
            noise.getDistanceToEdge(x, c1, c2)
            assertTrue(c1.distance <= c2.distance)
        }
    }

    @Test
    fun returnedCellsAreOrderedByDistance2d() {
        val noise = WorleyNoise(77L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach2d { x, y ->
            noise.getDistanceToEdge(x, y, c1, c2)
            assertTrue(c1.distance <= c2.distance)
        }
    }

    @Test
    fun returnedCellsAreOrderedByDistance3d() {
        val noise = WorleyNoise(77L)
        val c1 = WorleyCell()
        val c2 = WorleyCell()
        forEach3d { x, y, z ->
            noise.getDistanceToEdge(x, y, z, c1, c2)
            assertTrue(c1.distance <= c2.distance)
        }
    }

    @Test
    fun distanceToEdgeIsContinuous1d() {
        val noise = WorleyNoise(999L)
        forEach1d { x ->
            val d1 = noise.getDistanceToEdge(x)
            val d2 = noise.getDistanceToEdge(x + 1e-3f)
            assertTrue(abs(d1 - d2) < 0.01f)
        }
    }

    @Test
    fun distanceToEdgeIsContinuous2d() {
        val noise = WorleyNoise(999L)
        forEach2d { x, y ->
            val d1 = noise.getDistanceToEdge(x, y)
            val d2 = noise.getDistanceToEdge(x + 1e-3f, y)
            assertTrue(abs(d1 - d2) < 0.01f)
        }
    }

    @Test
    fun distanceToEdgeIsContinuous3d() {
        val noise = WorleyNoise(999L)
        forEach3d { x, y, z ->
            val d1 = noise.getDistanceToEdge(x, y, z)
            val d2 = noise.getDistanceToEdge(x + 1e-3f, y, z)
            assertTrue(abs(d1 - d2) < 0.01f)
        }
    }

    @Test
    fun zeroRandomnessProducesCorrectGridEdges1d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        forEach1d { x ->
            val d = noise.getDistanceToEdge(x)
            val dist = posMod(x, 1f)
            val expected = min(dist, 1f - dist)
            assertEquals(d, expected, 1e-5f)
        }
    }

    @Test
    fun zeroRandomnessProducesCorrectGridEdges2d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        forEach1d { x ->
            val d = noise.getDistanceToEdge(x, 0.5f)
            val dist = posMod(x, 1f)
            val expected = min(dist, 1f - dist)
            assertEquals(d, expected, 1e-5f)
        }
    }

    @Test
    fun zeroRandomnessProducesCorrectGridEdges3d() {
        val noise = WorleyNoise(0L, randomness = 0f)
        forEach1d { x ->
            val d = noise.getDistanceToEdge(x, 0.5f, 0.5f)
            val dist = posMod(x, 1f)
            val expected = min(dist, 1f - dist)
            assertEquals(d, expected, 1e-5f)
        }
    }

    @Test
    fun distanceToEdgeIsDeterministic1d() {
        val a = WorleyNoise(1234L)
        val b = WorleyNoise(1234L)
        forEach1d { x ->
            assertEquals(
                a.getDistanceToEdge(x),
                b.getDistanceToEdge(x)
            )
        }
    }

    @Test
    fun distanceToEdgeIsDeterministic2d() {
        val a = WorleyNoise(1234L)
        val b = WorleyNoise(1234L)
        forEach2d { x, y ->
            assertEquals(
                a.getDistanceToEdge(x, y),
                b.getDistanceToEdge(x, y)
            )
        }
    }

    @Test
    fun distanceToEdgeIsDeterministic3d() {
        val a = WorleyNoise(1234L)
        val b = WorleyNoise(1234L)
        repeat(500) {
            val x = it * 0.11f
            val y = it * -0.13f
            val z = it * 0.17f
            assertEquals(
                a.getDistanceToEdge(x, y, z),
                b.getDistanceToEdge(x, y, z)
            )
        }
    }

    val coords = floatArrayOf(
        1e3f, 1e5f, 1e7f,
        -1e3f, -1e5f, -1e7f
    )

    @Test
    fun distanceToEdgeLargeCoordinates1d() {
        val noise = WorleyNoise(101L)
        for (x in coords) {
            val d = noise.getDistanceToEdge(x)
            assertTrue(d in 0f..1f)
        }
    }

    @Test
    fun distanceToEdgeLargeCoordinates2d() {
        val noise = WorleyNoise(101L)
        for (x in coords) {
            val d = noise.getDistanceToEdge(x, x * 0.3f)
            assertTrue(d in 0f..1f)
        }
    }

    @Test
    fun distanceToEdgeLargeCoordinates3d() {
        val noise = WorleyNoise(101L)
        for (x in coords) {
            val d = noise.getDistanceToEdge(x, x * 0.3f, x * -0.2f)
            assertTrue(d in 0f..1f)
        }
    }
}