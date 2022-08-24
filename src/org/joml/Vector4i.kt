package org.joml

class Vector4i : Cloneable {
    var x = 0
    var y = 0
    var z = 0
    var w: Int

    constructor() {
        w = 1
    }

    constructor(v: Vector4i) {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
    }

    constructor(v: Vector3i, w: Int) {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
    }

    constructor(v: Vector2i, z: Int, w: Int) {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
    }

    constructor(v: Vector3f, w: Float, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        this.w = Math.roundUsing(w, mode)
    }

    constructor(v: Vector4f, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        w = Math.roundUsing(v.w, mode)
    }

    constructor(v: Vector4d, mode: Int) {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        w = Math.roundUsing(v.w, mode)
    }

    constructor(s: Int) {
        x = s
        y = s
        z = s
        w = s
    }

    constructor(x: Int, y: Int, z: Int, w: Int) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(xyzw: IntArray) {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
    }

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

    operator fun set(v: Vector4d, mode: Int): Vector4i {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        w = Math.roundUsing(v.w, mode)
        return this
    }

    operator fun set(v: Vector4f, mode: Int): Vector4i {
        x = Math.roundUsing(v.x, mode)
        y = Math.roundUsing(v.y, mode)
        z = Math.roundUsing(v.z, mode)
        w = Math.roundUsing(v.w, mode)
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
        val absX = Math.abs(x)
        val absY = Math.abs(y)
        val absZ = Math.abs(z)
        val absW = Math.abs(w)
        return if (absX >= absY && absX >= absZ && absX >= absW) {
            0
        } else if (absY >= absZ && absY >= absW) {
            1
        } else {
            if (absZ >= absW) 2 else 3
        }
    }

    fun minComponent(): Int {
        val absX = Math.abs(x)
        val absY = Math.abs(y)
        val absZ = Math.abs(z)
        val absW = Math.abs(w)
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

    fun sub(v: Vector4i): Vector4i {
        x -= v.x
        y -= v.y
        z -= v.z
        w -= v.w
        return this
    }

    fun sub(x: Int, y: Int, z: Int, w: Int): Vector4i {
        this.x -= x
        this.y -= y
        this.z -= z
        this.w -= w
        return this
    }

    fun sub(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        dest.w = w - v.w
        return dest
    }

    fun sub(x: Int, y: Int, z: Int, w: Int, dest: Vector4i): Vector4i {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        dest.w = this.w - w
        return dest
    }

    fun add(v: Vector4i): Vector4i {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
        return this
    }

    fun add(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        dest.w = w + v.w
        return dest
    }

    fun add(x: Int, y: Int, z: Int, w: Int): Vector4i {
        this.x += x
        this.y += y
        this.z += z
        this.w += w
        return this
    }

    fun add(x: Int, y: Int, z: Int, w: Int, dest: Vector4i): Vector4i {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        dest.w = this.w + w
        return dest
    }

    fun mul(v: Vector4i): Vector4i {
        x *= v.x
        y *= v.y
        z *= v.z
        w *= v.w
        return this
    }

    fun mul(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        dest.w = w * v.w
        return dest
    }

    operator fun div(v: Vector4i): Vector4i {
        x /= v.x
        y /= v.y
        z /= v.z
        w /= v.w
        return this
    }

    fun div(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = x / v.x
        dest.y = y / v.y
        dest.z = z / v.z
        dest.w = w / v.w
        return dest
    }

    fun mul(scalar: Int): Vector4i {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        return this
    }

    fun mul(scalar: Int, dest: Vector4i): Vector4i {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        dest.w = w * scalar
        return dest
    }

    operator fun div(scalar: Float): Vector4i {
        val inv = 1f / scalar
        x = (x.toFloat() * inv).toInt()
        y = (y.toFloat() * inv).toInt()
        z = (z.toFloat() * inv).toInt()
        w = (w.toFloat() * inv).toInt()
        return this
    }

    fun div(scalar: Float, dest: Vector4i): Vector4i {
        val inv = 1f / scalar
        dest.x = (x.toFloat() * inv).toInt()
        dest.y = (y.toFloat() * inv).toInt()
        dest.z = (z.toFloat() * inv).toInt()
        dest.w = (w.toFloat() * inv).toInt()
        return dest
    }

    operator fun div(scalar: Int): Vector4i {
        x /= scalar
        y /= scalar
        z /= scalar
        w /= scalar
        return this
    }

    fun div(scalar: Int, dest: Vector4i): Vector4i {
        dest.x = x / scalar
        dest.y = y / scalar
        dest.z = z / scalar
        dest.w = w / scalar
        return dest
    }

    fun lengthSquared(): Long {
        return x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w
    }

    fun length(): Double {
        return Math.sqrt(lengthSquared().toFloat()).toDouble()
    }

    fun distance(v: Vector4i): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return length(dx, dy, dz, dw)
    }

    fun distance(x: Int, y: Int, z: Int, w: Int): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return length(dx, dy, dz, dw)
    }

    fun gridDistance(v: Vector4i): Long {
        return (Math.abs(v.x - x) + Math.abs(v.y - y) + Math.abs(v.z - z) + Math.abs(
            v.w - w
        )).toLong()
    }

    fun gridDistance(x: Int, y: Int, z: Int, w: Int): Long {
        return (Math.abs(x - this.x) + Math.abs(y - this.y) + Math.abs(z - this.z) + Math.abs(w - this.w)).toLong()
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

    fun zero(): Vector4i {
        x = 0
        y = 0
        z = 0
        w = 0
        return this
    }

    fun negate(): Vector4i {
        x = -x
        y = -y
        z = -z
        w = -w
        return this
    }

    fun negate(dest: Vector4i): Vector4i {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        dest.w = -w
        return dest
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    fun min(v: Vector4i): Vector4i {
        x = java.lang.Math.min(x, v.x)
        y = java.lang.Math.min(y, v.y)
        z = java.lang.Math.min(z, v.z)
        w = java.lang.Math.min(w, v.w)
        return this
    }

    fun min(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        dest.w = java.lang.Math.min(w, v.w)
        return dest
    }

    fun max(v: Vector4i): Vector4i {
        x = java.lang.Math.max(x, v.x)
        y = java.lang.Math.max(y, v.y)
        z = java.lang.Math.max(z, v.z)
        w = java.lang.Math.max(w, v.w)
        return this
    }

    fun max(v: Vector4i, dest: Vector4i): Vector4i {
        dest.x = java.lang.Math.max(x, v.x)
        dest.y = java.lang.Math.max(y, v.y)
        dest.z = java.lang.Math.max(z, v.z)
        dest.w = java.lang.Math.max(w, v.w)
        return dest
    }

    fun absolute(): Vector4i {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        w = Math.abs(w)
        return this
    }

    fun absolute(dest: Vector4i): Vector4i {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        dest.w = Math.abs(w)
        return dest
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

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        fun lengthSquared(x: Int, y: Int, z: Int, w: Int): Long {
            return x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w
        }

        fun length(x: Int, y: Int, z: Int, w: Int): Double {
            return Math.sqrt((x.toLong() * x + y.toLong() * y + z.toLong() * z + w.toLong() * w).toFloat())
                .toDouble()
        }

        fun distance(x1: Int, y1: Int, z1: Int, w1: Int, x2: Int, y2: Int, z2: Int, w2: Int): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return Math.sqrt(lengthSquared(dx, dy, dz, dw).toFloat()).toDouble()
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