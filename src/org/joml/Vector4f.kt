package org.joml

class Vector4f {
    var x = 0f
    var y = 0f
    var z = 0f
    var w: Float

    constructor() {
        w = 1f
    }

    constructor(v: Vector4f) {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
    }

    constructor(v: Vector4i) {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        w = v.w.toFloat()
    }

    constructor(v: Vector3f, w: Float) {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
    }

    constructor(v: Vector3i, w: Float) {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        this.w = w
    }

    constructor(v: Vector2f, z: Float, w: Float) {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
    }

    constructor(v: Vector2i, z: Float, w: Float) {
        x = v.x.toFloat()
        y = v.y.toFloat()
        this.z = z
        this.w = w
    }

    constructor(d: Float) {
        x = d
        y = d
        z = d
        w = d
    }

    constructor(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(xyzw: FloatArray) {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
    }

    fun set(v: Vector4f): Vector4f {
        x = v.x
        y = v.y
        z = v.z
        w = v.w
        return this
    }

    fun set(v: Vector4i): Vector4f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        w = v.w.toFloat()
        return this
    }

    fun set(v: Vector4d): Vector4f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        w = v.w.toFloat()
        return this
    }

    operator fun set(v: Vector3f, w: Float): Vector4f {
        x = v.x
        y = v.y
        z = v.z
        this.w = w
        return this
    }

    operator fun set(v: Vector3i, w: Float): Vector4f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        this.w = w
        return this
    }

    operator fun set(v: Vector2f, z: Float, w: Float): Vector4f {
        x = v.x
        y = v.y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(v: Vector2i, z: Float, w: Float): Vector4f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        this.z = z
        this.w = w
        return this
    }

    fun set(d: Float): Vector4f {
        x = d
        y = d
        z = d
        w = d
        return this
    }

    operator fun set(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(x: Float, y: Float, z: Float): Vector4f {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(d: Double): Vector4f {
        x = d.toFloat()
        y = d.toFloat()
        z = d.toFloat()
        w = d.toFloat()
        return this
    }

    operator fun set(x: Double, y: Double, z: Double, w: Double): Vector4f {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.z = z.toFloat()
        this.w = w.toFloat()
        return this
    }

    fun set(xyzw: FloatArray): Vector4f {
        x = xyzw[0]
        y = xyzw[1]
        z = xyzw[2]
        w = xyzw[3]
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Float): Vector4f {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector4f): Vector4f {
        x -= v.x
        y -= v.y
        z -= v.z
        w -= v.w
        return this
    }

    fun sub(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x -= x
        this.y -= y
        this.z -= z
        this.w -= w
        return this
    }

    fun sub(v: Vector4f, dest: Vector4f): Vector4f {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        dest.w = w - v.w
        return dest
    }

    fun sub(x: Float, y: Float, z: Float, w: Float, dest: Vector4f): Vector4f {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        dest.w = this.w - w
        return dest
    }

    fun add(v: Vector4f): Vector4f {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
        return this
    }

    fun add(v: Vector4f, dest: Vector4f): Vector4f {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        dest.w = w + v.w
        return dest
    }

    fun add(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x += x
        this.y += y
        this.z += z
        this.w += w
        return this
    }

    fun add(x: Float, y: Float, z: Float, w: Float, dest: Vector4f): Vector4f {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        dest.w = this.w + w
        return dest
    }

    fun fma(a: Vector4f, b: Vector4f): Vector4f {
        x = Math.fma(a.x, b.x, x)
        y = Math.fma(a.y, b.y, y)
        z = Math.fma(a.z, b.z, z)
        w = Math.fma(a.w, b.w, w)
        return this
    }

    fun fma(a: Float, b: Vector4f): Vector4f {
        x = Math.fma(a, b.x, x)
        y = Math.fma(a, b.y, y)
        z = Math.fma(a, b.z, z)
        w = Math.fma(a, b.w, w)
        return this
    }

    fun fma(a: Vector4f, b: Vector4f, dest: Vector4f): Vector4f {
        dest.x = Math.fma(a.x, b.x, x)
        dest.y = Math.fma(a.y, b.y, y)
        dest.z = Math.fma(a.z, b.z, z)
        dest.w = Math.fma(a.w, b.w, w)
        return dest
    }

    fun fma(a: Float, b: Vector4f, dest: Vector4f): Vector4f {
        dest.x = Math.fma(a, b.x, x)
        dest.y = Math.fma(a, b.y, y)
        dest.z = Math.fma(a, b.z, z)
        dest.w = Math.fma(a, b.w, w)
        return dest
    }

    fun mulAdd(a: Vector4f, b: Vector4f): Vector4f {
        x = Math.fma(x, a.x, b.x)
        y = Math.fma(y, a.y, b.y)
        z = Math.fma(z, a.z, b.z)
        return this
    }

    fun mulAdd(a: Float, b: Vector4f): Vector4f {
        x = Math.fma(x, a, b.x)
        y = Math.fma(y, a, b.y)
        z = Math.fma(z, a, b.z)
        return this
    }

    fun mulAdd(a: Vector4f, b: Vector4f, dest: Vector4f): Vector4f {
        dest.x = Math.fma(x, a.x, b.x)
        dest.y = Math.fma(y, a.y, b.y)
        dest.z = Math.fma(z, a.z, b.z)
        return dest
    }

    fun mulAdd(a: Float, b: Vector4f, dest: Vector4f): Vector4f {
        dest.x = Math.fma(x, a, b.x)
        dest.y = Math.fma(y, a, b.y)
        dest.z = Math.fma(z, a, b.z)
        return dest
    }

    fun mul(v: Vector4f): Vector4f {
        x *= v.x
        y *= v.y
        z *= v.z
        w *= v.w
        return this
    }

    fun mul(v: Vector4f, dest: Vector4f): Vector4f {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        dest.w = w * v.w
        return dest
    }

    operator fun div(v: Vector4f): Vector4f {
        x /= v.x
        y /= v.y
        z /= v.z
        w /= v.w
        return this
    }

    fun div(v: Vector4f, dest: Vector4f): Vector4f {
        dest.x = x / v.x
        dest.y = y / v.y
        dest.z = z / v.z
        dest.w = w / v.w
        return dest
    }

    fun mul(mat: Matrix4f): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffine(mat, this) else mulGeneric(mat, this)
    }

    fun mul(mat: Matrix4f, dest: Vector4f): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffine(mat, dest) else mulGeneric(mat, dest)
    }

    fun mulTranspose(mat: Matrix4f): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, this) else mulGenericTranspose(mat, this)
    }

    fun mulTranspose(mat: Matrix4f, dest: Vector4f): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dest) else mulGenericTranspose(mat, dest)
    }

    fun mulAffine(mat: Matrix4f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        dest.w = w
        return dest
    }

    private fun mulGeneric(mat: Matrix4f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        dest.w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        return dest
    }

    fun mulAffineTranspose(mat: Matrix4f, dest: Vector4f): Vector4f {
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

    private fun mulGenericTranspose(mat: Matrix4f, dest: Vector4f): Vector4f {
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

    fun mul(mat: Matrix4x3f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        this.w = w
        return this
    }

    fun mul(mat: Matrix4x3f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w)))
        dest.w = w
        return dest
    }

    fun mulProject(mat: Matrix4f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        dest.w = 1f
        return dest
    }

    fun mulProject(mat: Matrix4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        this.w = 1f
        return this
    }

    fun mulProject(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        return dest
    }

    fun mul(scalar: Float): Vector4f {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        return this
    }

    fun mul(scalar: Float, dest: Vector4f): Vector4f {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        dest.w = w * scalar
        return dest
    }

    fun mul(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x *= x
        this.y *= y
        this.z *= z
        this.w *= w
        return this
    }

    fun mul(x: Float, y: Float, z: Float, w: Float, dest: Vector4f): Vector4f {
        dest.x = this.x * x
        dest.y = this.y * y
        dest.z = this.z * z
        dest.w = this.w * w
        return dest
    }

    operator fun div(scalar: Float): Vector4f {
        val inv = 1f / scalar
        x *= inv
        y *= inv
        z *= inv
        w *= inv
        return this
    }

    fun div(scalar: Float, dest: Vector4f): Vector4f {
        val inv = 1f / scalar
        dest.x = x * inv
        dest.y = y * inv
        dest.z = z * inv
        dest.w = w * inv
        return dest
    }

    fun div(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x /= x
        this.y /= y
        this.z /= z
        this.w /= w
        return this
    }

    fun div(x: Float, y: Float, z: Float, w: Float, dest: Vector4f): Vector4f {
        dest.x = this.x / x
        dest.y = this.y / y
        dest.z = this.z / z
        dest.w = this.w / w
        return dest
    }

    fun rotate(quat: Quaternionf): Vector4f {
        return quat.transform(this, this)
    }

    fun rotate(quat: Quaternionf, dest: Vector4f?): Vector4f {
        return quat.transform(this, dest!!)
    }

    fun rotateAbout(angle: Float, x: Float, y: Float, z: Float): Vector4f {
        return if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            this.rotateX(x * angle, this)
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            this.rotateY(y * angle, this)
        } else {
            if (x == 0f && y == 0f && Math.absEqualsOne(z)) this.rotateZ(
                z * angle,
                this
            ) else rotateAxisInternal(angle, x, y, z, this)
        }
    }

    fun rotateAxis(angle: Float, aX: Float, aY: Float, aZ: Float, dest: Vector4f): Vector4f {
        return if (aY == 0f && aZ == 0f && Math.absEqualsOne(aX)) {
            this.rotateX(aX * angle, dest)
        } else if (aX == 0f && aZ == 0f && Math.absEqualsOne(aY)) {
            this.rotateY(aY * angle, dest)
        } else {
            if (aX == 0f && aY == 0f && Math.absEqualsOne(aZ)) this.rotateZ(
                aZ * angle,
                dest
            ) else rotateAxisInternal(angle, aX, aY, aZ, dest)
        }
    }

    private fun rotateAxisInternal(angle: Float, aX: Float, aY: Float, aZ: Float, dest: Vector4f): Vector4f {
        val halfAngle = angle * 0.5f
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
        val x = x
        val y = y
        val z = z
        dest.x = (w2 + x2 - z2 - y2) * x + (-zw + xy - zw + xy) * y + (yw + xz + xz + yw) * z
        dest.y = (xy + zw + zw + xy) * x + (y2 - z2 + w2 - x2) * y + (yz + yz - xw - xw) * z
        dest.z = (xz - yw + xz - yw) * x + (yz + yz + xw + xw) * y + (z2 - y2 - x2 + w2) * z
        return dest
    }

    fun rotateX(angle: Float): Vector4f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        this.y = y
        this.z = z
        return this
    }

    fun rotateX(angle: Float, dest: Vector4f): Vector4f {
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

    fun rotateY(angle: Float): Vector4f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        this.x = x
        this.z = z
        return this
    }

    fun rotateY(angle: Float, dest: Vector4f): Vector4f {
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

    fun rotateZ(angle: Float): Vector4f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        this.x = x
        this.y = y
        return this
    }

    fun rotateZ(angle: Float, dest: Vector4f): Vector4f {
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

    fun lengthSquared(): Float {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
    }

    fun length(): Float {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
    }

    fun normalize(): Vector4f {
        val invLength = 1f / this.length()
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize(dest: Vector4f): Vector4f {
        val invLength = 1f / this.length()
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun normalize(length: Float): Vector4f {
        val invLength = 1f / this.length() * length
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize(length: Float, dest: Vector4f): Vector4f {
        val invLength = 1f / this.length() * length
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun normalize3(): Vector4f {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        x *= invLength
        y *= invLength
        z *= invLength
        w *= invLength
        return this
    }

    fun normalize3(dest: Vector4f): Vector4f {
        val invLength = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        dest.x = x * invLength
        dest.y = y * invLength
        dest.z = z * invLength
        dest.w = w * invLength
        return dest
    }

    fun distance(v: Vector4f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
    }

    fun distance(x: Float, y: Float, z: Float, w: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
    }

    fun distanceSquared(v: Vector4f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        val dw = w - v.w
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
    }

    fun distanceSquared(x: Float, y: Float, z: Float, w: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        val dw = this.w - w
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
    }

    fun dot(v: Vector4f): Float {
        return Math.fma(x, v.x, Math.fma(y, v.y, Math.fma(z, v.z, w * v.w)))
    }

    fun dot(x: Float, y: Float, z: Float, w: Float): Float {
        return Math.fma(this.x, x, Math.fma(this.y, y, Math.fma(this.z, z, this.w * w)))
    }

    fun angleCos(v: Vector4f): Float {
        val x = x
        val y = y
        val z = z
        val w = w
        val length1Squared = Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
        val length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, Math.fma(v.z, v.z, v.w * v.w)))
        val dot = Math.fma(x, v.x, Math.fma(y, v.y, Math.fma(z, v.z, w * v.w)))
        return dot / Math.sqrt(length1Squared * length2Squared)
    }

    fun angle(v: Vector4f): Float {
        var cos = angleCos(v)
        cos = java.lang.Math.min(cos, 1f)
        cos = java.lang.Math.max(cos, -1f)
        return Math.acos(cos)
    }

    fun zero(): Vector4f {
        x = 0f
        y = 0f
        z = 0f
        w = 0f
        return this
    }

    fun negate(): Vector4f {
        x = -x
        y = -y
        z = -z
        w = -w
        return this
    }

    fun negate(dest: Vector4f): Vector4f {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        dest.w = -w
        return dest
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    fun min(v: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        this.x = java.lang.Math.min(x, v.x)
        this.y = java.lang.Math.min(y, v.y)
        this.z = java.lang.Math.min(z, v.z)
        this.w = java.lang.Math.min(w, v.w)
        return this
    }

    fun min(v: Vector4f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        dest.w = java.lang.Math.min(w, v.w)
        return dest
    }

    fun max(v: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        this.x = java.lang.Math.max(x, v.x)
        this.y = java.lang.Math.max(y, v.y)
        this.z = java.lang.Math.max(z, v.z)
        this.w = java.lang.Math.max(w, v.w)
        return this
    }

    fun max(v: Vector4f, dest: Vector4f): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = java.lang.Math.max(x, v.x)
        dest.y = java.lang.Math.max(y, v.y)
        dest.z = java.lang.Math.max(z, v.z)
        dest.w = java.lang.Math.max(w, v.w)
        return dest
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + java.lang.Float.floatToIntBits(w)
        result = 31 * result + java.lang.Float.floatToIntBits(x)
        result = 31 * result + java.lang.Float.floatToIntBits(y)
        result = 31 * result + java.lang.Float.floatToIntBits(z)
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
            val other = obj as Vector4f
            if (java.lang.Float.floatToIntBits(w) != java.lang.Float.floatToIntBits(other.w)) {
                false
            } else if (java.lang.Float.floatToIntBits(x) != java.lang.Float.floatToIntBits(other.x)) {
                false
            } else if (java.lang.Float.floatToIntBits(y) != java.lang.Float.floatToIntBits(other.y)) {
                false
            } else {
                java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z)
            }
        }
    }

    fun equals(v: Vector4f?, delta: Float): Boolean {
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

    fun equals(x: Float, y: Float, z: Float, w: Float): Boolean {
        return if (java.lang.Float.floatToIntBits(this.x) != java.lang.Float.floatToIntBits(x)) {
            false
        } else if (java.lang.Float.floatToIntBits(this.y) != java.lang.Float.floatToIntBits(y)) {
            false
        } else if (java.lang.Float.floatToIntBits(this.z) != java.lang.Float.floatToIntBits(z)) {
            false
        } else {
            java.lang.Float.floatToIntBits(this.w) == java.lang.Float.floatToIntBits(w)
        }
    }

    fun smoothStep(v: Vector4f, t: Float, dest: Vector4f): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x
        dest.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y
        dest.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z
        dest.w = (w + w - v.w - v.w) * t3 + (3f * v.w - 3f * w) * t2 + w * t + w
        return dest
    }

    fun hermite(t0: Vector4f, v1: Vector4f, t1: Vector4f, t: Float, dest: Vector4f): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        val x = x
        val y = y
        val z = z
        val w = w
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        dest.w = (w + w - v1.w - v1.w + t1.w + t0.w) * t3 + (3f * v1.w - 3f * w - t0.w - t0.w - t1.w) * t2 + w * t + w
        return dest
    }

    fun lerp(other: Vector4f, t: Float): Vector4f {
        x = Math.fma(other.x - x, t, x)
        y = Math.fma(other.y - y, t, y)
        z = Math.fma(other.z - z, t, z)
        w = Math.fma(other.w - w, t, w)
        return this
    }

    fun lerp(other: Vector4f, t: Float, dest: Vector4f): Vector4f {
        dest.x = Math.fma(other.x - x, t, x)
        dest.y = Math.fma(other.y - y, t, y)
        dest.z = Math.fma(other.z - z, t, z)
        dest.w = Math.fma(other.w - w, t, w)
        return dest
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Float {
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
        dest.x = x
        dest.y = y
        dest.z = z
        dest.w = w
        return dest
    }

    operator fun get(dest: Vector4d): Vector4d {
        dest.x = x.toDouble()
        dest.y = y.toDouble()
        dest.z = z.toDouble()
        dest.w = w.toDouble()
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

    fun floor(): Vector4f {
        x = Math.floor(x)
        y = Math.floor(y)
        z = Math.floor(z)
        w = Math.floor(w)
        return this
    }

    fun floor(dest: Vector4f): Vector4f {
        dest.x = Math.floor(x)
        dest.y = Math.floor(y)
        dest.z = Math.floor(z)
        dest.w = Math.floor(w)
        return dest
    }

    fun ceil(): Vector4f {
        x = Math.ceil(x)
        y = Math.ceil(y)
        z = Math.ceil(z)
        w = Math.ceil(w)
        return this
    }

    fun ceil(dest: Vector4f): Vector4f {
        dest.x = Math.ceil(x)
        dest.y = Math.ceil(y)
        dest.z = Math.ceil(z)
        dest.w = Math.ceil(w)
        return dest
    }

    fun round(): Vector4f {
        x = Math.round(x).toFloat()
        y = Math.round(y).toFloat()
        z = Math.round(z).toFloat()
        w = Math.round(w).toFloat()
        return this
    }

    fun round(dest: Vector4f): Vector4f {
        dest.x = Math.round(x).toFloat()
        dest.y = Math.round(y).toFloat()
        dest.z = Math.round(z).toFloat()
        dest.w = Math.round(w).toFloat()
        return dest
    }

    val isFinite: Boolean
        get() = Math.isFinite(x) && Math.isFinite(y) && Math.isFinite(z) && Math.isFinite(w)

    fun absolute(): Vector4f {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        w = Math.abs(w)
        return this
    }

    fun absolute(dest: Vector4f): Vector4f {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        dest.w = Math.abs(w)
        return dest
    }

    companion object {
        fun lengthSquared(x: Float, y: Float, z: Float, w: Float): Float {
            return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
        }

        fun lengthSquared(x: Int, y: Int, z: Int, w: Int): Float {
            return Math.fma(
                x.toFloat(),
                x.toFloat(),
                Math.fma(y.toFloat(), y.toFloat(), Math.fma(z.toFloat(), z.toFloat(), (w * w).toFloat()))
            )
        }

        fun length(x: Float, y: Float, z: Float, w: Float): Float {
            return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
        }

        fun distance(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))))
        }

        fun distanceSquared(
            x1: Float,
            y1: Float,
            z1: Float,
            w1: Float,
            x2: Float,
            y2: Float,
            z2: Float,
            w2: Float
        ): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)))
        }
    }
}