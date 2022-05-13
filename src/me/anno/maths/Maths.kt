package me.anno.maths

import org.joml.*
import kotlin.math.*

@Suppress("unused")
object Maths {

    const val TAU = Math.PI * 2.0

    const val PIf = Math.PI.toFloat()
    const val TAUf = (Math.PI * 2.0).toFloat()

    const val MILLIS_TO_NANOS = 1_000_000L
    const val SECONDS_TO_NANOS = 1_000_000_000L

    const val SQRT1_2 = 0.7071067811865476
    const val SQRT1_2f = 0.70710677f
    const val SQRT2 = 1.4142135623730951
    const val SQRT2F = 1.4142135f
    const val SQRT3 = 1.7320508075688772
    const val GOLDEN_RATIO = 1.618033988749895 // phi
    const val PHI = GOLDEN_RATIO
    const val PHIf = PHI.toFloat()

    const val GoldenRatio = 1.618034f // (1f + sqrt(5f)) * 0.5f

    fun sq(x: Int) = x * x

    fun sq(x: Float) = x * x
    fun sq(x: Float, y: Float) = x * x + y * y
    fun sq(x: Float, y: Float, z: Float) = x * x + y * y + z * z

    fun sq(x: Double) = x * x
    fun sq(x: Double, y: Double) = x * x + y * y
    fun sq(x: Double, y: Double, z: Double) = x * x + y * y + z * z

    fun clamp(x: Byte, min: Byte, max: Byte) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Char, min: Char, max: Char) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Short, min: Short, max: Short) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Int, min: Int, max: Int) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Long, min: Long, max: Long) = if (x < min) min else if (x < max) x else max
    fun clamp(x: ULong, min: ULong, max: ULong) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Float, min: Float, max: Float) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Double, min: Double, max: Double) = if (x < min) min else if (x < max) x else max
    fun clamp(x: Double) = if (x < 0.0) 0.0 else if (x < 1.0) x else 1.0
    fun clamp(x: Float) = if (x < 0f) 0f else if (x < 1f) x else 1f

    fun median(a: Float, b: Float, c: Float): Float = max(min(a, b), min(max(a, b), c))
    fun median(a: Double, b: Double, c: Double): Double = max(min(a, b), min(max(a, b), c))

    fun smoothStep(x: Float): Float {
        return when {
            x <= 0f -> 0f
            x < 1f -> x * x * (3f - 2f * x)
            else -> 1f
        }
    }

    fun smoothStepUnsafe(x: Float): Float {
        return x * x * (3f - 2f * x)
    }

    fun smoothStep(a: Float, b: Float, x: Float): Float {
        return when {
            x <= 0f -> a
            x < 1f -> mix(a, b, x * x * (3f - 2f * x))
            else -> b
        }
    }

    fun smoothStepUnsafe(a: Float, b: Float, x: Float): Float {
        return mix(a, b, x * x * (3f - 2f * x))
    }

    fun clamp01(x: Float) = clamp(x, 0f, 1f)

    fun pow(base: Double, power: Double) = StrictMath.pow(base, power)
    fun log(base: Double) = StrictMath.log(base)
    fun pow(base: Float, power: Float) = StrictMath.pow(base.toDouble(), power.toDouble()).toFloat()
    fun log(base: Float) = StrictMath.log(base.toDouble()).toFloat()

    fun length(dx: Float, dy: Float) = hypot(dx, dy)
    fun length(dx: Double, dy: Double) = hypot(dx, dy)
    fun length(dx: Float, dy: Float, dz: Float) = sqrt(dx * dx + dy * dy + dz * dz)
    fun length(dx: Double, dy: Double, dz: Double) = sqrt(dx * dx + dy * dy + dz * dz)
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

    fun unmix(a: Float, b: Float, f: Float): Float {
        return (f - a) / (b - a)
    }

    fun unmix(a: Double, b: Double, f: Double): Double {
        return (f - a) / (b - a)
    }

    fun mix(a: Int, b: Int, f: Float): Int {
        return (a * (1f - f) + b * f).roundToInt()
    }

    fun mix(a: Int, b: Int, f: Int): Int {
        return (a * (255 - f) + b * f) / 255
    }

    fun mix2(a: Int, b: Int, f: Int): Int {
        return mix2(a, b, f / 255f)
    }

    fun mix2(a: Int, b: Int, f: Float): Int {
        val a2 = a * a
        val b2 = b * b
        return sqrt(mix(a2.toFloat(), b2.toFloat(), f)).roundToInt()
    }

    fun mix(a: Int, b: Int, f: Double) = a * (1.0 - f) + f * b
    fun mix(a: Long, b: Long, f: Double) = a * (1.0 - f) + f * b
    fun mix(a: Float, b: Float, f: Float, g: Float) = a * g + b * f
    fun mix(a: Double, b: Double, f: Float, g: Float) = a * g + b * f
    fun mix(a: Vector2f, b: Vector2f, f: Double, dst: Vector2f): Vector2f = a.lerp(b, f.toFloat(), dst)
    fun mix(a: Vector3f, b: Vector3f, f: Double, dst: Vector3f): Vector3f = a.lerp(b, f.toFloat(), dst)
    fun mix(a: Vector4f, b: Vector4f, f: Double, dst: Vector4f): Vector4f = a.lerp(b, f.toFloat(), dst)

    fun mixRandomly(a: Int, b: Int, f: Float): Int {
        return (a * (1f - f) + b * f + Math.random()).toInt()
    }

    fun mapClamped(input: Float, inputMin: Float, inputMax: Float, outputMin: Float, outputMax: Float): Float {
        val f0 = unmix(inputMin, inputMax, input)
        val f1 = clamp(f0, 0f, 1f)
        return mix(outputMin, outputMax, f1)
    }

    fun max(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    fun max(a: Long, b: Long): Long {
        return if (a > b) a else b
    }

    fun max(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    fun max(a: Float, b: Float, c: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    fun max(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    fun max(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    fun max(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    fun max(a: Double, b: Double): Double {
        return if (a > b) a else b
    }

    fun max(a: Double, b: Double, c: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    fun max(a: Double, b: Double, c: Double, d: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    fun max(a: Double, b: Double, c: Double, d: Double, e: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    fun max(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    fun min(a: Long, b: Long): Long {
        return if (a < b) a else b
    }

    fun min(a: Int, b: Int, c: Int): Int {
        return min(a, min(b, c))
    }

    fun min(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    fun min(a: Double, b: Double): Double {
        return if (a < b) a else b
    }

    fun min(a: Float, b: Float, c: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        return value
    }

    fun min(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }

    fun min(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        return value
    }

    fun min(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        value = if (value < f) value else f
        return value
    }

    fun absMax(a: Float, b: Float) = max(abs(a), abs(b))
    fun absMax(a: Float, b: Float, c: Float) = max(abs(a), abs(b), abs(c))
    fun absMax(a: Float, b: Float, c: Float, d: Float) = max(max(abs(a), abs(b)), max(abs(c), abs(d)))

    fun absMax(a: Double, b: Double) = max(abs(a), abs(b))
    fun absMax(a: Double, b: Double, c: Double) = max(max(abs(a), abs(b)), abs(c))
    fun absMax(a: Double, b: Double, c: Double, d: Double) = max(max(abs(a), abs(b)), max(abs(c), abs(d)))

    fun mixChannel(a: Int, b: Int, shift: Int, f: Float): Int {
        return mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    fun mixChannel(a: Int, b: Int, shift: Int, f: Int): Int {
        return mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    fun mixChannel2(a: Int, b: Int, shift: Int, f: Int): Int {
        return mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    fun mixChannel2(a: Int, b: Int, shift: Int, f: Float): Int {
        return mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    fun mixChannelRandomly(a: Int, b: Int, shift: Int, f: Float): Int {
        val ai = (a shr shift) and 0xff
        val bi = (b shr shift) and 0xff
        return clamp(mixRandomly(ai, bi, f), 0, 255) shl shift
    }

    fun convertARGB2RGBA(i: Int): Int {
        return i.shl(8) or i.ushr(24)
    }

    fun convertRGBA2ARGB(i: Int): Int {
        return i.ushr(8) or i.shl(24)
    }

    fun convertABGR2ARGB(i: Int): Int {
        return i.and(0xff00ff00.toInt()) or i.and(0xff0000).shr(16) or i.and(0xff).shl(16)
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

    fun mixARGB(a: Int, b: Int, f: Int): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    fun mixARGB2(a: Int, b: Int, f: Float): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    fun mixARGB2(a: Int, b: Int, f: Int): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    fun mixARGBRandomly(a: Int, b: Int, f: Float): Int {
        return mixChannelRandomly(a, b, 24, f) or
                mixChannelRandomly(a, b, 16, f) or
                mixChannelRandomly(a, b, 8, f) or
                mixChannelRandomly(a, b, 0, f)
    }

    fun mulAlpha(color: Int, factor: Float): Int {
        val alpha = factor * color.shr(24).and(255)
        return color.and(0xffffff) or clamp(alpha.toInt(), 0, 255).shl(24)
    }

    fun mix(a: Vector2fc, b: Vector2fc, f: Float) = Vector2f(
        mix(a.x(), b.x(), f),
        mix(a.y(), b.y(), f)
    )

    fun mix(a: Vector3fc, b: Vector3fc, f: Float) = Vector3f(
        mix(a.x(), b.x(), f),
        mix(a.y(), b.y(), f),
        mix(a.z(), b.z(), f)
    )

    fun mix(a: Vector4fc, b: Vector4fc, f: Float) = Vector4f(
        mix(a.x(), b.x(), f),
        mix(a.y(), b.y(), f),
        mix(a.z(), b.z(), f),
        mix(a.w(), b.w(), f)
    )

    fun sigmoid01(x: Float) = 1f / (1f + exp(-x))
    fun sigmoid01(x: Double) = 1.0 / (1.0 + exp(-x))
    fun sigmoid11(x: Float) = 2f / (1f + exp(-x)) - 1f
    fun sigmoid11(x: Double) = 2.0 / (1.0 + exp(-x)) - 1.0

    fun fract(f: Float) = f - floor(f)
    fun fract(d: Double) = d - floor(d)

    // fract, but in [-0.5, +0.5]
    fun roundFract(d: Float): Float {
        return fract(d + 0.5f) - 0.5f
    }

    // fract, but in [-0.5, +0.5]
    fun roundFract(d: Double): Double {
        return fract(d + 0.5) - 0.5
    }

    fun nonNegativeModulo(x: Int, div: Int): Int {
        var y = x % div
        if (y < 0) y += div
        return y
    }

    fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b
    fun roundDiv(a: Int, b: Int) = (a + b.shr(1)) / b
    fun ceilDiv(a: Long, b: Long) = (a + b - 1L) / b
    fun roundDiv(a: Long, b: Long) = (a + b.shr(1)) / b

    fun align(size: Int, rem: Int) = ceilDiv(size, rem) * rem
    fun align(size: Long, rem: Long) = ceilDiv(size, rem) * rem

}
