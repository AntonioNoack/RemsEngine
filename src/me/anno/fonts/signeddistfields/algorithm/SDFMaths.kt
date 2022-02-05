package me.anno.fonts.signeddistfields.algorithm

import org.joml.AABBf
import org.joml.Vector2dc
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object SDFMaths {

    fun nonZeroSign(f: Float) = if (f >= 0) +1 else -1

    fun mix(a: Float, b: Float, f: Float) = (1 - f) * a + b * f
    fun mix(a: Vector2fc, b: Vector2fc, f: Float) = Vector2f(mix(a.x(), b.x(), f), mix(a.y(), b.y(), f))
    fun crossProduct(a: Vector2fc, b: Vector2fc) = a.x() * b.y() - a.y() * b.x()

    fun crossProductXYY(x: Vector2fc, y0: Vector2fc, y1: Vector2fc) =
        x.x() * (y0.y() - y1.y()) - x.y() * (y0.x() - y1.x())

    fun dotProductXXY(x0: Vector2fc, x1: Vector2fc, y: Vector2fc) =
        x0.dot(y) - x1.dot(y)

    fun Vector2fc.getOrthonormal(polarity: Boolean, allowZero: Boolean, dst: Vector2f = Vector2f()): Vector2f {
        val length = length()
        return when {
            length == 0f -> dst.set(
                0f, when {
                    allowZero -> 0f
                    polarity -> 1f
                    else -> -1f
                }
            )
            polarity -> dst.set(-y() / length, x() / length)
            else -> dst.set(y() / length, -x() / length)
        }
    }

    fun union(aabb: AABBf, p1: Vector2fc) {
        aabb.union(p1.x(), p1.y(), 0f)
    }

    fun absDotNormalized(a: Vector2fc, b: Vector2fc): Float {
        return abs(a.dot(b) / sqrt(a.lengthSquared() * b.lengthSquared()))
    }

    fun absDotNormalizedXYY(a: Vector2fc, y0: Vector2fc, y1: Vector2fc): Float {
        return abs(dotProductXXY(y0, y1, a) / sqrt(a.lengthSquared() * y0.distanceSquared(y1)))
    }

}