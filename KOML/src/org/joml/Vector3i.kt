package org.joml

import kotlin.math.abs
import kotlin.math.sqrt

@Suppress("unused")
open class Vector3i(var x: Int, var y: Int, var z: Int) {

    constructor() : this(0, 0, 0)
    constructor(d: Int) : this(d, d, d)
    constructor(v: Vector3i) : this(v.x, v.y, v.z)
    constructor(v: Vector2i, z: Int) : this(v.x, v.y, z)
    constructor(xyz: IntArray) : this(xyz[0], xyz[1], xyz[2])

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
            2 -> z
            else -> throw IllegalArgumentException()
        }
    }

    fun setComponent(component: Int, value: Int): Vector3i {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
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
        dst.x = (x.toFloat() * inv).toInt()
        dst.y = (y.toFloat() * inv).toInt()
        dst.z = (z.toFloat() * inv).toInt()
        return dst
    }

    fun div(scalar: Int, dst: Vector3i = this): Vector3i {
        dst.x = x / scalar
        dst.y = y / scalar
        dst.z = z / scalar
        return dst
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

    fun zero(): Vector3i {
        x = 0
        y = 0
        z = 0
        return this
    }

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
        x = kotlin.math.min(x, v.x)
        y = kotlin.math.min(y, v.y)
        z = kotlin.math.min(z, v.z)
        return this
    }

    fun min(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = kotlin.math.min(x, v.x)
        dst.y = kotlin.math.min(y, v.y)
        dst.z = kotlin.math.min(z, v.z)
        return dst
    }

    fun max(v: Vector3i): Vector3i {
        x = kotlin.math.max(x, v.x)
        y = kotlin.math.max(y, v.y)
        z = kotlin.math.max(z, v.z)
        return this
    }

    fun max(v: Vector3i, dst: Vector3i = this): Vector3i {
        dst.x = kotlin.math.max(x, v.x)
        dst.y = kotlin.math.max(y, v.y)
        dst.z = kotlin.math.max(z, v.z)
        return dst
    }

    fun maxComponent(): Int {
        val absX = abs(x).toFloat()
        val absY = abs(y).toFloat()
        val absZ = abs(z).toFloat()
        return if (absX >= absY && absX >= absZ) 0 else if (absY >= absZ) 1 else 2
    }

    fun minComponent(): Int {
        val absX = abs(x).toFloat()
        val absY = abs(y).toFloat()
        val absZ = abs(z).toFloat()
        return if (absX < absY && absX < absZ) 0 else if (absY < absZ) 1 else 2
    }

    fun absolute(dst: Vector3i = this): Vector3i {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
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
            other as Vector3i
            return x == other.x && y == other.y && z == other.z
        }
    }

    fun equals(x: Int, y: Int, z: Int): Boolean {
        return x == this.x && y == this.y && z == this.z
    }

    companion object {

        fun lengthSquared(x: Int, y: Int, z: Int): Long {
            return x.toLong() * x + y.toLong() * y + z.toLong() * z
        }

        fun length(x: Int, y: Int, z: Int): Double {
            return sqrt(lengthSquared(x, y, z).toFloat()).toDouble()
        }

        fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
            return sqrt(lengthSquared(x1 - x2, y1 - y2, z1 - z2).toFloat()).toDouble()
        }

        fun distanceSquared(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Long {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return lengthSquared(dx, dy, dz)
        }
    }
}