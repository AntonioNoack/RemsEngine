package me.anno.tests.engine

import me.anno.engine.raycast.BlockTracing
import me.anno.engine.raycast.RayQuery
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.AABBi
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.test.assertEquals

// todo test a more complex world
class BlockTracingTest {

    val xs = doubleArrayOf(-1.5, -1.0, -0.001, 0.01, 0.3, 0.7, 0.999, 1.001, 1.3, 1.5)

    @Test
    fun testZeroSizedWorld() {
        for (y in xs) {
            for (x in xs) {
                val query = RayQuery(
                    Vector3d(x, y, -1.0),
                    Vector3d(0.0, 0.0, 1.0),
                    1e3
                )
                BlockTracing.blockTrace(query, 10, AABBi()) { _, _, _ ->
                    assertFail("Must not evaluate blocks that don't exist")
                }
                assertEquals(1e3, query.result.distance)
            }
        }
    }

    @Test
    fun testEmptyRandomWorld() {
        val size = 5
        val random = Random(1234)
        for (y in xs) {
            for (x in xs) {
                val query = RayQuery(
                    Vector3d(x, y, -1.0),
                    Vector3d(0.0, 0.0, 1.0),
                    1e3
                )
                val hit = BlockTracing.blockTrace(
                    query, 10,
                    AABBi(0, 0, 0, size, size, size)
                ) { xi, yi, zi ->
                    assertTrue(xi in 0..size)
                    assertTrue(yi in 0..size)
                    assertTrue(zi in 0..size)
                    if (random.nextBoolean()) BlockTracing.AIR_SKIP_NORMAL
                    else BlockTracing.AIR_BLOCK
                }
                assertFalse(hit)
                assertEquals(1e3, query.result.distance)
            }
        }
    }

    @Test
    fun testSingleBlockWorld() {
        for (y in xs) {
            for (x in xs) {
                val query = RayQuery(
                    Vector3d(x, y, -1.0),
                    Vector3d(0.0, 0.0, 1.0),
                    1e3
                )
                val hits = x in 0.0..1.0 && y in 0.0..1.0
                val hit = BlockTracing.blockTrace(
                    query, 10,
                    AABBi(0, 0, 0, 0, 0, 0)
                ) { xi, yi, zi ->
                    assertEquals(0, xi)
                    assertEquals(0, yi)
                    assertEquals(0, zi)
                    -1.0
                }
                assertEquals(hits, hit)
                if (hits) {
                    assertEquals(1.0, query.result.distance, 0.001)
                    assertTrue(query.result.positionWS.equals(Vector3d(x, y, 0.0), 0.001))
                } else {
                    assertEquals(1e3, query.result.distance)
                }
            }
        }
    }
}