package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.abs

abstract class EdgeSegment {

    abstract fun getPointAt(t: Float, dst: Vector2f): Vector2f
    abstract fun getDirectionAt(t: Float, dst: Vector2f): Vector2f

    abstract fun length(): Float
    abstract fun union(bounds: AABBf, tmp: FloatArray)

    abstract fun getSignedDistance(
        origin: Vector2f,
        outT: FloatPtr,
        tmp3: FloatArray,
        dst: SignedDistance
    ): SignedDistance

    abstract fun transformed(transform: Matrix3x2f): EdgeSegment

    fun getTrueSignedDistance(
        origin: Vector2f, outT: FloatPtr,
        tmp3: FloatArray, tmpD: SignedDistance
    ): Float {
        val distance = getSignedDistance(origin, outT, tmp3, tmpD)
        distanceToPseudoDistance(distance, origin, outT.value)
        return distance.distance
    }

    private fun distanceToPseudoDistance(distance: SignedDistance, origin: Vector2f, param: Float) {
        val v0 = JomlPools.vec2f.create()
        val v1 = JomlPools.vec2f.create()
        val v2 = JomlPools.vec2f.create()
        if (param < 0f) {
            val dir = getDirectionAt(0f, v0).normalize()
            val aq = v2.set(origin).sub(getPointAt(0f, v1))
            val ts = aq.dot(dir)
            if (ts < 0f) {
                val pseudoDistance = aq.cross(dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.set(pseudoDistance, 0f)
                }
            }
        } else if (param > 1f) {
            val dir = getDirectionAt(1f, v0).normalize()
            val bq = v2.set(origin).sub(getPointAt(1f, v1))
            val ts = bq.dot(dir)
            if (ts > 0f) {
                val pseudoDistance = bq.cross(dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.set(pseudoDistance, 0f)
                }
            }
        }
        JomlPools.vec2f.sub(3)
    }
}