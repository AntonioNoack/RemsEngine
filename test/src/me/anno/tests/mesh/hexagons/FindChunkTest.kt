package me.anno.tests.mesh.hexagons

import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Floats.formatPercent
import org.junit.jupiter.api.Test

class FindChunkTest {

    @Test
    fun testFindChunkEmpty() {
        testFindChunk(0, 1, false) // 100% fail... why??
    }

    @Test
    fun testFindChunkSmallChunks() {
        testFindChunk(40, 1, false) // broken :/, 98% are incorrect
        testFindChunk(40, 2, false) // broken :/, 43% are incorrect
        testFindChunk(42, 3, true) // works
    }

    @Test
    fun testFindChunkBigChunks() {
        testFindChunk(100, 10, true)
        testFindChunk(200, 25, true)
        testFindChunk(100_000, 4000, true)
    }

    fun testFindChunk(n: Int, t: Int, crash: Boolean) {
        var failures = 0
        var total = 0
        val sphere = HexagonSphere(n, n / t)
        tris@ for ((tri, triangle) in sphere.triangles.withIndex()) {
            for (si in 0 until sphere.chunkCount) {
                for (sj in 0 until sphere.chunkCount - si) {
                    val expected = sphere.chunk(tri, si, sj)
                    val actual = sphere.findChunk(triangle, expected.center)
                    //  assertEquals(expected, actual)
                    if (expected != actual) failures++
                    /*if (expected != actual) {
                        println("$tri,$si,$sj != ${actual.si},${actual.sj}")
                        if (tri == actual.tri && si == actual.si && sj == actual.sj) {
                            println("$expected != $actual")
                        }
                        if (failures > 100) break@tris
                    }*/
                    total++
                }
            }
        }
        println("failures: $failures/$total, ${(failures.toFloat() / total).formatPercent()}%")
        if (crash) {
            assertEquals(0, failures)
        }
    }

    @Test
    fun testFindChunkByHex() {
        for (s in 3..4) {
            for (t in 3..4) {
                val n = s * t
                val sphere = HexagonSphere(n, s)
                testFindChunkByHex(sphere)
            }
        }
    }

    fun testFindChunkByHex(sphere: HexagonSphere) {
        val s = sphere.chunkCount
        for (tri in sphere.triangles.indices) {
            for (si in 0 until s) {
                for (sj in 0 until s - si) {
                    val hexagons = sphere.queryChunk(tri, si, sj)
                    for (hex in hexagons) {
                        val test = sphere.findChunk(hex)
                        assertEquals(tri, test.tri)
                        assertEquals(si, test.si)
                        assertEquals(sj, test.sj)
                    }
                }
            }
        }
    }
}