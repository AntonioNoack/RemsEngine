package me.anno.utils

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

object Maths {

    val GoldenRatio = (1f + sqrt(5f)) * 0.5f

    fun sq(x: Int) = x*x

    fun sq(x: Float) = x * x
    fun sq(x: Float, y: Float) = x * x + y * y
    fun sq(x: Float, y: Float, z: Float) = x * x + y * y + z * z

    fun sq(x: Double) = x * x
    fun sq(x: Double, y: Double) = x * x + y * y
    fun sq(x: Double, y: Double, z: Double) = x * x + y * y + z * z

    fun clamp(x: Int, min: Int, max: Int) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Long, min: Long, max: Long) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Float, min: Float, max: Float) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Double, min: Double, max: Double) = if (x < min) min else if (x < max) x else max

    fun clamp01(x: Float) = clamp(x, 0f, 1f)

    fun pow(base: Float, power: Float) = StrictMath.pow(base.toDouble(), power.toDouble()).toFloat()

    fun length(dx: Float, dy: Float) = sqrt(dx * dx + dy * dy)
    fun length(dx: Double, dy: Double) = sqrt(dx * dx + dy * dy)
    fun length(dx: Float, dy: Float, dz: Float) = sqrt(dx * dx + dy * dy + dz * dz)
    fun distance(x0: Float, y0: Float, x1: Float, y1: Float) = length(x1 - x0, y1 - y0)
    fun distance(x0: Double, y0: Double, x1: Double, y1: Double) = length(x1 - x0, y1 - y0)
    fun mix(a: Short, b: Short, f: Double): Double {
        return a * (1f - f) + b * f
    }

    fun mix(a: Short, b: Short, f: Float): Float {
        return a * (1f - f) + b * f
    }

    fun mix(a: Float, b: Float, f: Float): Float {
        return a * (1f - f) + b * f
    }

    fun mix(a: Double, b: Double, f: Double): Double {
        return a * (1f - f) + b * f
    }

    fun mix(a: Int, b: Int, f: Float): Int {
        return (a * (1.0 - f) + b * f).roundToInt()
    }

    fun mixChannel(a: Int, b: Int, shift: Int, f: Float): Int {
        return clamp(mix((a shr shift) and 0xff, (b shr shift) and 0xff, f), 0, 255) shl shift
    }

    fun mixAngle(a: Float, b: Float, f: Float): Float {
        val d = a - b
        return when {
            d > 181 -> mixAngle(a, b + 360, f)
            d < -181 -> mixAngle(a, b - 360, f)
            else -> mix(a, b, f)
        }
    }

    fun mixARGB(a: Int, b: Int, f: Float): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    fun mix(a: Vector2f, b: Vector2f, f: Float) = Vector2f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f)
    )

    fun mix(a: Vector3f, b: Vector3f, f: Float) = Vector3f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f),
        mix(a.z, b.z, f)
    )

    fun mix(a: Vector4f, b: Vector4f, f: Float) = Vector4f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f),
        mix(a.z, b.z, f),
        mix(a.w, b.w, f)
    )

    fun sigmoid01(x: Float) = 1f / (1f + exp(-x))
    fun sigmoid01(x: Double) = 1.0 / (1.0 + exp(-x))
    fun sigmoid11(x: Float) = 2f / (1f + exp(-x)) - 1f
    fun sigmoid11(x: Double) = 2.0 / (1.0 + exp(-x)) - 1.0

    fun fract(f: Float) = f - floor(f)
    fun fract(d: Double) = d - floor(d)

}
