package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absAngleCos
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absAngleCosDiffXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossDiffXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotDiffXXY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.EquationSolver.solveCubic
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.avg
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class QuadraticSegment(val p0: Vector2f, p10: Vector2f, val p2: Vector2f) : EdgeSegment() {

    val p1 = if (p0 == p10 || p10 == p2) avg(p0, p2) else p10

    override fun toString() = "[$p0 $p1 $p2]"

    override fun getPointAt(t: Float, dst: Vector2f): Vector2f {
        val f0 = sq(1f - t)
        val f1 = 2f * (1f - t) * t
        val f2 = t * t
        return p0.mul(f0, dst)
            .add(p1.x * f1, p1.y * f1)
            .add(p2.x * f2, p2.y * f2)
    }

    override fun getDirectionAt(t: Float, dst: Vector2f): Vector2f {
        val b = 1f - t
        val b2 = b * b
        val a2 = t * t
        val ba = b - t
        dst.set(p0).mul(-b2)
            .add(p1.x * ba, p1.y * ba)
            .add(p2.x * a2, p2.y * a2)
        if (abs(dst.length()) < 1e-12f) return dst.set(p2).sub(p0)
        return dst
    }

    override fun length(): Float {

        val ab = JomlPools.vec2f.create()
        val br = JomlPools.vec2f.create()

        ab.set(p1).sub(p0)
        br.set(p2).sub(p1).sub(ab)

        val dist01 = p0.distanceSquared(p1)
        val abbr = ab.dot(br)
        val dist12 = br.lengthSquared()
        val abLen = sqrt(dist01)
        val brLen = sqrt(dist12)
        val crs = ab.cross(br)
        val h = sqrt(dist01 + abbr + abbr + dist12)

        JomlPools.vec2f.sub(2)

        return (brLen * ((abbr + dist12) * h - abbr * abLen) +
                crs * crs * ln((brLen * h + abbr + dist12) / (brLen * abLen + abbr))) / (dist12 * brLen)
    }

    override fun union(bounds: AABBf, tmp: FloatArray) {

        bounds.union(p0)
        bounds.union(p1)

        val bot = JomlPools.vec2f.create()
            .set(p1).add(p1)
            .sub(p0).sub(p2)

        if (bot.x != 0f) {
            val param = (p1.x - p0.x) / bot.x
            if (param > 0f && param < 1f) {
                bounds.union(getPointAt(param, bot))
            }
        } else {
            val param = (p1.y - p0.y) / bot.y
            if (param > 0f && param < 1f) {
                bounds.union(getPointAt(param, bot))
            }
        }

        JomlPools.vec2f.sub(1)
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

        p0.sub(origin, qa)
        p1.sub(p0, ab)
        p2.sub(p1, br).sub(ab)

        val a = br.lengthSquared()
        val b = 3f * ab.dot(br)
        val c = 2f * ab.lengthSquared() + qa.dot(br)
        val d = qa.dot(ab)
        val solutions = solveCubic(tmp3, a, b, c, d)

        val epDir = JomlPools.vec2f.create()

        getDirectionAt(0f, epDir)
        var minDistance = nonZeroSign(epDir.cross(qa)) * qa.length() // distance from A
        var t = -qa.dot(epDir) / epDir.lengthSquared()

        getDirectionAt(1f, epDir)
        val distance = p2.distance(origin) // distance from B
        if (distance < abs(minDistance)) {
            val cross = crossDiffXYY(epDir, p2, origin)
            minDistance = if (cross >= 0f) +distance else -distance
            t = dotDiffXXY(origin, p1, epDir) / epDir.lengthSquared()
        }

        val qe = JomlPools.vec2f.create()
        val dir = JomlPools.vec2f.create()
        for (i in 0 until solutions) {
            val tmpI = tmp3[i]
            if (tmpI > 0f && tmpI < 1f) {
                val tmpI2 = tmpI * tmpI
                // val qe = p0 + ab * (2 * tmpI) + br * (tmpI * tmpI) - origin
                ab.mul(2f * tmpI, qe)
                    .add(p0)
                    .add(br.x * tmpI2, br.y * tmpI2)
                    .sub(origin)
                val distance2 = qe.length()
                if (distance2 <= abs(minDistance)) {
                    getDirectionAt(tmpI, dir)
                    val cross = dir.cross(qe)
                    minDistance = if (cross >= 0f) distance2 else -distance2
                    t = tmpI
                }
            }
        }

        val dotDistance = when {
            t in 0f..1f -> 0f
            t < 0f -> absAngleCos(getDirectionAt(0f, epDir), qa)
            else -> absAngleCosDiffXYY(getDirectionAt(1f, epDir), p2, origin)
        }
        dst.set(minDistance, dotDistance)
        outT.value = t
        JomlPools.vec2f.sub(6)

        return dst
    }
}