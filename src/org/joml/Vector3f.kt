package org.joml

@Suppress("unused")
class Vector3f : Cloneable {
    var x = 0f
    var y = 0f
    var z = 0f

    constructor() {}
    constructor(d: Float) {
        x = d
        y = d
        z = d
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(v: Vector3f) {
        x = v.x
        y = v.y
        z = v.z
    }

    constructor(v: Vector3i) {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
    }

    constructor(v: Vector2f, z: Float) {
        x = v.x
        y = v.y
        this.z = z
    }

    constructor(v: Vector2i, z: Float) {
        x = v.x.toFloat()
        y = v.y.toFloat()
        this.z = z
    }

    constructor(xyz: FloatArray) {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
    }

    fun set(v: Vector3f): Vector3f {
        x = v.x
        y = v.y
        z = v.z
        return this
    }

    fun set(v: Vector3d): Vector3f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        return this
    }

    fun set(v: Vector3i): Vector3f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        z = v.z.toFloat()
        return this
    }

    operator fun set(v: Vector2f, z: Float): Vector3f {
        x = v.x
        y = v.y
        this.z = z
        return this
    }

    operator fun set(v: Vector2d, z: Float): Vector3f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        this.z = z
        return this
    }

    operator fun set(v: Vector2i, z: Float): Vector3f {
        x = v.x.toFloat()
        y = v.y.toFloat()
        this.z = z
        return this
    }

    fun set(d: Float): Vector3f {
        x = d
        y = d
        z = d
        return this
    }

    operator fun set(x: Float, y: Float, z: Float): Vector3f {
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

    operator fun set(x: Double, y: Double, z: Double): Vector3f {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.z = z.toFloat()
        return this
    }

    fun set(xyz: FloatArray): Vector3f {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun setComponent(component: Int, value: Float): Vector3f {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun sub(v: Vector3f): Vector3f {
        x -= v.x
        y -= v.y
        z -= v.z
        return this
    }

    fun sub(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x - v.x
        dest.y = y - v.y
        dest.z = z - v.z
        return dest
    }

    fun sub(x: Float, y: Float, z: Float): Vector3f {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    fun sub(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        dest.x = this.x - x
        dest.y = this.y - y
        dest.z = this.z - z
        return dest
    }

    fun add(v: Vector3f): Vector3f {
        x += v.x
        y += v.y
        z += v.z
        return this
    }

    fun add(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x + v.x
        dest.y = y + v.y
        dest.z = z + v.z
        return dest
    }

    fun add(x: Float, y: Float, z: Float): Vector3f {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun add(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        return dest
    }

    fun fma(a: Vector3f, b: Vector3f): Vector3f {
        x = Math.fma(a.x, b.x, x)
        y = Math.fma(a.y, b.y, y)
        z = Math.fma(a.z, b.z, z)
        return this
    }

    fun fma(a: Float, b: Vector3f): Vector3f {
        x = Math.fma(a, b.x, x)
        y = Math.fma(a, b.y, y)
        z = Math.fma(a, b.z, z)
        return this
    }

    fun fma(a: Vector3f, b: Vector3f, dest: Vector3f): Vector3f {
        dest.x = Math.fma(a.x, b.x, x)
        dest.y = Math.fma(a.y, b.y, y)
        dest.z = Math.fma(a.z, b.z, z)
        return dest
    }

    fun fma(a: Float, b: Vector3f, dest: Vector3f): Vector3f {
        dest.x = Math.fma(a, b.x, x)
        dest.y = Math.fma(a, b.y, y)
        dest.z = Math.fma(a, b.z, z)
        return dest
    }

    fun mulAdd(a: Vector3f, b: Vector3f): Vector3f {
        x = Math.fma(x, a.x, b.x)
        y = Math.fma(y, a.y, b.y)
        z = Math.fma(z, a.z, b.z)
        return this
    }

    fun mulAdd(a: Float, b: Vector3f): Vector3f {
        x = Math.fma(x, a, b.x)
        y = Math.fma(y, a, b.y)
        z = Math.fma(z, a, b.z)
        return this
    }

    fun mulAdd(a: Vector3f, b: Vector3f, dest: Vector3f): Vector3f {
        dest.x = Math.fma(x, a.x, b.x)
        dest.y = Math.fma(y, a.y, b.y)
        dest.z = Math.fma(z, a.z, b.z)
        return dest
    }

    fun mulAdd(a: Float, b: Vector3f, dest: Vector3f): Vector3f {
        dest.x = Math.fma(x, a, b.x)
        dest.y = Math.fma(y, a, b.y)
        dest.z = Math.fma(z, a, b.z)
        return dest
    }

    fun mul(v: Vector3f): Vector3f {
        x *= v.x
        y *= v.y
        z *= v.z
        return this
    }

    fun mul(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x * v.x
        dest.y = y * v.y
        dest.z = z * v.z
        return dest
    }

    operator fun div(v: Vector3f): Vector3f {
        x /= v.x
        y /= v.y
        z /= v.z
        return this
    }

    fun div(v: Vector3f, dest: Vector3f): Vector3f {
        dest.x = x / v.x
        dest.y = y / v.y
        dest.z = z / v.z
        return dest
    }

    fun mulProject(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW
        return dest
    }

    fun mulProject(mat: Matrix4f, w: Float, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)))
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW
        return dest
    }

    fun mulProject(mat: Matrix4f): Vector3f {
        val x = x
        val y = y
        val z = z
        val invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW
        return this
    }

    fun mul(mat: Matrix3f): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        x = Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * lz))
        y = Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * lz))
        z = Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * lz))
        return this
    }

    fun mul(mat: Matrix3f, dest: Vector3f): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        dest.x = Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * lz))
        dest.y = Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * lz))
        dest.z = Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * lz))
        return dest
    }

    fun mul(mat: Matrix3d): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        x = Math.fma(mat.m00, lx.toDouble(), Math.fma(mat.m10, ly.toDouble(), mat.m20 * lz.toDouble())).toFloat()
        y = Math.fma(mat.m01, lx.toDouble(), Math.fma(mat.m11, ly.toDouble(), mat.m21 * lz.toDouble())).toFloat()
        z = Math.fma(mat.m02, lx.toDouble(), Math.fma(mat.m12, ly.toDouble(), mat.m22 * lz.toDouble())).toFloat()
        return this
    }

    fun mul(mat: Matrix3d, dest: Vector3f): Vector3f {
        val lx = x
        val ly = y
        val lz = z
        dest.x = Math.fma(mat.m00, lx.toDouble(), Math.fma(mat.m10, ly.toDouble(), mat.m20 * lz.toDouble())).toFloat()
        dest.y = Math.fma(mat.m01, lx.toDouble(), Math.fma(mat.m11, ly.toDouble(), mat.m21 * lz.toDouble())).toFloat()
        dest.z = Math.fma(mat.m02, lx.toDouble(), Math.fma(mat.m12, ly.toDouble(), mat.m22 * lz.toDouble())).toFloat()
        return dest
    }

    fun mul(mat: Matrix3x2f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        this.z = z
        return this
    }

    fun mul(mat: Matrix3x2f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        dest.z = z
        return dest
    }

    fun mulTranspose(mat: Matrix3f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        return this
    }

    fun mulTranspose(mat: Matrix3f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        return dest
    }

    fun mulPosition(mat: Matrix4f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return this
    }

    fun mulPosition(mat: Matrix4x3f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return this
    }

    fun mulPosition(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return dest
    }

    fun mulPosition(mat: Matrix4x3f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return dest
    }

    fun mulTransposePosition(mat: Matrix4f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)))
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)))
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)))
        return this
    }

    fun mulTransposePosition(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)))
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)))
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)))
        return dest
    }

    fun mulPositionW(mat: Matrix4f): Float {
        val x = x
        val y = y
        val z = z
        val w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return w
    }

    fun mulPositionW(mat: Matrix4f, dest: Vector3f): Float {
        val x = x
        val y = y
        val z = z
        val w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)))
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)))
        return w
    }

    fun mulDirection(mat: Matrix4d): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x.toDouble(), Math.fma(mat.m10, y.toDouble(), mat.m20 * z.toDouble())).toFloat()
        this.y = Math.fma(mat.m01, x.toDouble(), Math.fma(mat.m11, y.toDouble(), mat.m21 * z.toDouble())).toFloat()
        this.z = Math.fma(mat.m02, x.toDouble(), Math.fma(mat.m12, y.toDouble(), mat.m22 * z.toDouble())).toFloat()
        return this
    }

    fun mulDirection(mat: Matrix4f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        return this
    }

    fun mulDirection(mat: Matrix4x3f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        return this
    }

    fun mulDirection(mat: Matrix4d, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x.toDouble(), Math.fma(mat.m10, y.toDouble(), mat.m20 * z.toDouble())).toFloat()
        dest.y = Math.fma(mat.m01, x.toDouble(), Math.fma(mat.m11, y.toDouble(), mat.m21 * z.toDouble())).toFloat()
        dest.z = Math.fma(mat.m02, x.toDouble(), Math.fma(mat.m12, y.toDouble(), mat.m22 * z.toDouble())).toFloat()
        return dest
    }

    fun mulDirection(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        return dest
    }

    fun mulDirection(mat: Matrix4x3f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z))
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z))
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z))
        return dest
    }

    fun mulTransposeDirection(mat: Matrix4f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        return this
    }

    fun mulTransposeDirection(mat: Matrix4f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z))
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z))
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z))
        return dest
    }

    fun mul(scalar: Float): Vector3f {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun mul(scalar: Float, dest: Vector3f): Vector3f {
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun mul(x: Float, y: Float, z: Float): Vector3f {
        this.x *= x
        this.y *= y
        this.z *= z
        return this
    }

    fun mul(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        dest.x = this.x * x
        dest.y = this.y * y
        dest.z = this.z * z
        return dest
    }

    operator fun div(scalar: Float): Vector3f {
        val inv = 1f / scalar
        x *= inv
        y *= inv
        z *= inv
        return this
    }

    fun div(scalar: Float, dest: Vector3f): Vector3f {
        val inv = 1f / scalar
        dest.x = x * inv
        dest.y = y * inv
        dest.z = z * inv
        return dest
    }

    fun div(x: Float, y: Float, z: Float): Vector3f {
        this.x /= x
        this.y /= y
        this.z /= z
        return this
    }

    fun div(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        dest.x = this.x / x
        dest.y = this.y / y
        dest.z = this.z / z
        return dest
    }

    fun rotate(quat: Quaternionf): Vector3f {
        return quat.transform(this, this)
    }

    fun rotate(quat: Quaternionf, dest: Vector3f?): Vector3f {
        return quat.transform(this, dest!!)
    }

    fun rotationTo(toDir: Vector3f?, dest: Quaternionf): Quaternionf {
        return dest.rotationTo(this, toDir!!)
    }

    fun rotationTo(toDirX: Float, toDirY: Float, toDirZ: Float, dest: Quaternionf): Quaternionf {
        return dest.rotationTo(x, y, z, toDirX, toDirY, toDirZ)
    }

    fun rotateAxis(angle: Float, x: Float, y: Float, z: Float): Vector3f {
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

    fun rotateAxis(angle: Float, aX: Float, aY: Float, aZ: Float, dest: Vector3f): Vector3f {
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

    private fun rotateAxisInternal(angle: Float, aX: Float, aY: Float, aZ: Float, dest: Vector3f): Vector3f {
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

    fun rotateX(angle: Float): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        this.y = y
        this.z = z
        return this
    }

    fun rotateX(angle: Float, dest: Vector3f): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun rotateY(angle: Float): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        this.x = x
        this.z = z
        return this
    }

    fun rotateY(angle: Float, dest: Vector3f): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun rotateZ(angle: Float): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        this.x = x
        this.y = y
        return this
    }

    fun rotateZ(angle: Float, dest: Vector3f): Vector3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    fun lengthSquared(): Float {
        return Math.fma(x, x, Math.fma(y, y, z * z))
    }

    fun length(): Float {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
    }

    fun normalize(): Vector3f {
        val scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun normalize(dest: Vector3f): Vector3f {
        val scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun normalize(length: Float): Vector3f {
        val scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun normalize(length: Float, dest: Vector3f): Vector3f {
        val scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z))) * length
        dest.x = x * scalar
        dest.y = y * scalar
        dest.z = z * scalar
        return dest
    }

    fun cross(v: Vector3f): Vector3f {
        val rx = Math.fma(y, v.z, -z * v.y)
        val ry = Math.fma(z, v.x, -x * v.z)
        val rz = Math.fma(x, v.y, -y * v.x)
        x = rx
        y = ry
        z = rz
        return this
    }

    fun cross(x: Float, y: Float, z: Float): Vector3f {
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun cross(v: Vector3f, dest: Vector3f): Vector3f {
        val rx = Math.fma(y, v.z, -z * v.y)
        val ry = Math.fma(z, v.x, -x * v.z)
        val rz = Math.fma(x, v.y, -y * v.x)
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun cross(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val rx = Math.fma(this.y, z, -this.z * y)
        val ry = Math.fma(this.z, x, -this.x * z)
        val rz = Math.fma(this.x, y, -this.y * x)
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun distance(v: Vector3f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)))
    }

    fun distance(x: Float, y: Float, z: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)))
    }

    fun distanceSquared(v: Vector3f): Float {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
    }

    fun distanceSquared(x: Float, y: Float, z: Float): Float {
        val dx = this.x - x
        val dy = this.y - y
        val dz = this.z - z
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
    }

    fun dot(v: Vector3f): Float {
        return Math.fma(x, v.x, Math.fma(y, v.y, z * v.z))
    }

    fun dot(x: Float, y: Float, z: Float): Float {
        return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
    }

    fun angleCos(v: Vector3f): Float {
        val x = x
        val y = y
        val z = z
        val length1Squared = Math.fma(x, x, Math.fma(y, y, z * z))
        val length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, v.z * v.z))
        val dot = Math.fma(x, v.x, Math.fma(y, v.y, z * v.z))
        return dot / Math.sqrt(length1Squared * length2Squared)
    }

    fun angle(v: Vector3f): Float {
        var cos = angleCos(v)
        cos = java.lang.Math.min(cos, 1f)
        cos = java.lang.Math.max(cos, -1f)
        return Math.acos(cos)
    }

    fun angleSigned(v: Vector3f, n: Vector3f): Float {
        return this.angleSigned(v.x, v.y, v.z, n.x, n.y, n.z)
    }

    fun angleSigned(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float): Float {
        val tx = this.x
        val ty = this.y
        val tz = this.z
        return Math.atan2(
            (ty * z - tz * y) * nx + (tz * x - tx * z) * ny + (tx * y - ty * x) * nz,
            tx * x + ty * y + tz * z
        )
    }

    fun min(v: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = java.lang.Math.min(x, v.x)
        this.y = java.lang.Math.min(y, v.y)
        this.z = java.lang.Math.min(z, v.z)
        return this
    }

    fun min(v: Vector3f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = java.lang.Math.min(x, v.x)
        dest.y = java.lang.Math.min(y, v.y)
        dest.z = java.lang.Math.min(z, v.z)
        return dest
    }

    fun max(v: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        this.x = java.lang.Math.max(x, v.x)
        this.y = java.lang.Math.max(y, v.y)
        this.z = java.lang.Math.max(z, v.z)
        return this
    }

    fun max(v: Vector3f, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        dest.x = Math.max(x, v.x)
        dest.y = Math.max(y, v.y)
        dest.z = Math.max(z, v.z)
        return dest
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

    fun negate(): Vector3f {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun negate(dest: Vector3f): Vector3f {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        return dest
    }

    fun absolute(): Vector3f {
        x = Math.abs(x)
        y = Math.abs(y)
        z = Math.abs(z)
        return this
    }

    fun absolute(dest: Vector3f): Vector3f {
        dest.x = Math.abs(x)
        dest.y = Math.abs(y)
        dest.z = Math.abs(z)
        return dest
    }

    override fun hashCode(): Int {
        var result = java.lang.Float.floatToIntBits(x)
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
            val other = obj as Vector3f
            if (java.lang.Float.floatToIntBits(x) != java.lang.Float.floatToIntBits(other.x)) {
                false
            } else if (java.lang.Float.floatToIntBits(y) != java.lang.Float.floatToIntBits(other.y)) {
                false
            } else {
                java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z)
            }
        }
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
        return if (java.lang.Float.floatToIntBits(this.x) != java.lang.Float.floatToIntBits(x)) {
            false
        } else if (java.lang.Float.floatToIntBits(this.y) != java.lang.Float.floatToIntBits(y)) {
            false
        } else {
            java.lang.Float.floatToIntBits(this.z) == java.lang.Float.floatToIntBits(z)
        }
    }

    fun reflect(normal: Vector3f): Vector3f {
        val x = normal.x
        val y = normal.y
        val z = normal.z
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(x: Float, y: Float, z: Float): Vector3f {
        val dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z))
        this.x -= (dot + dot) * x
        this.y -= (dot + dot) * y
        this.z -= (dot + dot) * z
        return this
    }

    fun reflect(normal: Vector3f, dest: Vector3f): Vector3f {
        return this.reflect(normal.x, normal.y, normal.z, dest)
    }

    fun reflect(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val dot = this.dot(x, y, z)
        dest.x = this.x - (dot + dot) * x
        dest.y = this.y - (dot + dot) * y
        dest.z = this.z - (dot + dot) * z
        return dest
    }

    fun half(other: Vector3f): Vector3f {
        return this.set(this).add(other.x, other.y, other.z).normalize()
    }

    fun half(other: Vector3f, dest: Vector3f): Vector3f {
        return this.half(other.x, other.y, other.z, dest)
    }

    @JvmOverloads
    fun half(x: Float, y: Float, z: Float, dest: Vector3f = this): Vector3f {
        return dest.set(this).add(x, y, z).normalize()
    }

    fun smoothStep(v: Vector3f, t: Float, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x
        dest.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y
        dest.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z
        return dest
    }

    fun hermite(t0: Vector3f, v1: Vector3f, t1: Vector3f, t: Float, dest: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val t2 = t * t
        val t3 = t2 * t
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z
        return dest
    }

    @JvmOverloads
    fun lerp(other: Vector3f, t: Float, dest: Vector3f = this): Vector3f {
        dest.x = Math.fma(other.x - x, t, x)
        dest.y = Math.fma(other.y - y, t, y)
        dest.z = Math.fma(other.z - z, t, z)
        return dest
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(component: Int): Float {
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
        dest.x = x
        dest.y = y
        dest.z = z
        return dest
    }

    operator fun get(dest: Vector3d): Vector3d {
        dest.x = x.toDouble()
        dest.y = y.toDouble()
        dest.z = z.toDouble()
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
    fun orthogonalize(v: Vector3f, dest: Vector3f = this): Vector3f {
        val rx: Float
        val ry: Float
        val rz: Float
        if (Math.abs(v.x) > Math.abs(v.z)) {
            rx = -v.y
            ry = v.x
            rz = 0f
        } else {
            rx = 0f
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
    fun orthogonalizeUnit(v: Vector3f, dest: Vector3f = this): Vector3f {
        return orthogonalize(v, dest)
    }

    @JvmOverloads
    fun floor(dest: Vector3f = this): Vector3f {
        dest.x = Math.floor(x)
        dest.y = Math.floor(y)
        dest.z = Math.floor(z)
        return dest
    }

    @JvmOverloads
    fun ceil(dest: Vector3f = this): Vector3f {
        dest.x = Math.ceil(x)
        dest.y = Math.ceil(y)
        dest.z = Math.ceil(z)
        return dest
    }

    @JvmOverloads
    fun round(dest: Vector3f = this): Vector3f {
        dest.x = Math.round(x).toFloat()
        dest.y = Math.round(y).toFloat()
        dest.z = Math.round(z).toFloat()
        return dest
    }

    val isFinite: Boolean
        get() = Math.isFinite(x) && Math.isFinite(y) && Math.isFinite(z)

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        fun lengthSquared(x: Float, y: Float, z: Float): Float {
            return Math.fma(x, x, Math.fma(y, y, z * z))
        }

        fun length(x: Float, y: Float, z: Float): Float {
            return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)))
        }

        fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2))
        }

        fun distanceSquared(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz))
        }
    }
}