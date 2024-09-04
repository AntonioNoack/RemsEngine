package org.joml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("unused")
open class Vector4i(
    @JvmField var x: Int,
    @JvmField var y: Int,
    @JvmField var z: Int,
    @JvmField var w: Int
) : Vector() {

    constructor() : this(0, 0, 0, 1)
    constructor(v: Vector4i) : this(v.x, v.y, v.z, v.w)
    constructor(v: Vector3i, w: Int) : this(v.x, v.y, v.z, w)
    constructor(v: Vector2i, z: Int, w: Int) : this(v.x, v.y, z, w)
    constructor(s: Int) : this(s, s, s, s)
    constructor(xyzw: IntArray) : this(xyzw[0], xyzw[1], xyzw[2], xyzw[3])

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toInt())
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z
    operator fun component4() = w

    fun set(v: Vector4i) = set(v.x, v.y, v.z, v.w)
    fun set(v: Vector4d) = set(v.x.toInt(), v.y.toInt(), v.z.toInt(), v.w.toInt())
    fun set(v: Vector3i, w: Int) = set(v.x, v.y, v.z, w)
    fun set(v: Vector2i, z: Int, w: Int) = set(v.x, v.y, z, w)
    fun set(s: Int) = set(s, s, s, s)
    fun set(v: IntArray) = set(v[0], v[1], v[2], v[3])

    fun set(x: Int, y: Int, z: Int, w: Int): Vector4i {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    operator fun get(component: Int): Int {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> w
        }
    }

    fun max(): Int {
        return max(max(x, y), max(z, w))
    }

    fun min(): Int {
        return min(min(x, y), min(z, w))
    }

    fun minComponent(): Int {
        return when (min()) {
            x -> 0
            y -> 1
            z -> 2
            else -> 3
        }
    }

    fun maxComponent(): Int {
        return when (max()) {
            x -> 0
            y -> 1
            z -> 2
            else -> 3
        }
    }

    fun setComponent(component: Int, value: Int): Vector4i {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> w = value
        }
        return this
    }

    fun sub(v: Vector4i, dst: Vector4i = this): Vector4i {
        return sub(v.x, v.y, v.z, v.w, dst)
    }

    fun sub(x: Int, y: Int, z: Int, w: Int, dst: Vector4i = this): Vector4i {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    fun add(v: Vector4i, dst: Vector4i = this): Vector4i {
        return add(v.x, v.y, v.z, v.w, dst)
    }

    fun add(x: Int, y: Int, z: Int, w: Int, dst: Vector4i = this): Vector4i {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    fun mul(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        dst.w = w * v.w
        return dst
    }

    fun div(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        dst.w = w / v.w
        return dst
    }

    fun mul(scalar: Int, dst: Vector4i = this): Vector4i {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        dst.w = w * scalar
        return dst
    }

    fun div(scalar: Float, dst: Vector4i = this): Vector4i {
        val inv = 1f / scalar
        dst.x = (x.toFloat() * inv).toInt()
        dst.y = (y.toFloat() * inv).toInt()
        dst.z = (z.toFloat() * inv).toInt()
        dst.w = (w.toFloat() * inv).toInt()
        return dst
    }

    fun div(scalar: Int, dst: Vector4i = this): Vector4i {
        dst.x = x / scalar
        dst.y = y / scalar
        dst.z = z / scalar
        dst.w = w / scalar
        return dst
    }

    fun lengthSquared(): Long {
        return x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w
    }

    fun length() = sqrt(lengthSquared().toFloat()).toDouble()
    fun distance(v: Vector4i): Double = distance(v.x, v.y, v.z, v.w)
    fun distance(x: Int, y: Int, z: Int, w: Int): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return length(dx, dy, dz, dw)
    }

    fun gridDistance(v: Vector4i): Long {
        return (abs(v.x - x) + abs(v.y - y) + abs(v.z - z) + abs(
            v.w - w
        )).toLong()
    }

    fun gridDistance(x: Int, y: Int, z: Int, w: Int): Long {
        val dx = this.x - x.toLong()
        val dy = this.y - y.toLong()
        val dz = this.z - z.toLong()
        val dw = this.w - w.toLong()
        return abs(dx) + abs(dy) + abs(dz) + abs(dw)
    }

    fun distanceSquared(v: Vector4i): Long {
        return distanceSquared(v.x, v.y, v.z, v.w)
    }

    fun distanceSquared(x: Int, y: Int, z: Int, w: Int): Long {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return lengthSquared(dx, dy, dz, dw)
    }

    fun dot(v: Vector4i): Int {
        return x * v.x + y * v.y + z * v.z + w * v.w
    }

    fun zero(): Vector4i = set(0, 0, 0, 0)
    fun negate(dst: Vector4i = this): Vector4i {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = -w
        return dst
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    @JvmOverloads
    fun min(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        dst.w = min(w, v.w)
        return dst
    }

    @JvmOverloads
    fun max(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        dst.w = max(w, v.w)
        return dst
    }

    @JvmOverloads
    fun absolute(dst: Vector4i = this): Vector4i {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        dst.w = abs(w)
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + w
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Vector4i && equals(other.x, other.y, other.z, other.w)
    }

    fun equals(x: Int, y: Int, z: Int, w: Int): Boolean {
        return x == this.x && y == this.y && z == this.z && w == this.w
    }

    companion object {

        @JvmStatic
        fun lengthSquared(x: Int, y: Int, z: Int, w: Int): Long {
            return x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w
        }

        @JvmStatic
        fun length(x: Int, y: Int, z: Int, w: Int): Double {
            return sqrt((x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w).toDouble())
        }

        @JvmStatic
        fun distance(x1: Int, y1: Int, z1: Int, w1: Int, x2: Int, y2: Int, z2: Int, w2: Int): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return sqrt(lengthSquared(dx, dy, dz, dw).toDouble())
        }

        @JvmStatic
        fun distanceSquared(x1: Int, y1: Int, z1: Int, w1: Int, x2: Int, y2: Int, z2: Int, w2: Int): Long {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return lengthSquared(dx, dy, dz, dw)
        }
    }
}