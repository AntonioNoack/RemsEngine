package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.DoublePtr
import me.anno.fonts.signeddistfields.structs.Point2
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.fonts.signeddistfields.structs.Vector2
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.utils.Vectors.minus
import org.joml.AABBd
import kotlin.math.abs

abstract class EdgeSegment {

    abstract fun clone(): EdgeSegment
    abstract fun point(param: Double): Point2
    fun point(param: Int) = point(param.toDouble())
    abstract fun direction(param: Double): Vector2
    fun direction(param: Int) = direction(param.toDouble())
    abstract fun length(): Double
    abstract fun reverse()
    abstract fun union(bounds: AABBd)
    abstract fun moveStartPoint(to: Point2)
    abstract fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int)
    abstract fun scanlineIntersections(x: DoubleArray, dy: IntArray, y: Double): Int
    abstract fun signedDistance(origin: Point2, param: DoublePtr): SignedDistance
    abstract fun directionChange(param: Double): Vector2

    fun trueSignedDistance(origin: Point2): Double {
        val param = DoublePtr()
        val distance = signedDistance(origin, param)
        distanceToPseudoDistance(distance, origin, param.value)
        return distance.distance
    }

    fun distanceToPseudoDistance(distance: SignedDistance, origin: Point2, param: Double) {
        if (param < 0) {
            val dir = direction(0).normalize()
            val aq = origin - point(0)
            val ts = aq.dot(dir)
            if (ts < 0) {
                val pseudoDistance = crossProduct(aq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.distance = pseudoDistance
                    distance.dot = 0.0
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
                    distance.dot = 0.0
                }
            }
        }
    }

}