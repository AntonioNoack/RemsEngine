package me.anno.fonts.signeddistfields.algorithm

import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.MinMax.min
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.types.Floats.toIntOr
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class SignedDistanceField2(
    val contours: List<Contour>, val roundEdges: Boolean,
    val sdfResolution: Float, padding: Float,
    useSpaceSizeBits: Boolean
) {

    companion object {
        /**
         * such that the shader can be the same even if the system only supports normal textures
         * */
        val offset = 0.5f

        private val pool = ProcessingGroup("SDF", 16)

        private fun calculateSpreadingBits(wf: Float, hf: Float, sdfResolution: Float): Int {
            // ~ 63 -> 5 bits -> 3 -> 8x spread
            val size = (min(wf, hf) * sdfResolution).toIntOr()
            val numBits = 32 - size.countLeadingZeroBits()
            return numBits - 2
        }
    }

    val segments = contours.flatMap { it.segments }

    val bounds = calculateBounds().apply {
        minX = floor(minX - padding)
        minY = floor(minY - padding)
        maxX = ceil(maxX + padding)
        maxY = ceil(maxY + padding)
    }

    private val deltaX = bounds.deltaX
    private val deltaY = bounds.deltaY

    val spreadingBits =
        if (useSpaceSizeBits) calculateSpreadingBits(deltaX, deltaY, sdfResolution)
        else 0

    val w = validateSize(deltaX, spreadingBits)
    val h = validateSize(deltaY, spreadingBits)

    val maxDistance = max(deltaX, deltaY) * 0.5f

    private val invW = 1f / (w - 1f)
    private val invH = 1f / (h - 1f)

    // temporary data
    private val pointBounds = AABBf()
    private val minDistance = SignedDistance()
    private val tmpDistance = SignedDistance()
    private val origin = Vector2f()
    private val ptr = FloatPtr()
    private val tmpArray = FloatArray(3)
    private val tmpParam = FloatPtr()

    var distancesI: FloatArray? = null

    fun getDistances(): FloatArray? {
        if (w <= 0 || h <= 0) return null
        if (distancesI == null) distancesI = calculateDistances()
        return distancesI
    }

    private fun validateSize(bounds: Float, spaceSizeBits: Int): Int {
        val size = (bounds * sdfResolution).toIntOr()
        if (size <= 0) return 0
        val sparseSize = 1 shl spaceSizeBits
        val rem = size % sparseSize
        if (rem == 1) return size
        if (rem == 0) return size + 1
        return size + sparseSize + 1 - rem
    }

    private fun calculateBounds(): AABBf {
        val bounds = AABBf()
        val tmp = FloatArray(2)
        for (contour in contours) {
            bounds.union(contour.calculateBounds(tmp))
        }
        return bounds
    }

    private fun lx(x: Int): Float {
        return mix(bounds.minX, bounds.maxX, x * invW)
    }

    private fun ly(y: Int): Float {
        return mix(bounds.maxY, bounds.minY, y * invH) // mirrored y for OpenGL
    }

    private fun calculateDistances(): FloatArray {
        val distances = FloatArray(w * h)
        if (false) {
            calculateDistancesParallel(distances)
            return distances
        }
        if (spreadingBits <= 0) {
            calculateDistancesSerial(distances)
        } else {
            // ~3x speedup
            val edges = IntArray(w * h)
            edges.fill(-2)
            fillInSparseClosestEdges(distances, edges, 1 shl spreadingBits)
            for (i in spreadingBits - 1 downTo 0) {
                spreadSparseClosestEdges(edges, 1 shl i)
            }
            calculateDistancesFromClosestEdges(distances, edges)
        }
        return distances
    }

    private fun calculateDistancesSerial(distances: FloatArray) {
        for (y in 0 until h) {
            val ly = ly(y)
            for (x in 0 until w) {
                distances[getIndex(x, y)] = mapDistance(calculateDistance(lx(x), ly))
            }
        }
    }

    private fun getIndex(x: Int, y: Int): Int {
        return x + y * w
    }

    private fun fillInSparseClosestEdges(distances: FloatArray, edges: IntArray, n: Int) {
        for (y in 0 until h step n) {
            for (x in 0 until w step n) {
                val edge = findClosestEdgeId(lx(x), ly(y))
                val index = getIndex(x, y)
                distances[index] = minDistance.distance
                edges[index] = edge
            }
        }
    }

    private fun spreadSparseClosestEdges(edges: IntArray, n: Int) {
        val n2 = n * 2
        val maxLowX = w - 1 - n2
        val maxLowY = h - 1 - n2
        for (y in 0 until h step n) {
            for (x in 0 until w step n) {
                val index = getIndex(x, y)
                if (edges[index] > -1) continue // done already

                // skip edge, if all four corners are the same
                val lowX = min(x - (x % n2), maxLowX)
                val lowY = min(y - (y % n2), maxLowY)

                val edge0 = edges[getIndex(lowX, lowY)]
                edges[index] = if (
                    (edges[getIndex(lowX + n2, lowY)] == edge0) and
                    (edges[getIndex(lowX, lowY + n2)] == edge0) and
                    (edges[getIndex(lowX + n2, lowY + n2)] == edge0)
                ) edge0 else findClosestEdgeId(lx(x), ly(y))
            }
        }
    }

    private fun calculateDistancesFromClosestEdges(distances: FloatArray, edges: IntArray) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = getIndex(x, y)
                // load data
                if (roundEdges) minDistance.distance = distances[index]
                origin.set(lx(x), ly(y))
                // calculate and store mapped distance
                val distance = calculateDistance(edges[index])
                distances[index] = mapDistance(distance)
            }
        }
    }

    private fun calculateDistancesParallel(distances: FloatArray) {
        // only a 4x speedup using 16 threads :/, and not thread-safe yet
        pool.processBalanced2d(0, 0, w, h, 8, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    distances[getIndex(x, y)] = mapDistance(calculateDistance(lx(x), ly(y)))
                }
            }
        }
    }

    private fun mapDistance(distance: Float): Float {
        return distance * sdfResolution + offset
    }

    private fun prepareBounds(lx: Float, ly: Float) {
        pointBounds
            .setMin(lx - maxDistance, ly - maxDistance, -1f)
            .setMax(lx + maxDistance, ly + maxDistance, +1f)
    }

    private fun setOrigin(lx: Float, ly: Float) {
        origin.set(lx, ly)
    }

    private fun findClosestEdgeId(lx: Float, ly: Float): Int {
        minDistance.clear()

        setOrigin(lx, ly)
        prepareBounds(lx, ly)

        var edgeId = 0
        var bestEdgeId = -1
        for (ci in contours.indices) {
            val contour = contours[ci]
            // this test brings down the complexity from O(chars * letters) to O(chars + letters)
            if (contour.bounds.testAABB(pointBounds)) {
                val segments = contour.segments
                for (si in segments.indices) {
                    val segment = segments[si]
                    val distance = segment
                        .getSignedDistance(origin, ptr, tmpArray, tmpDistance)
                    if (distance < minDistance) {
                        minDistance.set(distance)
                        bestEdgeId = edgeId
                    }
                    edgeId++
                }
            } else edgeId += contour.segments.size
        }
        return bestEdgeId
    }

    private fun calculateDistance(segment: EdgeSegment): Float {
        val distance =
            if (roundEdges) segment.getSignedDistance(origin, ptr, tmpArray, tmpDistance).distance
            else segment.getTrueSignedDistance(origin, tmpParam, tmpArray, tmpDistance)
        return clamp(distance, -maxDistance, +maxDistance)
    }

    private fun calculateDistance(lx: Float, ly: Float): Float {
        return calculateDistance(findClosestEdgeId(lx, ly))
    }

    private fun calculateDistance(closestEdgeId: Int): Float {
        if (closestEdgeId < 0) return maxDistance
        val closestEdge = segments[closestEdgeId]
        return calculateDistance(closestEdge)
    }
}