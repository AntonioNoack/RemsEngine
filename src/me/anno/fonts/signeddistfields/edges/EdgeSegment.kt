package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.utils.pooling.JomlPools
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs

abstract class EdgeSegment {

    abstract fun point(param: Float, dst: Vector2f): Vector2f

    abstract fun direction(param: Float, dst: Vector2f): Vector2f

    abstract fun length(): Float

    abstract fun union(bounds: AABBf, tmp: FloatArray)

    abstract fun signedDistance(
        origin: Vector2fc,
        param: FloatPtr,
        tmp: FloatArray,
        dst: SignedDistance
    ): SignedDistance

    fun trueSignedDistance(
        origin: Vector2f,
        tmpParam: FloatPtr,
        tmp2: FloatArray,
        tmp: SignedDistance
    ): Float {
        val distance = signedDistance(origin, tmpParam, tmp2, tmp)
        distanceToPseudoDistance(distance, origin, tmpParam.value)
        return distance.distance
    }

    private fun distanceToPseudoDistance(distance: SignedDistance, origin: Vector2fc, param: Float) {
        val v0 = JomlPools.vec2f.create()
        val v1 = JomlPools.vec2f.create()
        val v2 = JomlPools.vec2f.create()
        if (param < 0f) {
            val dir = direction(0f, v0).normalize()
            val aq = v2.set(origin).sub(point(0f, v1))
            val ts = aq.dot(dir)
            if (ts < 0f) {
                val pseudoDistance = crossProduct(aq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.set(pseudoDistance, 0f)
                }
            }
        } else if (param > 1f) {
            val dir = direction(1f, v0).normalize()
            val bq = v2.set(origin).sub(point(1f, v1))
            val ts = bq.dot(dir)
            if (ts > 0f) {
                val pseudoDistance = crossProduct(bq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.set(pseudoDistance, 0f)
                }
            }
        }
        JomlPools.vec2f.sub(3)
    }

}