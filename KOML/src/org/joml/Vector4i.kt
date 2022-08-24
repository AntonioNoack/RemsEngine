package org.joml

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sqrt

@Suppress("unused")
class Vector4i(var x: Int, var y: Int, var z: Int, var w: Int) {

    constructor() : this(0, 0, 0, 1)
    constructor(v: Vector4i) : this(v.x, v.y, v.z, v.w)
    constructor(v: Vector3i, w: Int) : this(v.x, v.y, v.z, w)
    constructor(v: Vector2i, z: Int, w: Int) : this(v.x, v.y, z, w)
    constructor(s: Int) : this(s, s, s, s)
    constructor(xyzw: IntArray) : this(xyzw[0], xyzw[1], xyzw[2], xyzw[3])

    fun set(v: Vector4i): Vector4i {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
        return this
    }

    fun set(v: Vector4d): Vector4i {
        x = v.x.toInt()
        y = v.y.toInt()
        z = v.z.toInt()
        w = v.w.toInt()
        return this
    }

    operator fun set(v: Vector3i, w: Int): Vector4i {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
        return this
    }

    operator fun set(v: Vector2i, z: Int, w: Int): Vector4i {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
        return this
    }

    fun set(s: Int): Vector4i {
        x = s
        y = s
        z = s
        w = s
        return this
    }

    operator fun set(x: Int, y: Int, z: Int, w: Int): Vector4i {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(xyzw: IntArray): Vector4i {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
        return this
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Int {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException()
        }
    }

    fun maxComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        val absW = abs(w)
        return if (absX >= absY && absX >= absZ && absX >= absW) {
            0
        } else if (absY >= absZ && absY >= absW) {
            1
        } else {
            if (absZ >= absW) 2 else 3
        }
    }

    fun minComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        val absW = abs(w)
        return if (absX < absY && absX < absZ && absX < absW) {
            0
        } else if (absY < absZ && absY < absW) {
            1
        } else {
            if (absZ < absW) 2 else 3
        }
    }

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Int): Vector4i {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        dst.w = w - v.w
        return dst
    }

    fun sub(x: Int, y: Int, z: Int, w: Int, dst: Vector4i = this): Vector4i {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    fun add(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        dst.w = w + v.w
        return dst
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
    fun distance(v: Vector4i) = distance(v.x, v.y, v.z, v.w)
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
        return (abs(x - this.x) + abs(y - this.y) + abs(z - this.z) + abs(w - this.w)).toLong()
    }

    fun distanceSquared(v: Vector4i): Long {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return lengthSquared(dx, dy, dz, dw)
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

    fun zero() = set(0, 0, 0, 0)

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

    fun min(v: Vector4i): Vector4i {
        x = min(x, v.x)
        y = min(y, v.y)
        z = min(z, v.z)
        w = min(w, v.w)
        return this
    }

    fun min(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        dst.w = min(w, v.w)
        return dst
    }

    fun max(v: Vector4i): Vector4i {
        x = max(x, v.x)
        y = max(y, v.y)
        z = max(z, v.z)
        w = max(w, v.w)
        return this
    }

    fun max(v: Vector4i, dst: Vector4i = this): Vector4i {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        dst.w = max(w, v.w)
        return dst
    }

    fun absolute(): Vector4i {
        x = abs(x)
        y = abs(y)
        z = abs(z)
        w = abs(w)
        return this
    }

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
        return if (this === other) {
            true
        } else if (other == null || this.javaClass != other.javaClass) {
            false
        } else {
            other as Vector4i
            return x == other.x && y == other.y && z == other.z && w == other.w
        }
    }

    fun equals(x: Int, y: Int, z: Int, w: Int): Boolean {
        return if (this.x != x) {
            false
        } else if (this.y != y) {
            false
        } else if (this.z != z) {
            false
        } else {
            this.w == w
        }
    }

    companion object {

        fun lengthSquared(x: Int, y: Int, z: Int, w: Int): Long {
            return x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w
        }

        fun length(x: Int, y: Int, z: Int, w: Int): Double {
            return sqrt((x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w).toFloat())
                .toDouble()
        }

        fun distance(x1: Int, y1: Int, z1: Int, w1: Int, x2: Int, y2: Int, z2: Int, w2: Int): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return sqrt(lengthSquared(dx, dy, dz, dw).toFloat()).toDouble()
        }

        fun distanceSquared(x1: Int, y1: Int, z1: Int, w1: Int, x2: Int, y2: Int, z2: Int, w2: Int): Long {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return lengthSquared(dx, dy, dz, dw)
        }
    }
}