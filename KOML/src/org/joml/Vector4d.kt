package org.joml

import kotlin.math.*

@Suppress("unused")
open class Vector4d(var x: Double, var y: Double, var z: Double, var w: Double) {

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
    constructor(xyzw: FloatArray) : this(xyzw[0].toDouble(), xyzw[1].toDouble(), xyzw[2].toDouble(), xyzw[3].toDouble())
    constructor(xyzw: DoubleArray) : this(xyzw[0], xyzw[1], xyzw[2], xyzw[3])

    fun set(v: Vector4d): Vector4d {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
        return this
    }

    fun set(v: Vector4f): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        w = v.w.toDouble()
        return this
    }

    fun set(v: Vector4i): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        w = v.w.toDouble()
        return this
    }

    fun set(v: Vector3d, w: Double): Vector4d {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
        return this
    }

    fun set(v: Vector3i, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
        return this
    }

    fun set(v: Vector3f, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
        return this
    }

    fun set(v: Vector2d, z: Double, w: Double): Vector4d {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
        return this
    }

    fun set(v: Vector2i, z: Double, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        this.w = w
        return this
    }

    fun set(d: Double): Vector4d {
        x = d
        y = d
        z = d
        w = d
        return this
    }

    fun set(v: Vector2f, z: Double, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        this.w = w
        return this
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

    fun set(xyzw: DoubleArray): Vector4d {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
        return this
    }

    fun set(xyzw: FloatArray): Vector4d {
        x = xyzw[0].toDouble()
        y = xyzw[1].toDouble()
        z = xyzw[2].toDouble()
        w = xyzw[3].toDouble()
        return this
    }

    operator fun set(component: Int, value: Double) = setComponent(component, value)
    fun setComponent(component: Int, value: Double): Vector4d {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector4d): Vector4d {
        x -= v.x
        y -= v.y
        z -= v.z
        w -= v.w
        return this
    }

    fun sub(v: Vector4d, dst: Vector4d): Vector4d {
        dst.x = x - v.x
        dst.y = y - v.y
        dst.z = z - v.z
        dst.w = w - v.w
        return dst
    }

    fun sub(v: Vector4f): Vector4d {
        x -= v.x.toDouble()
        y -= v.y.toDouble()
        z -= v.z.toDouble()
        w -= v.w.toDouble()
        return this
    }

    fun sub(v: Vector4f, dst: Vector4d): Vector4d {
        dst.x = x - v.x.toDouble()
        dst.y = y - v.y.toDouble()
        dst.z = z - v.z.toDouble()
        dst.w = w - v.w.toDouble()
        return dst
    }

    fun sub(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x -= x
        this.y -= y
        this.z -= z
        this.w -= w
        return this
    }

    fun sub(x: Double, y: Double, z: Double, w: Double, dst: Vector4d): Vector4d {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    fun add(v: Vector4d): Vector4d {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
        return this
    }

    fun add(v: Vector4d, dst: Vector4d): Vector4d {
        dst.x = x + v.x
        dst.y = y + v.y
        dst.z = z + v.z
        dst.w = w + v.w
        return dst
    }

    fun add(v: Vector4f, dst: Vector4d): Vector4d {
        dst.x = x + v.x.toDouble()
        dst.y = y + v.y.toDouble()
        dst.z = z + v.z.toDouble()
        dst.w = w + v.w.toDouble()
        return dst
    }

    fun add(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x += x
        this.y += y
        this.z += z
        this.w += w
        return this
    }

    fun add(x: Double, y: Double, z: Double, w: Double, dst: Vector4d): Vector4d {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    fun add(v: Vector4f): Vector4d {
        x += v.x.toDouble()
        y += v.y.toDouble()
        z += v.z.toDouble()
        w += v.w.toDouble()
        return this
    }

    fun fma(a: Vector4d, b: Vector4d): Vector4d {
        x = JomlMath.fma(a.x, b.x, x)
        y = JomlMath.fma(a.y, b.y, y)
        z = JomlMath.fma(a.z, b.z, z)
        w = JomlMath.fma(a.w, b.w, w)
        return this
    }

    fun fma(a: Double, b: Vector4d): Vector4d {
        x = JomlMath.fma(a, b.x, x)
        y = JomlMath.fma(a, b.y, y)
        z = JomlMath.fma(a, b.z, z)
        w = JomlMath.fma(a, b.w, w)
        return this
    }

    fun fma(a: Vector4d, b: Vector4d, dst: Vector4d): Vector4d {
        dst.x = JomlMath.fma(a.x, b.x, x)
        dst.y = JomlMath.fma(a.y, b.y, y)
        dst.z = JomlMath.fma(a.z, b.z, z)
        dst.w = JomlMath.fma(a.w, b.w, w)
        return dst
    }

    fun fma(a: Double, b: Vector4d, dst: Vector4d): Vector4d {
        dst.x = JomlMath.fma(a, b.x, x)
        dst.y = JomlMath.fma(a, b.y, y)
        dst.z = JomlMath.fma(a, b.z, z)
        dst.w = JomlMath.fma(a, b.w, w)
        return dst
    }

    fun mulAdd(a: Vector4d, b: Vector4d): Vector4d {
        x = JomlMath.fma(x, a.x, b.x)
        y = JomlMath.fma(y, a.y, b.y)
        z = JomlMath.fma(z, a.z, b.z)
        return this
    }

    fun mulAdd(a: Double, b: Vector4d): Vector4d {
        x = JomlMath.fma(x, a, b.x)
        y = JomlMath.fma(y, a, b.y)
        z = JomlMath.fma(z, a, b.z)
        return this
    }

    fun mulAdd(a: Vector4d, b: Vector4d, dst: Vector4d): Vector4d {
        dst.x = JomlMath.fma(x, a.x, b.x)
        dst.y = JomlMath.fma(y, a.y, b.y)
        dst.z = JomlMath.fma(z, a.z, b.z)
        return dst
    }

    fun mulAdd(a: Double, b: Vector4d, dst: Vector4d): Vector4d {
        dst.x = JomlMath.fma(x, a, b.x)
        dst.y = JomlMath.fma(y, a, b.y)
        dst.z = JomlMath.fma(z, a, b.z)
        return dst
    }

    fun mul(v: Vector4d): Vector4d {
        x *= v.x
        y *= v.y
        z *= v.z
        w *= v.w
        return this
    }

    fun mul(v: Vector4d, dst: Vector4d): Vector4d {
        dst.x = x * v.x
        dst.y = y * v.y
        dst.z = z * v.z
        dst.w = w * v.w
        return dst
    }

    fun div(v: Vector4d): Vector4d {
        x /= v.x
        y /= v.y
        z /= v.z
        w /= v.w
        return this
    }

    fun div(v: Vector4d, dst: Vector4d): Vector4d {
        dst.x = x / v.x
        dst.y = y / v.y
        dst.z = z / v.z
        dst.w = w / v.w
        return dst
    }

    fun mul(v: Vector4f): Vector4d {
        x *= v.x.toDouble()
        y *= v.y.toDouble()
        z *= v.z.toDouble()
        w *= v.w.toDouble()
        return this
    }

    fun mul(v: Vector4f, dst: Vector4d): Vector4d {
        dst.x = x * v.x.toDouble()
        dst.y = y * v.y.toDouble()
        dst.z = z * v.z.toDouble()
        dst.w = w * v.w.toDouble()
        return dst
    }

    fun mul(mat: Matrix4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, this) else this.mulGeneric(mat, this)
    }

    fun mul(mat: Matrix4d, dst: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dst) else this.mulGeneric(mat, dst)
    }

    fun mulTranspose(mat: Matrix4d): Vector4d {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, this) else mulGenericTranspose(mat, this)
    }

    fun mulTranspose(mat: Matrix4d, dst: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dst) else mulGenericTranspose(mat, dst)
    }

    fun mulAffine(mat: Matrix4d, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4d, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w)))
        val rw = JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
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
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, mat.m02 * z))
        dst.y = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, mat.m12 * z))
        dst.z = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, mat.m22 * z))
        dst.w = JomlMath.fma(mat.m30, x, JomlMath.fma(mat.m31, y, mat.m32 * z + w))
        return dst
    }

    private fun mulGenericTranspose(mat: Matrix4d, dst: Vector4d): Vector4d {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m01, y, JomlMath.fma(mat.m02, z, mat.m03 * w)))
        dst.y = JomlMath.fma(mat.m10, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m12, z, mat.m13 * w)))
        dst.z = JomlMath.fma(mat.m20, x, JomlMath.fma(mat.m21, y, JomlMath.fma(mat.m22, z, mat.m23 * w)))
        dst.w = JomlMath.fma(mat.m30, x, JomlMath.fma(mat.m31, y, JomlMath.fma(mat.m32, z, mat.m33 * w)))
        return dst
    }

    fun mul(mat: Matrix4x3d): Vector4d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w)))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix4x3d, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w)))
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w)))
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w)))
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    fun mul(mat: Matrix4x3f): Vector4d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix4x3f, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    fun mul(mat: Matrix4f): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, this) else this.mulGeneric(mat, this)
    }

    fun mul(mat: Matrix4f, dst: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dst) else this.mulGeneric(mat, dst)
    }

    private fun mulAffine(mat: Matrix4f, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4f, dst: Vector4d): Vector4d {
        val rx = JomlMath.fma(
            mat.m00.toDouble(),
            x,
            JomlMath.fma(mat.m10.toDouble(), y, JomlMath.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = JomlMath.fma(
            mat.m01.toDouble(),
            x,
            JomlMath.fma(mat.m11.toDouble(), y, JomlMath.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = JomlMath.fma(
            mat.m02.toDouble(),
            x,
            JomlMath.fma(mat.m12.toDouble(), y, JomlMath.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        val rw = JomlMath.fma(
            mat.m03.toDouble(),
            x,
            JomlMath.fma(mat.m13.toDouble(), y, JomlMath.fma(mat.m23.toDouble(), z, mat.m33.toDouble() * w))
        )
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = rw
        return dst
    }

    fun mulProject(mat: Matrix4d, dst: Vector4d): Vector4d {
        val invW = 1.0 / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        dst.w = 1.0
        return dst
    }

    fun mulProject(mat: Matrix4d): Vector4d {
        val invW = 1.0 / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w))) * invW
        x = rx
        y = ry
        z = rz
        w = 1.0
        return this
    }

    fun mulProject(mat: Matrix4d, dst: Vector3d): Vector3d {
        val invW = 1.0 / JomlMath.fma(mat.m03, x, JomlMath.fma(mat.m13, y, JomlMath.fma(mat.m23, z, mat.m33 * w)))
        val rx = JomlMath.fma(mat.m00, x, JomlMath.fma(mat.m10, y, JomlMath.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = JomlMath.fma(mat.m01, x, JomlMath.fma(mat.m11, y, JomlMath.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = JomlMath.fma(mat.m02, x, JomlMath.fma(mat.m12, y, JomlMath.fma(mat.m22, z, mat.m32 * w))) * invW
        dst.x = rx
        dst.y = ry
        dst.z = rz
        return dst
    }

    fun mul(scalar: Double): Vector4d {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        return this
    }

    fun mul(scalar: Double, dst: Vector4d): Vector4d {
        dst.x = x * scalar
        dst.y = y * scalar
        dst.z = z * scalar
        dst.w = w * scalar
        return dst
    }

    fun div(scalar: Double, dst: Vector4d = this) = mul(1.0 / scalar, dst)

    fun rotate(quat: Quaterniond, dst: Vector4d = this): Vector4d {
        quat.transform(this, dst)
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

    fun lengthSquared() = x * x + y * y + z * z + w * w
    fun length() = sqrt(lengthSquared())

    fun normalize(dst: Vector4d = this) = div(length(), dst)

    fun normalize(length: Double, dst: Vector4d = this) = mul(length / length(), dst)

    fun normalize3(dst: Vector4d = this) = div(sqrt(x * x + y * y + z * z), dst)

    fun distance(v: Vector4d): Double {
        return Companion.length(x - v.x, y - v.y, z - v.w, w - v.w)
    }

    fun distance(x: Double, y: Double, z: Double, w: Double): Double {
        return Companion.length(this.x - x, this.y - y, this.z - z, this.w - w)
    }

    fun distanceSquared(v: Vector4d): Double {
        return Companion.lengthSquared(x - v.x, y - v.y, z - v.w, w - v.w)
    }

    fun distanceSquared(x: Double, y: Double, z: Double, w: Double): Double {
        return Companion.lengthSquared(this.x - x, this.y - y, this.z - z, this.w - w)
    }

    fun dot(v: Vector4d) = x * v.x + y * v.y + z * v.z + w * v.w
    fun dot(x: Double, y: Double, z: Double, w: Double): Double {
        return this.x * x + this.y * y + this.z * z + this.w * w
    }

    fun angleCos(v: Vector4d): Double {
        val length1Squared = JomlMath.fma(x, x, JomlMath.fma(y, y, JomlMath.fma(z, z, w * w)))
        val length2Squared = JomlMath.fma(v.x, v.x, JomlMath.fma(v.y, v.y, JomlMath.fma(v.z, v.z, v.w * v.w)))
        val dot = JomlMath.fma(x, v.x, JomlMath.fma(y, v.y, JomlMath.fma(z, v.z, w * v.w)))
        return dot / sqrt(length1Squared * length2Squared)
    }

    fun angle(v: Vector4d): Double {
        var cos = angleCos(v)
        cos = min(cos, 1.0)
        cos = max(cos, -1.0)
        return acos(cos)
    }

    fun zero(): Vector4d {
        x = 0.0
        y = 0.0
        z = 0.0
        w = 0.0
        return this
    }

    fun negate(dst: Vector4d = this): Vector4d {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = -w
        return dst
    }

    fun min(v: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = min(x, v.x)
        dst.y = min(y, v.y)
        dst.z = min(z, v.z)
        dst.w = min(w, v.w)
        return dst
    }

    fun max(v: Vector4d, dst: Vector4d = this): Vector4d {
        dst.x = max(x, v.x)
        dst.y = max(y, v.y)
        dst.z = max(z, v.z)
        dst.w = max(w, v.w)
        return dst
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = (w).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (x).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (y).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (z).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is Vector4d) false
        else x == other.x && y == other.y && z == other.z && w == other.w
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

    fun lerp(other: Vector4d, t: Double, dst: Vector4d = this): Vector4d {
        dst.x = JomlMath.fma(other.x - x, t, x)
        dst.y = JomlMath.fma(other.y - y, t, y)
        dst.z = JomlMath.fma(other.z - z, t, z)
        dst.w = JomlMath.fma(other.w - w, t, w)
        return dst
    }

    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException()
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

    fun floor(dst: Vector4d = this): Vector4d {
        dst.x = floor(x)
        dst.y = floor(y)
        dst.z = floor(z)
        dst.w = floor(w)
        return dst
    }

    fun ceil(dst: Vector4d = this): Vector4d {
        dst.x = ceil(x)
        dst.y = ceil(y)
        dst.z = ceil(z)
        dst.w = ceil(w)
        return dst
    }

    fun round(dst: Vector4d = this): Vector4d {
        dst.x = round(x)
        dst.y = round(y)
        dst.z = round(z)
        dst.w = round(w)
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    fun absolute(dst: Vector4d = this): Vector4d {
        dst.x = abs(x)
        dst.y = abs(y)
        dst.z = abs(z)
        dst.w = abs(w)
        return dst
    }

    companion object {

        @JvmStatic
        fun lengthSquared(x: Double, y: Double, z: Double, w: Double): Double {
            return x * x + y * y + z * z + w * w
        }

        @JvmStatic
        fun length(x: Double, y: Double, z: Double, w: Double): Double {
            return sqrt(x * x + y * y + z * z + w * w)
        }

        @JvmStatic
        fun distance(
            x1: Double, y1: Double, z1: Double, w1: Double,
            x2: Double, y2: Double, z2: Double, w2: Double
        ): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return length(dx, dy, dz, dw)
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