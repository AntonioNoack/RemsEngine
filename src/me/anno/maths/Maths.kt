package me.anno.maths

import org.joml.*
import kotlin.math.*
import kotlin.random.Random

@Suppress("unused")
object Maths {

    const val TAU = PI * 2.0

    const val PIf = PI.toFloat()
    const val TAUf = (PI * 2.0).toFloat()

    const val MILLIS_TO_NANOS = 1_000_000L
    const val SECONDS_TO_NANOS = 1_000_000_000L

    const val SQRT1_2 = 0.7071067811865476
    const val SQRT1_2f = 0.70710677f
    const val SQRT2 = 1.4142135623730951
    const val SQRT2F = 1.4142135f
    const val SQRT3 = 1.7320508075688772
    const val SQRT3F = 1.7320508f
    const val GOLDEN_RATIO = 1.618033988749895 // phi

    const val SQRT_PIf = 1.7724539f
    const val INV_SQRT_PIf = 0.5641896f

    const val PHI = GOLDEN_RATIO
    const val PHIf = PHI.toFloat()

    const val GoldenRatio = 1.618034f // (1f + sqrt(5f)) * 0.5f

    @JvmStatic
    fun sq(x: Int) = x * x

    @JvmStatic
    fun sq(x: Long) = x * x

    @JvmStatic
    fun sq(x: Float) = x * x

    @JvmStatic
    fun sq(x: Float, y: Float) = x * x + y * y

    @JvmStatic
    fun sq(x: Float, y: Float, z: Float) = x * x + y * y + z * z

    @JvmStatic
    fun sq(x: Double) = x * x

    @JvmStatic
    fun sq(x: Double, y: Double) = x * x + y * y

    @JvmStatic
    fun sq(x: Int, y: Int) = x * x + y * y

    @JvmStatic
    fun sq(x: Int, y: Int, z: Int) = x * x + y * y + z * z

    @JvmStatic
    fun sq(x: Double, y: Double, z: Double) = x * x + y * y + z * z

    @JvmStatic
    fun clamp(x: Byte, min: Byte, max: Byte) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Char, min: Char, max: Char) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Short, min: Short, max: Short) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Int, min: Int, max: Int) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Long, min: Long, max: Long) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: ULong, min: ULong, max: ULong) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Float, min: Float, max: Float) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Double, min: Double, max: Double) = if (x < min) min else if (x < max) x else max

    @JvmStatic
    fun clamp(x: Double) = if (x < 0.0) 0.0 else if (x < 1.0) x else 1.0

    @JvmStatic
    fun clamp(x: Float) = if (x < 0f) 0f else if (x < 1f) x else 1f

    @JvmStatic
    fun cbrt(x: Float) = Math.cbrt(x.toDouble()).toFloat()

    @JvmStatic
    fun cbrt(x: Double) = Math.cbrt(x)

    @JvmStatic
    private val randomInst = Random(System.nanoTime())

    @JvmStatic
    fun random(): Double = synchronized(randomInst) { randomInst.nextDouble() }

    /**
     * if you want good smoothing depending on timeStep/dt, use this function
     * @param x time (seconds) times speed factor (1.0 up to 25.0 is reasonable)
     * @return interpolation factor for a call to lerp() or mix()
     * */
    @JvmStatic
    fun dtTo01(x: Float) = 1f - exp(-x)

    @JvmStatic
    fun dtTo01(x: Double) = 1.0 - exp(-x)

    @JvmStatic
    fun median(a: Float, b: Float, c: Float) = max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Double, b: Double, c: Double) = max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Int, b: Int, c: Int): Int = max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun median(a: Long, b: Long, c: Long) = max(min(a, b), min(max(a, b), c))

    @JvmStatic
    fun smoothStep(x: Float): Float {
        return when {
            x <= 0f -> 0f
            x < 1f -> x * x * (3f - 2f * x)
            else -> 1f
        }
    }

    @JvmStatic
    fun smoothStepUnsafe(x: Float): Float {
        return x * x * (3f - 2f * x)
    }

    @JvmStatic
    fun smoothStep(a: Float, b: Float, x: Float): Float {
        return when {
            x <= 0f -> a
            x < 1f -> mix(a, b, x * x * (3f - 2f * x))
            else -> b
        }
    }

    @JvmStatic
    fun smoothStepUnsafe(a: Float, b: Float, x: Float): Float {
        return mix(a, b, x * x * (3f - 2f * x))
    }

    @JvmStatic
    fun clamp01(x: Float) = clamp(x, 0f, 1f)

    @JvmStatic
    fun pow(base: Double, power: Double) = base.pow(power)

    @JvmStatic
    fun log(base: Double) = ln(base)

    @JvmStatic
    fun pow(base: Float, power: Float) = base.pow(power)

    @JvmStatic
    fun log(base: Float) = ln(base)

    /**
     * calculates log2().toInt() much quicker than usual
     * */
    @JvmStatic
    fun Float.log2i(): Int {
        val bits = toRawBits()
        val exponent = (bits shr 23) and 255
        return exponent - 127
    }

    /**
     * calculates log2().toInt() much quicker than usual
     * */
    @JvmStatic
    fun Double.log2i(): Int {
        val bits = toRawBits()
        val exponent = (bits shr 52).toInt() and 2047
        return exponent - 1023
    }

    /**
     * converts an angle from any radians into -pi to +pi
     * */
    @JvmStatic
    fun angleDifference(v0: Float): Float {
        return v0 - round(v0 / TAUf) * TAUf
    }

    /**
     * converts an angle from any radians into -pi to +pi
     * */
    @JvmStatic
    fun angleDifference(v0: Double): Double {
        return v0 - round(v0 / TAU) * TAU
    }

    @JvmStatic
    fun posMod(x: Int, y: Int) = if (x >= 0) x % y else (x % y) + y

    @JvmStatic
    fun posMod(x: Long, y: Long) = if (x >= 0) x % y else (x % y) + y

    @JvmStatic
    fun posMod(x: Float, y: Float) = if (x >= 0f) x % y else (x % y) + y

    @JvmStatic
    fun posMod(x: Double, y: Double) = if (x >= 0.0) x % y else (x % y) + y

    @JvmStatic
    fun length(dx: Float, dy: Float) = hypot(dx, dy)

    @JvmStatic
    fun length(dx: Double, dy: Double) = hypot(dx, dy)

    @JvmStatic
    fun length(dx: Float, dy: Float, dz: Float) = sqrt(dx * dx + dy * dy + dz * dz)

    @JvmStatic
    fun length(dx: Double, dy: Double, dz: Double) = sqrt(dx * dx + dy * dy + dz * dz)

    @JvmStatic
    fun distance(x0: Float, y0: Float, x1: Float, y1: Float) = length(x1 - x0, y1 - y0)

    @JvmStatic
    fun distance(x0: Double, y0: Double, x1: Double, y1: Double) = length(x1 - x0, y1 - y0)

    @JvmStatic
    fun mix(a: Short, b: Short, f: Double): Double {
        return a + (b - a) * f
    }

    @JvmStatic
    fun mix(a: Short, b: Short, f: Float): Float {
        return a + (b - a) * f
    }

    @JvmStatic
    fun mix(a: Float, b: Float, f: Float): Float {
        return a + (b - a) * f
    }

    @JvmStatic
    fun mix(a: Double, b: Double, f: Double): Double {
        return a + (b - a) * f
    }

    @JvmStatic
    fun unmix(a: Float, b: Float, f: Float): Float {
        return (f - a) / (b - a)
    }

    @JvmStatic
    fun unmix(a: Double, b: Double, f: Double): Double {
        return (f - a) / (b - a)
    }

    @JvmStatic
    fun mix(a: Int, b: Int, f: Float): Int {
        return (a + (b - a) * f).roundToInt()
    }

    @JvmStatic
    fun mix(a: Int, b: Int, f: Int): Int {
        return (a * (255 - f) + b * f) / 255
    }

    @JvmStatic
    fun mix2(a: Int, b: Int, f: Int): Int {
        return mix2(a, b, f / 255f)
    }

    @JvmStatic
    fun mix2(a: Int, b: Int, f: Float): Int {
        val a2 = a * a
        val b2 = b * b
        return sqrt(mix(a2.toFloat(), b2.toFloat(), f)).roundToInt()
    }

    @JvmStatic
    fun mix(a: Int, b: Int, f: Double) = a + (b - a) * f

    @JvmStatic
    fun mix(a: Long, b: Long, f: Double) = a + (b - a) * f

    @JvmStatic
    fun mix(a: Float, b: Float, f: Float, g: Float) = a * g + b * f

    @JvmStatic
    fun mix(a: Double, b: Double, f: Float, g: Float) = a * g + b * f

    @JvmStatic
    fun mix(a: Vector2f, b: Vector2f, f: Double, dst: Vector2f): Vector2f = a.lerp(b, f.toFloat(), dst)

    @JvmStatic
    fun mix(a: Vector3f, b: Vector3f, f: Double, dst: Vector3f): Vector3f = a.lerp(b, f.toFloat(), dst)

    @JvmStatic
    fun mix(a: Vector4f, b: Vector4f, f: Double, dst: Vector4f): Vector4f = a.lerp(b, f.toFloat(), dst)

    @JvmStatic
    fun mixRandomly(a: Int, b: Int, f: Float): Int {
        return (a * (1f - f) + b * f + random()).toInt()
    }

    @JvmStatic
    fun mapClamped(input: Float, inputMin: Float, inputMax: Float, outputMin: Float, outputMax: Float): Float {
        val f0 = unmix(inputMin, inputMax, input)
        val f1 = clamp(f0, 0f, 1f)
        return mix(outputMin, outputMax, f1)
    }

    @JvmStatic
    fun max(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Long, b: Long): Long {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Float, b: Float): Float {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    @JvmStatic
    fun max(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double): Double {
        return if (a > b) a else b
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double, e: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        return value
    }

    @JvmStatic
    fun max(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): Double {
        var value = if (a > b) a else b
        value = if (value > c) value else c
        value = if (value > d) value else d
        value = if (value > e) value else e
        value = if (value > f) value else f
        return value
    }

    @JvmStatic
    fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Long, b: Long): Long {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Int, b: Int, c: Int): Int {
        return min(a, min(b, c))
    }

    @JvmStatic
    fun min(a: Float, b: Float): Float {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Double, b: Double): Double {
        return if (a < b) a else b
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float, e: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        return value
    }

    @JvmStatic
    fun min(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float): Float {
        var value = if (a < b) a else b
        value = if (value < c) value else c
        value = if (value < d) value else d
        value = if (value < e) value else e
        value = if (value < f) value else f
        return value
    }

    @JvmStatic
    fun mix2d(v00: Float, v01: Float, v10: Float, v11: Float, fx: Float, fy: Float): Float {
        val gx = 1f - fx
        val v0x = gx * v00 + fx * v10
        val v1x = gx * v01 + fx * v11
        return (1f - fy) * v0x + fy * v1x
    }

    @JvmStatic
    fun mix2d(v00: Double, v01: Double, v10: Double, v11: Double, fx: Double, fy: Double): Double {
        val gx = 1.0 - fx
        val v0x = gx * v00 + fx * v10
        val v1x = gx * v01 + fx * v11
        return (1.0 - fy) * v0x + fy * v1x
    }

    @JvmStatic
    fun absMax(a: Float, b: Float) = max(abs(a), abs(b))

    @JvmStatic
    fun absMax(a: Float, b: Float, c: Float) = max(abs(a), abs(b), abs(c))

    @JvmStatic
    fun absMax(a: Float, b: Float, c: Float, d: Float) = max(max(abs(a), abs(b)), max(abs(c), abs(d)))

    @JvmStatic
    fun absMax(a: Double, b: Double) = max(abs(a), abs(b))

    @JvmStatic
    fun absMax(a: Double, b: Double, c: Double) = max(max(abs(a), abs(b)), abs(c))

    @JvmStatic
    fun absMax(a: Double, b: Double, c: Double, d: Double) = max(max(abs(a), abs(b)), max(abs(c), abs(d)))

    @JvmStatic
    fun mixChannel(a: Int, b: Int, shift: Int, f: Float): Int {
        return mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel(a: Int, b: Int, shift: Int, f: Int): Int {
        return mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel2(a: Int, b: Int, shift: Int, f: Int): Int {
        return mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel2(a: Int, b: Int, shift: Int, f: Float): Int {
        return mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel22d(v00: Int, v01: Int, v10: Int, v11: Int, shift: Int, fx: Float, fy: Float): Int {
        val r00 = sq((v00 shr shift) and 255).toFloat()
        val r01 = sq((v01 shr shift) and 255).toFloat()
        val r10 = sq((v10 shr shift) and 255).toFloat()
        val r11 = sq((v11 shr shift) and 255).toFloat()
        return sqrt(mix2d(r00, r01, r10, r11, fx, fy)).roundToInt() shl shift
    }

    @JvmStatic
    fun mixChannelRandomly(a: Int, b: Int, shift: Int, f: Float): Int {
        val ai = (a shr shift) and 0xff
        val bi = (b shr shift) and 0xff
        return clamp(mixRandomly(ai, bi, f), 0, 255) shl shift
    }

    @JvmStatic
    fun convertARGB2RGBA(i: Int): Int {
        return i.shl(8) or i.ushr(24)
    }

    @JvmStatic
    fun convertRGBA2ARGB(i: Int): Int {
        return i.ushr(8) or i.shl(24)
    }

    @JvmStatic
    fun convertABGR2ARGB(i: Int): Int {
        return i.and(0xff00ff00.toInt()) or i.and(0xff0000).shr(16) or i.and(0xff).shl(16)
    }

    @JvmStatic
    fun mixAngle(a: Float, b: Float, f: Float): Float {
        val d = a - b
        return when {
            d > 181 -> mixAngle(a, b + 360, f)
            d < -181 -> mixAngle(a, b - 360, f)
            else -> mix(a, b, f)
        }
    }

    @JvmStatic
    fun mixARGB(a: Int, b: Int, f: Float): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGB(a: Int, b: Int, f: Int): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGB2(a: Int, b: Int, f: Float): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGB22d(v00: Int, v01: Int, v10: Int, v11: Int, fx: Float, fy: Float): Int {
        return mixChannel22d(v00, v01, v10, v11, 24, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 16, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 8, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 0, fx, fy)
    }

    @JvmStatic
    fun mixARGB2(a: Int, b: Int, f: Int): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGBRandomly(a: Int, b: Int, f: Float): Int {
        return mixChannelRandomly(a, b, 24, f) or
                mixChannelRandomly(a, b, 16, f) or
                mixChannelRandomly(a, b, 8, f) or
                mixChannelRandomly(a, b, 0, f)
    }

    @JvmStatic
    fun mulAlpha(color: Int, factor: Float): Int {
        val alpha = factor * color.shr(24).and(255)
        return color.and(0xffffff) or clamp(alpha.toInt(), 0, 255).shl(24)
    }

    @JvmStatic
    fun mix(a: Vector2f, b: Vector2f, f: Float) = Vector2f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f)
    )

    @JvmStatic
    fun mix(a: Vector3f, b: Vector3f, f: Float) = Vector3f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f),
        mix(a.z, b.z, f)
    )

    @JvmStatic
    fun mix(a: Vector4f, b: Vector4f, f: Float) = Vector4f(
        mix(a.x, b.x, f),
        mix(a.y, b.y, f),
        mix(a.z, b.z, f),
        mix(a.w, b.w, f)
    )

    @JvmStatic
    fun sigmoid01(x: Float) = 1f / (1f + exp(-x))

    @JvmStatic
    fun sigmoid01(x: Double) = 1.0 / (1.0 + exp(-x))

    @JvmStatic
    fun sigmoid11(x: Float) = 2f / (1f + exp(-x)) - 1f

    @JvmStatic
    fun sigmoid11(x: Double) = 2.0 / (1.0 + exp(-x)) - 1.0

    @JvmStatic
    fun fract(f: Float) = f - floor(f)

    @JvmStatic
    fun fract(d: Double) = d - floor(d)

    // fract, but in [-0.5, +0.5]
    @JvmStatic
    fun roundFract(d: Float): Float {
        return fract(d + 0.5f) - 0.5f
    }

    // fract, but in [-0.5, +0.5]
    @JvmStatic
    fun roundFract(d: Double): Double {
        return fract(d + 0.5) - 0.5
    }

    @JvmStatic
    fun nonNegativeModulo(x: Int, div: Int): Int {
        var y = x % div
        if (y < 0) y += div
        return y
    }

    @JvmStatic
    fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

    @JvmStatic
    fun roundDiv(a: Int, b: Int) = (a + b.shr(1)) / b

    @JvmStatic
    fun ceilDiv(a: Long, b: Long) = (a + b - 1L) / b

    @JvmStatic
    fun roundDiv(a: Long, b: Long) = (a + b.shr(1)) / b

    @JvmStatic
    fun align(size: Int, rem: Int) = ceilDiv(size, rem) * rem

    @JvmStatic
    fun align(size: Long, rem: Long) = ceilDiv(size, rem) * rem

    @JvmStatic
    fun Int.hasFlag(flag: Int) = (this and flag) == flag

    @JvmStatic
    fun Long.hasFlag(flag: Long) = (this and flag) == flag

    @JvmStatic
    fun Int.factorial(): Int {
        if (this < 2) return 1
        var prod = this
        var n = this
        while (--n > 1) {
            prod *= n
        }
        return prod
    }

    @JvmStatic
    fun erfInv(x: Float): Float {
        // Based on "Approximating the erfInv function" by Mark Giles
        var w = -ln((1f - x) * (1f + x))
        var p: Float
        if (w < 5f) {
            w -= 2.5f
            p = 2.8102264E-8f
            p = 3.4327394E-7f + p * w
            p = -3.52338770e-06f + p * w
            p = -4.3915065E-6f + p * w
            p = 0.00021858087f + p * w
            p = -0.001253725f + p * w
            p = -0.0041776816f + p * w
            p = 0.24664073f + p * w
            p = 1.5014094f + p * w
        } else {
            w = sqrt(w) - 3f
            p = -2.0021426e-4f
            p = 1.0095056e-4f + p * w
            p = 0.0013493432f + p * w
            p = -0.0036734284f + p * w
            p = 0.0057395077f + p * w
            p = -0.00762246130f + p * w
            p = 0.0094388705f + p * w
            p = 1.001674f + p * w
            p = 2.8329768f + p * w
        }
        return p * x
    }

    @JvmStatic
    fun erf(x: Float): Float {
        val a1 = 0.2548296f
        val a2 = -0.28449672f
        val a3 = 1.4214138f
        val a4 = -1.4531521f
        val a5 = 1.0614054f
        val p = 0.3275911f

        val absX = abs(x)

        // A&S formula 7.1.26
        val t = 1f / (1f + p * absX)
        val y = 1f - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-x * x)

        return sign(x) * y
    }

    @JvmStatic
    fun dErf(x: Float) = 2f * exp(-x * x) * INV_SQRT_PIf

}
