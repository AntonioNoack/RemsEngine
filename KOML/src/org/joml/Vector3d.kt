package org.joml

import kotlin.math.*

@Suppress("unused")
open class Vector3d {
    var x = 0.0
    var y = 0.0
    var z = 0.0

    constructor()
    constructor(v: Double) {
        set(v)
    }

    constructor(x: Double, y: Double, z: Double) {
        set(x, y, z)
    }

    constructor(v: Vector3f) {
        set(v)
    }

    constructor(v: Vector3i) {
        set(v)
    }

    constructor(v: Vector2f, z: Double) {
        set(v, z)
    }

    constructor(v: Vector2i, z: Double) {
        set(v, z)
    }

    constructor(v: Vector3d) {
        set(v)
    }

    constructor(v: Vector2d, z: Double) {
        set(v, z)
    }

    constructor(xyz: DoubleArray) {
        set(xyz)
    }

    constructor(xyz: FloatArray) {
        set(xyz)
    }

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
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Double, dst: Vector3d = this): Vector3d = sub(v, v, v, dst)
    fun sub(v: Vector3d, dst: Vector3d = this): Vector3d = sub(v.x, v.y, v.z, dst)
    fun sub(v: Vector3f, dst: Vector3d = this): Vector3d = sub(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    fun sub(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        return dst
    }

    fun add(v: Double, dst: Vector3d = this): Vector3d = add(v, v, v, dst)
    fun add(v: Vector3d, dst: Vector3d = this): Vector3d = add(v.x, v.y, v.z, dst)
    fun add(v: Vector3f, dst: Vector3d = this): Vector3d = add(v.x.toDouble(), v.y.toDouble(), v.z.toDouble(), dst)
    fun add(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        return dst
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
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        return dst
    }

    fun mul(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        return dst
    }

    fun div(v: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        return dst
    }

    fun div(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        return dst
    }

    fun mulProject(mat: Matrix4d, w: Double, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33 * w)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30 * w))) * invW
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31 * w))) * invW
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32 * w))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulProject(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30))) * invW
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31))) * invW
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30) * invW
        val ry = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31) * invW
        val rz = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3d, dst: Vector3f): Vector3f {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        dst.x = rx.toFloat()
        dst.y = ry.toFloat()
        dst.z = rz.toFloat()
        return dst
    }

    fun mul(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        val rz = mat.m02 * x + mat.m12 * y + mat.m22 * z
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3x2d, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        dst.x = rx
        dst.y = ry
        dst.z = z
        return dst
    }

    fun mul(mat: Matrix3x2f, dst: Vector3d = this): Vector3d {
        val rx = mat.m00 * x + mat.m10 * y + mat.m20 * z
        val ry = mat.m01 * x + mat.m11 * y + mat.m21 * z
        dst.x = rx
        dst.y = ry
        dst.z = z
        return dst
    }

    fun mulTranspose(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTranspose(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposePosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + (mat.m02 * z + mat.m03)))
        val ry = (mat.m10 * x + (mat.m11 * y + (mat.m12 * z + mat.m13)))
        val rz = (mat.m20 * x + (mat.m21 * y + (mat.m22 * z + mat.m23)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposePosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + (mat.m02 * z + mat.m03)))
        val ry = (mat.m10 * x + (mat.m11 * y + (mat.m12 * z + mat.m13)))
        val rz = (mat.m20 * x + (mat.m21 * y + (mat.m22 * z + mat.m23)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPositionW(mat: Matrix4f, dst: Vector3d = this): Double {
        val w = (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return w
    }

    fun mulPositionW(mat: Matrix4d, dst: Vector3d = this): Double {
        val w = (mat.m03 * x + (mat.m13 * y + (mat.m23 * z + mat.m33)))
        val rx = (mat.m00 * x + (mat.m10 * y + (mat.m20 * z + mat.m30)))
        val ry = (mat.m01 * x + (mat.m11 * y + (mat.m21 * z + mat.m31)))
        val rz = (mat.m02 * x + (mat.m12 * y + (mat.m22 * z + mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return w
    }

    fun mulDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m10 * y + mat.m20 * z))
        val ry = (mat.m01 * x + (mat.m11 * y + mat.m21 * z))
        val rz = (mat.m02 * x + (mat.m12 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposeDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposeDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = (mat.m00 * x + (mat.m01 * y + mat.m02 * z))
        val ry = (mat.m10 * x + (mat.m11 * y + mat.m12 * z))
        val rz = (mat.m20 * x + (mat.m21 * y + mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(scalar: Double, dst: Vector3d = this): Vector3d {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        return dst
    }

    fun mul(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x * x
        dst.y = this.y * y
        dst.z = this.z * z
        return dst
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

    fun rotateAxis(angle: Double, aX: Double, aY: Double, aZ: Double, dst: Vector3d = this): Vector3d {
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

    private fun rotateAxisInternal(angle: Double, aX: Double, aY: Double, aZ: Double, dst: Vector3d = this): Vector3d {
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

    fun rotateX(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun rotateY(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun rotateZ(angle: Double, dst: Vector3d = this): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun div(scalar: Double, dst: Vector3d = this) = mul(1.0 / scalar, dst)

    fun div(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x / x
        dst.y = this.y / y
        dst.z = this.z / z
        return dst
    }

    fun lengthSquared() = x * x + y * y + z * z
    fun length(): Double {
        val ls = lengthSquared()
        return if (ls.isInfinite()) {
            val f1 = 2.225e-307
            val lx = x * f1
            val ly = y * f1
            val lz = z * f1
            sqrt(lx * lx + ly * ly + lz * lz) / f1
        } else sqrt(ls)
    }

    fun normalize(dst: Vector3d = this) = mul(1.0 / length(), dst)
    fun normalize(length: Double, dst: Vector3d = this) = mul(length / length(), dst)

    fun cross(v: Vector3d, dst: Vector3d = this) = cross(v.x, v.y, v.z, dst)
    fun cross(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        val rx = (this.y * z + -this.z * y)
        val ry = (this.z * x + -this.x * z)
        val rz = (this.x * y + -this.y * x)
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun distance(v: Vector3d) = distance(v.x, v.y, v.z)
    fun distance(x: Double, y: Double, z: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceSquared(v: Vector3d) = distanceSquared(v.x, v.y, v.z)
    fun distanceSquared(x: Double, y: Double, z: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return (dx * dx + (dy * dy + dz * dz))
    }

    fun dot(v: Vector3d): Double {
        return (x * v.x + (y * v.y + z * v.z))
    }

    fun dot(x: Double, y: Double, z: Double): Double {
        return (this.x * x + (this.y * y + this.z * z))
    }

    fun angleCos(v: Vector3d) = dot(v) / sqrt(lengthSquared() * v.lengthSquared())

    fun angle(v: Vector3d): Double {
        var cos = angleCos(v)
        cos = min(cos, 1.0)
        cos = max(cos, -1.0)
        return acos(cos)
    }

    fun angleSigned(v: Vector3d, n: Vector3d): Double {
        val x = v.x
        val y = v.y
        val z = v.z
        return atan2(
            (this.y * z - this.z * y) * n.x + (this.z * x - this.x * z) * n.y + (this.x * y - this.y * x) * n.z,
            this.x * x + this.y * y + this.z * z
        )
    }

    fun angleSigned(x: Double, y: Double, z: Double, nx: Double, ny: Double, nz: Double): Double {
        return atan2(
            (this.y * z - this.z * y) * nx + (this.z * x - this.x * z) * ny + (this.x * y - this.y * x) * nz,
            this.x * x + this.y * y + this.z * z
        )
    }

    fun min(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        return dst
    }

    fun max(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        return dst
    }

    fun zero() = set(0.0, 0.0, 0.0)

    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun negate(dst: Vector3d = this): Vector3d {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        return dst
    }

    fun absolute(dst: Vector3d = this): Vector3d {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        return dst
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = (x).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (y).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (z).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
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

    fun reflect(normal: Vector3d): Vector3d {
        val dot = dot(normal) * 2.0
        sub(dot * normal.x, dot * normal.y, dot * normal.z)
        return this
    }

    fun reflect(x: Double, y: Double, z: Double): Vector3d {
        val dot = dot(x, y, z) * 2.0
        this.x -= dot * x
        this.y -= dot * y
        this.z -= dot * z
        return this
    }

    fun reflect(normal: Vector3d, dst: Vector3d = this): Vector3d {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = dot(normal)
        dst.x = this.x - (dot + dot) * x
        dst.y = this.y - (dot + dot) * y
        dst.z = this.z - (dot + dot) * z
        return dst
    }

    fun reflect(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        val dot = (this.x * x + (this.y * y + this.z * z))
        dst.x = this.x - (dot + dot) * x
        dst.y = this.y - (dot + dot) * y
        dst.z = this.z - (dot + dot) * z
        return dst
    }

    fun half(other: Vector3d): Vector3d {
        return this.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(x: Double, y: Double, z: Double): Vector3d {
        return this.set(this).add(x, y, z).normalize()
    }

    fun half(other: Vector3d, dst: Vector3d = this): Vector3d {
        return dst.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        return dst.set(this).add(x, y, z).normalize()
    }

    fun smoothStep(v: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * x) * t2 + x * t + x
        dst.y = (y + y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * y) * t2 + y * t + y
        dst.z = (z + z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * z) * t2 + z * t + z
        return dst
    }

    fun hermite(t0: Vector3d, v1: Vector3d, t1: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        dst.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dst.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dst.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        return dst
    }

    fun lerp(other: Vector3d, t: Double): Vector3d {
        x = (other.x - x) * t + x
        y = (other.y - y) * t + y
        z = (other.z - z) * t + z
        return this
    }

    fun lerp(other: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        dst.x = (other.x - x) * t + x
        dst.y = (other.y - y) * t + y
        dst.z = (other.z - z) * t + z
        return dst
    }

    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IllegalArgumentException()
        }
    }

    operator fun get(dst: Vector3f): Vector3f {
        dst.x = x.toFloat()
        dst.y = y.toFloat()
        dst.z = z.toFloat()
        return dst
    }

    operator fun get(dst: Vector3d = this): Vector3d {
        dst.x = x
        dst.y = y
        dst.z = z
        return dst
    }

    fun maxComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        return if (absX >= absY && absX >= absZ) 0 else if (absY >= absZ) 1 else 2
    }

    fun minComponent(): Int {
        val absX = abs(x)
        val absY = abs(y)
        val absZ = abs(z)
        return if (absX < absY && absX < absZ) 0 else if (absY < absZ) 1 else 2
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

    fun floor(): Vector3d {
        x = floor(x)
        y = floor(y)
        z = floor(z)
        return this
    }

    fun floor(dst: Vector3d = this): Vector3d {
        dst.x = floor(x)
        dst.y = floor(y)
        dst.z = floor(z)
        return dst
    }

    fun ceil(): Vector3d {
        x = ceil(x)
        y = ceil(y)
        z = ceil(z)
        return this
    }

    fun ceil(dst: Vector3d = this): Vector3d {
        dst.x = ceil(x)
        dst.y = ceil(y)
        dst.z = ceil(z)
        return dst
    }

    fun round(): Vector3d {
        x = round(x)
        y = round(y)
        z = round(z)
        return this
    }

    fun round(dst: Vector3d = this): Vector3d {
        dst.x = round(x)
        dst.y = round(y)
        dst.z = round(z)
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z)

    operator fun plus(s: Vector3d) = Vector3d(x + s.x, y + s.y, z + s.z)
    operator fun minus(s: Vector3d) = Vector3d(x - s.x, y - s.y, z - s.z)
    operator fun times(s: Double) = Vector3d(x * s, y * s, z * s)

    fun safeNormalize(length: Double = 1.0): Vector3d {
        normalize(length)
        if (!isFinite) set(0.0)
        return this
    }

    fun roundToInt(dst: Vector3i = Vector3i()) = dst.set(x.roundToInt(), y.roundToInt(), z.roundToInt())
    fun floorToInt(dst: Vector3i = Vector3i()) =
        dst.set(kotlin.math.floor(x).toInt(), kotlin.math.floor(y).toInt(), kotlin.math.floor(z).toInt())

    fun findSecondAxis(dst: Vector3d = Vector3d()): Vector3d {
        val thirdAxis = if (abs(x) > abs(y)) dst.set(0.0, 1.0, 0.0)
        else dst.set(1.0, 0.0, 0.0)
        return cross(thirdAxis, dst).normalize()
    }

    fun findSystem(dstY: Vector3d = Vector3d(), dstZ: Vector3d = Vector3d()) {
        findSecondAxis(dstY)
        cross(dstY, dstZ).normalize()
    }

    fun rotateInv(q: Quaternionf, dst: Vector3d = this): Vector3d {
        synchronized(q) {
            q.conjugate()
            q.transform(this, dst)
            q.conjugate()
        }
        return dst
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
        return dst.identity().rotateYXZ(y, x, z)
    }

    fun rotate(q: Quaternionf): Vector3d {
        return Quaterniond.transform(
            q.x.toDouble(), q.y.toDouble(), q.z.toDouble(), q.w.toDouble(),
            x, y, z, this
        )
    }

    companion object {
        @JvmStatic
        fun lengthSquared(x: Double, y: Double, z: Double) = x * x + y * y + z * z

        @JvmStatic
        fun length(x: Double, y: Double, z: Double) = sqrt(x * x + y * y + z * z)

        @JvmStatic
        fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            return sqrt(distanceSquared(x1, y1, z1, x2, y2, z2))
        }

        @JvmStatic
        fun distanceSquared(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return dx * dx + dy * dy + dz * dz
        }
    }
}