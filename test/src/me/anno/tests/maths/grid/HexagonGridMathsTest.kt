package me.anno.tests.maths.grid

import me.anno.maths.chunks.hexagon.HexagonGridMaths
import me.anno.maths.chunks.hexagon.HexagonGridMaths.coordsToIndex
import me.anno.maths.chunks.hexagon.HexagonGridMaths.indexToCoords
import me.anno.maths.chunks.hexagon.HexagonGridMaths.neighbors
import me.anno.maths.chunks.hexagon.HexagonGridMaths.vertices
import me.anno.maths.Maths.TAU
import org.joml.Vector2d
import org.joml.Vector2i
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HexagonGridMathsTest {

    @Test
    fun testNeighborPositions() {
        val lengths = neighbors
            .map { indexToCoords(it, Vector2d()) }
            .map { it.length() }
        assertEquals(lengths.min(), lengths.max(), 1e-15)
    }

    @Test
    fun testNeighborPositionsAngles() {
        val lengths = neighbors
            .map { indexToCoords(it, Vector2d()) }
            .map { (atan2(it.y, it.x) / TAU * 6) }
        assertEquals(listOf(0.5, 1.5, 2.5, -2.5, -1.5, -0.5), lengths)
    }

    @Test
    fun testVertexPositionsLengths() {
        val lengths = vertices.map { it.length() }
        println(lengths)
        assertEquals(1.0, lengths.min(), 1e-15)
        assertEquals(1.0, lengths.max(), 1e-15)
    }

    @Test
    fun testVertexPositionsAngles() {
        val lengths = vertices
            .map { (atan2(it.y, it.x) / TAU * 6) }
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, -2.0, -1.0), lengths)
    }

    @Test
    fun testCoordsIndicesInvertible1() {
        testCoordsIndicesInvertible(HexagonGridMaths::coordsToIndexFast)
    }

    @Test
    fun testCoordsIndicesInvertible2() {
        testCoordsIndicesInvertible(HexagonGridMaths::coordsToIndex)
    }

    fun testCoordsIndicesInvertible(coordsToIndex: (Vector2d, Vector2i, Vector2d) -> Vector2i) {
        val coords = Vector2d()
        val remainder = Vector2d()
        val index = Vector2i()
        for (i in -5 until 5) {
            for (j in -5 until 5) {
                assertSame(indexToCoords(i, j, coords), coords)
                assertSame(coordsToIndex(coords, index, remainder), index)
                assertEquals(i, index.x)
                assertEquals(j, index.y)
                assertEquals(0.0, remainder.x, 1e-14)
                assertEquals(0.0, remainder.y, 1e-14)
            }
        }
    }

    @Test
    fun testCoordsIndicesClosest() {
        // assert that that index is the closest
        val s = 50
        val remainder = Vector2d()
        val coords = Vector2d()
        val index = Vector2i()
        val center = Vector2d()
        for (i in -s until s) {
            for (j in -s until s) {
                coords.set(i * 5.0 / s, j * 5.0 / s)
                assertSame(coordsToIndex(coords, index, remainder), index)
                assertSame(indexToCoords(index, center), center)

                assertEquals(coords.x, center.x + remainder.x, 1e-15)
                assertEquals(coords.y, center.y + remainder.y, 1e-15)

                val byCenter = center.distanceSquared(coords) - 1e-7
                for (nei in neighbors) {
                    val neiIdx = indexToCoords(Vector2i(nei).add(index), Vector2d())
                    val byNei = neiIdx.distanceSquared(coords)
                    assertTrue(byCenter <= byNei + 1e-15)
                }
            }
        }
    }
}