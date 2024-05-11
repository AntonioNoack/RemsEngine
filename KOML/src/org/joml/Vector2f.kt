package org.joml

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Suppress("unused")
open class Vector2f(
    @JvmField var x: Float,
    @JvmField var y: Float
) {

    constructor() : this(0f, 0f)
    constructor(v: Float) : this(v, v)
    constructor(v: Vector2f) : this(v.x, v.y)
    constructor(v: Vector2i) : this(v.x.toFloat(), v.y.toFloat())
    constructor(xy: FloatArray) : this(xy[0], xy[1])
    constructor(v: Vector2d) : this(v.x.toFloat(), v.y.toFloat())

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
        if (component == 0) {
            x = value
        } else {
            y = value
        }
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
        return kotlin.math.atan2(det, dot)
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

    @JvmOverloads
    fun normalize(length: Float, dst: Vector2f = this) = mul(length / length(), dst)

    @JvmOverloads
    fun add(v: Vector2f, dst: Vector2f = this) = add(v.x, v.y, dst)

    @JvmOverloads
    fun add(x: Float, y: Float, dst: Vector2f = this) = dst.set(this.x + x, this.y + y)

    fun zero() = set(0f, 0f)

    @JvmOverloads
    fun negate(dst: Vector2f = this) = dst.set(-x, -y)

    @JvmOverloads
    fun mul(scalar: Float, dst: Vector2f = this) = dst.set(x * scalar, y * scalar)

    @JvmOverloads
    fun mul(x: Float, y: Float, dst: Vector2f = this) = dst.set(this.x * x, this.y * y)

    @JvmOverloads
    fun mul(v: Vector2f, dst: Vector2f = this) =
        dst.set(x * v.x, y * v.y)

    @JvmOverloads
    fun div(v: Vector2f, dst: Vector2f = this) =
        dst.set(x / v.x, y / v.y)

    @JvmOverloads
    fun div(scalar: Float, dst: Vector2f = this) = dst.mul(1f / scalar)

    @JvmOverloads
    fun div(x: Float, y: Float, dst: Vector2f = this) =
        dst.set(this.x / x, this.y / y)

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

    @JvmOverloads
    fun lerp(other: Vector2f, t: Float, dst: Vector2f = this): Vector2f {
        dst.x = x + (other.x - x) * t
        dst.y = y + (other.y - y) * t
        return dst
    }

    override fun hashCode(): Int {
        return 31 * x.toBits() + y.toBits()
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
        dst.x = kotlin.math.min(x, v.x)
        dst.y = kotlin.math.min(y, v.y)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector2f, dst: Vector2f = this): Vector2f {
        dst.x = kotlin.math.max(x, v.x)
        dst.y = kotlin.math.max(y, v.y)
        return dst
    }

    fun maxComponent(): Int {
        val absX = kotlin.math.abs(x)
        val absY = kotlin.math.abs(y)
        return if (absX >= absY) 0 else 1
    }

    fun minComponent(): Int {
        val absX = kotlin.math.abs(x)
        val absY = kotlin.math.abs(y)
        return if (absX < absY) 0 else 1
    }

    @JvmOverloads
    fun floor(dst: Vector2f = this): Vector2f {
        dst.x = kotlin.math.floor(x)
        dst.y = kotlin.math.floor(y)
        return dst
    }

    @JvmOverloads
    fun ceil(dst: Vector2f = this): Vector2f {
        dst.x = kotlin.math.ceil(x)
        dst.y = kotlin.math.ceil(y)
        return dst
    }

    @JvmOverloads
    fun round(dst: Vector2f = this): Vector2f {
        dst.x = kotlin.math.round(x)
        dst.y = kotlin.math.round(y)
        return dst
    }

    val isFinite: Boolean
        get() = x.isFinite() && y.isFinite()

    @JvmOverloads
    fun absolute(dst: Vector2f = this): Vector2f {
        dst.x = kotlin.math.abs(x)
        dst.y = kotlin.math.abs(y)
        return dst
    }

    fun dot2(x: Float, y: Float) = this.x * x + this.y * y

    fun cross(other: Vector2f): Float {
        return x * other.y - y * other.x
    }

    fun mulAdd(f: Float, b: Vector2f, dst: Vector2f): Vector2f {
        return dst.set(x * f + b.x, y * f + b.y)
    }

    operator fun plus(s: Vector2f) = Vector2f(x + s.x, y + s.y)
    operator fun minus(s: Vector2f) = Vector2f(x - s.x, y - s.y)
    operator fun times(f: Float) = Vector2f(x * f, y * f)
    operator fun times(s: Vector2f) = Vector2f(x * s.x, y * s.y)

    fun makePerpendicular(other: Vector2f): Vector2f {
        val f = dot(other)
        x -= other.x * f
        y -= other.y * f
        return this
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