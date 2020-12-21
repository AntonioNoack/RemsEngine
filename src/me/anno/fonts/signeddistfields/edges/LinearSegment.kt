package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.DoublePtr
import me.anno.fonts.signeddistfields.structs.Point2
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.fonts.signeddistfields.structs.Vector2
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.getOrthonormal
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.mix
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.sign
import me.anno.utils.Vectors.minus
import org.joml.AABBd
import kotlin.math.abs

class LinearSegment(var p0: Point2, var p1: Point2) : EdgeSegment() {

    override fun clone() = LinearSegment(p0, p1)
    override fun point(param: Double) = mix(p0, p1, param)
    override fun direction(param: Double): Vector2 = p1 - p0
    override fun length(): Double = (p1 - p0).length()
    override fun reverse() {
        val t = p1
        p1 = p0
        p0 = t
    }

    override fun toString() = "[$p0 $p1]"

    override fun union(bounds: AABBd) {
        bounds.union(p0.x, p0.y, 0.0)
        bounds.union(p1.x, p1.y, 0.0)
    }

    override fun moveStartPoint(to: Point2) {
        p0 = to
    }

    override fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int) {
        parts[a] = LinearSegment(p0, point(1 / 3.0))
        parts[b] = LinearSegment(point(1 / 3.0), point(2 / 3.0))
        parts[c] = LinearSegment(point(2 / 3.0), p1)
    }

    override fun scanlineIntersections(x: DoubleArray, dy: IntArray, y: Double): Int {
        if (y >= p0.y && y < p1.y || y >= p1.y && y < p0.y) {
            val param: Double = (y - p0.y) / (p1.y - p0.y)
            x[0] = mix(p0.x, p1.x, param)
            dy[0] = sign(p1.y - p0.y)
            return 1
        }
        return 0
    }

    override fun signedDistance(origin: Point2, param: DoublePtr): SignedDistance {
        val aq = origin - p0
        val ab = p1 - p0
        param.value = dotProduct(aq, ab) / ab.lengthSquared()
        val eq = (if (param.value > 0.5) p1 else p0) - origin
        val endpointDistance = eq.length()
        if (param.value > 0 && param.value < 1) {
            val orthoDistance = dotProduct(ab.getOrthonormal(false), aq)
            if (abs(orthoDistance) < endpointDistance) {
                return SignedDistance(orthoDistance, 0.0)
            }// else should not happen, if I understand this correctly...
        }
        return SignedDistance(
            nonZeroSign(crossProduct(aq, ab)) * endpointDistance,
            abs(dotProduct(ab.normalize(), eq.normalize()))
        )
    }

    override fun directionChange(param: Double): Vector2 {
        return Vector2()
    }

}