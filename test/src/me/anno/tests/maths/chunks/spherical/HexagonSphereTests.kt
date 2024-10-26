package me.anno.tests.maths.chunks.spherical

import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTreeF
import me.anno.maths.Maths.posMod
import me.anno.maths.Maths.sq
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sqrt

class HexagonSphereTests {

    fun createSphereList(): List<HexagonSphere> {
        return listOf(
            HexagonSphere(0, 1),
            HexagonSphere(3, 1),
            HexagonSphere(7, 1),
            HexagonSphere(15, 3),
        )
    }

    class PointLookup : OctTreeF<Vector3f>(16) {
        override fun getPoint(data: Vector3f): Vector3f = data
        override fun createChild(): KdTree<Vector3f, Vector3f> = PointLookup()
    }

    fun checkDistances(set: HashSet<Vector3f>, minDistance: Float, maxDistance: Float, ignoredMin: Float) {
        val lookup = PointLookup()
        for (v in set) {
            lookup.add(v)
        }
        val min = Vector3f()
        val max = Vector3f()
        val minSq = sq(minDistance)
        val maxSq = sq(maxDistance)
        val ignoredSq = sq(ignoredMin)
        // var minTotal = Float.POSITIVE_INFINITY
        // var maxTotal = 0f
        for (v in set) {
            v.add(maxDistance, max)
            v.sub(maxDistance, min)
            var minDistanceSq = Float.POSITIVE_INFINITY
            var closestV: Vector3f = v
            lookup.query(min, max) { other ->
                if (other != v) {
                    val distanceSq = v.distanceSquared(other)
                    if (distanceSq in ignoredSq..minDistanceSq) {
                        minDistanceSq = distanceSq
                        closestV = other
                    }
                }
                false
            }
            assertTrue(minDistanceSq in minSq..maxSq) {
                val distance = sqrt(minDistanceSq)
                "|$v-$closestV| = $distance !in $minDistance .. $maxDistance"
            }
            // minTotal = min(minTotal, minDistanceSq)
            // maxTotal = max(maxTotal, minDistanceSq)
        }
        // println("Range: ${sqrt(minTotal)} .. ${sqrt(maxTotal)}")
    }

    @Test
    fun testHexagonPointSpacing() {
        for (sphere in createSphereList()) {
            // check that the vertices are normalized,
            // and that the distance between them is within a certain range,
            // so they are kind-of at least regularly spaced
            val centers = HashSet<Vector3f>()
            val corners = HashSet<Vector3f>()
            for (triIndex in 0 until HexagonSphere.TRIANGLE_COUNT) {
                for (chunkIndex1 in 0 until sphere.chunkCount) {
                    for (chunkIndex2 in 0 until sphere.chunkCount - chunkIndex1) {
                        val chunk = sphere.chunk(triIndex, chunkIndex1, chunkIndex2)
                        for (hexagon in sphere.queryChunk(chunk)) {
                            centers.add(hexagon.center)
                            corners.addAll(hexagon.corners)
                        }
                    }
                }
            }
            val minDistanceSq = 0.9999996f
            val maxDistanceSq = 1.0000004f
            for (v in centers) {
                assertTrue(v.lengthSquared() in minDistanceSq..maxDistanceSq)
            }
            for (v in corners) {
                assertTrue(v.lengthSquared() in minDistanceSq..maxDistanceSq)
            }
            assertEquals(sphere.numHexagons, centers.size.toLong())
            println("${sphere.hexagonsPerSide} -> ${sphere.len}")
            val min0 = sphere.len * 0.660f // quite a big range...
            val max0 = sphere.len * 1.000f
            checkDistances(centers, min0, max0, 0f)
            val min1 = sphere.len * 0.389f // quite a big range...
            val max1 = sphere.len * 0.577f
            checkDistances(corners, min1, max1, min0 * 0.01f)
        }
    }

    @Test
    fun testHexagonNeighborHexagons() {
        for (sphere in createSphereList()) {
            for (triIndex in 0 until HexagonSphere.TRIANGLE_COUNT) {
                for (chunkIndex1 in 0 until sphere.chunkCount) {
                    for (chunkIndex2 in 0 until sphere.chunkCount - chunkIndex1) {
                        val chunk = sphere.chunk(triIndex, chunkIndex1, chunkIndex2)
                        for (hexagon in sphere.queryChunk(chunk)) {
                            val corners = hexagon.corners
                            assertTrue(corners.size == 5 || corners.size == 6)
                            for (ni in corners.indices) {
                                val n0 = corners[ni]
                                val n1 = corners[posMod(ni + 1, corners.size)]
                                val approxCenter = n0.mix(n1, 0.5f, Vector3f()).mix(hexagon.center, -1f)
                                approxCenter.normalize()
                                val neighbor = sphere.findClosestHexagon(approxCenter)
                                assertNotEquals(hexagon.center, neighbor.center)
                                // validate that n0 and n1 are present on neighbor
                                val minDistanceSq = sq(sphere.len * 0.01f)
                                fun checkHasVertex(n0: Vector3f) {
                                    val count = neighbor.corners.count2 { n0.distanceSquared(it) < minDistanceSq }
                                    assertEquals(1, count)
                                }
                                checkHasVertex(n0)
                                checkHasVertex(n1)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testHexagonCornerSpacing() {
        val tmp0 = Vector3f()
        val tmp1 = Vector3f()
        for (sphere in createSphereList()) {
            val minDistanceSq0 = sq(sphere.len * 0.390f)
            val maxDistanceSq0 = sq(sphere.len * 0.577f)
            val minDistanceSq1 = sq(sphere.len * 0.389f)
            val maxDistanceSq1 = sq(sphere.len * 0.582f)
            // angle from one corner to the next should be roughly 60°
            // in my testing, I got these border-values: 51°-75°
            val minAngleCos = cos(75f.toRadians())
            val maxAngleCos = cos(51f.toRadians())
            for (triIndex in 0 until HexagonSphere.TRIANGLE_COUNT) {
                for (chunkIndex1 in 0 until sphere.chunkCount) {
                    for (chunkIndex2 in 0 until sphere.chunkCount - chunkIndex1) {
                        val chunk = sphere.chunk(triIndex, chunkIndex1, chunkIndex2)
                        for (hexagon in sphere.queryChunk(chunk)) {
                            val corners = hexagon.corners
                            assertTrue(corners.size == 5 || corners.size == 6)
                            for (ni in corners.indices) {
                                val c0 = corners[ni]
                                // check that the distance to the center is reasonable
                                val distanceSq0 = c0.distanceSquared(hexagon.center)
                                assertTrue(distanceSq0 in minDistanceSq0..maxDistanceSq0)
                                // check that the distance to the next neighbor is reasonable
                                val c1 = corners[posMod(ni + 1, corners.size)]
                                val distanceSq1 = c0.distanceSquared(c1)
                                assertTrue(distanceSq1 in minDistanceSq1..maxDistanceSq1)
                                // check that the angle to the next neighbor is reasonable
                                val c2 = corners[posMod(ni + 2, corners.size)]
                                val angleCos = c1.sub(c0, tmp0).angleCos(c2.sub(c1, tmp1))
                                assertTrue(angleCos in minAngleCos..maxAngleCos)
                            }
                        }
                    }
                }
            }
        }
    }
}