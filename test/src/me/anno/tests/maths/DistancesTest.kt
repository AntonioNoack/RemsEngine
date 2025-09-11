package me.anno.tests.maths

import me.anno.maths.Permutations.generatePermutations
import me.anno.maths.geometry.Distances.lineLineDistance
import me.anno.maths.geometry.Distances.linePointClosest
import me.anno.maths.geometry.Distances.linePointDistance
import me.anno.maths.geometry.Distances.pointPointDistance
import me.anno.maths.geometry.Distances.rayLineDistance
import me.anno.maths.geometry.Distances.rayPointClosest
import me.anno.maths.geometry.Distances.rayPointDistance
import me.anno.maths.geometry.Distances.rayRayClosestPoint
import me.anno.maths.geometry.Distances.rayRayClosestPoints
import me.anno.maths.geometry.Distances.rayRayDistance
import me.anno.maths.geometry.Distances.raySegmentDistance
import me.anno.maths.geometry.Distances.segmentPointClosest
import me.anno.maths.geometry.Distances.segmentSegmentDistance
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class DistancesTest {
    companion object {
        fun testAllAxes3d(
            runnableD: (List<Vector3d>) -> Unit,
            runnableF: (List<Vector3f>) -> Unit,
            vararg vectors: Vector3d,
        ) {
            // iterate over all permutations and signs
            val clonesD = vectors.map { Vector3d() }
            val clonesF = vectors.map { Vector3f() }
            val signs = DoubleArray(3)
            generatePermutations(listOf(0, 1, 2)) { order ->
                for (signI in 0 until 8) {
                    signs[0] = if (signI.hasFlag(1)) 1.0 else -1.0
                    signs[1] = if (signI.hasFlag(2)) 1.0 else -1.0
                    signs[2] = if (signI.hasFlag(4)) 1.0 else -1.0
                    for (i in vectors.indices) {
                        val srcV = vectors[i]
                        val dstD = clonesD[i]
                        val dstF = clonesF[i]
                        for ((srcI, dstI) in order.withIndex()) {
                            val value = signs[srcI] * srcV[srcI]
                            dstD[dstI] = value
                            dstF[dstI] = value.toFloat()
                        }
                    }
                    runnableD(clonesD)
                    runnableF(clonesF)
                }
            }
        }

        fun testAllAxes3f(
            vararg vectors: Vector3f,
            runnable: (List<Vector3f>) -> Unit,
        ) {
            // iterate over all permutations and signs
            val clones = vectors.map { Vector3f() }
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
    }

    @Test
    fun testPointPointDistance() {
        assertEquals(sqrt(3.0), pointPointDistance(Vector3d(2.0), Vector3d(1.0)))
        testAllAxes3d(
            { (a, b) ->
                assertEquals(sqrt(81.0 + 25.0 + 9.0), pointPointDistance(a, b))
            },
            { (a, b) ->
                assertEquals(sqrt(81f + 25f + 9f), pointPointDistance(a, b))
            },
            Vector3d(10.0, 7.0, 0.0),
            Vector3d(1.0, 2.0, 3.0),
        )
    }

    @Test
    fun testPointLineDistance() {
        testAllAxes3d(
            { (a, b, pt) ->
                assertEquals(sqrt(2.0), linePointDistance(a, b, pt))
            },
            { (a, b, pt) ->
                assertEquals(sqrt(2f), linePointDistance(a, b, pt))
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 0.0),
        )
        testAllAxes3d(
            { (a, b, pt) ->
                assertEquals(1.0, linePointDistance(a, b, pt))
            }, { (a, b, pt) ->
                assertEquals(1f, linePointDistance(a, b, pt))
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 1.0)
        )
    }

    @Test
    fun testPointLineClosest() {
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3f()), 1e-16)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3f()), 1e-7)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.2, 1.0, 1.0),
            Vector3d(+0.2, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3d()), 1e-15)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3f()), 1e-15)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+1.2, 1.0, 1.0),
            Vector3d(+1.2, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3d()), 1e-15)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3f()), 1e-15)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(-5.7, 1.0, 1.0),
            Vector3d(-5.7, 0.0, 1.0)
        )
    }

    @Test
    fun testPointSegmentClosest() {
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, linePointClosest(a, b, pt, Vector3f()), 1e-7)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3f()), 1e-7)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+0.2, 1.0, 1.0),
            Vector3d(+0.2, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3f()), 1e-7)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(+1.2, 1.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0)
        )
        testAllAxes3d(
            { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3d()), 1e-16)
            }, { (a, b, pt, closest) ->
                assertEquals(closest, segmentPointClosest(a, b, pt, Vector3f()), 1e-16)
            },
            Vector3d(-1.0, 0.0, 1.0),
            Vector3d(+1.0, 0.0, 1.0),
            Vector3d(-5.7, 1.0, 1.0),
            Vector3d(-1.0, 0.0, 1.0)
        )
    }

    @Test
    fun testLineLineDistance() {
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                assertEquals(0.0, lineLineDistance(a0, b0, a1, b1))
            },
            { (a0, b0, a1, b1) ->
                assertEquals(0f, lineLineDistance(a0, b0, a1, b1))
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 2.0, 0.0),
        )
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                assertEquals(1.0, lineLineDistance(a0, b0, a1, b1))
            },
            { (a0, b0, a1, b1) ->
                assertEquals(1f, lineLineDistance(a0, b0, a1, b1))
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 1.0),
            Vector3d(0.0, 2.0, 1.0),
        )
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                assertEquals(7.0, lineLineDistance(a0, b0, a1, b1))
            },
            { (a0, b0, a1, b1) ->
                assertEquals(7f, lineLineDistance(a0, b0, a1, b1))
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 7.0),
            Vector3d(0.0, 2.0, 7.0),
        )
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                assertEquals(sqrt(2.0), lineLineDistance(a0, b0, a1, b1), 1e-15)
            },
            { (a0, b0, a1, b1) ->
                assertEquals(sqrt(2f), lineLineDistance(a0, b0, a1, b1), 1e-7f)
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(2.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 1.0),
            Vector3d(0.0, 2.0, 0.0),
        )
        // parallel
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                assertEquals(sqrt(10.0), lineLineDistance(a0, b0, a1, b1), 1e-15)
            },
            { (a0, b0, a1, b1) ->
                assertEquals(sqrt(10f), lineLineDistance(a0, b0, a1, b1), 1e-7f)
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(2.0, 0.0, 0.0),
            Vector3d(5.0, 3.0, 1.0),
            Vector3d(6.0, 3.0, 1.0),
        )
    }

    @Test
    fun testRayRayClosest() {
        testAllAxes3d(
            { list ->
                val (pso0, dir0, pos1, dir1, expected0) = list
                val expected1 = list[5]

                val dst0 = Vector3d()
                val dst1 = Vector3d()
                rayRayClosestPoints(pso0, dir0, pos1, dir1, dst0, dst1)
                assertEquals(expected0, dst0)
                assertEquals(expected1, dst1)
            },
            { list ->
                val (pso0, dir0, pos1, dir1, expected0) = list
                val expected1 = list[5]

                val dst0 = Vector3f()
                val dst1 = Vector3f()
                rayRayClosestPoints(pso0, dir0, pos1, dir1, dst0, dst1)
                assertEquals(expected0, dst0)
                assertEquals(expected1, dst1)
            },
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
        )
    }

    @Test
    fun testRayPointClosestAndDistance() {
        testAllAxes3d(
            { (pos, dir, pt) ->
                val dst = Vector3d()
                val closest = rayPointClosest(pos, dir, pt, dst)
                assertEquals(closest, dst)

                // Ray along +X from origin, point at (0,1,0) -> closest (0,0,0), distance 1
                assertEquals(
                    Vector3d(0.0, 0.0, 0.0),
                    rayPointClosest(Vector3d(), Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d())
                )
                assertEquals(1.0, rayPointDistance(Vector3d(), Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0)))
            },
            { (pos, dir, pt) ->
                val dst = Vector3f()
                val closest = rayPointClosest(Vector3f(pos), Vector3f(dir), Vector3f(pt), dst)
                assertEquals(closest, dst)

                assertEquals(
                    Vector3f(0f, 0f, 0f),
                    rayPointClosest(Vector3f(), Vector3f(1f, 0f, 0f), Vector3f(0f, 1f, 0f), Vector3f())
                )
                assertEquals(1f, rayPointDistance(Vector3f(), Vector3f(1f, 0f, 0f), Vector3f(0f, 1f, 0f)))
            },
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
        )
    }

    @Test
    fun testRayLineDistance() {
        testAllAxes3d(
            { (pos, dir, a, b) ->
                // Ray: X+, Line: Z+, offset by Y=1 => distance 1
                assertEquals(1.0, rayLineDistance(pos, dir, a, b))
            },
            { (pos, dir, a, b) ->
                assertEquals(1f, rayLineDistance(pos, dir, a, b))
            },
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 1.0, 1.0),
        )
        testAllAxes3d(
            { (pos, dir, a, b) ->
                // Ray at origin along X, segment parallel in plane Y=1
                assertEquals(1.0, rayLineDistance(pos, dir, a, b))
            },
            { (pos, dir, a, b) ->
                assertEquals(1f, rayLineDistance(pos, dir, a, b))
            },
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(1.0, 1.0, 0.0),
            Vector3d(2.0, 1.0, 0.0),
        )
    }

    @Test
    fun testRaySegmentDistance() {
        testAllAxes3d(
            { (pos, dir, a, b) ->
                // Ray at origin along X, segment parallel in plane Y=1
                assertEquals(sqrt(2.0), raySegmentDistance(pos, dir, a, b))
            },
            { (pos, dir, a, b) ->
                assertEquals(sqrt(2f), raySegmentDistance(pos, dir, a, b))
            },
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(1.0, 1.0, 0.0),
            Vector3d(2.0, 1.0, 0.0),
        )
    }

    @Test
    fun testSegmentSegmentDistance() {
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                // Two parallel unit segments, offset by 1 in Y
                assertEquals(1.0, segmentSegmentDistance(a0, b0, a1, b1))
            },
            { (a0, b0, a1, b1) ->
                assertEquals(1f, segmentSegmentDistance(a0, b0, a1, b1))
            },
            Vector3d(0.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
            Vector3d(1.0, 1.0, 0.0),
        )
    }

    @Test
    fun testSegmentSegmentDistanceCrossing() {
        testAllAxes3d(
            { (a0, b0, a1, b1) ->
                // Perpendicular segments crossing at (0,0,0), so distance 0
                assertEquals(0.0, segmentSegmentDistance(a0, b0, a1, b1), 1e-15)
            },
            { (a0, b0, a1, b1) ->
                assertEquals(0f, segmentSegmentDistance(a0, b0, a1, b1), 1e-7f)
            },
            Vector3d(-1.0, 0.0, 0.0),
            Vector3d(1.0, 0.0, 0.0),
            Vector3d(0.0, -1.0, 0.0),
            Vector3d(0.0, 1.0, 0.0),
        )
    }

    @Test
    fun testRayRayClosestPointAndDistance() {
        // Case 1: Parallel rays offset by 1 in Y
        testAllAxes3d(
            { (pos0, dir0, pos1, dir1, exp) ->
                val closest0 = rayRayClosestPoint(pos0, dir0, pos1, dir1, Vector3d())
                assertEquals(exp, closest0) // ray0 is at origin along +X
                assertEquals(1.0, rayRayDistance(pos0, dir0, pos1, dir1)) // distance = 1 (Y offset)
            },
            { (pos0, dir0, pos1, dir1, exp) ->
                val closest0 = rayRayClosestPoint(pos0, dir0, pos1, dir1, Vector3f())
                assertEquals(exp, closest0)
                assertEquals(1f, rayRayDistance(pos0, dir0, pos1, dir1))
            },
            Vector3d(0.0, 0.0, 0.0), // pos0
            Vector3d(1.0, 0.0, 0.0), // dir0
            Vector3d(0.0, 1.0, 0.0), // pos1
            Vector3d(1.0, 0.0, 0.0), // dir1
            Vector3d(0.0, 0.5, 0.0), // expected center
        )

        // Case 2: Rays intersecting at (1,0,0)
        testAllAxes3d(
            { (pos0, dir0, pos1, dir1, expected) ->
                val closest0 = rayRayClosestPoint(pos0, dir0, pos1, dir1, Vector3d())
                assertEquals(expected, closest0, 1e-15)
                assertEquals(0.0, rayRayDistance(pos0, dir0, pos1, dir1), 1e-15)
            },
            { (pos0, dir0, pos1, dir1, expected) ->
                val closest0 = rayRayClosestPoint(pos0, dir0, pos1, dir1, Vector3f())
                assertEquals(expected, closest0, 1e-7)
                assertEquals(0f, rayRayDistance(pos0, dir0, pos1, dir1), 1e-7f)
            },
            Vector3d(0.0, 0.0, 0.0), // pos0
            Vector3d(1.0, +0.0, 0.0), // dir0
            Vector3d(1.0, -1.0, 0.0), // pos1
            Vector3d(0.0, +1.0, 0.0), // dir1
            Vector3d(1.0, +0.0, 0.0), // expected
        )

        // Case 3: Skew rays (shortest distance is sqrt(2))
        testAllAxes3d(
            { (pos0, dir0, pos1, dir1) ->
                val d = rayRayDistance(pos0, dir0, pos1, dir1)
                assertEquals(sqrt(2.0), d, 1e-15)
            },
            { (pos0, dir0, pos1, dir1) ->
                val d = rayRayDistance(Vector3f(pos0), Vector3f(dir0), Vector3f(pos1), Vector3f(dir1))
                assertEquals(sqrt(2f), d, 1e-7f)
            },
            Vector3d(0.0, 0.0, 0.0), // pos0
            Vector3d(1.0, 0.0, 0.0), // dir0 (x-axis)
            Vector3d(0.0, 1.0, 1.0), // pos1
            Vector3d(0.0, 1.0, 0.0), // dir1 (y-axis offset in z=1)
        )
    }
}