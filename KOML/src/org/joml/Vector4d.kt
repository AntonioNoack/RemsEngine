package org.joml

import org.joml.JomlMath.hash
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
open class Vector4d(
    @JvmField var x: Double,
    @JvmField var y: Double,
    @JvmField var z: Double,
    @JvmField var w: Double
) : Vector {

    constructor() : this(0.0, 0.0, 0.0, 1.0)
    constructor(v: Vector4d) : this(v.x, v.y, v.z, v.w)
    constructor(v: Vector4i) : this(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.w.toDouble())
    constructor(v: Vector3d, w: Double) : this(v.x, v.y, v.z, w)
    constructor(v: Vector3i, w: Double) : this(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), w)
    constructor(v: Vector2d, z: Double, w: Double) : this(v.x, v.y, z, w)
    constructor(v: Vector2i, z: Double, w: Double) : this(v.x.toDouble(), v.y.toDouble(), z, w)
    constructor(d: Double) : this(d, d, d, d)
    constructor(v: Vector4f) : this(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.w.toDouble())
    constructor(v: Vector3f, w: Double) : this(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), w)
    constructor(v: Vector2f, z: Double, w: Double) : this(v.x.toDouble(), v.y.toDouble(), z, w)
    constructor(v: DoubleArray, i: Int = 0) : this(v[i], v[i + 1], v[i + 2], v[i + 3])
    constructor(v: FloatArray, i: Int = 0) : this() {
        set(v, i)
    }

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = get(i)
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v)
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z
    operator fun component4() = w

    fun set(v: Vector4d): Vector4d {
        return set(v.x, v.y, v.z, v.w)
    }

    fun set(v: Vector4f): Vector4d {
        return set(v.x, v.y, v.z, v.w)
    }

    fun set(v: Vector4i): Vector4d {
        return set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.w.toDouble())
    }

    fun set(v: Vector3d, w: Double): Vector4d {
        return set(v.x, v.y, v.z, w)
    }

    fun set(v: Vector3i, w: Double): Vector4d {
        return set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), w)
    }

    fun set(v: Vector3f, w: Double): Vector4d {
        return set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), w)
    }

    fun set(v: Vector2d, z: Double, w: Double): Vector4d {
        return set(v.x, v.y, z, w)
    }

    fun set(v: Vector2i, z: Double, w: Double): Vector4d {
        return set(v.x.toDouble(), v.y.toDouble(), z, w)
    }

    fun set(d: Double): Vector4d {
        return set(d, d, d, d)
    }

    fun set(v: Vector2f, z: Double, w: Double): Vector4d {
        return set(v.x.toDouble(), v.y.toDouble(), z, w)
    }

    fun set(x: Float, y: Float, z: Float, w: Float): Vector4d {
        return set(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
    }

    fun set(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(x: Double, y: Double, z: Double): Vector4d {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(xyzw: DoubleArray, offset: Int = 0): Vector4d {
        return set(xyzw[offset], xyzw[offset + 1], xyzw[offset + 2], xyzw[offset + 3])
    }

    fun set(xyzw: FloatArray, offset: Int = 0): Vector4d {
        return set(xyzw[offset], xyzw[offset + 1], xyzw[offset + 2], xyzw[offset + 3])
    }

    operator fun set(component: Int, value: Double) = setComp(component, value)
    fun setComponent(component: Int, value: Double): Vector4d {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> w = value
        }
        return this
    }

    @JvmOverloads
    fun sub(v: Vector4d, dst: Vector4d = this): Vector4d {
        return sub(v.x, v.y, v.z, v.w, dst)
    }

    @JvmOverloads
    fun sub(v: Vector4f, dst: Vector4d = this): Vector4d {
        return sub(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.w.toDouble(), dst)
    }

    @JvmOverloads
    fun sub(x: Double, y: Double, z: Double, w: Double, dst: Vector4d = this): Vector4d {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    @JvmOverloads
    fun add(v: Vector4d, dst: Vector4d = this): Vector4d {
        return add(v.x, v.y, v.z, v.w, dst)
    }

    @JvmOverloads
    fun add(v: Vector4f, dst: Vector4d = this): Vector4d {
        return add(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), v.w.toDouble(), dst)
    }

    @JvmOverloads
    fun add(x: Double, y: Double, z: Double, w: Double, dst: Vector4d = this): Vector4d {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    @JvmOverloads
    fun fma(a: Vector4d, b: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = a.x * b.x + x
        dst.y = a.y * b.y + y
        dst.z = a.z * b.z + z
        dst.w = a.w * b.w + w
        return dst
    }

    @JvmOverloads
    fun fma(a: Double, b: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = a * b.x + x
        dst.y = a * b.y + y
        dst.z = a * b.z + z
        dst.w = a * b.w + w
        return dst
    }

    @JvmOverloads
    fun mulAdd3(a: Vector4d, b: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        dst.z = z * a.z + b.z
        return dst
    }

    @JvmOverloads
    fun mulAdd(a: Vector4d, b: Vector4d, dst: Vector4d = this): Vector4d {
        mulAdd3(a, b, dst)
        dst.w = w * a.w + b.w
        return dst
    }

    @JvmOverloads
    fun mulAdd3(a: Double, b: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    @JvmOverloads
    fun mulAdd(a: Double, b: Vector4d, dst: Vector4d = this): Vector4d {
        mulAdd3(a, b, dst)
        dst.w = w * a + b.w
        return dst
    }

    @JvmOverloads
    fun mul(v: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        dst.w = w * v.w
        return dst
    }

    @JvmOverloads
    fun div(v: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        dst.w = w / v.w
        return dst
    }

    @JvmOverloads
    fun mul(v: Vector4f, dst: Vector4d = this): Vector4d {
        dst.x = x * v.x.toDouble()
        dst.y = y * v.y.toDouble()
        dst.z = z * v.z.toDouble()
        dst.w = w * v.w.toDouble()
        return dst
    }

    @JvmOverloads
    fun mul(mat: Matrix4d, dst: Vector4d = this): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dst) else this.mulGeneric(mat, dst)
    }

    @JvmOverloads
    fun mulTranspose(mat: Matrix4d, dst: Vector4d = this): Vector4d {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dst) else mulGenericTranspose(mat, dst)
    }

    fun mulAffine(mat: Matrix4d, dst: Vector4d): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4d, dst: Vector4d): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        val rw = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = rw
        return dst
    }

    fun mulAffineTranspose(mat: Matrix4d, dst: Vector4d): Vector4d {
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

    private fun mulGenericTranspose(mat: Matrix4d, dst: Vector4d): Vector4d {
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

    @JvmOverloads
    fun mul(mat: Matrix4x3d, dst: Vector4d = this): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    @JvmOverloads
    fun mul(mat: Matrix4x3f, dst: Vector4d = this): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    @JvmOverloads
    fun mul(mat: Matrix4f, dst: Vector4d = this): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dst) else this.mulGeneric(mat, dst)
    }

    private fun mulAffine(mat: Matrix4f, dst: Vector4d): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4f, dst: Vector4d): Vector4d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        val rw = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = rw
        return dst
    }

    @JvmOverloads
    fun mulProject(mat: Matrix4d, dst: Vector4d = this): Vector4d {
        val invW = 1.0 / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        val rx = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        val ry = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        val rz = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = 1.0
        return dst
    }

    fun mulProject(mat: Matrix4d, dst: Vector3d): Vector3d {
        val invW = 1.0 / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        val rx = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        val ry = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        val rz = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    @JvmOverloads
    fun mul(scalar: Double, dst: Vector4d = this): Vector4d {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        dst.w = w * scalar
        return dst
    }

    @JvmOverloads
    fun div(scalar: Double, dst: Vector4d = this) = mul(1.0 / scalar, dst)

    @JvmOverloads
    fun rotate(quat: Quaterniond, dst: Vector4d = this): Vector4d {
        quat.transform(this, dst)
        return dst
    }

    @JvmOverloads
    fun rotateInv(q: Quaterniond, dst: Vector4d = this): Vector4d {
        synchronized(q) {
            q.conjugate()
            q.transform(this, dst)
            q.conjugate()
        }
        return dst
    }

    fun rotateAxis(angle: Double, aX: Double, aY: Double, aZ: Double, dst: Vector4d = this): Vector4d {
        return if (aY == 0.0 && aZ == 0.0 && JomlMath.absEqualsOne(aX)) {
            this.rotateX(aX * angle, dst)
        } else if (aX == 0.0 && aZ == 0.0 && JomlMath.absEqualsOne(aY)) {
            this.rotateY(aY * angle, dst)
        } else {
            if (aX == 0.0 && aY == 0.0 && JomlMath.absEqualsOne(aZ)) this.rotateZ(
                aZ * angle,
                dst
            ) else rotateAxisInternal(angle, aX, aY, aZ, dst)
        }
    }

    private fun rotateAxisInternal(angle: Double, aX: Double, aY: Double, aZ: Double, dst: Vector4d): Vector4d {
        val halfAngle = angle * 0.5
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
        val nx = (w2 + x2 - z2 - y2) * x + (-zw + xy - zw + xy) * y + (yw + xz + xz + yw) * z
        val ny = (xy + zw + zw + xy) * x + (y2 - z2 + w2 - x2) * y + (yz + yz - xw - xw) * z
        val nz = (xz - yw + xz - yw) * x + (yz + yz + xw + xw) * y + (z2 - y2 - x2 + w2) * z
        dst.x = nx
        dst.y = ny
        dst.z = nz
        return dst
    }

    @JvmOverloads
    fun rotateX(angle: Double, dst: Vector4d = this): Vector4d {
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

    @JvmOverloads
    fun rotateY(angle: Double, dst: Vector4d = this): Vector4d {
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

    @JvmOverloads
    fun rotateZ(angle: Double, dst: Vector4d = this): Vector4d {
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

    fun lengthSquared(): Double = Companion.lengthSquared(x, y, z, w)
    fun length(): Double = Companion.length(x, y, z, w)
    fun length3(): Double = sqrt(x * x + y * y + z * z)

    @JvmOverloads
    fun normalize(dst: Vector4d = this) = div(length(), dst)

    @JvmOverloads
    fun normalize(length: Double, dst: Vector4d = this) = mul(length / length(), dst)

    @JvmOverloads
    fun normalize3(dst: Vector4d = this) = div(length3(), dst)

    @JvmOverloads
    fun safeNormalize(length: Double = 1.0): Vector4d {
        normalize(length)
        if (!isFinite) set(0.0)
        return this
    }

    fun distance(v: Vector4d): Double {
        return distance(v.x, v.y, v.z, v.w)
    }

    fun distance(x: Double, y: Double, z: Double, w: Double): Double {
        return Companion.distance(this.x, this.y - y, this.z, this.w, x, y, z, w)
    }

    fun distanceSquared(v: Vector4d): Double {
        return distanceSquared(v.x, v.y, v.z, v.w)
    }

    fun distanceSquared(x: Double, y: Double, z: Double, w: Double): Double {
        return Companion.distanceSquared(this.x, this.y - y, this.z, this.w, x, y, z, w)
    }

    fun dot(v: Vector4f): Double = dot(v.x, v.y, v.z, v.w)
    fun dot(v: Vector4d): Double = dot(v.x, v.y, v.z, v.w)
    fun dot(vx: Float, vy: Float, vz: Float, vw: Float): Double = x * vx + y * vy + z * vz + w * vw
    fun dot(vx: Double, vy: Double, vz: Double, vw: Double): Double = x * vx + y * vy + z * vz + w * vw

    fun angleCos(v: Vector4d): Double = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Double, vy: Double, vz: Double, vw: Double): Double =
        dot(vx, vy, vz, vw) / sqrt(lengthSquared() * lengthSquared(vx, vy, vz, vw))

    fun angle(v: Vector4d): Double {
        var cos = angleCos(v)
        cos = min(cos, 1.0)
        cos = max(cos, -1.0)
        return acos(cos)
    }

    fun zero(): Vector4d = set(0.0)

    @JvmOverloads
    fun negate(dst: Vector4d = this): Vector4d {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = -w
        return dst
    }

    @JvmOverloads
    fun min(v: Vector4d, dst: Vector4d = this): Vector4d {
        return dst.set(min(x, v.x), min(y, v.y), min(z, v.z), min(w, v.w))
    }

    @JvmOverloads
    fun max(v: Vector4d, dst: Vector4d = this): Vector4d {
        return dst.set(max(x, v.x), max(y, v.y), max(z, v.z), max(w, v.w))
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + hash(x)
        result = 31 * result + hash(y)
        result = 31 * result + hash(z)
        result = 31 * result + hash(w)
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Vector4d && other.x == x && other.y == y && other.z == z && other.w == w
    }

    fun equals(v: Vector4d?, delta: Double): Boolean {
        return if (this === v) true
        else if (v == null) false
        else Runtime.equals(x, v.x, delta) && Runtime.equals(y, v.y, delta) &&
                Runtime.equals(z, v.z, delta) && Runtime.equals(w, v.w, delta)
    }

    fun equals(x: Double, y: Double, z: Double, w: Double): Boolean {
        return x == this.x && y == this.y && z == this.z && w == this.w
    }

    fun smoothStep(v: Vector4d, t: Double, dst: Vector4d = this): Vector4d {
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * x) * t2 + x * t + x
        dst.y = (y + y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * y) * t2 + y * t + y
        dst.z = (z + z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * z) * t2 + z * t + z
        dst.w = (w + w - v.w - v.w) * t3 + (3.0 * v.w - 3.0 * w) * t2 + w * t + w
        return dst
    }

    fun hermite(t0: Vector4d, v1: Vector4d, t1: Vector4d, t: Double, dst: Vector4d = this): Vector4d {
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dst.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dst.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        dst.w = (w + w - v1.w - v1.w + t1.w + t0.w) * t3 + (3.0 * v1.w - 3.0 * w - t0.w - t0.w - t1.w) * t2 + w * t + w
        return dst
    }

    fun mix(other: Vector4d, t: Double, dst: Vector4d = this): Vector4d {
        dst.x = (other.x - x) * t + x
        dst.y = (other.y - y) * t + y
        dst.z = (other.z - z) * t + z
        dst.w = (other.w - w) * t + w
        return dst
    }

    @JvmOverloads
    fun lerp(other: Vector4d, t: Double, dst: Vector4d = this): Vector4d {
        return mix(other, t, dst)
    }

    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> w
        }
    }

    fun get(dst: Vector4f): Vector4f {
        dst.x = x.toFloat()
        dst.y = y.toFloat()
        dst.z = z.toFloat()
        dst.w = w.toFloat()
        return dst
    }

    fun get(dst: Vector4d): Vector4d {
        dst.x = x
        dst.y = y
        dst.z = z
        dst.w = w
        return dst
    }

    fun max(): Double {
        return max(max(x, y), max(z, w))
    }

    fun min(): Double {
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

    fun floor(dst: Vector4d = this): Vector4d {
        return dst.set(floor(x), floor(y), floor(z), floor(w))
    }

    fun ceil(dst: Vector4d = this): Vector4d {
        return dst.set(ceil(x), ceil(y), ceil(z), ceil(w))
    }

    fun round(dst: Vector4d = this): Vector4d {
        return dst.set(round(x), round(y), round(z), round(w))
    }

    fun absolute(dst: Vector4d = this): Vector4d {
        return dst.set(abs(x), abs(y), abs(z), abs(w))
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    companion object {

        @JvmStatic
        fun lengthSquared(x: Double, y: Double, z: Double, w: Double): Double {
            return x * x + y * y + z * z + w * w
        }

        @JvmStatic
        fun length(x: Double, y: Double, z: Double, w: Double): Double {
            return sqrt(lengthSquared(x, y, z, w))
        }

        @JvmStatic
        fun distance(
            x1: Double, y1: Double, z1: Double, w1: Double,
            x2: Double, y2: Double, z2: Double, w2: Double
        ): Double {
            return sqrt(distanceSquared(x1, y1, z1, w1, x2, y2, z2, w2))
        }

        @JvmStatic
        fun distanceSquared(
            x1: Double, y1: Double, z1: Double, w1: Double,
            x2: Double, y2: Double, z2: Double, w2: Double
        ): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return lengthSquared(dx, dy, dz, dw)
        }
    }
}