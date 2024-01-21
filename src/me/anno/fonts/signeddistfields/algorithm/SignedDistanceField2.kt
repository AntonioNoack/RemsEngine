package me.anno.fonts.signeddistfields.algorithm

import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.ByteBufferPool
import org.joml.AABBf
import org.joml.Vector2f
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class SignedDistanceField2(contours: List<Contour>, roundEdges: Boolean, sdfResolution: Float, padding: Float) {

    val bounds = AABBf()

    init {
        for (contour in contours) {
            bounds.union(contour.calculateBounds())
        }
    }

    val minX = floor(bounds.minX - padding)
    val maxX = ceil(bounds.maxX + padding)
    val minY = floor(bounds.minY - padding)
    val maxY = ceil(bounds.maxY + padding)

    val w = ((maxX - minX) * sdfResolution).toInt()
    val h = ((maxY - minY) * sdfResolution).toInt()

    val distances = if (w >= 1 && h >= 1) {
        calculateDistances(contours, roundEdges)
    } else null

    private fun calculateDistances(
        contours: List<Contour>,
        roundEdges: Boolean
    ): FloatBuffer {

        val buffer = ByteBufferPool
            .allocateDirect(w * h * 4)
            .asFloatBuffer()

        val maxDistance = max(maxX - minX, maxY - minY)
        val pointBounds = AABBf()
        val minDistance = SignedDistance()
        val tmpDistance = SignedDistance()
        val origin = Vector2f()
        val ptr = FloatPtr()
        val tmpArray = FloatArray(3)
        val tmpParam = FloatPtr()

        val invH = 1f / (h - 1f)
        val invW = 1f / (w - 1f)

        val offset = 0.5f // such that the shader can be the same even if the system only supports normal textures
        for (y in 0 until h) {

            val ry = y * invH
            val ly = mix(maxY, minY, ry) // mirrored y for OpenGL
            var index = y * w
            for (x in 0 until w) {

                val rx = x * invW
                val lx = mix(minX, maxX, rx)

                origin.set(lx, ly)
                minDistance.clear()

                var closestEdge: EdgeSegment? = null

                pointBounds.setMin(lx - maxDistance, ly - maxDistance, -1f)
                pointBounds.setMax(lx + maxDistance, ly + maxDistance, +1f)

                for (ci in contours.indices) {
                    val contour = contours[ci]
                    if (contour.bounds.testAABB(pointBounds)) {// this test brings down the complexity from O(chars²) to O(chars)
                        val edges = contour.segments
                        for (edgeIndex in edges.indices) {
                            val edge = edges[edgeIndex]
                            val distance = edge.signedDistance(origin, ptr, tmpArray, tmpDistance)
                            if (distance < minDistance) {
                                minDistance.set(distance)
                                closestEdge = edge
                            }
                        }
                    }
                }

                val trueDistance = if (closestEdge != null) {
                    if (roundEdges) {
                        minDistance.distance
                    } else closestEdge.trueSignedDistance(origin, tmpParam, tmpArray, tmpDistance)
                } else 100f

                val dist = clamp(trueDistance, -maxDistance, +maxDistance)
                buffer.put(index, dist * SignedDistanceField.sdfResolution + offset)
                index++
            }
        }

        buffer.position(0)

        return buffer
    }
}