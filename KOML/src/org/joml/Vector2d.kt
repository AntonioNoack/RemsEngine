package org.joml

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

@Suppress("unused")
class Vector2d : Cloneable {
    var x = 0.0
    var y = 0.0

    constructor()

    @JvmOverloads
    constructor(x: Double, y: Double = x) {
        this.x = x
        this.y = y
    }

    constructor(v: Vector2d) : this(v.x, v.y)
    constructor(v: Vector2f) : this(v.x.toDouble(), v.y.toDouble())
    constructor(v: Vector2i) : this(v.x.toDouble(), v.y.toDouble())
    constructor(xy: DoubleArray) : this(xy[0], xy[1])
    constructor(xy: FloatArray) : this(xy[0].toDouble(), xy[1].toDouble())

    @JvmOverloads
    fun set(x: Double, y: Double = x): Vector2d {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Vector2d) = set(v.x, v.y)
    fun set(v: Vector2f) = set(v.x.toDouble(), v.y.toDouble())
    fun set(v: Vector2i) = set(v.x.toDouble(), v.y.toDouble())
    fun set(xy: DoubleArray) = set(xy[0], xy[1])
    fun set(xy: FloatArray) = set(xy[0].toDouble(), xy[1].toDouble())

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            else -> throw IllegalArgumentException()
        }
    }

    operator fun get(dst: Vector2f): Vector2f {
        dst.x = x.toFloat()
        dst.y = y.toFloat()
        return dst
    }

    operator fun get(dst: Vector2d = this): Vector2d {
        dst.x = x
        dst.y = y
        return dst
    }

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Double): Vector2d {
        when (component) {
            0 -> x = value
            1 -> y = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun perpendicular() = set(y, -x)

    @JvmOverloads
    fun sub(x: Double, y: Double, dst: Vector2d = this) = dst.set(this.x - x, this.y - y)

    @JvmOverloads
    fun sub(v: Vector2f, dst: Vector2d = this) = sub(v.x.toDouble(), v.y.toDouble(), dst)

    @JvmOverloads
    fun sub(v: Vector2d, dst: Vector2d = this) = sub(v.x, v.y, dst)

    @JvmOverloads
    fun mul(scalar: Double, dst: Vector2d = this) = dst.set(x * scalar, y * scalar)

    @JvmOverloads
    fun mul(x: Double, y: Double, dst: Vector2d = this) = dst.set(x * this.x, y * this.y)

    @JvmOverloads
    fun mul(v: Vector2d, dst: Vector2d = this) = mul(v.x, v.y, dst)

    @JvmOverloads
    fun div(scalar: Double, dst: Vector2d = this) = mul(1.0 / scalar, dst)

    @JvmOverloads
    fun div(x: Double, y: Double, dst: Vector2d = this) = dst.set(this.x / x, this.y / y)

    @JvmOverloads
    fun div(v: Vector2f, dst: Vector2d = this) = div(v.x.toDouble(), v.y.toDouble(), dst)

    @JvmOverloads
    fun div(v: Vector2d, dst: Vector2d = this) = div(v.x, v.y, dst)

    @JvmOverloads
    fun mul(mat: Matrix2d, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y)
    }

    @JvmOverloads
    fun mul(mat: Matrix2f, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00.toDouble() * x + mat.m10.toDouble() * y, mat.m01.toDouble() * x + mat.m11.toDouble() * y)
    }

    @JvmOverloads
    fun mulTranspose(mat: Matrix2d, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00 * x + mat.m01 * y, mat.m10 * x + mat.m11 * y)
    }

    @JvmOverloads
    fun mulTranspose(mat: Matrix2f, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00.toDouble() * x + mat.m01.toDouble() * y, mat.m10.toDouble() * x + mat.m11.toDouble() * y)
    }

    @JvmOverloads
    fun mulPosition(mat: Matrix3x2d, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00 * x + mat.m10 * y + mat.m20, mat.m01 * x + mat.m11 * y + mat.m21)
    }

    @JvmOverloads
    fun mulDirection(mat: Matrix3x2d, dst: Vector2d = this): Vector2d {
        return dst.set(mat.m00 * x + mat.m10 * y, mat.m01 * x + mat.m11 * y)
    }

    fun dot(v: Vector2d) = x * v.x + y * v.y

    fun angle(v: Vector2d): Double {
        val det = x * v.y - y * v.x
        return atan2(det, dot(v))
    }

    fun lengthSquared() = x * x + y * y
    fun length() = hypot(x, y)

    fun distance(v: Vector2d) = distance(v.x, v.y)
    fun distanceSquared(v: Vector2d) = distanceSquared(v.x, v.y)
    fun distance(v: Vector2f) = distance(v.x.toDouble(), v.y.toDouble())
    fun distanceSquared(v: Vector2f) = distanceSquared(v.x.toDouble(), v.y.toDouble())

    fun distance(x: Double, y: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        return hypot(dx, dy)
    }

    fun distanceSquared(x: Double, y: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        return dx * dx + dy * dy
    }

    @JvmOverloads
    fun normalize(dst: Vector2d = this) = mul(1.0 / length(), dst)

    @JvmOverloads
    fun normalize(length: Double, dst: Vector2d = this) = mul(length / length(), dst)

    @JvmOverloads
    fun add(x: Double, y: Double, dst: Vector2d = this): Vector2d {
        dst.x = this.x + x
        dst.y = this.y + y
        return dst
    }

    @JvmOverloads
    fun add(v: Vector2d, dst: Vector2d = this) = add(v.x, v.y, dst)

    @JvmOverloads
    fun add(v: Vector2f, dst: Vector2d = this) = add(v.x.toDouble(), v.y.toDouble(), dst)

    fun zero() = set(0.0, 0.0)

    @JvmOverloads
    fun negate(dst: Vector2d = this): Vector2d {
        dst.x = -x
        dst.y = -y
        return dst
    }

    @JvmOverloads
    fun lerp(other: Vector2d, t: Double, dst: Vector2d = this): Vector2d {
        dst.x = x + (other.x - x) * t
        dst.y = y + (other.y - y) * t
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = java.lang.Double.doubleToLongBits(x)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(y)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null) {
            false
        } else if (this.javaClass != other.javaClass) {
            false
        } else {
            other as Vector2d
            return x == other.x && y == other.y
        }
    }

    fun equals(v: Vector2d, delta: Double): Boolean {
        return if (this === v) {
            true
        } else if (!Runtime.equals(x, v.x, delta)) {
            false
        } else {
            Runtime.equals(y, v.y, delta)
        }
    }

    fun equals(x: Double, y: Double): Boolean {
        return if (java.lang.Double.doubleToLongBits(this.x) != java.lang.Double.doubleToLongBits(x)) {
            false
        } else {
            java.lang.Double.doubleToLongBits(this.y) == java.lang.Double.doubleToLongBits(y)
        }
    }

    override fun toString(): String {
        return "($x,$y)"
    }

    @JvmOverloads
    fun fma(a: Vector2d, b: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = x + a.x * b.x
        dst.y = y + a.y * b.y
        return dst
    }

    @JvmOverloads
    fun fma(a: Double, b: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = x + a * b.x
        dst.y = y + a * b.y
        return dst
    }

    @JvmOverloads
    fun min(v: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = kotlin.math.min(x, v.x)
        dst.y = kotlin.math.min(y, v.y)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = kotlin.math.max(x, v.x)
        dst.y = kotlin.math.max(y, v.y)
        return dst
    }

    fun maxComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        return if (absX >= absY) 0 else 1
    }

    fun minComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        return if (absX < absY) 0 else 1
    }

    @JvmOverloads
    fun floor(dst: Vector2d = this): Vector2d {
        dst.x = kotlin.math.floor(x)
        dst.y = kotlin.math.floor(y)
        return dst
    }

    @JvmOverloads
    fun ceil(dst: Vector2d = this): Vector2d {
        dst.x = kotlin.math.ceil(x)
        dst.y = kotlin.math.ceil(y)
        return dst
    }

    @JvmOverloads
    fun round(dst: Vector2d = this): Vector2d {
        dst.x = kotlin.math.round(x)
        dst.y = kotlin.math.round(y)
        return dst
    }

    val isFinite: Boolean
        get() = x.isFinite() && y.isFinite()

    @JvmOverloads
    fun absolute(dst: Vector2d = this): Vector2d {
        dst.x = abs(x)
        dst.y = abs(y)
        return dst
    }

    companion object {
        fun lengthSquared(x: Double, y: Double) = x * x + y * y
        fun length(x: Double, y: Double) = hypot(x, y)
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double) = hypot(x1 - x2, y1 - y2)
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double) = lengthSquared(x1 - x2, y1 - y2)
    }
}