package me.anno.tests.mesh.hexagons

import me.anno.maths.Maths.PIf
import me.anno.maths.chunks.spherical.HexagonSphere
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class QueryChunksTest {

    @Test
    fun main() {
        testQueryChunks(20000)
        testQueryChunks(40000)
    }

    fun testQueryChunks(n: Int) {
        println("Running --- $n ---")
        val sphere = HexagonSphere(n, n / 25)
        val dir = Vector3f(0f, 1f, 0f)
        val maxAngleDifference = PIf * 5f / n
        val result = ArrayList<HexagonSphere.Chunk>()
        sphere.queryChunks(dir, maxAngleDifference) { sc ->
            // println("  [${result.size}: $sc]")
            result.add(sc)
            false
        }
        println("total: ${result.size}, tris: ${result.map { it.tri }.toHashSet().sorted()}")
    }
}