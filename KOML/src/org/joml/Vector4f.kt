package org.joml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Vector4f(
    @JvmField var x: Float,
    @JvmField var y: Float,
    @JvmField var z: Float,
    @JvmField var w: Float
) : Vector() {

    constructor() : this(0f, 0f, 0f, 1f)
    constructor(v: Vector4f) : this(v.x, v.y, v.z, v.w)
    constructor(v: Vector4d) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    constructor(v: Vector4i) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    constructor(v: Vector3f, w: Float) : this(v.x, v.y, v.z, w)
    constructor(v: Vector3i, w: Float) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), w)
    constructor(v: Vector2f, z: Float, w: Float) : this(v.x, v.y, z, w)
    constructor(v: Vector2i, z: Float, w: Float) : this(v.x.toFloat(), v.y.toFloat(), z, w)
    constructor(d: Float) : this(d, d, d, d)
    constructor(v: FloatArray, i: Int = 0) : this(v[i], v[i + 1], v[i + 2], v[i + 3])

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toFloat())
    }

    fun set(v: Vector4f) = set(v.x, v.y, v.z, v.w)
    fun set(v: Vector4i) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun set(v: Vector4d) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun set(v: Vector3f, w: Float) = set(v.x, v.y, v.z, w)
    fun set(v: Vector3i, w: Float) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), w)
    fun set(v: Vector2f, z: Float, w: Float) = set(v.x, v.y, z, w)
    fun set(v: Vector2i, z: Float, w: Float) = set(v.x.toFloat(), v.y.toFloat(), z, w)
    fun set(d: Float) = set(d, d, d, d)
    fun set(x: Float, y: Float, z: Float) = set(x, y, z, this.w)
    fun set(d: Double) = set(d.toFloat())
    fun set(x: Double, y: Double, z: Double, w: Double) = set(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun set(v: FloatArray, i: Int = 0) = set(v[i], v[i + 1], v[i + 2], v[i + 3])
    fun set(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(component: Int, value: Float) = setComponent(component, value)
    fun setComponent(component: Int, value: Float): Vector4f {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> w = value
        }
        return this
    }

    fun sub(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        dst.w = w - v.w
        return dst
    }

    fun sub(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    fun add(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        dst.w = w + v.w
        return dst
    }

    fun add(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    fun fma(a: Vector4f, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = a.x * b.x + x
        dst.y = a.y * b.y + y
        dst.z = a.z * b.z + z
        dst.w = a.w * b.w + w
        return dst
    }

    fun fma(a: Float, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = a * b.x + x
        dst.y = a * b.y + y
        dst.z = a * b.z + z
        dst.w = a * b.w + w
        return dst
    }

    fun mulAdd(a: Vector4f, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        dst.z = z * a.z + b.z
        return dst
    }

    fun mulAdd(a: Float, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    fun mul(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        dst.w = w * v.w
        return dst
    }

    fun div(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        dst.w = w / v.w
        return dst
    }

    fun mul(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffine(mat, dst) else mulGeneric(mat, dst)
    }

    fun mulTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dst) else mulGenericTranspose(mat, dst)
    }

    fun mulAffine(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w
        return dst
    }

    fun mulAffineTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m01 * y + mat.m02 * z
        dst.y = mat.m10 * x + mat.m11 * y + mat.m12 * z
        dst.z = mat.m20 * x + mat.m21 * y + mat.m22 * z
        dst.w = mat.m30 * x + mat.m31 * y + mat.m32 * z + w
        return dst
    }

    private fun mulGenericTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m01 * y + mat.m02 * z + mat.m03 * w
        dst.y = mat.m10 * x + mat.m11 * y + mat.m12 * z + mat.m13 * w
        dst.z = mat.m20 * x + mat.m21 * y + mat.m22 * z + mat.m23 * w
        dst.w = mat.m30 * x + mat.m31 * y + mat.m32 * z + mat.m33 * w
        return dst
    }

    fun mul(mat: Matrix4x3f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = w
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        dst.x = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        dst.y = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        dst.z = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        dst.w = 1f
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        dst.x = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        dst.y = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        dst.z = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        return dst
    }

    fun mul(scalar: Float, dst: Vector4f = this): Vector4f {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        dst.w = w * scalar
        return dst
    }

    fun mul(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x * x
        dst.y = this.y * y
        dst.z = this.z * z
        dst.w = this.w * w
        return dst
    }

    fun div(scalar: Float, dst: Vector4f = this) = mul(1f / scalar, dst)
    fun div(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x / x
        dst.y = this.y / y
        dst.z = this.z / z
        dst.w = this.w / w
        return dst
    }

    fun rotate(quat: Quaternionf, dst: Vector4f = this): Vector4f {
        return quat.transform(this, dst)
    }

    fun rotateAbout(angle: Float, x: Float, y: Float, z: Float): Vector4f {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            this.rotateX(x * angle, this)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            this.rotateY(y * angle, this)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) this.rotateZ(
                z * angle,
                this
            ) else rotateAxisInternal(angle, x, y, z, this)
        }
    }

    fun rotateAxis(angle: Float, aX: Float, aY: Float, aZ: Float, dst: Vector4f = this): Vector4f {
        return if (aY == 0f && aZ == 0f && JomlMath.absEqualsOne(aX)) {
            this.rotateX(aX * angle, dst)
        } else if (aX == 0f && aZ == 0f && JomlMath.absEqualsOne(aY)) {
            this.rotateY(aY * angle, dst)
        } else {
            if (aX == 0f && aY == 0f && JomlMath.absEqualsOne(aZ)) this.rotateZ(
                aZ * angle,
                dst
            ) else rotateAxisInternal(angle, aX, aY, aZ, dst)
        }
    }

    private fun rotateAxisInternal(angle: Float, aX: Float, aY: Float, aZ: Float, dst: Vector4f = this): Vector4f {
        val halfAngle = angle * 0.5f
        val sinAngle = sin(halfAngle)
        val qx = aX * sinAngle
        val qy = aY * sinAngle
        val qz = aZ * sinAngle
        val qw = cos(halfAngle)
        val w2 = qw * qw
        val x2 = qx * qx
        val y2 = qy * qy
        val z2 = qz * qz
        val zw = qz * qw
        val xy = qx * qy
        val xz = qx * qz
        val yw = qy * qw
        val yz = qy * qz
        val xw = qx * qw
        val x = x
        val y = y
        val z = z
        dst.x = (w2 + x2 - z2 - y2) * x + (-zw + xy - zw + xy) * y + (yw + xz + xz + yw) * z
        dst.y = (xy + zw + zw + xy) * x + (y2 - z2 + w2 - x2) * y + (yz + yz - xw - xw) * z
        dst.z = (xz - yw + xz - yw) * x + (yz + yz + xw + xw) * y + (z2 - y2 - x2 + w2) * z
        return dst
    }

    fun rotateX(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        dst.w = w
        return dst
    }

    fun rotateY(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        dst.w = w
        return dst
    }

    fun rotateZ(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dst.x = x
        dst.y = y
        dst.z = z
        dst.w = w
        return dst
    }

    fun lengthSquared() = x * x + y * y + z * z + w * w
    fun length() = sqrt(lengthSquared())

    @JvmOverloads
    fun normalize(dst: Vector4f = this) = mul(1f / length(), dst)

    @JvmOverloads
    fun normalize(length: Float, dst: Vector4f = this) = mul(length / length(), dst)

    @JvmOverloads
    fun normalize3(dst: Vector4f = this) = mul(1f / length3(), dst)

    fun length3() = sqrt(x * x + y * y + z * z)

    fun distance(v: Vector4f) = distance(v.x, v.y, v.z, v.w)
    fun distance(x: Float, y: Float, z: Float, w: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return sqrt(dx * dx + dy * dy + dz * dz + dw * dw)
    }

    fun distanceSquared(v: Vector4f): Float = distanceSquared(v.x, v.y, v.z, v.w)
    fun distanceSquared(vx: Float, vy: Float, vz: Float, vw: Float): Float =
        lengthSquared(x - vx, y - vy, z - vz, w - vw)

    fun dot(v: Vector4f): Float = dot(v.x, v.y, v.z, v.w)
    fun dot(v: Vector4d): Double = dot(v.x, v.y, v.z, v.w)
    fun dot(vx: Float, vy: Float, vz: Float, vw: Float): Float = x * vx + y * vy + z * vz + w * vw
    fun dot(vx: Double, vy: Double, vz: Double, vw: Double): Double = x * vx + y * vy + z * vz + w * vw

    fun angleCos(v: Vector4f): Float = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Float, vy: Float, vz: Float, vw: Float): Float =
        dot(vx, vy, vz, vw) / sqrt(lengthSquared() * lengthSquared(vx, vy, vz, vw))

    fun angle(v: Vector4f): Float {
        var cos = angleCos(v)
        cos = min(cos, 1f)
        cos = max(cos, -1f)
        return acos(cos)
    }

    fun zero(): Vector4f {
        x = 0f
        y = 0f
        z = 0f
        w = 0f
        return this
    }

    fun negate(dst: Vector4f = this): Vector4f {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = -w
        return dst
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    fun min(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        dst.w = min(w, v.w)
        return dst
    }

    fun max(v: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        dst.w = max(w, v.w)
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + (w).toBits()
        result = 31 * result + (x).toBits()
        result = 31 * result + (y).toBits()
        result = 31 * result + (z).toBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Vector4f && other.x == x && other.y == y && other.z == z && other.w == w
    }

    fun equals(v: Vector4f, delta: Float): Boolean {
        return Runtime.equals(x, v.x, delta) &&
                Runtime.equals(y, v.y, delta) &&
                Runtime.equals(z, v.z, delta) &&
                Runtime.equals(w, v.w, delta)
    }

    fun equals(x: Float, y: Float, z: Float, w: Float): Boolean {
        return this.x == x && this.y == y && this.z == z && this.w == w
    }

    fun smoothStep(v: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x
        dst.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y
        dst.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z
        dst.w = (w + w - v.w - v.w) * t3 + (3f * v.w - 3f * w) * t2 + w * t + w
        return dst
    }

    fun hermite(t0: Vector4f, v1: Vector4f, t1: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dst.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dst.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        dst.w = (w + w - v1.w - v1.w + t1.w + t0.w) * t3 + (3f * v1.w - 3f * w - t0.w - t0.w - t1.w) * t2 + w * t + w
        return dst
    }

    fun lerp(other: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        dst.x = (other.x - x) * t + x
        dst.y = (other.y - y) * t + y
        dst.z = (other.z - z) * t + z
        dst.w = (other.w - w) * t + w
        return dst
    }

    operator fun get(component: Int): Float {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> w
        }
    }

    fun get(dst: Vector4f = this): Vector4f {
        dst.x = x
        dst.y = y
        dst.z = z
        dst.w = w
        return dst
    }

    fun get(dst: Vector4d): Vector4d {
        dst.x = x.toDouble()
        dst.y = y.toDouble()
        dst.z = z.toDouble()
        dst.w = w.toDouble()
        return dst
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

    fun floor(dst: Vector4f = this): Vector4f {
        dst.x = floor(x)
        dst.y = floor(y)
        dst.z = floor(z)
        dst.w = floor(w)
        return dst
    }

    fun ceil(dst: Vector4f = this): Vector4f {
        dst.x = ceil(x)
        dst.y = ceil(y)
        dst.z = ceil(z)
        dst.w = ceil(w)
        return dst
    }

    fun round(dst: Vector4f = this): Vector4f {
        dst.x = round(x)
        dst.y = round(y)
        dst.z = round(z)
        dst.w = round(w)
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    fun absolute(dst: Vector4f = this): Vector4f {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        dst.w = abs(w)
        return dst
    }

    operator fun minus(s: Vector4f) = Vector4f(x - s.x, y - s.y, z - s.z, w - s.w)
    operator fun plus(s: Vector4f) = Vector4f(x + s.x, y + s.y, z + s.z, w + s.w)
    operator fun plus(s: Float) = if (s == 0f) this else Vector4f(x + s, y + s, z + s, w + s)
    operator fun times(s: Float) = Vector4f(x * s, y * s, z * s, w * s)
    operator fun times(s: Vector4f) = Vector4f(x * s.x, y * s.y, z * s.z, w * s.w)
    fun mulAlpha(m: Float, dst: Vector4f = Vector4f()): Vector4f = dst.set(x, y, z, w * m)
    fun is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    companion object {

        @JvmStatic
        fun lengthSquared(x: Float, y: Float, z: Float, w: Float): Float {
            return x * x + y * y + z * z + w * w
        }

        @JvmStatic
        fun length(x: Float, y: Float, z: Float, w: Float): Float {
            return sqrt(x * x + y * y + z * z + w * w)
        }

        @JvmStatic
        fun distance(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return sqrt(dx * dx + dy * dy + dz * dz + dw * dw)
        }

        @JvmStatic
        fun distanceSquared(
            x1: Float, y1: Float, z1: Float, w1: Float,
            x2: Float, y2: Float, z2: Float, w2: Float
        ): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return dx * dx + dy * dy + dz * dz + dw * dw
        }
    }
}