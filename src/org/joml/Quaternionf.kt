package org.joml

@Suppress("unused")
class Quaternionf : Cloneable {
    var x = 0f
    var y = 0f
    var z = 0f
    var w = 0f

    constructor() {
        w = 1f
    }

    constructor(x: Double, y: Double, z: Double, w: Double) {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.z = z.toFloat()
        this.w = w.toFloat()
    }

    constructor(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(source: Quaternionf) {
        this.set(source)
    }

    constructor(source: Quaterniond) {
        this.set(source)
    }

    constructor(axisAngle: AxisAngle4f) {
        val sin = Math.sin(axisAngle.angle * 0.5f)
        val cos = Math.cosFromSin(sin, axisAngle.angle * 0.5f)
        x = axisAngle.x * sin
        y = axisAngle.y * sin
        z = axisAngle.z * sin
        w = cos
    }

    constructor(axisAngle: AxisAngle4d) {
        val sin = Math.sin(axisAngle.angle * 0.5)
        val cos = Math.cosFromSin(sin, axisAngle.angle * 0.5)
        x = (axisAngle.x * sin).toFloat()
        y = (axisAngle.y * sin).toFloat()
        z = (axisAngle.z * sin).toFloat()
        w = cos.toFloat()
    }

    @JvmOverloads
    fun normalize(dest: Quaternionf = this): Quaternionf {
        val invNorm = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
        dest.x = x * invNorm
        dest.y = y * invNorm
        dest.z = z * invNorm
        dest.w = w * invNorm
        return dest
    }

    @JvmOverloads
    fun add(x: Float, y: Float, z: Float, w: Float, dest: Quaternionf = this): Quaternionf {
        dest.x = this.x + x
        dest.y = this.y + y
        dest.z = this.z + z
        dest.w = this.w + w
        return dest
    }

    @JvmOverloads
    fun add(q2: Quaternionf, dest: Quaternionf = this): Quaternionf {
        dest.x = x + q2.x
        dest.y = y + q2.y
        dest.z = z + q2.z
        dest.w = w + q2.w
        return dest
    }

    fun dot(otherQuat: Quaternionf): Float {
        return x * otherQuat.x + y * otherQuat.y + z * otherQuat.z + w * otherQuat.w
    }

    fun angle(): Float {
        return (2.0 * Math.safeAcos(w).toDouble()).toFloat()
    }

    operator fun get(dest: Matrix3f): Matrix3f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix3d): Matrix3d {
        return dest.set(this)
    }

    operator fun get(dest: Matrix4f): Matrix4f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix4d): Matrix4d {
        return dest.set(this)
    }

    operator fun get(dest: Matrix4x3f): Matrix4x3f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix4x3d): Matrix4x3d {
        return dest.set(this)
    }

    operator fun get(dest: AxisAngle4f): AxisAngle4f {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Float
        if (w > 1f) {
            s = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dest.angle = 2f * Math.acos(w)
        s = Math.sqrt(1f - w * w)
        if (s < 0.001f) {
            dest.x = x
            dest.y = y
            dest.z = z
        } else {
            s = 1f / s
            dest.x = x * s
            dest.y = y * s
            dest.z = z * s
        }
        return dest
    }

    operator fun get(dest: AxisAngle4d): AxisAngle4d {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Float
        if (w > 1f) {
            s = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dest.angle = (2f * Math.acos(w)).toDouble()
        s = Math.sqrt(1f - w * w)
        if (s < 0.001f) {
            dest.x = x.toDouble()
            dest.y = y.toDouble()
            dest.z = z.toDouble()
        } else {
            s = 1f / s
            dest.x = (x * s).toDouble()
            dest.y = (y * s).toDouble()
            dest.z = (z * s).toDouble()
        }
        return dest
    }

    operator fun get(dest: Quaterniond): Quaterniond {
        return dest.set(this)
    }

    operator fun get(dest: Quaternionf): Quaternionf {
        return dest.set(this)
    }

    operator fun set(x: Float, y: Float, z: Float, w: Float): Quaternionf {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(q: Quaternionf): Quaternionf {
        x = q.x
        y = q.y
        z = q.z
        w = q.w
        return this
    }

    fun set(q: Quaterniond): Quaternionf {
        x = q.x.toFloat()
        y = q.y.toFloat()
        z = q.z.toFloat()
        w = q.w.toFloat()
        return this
    }

    fun set(axisAngle: AxisAngle4f): Quaternionf {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun set(axisAngle: AxisAngle4d): Quaternionf {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun setAngleAxis(angle: Float, x: Float, y: Float, z: Float): Quaternionf {
        val s = Math.sin(angle * 0.5f)
        this.x = x * s
        this.y = y * s
        this.z = z * s
        w = Math.cosFromSin(s, angle * 0.5f)
        return this
    }

    fun setAngleAxis(angle: Double, x: Double, y: Double, z: Double): Quaternionf {
        val s = Math.sin(angle * 0.5)
        this.x = (x * s).toFloat()
        this.y = (y * s).toFloat()
        this.z = (z * s).toFloat()
        w = Math.cosFromSin(s, angle * 0.5).toFloat()
        return this
    }

    fun rotationAxis(axisAngle: AxisAngle4f): Quaternionf {
        return this.rotationAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotationAxis(angle: Float, axisX: Float, axisY: Float, axisZ: Float): Quaternionf {
        val halfAngle = angle / 2f
        val sinAngle = Math.sin(halfAngle)
        val invVLength = Math.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        return this.set(
            axisX * invVLength * sinAngle,
            axisY * invVLength * sinAngle,
            axisZ * invVLength * sinAngle,
            Math.cosFromSin(sinAngle, halfAngle)
        )
    }

    fun rotationAxis(angle: Float, axis: Vector3f): Quaternionf {
        return this.rotationAxis(angle, axis.x, axis.y, axis.z)
    }

    fun rotationX(angle: Float): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return this.set(sin, 0f, 0f, cos)
    }

    fun rotationY(angle: Float): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return this.set(0f, sin, 0f, cos)
    }

    fun rotationZ(angle: Float): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return this.set(0f, 0f, sin, cos)
    }

    private fun setFromUnnormalized(
        m00: Float,
        m01: Float,
        m02: Float,
        m10: Float,
        m11: Float,
        m12: Float,
        m20: Float,
        m21: Float,
        m22: Float
    ) {
        val lenX = Math.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val lenY = Math.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val lenZ = Math.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        val nm00 = m00 * lenX
        val nm01 = m01 * lenX
        val nm02 = m02 * lenX
        val nm10 = m10 * lenY
        val nm11 = m11 * lenY
        val nm12 = m12 * lenY
        val nm20 = m20 * lenZ
        val nm21 = m21 * lenZ
        val nm22 = m22 * lenZ
        this.setFromNormalized(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    private fun setFromNormalized(
        m00: Float,
        m01: Float,
        m02: Float,
        m10: Float,
        m11: Float,
        m12: Float,
        m20: Float,
        m21: Float,
        m22: Float
    ) {
        val tr = m00 + m11 + m22
        var t: Float
        if (tr >= 0f) {
            t = Math.sqrt(tr + 1f)
            w = t * 0.5f
            t = 0.5f / t
            x = (m12 - m21) * t
            y = (m20 - m02) * t
            z = (m01 - m10) * t
        } else if (m00 >= m11 && m00 >= m22) {
            t = Math.sqrt(m00 - (m11 + m22) + 1f)
            x = t * 0.5f
            t = 0.5f / t
            y = (m10 + m01) * t
            z = (m02 + m20) * t
            w = (m12 - m21) * t
        } else if (m11 > m22) {
            t = Math.sqrt(m11 - (m22 + m00) + 1f)
            y = t * 0.5f
            t = 0.5f / t
            z = (m21 + m12) * t
            x = (m10 + m01) * t
            w = (m20 - m02) * t
        } else {
            t = Math.sqrt(m22 - (m00 + m11) + 1f)
            z = t * 0.5f
            t = 0.5f / t
            x = (m02 + m20) * t
            y = (m21 + m12) * t
            w = (m01 - m10) * t
        }
    }

    private fun setFromUnnormalized(
        m00: Double,
        m01: Double,
        m02: Double,
        m10: Double,
        m11: Double,
        m12: Double,
        m20: Double,
        m21: Double,
        m22: Double
    ) {
        val lenX = Math.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val lenY = Math.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val lenZ = Math.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        val nm00 = m00 * lenX
        val nm01 = m01 * lenX
        val nm02 = m02 * lenX
        val nm10 = m10 * lenY
        val nm11 = m11 * lenY
        val nm12 = m12 * lenY
        val nm20 = m20 * lenZ
        val nm21 = m21 * lenZ
        val nm22 = m22 * lenZ
        this.setFromNormalized(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    private fun setFromNormalized(
        m00: Double,
        m01: Double,
        m02: Double,
        m10: Double,
        m11: Double,
        m12: Double,
        m20: Double,
        m21: Double,
        m22: Double
    ) {
        val tr = m00 + m11 + m22
        var t: Double
        if (tr >= 0.0) {
            t = Math.sqrt(tr + 1.0)
            w = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((m12 - m21) * t).toFloat()
            y = ((m20 - m02) * t).toFloat()
            z = ((m01 - m10) * t).toFloat()
        } else if (m00 >= m11 && m00 >= m22) {
            t = Math.sqrt(m00 - (m11 + m22) + 1.0)
            x = (t * 0.5).toFloat()
            t = 0.5 / t
            y = ((m10 + m01) * t).toFloat()
            z = ((m02 + m20) * t).toFloat()
            w = ((m12 - m21) * t).toFloat()
        } else if (m11 > m22) {
            t = Math.sqrt(m11 - (m22 + m00) + 1.0)
            y = (t * 0.5).toFloat()
            t = 0.5 / t
            z = ((m21 + m12) * t).toFloat()
            x = ((m10 + m01) * t).toFloat()
            w = ((m20 - m02) * t).toFloat()
        } else {
            t = Math.sqrt(m22 - (m00 + m11) + 1.0)
            z = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((m02 + m20) * t).toFloat()
            y = ((m21 + m12) * t).toFloat()
            w = ((m01 - m10) * t).toFloat()
        }
    }

    fun setFromUnnormalized(mat: Matrix4f): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix4x3f): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix4x3d): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4f): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4x3f): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4x3d): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix4d): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4d): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix3f): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix3f): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix3d): Quaternionf {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix3d): Quaternionf {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun fromAxisAngleRad(axis: Vector3f, angle: Float): Quaternionf {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, angle)
    }

    fun fromAxisAngleRad(axisX: Float, axisY: Float, axisZ: Float, angle: Float): Quaternionf {
        val halfAngle = angle / 2f
        val sinAngle = Math.sin(halfAngle)
        val vLength = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        x = axisX / vLength * sinAngle
        y = axisY / vLength * sinAngle
        z = axisZ / vLength * sinAngle
        w = Math.cosFromSin(sinAngle, halfAngle)
        return this
    }

    fun fromAxisAngleDeg(axis: Vector3f, angle: Float): Quaternionf {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, Math.toRadians(angle))
    }

    fun fromAxisAngleDeg(axisX: Float, axisY: Float, axisZ: Float, angle: Float): Quaternionf {
        return this.fromAxisAngleRad(axisX, axisY, axisZ, Math.toRadians(angle))
    }

    @JvmOverloads
    fun mul(q: Quaternionf, dest: Quaternionf = this): Quaternionf {
        return dest.set(
            Math.fma(w, q.x, Math.fma(x, q.w, Math.fma(y, q.z, -z * q.y))), Math.fma(
                w, q.y, Math.fma(-x, q.z, Math.fma(y, q.w, z * q.x))
            ), Math.fma(
                w, q.z, Math.fma(
                    x, q.y, Math.fma(-y, q.x, z * q.w)
                )
            ), Math.fma(w, q.w, Math.fma(-x, q.x, Math.fma(-y, q.y, -z * q.z)))
        )
    }

    @JvmOverloads
    fun mul(qx: Float, qy: Float, qz: Float, qw: Float, dest: Quaternionf = this): Quaternionf {
        return dest.set(
            Math.fma(w, qx, Math.fma(x, qw, Math.fma(y, qz, -z * qy))), Math.fma(
                w, qy, Math.fma(-x, qz, Math.fma(y, qw, z * qx))
            ), Math.fma(
                w, qz, Math.fma(
                    x, qy, Math.fma(-y, qx, z * qw)
                )
            ), Math.fma(w, qw, Math.fma(-x, qx, Math.fma(-y, qy, -z * qz)))
        )
    }

    @JvmOverloads
    fun premul(q: Quaternionf, dest: Quaternionf = this): Quaternionf {
        return dest.set(
            Math.fma(q.w, x, Math.fma(q.x, w, Math.fma(q.y, z, -q.z * y))),
            Math.fma(q.w, y, Math.fma(-q.x, z, Math.fma(q.y, w, q.z * x))),
            Math.fma(q.w, z, Math.fma(q.x, y, Math.fma(-q.y, x, q.z * w))),
            Math.fma(q.w, w, Math.fma(-q.x, x, Math.fma(-q.y, y, -q.z * z)))
        )
    }

    @JvmOverloads
    fun premul(qx: Float, qy: Float, qz: Float, qw: Float, dest: Quaternionf = this): Quaternionf {
        return dest.set(
            Math.fma(qw, x, Math.fma(qx, w, Math.fma(qy, z, -qz * y))),
            Math.fma(qw, y, Math.fma(-qx, z, Math.fma(qy, w, qz * x))),
            Math.fma(qw, z, Math.fma(qx, y, Math.fma(-qy, x, qz * w))),
            Math.fma(qw, w, Math.fma(-qx, x, Math.fma(-qy, y, -qz * z)))
        )
    }

    fun transform(vec: Vector3f): Vector3f {
        return this.transform(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverse(vec: Vector3f): Vector3f {
        return this.transformInverse(vec.x, vec.y, vec.z, vec)
    }

    fun transformPositiveX(dest: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dest.x = ww + xx - zz - yy
        dest.y = xy + zw + zw + xy
        dest.z = xz - yw + xz - yw
        return dest
    }

    fun transformPositiveX(dest: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dest.x = ww + xx - zz - yy
        dest.y = xy + zw + zw + xy
        dest.z = xz - yw + xz - yw
        return dest
    }

    fun transformUnitPositiveX(dest: Vector3f): Vector3f {
        val xy = x * y
        val xz = x * z
        val yy = y * y
        val yw = y * w
        val zz = z * z
        val zw = z * w
        dest.x = 1f - yy - zz - yy - zz
        dest.y = xy + zw + xy + zw
        dest.z = xz - yw + xz - yw
        return dest
    }

    fun transformUnitPositiveX(dest: Vector4f): Vector4f {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dest.x = 1f - yy - yy - zz - zz
        dest.y = xy + zw + xy + zw
        dest.z = xz - yw + xz - yw
        return dest
    }

    fun transformPositiveY(dest: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dest.x = -zw + xy - zw + xy
        dest.y = yy - zz + ww - xx
        dest.z = yz + yz + xw + xw
        return dest
    }

    fun transformPositiveY(dest: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dest.x = -zw + xy - zw + xy
        dest.y = yy - zz + ww - xx
        dest.z = yz + yz + xw + xw
        return dest
    }

    fun transformUnitPositiveY(dest: Vector4f): Vector4f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dest.x = xy - zw + xy - zw
        dest.y = 1f - xx - xx - zz - zz
        dest.z = yz + yz + xw + xw
        return dest
    }

    fun transformUnitPositiveY(dest: Vector3f): Vector3f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dest.x = xy - zw + xy - zw
        dest.y = 1f - xx - xx - zz - zz
        dest.z = yz + yz + xw + xw
        return dest
    }

    fun transformPositiveZ(dest: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dest.x = yw + xz + xz + yw
        dest.y = yz + yz - xw - xw
        dest.z = zz - yy - xx + ww
        return dest
    }

    fun transformPositiveZ(dest: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dest.x = yw + xz + xz + yw
        dest.y = yz + yz - xw - xw
        dest.z = zz - yy - xx + ww
        return dest
    }

    fun transformUnitPositiveZ(dest: Vector4f): Vector4f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dest.x = xz + yw + xz + yw
        dest.y = yz + yz - xw - xw
        dest.z = 1f - xx - xx - yy - yy
        return dest
    }

    fun transformUnitPositiveZ(dest: Vector3f): Vector3f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dest.x = xz + yw + xz + yw
        dest.y = yz + yz - xw - xw
        dest.z = 1f - xx - xx - yy - yy
        return dest
    }

    fun transform(vec: Vector3f, dest: Vector3f): Vector3f {
        return this.transform(vec.x, vec.y, vec.z, dest)
    }

    fun transformInverse(vec: Vector3f, dest: Vector3f): Vector3f {
        return this.transformInverse(vec.x, vec.y, vec.z, dest)
    }

    fun transform(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val xx = this.x * this.x
        val yy = this.y * this.y
        val zz = this.z * this.z
        val ww = w * w
        val xy = this.x * this.y
        val xz = this.x * this.z
        val yz = this.y * this.z
        val xw = this.x * w
        val zw = this.z * w
        val yw = this.y * w
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, 2f * (xz + yw) * k * z)),
            Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz - xw) * k * z)),
            Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transformInverse(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, w * w)))
        val qx = this.x * n
        val qy = this.y * n
        val qz = this.z * n
        val qw = w * n
        val xx = qx * qx
        val yy = qy * qy
        val zz = qz * qz
        val ww = qw * qw
        val xy = qx * qy
        val xz = qx * qz
        val yz = qy * qz
        val xw = qx * qw
        val zw = qz * qw
        val yw = qy * qw
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, 2f * (xz - yw) * k * z)),
            Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz + xw) * k * z)),
            Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transformUnit(vec: Vector3f): Vector3f {
        return this.transformUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverseUnit(vec: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformUnit(vec: Vector3f, dest: Vector3f): Vector3f {
        return this.transformUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformInverseUnit(vec: Vector3f, dest: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, 2f * (xz + yw) * z)), Math.fma(
                2f * (xy + zw), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f), y, 2f * (yz - xw) * z
                )
            ), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, Math.fma(-2f, xx + yy, 1f) * z))
        )
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, 2f * (xz - yw) * z)), Math.fma(
                2f * (xy - zw), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f), y, 2f * (yz + xw) * z
                )
            ), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, Math.fma(-2f, xx + yy, 1f) * z))
        )
    }

    @JvmOverloads
    fun transform(vec: Vector4f, dest: Vector4f = vec): Vector4f {
        return this.transform(vec.x, vec.y, vec.z, dest)
    }

    @JvmOverloads
    fun transformInverse(vec: Vector4f, dest: Vector4f = vec): Vector4f {
        return this.transformInverse(vec.x, vec.y, vec.z, dest)
    }

    fun transform(x: Float, y: Float, z: Float, dest: Vector4f): Vector4f {
        val xx = this.x * this.x
        val yy = this.y * this.y
        val zz = this.z * this.z
        val ww = w * w
        val xy = this.x * this.y
        val xz = this.x * this.z
        val yz = this.y * this.z
        val xw = this.x * w
        val zw = this.z * w
        val yw = this.y * w
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, 2f * (xz + yw) * k * z)),
            Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz - xw) * k * z)),
            Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transformInverse(x: Float, y: Float, z: Float, dest: Vector4f): Vector4f {
        val n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, w * w)))
        val qx = this.x * n
        val qy = this.y * n
        val qz = this.z * n
        val qw = w * n
        val xx = qx * qx
        val yy = qy * qy
        val zz = qz * qz
        val ww = qw * qw
        val xy = qx * qy
        val xz = qx * qz
        val yz = qy * qz
        val xw = qx * qw
        val zw = qz * qw
        val yw = qy * qw
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, 2f * (xz - yw) * k * z)),
            Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz + xw) * k * z)),
            Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transform(vec: Vector3d): Vector3d {
        return this.transform(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverse(vec: Vector3d): Vector3d {
        return this.transformInverse(vec.x, vec.y, vec.z, vec)
    }

    fun transformUnit(vec: Vector4f): Vector4f {
        return this.transformUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverseUnit(vec: Vector4f): Vector4f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformUnit(vec: Vector4f, dest: Vector4f): Vector4f {
        return this.transformUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformInverseUnit(vec: Vector4f, dest: Vector4f): Vector4f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dest: Vector4f): Vector4f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, 2f * (xz + yw) * z)), Math.fma(
                2f * (xy + zw), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f), y, 2f * (yz - xw) * z
                )
            ), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, Math.fma(-2f, xx + yy, 1f) * z))
        )
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dest: Vector4f): Vector4f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, 2f * (xz - yw) * z)), Math.fma(
                2f * (xy - zw), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f), y, 2f * (yz + xw) * z
                )
            ), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, Math.fma(-2f, xx + yy, 1f) * z))
        )
    }

    fun transformPositiveX(dest: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dest.x = (ww + xx - zz - yy).toDouble()
        dest.y = (xy + zw + zw + xy).toDouble()
        dest.z = (xz - yw + xz - yw).toDouble()
        return dest
    }

    fun transformPositiveX(dest: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dest.x = (ww + xx - zz - yy).toDouble()
        dest.y = (xy + zw + zw + xy).toDouble()
        dest.z = (xz - yw + xz - yw).toDouble()
        return dest
    }

    fun transformUnitPositiveX(dest: Vector3d): Vector3d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dest.x = (1f - yy - yy - zz - zz).toDouble()
        dest.y = (xy + zw + xy + zw).toDouble()
        dest.z = (xz - yw + xz - yw).toDouble()
        return dest
    }

    fun transformUnitPositiveX(dest: Vector4d): Vector4d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dest.x = (1f - yy - yy - zz - zz).toDouble()
        dest.y = (xy + zw + xy + zw).toDouble()
        dest.z = (xz - yw + xz - yw).toDouble()
        return dest
    }

    fun transformPositiveY(dest: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dest.x = (-zw + xy - zw + xy).toDouble()
        dest.y = (yy - zz + ww - xx).toDouble()
        dest.z = (yz + yz + xw + xw).toDouble()
        return dest
    }

    fun transformPositiveY(dest: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dest.x = (-zw + xy - zw + xy).toDouble()
        dest.y = (yy - zz + ww - xx).toDouble()
        dest.z = (yz + yz + xw + xw).toDouble()
        return dest
    }

    fun transformUnitPositiveY(dest: Vector4d): Vector4d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dest.x = (xy - zw + xy - zw).toDouble()
        dest.y = (1f - xx - xx - zz - zz).toDouble()
        dest.z = (yz + yz + xw + xw).toDouble()
        return dest
    }

    fun transformUnitPositiveY(dest: Vector3d): Vector3d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dest.x = (xy - zw + xy - zw).toDouble()
        dest.y = (1f - xx - xx - zz - zz).toDouble()
        dest.z = (yz + yz + xw + xw).toDouble()
        return dest
    }

    fun transformPositiveZ(dest: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dest.x = (yw + xz + xz + yw).toDouble()
        dest.y = (yz + yz - xw - xw).toDouble()
        dest.z = (zz - yy - xx + ww).toDouble()
        return dest
    }

    fun transformPositiveZ(dest: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dest.x = (yw + xz + xz + yw).toDouble()
        dest.y = (yz + yz - xw - xw).toDouble()
        dest.z = (zz - yy - xx + ww).toDouble()
        return dest
    }

    fun transformUnitPositiveZ(dest: Vector4d): Vector4d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dest.x = (xz + yw + xz + yw).toDouble()
        dest.y = (yz + yz - xw - xw).toDouble()
        dest.z = (1f - xx - xx - yy - yy).toDouble()
        return dest
    }

    fun transformUnitPositiveZ(dest: Vector3d): Vector3d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dest.x = (xz + yw + xz + yw).toDouble()
        dest.y = (yz + yz - xw - xw).toDouble()
        dest.z = (1f - xx - xx - yy - yy).toDouble()
        return dest
    }

    fun transform(vec: Vector3d, dest: Vector3d): Vector3d {
        return this.transform(vec.x, vec.y, vec.z, dest)
    }

    fun transformInverse(vec: Vector3d, dest: Vector3d): Vector3d {
        return this.transformInverse(vec.x, vec.y, vec.z, dest)
    }

    fun transform(x: Float, y: Float, z: Float, dest: Vector3d): Vector3d {
        return this.transform(x.toDouble(), y.toDouble(), z.toDouble(), dest)
    }

    fun transformInverse(x: Float, y: Float, z: Float, dest: Vector3d): Vector3d {
        return this.transformInverse(x.toDouble(), y.toDouble(), z.toDouble(), dest)
    }

    fun transform(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val xx = this.x * this.x
        val yy = this.y * this.y
        val zz = this.z * this.z
        val ww = w * w
        val xy = this.x * this.y
        val xz = this.x * this.z
        val yz = this.y * this.z
        val xw = this.x * w
        val zw = this.z * w
        val yw = this.y * w
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma(
                ((xx - yy - zz + ww) * k).toDouble(),
                x,
                Math.fma((2f * (xy - zw) * k).toDouble(), y, (2f * (xz + yw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy + zw) * k).toDouble(),
                x,
                Math.fma(((yy - xx - zz + ww) * k).toDouble(), y, (2f * (yz - xw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xz - yw) * k).toDouble(),
                x,
                Math.fma((2f * (yz + xw) * k).toDouble(), y, ((zz - xx - yy + ww) * k).toDouble() * z)
            )
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, w * w)))
        val qx = this.x * n
        val qy = this.y * n
        val qz = this.z * n
        val qw = w * n
        val xx = qx * qx
        val yy = qy * qy
        val zz = qz * qz
        val ww = qw * qw
        val xy = qx * qy
        val xz = qx * qz
        val yz = qy * qz
        val xw = qx * qw
        val zw = qz * qw
        val yw = qy * qw
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma(
                ((xx - yy - zz + ww) * k).toDouble(),
                x,
                Math.fma((2f * (xy + zw) * k).toDouble(), y, (2f * (xz - yw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy - zw) * k).toDouble(),
                x,
                Math.fma(((yy - xx - zz + ww) * k).toDouble(), y, (2f * (yz + xw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xz + yw) * k).toDouble(),
                x,
                Math.fma((2f * (yz - xw) * k).toDouble(), y, ((zz - xx - yy + ww) * k).toDouble() * z)
            )
        )
    }

    @JvmOverloads
    fun transform(vec: Vector4d, dest: Vector4d = vec): Vector4d {
        return this.transform(vec.x, vec.y, vec.z, dest)
    }

    @JvmOverloads
    fun transformInverse(vec: Vector4d, dest: Vector4d = vec): Vector4d {
        return this.transformInverse(vec.x, vec.y, vec.z, dest)
    }

    fun transform(x: Double, y: Double, z: Double, dest: Vector4d): Vector4d {
        val xx = this.x * this.x
        val yy = this.y * this.y
        val zz = this.z * this.z
        val ww = w * w
        val xy = this.x * this.y
        val xz = this.x * this.z
        val yz = this.y * this.z
        val xw = this.x * w
        val zw = this.z * w
        val yw = this.y * w
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma(
                ((xx - yy - zz + ww) * k).toDouble(),
                x,
                Math.fma((2f * (xy - zw) * k).toDouble(), y, (2f * (xz + yw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy + zw) * k).toDouble(),
                x,
                Math.fma(((yy - xx - zz + ww) * k).toDouble(), y, (2f * (yz - xw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xz - yw) * k).toDouble(),
                x,
                Math.fma((2f * (yz + xw) * k).toDouble(), y, ((zz - xx - yy + ww) * k).toDouble() * z)
            )
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dest: Vector4d): Vector4d {
        val n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, w * w)))
        val qx = this.x * n
        val qy = this.y * n
        val qz = this.z * n
        val qw = w * n
        val xx = qx * qx
        val yy = qy * qy
        val zz = qz * qz
        val ww = qw * qw
        val xy = qx * qy
        val xz = qx * qz
        val yz = qy * qz
        val xw = qx * qw
        val zw = qz * qw
        val yw = qy * qw
        val k = 1f / (xx + yy + zz + ww)
        return dest.set(
            Math.fma(
                ((xx - yy - zz + ww) * k).toDouble(),
                x,
                Math.fma((2f * (xy + zw) * k).toDouble(), y, (2f * (xz - yw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy - zw) * k).toDouble(),
                x,
                Math.fma(((yy - xx - zz + ww) * k).toDouble(), y, (2f * (yz + xw) * k).toDouble() * z)
            ),
            Math.fma(
                (2f * (xz + yw) * k).toDouble(),
                x,
                Math.fma((2f * (yz - xw) * k).toDouble(), y, ((zz - xx - yy + ww) * k).toDouble() * z)
            )
        )
    }

    fun transformUnit(vec: Vector3d, dest: Vector3d): Vector3d {
        return this.transformUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformInverseUnit(vec: Vector3d, dest: Vector3d): Vector3d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dest: Vector3d): Vector3d {
        return this.transformUnit(x.toDouble(), y.toDouble(), z.toDouble(), dest)
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dest: Vector3d): Vector3d {
        return this.transformInverseUnit(x.toDouble(), y.toDouble(), z.toDouble(), dest)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(
                Math.fma(-2f, yy + zz, 1f).toDouble(),
                x,
                Math.fma((2f * (xy - zw)).toDouble(), y, (2f * (xz + yw)).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy + zw)).toDouble(), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f).toDouble(), y, (2f * (yz - xw)).toDouble() * z
                )
            ),
            Math.fma(
                (2f * (xz - yw)).toDouble(),
                x,
                Math.fma((2f * (yz + xw)).toDouble(), y, Math.fma(-2f, xx + yy, 1f).toDouble() * z)
            )
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(
                Math.fma(-2f, yy + zz, 1f).toDouble(),
                x,
                Math.fma((2f * (xy + zw)).toDouble(), y, (2f * (xz - yw)).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy - zw)).toDouble(), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f).toDouble(), y, (2f * (yz + xw)).toDouble() * z
                )
            ),
            Math.fma(
                (2f * (xz + yw)).toDouble(),
                x,
                Math.fma((2f * (yz - xw)).toDouble(), y, Math.fma(-2f, xx + yy, 1f).toDouble() * z)
            )
        )
    }

    @JvmOverloads
    fun transformUnit(vec: Vector4d, dest: Vector4d = vec): Vector4d {
        return this.transformUnit(vec.x, vec.y, vec.z, dest)
    }

    @JvmOverloads
    fun transformInverseUnit(vec: Vector4d, dest: Vector4d = vec): Vector4d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dest: Vector4d): Vector4d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(
                Math.fma(-2f, yy + zz, 1f).toDouble(),
                x,
                Math.fma((2f * (xy - zw)).toDouble(), y, (2f * (xz + yw)).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy + zw)).toDouble(), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f).toDouble(), y, (2f * (yz - xw)).toDouble() * z
                )
            ),
            Math.fma(
                (2f * (xz - yw)).toDouble(),
                x,
                Math.fma((2f * (yz + xw)).toDouble(), y, Math.fma(-2f, xx + yy, 1f).toDouble() * z)
            )
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dest: Vector4d): Vector4d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dest.set(
            Math.fma(
                Math.fma(-2f, yy + zz, 1f).toDouble(),
                x,
                Math.fma((2f * (xy + zw)).toDouble(), y, (2f * (xz - yw)).toDouble() * z)
            ),
            Math.fma(
                (2f * (xy - zw)).toDouble(), x, Math.fma(
                    Math.fma(-2f, xx + zz, 1f).toDouble(), y, (2f * (yz + xw)).toDouble() * z
                )
            ),
            Math.fma(
                (2f * (xz + yw)).toDouble(),
                x,
                Math.fma((2f * (yz - xw)).toDouble(), y, Math.fma(-2f, xx + yy, 1f).toDouble() * z)
            )
        )
    }

    @JvmOverloads
    fun invert(dest: Quaternionf = this): Quaternionf {
        val invNorm = 1f / Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
        dest.x = -x * invNorm
        dest.y = -y * invNorm
        dest.z = -z * invNorm
        dest.w = w * invNorm
        return dest
    }

    @JvmOverloads
    fun div(b: Quaternionf, dest: Quaternionf = this): Quaternionf {
        val invNorm = 1f / Math.fma(b.x, b.x, Math.fma(b.y, b.y, Math.fma(b.z, b.z, b.w * b.w)))
        val x = -b.x * invNorm
        val y = -b.y * invNorm
        val z = -b.z * invNorm
        val w = b.w * invNorm
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    @JvmOverloads
    fun conjugate(dest: Quaternionf = this): Quaternionf {
        dest.x = -x
        dest.y = -y
        dest.z = -z
        dest.w = w
        return dest
    }

    fun identity(): Quaternionf {
        x = 0f
        y = 0f
        z = 0f
        w = 1f
        return this
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dest: Quaternionf = this): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        val w = cx * cycz - sx * sysz
        val x = sx * cycz + cx * sysz
        val y = cx * sycz - sx * cysz
        val z = cx * cysz + sx * sycz
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dest: Quaternionf = this): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        val w = cx * cycz + sx * sysz
        val x = sx * cycz - cx * sysz
        val y = cx * sycz + sx * cysz
        val z = cx * cysz - sx * sycz
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dest: Quaternionf = this): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val yx = cy * sx
        val yy = sy * cx
        val yz = sy * sx
        val yw = cy * cx
        val x = yx * cz + yy * sz
        val y = yy * cz - yx * sz
        val z = yw * sz - yz * cz
        val w = yw * cz + yz * sz
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    fun getEulerAnglesXYZ(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = Math.atan2(x * w - y * z, 0.5f - x * x - y * y)
        eulerAngles.y = Math.safeAsin(2f * (x * z + y * w))
        eulerAngles.z = Math.atan2(z * w - x * y, 0.5f - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZYX(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = Math.atan2(y * z + w * x, 0.5f - x * x + y * y)
        eulerAngles.y = Math.safeAsin(-2f * (x * z - w * y))
        eulerAngles.z = Math.atan2(x * y + w * z, 0.5f - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZXY(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = Math.safeAsin(2f * (w * x + y * z))
        eulerAngles.y = Math.atan2(w * y - x * z, 0.5f - y * y - x * x)
        eulerAngles.z = Math.atan2(w * z - x * y, 0.5f - z * z - x * x)
        return eulerAngles
    }

    fun getEulerAnglesYXZ(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = Math.safeAsin(-2f * (y * z - w * x))
        eulerAngles.y = Math.atan2(x * z + y * w, 0.5f - y * y - x * x)
        eulerAngles.z = Math.atan2(y * x + w * z, 0.5f - x * x - z * z)
        return eulerAngles
    }

    fun lengthSquared(): Float {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))
    }

    fun rotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        w = cx * cycz - sx * sysz
        x = sx * cycz + cx * sysz
        y = cx * sycz - sx * cysz
        z = cx * cysz + sx * sycz
        return this
    }

    fun rotationZYX(angleZ: Float, angleY: Float, angleX: Float): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        w = cx * cycz + sx * sysz
        x = sx * cycz - cx * sysz
        y = cx * sycz + sx * cysz
        z = cx * cysz - sx * sycz
        return this
    }

    fun rotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Quaternionf {
        val sx = Math.sin(angleX * 0.5f)
        val cx = Math.cosFromSin(sx, angleX * 0.5f)
        val sy = Math.sin(angleY * 0.5f)
        val cy = Math.cosFromSin(sy, angleY * 0.5f)
        val sz = Math.sin(angleZ * 0.5f)
        val cz = Math.cosFromSin(sz, angleZ * 0.5f)
        val x = cy * sx
        val y = sy * cx
        val z = sy * sx
        val w = cy * cx
        this.x = x * cz + y * sz
        this.y = y * cz - x * sz
        this.z = w * sz - z * cz
        this.w = w * cz + z * sz
        return this
    }

    @JvmOverloads
    fun slerp(target: Quaternionf, alpha: Float, dest: Quaternionf = this): Quaternionf {
        val cosom = Math.fma(x, target.x, Math.fma(y, target.y, Math.fma(z, target.z, w * target.w)))
        val absCosom = Math.abs(cosom)
        val scale0: Float
        var scale1: Float
        if (1f - absCosom > 1.0E-6f) {
            val sinSqr = 1f - absCosom * absCosom
            val sinom = Math.invsqrt(sinSqr)
            val omega = Math.atan2(sinSqr * sinom, absCosom)
            scale0 = (Math.sin((1.0 - alpha.toDouble()) * omega.toDouble()) * sinom.toDouble()).toFloat()
            scale1 = Math.sin(alpha * omega) * sinom
        } else {
            scale0 = 1f - alpha
            scale1 = alpha
        }
        scale1 = if (cosom >= 0f) scale1 else -scale1
        dest.x = Math.fma(scale0, x, scale1 * target.x)
        dest.y = Math.fma(scale0, y, scale1 * target.y)
        dest.z = Math.fma(scale0, z, scale1 * target.z)
        dest.w = Math.fma(scale0, w, scale1 * target.w)
        return dest
    }

    @JvmOverloads
    fun scale(factor: Float, dest: Quaternionf = this): Quaternionf {
        val sqrt = Math.sqrt(factor)
        dest.x = sqrt * x
        dest.y = sqrt * y
        dest.z = sqrt * z
        dest.w = sqrt * w
        return dest
    }

    fun scaling(factor: Float): Quaternionf {
        val sqrt = Math.sqrt(factor)
        x = 0f
        y = 0f
        z = 0f
        w = sqrt
        return this
    }

    @JvmOverloads
    fun integrate(dt: Float, vx: Float, vy: Float, vz: Float, dest: Quaternionf = this): Quaternionf {
        val thetaX = dt * vx * 0.5f
        val thetaY = dt * vy * 0.5f
        val thetaZ = dt * vz * 0.5f
        val thetaMagSq = thetaX * thetaX + thetaY * thetaY + thetaZ * thetaZ
        val s: Float
        val dqW: Float
        if (thetaMagSq * thetaMagSq / 24f < 1.0E-8f) {
            dqW = 1f - thetaMagSq * 0.5f
            s = 1f - thetaMagSq / 6f
        } else {
            val thetaMag = Math.sqrt(thetaMagSq)
            val sin = Math.sin(thetaMag)
            s = sin / thetaMag
            dqW = Math.cosFromSin(sin, thetaMag)
        }
        val dqX = thetaX * s
        val dqY = thetaY * s
        val dqZ = thetaZ * s
        return dest.set(
            Math.fma(dqW, x, Math.fma(dqX, w, Math.fma(dqY, z, -dqZ * y))),
            Math.fma(dqW, y, Math.fma(-dqX, z, Math.fma(dqY, w, dqZ * x))),
            Math.fma(dqW, z, Math.fma(dqX, y, Math.fma(-dqY, x, dqZ * w))),
            Math.fma(dqW, w, Math.fma(-dqX, x, Math.fma(-dqY, y, -dqZ * z)))
        )
    }

    @JvmOverloads
    fun nlerp(q: Quaternionf, factor: Float, dest: Quaternionf = this): Quaternionf {
        val cosom = Math.fma(x, q.x, Math.fma(y, q.y, Math.fma(z, q.z, w * q.w)))
        val scale0 = 1f - factor
        val scale1 = if (cosom >= 0f) factor else -factor
        dest.x = Math.fma(scale0, x, scale1 * q.x)
        dest.y = Math.fma(scale0, y, scale1 * q.y)
        dest.z = Math.fma(scale0, z, scale1 * q.z)
        dest.w = Math.fma(scale0, w, scale1 * q.w)
        val s =
            Math.invsqrt(Math.fma(dest.x, dest.x, Math.fma(dest.y, dest.y, Math.fma(dest.z, dest.z, dest.w * dest.w))))
        dest.x *= s
        dest.y *= s
        dest.z *= s
        dest.w *= s
        return dest
    }

    @JvmOverloads
    fun nlerpIterative(q: Quaternionf, alpha: Float, dotThreshold: Float, dest: Quaternionf = this): Quaternionf {
        var q1x = x
        var q1y = y
        var q1z = z
        var q1w = w
        var q2x = q.x
        var q2y = q.y
        var q2z = q.z
        var q2w = q.w
        var dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)))
        var absDot = Math.abs(dot)
        return if (0.999999f < absDot) {
            dest.set(this)
        } else {
            var alphaN: Float
            var scale0: Float
            var scale1: Float
            var s: Float
            alphaN = alpha
            while (absDot < dotThreshold) {
                scale0 = 0.5f
                scale1 = if (dot >= 0f) 0.5f else -0.5f
                if (alphaN < 0.5f) {
                    q2x = Math.fma(scale0, q2x, scale1 * q1x)
                    q2y = Math.fma(scale0, q2y, scale1 * q1y)
                    q2z = Math.fma(scale0, q2z, scale1 * q1z)
                    q2w = Math.fma(scale0, q2w, scale1 * q1w)
                    s = Math.invsqrt(
                        Math.fma(
                            q2x,
                            q2x,
                            Math.fma(q2y, q2y, Math.fma(q2z, q2z, q2w * q2w))
                        )
                    )
                    q2x *= s
                    q2y *= s
                    q2z *= s
                    q2w *= s
                    alphaN += alphaN
                } else {
                    q1x = Math.fma(scale0, q1x, scale1 * q2x)
                    q1y = Math.fma(scale0, q1y, scale1 * q2y)
                    q1z = Math.fma(scale0, q1z, scale1 * q2z)
                    q1w = Math.fma(scale0, q1w, scale1 * q2w)
                    s = Math.invsqrt(
                        Math.fma(
                            q1x,
                            q1x,
                            Math.fma(q1y, q1y, Math.fma(q1z, q1z, q1w * q1w))
                        )
                    )
                    q1x *= s
                    q1y *= s
                    q1z *= s
                    q1w *= s
                    alphaN = alphaN + alphaN - 1f
                }
                dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)))
                absDot = Math.abs(dot)
            }
            scale0 = 1f - alphaN
            scale1 = if (dot >= 0f) alphaN else -alphaN
            s = Math.fma(scale0, q1x, scale1 * q2x)
            val resY = Math.fma(scale0, q1y, scale1 * q2y)
            val resZ = Math.fma(scale0, q1z, scale1 * q2z)
            val resW = Math.fma(scale0, q1w, scale1 * q2w)
            s = Math.invsqrt(
                Math.fma(
                    s,
                    s,
                    Math.fma(resY, resY, Math.fma(resZ, resZ, resW * resW))
                )
            )
            dest.x = s * s
            dest.y = resY * s
            dest.z = resZ * s
            dest.w = resW * s
            dest
        }
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Quaternionf {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dest: Quaternionf): Quaternionf {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dest: Quaternionf = this
    ): Quaternionf {
        val invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val dirnX = -dirX * invDirLength
        val dirnY = -dirY * invDirLength
        val dirnZ = -dirZ * invDirLength
        var leftX = upY * dirnZ - upZ * dirnY
        var leftY = upZ * dirnX - upX * dirnZ
        var leftZ = upX * dirnY - upY * dirnX
        val invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirnY * leftZ - dirnZ * leftY
        val upnY = dirnZ * leftX - dirnX * leftZ
        val upnZ = dirnX * leftY - dirnY * leftX
        val tr = (leftX + upnY + dirnZ).toDouble()
        val x: Float
        val y: Float
        val z: Float
        val w: Float
        var t: Double
        if (tr >= 0.0) {
            t = Math.sqrt(tr + 1.0)
            w = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((dirnY - upnZ).toDouble() * t).toFloat()
            y = ((leftZ - dirnX).toDouble() * t).toFloat()
            z = ((upnX - leftY).toDouble() * t).toFloat()
        } else if (leftX > upnY && leftX > dirnZ) {
            t = Math.sqrt(1.0 + leftX.toDouble() - upnY.toDouble() - dirnZ.toDouble())
            x = (t * 0.5).toFloat()
            t = 0.5 / t
            y = ((leftY + upnX).toDouble() * t).toFloat()
            z = ((dirnX + leftZ).toDouble() * t).toFloat()
            w = ((dirnY - upnZ).toDouble() * t).toFloat()
        } else if (upnY > dirnZ) {
            t = Math.sqrt(1.0 + upnY.toDouble() - leftX.toDouble() - dirnZ.toDouble())
            y = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((leftY + upnX).toDouble() * t).toFloat()
            z = ((upnZ + dirnY).toDouble() * t).toFloat()
            w = ((leftZ - dirnX).toDouble() * t).toFloat()
        } else {
            t = Math.sqrt(1.0 + dirnZ.toDouble() - leftX.toDouble() - upnY.toDouble())
            z = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((dirnX + leftZ).toDouble() * t).toFloat()
            y = ((upnZ + dirnY).toDouble() * t).toFloat()
            w = ((upnX - leftY).toDouble() * t).toFloat()
        }
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    fun rotationTo(
        fromDirX: Float,
        fromDirY: Float,
        fromDirZ: Float,
        toDirX: Float,
        toDirY: Float,
        toDirZ: Float
    ): Quaternionf {
        val fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)))
        val tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)))
        val fx = fromDirX * fn
        val fy = fromDirY * fn
        val fz = fromDirZ * fn
        val tx = toDirX * tn
        val ty = toDirY * tn
        val tz = toDirZ * tn
        val dot = fx * tx + fy * ty + fz * tz
        var x: Float
        var y: Float
        var z: Float
        val w: Float
        if (dot < -0.999999f) {
            x = fy
            y = -fx
            z = 0f
            if (fy * fy + y * y == 0f) {
                x = 0f
                y = fz
                z = -fy
            }
            this.x = x
            this.y = y
            this.z = z
            this.w = 0f
        } else {
            val sd2 = Math.sqrt((1f + dot) * 2f)
            val isd2 = 1f / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5f
            val n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
            this.x = x * n2
            this.y = y * n2
            this.z = z * n2
            this.w = w * n2
        }
        return this
    }

    fun rotationTo(fromDir: Vector3f, toDir: Vector3f): Quaternionf {
        return this.rotationTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z)
    }

    @JvmOverloads
    fun rotateTo(
        fromDirX: Float,
        fromDirY: Float,
        fromDirZ: Float,
        toDirX: Float,
        toDirY: Float,
        toDirZ: Float,
        dest: Quaternionf = this
    ): Quaternionf {
        val fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)))
        val tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)))
        val fx = fromDirX * fn
        val fy = fromDirY * fn
        val fz = fromDirZ * fn
        val tx = toDirX * tn
        val ty = toDirY * tn
        val tz = toDirZ * tn
        val dot = fx * tx + fy * ty + fz * tz
        var x: Float
        var y: Float
        var z: Float
        var w: Float
        if (dot < -0.999999f) {
            x = fy
            y = -fx
            z = 0f
            w = 0f
            if (fy * fy + y * y == 0f) {
                x = 0f
                y = fz
                z = -fy
                w = 0f
            }
        } else {
            val sd2 = Math.sqrt((1f + dot) * 2f)
            val isd2 = 1f / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5f
            val n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))))
            x *= n2
            y *= n2
            z *= n2
            w *= n2
        }
        return dest.set(
            Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))),
            Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))),
            Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))),
            Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z)))
        )
    }

    fun rotateTo(fromDir: Vector3f, toDir: Vector3f, dest: Quaternionf): Quaternionf {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, dest)
    }

    fun rotateTo(fromDir: Vector3f, toDir: Vector3f): Quaternionf {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, this)
    }

    @JvmOverloads
    fun rotateX(angle: Float, dest: Quaternionf = this): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return dest.set(w * sin + x * cos, y * cos + z * sin, z * cos - y * sin, w * cos - x * sin)
    }

    @JvmOverloads
    fun rotateY(angle: Float, dest: Quaternionf = this): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return dest.set(x * cos - z * sin, w * sin + y * cos, x * sin + z * cos, w * cos - y * sin)
    }

    @JvmOverloads
    fun rotateZ(angle: Float, dest: Quaternionf = this): Quaternionf {
        val sin = Math.sin(angle * 0.5f)
        val cos = Math.cosFromSin(sin, angle * 0.5f)
        return dest.set(x * cos + y * sin, y * cos - x * sin, w * sin + z * cos, w * cos - z * sin)
    }

    @JvmOverloads
    fun rotateLocalX(angle: Float, dest: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = Math.sin(halfAngle)
        val c = Math.cosFromSin(s, halfAngle)
        dest[c * x + s * w, c * y - s * z, c * z + s * y] = c * w - s * x
        return dest
    }

    @JvmOverloads
    fun rotateLocalY(angle: Float, dest: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = Math.sin(halfAngle)
        val c = Math.cosFromSin(s, halfAngle)
        dest[c * x + s * z, c * y + s * w, c * z - s * x] = c * w - s * y
        return dest
    }

    @JvmOverloads
    fun rotateLocalZ(angle: Float, dest: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = Math.sin(halfAngle)
        val c = Math.cosFromSin(s, halfAngle)
        dest[c * x - s * y, c * y + s * x, c * z + s * w] = c * w - s * z
        return dest
    }

    @JvmOverloads
    fun rotateAxis(angle: Float, axisX: Float, axisY: Float, axisZ: Float, dest: Quaternionf = this): Quaternionf {
        val halfAngle = angle / 2f
        val sinAngle = Math.sin(halfAngle)
        val invVLength = Math.invsqrt(Math.fma(axisX, axisX, Math.fma(axisY, axisY, axisZ * axisZ)))
        val rx = axisX * invVLength * sinAngle
        val ry = axisY * invVLength * sinAngle
        val rz = axisZ * invVLength * sinAngle
        val rw = Math.cosFromSin(sinAngle, halfAngle)
        return dest.set(
            Math.fma(w, rx, Math.fma(x, rw, Math.fma(y, rz, -z * ry))), Math.fma(
                w, ry, Math.fma(-x, rz, Math.fma(y, rw, z * rx))
            ), Math.fma(
                w, rz, Math.fma(
                    x, ry, Math.fma(-y, rx, z * rw)
                )
            ), Math.fma(w, rw, Math.fma(-x, rx, Math.fma(-y, ry, -z * rz)))
        )
    }

    fun rotateAxis(angle: Float, axis: Vector3f, dest: Quaternionf): Quaternionf {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, dest)
    }

    fun rotateAxis(angle: Float, axis: Vector3f): Quaternionf {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, this)
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
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
            val other = obj as Quaternionf
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

    @JvmOverloads
    fun difference(other: Quaternionf, dest: Quaternionf = this): Quaternionf {
        val invNorm = 1f / lengthSquared()
        val x = -x * invNorm
        val y = -y * invNorm
        val z = -z * invNorm
        val w = w * invNorm
        dest[Math.fma(w, other.x, Math.fma(x, other.w, Math.fma(y, other.z, -z * other.y))), Math.fma(
            w,
            other.y,
            Math.fma(-x, other.z, Math.fma(y, other.w, z * other.x))
        ), Math.fma(w, other.z, Math.fma(x, other.y, Math.fma(-y, other.x, z * other.w)))] =
            Math.fma(w, other.w, Math.fma(-x, other.x, Math.fma(-y, other.y, -z * other.z)))
        return dest
    }

    fun positiveX(dir: Vector3f): Vector3f {
        val invNorm = 1f / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dy = ny + ny
        val dz = nz + nz
        dir.x = -ny * dy - nz * dz + 1f
        dir.y = nx * dy + nw * dz
        dir.z = nx * dz - nw * dy
        return dir
    }

    fun normalizedPositiveX(dir: Vector3f): Vector3f {
        val dy = y + y
        val dz = z + z
        dir.x = -y * dy - z * dz + 1f
        dir.y = x * dy - w * dz
        dir.z = x * dz + w * dy
        return dir
    }

    fun positiveY(dir: Vector3f): Vector3f {
        val invNorm = 1f / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dx = nx + nx
        val dy = ny + ny
        val dz = nz + nz
        dir.x = nx * dy - nw * dz
        dir.y = -nx * dx - nz * dz + 1f
        dir.z = ny * dz + nw * dx
        return dir
    }

    fun normalizedPositiveY(dir: Vector3f): Vector3f {
        val dx = x + x
        val dy = y + y
        val dz = z + z
        dir.x = x * dy + w * dz
        dir.y = -x * dx - z * dz + 1f
        dir.z = y * dz - w * dx
        return dir
    }

    fun positiveZ(dir: Vector3f): Vector3f {
        val invNorm = 1f / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dx = nx + nx
        val dy = ny + ny
        val dz = nz + nz
        dir.x = nx * dz + nw * dy
        dir.y = ny * dz - nw * dx
        dir.z = -nx * dx - ny * dy + 1f
        return dir
    }

    fun normalizedPositiveZ(dir: Vector3f): Vector3f {
        val dx = x + x
        val dy = y + y
        val dz = z + z
        dir.x = x * dz - w * dy
        dir.y = y * dz + w * dx
        dir.z = -x * dx - y * dy + 1f
        return dir
    }

    @JvmOverloads
    fun conjugateBy(q: Quaternionf, dest: Quaternionf = this): Quaternionf {
        val invNorm = 1f / q.lengthSquared()
        val qix = -q.x * invNorm
        val qiy = -q.y * invNorm
        val qiz = -q.z * invNorm
        val qiw = q.w * invNorm
        val qpx = Math.fma(q.w, x, Math.fma(q.x, w, Math.fma(q.y, z, -q.z * y)))
        val qpy = Math.fma(q.w, y, Math.fma(-q.x, z, Math.fma(q.y, w, q.z * x)))
        val qpz = Math.fma(q.w, z, Math.fma(q.x, y, Math.fma(-q.y, x, q.z * w)))
        val qpw = Math.fma(q.w, w, Math.fma(-q.x, x, Math.fma(-q.y, y, -q.z * z)))
        return dest.set(
            Math.fma(qpw, qix, Math.fma(qpx, qiw, Math.fma(qpy, qiz, -qpz * qiy))),
            Math.fma(qpw, qiy, Math.fma(-qpx, qiz, Math.fma(qpy, qiw, qpz * qix))),
            Math.fma(qpw, qiz, Math.fma(qpx, qiy, Math.fma(-qpy, qix, qpz * qiw))),
            Math.fma(qpw, qiw, Math.fma(-qpx, qix, Math.fma(-qpy, qiy, -qpz * qiz)))
        )
    }

    val isFinite: Boolean
        get() = Math.isFinite(x) && Math.isFinite(y) && Math.isFinite(z) && Math.isFinite(w)

    fun equals(q: Quaternionf?, delta: Float): Boolean {
        return if (this === q) {
            true
        } else if (q == null) {
            false
        } else if (!Runtime.equals(x, q.x, delta)) {
            false
        } else if (!Runtime.equals(y, q.y, delta)) {
            false
        } else if (!Runtime.equals(z, q.z, delta)) {
            false
        } else {
            Runtime.equals(w, q.w, delta)
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

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }

    companion object {
        fun slerp(qs: Array<Quaternionf>, weights: FloatArray, dest: Quaternionf): Quaternionf {
            dest.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dest.slerp(qs[i], rw1)
            }
            return dest
        }

        fun nlerp(qs: Array<Quaternionf>, weights: FloatArray, dest: Quaternionf): Quaternionf {
            dest.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dest.nlerp(qs[i], rw1)
            }
            return dest
        }

        fun nlerpIterative(
            qs: Array<Quaternionf>,
            weights: FloatArray,
            dotThreshold: Float,
            dest: Quaternionf
        ): Quaternionf {
            dest.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dest.nlerpIterative(qs[i], rw1, dotThreshold)
            }
            return dest
        }
    }
}