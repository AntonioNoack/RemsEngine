package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.DoublePtr
import me.anno.fonts.signeddistfields.Flags.MSDFGEN_CUBIC_SEARCH_STARTS
import me.anno.fonts.signeddistfields.Flags.MSDFGEN_CUBIC_SEARCH_STEPS
import me.anno.fonts.signeddistfields.structs.Point2
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.fonts.signeddistfields.structs.Vector2
import me.anno.fonts.signeddistfields.algorithm.EquationSolver.solveCubic
import me.anno.fonts.signeddistfields.algorithm.EquationSolver.solveQuadratic
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.getOrthonormal
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.mix
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.sign
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.union
import me.anno.utils.Vectors.minus
import me.anno.utils.Vectors.plus
import me.anno.utils.Vectors.times
import org.joml.AABBd
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * adapted from Multi Channel Signed Distance fields
 * */
class CubicSegment(
    var p0: Point2, p10: Point2, p20: Point2, var p3: Point2
) :
    EdgeSegment() {

    var p1 = if ((p10 == p0 || p10 == p3) && (p20 == p0 || p20 == p3)) mix(p0, p3, 1.0 / 3.0) else p10
    var p2 = if ((p10 == p0 || p10 == p3) && (p20 == p0 || p20 == p3)) mix(p0, p3, 2.0 / 3.0) else p20

    override fun toString() = "[$p0 $p1 $p2 $p3]"

    override fun clone() = CubicSegment(p0, p1, p2, p3)
    override fun point(param: Double): Point2 {
        val p12 = mix(p1, p2, param)
        return mix(
            mix(mix(p0, p1, param), p12, param),
            mix(p12, mix(p2, p3, param), param),
            param
        )
    }

    override fun direction(param: Double): Vector2 {
        val tangent = mix(
            mix(p1 - p0, p2 - p1, param),
            mix(p2 - p1, p3 - p2, param),
            param
        )
        if (tangent.length() == 0.0) {
            if (param == 0.0) return p2 - p0
            if (param == 1.0) return p3 - p1
        }
        return tangent
    }

    override fun length(): Double {
        TODO("Not yet implemented")
    }

    override fun reverse() {
        var t = p3
        p3 = p0
        p0 = t
        t = p1
        p1 = p2
        p2 = t
    }

    override fun union(bounds: AABBd) {
        union(bounds, p0)
        union(bounds, p3)
        val a0 = p1 - p0
        val a1 = (p2 - p1 - a0) * 2.0
        val a2 = p3 - p2 * 3.0 + p1 * 3.0 - p0
        val params = DoubleArray(2)
        var solutions = solveQuadratic(params, a2.x, a1.x, a0.x)
        for (i in 0 until solutions) {
            if (params[i] > 0 && params[i] < 1)
                union(bounds, point(params[i]))
        }
        solutions = solveQuadratic(params, a2.y, a1.y, a0.y);
        for (i in 0 until solutions) {
            if (params[i] > 0 && params[i] < 1)
                union(bounds, point(params[i]))
        }
    }

    override fun moveStartPoint(to: Point2) {
        p1 += (to - p0)
        p0 = to
    }

    override fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int) {
        parts[a] = CubicSegment(
            p0,
            if (p0 == p1) p0 else mix(p0, p1, 1 / 3.0),
            mix(mix(p0, p1, 1 / 3.0), mix(p1, p2, 1 / 3.0), 1 / 3.0),
            point(1 / 3.0)
        );
        parts[b] = CubicSegment(
            point(1 / 3.0),
            mix(
                mix(mix(p0, p1, 1 / 3.0), mix(p1, p2, 1 / 3.0), 1 / 3.0),
                mix(mix(p1, p2, 1 / 3.0), mix(p2, p3, 1 / 3.0), 1 / 3.0),
                2 / 3.0
            ),
            mix(
                mix(mix(p0, p1, 2 / 3.0), mix(p1, p2, 2 / 3.0), 2 / 3.0),
                mix(mix(p1, p2, 2 / 3.0), mix(p2, p3, 2 / 3.0), 2 / 3.0),
                1 / 3.0
            ),
            point(2 / 3.0)
        )
        parts[c] = CubicSegment(
            point(2 / 3.0),
            mix(mix(p1, p2, 2 / 3.0), mix(p2, p3, 2 / 3.0), 2 / 3.0),
            if (p2 == p3) p3 else mix(p2, p3, 2 / 3.0),
            p3
        )
    }

    override fun scanlineIntersections(x: DoubleArray, dy: IntArray, y: Double): Int {
        var total = 0
        var nextDY = if (y > p0.y) 1 else -1
        x[total] = p0.x
        if (p0.y == y) {
            if (p0.y < p1.y || (p0.y == p1.y && (p0.y < p2.y || (p0.y == p2.y && p0.y < p3.y))))
                dy[total++] = 1;
            else
                nextDY = 1;
        }

        val ab = p1 - p0
        val br = p2 - p1 - ab
        val az = (p3 - p2) - (p2 - p1) - br
        val t = DoubleArray(3)
        val solutions = solveCubic(t, az.y, br.y * 3.0, ab.y * 3.0, p0.y - y);
        // Sort solutions
        if (solutions >= 2) {
            if (t[0] > t[1]) {
                val tmp = t[0]
                t[0] = t[1]
                t[1] = tmp
            }
            if (solutions >= 3 && t[1] > t[2]) {
                var tmp = t[1]
                t[1] = t[2]
                t[2] = tmp
                if (t[0] > t[1]) {
                    tmp = t[0]
                    t[0] = t[1]
                    t[1] = tmp
                }
            }
        }
        for (i in 0 until solutions) {
            if (total < 3) {
                if (t[i] in 0.0..1.0) {
                    x[total] = p0.x + 3 * t[i] * ab.x + 3 * t[i] * t[i] * br.x + t[i] * t[i] * t[i] * az.x;
                    if (nextDY * (ab.y + 2 * t[i] * br.y + t[i] * t[i] * az.y) >= 0) {
                        dy[total++] = nextDY;
                        nextDY = -nextDY;
                    }
                }
            } else break
        }

        if (p3.y == y) {
            if (nextDY > 0 && total > 0) {
                total--
                nextDY = -1
            }
            if ((p3.y < p2.y || (p3.y == p2.y && (p3.y < p1.y || (p3.y == p1.y && p3.y < p0.y)))) && total < 3) {
                x[total] = p3.x
                if (nextDY < 0) {
                    dy[total++] = -1
                    nextDY = 1
                }
            }
        }
        if (nextDY != (if (y >= p3.y) 1 else -1)) {
            if (total > 0) {
                total--
            } else {
                if (abs(p3.y - y) < abs(p0.y - y)) {
                    x[total] = p3.x
                }
                dy[total++] = nextDY
            }
        }
        return total
    }

    override fun signedDistance(origin: Point2, param: DoublePtr): SignedDistance {
        val qa: Vector2 = p0 - origin
        val ab: Vector2 = p1 - p0
        val br: Vector2 = p2 - p1 - ab
        val az: Vector2 = p3 - p2 - (p2 - p1) - br

        var epDir = direction(0.0)
        var minDistance: Double = nonZeroSign(crossProduct(epDir, qa)) * qa.length() // distance from A

        param.value = -dotProduct(qa, epDir) / dotProduct(epDir, epDir)

        epDir = direction(1.0)
        val distance: Double = (p3 - origin).length() // distance from B
        if (distance < abs(minDistance)) {
            minDistance = nonZeroSign(crossProduct(epDir, p3 - origin)) * distance
            param.value = dotProduct(epDir - (p3 - origin), epDir) / dotProduct(epDir, epDir)
        }

        // Iterative minimum distance search
        for (i in 0..MSDFGEN_CUBIC_SEARCH_STARTS) {
            var t: Double = i.toDouble() / MSDFGEN_CUBIC_SEARCH_STARTS
            var qe = qa + 3 * t * ab + 3 * t * t * br + t * t * t * az
            for (step in 0 until MSDFGEN_CUBIC_SEARCH_STEPS) {
                // Improve t
                val d1: Vector2 = az * 3.0 * t * t + br * 6.0 * t + ab * 3.0
                val d2: Vector2 = az * 6.0 * t + br * 6.0
                t -= dotProduct(qe, d1) / (dotProduct(d1, d1) + dotProduct(qe, d2))
                if (t <= 0 || t >= 1) break
                qe = qa + 3 * t * ab + 3 * t * t * br + t * t * t * az
                val distance = qe.length()
                if (distance < abs(minDistance)) {
                    minDistance = nonZeroSign(crossProduct(direction(t), qe)) * distance
                    param.value = t
                }
            }
        }

        if (param.value in 0.0..1.0) return SignedDistance(
            minDistance,
            0.0
        )
        return if (param.value < .5) SignedDistance(
            minDistance,
            abs(dotProduct(direction(0.0).normalize(), qa.normalize()))
        ) else SignedDistance(
            minDistance,
            abs(dotProduct(direction(1.0).normalize(), (p3 - origin).normalize()))
        )
    }

    operator fun Int.times(p: Point2) = p * this.toDouble()
    operator fun Double.times(p: Point2) = p * this

    fun deconverge(param: Int, amount: Double) {
        val dir = direction(param)
        val normal = dir.getOrthonormal()
        val h = dotProduct(directionChange(param.toDouble()) - dir, normal)
        when (param) {
            0 -> p1 += amount * (dir + sign(h) * sqrt(abs(h)) * normal)
            1 -> p2 -= amount * (dir - sign(h) * sqrt(abs(h)) * normal)
        }
    }

    override fun directionChange(param: Double) = mix((p2 - p1) - (p1 - p0), (p3 - p2) - (p2 - p1), param)

}