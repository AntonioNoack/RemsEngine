package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absDotNormalizedXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.getOrthonormal
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.math.abs

class LinearSegment(val p0: Vector2f, val p1: Vector2f) : EdgeSegment() {

    override fun point(t: Float, dst: Vector2f): Vector2f = dst.set(p0).lerp(p1, t)

    override fun direction(t: Float, dst: Vector2f): Vector2f = dst.set(p1).sub(p0)

    override fun length(): Float = p1.distance(p0)

    override fun toString() = "[$p0 $p1]"

    override fun union(bounds: AABBf, tmp: FloatArray) {
        bounds.union(p0.x, p0.y, 0f)
        bounds.union(p1.x, p1.y, 0f)
    }

    override fun signedDistance(
        origin: Vector2f,
        param: FloatPtr,
        tmp: FloatArray,
        dst: SignedDistance
    ): SignedDistance {

        val aq = JomlPools.vec2f.create()
        val ab = JomlPools.vec2f.create()
        val orthoNormal = JomlPools.vec2f.create()

        aq.set(origin).sub(p0)
        ab.set(p1).sub(p0)
        param.value = aq.dot(ab) / ab.lengthSquared()
        val eqRef = if (param.value > 0.5) p1 else p0
        val endpointDistance = eqRef.distance(origin)
        if (param.value > 0 && param.value < 1) {
            ab.getOrthonormal(false, allowZero = false, orthoNormal)
            val orthoDistance = orthoNormal.dot(aq)
            if (abs(orthoDistance) < endpointDistance) {
                JomlPools.vec2f.sub(3)
                return dst.set(orthoDistance, 0f)
            }// else should not happen, if I understand this correctly...
        }

        dst.set(
            nonZeroSign(aq.cross(ab)) * endpointDistance,
            absDotNormalizedXYY(ab, eqRef, origin)
        )

        JomlPools.vec2f.sub(3)

        return dst
    }

    companion object {
        fun signedDistanceSq(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
            val pax = px - ax
            val pay = py - ay
            val bax = bx - ax
            val bay = by - ay
            val h = Maths.clamp((pax * bax + pay * bay) / Vector2f.lengthSquared(bax, bay))
            return Vector2f.lengthSquared(pax - bax * h, pay - bay * h)
        }
    }

}