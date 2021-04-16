package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.EquationSolver.solveCubic
import me.anno.fonts.signeddistfields.algorithm.EquationSolver.solveQuadratic
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.mix
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.union
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class QuadraticSegment(var p0: Vector2fc, p10: Vector2fc, var p2: Vector2fc) : EdgeSegment() {

    var p1 = if (p0 == p10 || p10 == p2) (p0 + p2) * 0.5f else p10

    override fun toString() = "[$p0 $p1 $p2]"

    override fun clone() = QuadraticSegment(p0, p1, p2)
    override fun point(param: Float): Vector2f {
        return mix(mix(p0, p1, param), mix(p1, p2, param), param)
    }

    override fun direction(param: Float): Vector2f {
        val tangent = mix(p1 - p0, p2 - p1, param)
        if (tangent.length() == 0f) return p2 - p0
        return tangent
    }

    override fun length(): Float {
        val ab: Vector2f = p1 - p0
        val br: Vector2f = p2 - p1 - ab
        val abab = ab.dot(ab)
        val abbr = ab.dot(br)
        val brbr = br.dot(br)
        val abLen = sqrt(abab)
        val brLen = sqrt(brbr)
        val crs = crossProduct(ab, br)
        val h = sqrt(abab + abbr + abbr + brbr)
        return (brLen * ((abbr + brbr) * h - abbr * abLen) +
                crs * crs * ln((brLen * h + abbr + brbr) / (brLen * abLen + abbr))) / (brbr * brLen)
    }

    override fun reverse() {
        val t = p0
        p0 = p2
        p2 = t
    }

    override fun union(bounds: AABBf) {
        union(bounds, p0)
        union(bounds, p1)
        val bot = (p1 - p0) - (p2 - p1)
        if (bot.x != 0f) {
            val param = (p1.x() - p0.x()) / bot.x
            if (param > 0 && param < 1) union(bounds, point(param))
        } else {
            val param = (p1.y() - p0.y()) / bot.y
            if (param > 0 && param < 1) union(bounds, point(param))
        }
    }

    override fun moveStartPoint(to: Vector2f) {
        val origSDir = p0 - p1
        val origP1 = p1
        p1 += (p2 - p1) * (crossProduct(p0 - p1, to - p0) / crossProduct(p0 - p1, p2 - p1))
        p0 = to
        if (dotProduct(origSDir, p0 - p1) < 0) {
            p1 = origP1
        }
    }

    override fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int) {
        parts[a] = QuadraticSegment(p0, mix(p0, p1, 1f / 3f), point(1f / 3f))
        parts[b] = QuadraticSegment(
            point(1f / 3f), mix(
                mix(p0, p1, 5f / 9f),
                mix(p1, p2, 4f / 9f),
                0.5f
            ), point(2f / 3f)
        )
        parts[c] = QuadraticSegment(point(2f / 3f), mix(p1, p2, 2f / 3f), p2)
    }

    override fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int {
        var total = 0
        var nextDY = if (y > p0.y()) 1 else -1
        x[total] = p0.x()
        if (p0.y() == y) {
            if (p0.y() < p1.y() || (p0.y() == p1.y() && p0.y() < p2.y()))
                dy[total++] = 1
            else
                nextDY = 1
        }

        val ab = p1 - p0
        val br = p2 - p1 - ab
        val t = FloatArray(2)
        val solutions = solveQuadratic(t, br.y, 2 * ab.y, p0.y() - y)
        // Sort solutions
        if (solutions >= 2 && t[0] > t[1]) {
            val tmp = t[0]
            t[0] = t[1]
            t[1] = tmp
        }
        for (i in 0 until solutions) {
            if (total < 2) {
                if (t[i] in 0.0..1.0) {
                    x[total] = p0.x() + 2 * t[i] * ab.x + t[i] * t[i] * br.x
                    if (nextDY * (ab.y + t[i] * br.y) >= 0) {
                        dy[total++] = nextDY
                        nextDY = -nextDY
                    }
                }
            } else break
        }

        if (p2.y() == y) {
            if (nextDY > 0 && total > 0) {
                total--
                nextDY = -1
            }
            if ((p2.y() < p1.y() || (p2.y() == p1.y() && p2.y() < p0.y())) && total < 2) {
                x[total] = p2.x()
                if (nextDY < 0) {
                    dy[total++] = -1
                    nextDY = 1
                }
            }
        }
        if (nextDY != (if (y >= p2.y()) 1 else -1)) {
            if (total > 0) {
                total--
            } else {
                if (abs(p2.y() - y) < abs(p0.y() - y)) {
                    x[total] = p2.x()
                }
                dy[total++] = nextDY
            }
        }
        return total
    }

    override fun signedDistance(origin: Vector2fc, param: FloatPtr): SignedDistance {

        val qa = p0 - origin
        val ab = p1 - p0
        val br = p2 - p1 - ab
        val a = dotProduct(br, br)
        val b = 3 * dotProduct(ab, br)
        val c = 2 * dotProduct(ab, ab) + dotProduct(qa, br)
        val d = dotProduct(qa, ab)
        val t = FloatArray(3)
        val solutions = solveCubic(t, a, b, c, d)

        var epDir = direction(0f)
        var minDistance = nonZeroSign(crossProduct(epDir, qa)) * qa.length() // distance from A
        param.value = -dotProduct(qa, epDir) / dotProduct(epDir, epDir)

        epDir = direction(1f)
        val distance = p2.distance(origin); // distance from B
        if (distance < abs(minDistance)) {
            val cross = crossProduct(epDir, p2 - origin)
            minDistance = if (cross >= 0f) +distance else -distance
            param.value = dotProduct(origin - p1, epDir) / dotProduct(epDir, epDir)
        }

        for (i in 0 until solutions) {
            if (t[i] > 0 && t[i] < 1) {
                val qe = p0 + ab * (2 * t[i]) + br * (t[i] * t[i]) - origin
                val distance2 = qe.length()
                if (distance2 <= abs(minDistance)) {
                    val cross = crossProduct(direction(t[i]), qe)
                    minDistance = if (cross >= 0f) distance2 else -distance2
                    param.value = t[i]
                }
            }
        }

        return when {
            param.value in 0.0..1.0 -> SignedDistance(minDistance, 0f)
            param.value < .5 -> SignedDistance(minDistance, abs(normalizedDot(direction(0), qa)))
            else -> SignedDistance(minDistance, abs(dotProduct(direction(1).normalize(), (p2 - origin).normalize())))
        }
    }

    fun normalizedDot(a: Vector2f, b: Vector2f): Float {
        val ax = a.x
        val ay = a.y
        val bx = b.x
        val by = b.y
        return (ax * bx + ay * by) / sqrt((ax * ax + ay * ay) * (bx * bx + by * by))
    }

    fun convertToCubic() = CubicSegment(p0, mix(p0, p1, 2f / 3f), mix(p1, p2, 1f / 3f), p2)

    override fun directionChange(param: Float): Vector2f {
        return (p2 - p1) - (p1 - p0)
    }

}