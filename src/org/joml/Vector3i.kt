package org.joml

class Vector3i : Cloneable {
    var x = 0
    var y = 0
    var z = 0

    constructor() {}
    constructor(d: Int) {
        x = d
        y = d
        z = d
    }

    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(v: Vector3i) {
        x = v.x
        y = v.y
        z = v.z
    }

    constructor(v: Vector2i, z: Int) {
        x = v.x
        y = v.y
        this.z = z
    }

    constructor(x: Float, y: Float, z: Float, mode: Int) {
        this.x = Math.roundUsing(x, mode)
        this.y = Math.roundUsing(y, mode)
        this.z = Math.roundUsing(z, mode)
    }

    constructor(x: Double, y: Double, z: Double, mode: Int) {
        this.x = Math.roundUsing(x, mode)
        this.y = Math.roundUsing(y, mode)
        this.z = Math.roundUsing(z, mode)
    }

    constructor(v: Vector2f, z: Float, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        this.z = Math.roundUsing(z, mode)
    }

    constructor(v: Vector3f, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
    }

    constructor(v: Vector2d, z: Float, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        this.z = Math.roundUsing(z, mode)
    }

    constructor(v: Vector3d, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
    }

    constructor(xyz: IntArray) {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
    }

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

    operator fun set(v: Vector3d, mode: Int): Vector3i {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        return this
    }

    operator fun set(v: Vector3f, mode: Int): Vector3i {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        return this
    }

    operator fun set(v: Vector2i, z: Int): Vector3i {
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

    operator fun set(x: Int, y: Int, z: Int): Vector3i {
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

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Int {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IllegalArgumentException()
        }
    }

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Int): Vector3i {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector3i): Vector3i {
        x -= v.x
        y -= v.y
        z -= v.z
        return this
    }

    fun sub(v: Vector3i, dest: Vector3i): Vector3i {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        return dest
    }

    fun sub(x: Int, y: Int, z: Int): Vector3i {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    fun sub(x: Int, y: Int, z: Int, dest: Vector3i): Vector3i {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        return dest
    }

    fun add(v: Vector3i): Vector3i {
        x += v.x
        y += v.y
        z += v.z
        return this
    }

    fun add(v: Vector3i, dest: Vector3i): Vector3i {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        return dest
    }

    fun add(x: Int, y: Int, z: Int): Vector3i {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun add(x: Int, y: Int, z: Int, dest: Vector3i): Vector3i {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        return dest
    }

    fun mul(scalar: Int): Vector3i {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun mul(scalar: Int, dest: Vector3i): Vector3i {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun mul(v: Vector3i): Vector3i {
        x *= v.x
        y *= v.y
        z *= v.z
        return this
    }

    fun mul(v: Vector3i, dest: Vector3i): Vector3i {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        return dest
    }

    fun mul(x: Int, y: Int, z: Int): Vector3i {
        this.x *= x
        this.y *= y
        this.z *= z
        return this
    }

    fun mul(x: Int, y: Int, z: Int, dest: Vector3i): Vector3i {
        dest.x = this.x * x
        dest.y = this.y * y
        dest.z = this.z * z
        return dest
    }

    operator fun div(scalar: Float): Vector3i {
        val inv = 1f / scalar
        x = (x.toFloat() * inv).toInt()
        y = (y.toFloat() * inv).toInt()
        z = (z.toFloat() * inv).toInt()
        return this
    }

    fun div(scalar: Float, dest: Vector3i): Vector3i {
        val inv = 1f / scalar
        dest.x = (x.toFloat() * inv).toInt()
        dest.y = (y.toFloat() * inv).toInt()
        dest.z = (z.toFloat() * inv).toInt()
        return dest
    }

    operator fun div(scalar: Int): Vector3i {
        x /= scalar
        y /= scalar
        z /= scalar
        return this
    }

    fun div(scalar: Int, dest: Vector3i): Vector3i {
        dest.x = x / scalar
        dest.y = y / scalar
        dest.z = z / scalar
        return dest
    }

    fun lengthSquared(): Long {
        return x.toLong() * x + y.toLong() * y + z.toLong() * z
    }

    fun length(): Double {
        return Math.sqrt(lengthSquared().toFloat()).toDouble()
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
        return (Math.abs(v.x - x) + Math.abs(v.y - y) + Math.abs(v.z - z)).toLong()
    }

    fun gridDistance(x: Int, y: Int, z: Int): Long {
        return (Math.abs(x - this.x) + Math.abs(y - this.y) + Math.abs(z - this.z)).toLong()
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

    fun negate(): Vector3i {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun negate(dest: Vector3i): Vector3i {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        return dest
    }

    fun min(v: Vector3i): Vector3i {
        x = java.lang.Math.min(x, v.x)
        y = java.lang.Math.min(y, v.y)
        z = java.lang.Math.min(z, v.z)
        return this
    }

    fun min(v: Vector3i, dest: Vector3i): Vector3i {
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        return dest
    }

    fun max(v: Vector3i): Vector3i {
        x = java.lang.Math.max(x, v.x)
        y = java.lang.Math.max(y, v.y)
        z = java.lang.Math.max(z, v.z)
        return this
    }

    fun max(v: Vector3i, dest: Vector3i): Vector3i {
        dest.x = java.lang.Math.max(x, v.x)
        dest.y = java.lang.Math.max(y, v.y)
        dest.z = java.lang.Math.max(z, v.z)
        return dest
    }

    fun maxComponent(): Int {
        val absX = Math.abs(x).toFloat()
        val absY = Math.abs(y).toFloat()
        val absZ = Math.abs(z).toFloat()
        return if (absX >= absY && absX >= absZ) {
            0
        } else {
            if (absY >= absZ) 1 else 2
        }
    }

    fun minComponent(): Int {
        val absX = Math.abs(x).toFloat()
        val absY = Math.abs(y).toFloat()
        val absZ = Math.abs(z).toFloat()
        return if (absX < absY && absX < absZ) {
            0
        } else {
            if (absY < absZ) 1 else 2
        }
    }

    fun absolute(): Vector3i {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        return this
    }

    fun absolute(dest: Vector3i): Vector3i {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        return dest
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null) {
            false
        } else if (this.javaClass != obj.javaClass) {
            false
        } else {
            val other = obj as Vector3i
            if (x != other.x) {
                false
            } else if (y != other.y) {
                false
            } else {
                z == other.z
            }
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
            return Math.sqrt(lengthSquared(x, y, z).toFloat()).toDouble()
        }

        fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
            return Math.sqrt(lengthSquared(x1 - x2, y1 - y2, z1 - z2).toFloat()).toDouble()
        }

        fun distanceSquared(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Long {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return lengthSquared(dx, dy, dz)
        }
    }
}