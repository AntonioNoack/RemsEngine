package org.joml

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Vector3f(
    @JvmField var x: Float,
    @JvmField var y: Float,
    @JvmField var z: Float
) : Vector {

    constructor() : this(0f, 0f, 0f)
    constructor(d: Float) : this(d, d, d)
    constructor(v: Vector3f) : this(v.x, v.y, v.z)
    constructor(v: Vector4f) : this(v.x, v.y, v.z)
    constructor(v: Vector3d) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    constructor(v: Vector3i) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    constructor(v: Vector2f, z: Float) : this(v.x, v.y, z)
    constructor(v: Vector2i, z: Float) : this(v.x.toFloat(), v.y.toFloat(), z)
    constructor(xyz: FloatArray, i: Int) : this(xyz[i], xyz[i + 1], xyz[i + 2])
    constructor(xyz: FloatArray) : this(xyz, 0)
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())

    override val numComponents: Int get() = 3
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toFloat())
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    fun set(v: Vector3f): Vector3f = set(v.x, v.y, v.z)
    fun set(v: Vector4f): Vector3f = set(v.x, v.y, v.z)
    fun set(v: Vector3d): Vector3f = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun set(v: Vector3i): Vector3f = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun set(v: Vector2f, z: Float): Vector3f = set(v.x, v.y, z)
    fun set(v: Vector2d, z: Float): Vector3f = set(v.x.toFloat(), v.y.toFloat(), z)
    fun set(v: Vector2i, z: Float): Vector3f = set(v.x.toFloat(), v.y.toFloat(), z)
    fun set(v: Float): Vector3f = set(v, v, v)
    fun set(v: Double): Vector3f = set(v, v, v)
    fun set(x: Double, y: Double, z: Double): Vector3f = set(x.toFloat(), y.toFloat(), z.toFloat())
    fun set(v: FloatArray) = set(v[0], v[1], v[2])
    fun set(v: FloatArray, i: Int) = set(v[i], v[i + 1], v[i + 2])
    fun set(x: Float, y: Float, z: Float): Vector3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    operator fun set(component: Int, value: Float) = setComponent(component, value)
    fun setComponent(component: Int, value: Float): Vector3f {
        when (component) {
            0 -> x = value
            1 -> y = value
            else -> z = value
        }
        return this
    }

    fun sub(v: Float, dst: Vector3f = this): Vector3f = sub(v, v, v, dst)
    fun sub(v: Vector3f, dst: Vector3f = this): Vector3f = sub(v.x, v.y, v.z, dst)
    fun sub(vx: Float, vy: Float, vz: Float, dst: Vector3f = this): Vector3f {
        return dst.set(x - vx, y - vy, z - vz)
    }

    fun add(v: Float, dst: Vector3f = this): Vector3f = add(v, v, v, dst)
    fun add(v: Vector3f, dst: Vector3f = this): Vector3f = add(v.x, v.y, v.z, dst)
    fun add(vx: Float, vy: Float, vz: Float, dst: Vector3f = this): Vector3f {
        return dst.set(x + vx, y + vy, z + vz)
    }

    fun fma(a: Vector3f, b: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = a.x * b.x + x
        dst.y = a.y * b.y + y
        dst.z = a.z * b.z + z
        return dst
    }

    fun fma(a: Float, b: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = a * b.x + x
        dst.y = a * b.y + y
        dst.z = a * b.z + z
        return dst
    }

    fun mulAdd(a: Vector3f, b: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        dst.z = z * a.z + b.z
        return dst
    }

    fun mulAdd(a: Vector3d, b: Vector3d, dst: Vector3d): Vector3d {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        dst.z = z * a.z + b.z
        return dst
    }

    fun mulAdd(a: Float, b: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    fun mulAdd(a: Double, b: Vector3d, dst: Vector3d): Vector3d {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    fun mul(v: Vector3f, dst: Vector3f = this): Vector3f {
        return dst.set(x * v.x, y * v.y, z * v.z)
    }

    fun mul(v: Double, dst: Vector3d): Vector3d {
        return dst.set(x * v, y * v, z * v)
    }

    fun div(v: Vector3f, dst: Vector3f = this): Vector3f {
        return dst.set(x / v.x, y / v.y, z / v.z)
    }

    fun mulProject(mat: Matrix4f, dst: Vector3f = this) = mulProject(mat, 1f, dst)
    fun mulProject(mat: Matrix4f, w: Float, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        ).div(mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
    }

    fun mul(mat: Matrix3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mul(mat: Matrix3d, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mul(mat: Matrix3x2f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z, z
        )
    }

    fun mulTranspose(mat: Matrix3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m01 * y + mat.m02 * z,
            mat.m10 * x + mat.m11 * y + mat.m12 * z,
            mat.m20 * x + mat.m21 * y + mat.m22 * z
        )
    }

    @JvmOverloads
    fun mulPosition(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32
        )
    }

    @JvmOverloads
    fun mulPosition(mat: Matrix4x3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32
        )
    }

    @JvmOverloads
    fun mulPosition(mat: Matrix4x3, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32
        )
    }

    @JvmOverloads
    fun mulPosition(mat: Matrix4x3d, dst: Vector3f = this): Vector3f {
        val x = x.toDouble()
        val y = y.toDouble()
        val z = z.toDouble()
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32
        )
    }

    fun mulTransposePosition(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m01 * y + mat.m02 * z + mat.m03,
            mat.m10 * x + mat.m11 * y + mat.m12 * z + mat.m13,
            mat.m20 * x + mat.m21 * y + mat.m22 * z + mat.m23
        )
    }

    fun mulPositionW(mat: Matrix4f, dst: Vector3f = this): Float {
        val x = x
        val y = y
        val z = z
        val w = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33
        dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30,
            mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31,
            mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32
        )
        return w
    }

    fun mulDirection(mat: Matrix4d, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mulDirection(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mulDirection(mat: Matrix4x3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mulDirection(mat: Matrix4x3, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mulDirection(mat: Matrix4x3d, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m10 * y + mat.m20 * z,
            mat.m01 * x + mat.m11 * y + mat.m21 * z,
            mat.m02 * x + mat.m12 * y + mat.m22 * z
        )
    }

    fun mulTransposeDirection(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        return dst.set(
            mat.m00 * x + mat.m01 * y + mat.m02 * z,
            mat.m10 * x + mat.m11 * y + mat.m12 * z,
            mat.m20 * x + mat.m21 * y + mat.m22 * z
        )
    }

    fun mul(scalar: Float, dst: Vector3f = this) = mul(scalar, scalar, scalar, dst)
    fun mul(vx: Float, vy: Float, vz: Float, dst: Vector3f = this): Vector3f {
        return dst.set(x * vx, y * vy, z * vz)
    }

    fun div(scalar: Float, dst: Vector3f = this): Vector3f {
        return mul(1f / scalar, dst)
    }

    fun div(vx: Float, vy: Float, vz: Float, dst: Vector3f = this): Vector3f {
        return dst.set(x / vx, y / vy, z / vz)
    }

    fun rotate(quat: Quaternionf, dst: Vector3f = this): Vector3f {
        return quat.transform(this, dst)
    }

    fun rotationTo(toDir: Vector3f, dst: Quaternionf): Quaternionf {
        return dst.rotationTo(this, toDir)
    }

    fun rotationTo(toDirX: Float, toDirY: Float, toDirZ: Float, dst: Quaternionf): Quaternionf {
        return dst.rotationTo(x, y, z, toDirX, toDirY, toDirZ)
    }

    fun rotateAxis(angle: Float, ax: Float, ay: Float, az: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val vx = x
        val vy = y
        val vz = z
        val dot = ax * vx + ay * vy + az * vz
        val invCos = 1.0 - cos
        return dst.set(
            vx * cos + sin * (ay * vz - az * vy) + invCos * dot * ax,
            vy * cos + sin * (az * vx - ax * vz) + invCos * dot * ay,
            vz * cos + sin * (ax * vy - ay * vx) + invCos * dot * az
        )
    }

    @JvmOverloads
    fun rotateX(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        return dst.set(x, y, z)
    }

    @JvmOverloads
    fun rotateY(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        return dst.set(x, y, z)
    }

    @JvmOverloads
    fun rotateZ(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        return dst.set(x, y, z)
    }

    @JvmOverloads
    fun normalize(dst: Vector3f = this) = mul(1f / length(), dst)

    @JvmOverloads
    fun normalize(length: Float, dst: Vector3f = this) = mul(length / length(), dst)

    @JvmOverloads
    fun cross(v: Vector3f, dst: Vector3f = this) = cross(v.x, v.y, v.z, dst)

    @JvmOverloads
    fun cross(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        val rx = this.y * z - this.z * y
        val ry = this.z * x - this.x * z
        val rz = this.x * y - this.y * x
        return dst.set(rx, ry, rz)
    }

    fun length(): Float = length(x, y, z)
    fun lengthSquared() = x * x + y * y + z * z
    fun distance(v: Vector3f) = distance(v.x, v.y, v.z)
    fun distance(vx: Float, vy: Float, vz: Float): Float = distance(x, y, z, vx, vy, vz)
    fun distanceSquared(v: Vector3f) = distanceSquared(v.x, v.y, v.z)
    fun distanceSquared(vx: Float, vy: Float, vz: Float): Float = distanceSquared(x, y, z, vx, vy, vz)

    fun lengthXZ(): Float = Vector2f.length(x, z)
    fun lengthXZSquared(): Float = Vector2f.lengthSquared(x, z)
    fun distanceXZ(v: Vector3f): Float = hypot(x - v.x, z - v.z)
    fun distanceXZ(vx: Float, vz: Float): Float = hypot(x - vx, z - vz)
    fun distanceXZSquared(v: Vector3f): Float = Vector2f.lengthSquared(x - v.x, z - v.z)
    fun distanceXZSquared(vx: Float, vz: Float): Float = Vector2f.lengthSquared(x - vx, z - vz)

    fun dot(v: Vector3f): Float = dot(v.x, v.y, v.z)
    fun dot(v: Vector3d): Double = dot(v.x, v.y, v.z)
    fun dot(vx: Float, vy: Float, vz: Float): Float = x * vx + y * vy + z * vz
    fun dot(vx: Double, vy: Double, vz: Double): Double = x * vx + y * vy + z * vz

    fun angleCos(v: Vector3f): Float = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Float, vy: Float, vz: Float): Float {
        return dot(vx, vy, vz) / sqrt(lengthSquared() * lengthSquared(vx, vy, vz))
    }

    fun angle(v: Vector3f): Float {
        var cos = angleCos(v)
        cos = min(cos, 1f)
        cos = max(cos, -1f)
        return acos(cos)
    }

    fun angleSigned(v: Vector3f, n: Vector3f): Float {
        return angleSigned(v.x, v.y, v.z, n.x, n.y, n.z)
    }

    fun angleSigned(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float): Float {
        val tx = this.x
        val ty = this.y
        val tz = this.z
        return atan2(
            (ty * z - tz * y) * nx + (tz * x - tx * z) * ny + (tx * y - ty * x) * nz,
            tx * x + ty * y + tz * z
        )
    }

    fun min(v: Vector3f, dst: Vector3f = this) = min(v.x, v.y, v.z, dst)
    fun min(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = min(this.x, x)
        dst.y = min(this.y, y)
        dst.z = min(this.z, z)
        return dst
    }

    fun max(v: Vector3f, dst: Vector3f = this) = max(v.x, v.y, v.z, dst)
    fun max(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = max(this.x, x)
        dst.y = max(this.y, y)
        dst.z = max(this.z, z)
        return dst
    }

    fun zero(): Vector3f = set(0f)

    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun negate(dst: Vector3f = this): Vector3f {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        return dst
    }

    fun absolute(dst: Vector3f = this): Vector3f {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + x.toRawBits()
        result = 31 * result + y.toRawBits()
        result = 31 * result + z.toRawBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Vector3f && x == other.x && y == other.y && z == other.z
    }

    fun equals(v: Vector3f?, delta: Float): Boolean {
        if (this === v) return true
        return v != null &&
                Runtime.equals(x, v.x, delta) && Runtime.equals(y, v.y, delta) && Runtime.equals(z, v.z, delta)
    }

    fun equals(x: Float, y: Float, z: Float): Boolean {
        return this.x == x && this.y == y && this.z == z
    }

    @JvmOverloads
    fun reflect(normal: Vector3f, dst: Vector3f = this): Vector3f {
        return this.reflect(normal.x, normal.y, normal.z, dst)
    }

    @JvmOverloads
    fun reflect(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        val dot = this.dot(x, y, z)
        dst.x = this.x - (dot + dot) * x
        dst.y = this.y - (dot + dot) * y
        dst.z = this.z - (dot + dot) * z
        return dst
    }

    @JvmOverloads
    fun half(other: Vector3f, dst: Vector3f = this): Vector3f {
        return this.half(other.x, other.y, other.z, dst)
    }

    @JvmOverloads
    fun half(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        return dst.set(this).add(x, y, z).normalize()
    }

    fun smoothStep(v: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.smoothStep(x, v.x, t, t2, t3),
            JomlMath.smoothStep(y, v.y, t, t2, t3),
            JomlMath.smoothStep(z, v.z, t, t2, t3),
        )
    }

    fun hermite(t0: Vector3f, v1: Vector3f, t1: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.hermite(x, t0.x, v1.x, t1.x, t, t2, t3),
            JomlMath.hermite(y, t0.y, v1.y, t1.y, t, t2, t3),
            JomlMath.hermite(z, t0.z, v1.z, t1.z, t, t2, t3),
        )
    }

    fun mix(other: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        return dst.set(
            JomlMath.mix(x, other.x, t),
            JomlMath.mix(y, other.y, t),
            JomlMath.mix(z, other.z, t),
        )
    }

    @JvmOverloads
    fun lerp(other: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        return mix(other, t, dst)
    }

    operator fun get(component: Int): Float {
        return when (component) {
            0 -> x
            1 -> y
            else -> z
        }
    }

    fun get(dst: Vector3f = this): Vector3f {
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun get(dst: Vector3d): Vector3d {
        dst.x = x.toDouble()
        dst.y = y.toDouble()
        dst.z = z.toDouble()
        return dst
    }

    fun get(dst: FloatArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
    }

    fun max(): Float {
        return max(max(x, y), z)
    }

    fun min(): Float {
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

    @JvmOverloads
    fun orthogonalize(v: Vector3f, dst: Vector3f = this): Vector3f {
        val rx: Float
        val ry: Float
        val rz: Float
        if (abs(v.x) > abs(v.z)) {
            rx = -v.y
            ry = v.x
            rz = 0f
        } else {
            rx = 0f
            ry = -v.z
            rz = v.y
        }
        val invLen = JomlMath.invsqrt(rx * rx + ry * ry + rz * rz)
        dst.x = rx * invLen
        dst.y = ry * invLen
        dst.z = rz * invLen
        return dst
    }

    @JvmOverloads
    fun orthogonalizeUnit(v: Vector3f, dst: Vector3f = this): Vector3f {
        return orthogonalize(v, dst)
    }

    @JvmOverloads
    fun floor(dst: Vector3f = this): Vector3f {
        dst.x = floor(x)
        dst.y = floor(y)
        dst.z = floor(z)
        return dst
    }

    @JvmOverloads
    fun ceil(dst: Vector3f = this): Vector3f {
        dst.x = ceil(x)
        dst.y = ceil(y)
        dst.z = ceil(z)
        return dst
    }

    @JvmOverloads
    fun round(dst: Vector3f = this): Vector3f {
        dst.x = round(x)
        dst.y = round(y)
        dst.z = round(z)
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z)

    operator fun plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun times(s: Float) = Vector3f(x * s, y * s, z * s)
    operator fun times(s: Vector3f) = Vector3f(x * s.x, y * s.y, z * s.z)

    operator fun plus(s: Vector3i) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun minus(s: Vector3i) = Vector3f(x - s.x, y - s.y, z - s.z)

    fun safeNormalize(length: Float = 1f): Vector3f {
        normalize(length)
        if (!isFinite) set(0f)
        return this
    }

    fun roundToInt(dst: Vector3i = Vector3i()): Vector3i {
        return dst.set(
            if (x.isNaN()) 0 else x.roundToInt(),
            if (y.isNaN()) 0 else y.roundToInt(),
            if (z.isNaN()) 0 else z.roundToInt()
        )
    }

    fun is000(): Boolean = x == 0f && y == 0f && z == 0f
    fun is111(): Boolean = x == 1f && y == 1f && z == 1f

    fun findSecondAxis(dst: Vector3f = Vector3f()): Vector3f {
        val thirdAxis = if (abs(x) > abs(y)) dst.set(0f, 1f, 0f)
        else dst.set(1f, 0f, 0f)
        return cross(thirdAxis, dst).normalize()
    }

    fun findSystem(dstY: Vector3f = Vector3f(), dstZ: Vector3f = Vector3f()) {
        findSecondAxis(dstY)
        cross(dstY, dstZ).normalize()
    }

    fun rotateInv(q: Quaternionf, dst: Vector3f = this): Vector3f {
        synchronized(q) {
            q.conjugate()
            q.transform(this, dst)
            q.conjugate()
        }
        return dst
    }

    fun fract(dst: Vector3f = this): Vector3f =
        dst.set(Runtime.fract(x), Runtime.fract(y), Runtime.fract(z))

    fun makePerpendicular(other: Vector3f): Vector3f =
        other.mulAdd(-dot(other), this, this) // this -= dot(this,other)*other

    fun toQuaternionDegrees(dst: Quaternionf = Quaternionf()): Quaternionf {
        val fromDegrees = (PI / 180.0).toFloat()
        val x = x * fromDegrees
        val y = y * fromDegrees
        val z = z * fromDegrees
        return dst.rotationYXZ(y, x, z)
    }

    fun toQuaternionRadians(dst: Quaternionf = Quaternionf()): Quaternionf {
        return dst.rotationYXZ(y, x, z)
    }

    companion object {

        @JvmStatic
        fun angleCos(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
            val dot = ax * bx + ay * by + az * bz
            return dot / sqrt(lengthSquared(ax, ay, az) * lengthSquared(bx, by, bz))
        }

        @JvmStatic
        fun lengthSquared(x: Float, y: Float, z: Float): Float {
            return x * x + y * y + z * z
        }

        @JvmStatic
        fun length(x: Float, y: Float, z: Float): Float {
            return Vector3d.length(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
        }

        @JvmStatic
        fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            return length(x1 - x2, y1 - y2, z1 - z2)
        }

        @JvmStatic
        fun distanceSquared(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return dx * dx + dy * dy + dz * dz
        }
    }
}