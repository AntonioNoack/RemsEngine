package me.anno.fonts.signeddistfields.edges

import me.anno.fonts.signeddistfields.structs.FloatPtr
import me.anno.fonts.signeddistfields.structs.SignedDistance
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.crossProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.dotProduct
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.getOrthonormal
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.mix
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.nonZeroSign
import me.anno.fonts.signeddistfields.algorithm.SDFMaths.sign
import me.anno.utils.types.Vectors.minus
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs

class LinearSegment(var p0: Vector2fc, var p1: Vector2fc) : EdgeSegment() {

    override fun clone() = LinearSegment(p0, p1)
    override fun point(param: Float) = mix(p0, p1, param)
    override fun direction(param: Float): Vector2f = p1 - p0
    override fun length(): Float = (p1 - p0).length()
    override fun reverse() {
        val t = p1
        p1 = p0
        p0 = t
    }

    override fun toString() = "[$p0 $p1]"

    override fun union(bounds: AABBf) {
        bounds.union(p0.x(), p0.y(), 0f)
        bounds.union(p1.x(), p1.y(), 0f)
    }

    override fun moveStartPoint(to: Vector2f) {
        p0 = to
    }

    override fun splitInThirds(parts: Array<EdgeSegment?>, a: Int, b: Int, c: Int) {
        parts[a] = LinearSegment(p0, point(1f / 3f))
        parts[b] = LinearSegment(point(1f / 3f), point(2f / 3f))
        parts[c] = LinearSegment(point(2f / 3f), p1)
    }

    override fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int {
        if (y >= p0.y() && y < p1.y() || y >= p1.y() && y < p0.y()) {
            val param: Float = (y - p0.y()) / (p1.y() - p0.y())
            x[0] = mix(p0.x(), p1.x(), param)
            dy[0] = sign(p1.y() - p0.y())
            return 1
        }
        return 0
    }

    override fun signedDistance(origin: Vector2fc, param: FloatPtr): SignedDistance {
        val aq = origin - p0
        val ab = p1 - p0
        param.value = dotProduct(aq, ab) / ab.lengthSquared()
        val eq = (if (param.value > 0.5) p1 else p0) - origin
        val endpointDistance = eq.length()
        if (param.value > 0 && param.value < 1) {
            val orthoDistance = dotProduct(ab.getOrthonormal(false), aq)
            if (abs(orthoDistance) < endpointDistance) {
                return SignedDistance(orthoDistance, 0f)
            }// else should not happen, if I understand this correctly...
        }
        return SignedDistance(
            nonZeroSign(crossProduct(aq, ab)) * endpointDistance,
            abs(dotProduct(ab.normalize(), eq.normalize()))
        )
    }

    override fun directionChange(param: Float): Vector2f {
        return Vector2f()
    }

}