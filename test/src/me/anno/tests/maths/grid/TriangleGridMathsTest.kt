package me.anno.tests.maths.grid

import me.anno.maths.Maths.TAU
import me.anno.maths.chunks.triangles.TriangleGridMaths.coordsToIndex
import me.anno.maths.chunks.triangles.TriangleGridMaths.getNeighbors
import me.anno.maths.chunks.triangles.TriangleGridMaths.getVertices
import me.anno.maths.chunks.triangles.TriangleGridMaths.indexToCoords
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2d
import org.joml.Vector2i
import org.junit.jupiter.api.Test
import kotlin.math.atan2

class TriangleGridMathsTest {

    @Test
    fun transformInvertible() {
        val positions = Vector2d()
        val index = Vector2i()
        val remainder = Vector2d()
        for (j in -5..5) {
            for (i in -5..5) {
                assertSame(indexToCoords(index.set(i, j), positions), positions)
                assertSame(coordsToIndex(positions, index, remainder, false), index)
                assertEquals(0.0, remainder.x, 1e-15)
                assertEquals(0.0, remainder.y, 1e-15)
                assertEquals(i, index.x)
                assertEquals(j, index.y)
                assertSame(coordsToIndex(positions, index, remainder, true), index)
                assertEquals(0.0, remainder.x, 1e-15)
                assertEquals(0.0, remainder.y, 1e-15)
                assertEquals(i, index.x)
                assertEquals(j, index.y)
            }
        }
    }

    @Test
    fun transformInvertible2() {
        val positions = Vector2d()
        val index = Vector2i()
        val remainder = Vector2d()
        val s = 10
        val scale = 1.0 / s
        for (j in -s..s) {
            for (i in -s..s) {
                val x = i * scale
                val y = j * scale
                positions.set(x, y)
                assertSame(coordsToIndex(positions, index, remainder, false), index)
                positions.set(0.0) // delete value just in case ^^
                assertSame(indexToCoords(index, positions), positions)
                positions.add(remainder)
                assertEquals(x, positions.x, 1e-15)
                assertEquals(y, positions.y, 1e-15)
            }
        }
    }

    @Test
    fun neighborsAreSortedByAngle() {
        val neighborPosition = Vector2d()
        val neighborVertex = Vector2d()
        val ownCenter = Vector2d()
        val ownIndex = Vector2i()
        for (direct in listOf(true, false)) {
            val len = if (direct) 3 else 12
            val minCommonVertices = if (direct) 2 else 1
            for (down in listOf(false, true)) {
                ownIndex.set(down.toInt(), 0)
                indexToCoords(ownIndex, ownCenter)
                val ownVertices = getVertices(down).map {
                    it.add(ownCenter, Vector2d())
                }
                val neighbors = getNeighbors(direct, down)
                // println("checking vertex $ownIndex -> $ownCenter / ${ownVertices.joinToString()}")
                for (neighbor in neighbors) {
                    // check numCommonVertices vertices are common
                    val nx = neighbor.x + ownIndex.x
                    val ny = neighbor.y + ownIndex.y
                    val neighborDown = nx.hasFlag(1)
                    indexToCoords(nx, ny, neighborPosition)
                    // println("  checking neighbor $neighbor -> ($nx,$ny) -> $neighborPosition")
                    val neighborVertices = getVertices(neighborDown)
                    var ctr = 0
                    for (v in neighborVertices) {
                        neighborPosition.add(v, neighborVertex)
                        // println("    $neighborVertex?")
                        ctr += (neighborVertex in ownVertices).toInt()
                    }
                    assertTrue(minCommonVertices <= ctr)
                    assertNotEquals(3, ctr)
                }
                assertEquals(len, neighbors.size)
                val positions = neighbors.map { indexToCoords(it, Vector2d()) }
                val angles = positions.map { (atan2(it.y, it.x) + TAU) % TAU }
                assertEquals(neighbors.size, neighbors.distinct().size) // no duplicates
                assertContentEquals(angles.sorted(), angles)
            }
        }
    }
}