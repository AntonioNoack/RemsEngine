package me.anno.fonts.signeddistfields.algorithm

import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2fc
import kotlin.math.abs
import kotlin.math.sqrt

object SDFMaths {

    fun nonZeroSign(f: Float) = if (f >= 0) +1 else -1

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

    fun absDotNormalized(a: Vector2fc, b: Vector2fc) =
        abs(a.dot(b) / sqrt(a.lengthSquared() * b.lengthSquared()))

    fun absDotNormalizedXYY(a: Vector2fc, y0: Vector2fc, y1: Vector2fc) =
        abs(dotProductXXY(y0, y1, a) / sqrt(a.lengthSquared() * y0.distanceSquared(y1)))

}