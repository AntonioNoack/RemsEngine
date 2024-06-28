package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absDotNormalized
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absDotNormalizedXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProductXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.EquationSolver.solveQuadratic
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.abs

/**
 * adapted from Multi Channel Signed Distance fields
 * */
class CubicSegment(
    val p0: Vector2f,
    p10: Vector2f,
    p20: Vector2f,
    val p3: Vector2f
) : EdgeSegment() {

    companion object {
        var CUBIC_SEARCH_STARTS = 4
        var CUBIC_SEARCH_STEPS = 4
    }

    val p1 = if ((p10 == p0 || p10 == p3) && (p20 == p0 || p20 == p3)) mix(p0, p3, 1f / 3f) else p10
    val p2 = if ((p10 == p0 || p10 == p3) && (p20 == p0 || p20 == p3)) mix(p0, p3, 2f / 3f) else p20

    override fun toString() = "[$p0 $p1 $p2 $p3]"

    override fun getPointAt(t: Float, dst: Vector2f): Vector2f {
        val b = 1f - t
        val b2 = b * b
        val pr2 = t * t
        val aaa = pr2 * t
        val aab = 3f * pr2 * b
        val abb = 3f * t * b2
        val bbb = b * b2
        return dst.set(p0).mul(bbb)
            .add(p1.x * abb, p1.y * abb)
            .add(p2.x * aab, p2.y * aab)
            .add(p3.x * aaa, p3.y * aaa)
    }

    override fun getDirectionAt(t: Float, dst: Vector2f): Vector2f {
        val b = 1f - t
        val a2 = t * t
        val ab2 = 2f * t * b
        val b2 = b * b
        val f1 = b2 - ab2
        val f2 = ab2 - a2
        dst.set(p0).mul(-b2)
            .add(p1.x * f1, p1.y * f1)
            .add(p2.x * f2, p2.y * f2)
            .add(p3.x * a2, p3.y * a2)
        if (dst.lengthSquared() == 0f) {
            if (t == 0f) return dst.set(p2).sub(p0)
            if (t == 1f) return dst.set(p3).sub(p1)
        }
        return dst
    }

    override fun length(): Float {
        throw RuntimeException("length() not implemented")
    }

    override fun union(bounds: AABBf, tmp: FloatArray) {

        bounds.union(p0.x, p0.y, 0f)
        bounds.union(p3.x, p3.y, 0f)

        val a0 = JomlPools.vec2f.create()
        val a1 = JomlPools.vec2f.create()
        val a2 = JomlPools.vec2f.create()

        a0.set(p1).sub(p0)
        a1.set(p2).sub(p1).sub(a0).mul(2f)
        a2.set(p1).sub(p2).mul(3f).add(p3).sub(p0)

        val tmpV2 = JomlPools.vec2f.create()

        var solutions = solveQuadratic(tmp, a2.x, a1.x, a0.x)
        for (i in 0 until solutions) {
            val tmpI = tmp[i]
            if (tmpI > 0f && tmpI < 1f) {
                bounds.union(getPointAt(tmpI, tmpV2))
            }
        }

        solutions = solveQuadratic(tmp, a2.y, a1.y, a0.y)
        for (i in 0 until solutions) {
            val tmpI = tmp[i]
            if (tmpI > 0f && tmpI < 1f) {
                bounds.union(getPointAt(tmpI, tmpV2))
            }
        }

        JomlPools.vec2f.sub(4)
    }

    override fun getSignedDistance(
        origin: Vector2f,
        outT: FloatPtr,
        tmp3: FloatArray,
        dst: SignedDistance
    ): SignedDistance {

        val qa = JomlPools.vec2f.create()
        val ab = JomlPools.vec2f.create()
        val br = JomlPools.vec2f.create()
        val az = JomlPools.vec2f.create()
        val epDir = JomlPools.vec2f.create()
        val d1 = JomlPools.vec2f.create()
        val d2 = JomlPools.vec2f.create()
        val qe = JomlPools.vec2f.create()

        qa.set(p0).sub(origin)
        ab.set(p1).sub(p0)
        br.set(p2).sub(p1).sub(ab)
        az.set(p3).sub(p2).sub(p2).add(p1).sub(br)

        getDirectionAt(0f, epDir)
        var minDistance = nonZeroSign(epDir.cross(qa)) * qa.length() // distance from A

        outT.value = -qa.dot(epDir) / epDir.lengthSquared()

        getDirectionAt(1f, epDir)
        val distance = p3.distance(origin) // distance from B
        if (distance < abs(minDistance)) {
            minDistance = nonZeroSign(crossProductXYY(epDir, p3, origin)) * distance
            val dotProduct = epDir.lengthSquared() - p3.dot(epDir) + origin.dot(epDir)
            outT.value = dotProduct / epDir.lengthSquared()
        }

        // Iterative minimum distance search
        for (i in 0..CUBIC_SEARCH_STARTS) {
            var t = i.toFloat() / CUBIC_SEARCH_STARTS
            interpolate(qe, qa, ab, br, az, t)
            for (step in 0 until CUBIC_SEARCH_STEPS) {
                // Improve t
                d1.set(az).mul(t * t)
                    .add(br.x * 2f * t, br.y * 2f * t)
                    .add(ab.x, ab.y)
                    .mul(3f) // az * (3f * t * t) + br * (6f * t) + ab * 3f
                d2.set(az).mul(t).add(br).mul(6f) // az * (6f * t) + br * 6f
                t -= qe.dot(d1) / (d1.lengthSquared() + qe.dot(d2))
                if (t <= 0f || t >= 1f) break
                interpolate(qe, qa, ab, br, az, t)
                val distance2 = qe.length()
                if (distance2 < abs(minDistance)) {
                    minDistance = nonZeroSign(getDirectionAt(t, epDir).cross(qe)) * distance2
                    outT.value = t
                }
            }
        }

        dst.set(
            minDistance, when {
                outT.value in 0f..1f -> 0f
                outT.value < 0.5f -> absDotNormalized(getDirectionAt(0f, epDir), qa)
                else -> absDotNormalizedXYY(getDirectionAt(1f, epDir), p3, origin)
            }
        )

        JomlPools.vec2f.sub(8)

        return dst
    }

    private fun interpolate(dst: Vector2f, qa: Vector2f, ab: Vector2f, br: Vector2f, az: Vector2f, t: Float) {
        // var qe = qa + (3 * t) * ab + (3 * t * t) * br + (t * t * t) * az
        // = qa + t*(3*ab + t * (3*br + t*az))
        val f0 = 3f * t
        val f1 = 3f * t * t
        val f2 = t * t * t
        dst.set(qa)
            .add(ab.x * f0, ab.y * f0)
            .add(br.x * f1, br.y * f1)
            .add(az.x * f2, az.y * f2)
    }
}