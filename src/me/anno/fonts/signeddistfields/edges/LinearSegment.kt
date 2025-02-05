package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.absAngleCosDiffXYY
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.getOrthonormal
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector2f

class LinearSegment(val p0: Vector2f, val p1: Vector2f) : EdgeSegment() {

    override fun getPointAt(t: Float, dst: Vector2f): Vector2f = p0.mix(p1, t, dst)
    override fun getDirectionAt(t: Float, dst: Vector2f): Vector2f = p1.sub(p0,dst)
    override fun length(): Float = p1.distance(p0)

    override fun toString() = "[$p0 $p1]"

    override fun union(bounds: AABBf, tmp: FloatArray) {
        bounds.union(p0.x, p0.y, 0f)
        bounds.union(p1.x, p1.y, 0f)
    }

    override fun getSignedDistance(
        origin: Vector2f,
        outT: FloatPtr,
        tmp3: FloatArray,
        dst: SignedDistance
    ): SignedDistance {

        val aq = JomlPools.vec2f.create()
        val ab = JomlPools.vec2f.create()

        aq.set(origin).sub(p0)
        ab.set(p1).sub(p0)

        val t = aq.dot(ab) / ab.lengthSquared()
        outT.value = t

        return if (t in 0f..1f) {
            val orthoNormal = ab.getOrthonormal(ab)
            val orthoDistance = orthoNormal.dot(aq)
            JomlPools.vec2f.sub(2)
            dst.set(orthoDistance, 0f)
        } else {
            val eqRef = if (outT.value > 0.5) p1 else p0
            val endpointDistance = eqRef.distance(origin)
            val distance = nonZeroSign(aq.cross(ab)) * endpointDistance
            val dotDistance = absAngleCosDiffXYY(ab, eqRef, origin)
            JomlPools.vec2f.sub(2)
            dst.set(distance, dotDistance)
        }
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