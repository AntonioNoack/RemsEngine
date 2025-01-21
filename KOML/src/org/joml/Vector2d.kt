package org.joml

import org.joml.JomlMath.hash
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

@Suppress("unused")
open class Vector2d(
    @JvmField var x: Double,
    @JvmField var y: Double
) : Vector {

    constructor() : this(0.0, 0.0)
    constructor(v: Double) : this(v, v)
    constructor(v: Vector2d) : this(v.x, v.y)
    constructor(v: Vector2f) : this(v.x.toDouble(), v.y.toDouble())
    constructor(v: Vector2i) : this(v.x.toDouble(), v.y.toDouble())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(xy: DoubleArray) : this(xy[0], xy[1])
    constructor(xy: DoubleArray, offset: Int) : this(xy[offset], xy[offset + 1])
    constructor(xy: FloatArray) : this(xy[0].toDouble(), xy[1].toDouble())

    override val numComponents: Int get() = 2
    override fun getComp(i: Int): Double = get(i)
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v)
    }

    operator fun component1() = x
    operator fun component2() = y

    fun set(x: Double, y: Double): Vector2d {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Double) = set(v, v)
    fun set(v: Vector2d) = set(v.x, v.y)
    fun set(v: Vector2f) = set(v.x.toDouble(), v.y.toDouble())
    fun set(v: Vector2i) = set(v.x.toDouble(), v.y.toDouble())
    fun set(xy: DoubleArray) = set(xy[0], xy[1])
    fun set(xy: DoubleArray, offset: Int) = set(xy[offset], xy[offset + 1])
    fun set(xy: FloatArray) = set(xy[0].toDouble(), xy[1].toDouble())

    operator fun get(component: Int): Double {
        return if (component == 0) x else y
    }

    fun get(dst: Vector2f): Vector2f {
        return dst.set(this)
    }

    fun get(dst: Vector2d): Vector2d {
        return dst.set(this)
    }

    fun get(dst: DoubleArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
    }

    operator fun set(component: Int, value: Double) = setComponent(component, value)
    fun setComponent(component: Int, value: Double): Vector2d {
        if (component == 0) {
            x = value
        } else {
            y = value
        }
        return this
    }

    fun perpendicular() = set(y, -x)

    @JvmOverloads
    fun sub(x: Double, y: Double, dst: Vector2d = this): Vector2d = dst.set(this.x - x, this.y - y)

    @JvmOverloads
    fun sub(x: Double, dst: Vector2d = this): Vector2d = sub(x, x, dst)

    @JvmOverloads
    fun sub(v: Vector2f, dst: Vector2d = this): Vector2d = sub(v.x.toDouble(), v.y.toDouble(), dst)

    @JvmOverloads
    fun sub(v: Vector2d, dst: Vector2d = this): Vector2d = sub(v.x, v.y, dst)

    @JvmOverloads
    fun mul(scalar: Double, dst: Vector2d = this): Vector2d = dst.set(x * scalar, y * scalar)

    @JvmOverloads
    fun mul(x: Double, y: Double, dst: Vector2d = this): Vector2d = dst.set(x * this.x, y * this.y)

    @JvmOverloads
    fun mul(v: Vector2d, dst: Vector2d = this): Vector2d = mul(v.x, v.y, dst)

    @JvmOverloads
    fun div(scalar: Double, dst: Vector2d = this): Vector2d = mul(1.0 / scalar, dst)

    @JvmOverloads
    fun div(x: Double, y: Double, dst: Vector2d = this): Vector2d = dst.set(this.x / x, this.y / y)

    @JvmOverloads
    fun div(v: Vector2f, dst: Vector2d = this): Vector2d = div(v.x.toDouble(), v.y.toDouble(), dst)

    @JvmOverloads
    fun div(v: Vector2d, dst: Vector2d = this): Vector2d = div(v.x, v.y, dst)

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

    fun dot(v: Vector2f): Double = x * v.x + y * v.y
    fun dot(v: Vector2d): Double = x * v.x + y * v.y
    fun dot(vx: Float, vy: Float): Double = x * vx + y * vy
    fun dot(vx: Double, vy: Double): Double = x * vx + y * vy

    fun angle(v: Vector2d): Double {
        val det = x * v.y - y * v.x
        return atan2(det, dot(v))
    }

    fun rotate(radians: Double, dst: Vector2d = this): Vector2d {
        val c = cos(radians)
        val s = sin(radians)
        return dst.set(c * x - s * y, c * y + s * x)
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

    fun safeNormalize(length: Double = 1.0): Vector2d {
        normalize(length)
        if (!isFinite) set(0.0)
        return this
    }

    @JvmOverloads
    fun add(vx: Double, vy: Double, dst: Vector2d = this): Vector2d {
        return dst.set(x + vx, y + vy)
    }

    @JvmOverloads
    fun add(v: Vector2d, dst: Vector2d = this): Vector2d = add(v.x, v.y, dst)

    @JvmOverloads
    fun add(v: Vector2f, dst: Vector2d = this): Vector2d = add(v.x.toDouble(), v.y.toDouble(), dst)

    fun zero(): Vector2d = set(0.0, 0.0)

    @JvmOverloads
    fun negate(dst: Vector2d = this): Vector2d {
        dst.x = -x
        dst.y = -y
        return dst
    }

    fun mix(other: Vector2d, t: Double, dst: Vector2d = this): Vector2d {
        dst.x = x + (other.x - x) * t
        dst.y = y + (other.y - y) * t
        return dst
    }

    @JvmOverloads
    fun lerp(other: Vector2d, t: Double, dst: Vector2d = this): Vector2d {
        return mix(other, t, dst)
    }

    override fun hashCode(): Int {
        return 31 * hash(x) + hash(y)
    }

    override fun equals(other: Any?): Boolean {
        return other is Vector2d && other.x == x && other.y == y
    }

    fun equals(v: Vector2d, delta: Double): Boolean {
        return Runtime.equals(x, v.x, delta) && Runtime.equals(y, v.y, delta)
    }

    fun equals(x: Double, y: Double) = this.x == x && this.y == y

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
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        return dst
    }

    fun max(): Double {
        return max(x, y)
    }

    fun min(): Double {
        return min(x, y)
    }

    fun minComponent(): Int {
        return if (x < y) 0 else 1
    }

    fun maxComponent(): Int {
        return if (x > y) 0 else 1
    }

    @JvmOverloads
    fun floor(dst: Vector2d = this): Vector2d {
        dst.x = floor(x)
        dst.y = floor(y)
        return dst
    }

    @JvmOverloads
    fun ceil(dst: Vector2d = this): Vector2d {
        dst.x = ceil(x)
        dst.y = ceil(y)
        return dst
    }

    @JvmOverloads
    fun round(dst: Vector2d = this): Vector2d {
        dst.x = round(x)
        dst.y = round(y)
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

    fun cross(ox: Double, oy: Double): Double {
        return x * oy - y * ox
    }

    fun cross(other: Vector2d): Double {
        return cross(other.x, other.y)
    }

    fun mulAdd(f: Double, b: Vector2d, dst: Vector2d): Vector2d {
        return dst.set(x * f + b.x, y * f + b.y)
    }

    fun mulAdd(a: Vector2d, b: Vector2d, dst: Vector2d = this): Vector2d {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        return dst
    }

    operator fun plus(s: Vector2d) = add(s, Vector2d())
    operator fun minus(s: Vector2d) = sub(s, Vector2d())
    operator fun times(f: Double) = mul(f, Vector2d())
    operator fun times(s: Vector2d) = mul(s, Vector2d())

    // other should be normalized
    fun makePerpendicular(other: Vector2d): Vector2d {
        val f = dot(other) / other.lengthSquared()
        x -= other.x * f
        y -= other.y * f
        return this
    }

    fun smoothStep(v: Vector2d, t: Double, dst: Vector2d = this): Vector2d {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.smoothStep(x, v.x, t, t2, t3),
            JomlMath.smoothStep(y, v.y, t, t2, t3),
        )
    }

    fun hermite(t0: Vector2d, v1: Vector2d, t1: Vector2d, t: Double, dst: Vector2d = this): Vector2d {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.hermite(x, t0.x, v1.x, t1.x, t, t2, t3),
            JomlMath.hermite(y, t0.y, v1.y, t1.y, t, t2, t3),
        )
    }

    companion object {
        @JvmStatic
        fun lengthSquared(x: Double, y: Double) = x * x + y * y

        @JvmStatic
        fun length(x: Double, y: Double) = hypot(x, y)

        @JvmStatic
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double) = hypot(x1 - x2, y1 - y2)

        @JvmStatic
        fun distanceSquared(x1: Double, y1: Double, x2: Double, y2: Double) = lengthSquared(x1 - x2, y1 - y2)
    }
}