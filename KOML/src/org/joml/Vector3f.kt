package org.joml

import kotlin.math.*

@Suppress("unused")
open class Vector3f(var x: Float, var y: Float, var z: Float) {

    constructor() : this(0f, 0f, 0f)
    constructor(d: Float) : this(d, d, d)
    constructor(v: Vector3f) : this(v.x, v.y, v.z)
    constructor(v: Vector3d) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    constructor(v: Vector3i) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    constructor(v: Vector2f, z: Float) : this(v.x, v.y, z)
    constructor(v: Vector2i, z: Float) : this(v.x.toFloat(), v.y.toFloat(), z)
    constructor(xyz: FloatArray) : this(xyz[0], xyz[1], xyz[2])
    constructor(x: Int, y: Int, z: Int) : this(x.toFloat(), y.toFloat(), z.toFloat())

    fun set(v: Vector3f): Vector3f {
        x = v.x
        y = v.y
        z = v.z
        return this
    }

    fun set(v: Vector3d) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun set(v: Vector3i) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    fun set(v: Vector2f, z: Float) = set(v.x, v.y, z)
    fun set(v: Vector2d, z: Float) = set(v.x.toFloat(), v.y.toFloat(), z)
    fun set(v: Vector2i, z: Float) = set(v.x.toFloat(), v.y.toFloat(), z)

    fun set(d: Float): Vector3f {
        x = d
        y = d
        z = d
        return this
    }

    fun set(x: Float, y: Float, z: Float): Vector3f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(d: Double): Vector3f {
        x = d.toFloat()
        y = d.toFloat()
        z = d.toFloat()
        return this
    }

    fun set(x: Double, y: Double, z: Double): Vector3f {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.z = z.toFloat()
        return this
    }

    fun set(v: FloatArray) = set(v[0], v[1], v[2])
    fun set(v: FloatArray, i: Int) = set(v[i], v[i + 1], v[i + 2])

    operator fun set(component: Int, value: Float) = setComponent(component, value)
    fun setComponent(component: Int, value: Float): Vector3f {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        return dst
    }

    fun sub(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        return dst
    }

    fun add(v: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        return dst
    }

    fun add(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        return dst
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

    fun mulAdd(a: Float, b: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    fun mul(v: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        return dst
    }

    fun div(v: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        val invW = 1f / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33)))
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30))) * invW
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31))) * invW
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32))) * invW
        return dst
    }

    fun mulProject(mat: Matrix4f, w: Float, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        val invW = 1f / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w))) * invW
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w))) * invW
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w))) * invW
        return dst
    }

    fun mul(mat: Matrix3f, dst: Vector3f = this): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        dst.x = JomlMath.fma(mat.m00, lx, JomlMath.fma(mat.m10, ly, mat.m20 * lz))
        dst.y = JomlMath.fma(mat.m01, lx, JomlMath.fma(mat.m11, ly, mat.m21 * lz))
        dst.z = JomlMath.fma(mat.m02, lx, JomlMath.fma(mat.m12, ly, mat.m22 * lz))
        return dst
    }

    fun mul(mat: Matrix3d, dst: Vector3f = this): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        dst.x = JomlMath.fma(mat.m00, lx.toDouble(), JomlMath.fma(mat.m10, ly.toDouble(), mat.m20 * lz.toDouble()))
            .toFloat()
        dst.y = JomlMath.fma(mat.m01, lx.toDouble(), JomlMath.fma(mat.m11, ly.toDouble(), mat.m21 * lz.toDouble()))
            .toFloat()
        dst.z = JomlMath.fma(mat.m02, lx.toDouble(), JomlMath.fma(mat.m12, ly.toDouble(), mat.m22 * lz.toDouble()))
            .toFloat()
        return dst
    }

    fun mul(mat: Matrix3x2f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        dst.z = z
        return dst
    }

    fun mulTranspose(mat: Matrix3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, mat.m02 * z))
        dst.y = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, mat.m12 * z))
        dst.z = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, mat.m22 * z))
        return dst
    }

    fun mulPosition(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        return dst
    }

    fun mulPosition(mat: Matrix4x3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        return dst
    }

    fun mulTransposePosition(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, JomlMath.fma(mat.m02, z, mat.m03)))
        dst.y = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m12, z, mat.m13)))
        dst.z = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, JomlMath.fma(mat.m22, z, mat.m23)))
        return dst
    }

    fun mulPositionW(mat: Matrix4f, dst: Vector3f = this): Float {
        val x = x
        val y = y
        val z = z
        val w = JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33)))
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        return w
    }

    fun mulDirection(mat: Matrix4d, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x =
            JomlMath.fma(mat.m00, x.toDouble(), JomlMath.fma(mat.m10, y.toDouble(), mat.m20 * z.toDouble())).toFloat()
        dst.y =
            JomlMath.fma(mat.m01, x.toDouble(), JomlMath.fma(mat.m11, y.toDouble(), mat.m21 * z.toDouble())).toFloat()
        dst.z =
            JomlMath.fma(mat.m02, x.toDouble(), JomlMath.fma(mat.m12, y.toDouble(), mat.m22 * z.toDouble())).toFloat()
        return dst
    }

    fun mulDirection(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        return dst
    }

    fun mulDirection(mat: Matrix4x3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        dst.y = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        dst.z = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        return dst
    }

    fun mulTransposeDirection(mat: Matrix4f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, mat.m02 * z))
        dst.y = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, mat.m12 * z))
        dst.z = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, mat.m22 * z))
        return dst
    }

    fun mul(scalar: Float, dst: Vector3f = this): Vector3f {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        return dst
    }

    fun mul(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = this.x * x
        dst.y = this.y * y
        dst.z = this.z * z
        return dst
    }

    fun div(scalar: Float, dst: Vector3f = this): Vector3f {
        val inv = 1f / scalar
        dst.x = x * inv
        dst.y = y * inv
        dst.z = z * inv
        return dst
    }

    fun div(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = this.x / x
        dst.y = this.y / y
        dst.z = this.z / z
        return dst
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

    fun rotateAxis(angle: Float, aX: Float, aY: Float, aZ: Float, dst: Vector3f = this): Vector3f {
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

    private fun rotateAxisInternal(angle: Float, aX: Float, aY: Float, aZ: Float, dst: Vector3f = this): Vector3f {
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

    fun rotateX(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun rotateY(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun rotateZ(angle: Float, dst: Vector3f = this): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun lengthSquared() = x * x + y * y + z * z
    fun length(): Float {
        val ls = lengthSquared()
        return if (ls.isInfinite()) {
            val f1 = 1.175494e-38f
            val lx = x * f1
            val ly = y * f1
            val lz = z * f1
            sqrt(lx * lx + ly * ly + lz * lz) / f1
        } else sqrt(ls)
    }

    fun normalize(dst: Vector3f = this) = mul(1f / length(), dst)
    fun normalize(length: Float, dst: Vector3f = this) = mul(length / length(), dst)

    fun cross(v: Vector3f): Vector3f {
        val rx = JomlMath.fma(y, v.z, -z * v.y)
        val ry = JomlMath.fma(z, v.x, -x * v.z)
        val rz = JomlMath.fma(x, v.y, -y * v.x)
        x = rx
        y = ry
        z = rz
        return this
    }

    fun cross(x: Float, y: Float, z: Float): Vector3f {
        val rx = JomlMath.fma(this.y, z, -this.z * y)
        val ry = JomlMath.fma(this.z, x, -this.x * z)
        val rz = JomlMath.fma(this.x, y, -this.y * x)
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun cross(v: Vector3f, dst: Vector3f = this): Vector3f {
        val rx = JomlMath.fma(y, v.z, -z * v.y)
        val ry = JomlMath.fma(z, v.x, -x * v.z)
        val rz = JomlMath.fma(x, v.y, -y * v.x)
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun cross(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        val rx = JomlMath.fma(this.y, z, -this.z * y)
        val ry = JomlMath.fma(this.z, x, -this.x * z)
        val rz = JomlMath.fma(this.x, y, -this.y * x)
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun distance(v: Vector3f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return sqrt(JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz)))
    }

    fun distance(x: Float, y: Float, z: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return sqrt(JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz)))
    }

    fun distanceSquared(v: Vector3f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz))
    }

    fun distanceSquared(x: Float, y: Float, z: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz))
    }

    fun dot(v: Vector3f): Float {
        return JomlMath.fma(x, v.x, JomlMath.fma(y, v.y, z * v.z))
    }

    fun dot(x: Float, y: Float, z: Float): Float {
        return JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
    }

    fun angleCos(v: Vector3f) = dot(v) / sqrt(lengthSquared() * v.lengthSquared())

    fun angle(v: Vector3f): Float {
        var cos = angleCos(v)
        cos = min(cos, 1f)
        cos = max(cos, -1f)
        return acos(cos)
    }

    fun angleSigned(v: Vector3f, n: Vector3f): Float {
        return this.angleSigned(v.x, v.y, v.z, n.x, n.y, n.z)
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

    fun min(v: Vector3f, dst: Vector3f = this): Vector3f {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        return dst
    }

    fun min(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = min(this.x, x)
        dst.y = min(this.y, y)
        dst.z = min(this.z, z)
        return dst
    }

    fun max(v: Vector3f, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        return dst
    }

    fun max(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        dst.x = max(this.x, x)
        dst.y = max(this.y, y)
        dst.z = max(this.z, z)
        return dst
    }

    fun zero(): Vector3f {
        x = 0f
        y = 0f
        z = 0f
        return this
    }

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
        var result = (x).toBits()
        result = 31 * result + (y).toBits()
        result = 31 * result + (z).toBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is Vector3f) false
        else x == other.x && y == other.y && z == other.z
    }

    fun equals(v: Vector3f?, delta: Float): Boolean {
        return if (this === v) {
            true
        } else if (v == null) {
            false
        } else if (!Runtime.equals(x, v.x, delta)) {
            false
        } else if (!Runtime.equals(y, v.y, delta)) {
            false
        } else {
            Runtime.equals(z, v.z, delta)
        }
    }

    fun equals(x: Float, y: Float, z: Float): Boolean {
        return if ((this.x) != (x)) {
            false
        } else if ((this.y) != (y)) {
            false
        } else {
            (this.z) == (z)
        }
    }

    fun reflect(normal: Vector3f): Vector3f {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(x: Float, y: Float, z: Float): Vector3f {
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(normal: Vector3f, dst: Vector3f = this): Vector3f {
        return this.reflect(normal.x, normal.y, normal.z, dst)
    }

    fun reflect(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        val dot = this.dot(x, y, z)
        dst.x = this.x - (dot + dot) * x
        dst.y = this.y - (dot + dot) * y
        dst.z = this.z - (dot + dot) * z
        return dst
    }

    fun half(other: Vector3f): Vector3f {
        return this.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(other: Vector3f, dst: Vector3f = this): Vector3f {
        return this.half(other.x, other.y, other.z, dst)
    }

    @JvmOverloads
    fun half(x: Float, y: Float, z: Float, dst: Vector3f = this): Vector3f {
        return dst.set(this).add(x, y, z).normalize()
    }

    fun smoothStep(v: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x
        dst.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y
        dst.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z
        return dst
    }

    fun hermite(t0: Vector3f, v1: Vector3f, t1: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        val x = x
        val y = y
        val z = z
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dst.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dst.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        return dst
    }

    @JvmOverloads
    fun lerp(other: Vector3f, t: Float, dst: Vector3f = this): Vector3f {
        dst.x = JomlMath.fma(other.x - x, t, x)
        dst.y = JomlMath.fma(other.y - y, t, y)
        dst.z = JomlMath.fma(other.z - z, t, z)
        return dst
    }

    operator fun get(component: Int): Float {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IllegalArgumentException()
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

    fun maxComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        return if (absX >= absY && absX >= absZ) {
            0
        } else {
            if (absY >= absZ) 1 else 2
        }
    }

    fun minComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        return if (absX < absY && absX < absZ) {
            0
        } else {
            if (absY < absZ) 1 else 2
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

    fun roundToInt(dst: Vector3i = Vector3i()) = dst.set(x.roundToInt(), y.roundToInt(), z.roundToInt())
    fun is000() = x == 0f && y == 0f && z == 0f
    fun is111() = x == 1f && y == 1f && z == 1f

    fun addSmoothly(other: Vector3f, scale: Float): Vector3f {
        mul(1f - scale)
        other.mulAdd(scale, this, this)
        return this
    }

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

    fun rotateInv(q: Quaterniond, dst: Vector3f = this): Vector3f {
        synchronized(q) {
            q.conjugate()
            q.transform(this, dst)
            q.conjugate()
        }
        return dst
    }

    fun fract(dst: Vector3f = this): Vector3f =
        dst.set(org.joml.Runtime.fract(x), org.joml.Runtime.fract(y), org.joml.Runtime.fract(z))

    fun makePerpendicular(other: Vector3f): Vector3f =
        other.mulAdd(-dot(other), this, this) // this -= dot(this,other)*other

    companion object {
        @JvmStatic
        fun lengthSquared(x: Float, y: Float, z: Float): Float {
            return x * x + y * y + z * z
        }

        @JvmStatic
        fun length(x: Float, y: Float, z: Float): Float {
            return sqrt(x * x + y * y + z * z)
        }

        @JvmStatic
        fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            return sqrt(distanceSquared(x1, y1, z1, x2, y2, z2))
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