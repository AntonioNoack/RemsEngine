package org.joml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("unused")
open class Vector2i(
    @JvmField var x: Int,
    @JvmField var y: Int
) : Vector() {

    constructor() : this(0, 0)
    constructor(d: Int) : this(d, d)
    constructor(v: Vector2i) : this(v.x, v.y)
    constructor(xy: IntArray) : this(xy[0], xy[1])

    override val numComponents: Int get() = 2
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toInt())
    }

    operator fun component1() = x
    operator fun component2() = y

    @JvmOverloads
    fun set(x: Int, y: Int = x): Vector2i {
        this.x = x
        this.y = y
        return this
    }

    fun set(v: Vector2i) = set(v.x, v.y)
    fun set(v: Vector2d) = set(v.x.toInt(), v.y.toInt())
    fun set(xy: IntArray) = set(xy[0], xy[1])

    operator fun get(component: Int): Int {
        return if (component == 0) x else y
    }

    fun setComponent(component: Int, value: Int): Vector2i {
        if (component == 0) {
            x = value
        } else {
            y = value
        }
        return this
    }

    @JvmOverloads
    fun sub(v: Vector2i, dst: Vector2i = this): Vector2i {
        dst.x = x - v.x
        dst.y = y - v.y
        return dst
    }

    @JvmOverloads
    fun sub(x: Int, y: Int, dst: Vector2i = this): Vector2i {
        dst.x = this.x - x
        dst.y = this.y - y
        return dst
    }

    fun lengthSquared(): Long {
        return x.toLong() * x + y.toLong() * y
    }

    fun length(): Double {
        return sqrt(lengthSquared().toDouble())
    }

    fun distance(v: Vector2i) = distance(v.x, v.y)

    fun distance(x: Int, y: Int): Double {
        val dx = this.x - x
        val dy = this.y - y
        return sqrt((dx.toLong() * dx + dy.toLong() * dy).toDouble())
    }

    fun distanceSquared(v: Vector2i) = distanceSquared(v.x, v.y)

    fun distanceSquared(x: Int, y: Int): Long {
        val dx = this.x - x
        val dy = this.y - y
        return dx.toLong() * dx + dy.toLong() * dy
    }

    fun gridDistance(v: Vector2i) = gridDistance(v.x, v.y)

    fun gridDistance(x: Int, y: Int): Long {
        return (abs(x - this.x) + abs(y - this.y)).toLong()
    }

    fun dot(ox: Int, oy: Int): Long = x * ox.toLong() + y * oy.toLong()
    fun dot(other: Vector2i): Long = dot(other.x, other.y)

    @JvmOverloads
    fun add(v: Vector2i, dst: Vector2i = this) = dst.set(x + v.x, y + v.y)

    @JvmOverloads
    fun add(x: Int, y: Int, dst: Vector2i = this) = dst.set(this.x + x, this.y + y)

    @JvmOverloads
    fun mul(scalar: Int, dst: Vector2i = this): Vector2i {
        dst.x = x * scalar
        dst.y = y * scalar
        return dst
    }

    @JvmOverloads
    fun mul(v: Vector2i, dst: Vector2i = this): Vector2i {
        dst.x = x * v.x
        dst.y = y * v.y
        return dst
    }

    @JvmOverloads
    fun mul(x: Int, y: Int, dst: Vector2i = this): Vector2i {
        dst.x = this.x * x
        dst.y = this.y * y
        return dst
    }

    @JvmOverloads
    fun div(scalar: Float, dst: Vector2i = this): Vector2i {
        val inv = 1f / scalar
        return dst.set((x * inv).toInt(), (y * inv).toInt())
    }

    @JvmOverloads
    fun div(scalar: Int, dst: Vector2i = this): Vector2i {
        return dst.set(x / scalar, y / scalar)
    }

    @JvmOverloads
    fun div(other: Vector2i, dst: Vector2i = this): Vector2i {
        return dst.set(x / other.x, y / other.y)
    }

    fun zero() = set(0, 0)

    @JvmOverloads
    fun negate(dst: Vector2i = this): Vector2i {
        dst.x = -x
        dst.y = -y
        return dst
    }

    @JvmOverloads
    fun min(v: Vector2i, dst: Vector2i = this): Vector2i {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector2i, dst: Vector2i = this): Vector2i {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        return dst
    }

    fun max(): Int {
        return max(x, y)
    }

    fun min(): Int {
        return min(x, y)
    }

    fun minComponent(): Int {
        return if (x < y) 0 else 1
    }

    fun maxComponent(): Int {
        return if (x > y) 0 else 1
    }

    @JvmOverloads
    fun absolute(dst: Vector2i = this) = dst.set(abs(x), abs(y))

    override fun hashCode() = x * 31 + y

    override fun equals(other: Any?): Boolean {
        return other is Vector2i && other.x == x && other.y == y
    }

    fun equals(x: Int, y: Int) = this.x == x && this.y == y

    override fun toString() = "($x,$y)"

    companion object {
        @JvmStatic
        fun lengthSquared(x: Int, y: Int) = x.toLong() * x + y.toLong() * y

        @JvmStatic
        fun length(x: Int, y: Int) = sqrt(lengthSquared(x, y).toDouble())

        @JvmStatic
        fun distance(x1: Int, y1: Int, x2: Int, y2: Int) = length(x1 - x2, y1 - y2)

        @JvmStatic
        fun distanceSquared(x1: Int, y1: Int, x2: Int, y2: Int) = lengthSquared(x1 - x2, y1 - y2)
    }
}