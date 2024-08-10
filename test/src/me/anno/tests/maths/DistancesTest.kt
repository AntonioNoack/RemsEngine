package me.anno.tests.maths

import me.anno.maths.Permutations.generatePermutations
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import me.anno.maths.geometry.Distances.lineLineDistance
import me.anno.maths.geometry.Distances.linePointDistance
import me.anno.maths.geometry.Distances.pointPointDistance
import me.anno.maths.geometry.Distances.rayRayClosestPoints
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class DistancesTest {

    fun testAllAxes(vararg vectors: Vector3d, runnable: (List<Vector3d>) -> Unit) {
        // iterate over all permutations and signs
        val clones = vectors.map { Vector3d() }
        val signs = FloatArray(3)
        generatePermutations(listOf(0, 1, 2)) { order ->
            for (signI in 0 until 8) {
                signs[0] = if (signI.hasFlag(1)) 1f else -1f
                signs[1] = if (signI.hasFlag(2)) 1f else -1f
                signs[2] = if (signI.hasFlag(4)) 1f else -1f
                for (i in vectors.indices) {
                    val srcV = vectors[i]
                    val dstV = clones[i]
                    for ((srcI, dstI) in order.withIndex()) {
                        dstV[dstI] = signs[srcI] * srcV[srcI]
                    }
                }
                runnable(clones)
            }
        }
    }

    @Test
    fun testPointPointDistance() {
        assertEquals(sqrt(3.0), pointPointDistance(Vector3d(2.0), Vector3d(1.0)))
        testAllAxes(
            Vector3d(10.0, 7.0, 0.0),
            Vector3d(1.0, 2.0, 3.0)
        ) { (a, b) ->
            assertEquals(sqrt(81.0 + 25.0 + 9.0), pointPointDistance(a, b))
        }
    }

    @Test
    fun testPointLineDistance() {
        testAllAxes(
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 0.0)
        ) { (a, b, pt) ->
            assertEquals(sqrt(2.0), linePointDistance(a, b, pt))
        }
        testAllAxes(
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 1.0)
        ) { (a, b, pt) ->
            assertEquals(1.0, linePointDistance(a, b, pt))
        }
    }

    @Test
    fun testLineLineDistance() {
        testAllAxes(
            Vector3d(1.0, 0.0, 0.0), Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 2.0, 0.0),
        ) { (a0, b0, a1, b1) ->
            assertEquals(0.0, lineLineDistance(a0, b0, a1, b1))
        }
        testAllAxes(
            Vector3d(1.0, 0.0, 0.0), Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 1.0), Vector3d(0.0, 2.0, 1.0),
        ) { (a0, b0, a1, b1) ->
            assertEquals(1.0, lineLineDistance(a0, b0, a1, b1))
        }
        testAllAxes(
            Vector3d(1.0, 0.0, 0.0), Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 7.0), Vector3d(0.0, 2.0, 7.0),
        ) { (a0, b0, a1, b1) ->
            assertEquals(7.0, lineLineDistance(a0, b0, a1, b1))
        }
        testAllAxes(
            Vector3d(1.0, 0.0, 0.0), Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 1.0), Vector3d(0.0, 2.0, 0.0),
        ) { (a0, b0, a1, b1) ->
            assertEquals(sqrt(2.0), lineLineDistance(a0, b0, a1, b1))
        }
    }

    @Test
    fun testRayRayClosest() {
        assertEquals(
            Vector3d(1.0, 0.0, 0.0) to Vector3d(0.0, 1.0, 0.0),
            rayRayClosestPoints(
                Vector3d(1.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, 0.0), Vector3d(0.0, 1.0, 0.0)
            )
        )
    }
}