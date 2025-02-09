package me.anno.tests.maths.grid

import me.anno.maths.Maths.TAU
import me.anno.maths.chunks.hexagon.HexagonGridMaths
import me.anno.maths.chunks.hexagon.HexagonGridMaths.corners
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getCenter
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getClosestHexagon
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getCorner
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getGridDistance
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getNeighborCorner
import me.anno.maths.chunks.hexagon.HexagonGridMaths.getNeighborHexagon
import me.anno.maths.chunks.hexagon.HexagonGridMaths.neighbors
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3i
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.atan2

class HexagonGridMathsTest {

    @Test
    fun testNeighborPositions() {
        val lengths = neighbors
            .map { getCenter(it, Vector2d()) }
            .map { it.length() }
        assertEquals(lengths.min(), lengths.max(), 1e-15)
    }

    @Test
    fun testNeighborPositionsAngles() {
        val lengths = neighbors
            .map { getCenter(it, Vector2d()) }
            .map { (atan2(it.y, it.x) / TAU * 6) }
        assertEquals(listOf(0.5, 1.5, 2.5, -2.5, -1.5, -0.5), lengths)
    }

    @Test
    fun testCornerPositionsLengths() {
        val lengths = corners.map { it.length() }
        println(lengths)
        assertEquals(1.0, lengths.min(), 1e-15)
        assertEquals(1.0, lengths.max(), 1e-15)
    }

    @Test
    fun testCornerPositionsAngles() {
        val lengths = corners
            .map { (atan2(it.y, it.x) / TAU * 6) }
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, -2.0, -1.0), lengths)
    }

    @Test
    fun testCenterIndicesInvertible1() {
        testCenterIndicesInvertible(HexagonGridMaths::getCloseHexagon)
    }

    @Test
    fun testCenterIndicesInvertible2() {
        testCenterIndicesInvertible(HexagonGridMaths::getClosestHexagon)
    }

    fun testCenterIndicesInvertible(centerToIndex: (Vector2d, Vector2i, Vector2d) -> Vector2i) {
        val center = Vector2d()
        val remainder = Vector2d()
        val index = Vector2i()
        for (i in -5 until 5) {
            for (j in -5 until 5) {
                assertSame(getCenter(i, j, center), center)
                assertSame(centerToIndex(center, index, remainder), index)
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
        val somePoint = Vector2d()
        val index = Vector2i()
        val center = Vector2d()
        for (i in -s until s) {
            for (j in -s until s) {
                somePoint.set(i * 5.0 / s, j * 5.0 / s)
                assertSame(getClosestHexagon(somePoint, index, remainder), index)
                assertSame(getCenter(index, center), center)

                assertEquals(somePoint.x, center.x + remainder.x, 1e-15)
                assertEquals(somePoint.y, center.y + remainder.y, 1e-15)

                val byCenter = center.distanceSquared(somePoint) - 1e-7
                for (neighbor in neighbors) {
                    val neiIdx = getCenter(neighbor.add(index, Vector2i()), Vector2d())
                    val byNei = neiIdx.distanceSquared(somePoint)
                    assertTrue(byCenter <= byNei + 1e-15)
                }
            }
        }
    }

    @Test
    fun testGridDistanceNeighbors0() {
        assertEquals(0, getGridDistance(Vector2i()))
    }

    @Test
    fun testGridDistanceNeighbors1() {
        assertEquals(1, getGridDistance(neighbors[0]))
        assertEquals(1, getGridDistance(neighbors[1]))
        assertEquals(1, getGridDistance(neighbors[2]))
        assertEquals(1, getGridDistance(neighbors[3]))
        assertEquals(1, getGridDistance(neighbors[4]))
        assertEquals(1, getGridDistance(neighbors[5]))
    }

    @Test
    fun testGridDistanceNeighbors2() {
        for (nei1 in neighbors) {
            for (nei2 in neighbors) {
                val nei = nei1 + nei2
                if (nei.lengthSquared() == 0L || nei in neighbors) {
                    continue
                }
                assertEquals(2, getGridDistance(nei))
            }
        }
    }

    @Test
    fun validateNeighborCorners() {
        for (vi in 0 until 6) {
            val cornerI = Vector3i(0, 0, vi)
            val corner = getCorner(cornerI, Vector2d())
            val otherNeighbors = ArrayList<Vector2d>()
            for (vj in 0 until 3) {
                val neighborI = getNeighborCorner(cornerI, vj, Vector3i())
                val neighbor = getCorner(neighborI, Vector2d())
                assertEquals(1.0, corner.distance(neighbor), 1e-15)
                for (otherNeighbor in otherNeighbors) {
                    assertEquals(3.0, neighbor.distanceSquared(otherNeighbor), 1e-15)
                }
                otherNeighbors.add(neighbor)
            }
        }
    }

    /**
     * given a hexagon corner, find the closest neighbor corner, that is not our own corner
     * */
    @Test
    fun generateNeighborCorners() {
        fun findOppositeCorner(ownCornerId: Int): Vector3i {
            val ownCorner = corners[ownCornerId]
            val minDistSqFromCenter = ownCorner.lengthSquared() * 1.2
            val tmp = Vector2d()
            for (vi in 0 until 6) {
                for (neighbor in neighbors) {
                    val pos = getCorner(neighbor.x, neighbor.y, vi, tmp)
                    if (pos.distanceSquared(ownCorner) in 0.99..1.01 &&
                        pos.lengthSquared() > minDistSqFromCenter
                    ) return Vector3i(neighbor.x, neighbor.y, vi)
                }
            }
            throw IllegalStateException()
        }

        println("val oppositeCorners = listOf(")
        for (i in 0 until 6) {
            val c = findOppositeCorner(i)
            println("  Vector3i(${c.x}, ${c.y}, ${c.z}),")
        }
        println(")")
    }

    @Test
    fun validateNeighborHexagons() {
        for (i in 0 until 6) {
            val cornerI = Vector3i(0, 0, i)
            val corner = getCorner(cornerI, Vector2d())
            val unique = HashSet<Vector2i>()
            for (j in 0 until 3) {
                val neighborHex = getNeighborHexagon(cornerI, j, Vector2i())
                val neighborCenter = getCenter(neighborHex, Vector2d())
                assertEquals(1.0, corner.distance(neighborCenter), 1e-15)
                assertEquals(if (j < 2) 3.0 else 0.0, neighborCenter.lengthSquared(), 1e-15)
                assertTrue(unique.add(neighborHex))
            }
        }
    }

    /**
     * given a hexagon corner, find all neighboring hexagons
     * */
    @Test
    fun generateNeighborHexagons() {
        fun findNeighborHexagons(ownCornerId: Int): List<Vector2i> {
            // given a hex, find the closest neighbor corner,
            // that is not our own corner
            val ownCorner = corners[ownCornerId]
            return neighbors.filter { neighbor ->
                val pos = getCenter(neighbor.x, neighbor.y, Vector2d())
                val dist = pos.distanceSquared(ownCorner)
                abs(dist - 1.0) < 1e-7
            }
        }

        println("val neighborHexagons = listOf(")
        for (i in 0 until 6) {
            val cs = findNeighborHexagons(i)
            assertEquals(2, cs.size)
            for (c in cs) println("  Vector2i(${c.x}, ${c.y}),")
        }
        println(")")
    }
}