package org.joml

class Vector4d : Cloneable {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var w: Double

    constructor() {
        w = 1.0
    }

    constructor(v: Vector4d) {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
    }

    constructor(v: Vector4i) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        w = v.w.toDouble()
    }

    constructor(v: Vector3d, w: Double) {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
    }

    constructor(v: Vector3i, w: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
    }

    constructor(v: Vector2d, z: Double, w: Double) {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
    }

    constructor(v: Vector2i, z: Double, w: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        this.w = w
    }

    constructor(d: Double) {
        x = d
        y = d
        z = d
        w = d
    }

    constructor(v: Vector4f) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        w = v.w.toDouble()
    }

    constructor(v: Vector3f, w: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
    }

    constructor(v: Vector2f, z: Double, w: Double) {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        this.w = w
    }

    constructor(x: Double, y: Double, z: Double, w: Double) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(xyzw: FloatArray) {
        x = xyzw[0].toDouble()
        y = xyzw[1].toDouble()
        z = xyzw[2].toDouble()
        w = xyzw[3].toDouble()
    }

    constructor(xyzw: DoubleArray) {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
    }

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

    operator fun set(v: Vector3d, w: Double): Vector4d {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
        return this
    }

    operator fun set(v: Vector3i, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
        return this
    }

    operator fun set(v: Vector3f, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        z = v.z.toDouble()
        this.w = w
        return this
    }

    operator fun set(v: Vector2d, z: Double, w: Double): Vector4d {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(v: Vector2i, z: Double, w: Double): Vector4d {
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

    operator fun set(v: Vector2f, z: Double, w: Double): Vector4d {
        x = v.x.toDouble()
        y = v.y.toDouble()
        this.z = z
        this.w = w
        return this
    }

    operator fun set(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(x: Double, y: Double, z: Double): Vector4d {
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

    @Throws(IllegalArgumentException::class)
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

    fun sub(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        dest.w = w - v.w
        return dest
    }

    fun sub(v: Vector4f): Vector4d {
        x -= v.x.toDouble()
        y -= v.y.toDouble()
        z -= v.z.toDouble()
        w -= v.w.toDouble()
        return this
    }

    fun sub(v: Vector4f, dest: Vector4d): Vector4d {
        dest.x = x - v.x.toDouble()
        dest.y = y - v.y.toDouble()
        dest.z = z - v.z.toDouble()
        dest.w = w - v.w.toDouble()
        return dest
    }

    fun sub(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x -= x
        this.y -= y
        this.z -= z
        this.w -= w
        return this
    }

    fun sub(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        dest.w = this.w - w
        return dest
    }

    fun add(v: Vector4d): Vector4d {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
        return this
    }

    fun add(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        dest.w = w + v.w
        return dest
    }

    fun add(v: Vector4f, dest: Vector4d): Vector4d {
        dest.x = x + v.x.toDouble()
        dest.y = y + v.y.toDouble()
        dest.z = z + v.z.toDouble()
        dest.w = w + v.w.toDouble()
        return dest
    }

    fun add(x: Double, y: Double, z: Double, w: Double): Vector4d {
        this.x += x
        this.y += y
        this.z += z
        this.w += w
        return this
    }

    fun add(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        dest.w = this.w + w
        return dest
    }

    fun add(v: Vector4f): Vector4d {
        x += v.x.toDouble()
        y += v.y.toDouble()
        z += v.z.toDouble()
        w += v.w.toDouble()
        return this
    }

    fun fma(a: Vector4d, b: Vector4d): Vector4d {
        x = Math.fma(a.x, b.x, x)
        y = Math.fma(a.y, b.y, y)
        z = Math.fma(a.z, b.z, z)
        w = Math.fma(a.w, b.w, w)
        return this
    }

    fun fma(a: Double, b: Vector4d): Vector4d {
        x = Math.fma(a, b.x, x)
        y = Math.fma(a, b.y, y)
        z = Math.fma(a, b.z, z)
        w = Math.fma(a, b.w, w)
        return this
    }

    fun fma(a: Vector4d, b: Vector4d, dest: Vector4d): Vector4d {
        dest.x = Math.fma(a.x, b.x, x)
        dest.y = Math.fma(a.y, b.y, y)
        dest.z = Math.fma(a.z, b.z, z)
        dest.w = Math.fma(a.w, b.w, w)
        return dest
    }

    fun fma(a: Double, b: Vector4d, dest: Vector4d): Vector4d {
        dest.x = Math.fma(a, b.x, x)
        dest.y = Math.fma(a, b.y, y)
        dest.z = Math.fma(a, b.z, z)
        dest.w = Math.fma(a, b.w, w)
        return dest
    }

    fun mulAdd(a: Vector4d, b: Vector4d): Vector4d {
        x = Math.fma(x, a.x, b.x)
        y = Math.fma(y, a.y, b.y)
        z = Math.fma(z, a.z, b.z)
        return this
    }

    fun mulAdd(a: Double, b: Vector4d): Vector4d {
        x = Math.fma(x, a, b.x)
        y = Math.fma(y, a, b.y)
        z = Math.fma(z, a, b.z)
        return this
    }

    fun mulAdd(a: Vector4d, b: Vector4d, dest: Vector4d): Vector4d {
        dest.x = Math.fma(x, a.x, b.x)
        dest.y = Math.fma(y, a.y, b.y)
        dest.z = Math.fma(z, a.z, b.z)
        return dest
    }

    fun mulAdd(a: Double, b: Vector4d, dest: Vector4d): Vector4d {
        dest.x = Math.fma(x, a, b.x)
        dest.y = Math.fma(y, a, b.y)
        dest.z = Math.fma(z, a, b.z)
        return dest
    }

    fun mul(v: Vector4d): Vector4d {
        x *= v.x
        y *= v.y
        z *= v.z
        w *= v.w
        return this
    }

    fun mul(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        dest.w = w * v.w
        return dest
    }

    operator fun div(v: Vector4d): Vector4d {
        x /= v.x
        y /= v.y
        z /= v.z
        w /= v.w
        return this
    }

    fun div(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = x / v.x
        dest.y = y / v.y
        dest.z = z / v.z
        dest.w = w / v.w
        return dest
    }

    fun mul(v: Vector4f): Vector4d {
        x *= v.x.toDouble()
        y *= v.y.toDouble()
        z *= v.z.toDouble()
        w *= v.w.toDouble()
        return this
    }

    fun mul(v: Vector4f, dest: Vector4d): Vector4d {
        dest.x = x * v.x.toDouble()
        dest.y = y * v.y.toDouble()
        dest.z = z * v.z.toDouble()
        dest.w = w * v.w.toDouble()
        return dest
    }

    fun mul(mat: Matrix4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, this) else this.mulGeneric(mat, this)
    }

    fun mul(mat: Matrix4d, dest: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dest) else this.mulGeneric(mat, dest)
    }

    fun mulTranspose(mat: Matrix4d): Vector4d {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, this) else mulGenericTranspose(mat, this)
    }

    fun mulTranspose(mat: Matrix4d, dest: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dest) else mulGenericTranspose(mat, dest)
    }

    fun mulAffine(mat: Matrix4d, dest: Vector4d): Vector4d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = w
        return dest
    }

    private fun mulGeneric(mat: Matrix4d, dest: Vector4d): Vector4d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        val rw = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = rw
        return dest
    }

    fun mulAffineTranspose(mat: Matrix4d, dest: Vector4d): Vector4d {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        dest.w = Math.fma(mat.m30, x, Math.fma(mat.m31, y, mat.m32 * z + w))
        return dest
    }

    private fun mulGenericTranspose(mat: Matrix4d, dest: Vector4d): Vector4d {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03 * w)))
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13 * w)))
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23 * w)))
        dest.w = Math.fma(mat.m30, x, Math.fma(mat.m31, y, Math.fma(mat.m32, z, mat.m33 * w)))
        return dest
    }

    fun mul(mat: Matrix4x3d): Vector4d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix4x3d, dest: Vector4d): Vector4d {
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = w
        return dest
    }

    fun mul(mat: Matrix4x3f): Vector4d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        x = rx
        y = ry
        z = rz
        return this
    }

    fun mul(mat: Matrix4x3f, dest: Vector4d): Vector4d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = w
        return dest
    }

    fun mul(mat: Matrix4f): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, this) else this.mulGeneric(mat, this)
    }

    fun mul(mat: Matrix4f, dest: Vector4d): Vector4d {
        return if (mat.properties() and 2 != 0) this.mulAffine(mat, dest) else this.mulGeneric(mat, dest)
    }

    private fun mulAffine(mat: Matrix4f, dest: Vector4d): Vector4d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = w
        return dest
    }

    private fun mulGeneric(mat: Matrix4f, dest: Vector4d): Vector4d {
        val rx = Math.fma(
            mat.m00.toDouble(),
            x,
            Math.fma(mat.m10.toDouble(), y, Math.fma(mat.m20.toDouble(), z, mat.m30.toDouble() * w))
        )
        val ry = Math.fma(
            mat.m01.toDouble(),
            x,
            Math.fma(mat.m11.toDouble(), y, Math.fma(mat.m21.toDouble(), z, mat.m31.toDouble() * w))
        )
        val rz = Math.fma(
            mat.m02.toDouble(),
            x,
            Math.fma(mat.m12.toDouble(), y, Math.fma(mat.m22.toDouble(), z, mat.m32.toDouble() * w))
        )
        val rw = Math.fma(
            mat.m03.toDouble(),
            x,
            Math.fma(mat.m13.toDouble(), y, Math.fma(mat.m23.toDouble(), z, mat.m33.toDouble() * w))
        )
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = rw
        return dest
    }

    fun mulProject(mat: Matrix4d, dest: Vector4d): Vector4d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = 1.0
        return dest
    }

    fun mulProject(mat: Matrix4d): Vector4d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        x = rx
        y = ry
        z = rz
        w = 1.0
        return this
    }

    fun mulProject(mat: Matrix4d, dest: Vector3d): Vector3d {
        val invW = 1.0 / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        val rx = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        val ry = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        val rz = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun mul(scalar: Double): Vector4d {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        return this
    }

    fun mul(scalar: Double, dest: Vector4d): Vector4d {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        dest.w = w * scalar
        return dest
    }

    operator fun div(scalar: Double): Vector4d {
        val inv = 1.0 / scalar
        x *= inv
        y *= inv
        z *= inv
        w *= inv
        return this
    }

    fun div(scalar: Double, dest: Vector4d): Vector4d {
        val inv = 1.0 / scalar
        dest.x = x * inv
        dest.y = y * inv
        dest.z = z * inv
        dest.w = w * inv
        return dest
    }

    fun rotate(quat: Quaterniond): Vector4d {
        quat.transform(this, this)
        return this
    }

    fun rotate(quat: Quaterniond, dest: Vector4d): Vector4d {
        quat.transform(this, dest)
        return dest
    }

    fun rotateAxis(angle: Double, x: Double, y: Double, z: Double): Vector4d {
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

    fun rotateAxis(angle: Double, aX: Double, aY: Double, aZ: Double, dest: Vector4d): Vector4d {
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

    private fun rotateAxisInternal(angle: Double, aX: Double, aY: Double, aZ: Double, dest: Vector4d): Vector4d {
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

    fun rotateX(angle: Double): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        this.y = y
        this.z = z
        return this
    }

    fun rotateX(angle: Double, dest: Vector4d): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        dest.w = w
        return dest
    }

    fun rotateY(angle: Double): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        this.x = x
        this.z = z
        return this
    }

    fun rotateY(angle: Double, dest: Vector4d): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        dest.w = w
        return dest
    }

    fun rotateZ(angle: Double): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        this.x = x
        this.y = y
        return this
    }

    fun rotateZ(angle: Double, dest: Vector4d): Vector4d {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dest.x = x
        dest.y = y
        dest.z = z
        dest.w = w
        return dest
    }

    fun lengthSquared(): Double {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
    }

    fun length(): Double {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
    }

    fun normalize(): Vector4d {
        val invLength = 1.0 / this.length()
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize(dest: Vector4d): Vector4d {
        val invLength = 1.0 / this.length()
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun normalize(length: Double): Vector4d {
        val invLength = 1.0 / this.length() * length
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize(length: Double, dest: Vector4d): Vector4d {
        val invLength = 1.0 / this.length() * length
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun normalize3(): Vector4d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize3(dest: Vector4d): Vector4d {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun distance(v: Vector4d): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
    }

    fun distance(x: Double, y: Double, z: Double, w: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
    }

    fun distanceSquared(v: Vector4d): Double {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
    }

    fun distanceSquared(x: Double, y: Double, z: Double, w: Double): Double {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
    }

    fun dot(v: Vector4d): Double {
        return Math.fma(x, v.x, Math.fma(y, v.y, Math.fma(z, v.z, w * v.w)))
    }

    fun dot(x: Double, y: Double, z: Double, w: Double): Double {
        return Math.fma(this.x, x, Math.fma(this.y, y, Math.fma(this.z, z, this.w * w)))
    }

    fun angleCos(v: Vector4d): Double {
        val length1Squared = Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
        val length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, Math.fma(v.z, v.z, v.w * v.w)))
        val dot = Math.fma(x, v.x, Math.fma(y, v.y, Math.fma(z, v.z, w * v.w)))
        return dot / Math.sqrt(length1Squared * length2Squared)
    }

    fun angle(v: Vector4d): Double {
        var cos = angleCos(v)
        cos = java.lang.Math.min(cos, 1.0)
        cos = java.lang.Math.max(cos, -1.0)
        return Math.acos(cos)
    }

    fun zero(): Vector4d {
        x = 0.0
        y = 0.0
        z = 0.0
        w = 0.0
        return this
    }

    fun negate(): Vector4d {
        x = -x
        y = -y
        z = -z
        w = -w
        return this
    }

    fun negate(dest: Vector4d): Vector4d {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        dest.w = -w
        return dest
    }

    fun min(v: Vector4d): Vector4d {
        x = java.lang.Math.min(x, v.x)
        y = java.lang.Math.min(y, v.y)
        z = java.lang.Math.min(z, v.z)
        w = java.lang.Math.min(w, v.w)
        return this
    }

    fun min(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        dest.w = java.lang.Math.min(w, v.w)
        return dest
    }

    fun max(v: Vector4d): Vector4d {
        x = java.lang.Math.max(x, v.x)
        y = java.lang.Math.max(y, v.y)
        z = java.lang.Math.max(z, v.z)
        w = java.lang.Math.max(w, v.w)
        return this
    }

    fun max(v: Vector4d, dest: Vector4d): Vector4d {
        dest.x = java.lang.Math.max(x, v.x)
        dest.y = java.lang.Math.max(y, v.y)
        dest.z = java.lang.Math.max(z, v.z)
        dest.w = java.lang.Math.max(w, v.w)
        return dest
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = java.lang.Double.doubleToLongBits(w)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(x)
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
            val other = obj as Vector4d
            if (java.lang.Double.doubleToLongBits(w) != java.lang.Double.doubleToLongBits(other.w)) {
                false
            } else if (java.lang.Double.doubleToLongBits(x) != java.lang.Double.doubleToLongBits(other.x)) {
                false
            } else if (java.lang.Double.doubleToLongBits(y) != java.lang.Double.doubleToLongBits(other.y)) {
                false
            } else {
                java.lang.Double.doubleToLongBits(z) == java.lang.Double.doubleToLongBits(other.z)
            }
        }
    }

    fun equals(v: Vector4d?, delta: Double): Boolean {
        return if (this === v) {
            true
        } else if (v == null) {
            false
        } else if (!Runtime.equals(x, v.x, delta)) {
            false
        } else if (!Runtime.equals(y, v.y, delta)) {
            false
        } else if (!Runtime.equals(z, v.z, delta)) {
            false
        } else {
            Runtime.equals(w, v.w, delta)
        }
    }

    fun equals(x: Double, y: Double, z: Double, w: Double): Boolean {
        return if (java.lang.Double.doubleToLongBits(this.x) != java.lang.Double.doubleToLongBits(x)) {
            false
        } else if (java.lang.Double.doubleToLongBits(this.y) != java.lang.Double.doubleToLongBits(y)) {
            false
        } else if (java.lang.Double.doubleToLongBits(this.z) != java.lang.Double.doubleToLongBits(z)) {
            false
        } else {
            java.lang.Double.doubleToLongBits(this.w) == java.lang.Double.doubleToLongBits(w)
        }
    }

    fun smoothStep(v: Vector4d, t: Double, dest: Vector4d): Vector4d {
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * x) * t2 + x * t + x
        dest.y = (y + y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * y) * t2 + y * t + y
        dest.z = (z + z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * z) * t2 + z * t + z
        dest.w = (w + w - v.w - v.w) * t3 + (3.0 * v.w - 3.0 * w) * t2 + w * t + w
        return dest
    }

    fun hermite(t0: Vector4d, v1: Vector4d, t1: Vector4d, t: Double, dest: Vector4d): Vector4d {
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        dest.w = (w + w - v1.w - v1.w + t1.w + t0.w) * t3 + (3.0 * v1.w - 3.0 * w - t0.w - t0.w - t1.w) * t2 + w * t + w
        return dest
    }

    fun lerp(other: Vector4d, t: Double): Vector4d {
        x = Math.fma(other.x - x, t, x)
        y = Math.fma(other.y - y, t, y)
        z = Math.fma(other.z - z, t, z)
        w = Math.fma(other.w - w, t, w)
        return this
    }

    fun lerp(other: Vector4d, t: Double, dest: Vector4d): Vector4d {
        dest.x = Math.fma(other.x - x, t, x)
        dest.y = Math.fma(other.y - y, t, y)
        dest.z = Math.fma(other.z - z, t, z)
        dest.w = Math.fma(other.w - w, t, w)
        return dest
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Double {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException()
        }
    }

    operator fun get(mode: Int, dest: Vector4i): Vector4i {
        dest.x = Math.roundUsing(x, mode)
        dest.y = Math.roundUsing(y, mode)
        dest.z = Math.roundUsing(z, mode)
        dest.w = Math.roundUsing(w, mode)
        return dest
    }

    operator fun get(dest: Vector4f): Vector4f {
        dest.x = x.toFloat()
        dest.y = y.toFloat()
        dest.z = z.toFloat()
        dest.w = w.toFloat()
        return dest
    }

    operator fun get(dest: Vector4d): Vector4d {
        dest.x = x
        dest.y = y
        dest.z = z
        dest.w = w
        return dest
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

    fun floor(): Vector4d {
        x = Math.floor(x)
        y = Math.floor(y)
        z = Math.floor(z)
        w = Math.floor(w)
        return this
    }

    fun floor(dest: Vector4d): Vector4d {
        dest.x = Math.floor(x)
        dest.y = Math.floor(y)
        dest.z = Math.floor(z)
        dest.w = Math.floor(w)
        return dest
    }

    fun ceil(): Vector4d {
        x = Math.ceil(x)
        y = Math.ceil(y)
        z = Math.ceil(z)
        w = Math.ceil(w)
        return this
    }

    fun ceil(dest: Vector4d): Vector4d {
        dest.x = Math.ceil(x)
        dest.y = Math.ceil(y)
        dest.z = Math.ceil(z)
        dest.w = Math.ceil(w)
        return dest
    }

    fun round(): Vector4d {
        x = Math.round(x).toDouble()
        y = Math.round(y).toDouble()
        z = Math.round(z).toDouble()
        w = Math.round(w).toDouble()
        return this
    }

    fun round(dest: Vector4d): Vector4d {
        dest.x = Math.round(x).toDouble()
        dest.y = Math.round(y).toDouble()
        dest.z = Math.round(z).toDouble()
        dest.w = Math.round(w).toDouble()
        return dest
    }

    val isFinite: Boolean
        get() = Math.isFinite(x) && Math.isFinite(y) && Math.isFinite(z) && Math.isFinite(w)

    fun absolute(): Vector4d {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        w = Math.abs(w)
        return this
    }

    fun absolute(dest: Vector4d): Vector4d {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        dest.w = Math.abs(w)
        return dest
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        fun lengthSquared(x: Double, y: Double, z: Double, w: Double): Double {
            return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
        }

        fun length(x: Double, y: Double, z: Double, w: Double): Double {
            return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
        }

        fun distance(
            x1: Double,
            y1: Double,
            z1: Double,
            w1: Double,
            x2: Double,
            y2: Double,
            z2: Double,
            w2: Double
        ): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
        }

        fun distanceSquared(
            x1: Double,
            y1: Double,
            z1: Double,
            w1: Double,
            x2: Double,
            y2: Double,
            z2: Double,
            w2: Double
        ): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
        }
    }
}