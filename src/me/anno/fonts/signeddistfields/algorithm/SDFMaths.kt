package me.anno.fonts.signeddistfields.algorithm

import org.joml.AABBd
import org.joml.Vector2d
import kotlin.math.max
import kotlin.math.min

object SDFMaths {

    fun median(a: Float, b: Float, c: Float): Float {
        return max(min(a, b), min(max(a, b), c))
    }

    fun median(a: Double, b: Double, c: Double): Double {
        return max(min(a, b), min(max(a, b), c))
    }

    fun clamp(f: Float) = if (f < 0) 0f else if (f < 1f) f else 1f
    fun clamp(n: Int, b: Int) = if (n in 0..b) n else if (n > 0) b else 0
    fun clamp(n: Float, b: Float) = if (n in 0f..b) n else if (n > 0) b else 0
    fun clamp(f: Int, min: Int, max: Int) = if (f < min) min else if (f < max) f else max
    fun clamp(f: Float, min: Float, max: Float) = if (f < min) min else if (f < max) f else max

    fun sign(f: Int) = if (f < 0) -1 else if (f > 0) +1 else 0
    fun sign(f: Float) = if (f < 0) -1 else if (f > 0) +1 else 0
    fun sign(f: Double) = if (f < 0) -1 else if (f > 0) +1 else 0

    fun nonZeroSign(f: Int) = if (f >= 0) +1 else -1
    fun nonZeroSign(f: Float) = if (f >= 0) +1 else -1
    fun nonZeroSign(f: Double) = if (f >= 0) +1 else -1

    fun mix(a: Double, b: Double, f: Double) = (1 - f) * a + b * f
    fun mix(a: Vector2d, b: Vector2d, f: Double) = Vector2d(mix(a.x, b.x, f), mix(a.y, b.y, f))
    fun crossProduct(a: Vector2d, b: Vector2d) = a.x * b.y - a.y * b.x
    fun dotProduct(a: Vector2d, b: Vector2d) = a.x * b.x + a.y * b.y

    fun Vector2d.getOrthonormal(polarity: Boolean = true, allowZero: Boolean = false): Vector2d {
        val length = length()
        return if (length == 0.0) {
            if (allowZero) {
                Vector2d(0.0)
            } else {
                if (polarity) Vector2d(0.0, 1.0) else Vector2d(0.0, -1.0)
            }
        } else if (polarity) Vector2d(-y / length, x / length) else Vector2d(y / length, -x / length)
    }

    fun union(aabb: AABBd, p1: Vector2d){
        aabb.union(p1.x, p1.y, 0.0)
    }

}