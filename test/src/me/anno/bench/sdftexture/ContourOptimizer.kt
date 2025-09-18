package me.anno.bench.sdftexture

import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.MinMax.max
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.posMod
import me.anno.utils.assertions.assertEquals
import org.joml.Vector2f
import kotlin.math.abs

object ContourOptimizer {

    @JvmStatic
    fun optimizeContours(contours: List<Contour>, maxError: Float): List<Contour> {
        return contours.map { optimizeContour(it, maxError) }
    }

    @JvmStatic
    fun optimizeContour(contour: Contour, maxError: Float): Contour {
        val segments = ArrayList(contour.segments)
        optimizeContour(segments, maxError)
        return Contour(segments)
    }

    @JvmStatic
    private fun optimizeContour(segments: MutableList<EdgeSegment>, maxError: Float) {
        var i = segments.lastIndex
        while (i >= 0 && segments.size > 1) {
            val j = posMod(i + 1, segments.size)
            val a = segments[i]
            val b = segments[j]
            val ab = joinSegments(a, b, maxError)
            if (ab != null) {
                segments[i] = ab
                segments.removeAt(j)
                i = min(i, segments.lastIndex)
            } else i--
        }
    }

    @JvmStatic
    private fun joinSegments(a: EdgeSegment, b: EdgeSegment, maxError: Float): EdgeSegment? {
        if (a is LinearSegment && b is LinearSegment) {
            // todo also try quadratic segment as the result
            assertEquals(a.p1, b.p0)
            val ab = LinearSegment(a.p0, b.p1)
            if (calculateError(a, b, ab) < maxError) return ab
        } else if (a is QuadraticSegment && b is QuadraticSegment) {
            assertEquals(a.p2, b.p0)
            return optimizeError(a, b, maxError)
        }
        return null
    }

    @JvmStatic
    private fun optimizeError(a: QuadraticSegment, b: QuadraticSegment, maxError: Float): EdgeSegment? {
        val trueMiddle = Vector2f()

        val min0 = 0.2f
        val max0 = 1.8f

        var min = min0
        var max = max0

        var minErr = calculateError(a, b, min, trueMiddle)
        var maxErr = calculateError(a, b, min, trueMiddle)
        while (max > min + 1e-5f) {
            val middle = (min + max) * 0.5f
            val middleErr = calculateError(a, b, middle, trueMiddle)
            if (minErr < maxErr) {
                max = middle
                maxErr = middleErr
            } else {
                min = middle
                minErr = middleErr
            }
        }
        if (maxErr < minErr) {
            minErr = maxErr
            min = max
        }
        return if (minErr < maxError) {
            calculateSegment(a, b, min, trueMiddle)
        } else null
    }

    @JvmStatic
    private fun calculateSegment(
        a: QuadraticSegment, b: QuadraticSegment,
        trueMiddleT: Float, trueMiddle: Vector2f
    ): QuadraticSegment {
        if (trueMiddleT < 1f) a.getPointAt(trueMiddleT, trueMiddle)
        else b.getPointAt(trueMiddleT - 1f, trueMiddle)

        val middle = trueMiddle.mul(4f) // 2M-(A+B)/2
            .sub(a.p0).sub(b.p2).mul(0.5f)
        return QuadraticSegment(a.p0, middle, b.p2)
    }

    @JvmStatic
    private fun calculateError(
        a: QuadraticSegment, b: QuadraticSegment,
        trueMiddleT: Float, trueMiddle: Vector2f
    ): Float {

        if (trueMiddleT < 1f) a.getPointAt(trueMiddleT, trueMiddle)
        else b.getPointAt(trueMiddleT - 1f, trueMiddle)

        val middle = trueMiddle.mul(4f) // 2M-(A+B)/2
            .sub(a.p0).sub(b.p2).mul(0.5f)
        val ab = QuadraticSegment(a.p0, middle, b.p2)
        return calculateError(a, b, ab)
    }

    @JvmStatic
    private fun calculateError(a: EdgeSegment, b: EdgeSegment, ab: EdgeSegment): Float {
        val n = 21 // should be odd
        val actual = Vector2f()
        val tmp = FloatPtr()
        val tmp3 = FloatArray(3)
        val tmpDist = SignedDistance()
        var maxFoundError = 0f
        for (i in 1 until n) {
            val t = i.toFloat() / n
            ab.getPointAt(t, actual)
            val seg = if (i * 2 < n) a else b
            val dist = seg.getSignedDistance(actual, tmp, tmp3, tmpDist)
            val error = abs(dist.distance)
            maxFoundError = max(maxFoundError, error)
        }
        return maxFoundError
    }
}