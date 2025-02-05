package me.anno.fonts.signeddistfields.algorithm

import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.sdfResolution
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.utils.hpc.ProcessingGroup
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class SignedDistanceField2(contours: List<Contour>, val roundEdges: Boolean, sdfResolution: Float, padding: Float) {

    companion object {
        private val pool = ProcessingGroup("SDF", 16)
    }

    val bounds = calculateBounds(contours)

    val minX = floor(bounds.minX - padding)
    val maxX = ceil(bounds.maxX + padding)
    val minY = floor(bounds.minY - padding)
    val maxY = ceil(bounds.maxY + padding)

    val w = ((maxX - minX) * sdfResolution).toInt()
    val h = ((maxY - minY) * sdfResolution).toInt()

    val maxDistance = max(maxX - minX, maxY - minY) * 0.5f

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

    val distances = if (w >= 1 && h >= 1) {
        calculateDistances(contours)
    } else null

    private fun calculateBounds(contours: List<Contour>): AABBf {
        val bounds = AABBf()
        val tmp = FloatArray(2)
        for (contour in contours) {
            bounds.union(contour.calculateBounds(tmp))
        }
        return bounds
    }

    private fun getLx(x: Int): Float {
        return mix(minX, maxX, x * invW)
    }

    private fun getLy(y: Int): Float {
        return mix(maxY, minY, y * invH) // mirrored y for OpenGL
    }

    private fun calculateDistances(contours: List<Contour>): FloatArray {
        val buffer = FloatArray(w * h)
        val offset = 0.5f // such that the shader can be the same even if the system only supports normal textures
        if (false) {
            // 4x speedup using 16 threads :/, and not thread-safe yet
            pool.processBalanced2d(0, 0, w, h, 8, 4) { x0, y0, x1, y1 ->
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val lx = getLx(x)
                        val ly = getLy(y)
                        buffer[x + y * w] = calculateDistance(lx, ly, contours) * sdfResolution + offset
                    }
                }
            }
        } else {
            for (y in 0 until h) {
                val ly = getLy(y)
                for (x in 0 until w) {
                    val lx = getLx(x)
                    buffer[x + y * w] = calculateDistance(lx, ly, contours) * sdfResolution + offset
                }
            }
        }
        return buffer
    }

    private fun calculateDistance(lx: Float, ly: Float, contours: List<Contour>): Float {
        origin.set(lx, ly)
        minDistance.clear()

        var closestEdge: EdgeSegment? = null

        pointBounds.setMin(lx - maxDistance, ly - maxDistance, -1f)
        pointBounds.setMax(lx + maxDistance, ly + maxDistance, +1f)

        for (ci in contours.indices) {
            val contour = contours[ci]
            // this test brings down the complexity from O(chars * letters) to O(chars + letters)
            if (contour.bounds.testAABB(pointBounds)) {
                val edges = contour.segments
                for (edgeIndex in edges.indices) {
                    val edge = edges[edgeIndex]
                    val distance = edge.getSignedDistance(origin, ptr, tmpArray, tmpDistance)
                    if (distance < minDistance) {
                        minDistance.set(distance)
                        closestEdge = edge
                    }
                }
            }
        }

        return if (closestEdge != null) {
            val distance =
                if (roundEdges) minDistance.distance
                else closestEdge.getTrueSignedDistance(origin, tmpParam, tmpArray, tmpDistance)
            clamp(distance, -maxDistance, +maxDistance)
        } else maxDistance
    }
}