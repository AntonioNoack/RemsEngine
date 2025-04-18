package org.joml

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Vector2f(
    @JvmField var x: Float,
    @JvmField var y: Float
) : Vector {

    constructor() : this(0f, 0f)
    constructor(v: Float) : this(v, v)
    constructor(v: Vector2f) : this(v.x, v.y)
    constructor(v: Vector2i) : this(v.x, v.y)
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(xy: FloatArray, offset: Int) : this(xy[offset], xy[offset + 1])
    constructor(xy: FloatArray) : this(xy, 0)
    constructor(v: Vector2d) : this(v.x.toFloat(), v.y.toFloat())

    override val numComponents: Int get() = 2
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toFloat())
    }

    operator fun component1() = x
    operator fun component2() = y

    @JvmOverloads
    fun set(x: Float, y: Float = x): Vector2f {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Double) = set(v, v)
    fun set(x: Double, y: Double) = set(x.toFloat(), y.toFloat())
    fun set(v: Vector2f) = set(v.x, v.y)
    fun set(v: Vector2i) = set(v.x.toFloat(), v.y.toFloat())
    fun set(v: Vector2d) = set(v.x.toFloat(), v.y.toFloat())
    fun set(xy: FloatArray) = set(xy[0], xy[1])
    fun set(xy: FloatArray, i: Int) = set(xy[i], xy[i + 1])

    operator fun get(component: Int): Float {
        return if (component == 0) x else y
    }

    fun get(dst: Vector2f) = dst.set(x, y)
    fun get(dst: Vector2d): Vector2d = dst.set(x.toDouble(), y.toDouble())
    fun get(dst: FloatArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
    }

    operator fun set(component: Int, value: Float) = setComponent(component, value)
    fun setComponent(component: Int, value: Float): Vector2f {
        if (component == 0) x = value
        else y = value
        return this
    }

    fun perpendicular(): Vector2f {
        val tmp = y
        y = -x
        x = tmp
        return this
    }

    @JvmOverloads
    fun sub(v: Vector2f, dst: Vector2f = this) = sub(v.x, v.y, dst)

    @JvmOverloads
    fun sub(x: Float, y: Float, dst: Vector2f = this): Vector2f {
        dst.x = this.x - x
        dst.y = this.y - y
        return dst
    }

    fun dot(v: Vector2f): Float = x * v.x + y * v.y
    fun dot(v: Vector2d): Double = x * v.x + y * v.y
    fun dot(vx: Float, vy: Float): Float = x * vx + y * vy
    fun dot(vx: Double, vy: Double): Double = x * vx + y * vy

    fun angle(v: Vector2f): Float {
        val dot = x * v.x + y * v.y
        val det = x * v.y - y * v.x
        return atan2(det, dot)
    }

    fun angleCos(v: Vector2f): Float = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Float, vy: Float): Float {
        return dot(vx, vy) / sqrt(lengthSquared() * lengthSquared(vx, vy))
    }

    fun rotate(radians: Float, dst: Vector2f = this): Vector2f {
        val c = cos(radians)
        val s = sin(radians)
        return dst.set(c * x - s * y, c * y + s * x)
    }

    fun lengthSquared() = x * x + y * y
    fun length() = hypot(x, y)
    fun distance(v: Vector2f) = hypot(x - v.x, y - v.y)
    fun distanceSquared(v: Vector2f) = distanceSquared(v.x, v.y)
    fun distance(x: Float, y: Float) = hypot(this.x - x, this.y - y)
    fun distanceSquared(x: Float, y: Float) = lengthSquared(this.x - x, this.y - y)

    @JvmOverloads
    fun normalize(dst: Vector2f = this) = mul(1f / length(), dst)

    fun safeNormalize(length: Float = 1f): Vector2f {
        normalize(length)
        if (!isFinite) set(0f)
        return this
    }

    @JvmOverloads
    fun normalize(length: Float, dst: Vector2f = this) = mul(length / length(), dst)

    @JvmOverloads
    fun add(v: Vector2f, dst: Vector2f = this) = add(v.x, v.y, dst)

    @JvmOverloads
    fun add(x: Float, y: Float, dst: Vector2f = this) = dst.set(this.x + x, this.y + y)

    fun zero(): Vector2f = set(0f, 0f)

    @JvmOverloads
    fun negate(dst: Vector2f = this) = dst.set(-x, -y)

    @JvmOverloads
    fun mul(scalar: Float, dst: Vector2f = this) = dst.set(x * scalar, y * scalar)

    @JvmOverloads
    fun mul(x: Float, y: Float, dst: Vector2f = this) = dst.set(this.x * x, this.y * y)

    @JvmOverloads
    fun mul(v: Vector2f, dst: Vector2f = this) = dst.set(x * v.x, y * v.y)

    @JvmOverloads
    fun div(v: Vector2f, dst: Vector2f = this) = dst.set(x / v.x, y / v.y)

    @JvmOverloads
    fun div(scalar: Float, dst: Vector2f = this) = dst.mul(1f / scalar)

    @JvmOverloads
    fun div(x: Float, y: Float, dst: Vector2f = this) = dst.set(this.x / x, this.y / y)

    @JvmOverloads
    fun mul(mat: Matrix2f, dst: Vector2f = this) =
        dst.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y)

    @JvmOverloads
    fun mul(mat: Matrix2d, dst: Vector2f = this) =
        dst.set((mat.m00 * x + mat.m10 * y).toFloat(), (mat.m01 * x + mat.m11 * y).toFloat())

    @JvmOverloads
    fun mulTranspose(mat: Matrix2f, dst: Vector2f = this) =
        dst.set(mat.m00 * x + mat.m01 * y, mat.m10 * x + mat.m11 * y)

    @JvmOverloads
    fun mulPosition(mat: Matrix3x2f, dst: Vector2f = this) =
        dst.set(mat.m00 * x + mat.m10 * y + mat.m20, mat.m01 * x + mat.m11 * y + mat.m21)

    @JvmOverloads
    fun mulDirection(mat: Matrix3x2f, dst: Vector2f = this) =
        dst.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y)

    fun mix(other: Vector2f, t: Float, dst: Vector2f = this): Vector2f {
        dst.x = x + (other.x - x) * t
        dst.y = y + (other.y - y) * t
        return dst
    }

    @JvmOverloads
    fun lerp(other: Vector2f, t: Float, dst: Vector2f = this): Vector2f {
        return mix(other, t, dst)
    }

    override fun hashCode(): Int {
        return 31 * x.toRawBits() + y.toRawBits()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is Vector2f && other.x == x && other.y == y)
    }

    fun equals(v: Vector2f, delta: Float): Boolean {
        return (this === v) || (Runtime.equals(x, v.x, delta) && Runtime.equals(y, v.y, delta))
    }

    fun equals(x: Float, y: Float): Boolean {
        return this.x == x && this.y == y
    }

    override fun toString() = "($x,$y)"

    @JvmOverloads
    fun fma(a: Vector2f, b: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = x + a.x * b.x
        dst.y = y + a.y * b.y
        return dst
    }

    @JvmOverloads
    fun fma(a: Float, b: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = x + a * b.x
        dst.y = y + a * b.y
        return dst
    }

    @JvmOverloads
    fun min(v: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        return dst
    }

    fun max(): Float {
        return max(x, y)
    }

    fun min(): Float {
        return min(x, y)
    }

    fun minComponent(): Int {
        return if (x < y) 0 else 1
    }

    fun maxComponent(): Int {
        return if (x > y) 0 else 1
    }

    @JvmOverloads
    fun floor(dst: Vector2f = this): Vector2f {
        dst.x = floor(x)
        dst.y = floor(y)
        return dst
    }

    @JvmOverloads
    fun ceil(dst: Vector2f = this): Vector2f {
        dst.x = ceil(x)
        dst.y = ceil(y)
        return dst
    }

    @JvmOverloads
    fun round(dst: Vector2f = this): Vector2f {
        dst.x = round(x)
        dst.y = round(y)
        return dst
    }

    val isFinite: Boolean
        get() = x.isFinite() && y.isFinite()

    @JvmOverloads
    fun absolute(dst: Vector2f = this): Vector2f {
        dst.x = abs(x)
        dst.y = abs(y)
        return dst
    }

    fun cross(other: Vector2f): Float {
        return dot(other.y, -other.x)
    }

    fun cross(vx: Float, vy: Float): Float {
        return dot(vy, -vx)
    }

    fun mulAdd(f: Float, b: Vector2f, dst: Vector2f): Vector2f {
        return dst.set(x * f + b.x, y * f + b.y)
    }

    fun mulAdd(a: Vector2f, b: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        return dst
    }

    operator fun plus(s: Vector2f) = add(s, Vector2f())
    operator fun minus(s: Vector2f) = sub(s, Vector2f())
    operator fun times(f: Float) = mul(f, Vector2f())
    operator fun times(s: Vector2f) = mul(s, Vector2f())

    fun makePerpendicular(other: Vector2f): Vector2f {
        val f = dot(other) / other.lengthSquared()
        x -= other.x * f
        y -= other.y * f
        return this
    }

    fun smoothStep(v: Vector2f, t: Float, dst: Vector2f = this): Vector2f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.smoothStep(x, v.x, t, t2, t3),
            JomlMath.smoothStep(y, v.y, t, t2, t3),
        )
    }

    fun hermite(t0: Vector2f, v1: Vector2f, t1: Vector2f, t: Float, dst: Vector2f = this): Vector2f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.hermite(x, t0.x, v1.x, t1.x, t, t2, t3),
            JomlMath.hermite(y, t0.y, v1.y, t1.y, t, t2, t3),
        )
    }

    companion object {

        @JvmStatic
        fun lengthSquared(x: Float, y: Float) = x * x + y * y

        @JvmStatic
        fun length(x: Float, y: Float) = hypot(x, y)

        @JvmStatic
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = hypot(x1 - x2, y1 - y2)

        @JvmStatic
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float) = lengthSquared(x1 - x2, y1 - y2)
    }
}