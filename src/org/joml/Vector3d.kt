package org.joml

class Vector3d : Cloneable {
    var x = 0.0
    var y = 0.0
    var z = 0.0

    constructor() {}
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

    operator fun set(v: Vector2d, z: Double): Vector3d {
        x = v.x
        y = v.y
        this.z = z
        return this
    }

    operator fun set(v: Vector2i, z: Double): Vector3d {
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

    operator fun set(v: Vector2f, z: Double): Vector3d {
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

    operator fun set(x: Double, y: Double, z: Double): Vector3d {
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

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Double): Vector3d {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector3d): Vector3d {
        x -= v.x
        y -= v.y
        z -= v.z
        return this
    }

    fun sub(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        return dest
    }

    fun sub(v: Vector3f): Vector3d {
        x -= v.x.toDouble()
        y -= v.y.toDouble()
        z -= v.z.toDouble()
        return this
    }

    fun sub(v: Vector3f, dest: Vector3d): Vector3d {
        dest.x = x - v.x.toDouble()
        dest.y = y - v.y.toDouble()
        dest.z = z - v.z.toDouble()
        return dest
    }

    fun sub(x: Double, y: Double, z: Double): Vector3d {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    fun sub(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        return dest
    }

    fun add(v: Vector3d): Vector3d {
        x += v.x
        y += v.y
        z += v.z
        return this
    }

    fun add(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        return dest
    }

    fun add(v: Vector3f): Vector3d {
        x += v.x.toDouble()
        y += v.y.toDouble()
        z += v.z.toDouble()
        return this
    }

    fun add(v: Vector3f, dest: Vector3d): Vector3d {
        dest.x = x + v.x.toDouble()
        dest.y = y + v.y.toDouble()
        dest.z = z + v.z.toDouble()
        return dest
    }

    fun add(x: Double, y: Double, z: Double): Vector3d {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun add(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        return dest
    }

    fun fma(a: Vector3d, b: Vector3d): Vector3d {
        x = Math.fma(a.x, b.x, x)
        y = Math.fma(a.y, b.y, y)
        z = Math.fma(a.z, b.z, z)
        return this
    }

    fun fma(a: Double, b: Vector3d): Vector3d {
        x = Math.fma(a, b.x, x)
        y = Math.fma(a, b.y, y)
        z = Math.fma(a, b.z, z)
        return this
    }

    fun fma(a: Vector3f, b: Vector3f): Vector3d {
        x = Math.fma(a.x.toDouble(), b.x.toDouble(), x)
        y = Math.fma(a.y.toDouble(), b.y.toDouble(), y)
        z = Math.fma(a.z.toDouble(), b.z.toDouble(), z)
        return this
    }

    fun fma(a: Vector3f, b: Vector3f, dest: Vector3d): Vector3d {
        dest.x = Math.fma(a.x.toDouble(), b.x.toDouble(), x)
        dest.y = Math.fma(a.y.toDouble(), b.y.toDouble(), y)
        dest.z = Math.fma(a.z.toDouble(), b.z.toDouble(), z)
        return dest
    }

    fun fma(a: Double, b: Vector3f): Vector3d {
        x = Math.fma(a, b.x.toDouble(), x)
        y = Math.fma(a, b.y.toDouble(), y)
        z = Math.fma(a, b.z.toDouble(), z)
        return this
    }

    fun fma(a: Vector3d, b: Vector3d, dest: Vector3d): Vector3d {
        dest.x = Math.fma(a.x, b.x, x)
        dest.y = Math.fma(a.y, b.y, y)
        dest.z = Math.fma(a.z, b.z, z)
        return dest
    }

    fun fma(a: Double, b: Vector3d, dest: Vector3d): Vector3d {
        dest.x = Math.fma(a, b.x, x)
        dest.y = Math.fma(a, b.y, y)
        dest.z = Math.fma(a, b.z, z)
        return dest
    }

    fun fma(a: Vector3d, b: Vector3f, dest: Vector3d): Vector3d {
        dest.x = Math.fma(a.x, b.x.toDouble(), x)
        dest.y = Math.fma(a.y, b.y.toDouble(), y)
        dest.z = Math.fma(a.z, b.z.toDouble(), z)
        return dest
    }

    fun fma(a: Double, b: Vector3f, dest: Vector3d): Vector3d {
        dest.x = Math.fma(a, b.x.toDouble(), x)
        dest.y = Math.fma(a, b.y.toDouble(), y)
        dest.z = Math.fma(a, b.z.toDouble(), z)
        return dest
    }

    fun mulAdd(a: Vector3d, b: Vector3d): Vector3d {
        x = Math.fma(x, a.x, b.x)
        y = Math.fma(y, a.y, b.y)
        z = Math.fma(z, a.z, b.z)
        return this
    }

    fun mulAdd(a: Double, b: Vector3d): Vector3d {
        x = Math.fma(x, a, b.x)
        y = Math.fma(y, a, b.y)
        z = Math.fma(z, a, b.z)
        return this
    }

    fun mulAdd(a: Vector3d, b: Vector3d, dest: Vector3d): Vector3d {
        dest.x = Math.fma(x, a.x, b.x)
        dest.y = Math.fma(y, a.y, b.y)
        dest.z = Math.fma(z, a.z, b.z)
        return dest
    }

    fun mulAdd(a: Double, b: Vector3d, dest: Vector3d): Vector3d {
        dest.x = Math.fma(x, a, b.x)
        dest.y = Math.fma(y, a, b.y)
        dest.z = Math.fma(z, a, b.z)
        return dest
    }

    fun mulAdd(a: Vector3f, b: Vector3d, dest: Vector3d): Vector3d {
        dest.x = Math.fma(x, a.x.toDouble(), b.x)
        dest.y = Math.fma(y, a.y.toDouble(), b.y)
        dest.z = Math.fma(z, a.z.toDouble(), b.z)
        return dest
    }

    fun mul(v: Vector3d): Vector3d {
        x *= v.x
        y *= v.y
        z *= v.z
        return this
    }

    fun mul(v: Vector3f): Vector3d {
        x *= v.x.toDouble()
        y *= v.y.toDouble()
        z *= v.z.toDouble()
        return this
    }

    fun mul(v: Vector3f, dest: Vector3d): Vector3d {
        dest.x = x * v.x.toDouble()
        dest.y = y * v.y.toDouble()
        dest.z = z * v.z.toDouble()
        return dest
    }

    fun mul(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        return dest
    }

    operator fun div(v: Vector3d): Vector3d {
        x /= v.x
        y /= v.y
        z /= v.z
        return this
    }

    operator fun div(v: Vector3f): Vector3d {
        x /= v.x.toDouble()
        y /= v.y.toDouble()
        z /= v.z.toDouble()
        return this
    }

    fun div(v: Vector3f, dest: Vector3d): Vector3d {
        dest.x = x / v.x.toDouble()
        dest.y = y / v.y.toDouble()
        dest.z = z / v.z.toDouble()
        return dest
    }

    fun div(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = x / v.x
        dest.y = y / v.y
        dest.z = z / v.z
        return dest
    }

    fun mulProject(mat: Matrix4d, w: Double, dest: Vector3d): Vector3d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulProject(mat: Matrix4d, dest: Vector3d): Vector3d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulProject(mat: Matrix4d): Vector3d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulProject(mat: Matrix4f, dest: Vector3d): Vector3d {
        val invW = 1.0 / Math.fma(
            mat.m03.toDouble(),
            x,
            Math.fma(mat.m13.toDouble(), y, Math.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = (mat.m00.toDouble() * x + mat.m10.toDouble() * y + mat.m20.toDouble() * z + mat.m30.toDouble()) * invW
        val ry = (mat.m01.toDouble() * x + mat.m11.toDouble() * y + mat.m21.toDouble() * z + mat.m31.toDouble()) * invW
        val rz = (mat.m02.toDouble() * x + mat.m12.toDouble() * y + mat.m22.toDouble() * z + mat.m32.toDouble()) * invW
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulProject(mat: Matrix4f): Vector3d {
        val invW = 1.0 / Math.fma(
            mat.m03.toDouble(),
            x,
            Math.fma(mat.m13.toDouble(), y, Math.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = (mat.m00.toDouble() * x + mat.m10.toDouble() * y + mat.m20.toDouble() * z + mat.m30.toDouble()) * invW
        val ry = (mat.m01.toDouble() * x + mat.m11.toDouble() * y + mat.m21.toDouble() * z + mat.m31.toDouble()) * invW
        val rz = (mat.m02.toDouble() * x + mat.m12.toDouble() * y + mat.m22.toDouble() * z + mat.m32.toDouble()) * invW
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix3f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix3d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mul(mat: Matrix3d, dest: Vector3f): Vector3f {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        dest.x = rx.toFloat()
        dest.y = ry.toFloat()
        dest.z = rz.toFloat()
        return dest
    }

    fun mul(mat: Matrix3f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mul(mat: Matrix3x2d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        x = rx
        y = ry
        return this
    }

    fun mul(mat: Matrix3x2d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        dest.x = rx
        dest.y = ry
        dest.z = z
        return dest
    }

    fun mul(mat: Matrix3x2f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        x = rx
        y = ry
        return this
    }

    fun mul(mat: Matrix3x2f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = z
        return dest
    }

    fun mulTranspose(mat: Matrix3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTranspose(mat: Matrix3d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulTranspose(mat: Matrix3f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = Math.fma(mat.m10.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = Math.fma(mat.m20.toDouble(), x, Math.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTranspose(mat: Matrix3f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = Math.fma(mat.m10.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = Math.fma(mat.m20.toDouble(), x, Math.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulPosition(mat: Matrix4f): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulPosition(mat: Matrix4d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulPosition(mat: Matrix4x3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulPosition(mat: Matrix4x3f): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulPosition(mat: Matrix4d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulPosition(mat: Matrix4f, dest: Vector3d): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulPosition(mat: Matrix4x3d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulPosition(mat: Matrix4x3f, dest: Vector3d): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulTransposePosition(mat: Matrix4d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTransposePosition(mat: Matrix4d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulTransposePosition(mat: Matrix4f): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m01.toDouble(), y, Math.fma(mat.m02.toDouble(), z, mat.m03.toDouble()))
        )
        val ry = Math.fma(
            mat.m10.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m12.toDouble(), z, mat.m13.toDouble()))
        )
        val rz = Math.fma(
            mat.m20.toDouble(),
            x,
            Math.fma(mat.m21.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m23.toDouble()))
        )
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTransposePosition(mat: Matrix4f, dest: Vector3d): Vector3d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m01.toDouble(), y, Math.fma(mat.m02.toDouble(), z, mat.m03.toDouble()))
        )
        val ry = Math.fma(
            mat.m10.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m12.toDouble(), z, mat.m13.toDouble()))
        )
        val rz = Math.fma(
            mat.m20.toDouble(),
            x,
            Math.fma(mat.m21.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m23.toDouble()))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulPositionW(mat: Matrix4f): Double {
        val w = Math.fma(
            mat.m03.toDouble(),
            x,
            Math.fma(mat.m13.toDouble(), y, Math.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        x = rx
        y = ry
        z = rz
        return w
    }

    fun mulPositionW(mat: Matrix4f, dest: Vector3d): Double {
        val w = Math.fma(
            mat.m03.toDouble(),
            x,
            Math.fma(mat.m13.toDouble(), y, Math.fma(mat.m23.toDouble(), z, mat.m33.toDouble()))
        )
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble()))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble()))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble()))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return w
    }

    fun mulPositionW(mat: Matrix4d): Double {
        val w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        x = rx
        y = ry
        z = rz
        return w
    }

    fun mulPositionW(mat: Matrix4d, dest: Vector3d): Double {
        val w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return w
    }

    fun mulDirection(mat: Matrix4f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulDirection(mat: Matrix4d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulDirection(mat: Matrix4x3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulDirection(mat: Matrix4x3f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulDirection(mat: Matrix4d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulDirection(mat: Matrix4f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulDirection(mat: Matrix4x3d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulDirection(mat: Matrix4x3f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m10.toDouble(), y, mat.m20.toDouble() * z))
        val ry = Math.fma(mat.m01.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m21.toDouble() * z))
        val rz = Math.fma(mat.m02.toDouble(), x, Math.fma(mat.m12.toDouble(), y, mat.m22.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulTransposeDirection(mat: Matrix4d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTransposeDirection(mat: Matrix4d, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        val ry = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        val rz = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mulTransposeDirection(mat: Matrix4f): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = Math.fma(mat.m10.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = Math.fma(mat.m20.toDouble(), x, Math.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mulTransposeDirection(mat: Matrix4f, dest: Vector3d): Vector3d {
        val rx = Math.fma(mat.m00.toDouble(), x, Math.fma(mat.m01.toDouble(), y, mat.m02.toDouble() * z))
        val ry = Math.fma(mat.m10.toDouble(), x, Math.fma(mat.m11.toDouble(), y, mat.m12.toDouble() * z))
        val rz = Math.fma(mat.m20.toDouble(), x, Math.fma(mat.m21.toDouble(), y, mat.m22.toDouble() * z))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mul(scalar: Double): Vector3d {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun mul(scalar: Double, dest: Vector3d): Vector3d {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun mul(x: Double, y: Double, z: Double): Vector3d {
        this.x *= x
        this.y *= y
        this.z *= z
        return this
    }

    fun mul(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        dest.x = this.x * x
        dest.y = this.y * y
        dest.z = this.z * z
        return dest
    }

    fun rotate(quat: Quaterniond): Vector3d {
        return quat.transform(this, this)
    }

    fun rotate(quat: Quaterniond, dest: Vector3d?): Vector3d {
        return quat.transform(this, dest!!)
    }

    fun rotationTo(toDir: Vector3d?, dest: Quaterniond): Quaterniond {
        return dest.rotationTo(this, toDir!!)
    }

    fun rotationTo(toDirX: Double, toDirY: Double, toDirZ: Double, dest: Quaterniond): Quaterniond {
        return dest.rotationTo(x, y, z, toDirX, toDirY, toDirZ)
    }

    fun rotateAxis(angle: Double, x: Double, y: Double, z: Double): Vector3d {
        return if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            this.rotateX(x * angle, this)
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            this.rotateY(y * angle, this)
        } else {
            if (x == 0.0 && y == 0.0 && Math.absEqualsOne(z)) this.rotateZ(
                z * angle,
                this
            ) else rotateAxisInternal(angle, x, y, z, this)
        }
    }

    fun rotateAxis(angle: Double, aX: Double, aY: Double, aZ: Double, dest: Vector3d): Vector3d {
        return if (aY == 0.0 && aZ == 0.0 && Math.absEqualsOne(aX)) {
            this.rotateX(aX * angle, dest)
        } else if (aX == 0.0 && aZ == 0.0 && Math.absEqualsOne(aY)) {
            this.rotateY(aY * angle, dest)
        } else {
            if (aX == 0.0 && aY == 0.0 && Math.absEqualsOne(aZ)) this.rotateZ(
                aZ * angle,
                dest
            ) else rotateAxisInternal(angle, aX, aY, aZ, dest)
        }
    }

    private fun rotateAxisInternal(angle: Double, aX: Double, aY: Double, aZ: Double, dest: Vector3d): Vector3d {
        val halfAngle = angle * 0.5
        val sinAngle = Math.sin(halfAngle)
        val qx = aX * sinAngle
        val qy = aY * sinAngle
        val qz = aZ * sinAngle
        val qw = Math.cosFromSin(sinAngle, halfAngle)
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
        dest.x = nx
        dest.y = ny
        dest.z = nz
        return dest
    }

    fun rotateX(angle: Double): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        this.y = y
        this.z = z
        return this
    }

    fun rotateX(angle: Double, dest: Vector3d): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun rotateY(angle: Double): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        this.x = x
        this.z = z
        return this
    }

    fun rotateY(angle: Double, dest: Vector3d): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun rotateZ(angle: Double): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        this.x = x
        this.y = y
        return this
    }

    fun rotateZ(angle: Double, dest: Vector3d): Vector3d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    operator fun div(scalar: Double): Vector3d {
        val inv = 1.0 / scalar
        x *= inv
        y *= inv
        z *= inv
        return this
    }

    fun div(scalar: Double, dest: Vector3d): Vector3d {
        val inv = 1.0 / scalar
        dest.x = x * inv
        dest.y = y * inv
        dest.z = z * inv
        return dest
    }

    fun div(x: Double, y: Double, z: Double): Vector3d {
        this.x /= x
        this.y /= y
        this.z /= z
        return this
    }

    fun div(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        dest.x = this.x / x
        dest.y = this.y / y
        dest.z = this.z / z
        return dest
    }

    fun lengthSquared(): Double {
        return Math.fma(x, x, Math.fma(y, y, z * z))
    }

    fun length(): Double {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
    }

    fun normalize(): Vector3d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        x *= invLength
        y *= invLength
        z *= invLength
        return this
    }

    fun normalize(dest: Vector3d): Vector3d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        return dest
    }

    fun normalize(length: Double): Vector3d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        x *= invLength
        y *= invLength
        z *= invLength
        return this
    }

    fun normalize(length: Double, dest: Vector3d): Vector3d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        return dest
    }

    fun cross(v: Vector3d): Vector3d {
        val rx = Math.fma(y, v.z, -z * v.y)
        val ry = Math.fma(z, v.x, -x * v.z)
        val rz = Math.fma(x, v.y, -y * v.x)
        x = rx
        y = ry
        z = rz
        return this
    }

    fun cross(x: Double, y: Double, z: Double): Vector3d {
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun cross(v: Vector3d, dest: Vector3d): Vector3d {
        val rx = Math.fma(y, v.z, -z * v.y)
        val ry = Math.fma(z, v.x, -x * v.z)
        val rz = Math.fma(x, v.y, -y * v.x)
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun cross(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun distance(v: Vector3d): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)))
    }

    fun distance(x: Double, y: Double, z: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)))
    }

    fun distanceSquared(v: Vector3d): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
    }

    fun distanceSquared(x: Double, y: Double, z: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
    }

    fun dot(v: Vector3d): Double {
        return Math.fma(x, v.x, Math.fma(y, v.y, z * v.z))
    }

    fun dot(x: Double, y: Double, z: Double): Double {
        return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
    }

    fun angleCos(v: Vector3d): Double {
        val length1Squared = Math.fma(x, x, Math.fma(y, y, z * z))
        val length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, v.z * v.z))
        val dot = Math.fma(x, v.x, Math.fma(y, v.y, z * v.z))
        return dot / Math.sqrt(length1Squared * length2Squared)
    }

    fun angle(v: Vector3d): Double {
        var cos = angleCos(v)
        cos = java.lang.Math.min(cos, 1.0)
        cos = java.lang.Math.max(cos, -1.0)
        return Math.acos(cos)
    }

    fun angleSigned(v: Vector3d, n: Vector3d): Double {
        val x = v.x
        val y = v.y
        val z = v.z
        return Math.atan2(
            (this.y * z - this.z * y) * n.x + (this.z * x - this.x * z) * n.y + (this.x * y - this.y * x) * n.z,
            this.x * x + this.y * y + this.z * z
        )
    }

    fun angleSigned(x: Double, y: Double, z: Double, nx: Double, ny: Double, nz: Double): Double {
        return Math.atan2(
            (this.y * z - this.z * y) * nx + (this.z * x - this.x * z) * ny + (this.x * y - this.y * x) * nz,
            this.x * x + this.y * y + this.z * z
        )
    }

    fun min(v: Vector3d): Vector3d {
        x = java.lang.Math.min(x, v.x)
        y = java.lang.Math.min(y, v.y)
        z = java.lang.Math.min(z, v.z)
        return this
    }

    fun min(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        return dest
    }

    fun max(v: Vector3d): Vector3d {
        x = java.lang.Math.max(x, v.x)
        y = java.lang.Math.max(y, v.y)
        z = java.lang.Math.max(z, v.z)
        return this
    }

    fun max(v: Vector3d, dest: Vector3d): Vector3d {
        dest.x = java.lang.Math.max(x, v.x)
        dest.y = java.lang.Math.max(y, v.y)
        dest.z = java.lang.Math.max(z, v.z)
        return dest
    }

    fun zero(): Vector3d {
        x = 0.0
        y = 0.0
        z = 0.0
        return this
    }

    override fun toString(): String {
        return "($x,$y,$z)"
    }

    fun negate(): Vector3d {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun negate(dest: Vector3d): Vector3d {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        return dest
    }

    fun absolute(): Vector3d {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        return this
    }

    fun absolute(dest: Vector3d): Vector3d {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        return dest
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = java.lang.Double.doubleToLongBits(x)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(y)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(z)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
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
            val other = obj as Vector3d
            if (java.lang.Double.doubleToLongBits(x) != java.lang.Double.doubleToLongBits(other.x)) {
                false
            } else if (java.lang.Double.doubleToLongBits(y) != java.lang.Double.doubleToLongBits(other.y)) {
                false
            } else {
                java.lang.Double.doubleToLongBits(z) == java.lang.Double.doubleToLongBits(other.z)
            }
        }
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
        return if (java.lang.Double.doubleToLongBits(this.x) != java.lang.Double.doubleToLongBits(x)) {
            false
        } else if (java.lang.Double.doubleToLongBits(this.y) != java.lang.Double.doubleToLongBits(y)) {
            false
        } else {
            java.lang.Double.doubleToLongBits(this.z) == java.lang.Double.doubleToLongBits(z)
        }
    }

    fun reflect(normal: Vector3d): Vector3d {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(x: Double, y: Double, z: Double): Vector3d {
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(normal: Vector3d, dest: Vector3d): Vector3d {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        dest.x = this.x - (dot + dot) * x
        dest.y = this.y - (dot + dot) * y
        dest.z = this.z - (dot + dot) * z
        return dest
    }

    fun reflect(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        dest.x = this.x - (dot + dot) * x
        dest.y = this.y - (dot + dot) * y
        dest.z = this.z - (dot + dot) * z
        return dest
    }

    fun half(other: Vector3d): Vector3d {
        return this.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(x: Double, y: Double, z: Double): Vector3d {
        return this.set(this).add(x, y, z).normalize()
    }

    fun half(other: Vector3d, dest: Vector3d): Vector3d {
        return dest.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        return dest.set(this).add(x, y, z).normalize()
    }

    fun smoothStep(v: Vector3d, t: Double, dest: Vector3d): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * x) * t2 + x * t + x
        dest.y = (y + y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * y) * t2 + y * t + y
        dest.z = (z + z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * z) * t2 + z * t + z
        return dest
    }

    fun hermite(t0: Vector3d, v1: Vector3d, t1: Vector3d, t: Double, dest: Vector3d): Vector3d {
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        return dest
    }

    fun lerp(other: Vector3d, t: Double): Vector3d {
        x = Math.fma(other.x - x, t, x)
        y = Math.fma(other.y - y, t, y)
        z = Math.fma(other.z - z, t, z)
        return this
    }

    fun lerp(other: Vector3d, t: Double, dest: Vector3d): Vector3d {
        dest.x = Math.fma(other.x - x, t, x)
        dest.y = Math.fma(other.y - y, t, y)
        dest.z = Math.fma(other.z - z, t, z)
        return dest
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IllegalArgumentException()
        }
    }

    operator fun get(mode: Int, dest: Vector3i): Vector3i {
        dest.x = Math.roundUsing(x, mode)
        dest.y = Math.roundUsing(y, mode)
        dest.z = Math.roundUsing(z, mode)
        return dest
    }

    operator fun get(dest: Vector3f): Vector3f {
        dest.x = x.toFloat()
        dest.y = y.toFloat()
        dest.z = z.toFloat()
        return dest
    }

    operator fun get(dest: Vector3d): Vector3d {
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun maxComponent(): Int {
        val absX = Math.abs(x)
        val absY = Math.abs(y)
        val absZ = Math.abs(z)
        return if (absX >= absY && absX >= absZ) {
            0
        } else {
            if (absY >= absZ) 1 else 2
        }
    }

    fun minComponent(): Int {
        val absX = Math.abs(x)
        val absY = Math.abs(y)
        val absZ = Math.abs(z)
        return if (absX < absY && absX < absZ) {
            0
        } else {
            if (absY < absZ) 1 else 2
        }
    }

    @JvmOverloads
    fun orthogonalize(v: Vector3d, dest: Vector3d = this): Vector3d {
        val rx: Double
        val ry: Double
        val rz: Double
        if (Math.abs(v.x) > Math.abs(v.z)) {
            rx = -v.y
            ry = v.x
            rz = 0.0
        } else {
            rx = 0.0
            ry = -v.z
            rz = v.y
        }
        val invLen = Math.invsqrt(rx * rx + ry * ry + rz * rz)
        dest.x = rx * invLen
        dest.y = ry * invLen
        dest.z = rz * invLen
        return dest
    }

    @JvmOverloads
    fun orthogonalizeUnit(v: Vector3d, dest: Vector3d = this): Vector3d {
        return orthogonalize(v, dest)
    }

    fun floor(): Vector3d {
        x = Math.floor(x)
        y = Math.floor(y)
        z = Math.floor(z)
        return this
    }

    fun floor(dest: Vector3d): Vector3d {
        dest.x = Math.floor(x)
        dest.y = Math.floor(y)
        dest.z = Math.floor(z)
        return dest
    }

    fun ceil(): Vector3d {
        x = Math.ceil(x)
        y = Math.ceil(y)
        z = Math.ceil(z)
        return this
    }

    fun ceil(dest: Vector3d): Vector3d {
        dest.x = Math.ceil(x)
        dest.y = Math.ceil(y)
        dest.z = Math.ceil(z)
        return dest
    }

    fun round(): Vector3d {
        x = Math.round(x).toDouble()
        y = Math.round(y).toDouble()
        z = Math.round(z).toDouble()
        return this
    }

    fun round(dest: Vector3d): Vector3d {
        dest.x = Math.round(x).toDouble()
        dest.y = Math.round(y).toDouble()
        dest.z = Math.round(z).toDouble()
        return dest
    }

    val isFinite: Boolean
        get() = Math.isFinite(x) && Math.isFinite(y) && Math.isFinite(z)

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        fun lengthSquared(x: Double, y: Double, z: Double): Double {
            return Math.fma(x, x, Math.fma(y, y, z * z))
        }

        fun length(x: Double, y: Double, z: Double): Double {
            return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        }

        fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2))
        }

        fun distanceSquared(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
        }
    }
}