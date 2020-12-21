package me.anno.fonts.signeddistfields.algorithm

import org.joml.AABBf
import org.joml.Vector2d
import org.joml.Vector2f
import kotlin.math.max
import kotlin.math.min

object SDFMaths {

    fun median(a: Float, b: Float, c: Float): Float {
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

    fun mix(a: Float, b: Float, f: Float) = (1 - f) * a + b * f
    fun mix(a: Vector2f, b: Vector2f, f: Float) = Vector2f(mix(a.x, b.x, f), mix(a.y, b.y, f))
    fun crossProduct(a: Vector2d, b: Vector2d) = a.x * b.y - a.y * b.x
    fun dotProduct(a: Vector2d, b: Vector2d) = a.x * b.x + a.y * b.y
    fun crossProduct(a: Vector2f, b: Vector2f) = a.x * b.y - a.y * b.x
    fun dotProduct(a: Vector2f, b: Vector2f) = a.x * b.x + a.y * b.y

    fun Vector2f.getOrthonormal(polarity: Boolean = true, allowZero: Boolean = false): Vector2f {
        val length = length()
        return if (length == 0f) {
            if (allowZero) {
                Vector2f(0f)
            } else {
                if (polarity) Vector2f(0f, 1f) else Vector2f(0f, -1f)
            }
        } else if (polarity) Vector2f(-y / length, x / length) else Vector2f(y / length, -x / length)
    }

    fun union(aabb: AABBf, p1: Vector2f){
        aabb.union(p1.x, p1.y, 0f)
    }

}