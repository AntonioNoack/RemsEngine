package org.joml

import org.joml.JomlMath.hash
import org.joml.Vector4d.Companion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Vector3d(
    @JvmField var x: Double,
    @JvmField var y: Double,
    @JvmField var z: Double,
) : Vector {

    constructor() : this(0.0, 0.0, 0.0)
    constructor(v: Double) : this(v, v, v)
    constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble())
    constructor(v: Vector3f) : this(v.x, v.y, v.z)
    constructor(v: Vector3i) : this(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
    constructor(v: Vector2f, z: Double) : this(v.x.toDouble(), v.y.toDouble(), z)
    constructor(v: Vector2i, z: Double) : this(v.x.toDouble(), v.y.toDouble(), z)
    constructor(v: Vector3d) : this(v.x, v.y, v.z)
    constructor(v: Vector2d, z: Double) : this(v.x, v.y, z)
    constructor(xyz: DoubleArray, i: Int) : this(xyz[i], xyz[i + 1], xyz[i + 2])
    constructor(xyz: FloatArray, i: Int) : this(xyz[i], xyz[i + 1], xyz[i + 2])
    constructor(xyz: DoubleArray) : this(xyz, 0)
    constructor(xyz: FloatArray) : this(xyz, 0)

    override val numComponents: Int get() = 3
    override fun getComp(i: Int): Double = get(i)
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v)
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z

    fun set(v: Vector3d): Vector3d = set(v.x, v.y, v.z)
    fun set(v: Vector3i): Vector3d = set(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
    fun set(v: Vector2d, z: Double): Vector3d = set(v.x, v.y, z)
    fun set(v: Vector2i, z: Double): Vector3d = set(v.x.toDouble(), v.y.toDouble(), z)
    fun set(v: Vector3f): Vector3d = set(v.x, v.y, v.z)
    fun set(v: Vector2f, z: Double): Vector3d = set(v.x.toDouble(), v.y.toDouble(), z)
    fun set(v: Double): Vector3d = set(v, v, v)
    fun set(x: Float, y: Float, z: Float): Vector3d = set(x.toDouble(), y.toDouble(), z.toDouble())
    fun set(xyz: DoubleArray): Vector3d = set(xyz, 0)
    fun set(xyz: DoubleArray, i: Int): Vector3d = set(xyz[i], xyz[i + 1], xyz[i + 2])
    fun set(xyz: FloatArray): Vector3d = set(xyz, 0)
    fun set(xyz: FloatArray, i: Int): Vector3d = set(xyz[i], xyz[i + 1], xyz[i + 2])
    fun set(x: Double, y: Double, z: Double): Vector3d {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    operator fun set(component: Int, value: Double) = setComponent(component, value)
    fun setComponent(component: Int, value: Double): Vector3d {
        when (component) {
            0 -> x = value
            1 -> y = value
            else -> z = value
        }
        return this
    }

    fun sub(v: Double, dst: Vector3d = this): Vector3d = sub(v, v, v, dst)
    fun sub(v: Vector3d, dst: Vector3d = this): Vector3d = sub(v.x, v.y, v.z, dst)
    fun sub(v: Vector3f, dst: Vector3d = this): Vector3d = sub(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    fun sub(x: Float, y: Float, z: Float, dst: Vector3d = this): Vector3d =
        sub(x.toDouble(), y.toDouble(), z.toDouble(), dst)

    fun sub(vx: Double, vy: Double, vz: Double, dst: Vector3d = this): Vector3d {
        return dst.set(x - vx, y - vy, z - vz)
    }

    fun add(v: Double, dst: Vector3d = this): Vector3d = add(v, v, v, dst)
    fun add(v: Vector3d, dst: Vector3d = this): Vector3d = add(v.x, v.y, v.z, dst)
    fun add(v: Vector3f, dst: Vector3d = this): Vector3d = add(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    fun add(x: Float, y: Float, z: Float, dst: Vector3d = this): Vector3d =
        add(x.toDouble(), y.toDouble(), z.toDouble(), dst)

    fun add(vx: Double, vy: Double, vz: Double, dst: Vector3d = this): Vector3d {
        return dst.set(x + vx, y + vy, z + vz)
    }

    fun fma(a: Vector3f, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = (a.x * b.x + x)
        dst.y = (a.y * b.y + y)
        dst.z = (a.z * b.z + z)
        return dst
    }

    fun fma(a: Vector3d, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = (a.x * b.x + x)
        dst.y = (a.y * b.y + y)
        dst.z = (a.z * b.z + z)
        return dst
    }

    fun fma(a: Double, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = (a * b.x + x)
        dst.y = (a * b.y + y)
        dst.z = (a * b.z + z)
        return dst
    }

    fun fma(a: Vector3d, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = (a.x * b.x + x)
        dst.y = (a.y * b.y + y)
        dst.z = (a.z * b.z + z)
        return dst
    }

    fun fma(a: Double, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = (a * b.x + x)
        dst.y = (a * b.y + y)
        dst.z = (a * b.z + z)
        return dst
    }

    fun mulAdd(a: Vector3d, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = (x * a.x + b.x)
        dst.y = (y * a.y + b.y)
        dst.z = (z * a.z + b.z)
        return dst
    }

    fun mulAdd(a: Double, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = (x * a + b.x)
        dst.y = (y * a + b.y)
        dst.z = (z * a + b.z)
        return dst
    }

    fun mulAdd(a: Vector3f, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = (x * a.x + b.x)
        dst.y = (y * a.y + b.y)
        dst.z = (z * a.z + b.z)
        return dst
    }

    fun mul(v: Vector3f, dst: Vector3d = this): Vector3d {
        return mul(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    }

    fun mul(v: Vector3d, dst: Vector3d = this): Vector3d {
        return mul(v.x, v.y, v.z, dst)
    }

    fun div(v: Vector3f, dst: Vector3d = this): Vector3d {
        return div(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    }

    fun div(v: Vector3d, dst: Vector3d = this): Vector3d {
        return div(v.x, v.y, v.z, dst)
    }

    fun mulProject(mat: Matrix4d, w: Double, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33 * w)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30 * w))) * invW
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31 * w))) * invW
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32 * w))) * invW
        return dst.set(rx, ry, rz)
    }

    fun mulProject(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))) * invW
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))) * invW
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))) * invW
        return dst.set(rx, ry, rz)
    }

    fun mulProject(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30) * invW
        val ry = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31) * invW
        val rz = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32) * invW
        return dst.set(rx, ry, rz)
    }

    fun mul(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        return dst.set(rx, ry, rz)
    }

    fun mul(mat: Matrix3d, dst: Vector3f): Vector3f {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        return dst.set(rx, ry, rz)
    }

    fun mul(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        return dst.set(rx, ry, rz)
    }

    fun mul(mat: Matrix3x2d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        return dst.set(rx, ry, z)
    }

    fun mul(mat: Matrix3x2f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        return dst.set(rx, ry, z)
    }

    fun mulTranspose(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m01 * y + mat.m02 * z)
        val ry = mat.m10 * x + (mat.m11 * y + mat.m12 * z)
        val rz = mat.m20 * x + (mat.m21 * y + mat.m22 * z)
        return dst.set(rx, ry, rz)
    }

    fun mulTranspose(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m01 * y + mat.m02 * z)
        val ry = mat.m10 * x + (mat.m11 * y + mat.m12 * z)
        val rz = mat.m20 * x + (mat.m21 * y + mat.m22 * z)
        return dst.set(rx, ry, rz)
    }

    fun mulPosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))
        val ry = mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))
        val rz = mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))
        return dst.set(rx, ry, rz)
    }

    fun mulPosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))
        val ry = mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))
        val rz = mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))
        return dst.set(rx, ry, rz)
    }

    fun mulPosition(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))
        val ry = mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))
        val rz = mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))
        return dst.set(rx, ry, rz)
    }

    fun mulPosition(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))
        val ry = mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))
        val rz = mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))
        return dst.set(rx, ry, rz)
    }

    fun mulTransposePosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + (mat.m02 * z + mat.m03)))
        val ry = (mat.m10 * x + (mat.m11 * y + (mat.m12 * z + mat.m13)))
        val rz = (mat.m20 * x + (mat.m21 * y + (mat.m22 * z + mat.m23)))
        return dst.set(rx, ry, rz)
    }

    fun mulTransposePosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + (mat.m02 * z + mat.m03)))
        val ry = (mat.m10 * x + (mat.m11 * y + (mat.m12 * z + mat.m13)))
        val rz = (mat.m20 * x + (mat.m21 * y + (mat.m22 * z + mat.m23)))
        return dst.set(rx, ry, rz)
    }

    fun mulPositionW(mat: Matrix4f, dst: Vector3d = this): Double {
        val w = (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.set(rx, ry, rz)
        return w
    }

    fun mulPositionW(mat: Matrix4d, dst: Vector3d = this): Double {
        val w = (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.set(rx, ry, rz)
        return w
    }

    fun mulDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mulDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mulDirection(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mulDirection(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mulTransposeDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mulTransposeDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        return dst.set(rx, ry, rz)
    }

    fun mul(scalar: Double, dst: Vector3d = this): Vector3d {
        return dst.set(x * scalar, y * scalar, z * scalar)
    }

    fun mul(vx: Double, vy: Double, vz: Double, dst: Vector3d = this): Vector3d {
        return dst.set(x * vx, y * vy, z * vz)
    }

    fun rotate(quat: Quaternionf, dst: Vector3d = this): Vector3d {
        return Quaterniond(quat).transform(this, dst)
    }

    fun rotate(quat: Quaterniond, dst: Vector3d = this): Vector3d {
        return quat.transform(this, dst)
    }

    fun rotationTo(toDir: Vector3d, dst: Quaterniond): Quaterniond {
        return dst.rotationTo(this, toDir)
    }

    fun rotationTo(toDirX: Double, toDirY: Double, toDirZ: Double, dst: Quaterniond): Quaterniond {
        return dst.rotationTo(x, y, z, toDirX, toDirY, toDirZ)
    }

    /**
     * Warning: ax,ay,az must be normalized!
     * */
    fun rotateAxis(angle: Double, ax: Double, ay: Double, az: Double, dst: Vector3d = this): Vector3d {
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

    fun rotateX(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val ry = y * cos - z * sin
        val rz = y * sin + z * cos
        return dst.set(x, ry, rz)
    }

    fun rotateY(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val rx = x * cos + z * sin
        val rz = -x * sin + z * cos
        return dst.set(rx, y, rz)
    }

    fun rotateZ(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val rx = x * cos - y * sin
        val ry = x * sin + y * cos
        return dst.set(rx, ry, z)
    }

    fun div(scalar: Double, dst: Vector3d = this) = mul(1.0 / scalar, dst)

    fun div(vx: Double, vy: Double, vz: Double, dst: Vector3d = this): Vector3d {
        return dst.set(x / vx, y / vy, z / vz)
    }

    fun lengthSquared(): Double = lengthSquared(x, y, z)
    fun length(): Double = length(x, y, z)

    @JvmOverloads
    fun normalize(dst: Vector3d = this) = mul(1.0 / length(), dst)

    @JvmOverloads
    fun normalize(length: Double, dst: Vector3d = this) = mul(length / length(), dst)

    fun cross(v: Vector3d, dst: Vector3d = this) = cross(v.x, v.y, v.z, dst)
    fun cross(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        val rx = (this.y * z + -this.z * y)
        val ry = (this.z * x + -this.x * z)
        val rz = (this.x * y + -this.y * x)
        return dst.set(rx, ry, rz)
    }

    fun distance(v: Vector3d) = distance(v.x, v.y, v.z)
    fun distance(vx: Double, vy: Double, vz: Double): Double = distance(x, y, z, vx, vy, vz)

    fun distanceSquared(v: Vector3d): Double = distanceSquared(v.x, v.y, v.z)
    fun distanceSquared(vx: Double, vy: Double, vz: Double): Double = lengthSquared(x - vx, y - vy, z - vz)

    fun dot(v: Vector3f): Double = x * v.x + y * v.y + z * v.z
    fun dot(v: Vector3d): Double = x * v.x + y * v.y + z * v.z
    fun dot(vx: Float, vy: Float, vz: Float): Double = x * vx + y * vy + z * vz
    fun dot(vx: Double, vy: Double, vz: Double): Double = x * vx + y * vy + z * vz

    fun angleCos(v: Vector3d): Double = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Double, vy: Double, vz: Double): Double =
        dot(vx, vy, vz) / sqrt(lengthSquared() * lengthSquared(vx, vy, vz))

    /**
     * returns the angle from this to the other vector [0, PI]
     * */
    fun angle(v: Vector3d): Double {
        var cos = angleCos(v)
        cos = min(cos, 1.0)
        cos = max(cos, -1.0)
        return acos(cos)
    }

    fun angleSigned(v: Vector3d, n: Vector3d): Double {
        return angleSigned(v.x, v.y, v.z, n.x, n.y, n.z)
    }

    fun angleSigned(vx: Double, vy: Double, vz: Double, nx: Double, ny: Double, nz: Double): Double {
        return atan2(
            (y * vz - z * vy) * nx + (z * vx - x * vz) * ny + (x * vy - y * vx) * nz,
            x * vx + y * vy + z * vz
        )
    }

    fun min(v: Vector3d, dst: Vector3d = this): Vector3d {
        return dst.set(min(x, v.x), min(y, v.y), min(z, v.z))
    }

    fun max(v: Vector3d, dst: Vector3d = this): Vector3d {
        return dst.set(max(x, v.x), max(y, v.y), max(z, v.z))
    }

    fun zero(): Vector3d = set(0.0, 0.0, 0.0)

    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun negate(dst: Vector3d = this): Vector3d {
        return dst.set(-x, -y, -z)
    }

    // add this to all vector classes?
    operator fun unaryMinus() = negate(Vector3d())
    operator fun unaryPlus() = this

    fun absolute(dst: Vector3d = this): Vector3d {
        return dst.set(abs(x), abs(y), abs(z))
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + hash(x)
        result = 31 * result + hash(y)
        result = 31 * result + hash(z)
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else other is Vector3d && other.x == x && other.y == y && other.z == z
    }

    fun equals(other: Vector3d?, delta: Double): Boolean {
        return if (this === other) true
        else other is Vector3d &&
                Runtime.equals(x, other.x, delta) &&
                Runtime.equals(y, other.y, delta) &&
                Runtime.equals(z, other.z, delta)
    }

    fun equals(x: Double, y: Double, z: Double): Boolean {
        return x == this.x && y == this.y && z == this.z
    }

    @JvmOverloads
    fun reflect(normal: Vector3d, dst: Vector3d = this): Vector3d {
        return reflect(normal.x, normal.y, normal.z, dst)
    }

    @JvmOverloads
    fun reflect(vx: Double, vy: Double, vz: Double, dst: Vector3d = this): Vector3d {
        val dot = 2.0 * dot(vx, vy, vz)
        return sub(dot * vx, dot * vy, dot * vz, dst)
    }

    @JvmOverloads
    fun half(other: Vector3d, dst: Vector3d = this): Vector3d {
        return half(other.x, other.y, other.z, dst)
    }

    @JvmOverloads
    fun half(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        return add(x, y, z, dst).normalize()
    }

    fun smoothStep(v: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.smoothStep(x, v.x, t, t2, t3),
            JomlMath.smoothStep(y, v.y, t, t2, t3),
            JomlMath.smoothStep(z, v.z, t, t2, t3),
        )
    }

    fun hermite(t0: Vector3d, v1: Vector3d, t1: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.hermite(x, t0.x, v1.x, t1.x, t, t2, t3),
            JomlMath.hermite(y, t0.y, v1.y, t1.y, t, t2, t3),
            JomlMath.hermite(z, t0.z, v1.z, t1.z, t, t2, t3),
        )
    }

    fun mix(other: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        return dst.set(
            JomlMath.mix(x, other.x, t),
            JomlMath.mix(y, other.y, t),
            JomlMath.mix(z, other.z, t),
        )
    }

    @JvmOverloads
    fun lerp(other: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        return mix(other, t, dst)
    }

    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            else -> z
        }
    }

    fun get(dst: Vector3f): Vector3f {
        return dst.set(x, y, z)
    }

    fun get(dst: Vector3d): Vector3d {
        return dst.set(x, y, z)
    }

    fun get(dst: FloatArray, i: Int) {
        dst[i] = x.toFloat()
        dst[i + 1] = y.toFloat()
        dst[i + 2] = z.toFloat()
    }

    fun get(dst: DoubleArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
    }

    fun max(): Double {
        return max(max(x, y), z)
    }

    fun min(): Double {
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
    fun orthogonalize(v: Vector3d, dst: Vector3d = this): Vector3d {
        val rx: Double
        val ry: Double
        val rz: Double
        if (abs(v.x) > abs(v.z)) {
            rx = -v.y
            ry = v.x
            rz = 0.0
        } else {
            rx = 0.0
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
    fun orthogonalizeUnit(v: Vector3d, dst: Vector3d = this): Vector3d {
        return orthogonalize(v, dst)
    }

    @JvmOverloads
    fun floor(dst: Vector3d = this): Vector3d {
        return dst.set(floor(x), floor(y), floor(z))
    }

    @JvmOverloads
    fun ceil(dst: Vector3d = this): Vector3d {
        return dst.set(ceil(x), ceil(y), ceil(z))
    }

    @JvmOverloads
    fun round(dst: Vector3d = this): Vector3d {
        return dst.set(round(x), round(y), round(z))
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z)

    operator fun plus(s: Vector3d): Vector3d = Vector3d(x + s.x, y + s.y, z + s.z)
    operator fun minus(s: Vector3d): Vector3d = Vector3d(x - s.x, y - s.y, z - s.z)
    operator fun times(s: Double): Vector3d = Vector3d(x * s, y * s, z * s)
    operator fun div(s: Double): Vector3d = mul(1.0 / s)

    fun safeNormalize(length: Double = 1.0): Vector3d {
        normalize(length)
        if (!isFinite) set(0.0)
        return this
    }

    fun roundToInt(dst: Vector3i = Vector3i()): Vector3i {
        return dst.set(
            if (x.isNaN()) 0 else x.roundToInt(),
            if (y.isNaN()) 0 else y.roundToInt(),
            if (z.isNaN()) 0 else z.roundToInt()
        )
    }

    fun floorToInt(dst: Vector3i = Vector3i()): Vector3i {
        return dst.set(
            if (x.isNaN()) 0 else kotlin.math.floor(x).toInt(),
            if (y.isNaN()) 0 else kotlin.math.floor(y).toInt(),
            if (z.isNaN()) 0 else kotlin.math.floor(z).toInt()
        )
    }

    fun findSecondAxis(dst: Vector3d = Vector3d()): Vector3d {
        val thirdAxis = if (abs(x) > abs(y)) dst.set(0.0, 1.0, 0.0)
        else dst.set(1.0, 0.0, 0.0)
        return cross(thirdAxis, dst).safeNormalize()
    }

    fun findSystem(dstY: Vector3d = Vector3d(), dstZ: Vector3d = Vector3d()) {
        findSecondAxis(dstY)
        cross(dstY, dstZ).safeNormalize()
    }

    fun rotateInv(q: Quaterniond, dst: Vector3d = this): Vector3d {
        synchronized(q) {
            q.conjugate()
            q.transform(this, dst)
            q.conjugate()
        }
        return dst
    }

    fun fract(dst: Vector3d = this): Vector3d =
        dst.set(org.joml.Runtime.fract(x), org.joml.Runtime.fract(y), org.joml.Runtime.fract(z))

    fun makePerpendicular(other: Vector3d): Vector3d =
        other.mulAdd(-dot(other), this, this) // this -= dot(this,other)*other

    fun toQuaternionDegrees(dst: Quaterniond = Quaterniond()): Quaterniond {
        val fromDegrees = PI / 180.0
        val x = x * fromDegrees
        val y = y * fromDegrees
        val z = z * fromDegrees
        return dst.rotationYXZ(y, x, z)
    }

    fun toQuaternionRadians(dst: Quaterniond = Quaterniond()): Quaterniond {
        return dst.rotationYXZ(y, x, z)
    }

    companion object {
        @JvmStatic
        fun lengthSquared(x: Double, y: Double, z: Double): Double {
            return x * x + y * y + z * z
        }

        @JvmStatic
        fun length(x: Double, y: Double, z: Double): Double {
            val sq = lengthSquared(x, y, z)
            return when {
                sq < JomlMath.MIN_DOUBLE -> lengthScaled(x, y, z, Double.MAX_VALUE, JomlMath.MIN_DOUBLE)
                sq.isFinite() -> sqrt(sq)
                else -> lengthScaled(x, y, z, JomlMath.MIN_DOUBLE, Double.MAX_VALUE)
            }
        }

        @JvmStatic
        private fun lengthScaled(x: Double, y: Double, z: Double, s: Double, invS: Double): Double {
            return sqrt(lengthSquared(x * s, y * s, z * s)) * invS
        }

        @JvmStatic
        fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            return length(x2 - x1, y2 - y1, z2 - z1)
        }

        @JvmStatic
        fun distanceSquared(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return lengthSquared(dx, dy, dz)
        }
    }
}