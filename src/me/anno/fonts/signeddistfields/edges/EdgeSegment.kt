package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.utils.types.Vectors.minus
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs

abstract class EdgeSegment {

    abstract fun clone(): EdgeSegment
    abstract fun point(param: Float): Vector2f

    fun point(param: Int) = point(param.toFloat())

    abstract fun direction(param: Float): Vector2f

    fun direction(param: Int) = direction(param.toFloat())

    abstract fun length(): Float
    abstract fun reverse()
    abstract fun union(bounds: AABBf)
    abstract fun moveStartPoint(to: Vector2f)
    abstract fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int)
    abstract fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int
    abstract fun signedDistance(origin: Vector2fc, param: FloatPtr): SignedDistance
    abstract fun directionChange(param: Float): Vector2f

    fun trueSignedDistance(origin: Vector2f): Float {
        val param = FloatPtr()
        val distance = signedDistance(origin, param)
        distanceToPseudoDistance(distance, origin, param.value)
        return distance.distance
    }

    fun distanceToPseudoDistance(distance: SignedDistance, origin: Vector2fc, param: Float) {
        if (param < 0) {
            val dir = direction(0).normalize()
            val aq = origin - point(0)
            val ts = aq.dot(dir)
            if (ts < 0) {
                val pseudoDistance = crossProduct(aq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.distance = pseudoDistance
                    distance.dot = 0f
                }
            }
        } else if (param > 1) {
            val dir = direction(1).normalize()
            val bq = origin - point(1)
            val ts = dotProduct(bq, dir)
            if (ts > 0) {
                val pseudoDistance = crossProduct(bq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.distance = pseudoDistance
                    distance.dot = 0f
                }
            }
        }
    }

}