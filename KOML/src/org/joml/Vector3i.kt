package org.joml

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Suppress("unused")
open class Vector3i(
    @JvmField var x: Int,
    @JvmField var y: Int,
    @JvmField var z: Int
) : Vector {

    constructor() : this(0, 0, 0)
    constructor(d: Int) : this(d, d, d)
    constructor(v: Vector3i) : this(v.x, v.y, v.z)
    constructor(v: Vector2i, z: Int) : this(v.x, v.y, z)
    constructor(xyz: IntArray) : this(xyz[0], xyz[1], xyz[2])

    override val numComponents: Int get() = 3
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toInt())
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    fun set(v: Vector3i): Vector3i {
        x = v.x
        y = v.y
        z = v.z
        return this
    }

    fun set(v: Vector3d): Vector3i {
        x = v.x.toInt()
        y = v.y.toInt()
        z = v.z.toInt()
        return this
    }

    fun set(v: Vector2i, z: Int): Vector3i {
        x = v.x
        y = v.y
        this.z = z
        return this
    }

    fun set(d: Int): Vector3i {
        x = d
        y = d
        z = d
        return this
    }

    fun set(x: Int, y: Int, z: Int): Vector3i {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(xyz: IntArray): Vector3i {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
        return this
    }

    operator fun get(component: Int): Int {
        return when (component) {
            0 -> x
            1 -> y
            else -> z
        }
    }

    fun setComponent(component: Int, value: Int): Vector3i {
        when (component) {
            0 -> x = value
            1 -> y = value
            else -> z = value
        }
        return this
    }

    fun sub(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        return dst
    }

    fun sub(x: Int, y: Int, z: Int, dst: Vector3i = this): Vector3i {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        return dst
    }

    fun add(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        return dst
    }

    fun add(x: Int, y: Int, z: Int, dst: Vector3i = this): Vector3i {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        return dst
    }

    fun mul(scalar: Int, dst: Vector3i = this): Vector3i {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        return dst
    }

    fun mul(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        return dst
    }

    fun mul(x: Int, y: Int, z: Int, dst: Vector3i = this): Vector3i {
        dst.x = this.x * x
        dst.y = this.y * y
        dst.z = this.z * z
        return dst
    }

    fun div(scalar: Float, dst: Vector3i = this): Vector3i {
        val inv = 1f / scalar
        return dst.set((x.toFloat() * inv).toInt(), (y.toFloat() * inv).toInt(), (z.toFloat() * inv).toInt())
    }

    fun div(scalar: Int, dst: Vector3i = this): Vector3i {
        return dst.set(x / scalar, y / scalar, z / scalar)
    }

    fun div(other: Vector3i, dst: Vector3i = this): Vector3i {
        return dst.set(x / other.x, y / other.y, z / other.z)
    }

    fun lengthSquared(): Long {
        return x.toLong() * x + y.toLong() * y + z.toLong() * z
    }

    fun length(): Double {
        return sqrt(lengthSquared().toFloat()).toDouble()
    }

    fun distance(v: Vector3i): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return length(dx, dy, dz)
    }

    fun distance(x: Int, y: Int, z: Int): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return lengthSquared(dx, dy, dz).toDouble()
    }

    fun gridDistance(v: Vector3i): Long {
        return (abs(v.x - x) + abs(v.y - y) + abs(v.z - z)).toLong()
    }

    fun gridDistance(x: Int, y: Int, z: Int): Long {
        return (abs(x - this.x) + abs(y - this.y) + abs(z - this.z)).toLong()
    }

    fun dot(ox: Int, oy: Int, oz: Int): Long = x * ox.toLong() + y * oy.toLong() + z * oz.toLong()
    fun dot(other: Vector3i): Long = dot(other.x, other.y, other.z)

    fun distanceSquared(v: Vector3i): Long {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return lengthSquared(dx, dy, dz)
    }

    fun distanceSquared(x: Int, y: Int, z: Int): Long {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return lengthSquared(dx, dy, dz)
    }

    fun zero(): Vector3i = set(0)

    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun negate(dst: Vector3i = this): Vector3i {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        return dst
    }

    fun min(v: Vector3i): Vector3i {
        x = min(x, v.x)
        y = min(y, v.y)
        z = min(z, v.z)
        return this
    }

    fun min(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        return dst
    }

    fun max(v: Vector3i): Vector3i {
        x = max(x, v.x)
        y = max(y, v.y)
        z = max(z, v.z)
        return this
    }

    fun max(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        return dst
    }

    fun max(): Int {
        return max(max(x, y), z)
    }

    fun min(): Int {
        return min(min(x, y), z)
    }

    fun minComponent(): Int {
        return when (min()) {
            x -> 0
            y -> 1
            else -> 2
        }
    }

    fun maxComponent(): Int {
        return when (max()) {
            x -> 0
            y -> 1
            else -> 2
        }
    }

    fun absolute(dst: Vector3i = this): Vector3i {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        return dst
    }

    fun cross(v: Vector3i, dst: Vector3i = this): Vector3i {
        val rx = y * v.z - z * v.y
        val ry = z * v.x - x * v.z
        val rz = x * v.y - y * v.x
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    operator fun plus(s: Vector3i) = add(s, Vector3i())
    operator fun plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun minus(s: Vector3i) = sub(s, Vector3i())
    operator fun times(o: Int): Vector3i = mul(o, Vector3i())
    operator fun times(s: Float) = Vector3f(x * s, y * s, z * s)
    operator fun times(o: Vector3i): Vector3i = mul(o, Vector3i())
    operator fun minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun div(o: Vector3i): Vector3i = div(o, Vector3i())
    operator fun div(o: Int): Vector3i = div(o, Vector3i())

    fun shr(i: Int): Vector3i = set(x shr i, y shr i, z shr i)
    fun shl(i: Int): Vector3i = set(x shl i, y shl i, z shl i)

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Vector3i && other.x == x && other.y == y && other.z == z
    }

    fun equals(x: Int, y: Int, z: Int): Boolean {
        return x == this.x && y == this.y && z == this.z
    }

    companion object {

        @JvmStatic
        fun lengthSquared(x: Int, y: Int, z: Int): Long {
            return x.toLong() * x + y.toLong() * y + z.toLong() * z
        }

        @JvmStatic
        fun length(x: Int, y: Int, z: Int): Double {
            return sqrt(lengthSquared(x, y, z).toFloat()).toDouble()
        }

        @JvmStatic
        fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
            return sqrt(lengthSquared(x1 - x2, y1 - y2, z1 - z2).toFloat()).toDouble()
        }

        @JvmStatic
        fun distanceSquared(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Long {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return lengthSquared(dx, dy, dz)
        }
    }
}