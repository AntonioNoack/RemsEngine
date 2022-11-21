package org.joml

import kotlin.math.*

@Suppress("unused")
open class Vector3d {
    var x = 0.0
    var y = 0.0
    var z = 0.0

    constructor()
    constructor(d: Double) {
        x = d
        y = d
        z = d
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(v: Vector3f) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
    }

    constructor(v: Vector3i) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
    }

    constructor(v: Vector2f, z: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
    }

    constructor(v: Vector2i, z: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
    }

    constructor(v: Vector3d) {
        x = v.x
        y = v.y
        z = v.z
    }

    constructor(v: Vector2d, z: Double) {
        x = v.x
        y = v.y
        this.z = z
    }

    constructor(xyz: DoubleArray) {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
    }

    constructor(xyz: FloatArray) {
        x = xyz[0].toDouble()
        y = xyz[1].toDouble()
        z = xyz[2].toDouble()
    }

    fun set(v: Vector3d): Vector3d {
        x = v.x
        y = v.y
        z = v.z
        return this
    }

    fun set(v: Vector3i): Vector3d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        return this
    }

    fun set(v: Vector2d, z: Double): Vector3d {
        x = v.x
        y = v.y
        this.z = z
        return this
    }

    fun set(v: Vector2i, z: Double): Vector3d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        return this
    }

    fun set(v: Vector3f): Vector3d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        return this
    }

    fun set(v: Vector2f, z: Double): Vector3d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        return this
    }

    fun set(d: Double): Vector3d {
        x = d
        y = d
        z = d
        return this
    }

    fun set(x: Double, y: Double, z: Double): Vector3d {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(xyz: DoubleArray): Vector3d {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
        return this
    }

    fun set(xyz: FloatArray): Vector3d {
        x = xyz[0].toDouble()
        y = xyz[1].toDouble()
        z = xyz[2].toDouble()
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

    fun sub(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        return dst
    }

    fun sub(v: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = x - v.x.toDouble()
        dst.y = y - v.y.toDouble()
        dst.z = z - v.z.toDouble()
        return dst
    }

    fun sub(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        return dst
    }

    fun add(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        return dst
    }

    fun add(v: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = x + v.x.toDouble()
        dst.y = y + v.y.toDouble()
        dst.z = z + v.z.toDouble()
        return dst
    }

    fun add(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        return dst
    }

    fun fma(a: Vector3f, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(a.x.toDouble(), b.x.toDouble(), x)
        dst.y = JomlMath.fma(a.y.toDouble(), b.y.toDouble(), y)
        dst.z = JomlMath.fma(a.z.toDouble(), b.z.toDouble(), z)
        return dst
    }

    fun fma(a: Vector3d, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(a.x, b.x, x)
        dst.y = JomlMath.fma(a.y, b.y, y)
        dst.z = JomlMath.fma(a.z, b.z, z)
        return dst
    }

    fun fma(a: Double, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(a, b.x, x)
        dst.y = JomlMath.fma(a, b.y, y)
        dst.z = JomlMath.fma(a, b.z, z)
        return dst
    }

    fun fma(a: Vector3d, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(a.x, b.x.toDouble(), x)
        dst.y = JomlMath.fma(a.y, b.y.toDouble(), y)
        dst.z = JomlMath.fma(a.z, b.z.toDouble(), z)
        return dst
    }

    fun fma(a: Double, b: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(a, b.x.toDouble(), x)
        dst.y = JomlMath.fma(a, b.y.toDouble(), y)
        dst.z = JomlMath.fma(a, b.z.toDouble(), z)
        return dst
    }

    fun mulAdd(a: Vector3d, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(x, a.x, b.x)
        dst.y = JomlMath.fma(y, a.y, b.y)
        dst.z = JomlMath.fma(z, a.z, b.z)
        return dst
    }

    fun mulAdd(a: Double, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(x, a, b.x)
        dst.y = JomlMath.fma(y, a, b.y)
        dst.z = JomlMath.fma(z, a, b.z)
        return dst
    }

    fun mulAdd(a: Vector3f, b: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(x, a.x.toDouble(), b.x)
        dst.y = JomlMath.fma(y, a.y.toDouble(), b.y)
        dst.z = JomlMath.fma(z, a.z.toDouble(), b.z)
        return dst
    }

    fun mul(v: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = x * v.x.toDouble()
        dst.y = y * v.y.toDouble()
        dst.z = z * v.z.toDouble()
        return dst
    }

    fun mul(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        return dst
    }

    fun div(v: Vector3f, dst: Vector3d = this): Vector3d {
        dst.x = x / v.x.toDouble()
        dst.y = y / v.y.toDouble()
        dst.z = z / v.z.toDouble()
        return dst
    }

    fun div(v: Vector3d, dst: Vector3d = this): Vector3d {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        return dst
    }

    fun mulProject(mat: Matrix4d, w: Double, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulProject(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30))) * invW
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31))) * invW
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val invW = 1.0 / JomlMath.fma(
            mat.m03.toDouble(),
            x,
            JomlMath.fma(mat.m13.toDouble(), y, JomlMath.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = (mat.m00.toDouble() * x + mat.m10.toDouble() * y + mat.m20.toDouble() * z + mat.m30.toDouble()) * invW
        val ry = (mat.m01.toDouble() * x + mat.m11.toDouble() * y + mat.m21.toDouble() * z + mat.m31.toDouble()) * invW
        val rz = (mat.m02.toDouble() * x + mat.m12.toDouble() * y + mat.m22.toDouble() * z + mat.m32.toDouble()) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3d, dst: Vector3f): Vector3f {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        dst.x = rx.toFloat()
        dst.y = ry.toFloat()
        dst.z = rz.toFloat()
        return dst
    }

    fun mul(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = JomlMath.fma(mat.m01.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = JomlMath.fma(mat.m02.toDouble(), x, JomlMath.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(mat: Matrix3x2d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        dst.x = rx
        dst.y = ry
        dst.z = z
        return dst
    }

    fun mul(mat: Matrix3x2f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = JomlMath.fma(mat.m01.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        dst.x = rx
        dst.y = ry
        dst.z = z
        return dst
    }

    fun mulTranspose(mat: Matrix3d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, mat.m02 * z))
        val ry = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, mat.m12 * z))
        val rz = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTranspose(mat: Matrix3f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = JomlMath.fma(mat.m10.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = JomlMath.fma(mat.m20.toDouble(), x, JomlMath.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPosition(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposePosition(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, JomlMath.fma(mat.m02, z, mat.m03)))
        val ry = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m12, z, mat.m13)))
        val rz = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, JomlMath.fma(mat.m22, z, mat.m23)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposePosition(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m01.toDouble(), y, JomlMath.fma(mat.m02.toDouble(), z, mat.m03.toDouble()))
        )
        val ry = JomlMath.fma(
            mat.m10.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m12.toDouble(), z, mat.m13.toDouble()))
        )
        val rz = JomlMath.fma(
            mat.m20.toDouble(),
            x,
            JomlMath.fma(mat.m21.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m23.toDouble()))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulPositionW(mat: Matrix4f, dst: Vector3d = this): Double {
        val w = JomlMath.fma(
            mat.m03.toDouble(),
            x,
            JomlMath.fma(mat.m13.toDouble(), y, JomlMath.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return w
    }

    fun mulPositionW(mat: Matrix4d, dst: Vector3d = this): Double {
        val w = JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return w
    }

    fun mulDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = JomlMath.fma(mat.m01.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = JomlMath.fma(mat.m02.toDouble(), x, JomlMath.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4x3d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, mat.m20 * z))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, mat.m21 * z))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulDirection(mat: Matrix4x3f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = JomlMath.fma(mat.m01.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = JomlMath.fma(mat.m02.toDouble(), x, JomlMath.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposeDirection(mat: Matrix4d, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, mat.m02 * z))
        val ry = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, mat.m12 * z))
        val rz = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, mat.m22 * z))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mulTransposeDirection(mat: Matrix4f, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(mat.m00.toDouble(), x, JomlMath.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = JomlMath.fma(mat.m10.toDouble(), x, JomlMath.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = JomlMath.fma(mat.m20.toDouble(), x, JomlMath.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
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
    fun length() = sqrt(lengthSquared())

    fun normalize(dst: Vector3d = this) = mul(1.0 / length(), dst)
    fun normalize(length: Double, dst: Vector3d = this) = mul(length / length(), dst)

    fun cross(v: Vector3d, dst: Vector3d = this) = cross(v.x, v.y, v.z, dst)
    fun cross(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        val rx = JomlMath.fma(this.y, z, -this.z * y)
        val ry = JomlMath.fma(this.z, x, -this.x * z)
        val rz = JomlMath.fma(this.x, y, -this.y * x)
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
        return JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz))
    }

    fun dot(v: Vector3d): Double {
        return JomlMath.fma(x, v.x, JomlMath.fma(y, v.y, z * v.z))
    }

    fun dot(x: Double, y: Double, z: Double): Double {
        return JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
    }

    fun angleCos(v: Vector3d): Double {
        val length1Squared = JomlMath.fma(x, x, JomlMath.fma(y, y, z * z))
        val length2Squared = JomlMath.fma(v.x, v.x, JomlMath.fma(v.y, v.y, v.z * v.z))
        val dot = JomlMath.fma(x, v.x, JomlMath.fma(y, v.y, z * v.z))
        return dot / sqrt(length1Squared * length2Squared)
    }

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
        return if (this === other) {
            true
        } else if (other !is Vector3d) {
            false
        } else x == other.x && y == other.y && z == other.z
    }

    fun equals(v: Vector3d?, delta: Double): Boolean {
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

    fun equals(x: Double, y: Double, z: Double): Boolean {
        return if ((this.x) != (x)) {
            false
        } else if ((this.y) != (y)) {
            false
        } else {
            (this.z) == (z)
        }
    }

    fun reflect(normal: Vector3d): Vector3d {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(x: Double, y: Double, z: Double): Vector3d {
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(normal: Vector3d, dst: Vector3d = this): Vector3d {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
        dst.x = this.x - (dot + dot) * x
        dst.y = this.y - (dot + dot) * y
        dst.z = this.z - (dot + dot) * z
        return dst
    }

    fun reflect(x: Double, y: Double, z: Double, dst: Vector3d = this): Vector3d {
        val dot = JomlMath.fma(this.x, x, JomlMath.fma(this.y, y, this.z * z))
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
        x = JomlMath.fma(other.x - x, t, x)
        y = JomlMath.fma(other.y - y, t, y)
        z = JomlMath.fma(other.z - z, t, z)
        return this
    }

    fun lerp(other: Vector3d, t: Double, dst: Vector3d = this): Vector3d {
        dst.x = JomlMath.fma(other.x - x, t, x)
        dst.y = JomlMath.fma(other.y - y, t, y)
        dst.z = JomlMath.fma(other.z - z, t, z)
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
            return JomlMath.fma(dx, dx, JomlMath.fma(dy, dy, dz * dz))
        }
    }
}