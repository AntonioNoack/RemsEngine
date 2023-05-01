package org.joml

import kotlin.math.*

@Suppress("unused")
open class Quaterniond {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var w: Double

    constructor() {
        w = 1.0
    }

    constructor(x: Double, y: Double, z: Double, w: Double) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    constructor(source: Quaterniond) {
        x = source.x
        y = source.y
        z = source.z
        w = source.w
    }

    constructor(source: Quaternionf) {
        x = source.x.toDouble()
        y = source.y.toDouble()
        z = source.z.toDouble()
        w = source.w.toDouble()
    }

    constructor(axisAngle: AxisAngle4f) {
        val s = sin(axisAngle.angle.toDouble() * 0.5)
        x = axisAngle.x.toDouble() * s
        y = axisAngle.y.toDouble() * s
        z = axisAngle.z.toDouble() * s
        w = cos(axisAngle.angle.toDouble() * 0.5)
    }

    constructor(axisAngle: AxisAngle4d) {
        val s = sin(axisAngle.angle * 0.5)
        x = axisAngle.x * s
        y = axisAngle.y * s
        z = axisAngle.z * s
        w = cos(axisAngle.angle * 0.5)
    }

    fun normalize(): Quaterniond {
        val invNorm = JomlMath.invsqrt(lengthSquared())
        x *= invNorm
        y *= invNorm
        z *= invNorm
        w *= invNorm
        return this
    }

    fun normalize(dst: Quaterniond): Quaterniond {
        val invNorm = JomlMath.invsqrt(lengthSquared())
        dst.x = x * invNorm
        dst.y = y * invNorm
        dst.z = z * invNorm
        dst.w = w * invNorm
        return dst
    }

    @JvmOverloads
    fun add(x: Double, y: Double, z: Double, w: Double, dst: Quaterniond = this): Quaterniond {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    fun add(q2: Quaterniond): Quaterniond {
        x += q2.x
        y += q2.y
        z += q2.z
        w += q2.w
        return this
    }

    fun add(q2: Quaterniond, dst: Quaterniond): Quaterniond {
        dst.x = x + q2.x
        dst.y = y + q2.y
        dst.z = z + q2.z
        dst.w = w + q2.w
        return dst
    }

    fun dot(otherQuat: Quaterniond): Double {
        return x * otherQuat.x + y * otherQuat.y + z * otherQuat.z + w * otherQuat.w
    }

    fun angle(): Double {
        return 2.0 * JomlMath.safeAcos(w)
    }

    fun get(dst: Matrix3d): Matrix3d {
        return dst.set(this)
    }

    fun get(dst: Matrix3f): Matrix3f {
        return dst.set(this)
    }

    fun get(dst: Matrix4d): Matrix4d {
        return dst.set(this)
    }

    fun get(dst: Matrix4f): Matrix4f {
        return dst.set(this)
    }

    fun get(dst: AxisAngle4f): AxisAngle4f {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Double
        if (w > 1.0) {
            s = JomlMath.invsqrt(lengthSquared())
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dst.angle = (2.0 * acos(w)).toFloat()
        s = sqrt(1.0 - w * w)
        if (s < 0.001) {
            dst.x = x.toFloat()
            dst.y = y.toFloat()
            dst.z = z.toFloat()
        } else {
            s = 1.0 / s
            dst.x = (x * s).toFloat()
            dst.y = (y * s).toFloat()
            dst.z = (z * s).toFloat()
        }
        return dst
    }

    fun get(dst: AxisAngle4d): AxisAngle4d {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Double
        if (w > 1.0) {
            s = JomlMath.invsqrt(lengthSquared())
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dst.angle = 2.0 * acos(w)
        s = sqrt(1.0 - w * w)
        if (s < 0.001) {
            dst.x = x
            dst.y = y
            dst.z = z
        } else {
            s = 1.0 / s
            dst.x = x * s
            dst.y = y * s
            dst.z = z * s
        }
        return dst
    }

    fun get(dst: Quaterniond): Quaterniond {
        return dst.set(this)
    }

    fun get(dst: Quaternionf): Quaternionf {
        return dst.set(this)
    }

    fun set(x: Float, y: Float, z: Float, w: Float): Quaterniond =
        set(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())

    fun set(x: Double, y: Double, z: Double, w: Double): Quaterniond {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(q: Quaterniond): Quaterniond = set(q.x, q.y, q.z, q.w)
    fun set(q: Quaternionf): Quaterniond = set(q.x.toDouble(), q.y.toDouble(), q.z.toDouble(), q.w.toDouble())

    fun set(src: DoubleArray, i: Int) = set(src[i], src[i + 1], src[i + 2], src[i + 3])
    fun get(dst: DoubleArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
        dst[i + 3] = w
    }

    fun set(axisAngle: AxisAngle4f): Quaterniond {
        return this.setAngleAxis(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble()
        )
    }

    fun set(axisAngle: AxisAngle4d): Quaterniond {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun setAngleAxis(angle: Double, x: Double, y: Double, z: Double): Quaterniond {
        val s = sin(angle * 0.5)
        this.x = x * s
        this.y = y * s
        this.z = z * s
        w = cos(angle * 0.5)
        return this
    }

    fun setAngleAxis(angle: Double, axis: Vector3d): Quaterniond {
        return this.setAngleAxis(angle, axis.x, axis.y, axis.z)
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
        val lenX = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val lenY = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val lenZ = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
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
            t = sqrt(tr + 1.0)
            w = t * 0.5
            t = 0.5 / t
            x = (m12 - m21) * t
            y = (m20 - m02) * t
            z = (m01 - m10) * t
        } else if (m00 >= m11 && m00 >= m22) {
            t = sqrt(m00 - (m11 + m22) + 1.0)
            x = t * 0.5
            t = 0.5 / t
            y = (m10 + m01) * t
            z = (m02 + m20) * t
            w = (m12 - m21) * t
        } else if (m11 > m22) {
            t = sqrt(m11 - (m22 + m00) + 1.0)
            y = t * 0.5
            t = 0.5 / t
            z = (m21 + m12) * t
            x = (m10 + m01) * t
            w = (m20 - m02) * t
        } else {
            t = sqrt(m22 - (m00 + m11) + 1.0)
            z = t * 0.5
            t = 0.5 / t
            x = (m02 + m20) * t
            y = (m21 + m12) * t
            w = (m01 - m10) * t
        }
    }

    fun setFromUnnormalized(mat: Matrix4f): Quaterniond {
        this.setFromUnnormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromUnnormalized(mat: Matrix4x3f): Quaterniond {
        this.setFromUnnormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromUnnormalized(mat: Matrix4x3d): Quaterniond {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4f): Quaterniond {
        this.setFromNormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromNormalized(mat: Matrix4x3f): Quaterniond {
        this.setFromNormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromNormalized(mat: Matrix4x3d): Quaterniond {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix4d): Quaterniond {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix4d): Quaterniond {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromUnnormalized(mat: Matrix3f): Quaterniond {
        this.setFromUnnormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromNormalized(mat: Matrix3f): Quaterniond {
        this.setFromNormalized(
            mat.m00.toDouble(),
            mat.m01.toDouble(),
            mat.m02.toDouble(),
            mat.m10.toDouble(),
            mat.m11.toDouble(),
            mat.m12.toDouble(),
            mat.m20.toDouble(),
            mat.m21.toDouble(),
            mat.m22.toDouble()
        )
        return this
    }

    fun setFromUnnormalized(mat: Matrix3d): Quaterniond {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun setFromNormalized(mat: Matrix3d): Quaterniond {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22)
        return this
    }

    fun fromAxisAngleRad(axis: Vector3d, angle: Double): Quaterniond {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, angle)
    }

    fun fromAxisAngleRad(axisX: Double, axisY: Double, axisZ: Double, angle: Double): Quaterniond {
        val halfAngle = angle / 2.0
        val sinAngle = sin(halfAngle)
        val vLength = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        x = axisX / vLength * sinAngle
        y = axisY / vLength * sinAngle
        z = axisZ / vLength * sinAngle
        w = cos(halfAngle)
        return this
    }

    @JvmOverloads
    fun mul(q: Quaterniond, dst: Quaterniond = this): Quaterniond {
        return this.mul(q.x, q.y, q.z, q.w, dst)
    }

    @JvmOverloads
    fun mul(qx: Double, qy: Double, qz: Double, qw: Double, dst: Quaterniond = this): Quaterniond {
        return dst.set(
            JomlMath.fma(w, qx, JomlMath.fma(x, qw, JomlMath.fma(y, qz, -z * qy))), JomlMath.fma(
                w, qy, JomlMath.fma(-x, qz, JomlMath.fma(y, qw, z * qx))
            ), JomlMath.fma(
                w, qz, JomlMath.fma(
                    x, qy, JomlMath.fma(-y, qx, z * qw)
                )
            ), JomlMath.fma(w, qw, JomlMath.fma(-x, qx, JomlMath.fma(-y, qy, -z * qz)))
        )
    }

    @JvmOverloads
    fun premul(q: Quaterniond, dst: Quaterniond = this): Quaterniond {
        return this.premul(q.x, q.y, q.z, q.w, dst)
    }

    @JvmOverloads
    fun premul(qx: Double, qy: Double, qz: Double, qw: Double, dst: Quaterniond = this): Quaterniond {
        return dst.set(
            JomlMath.fma(qw, x, JomlMath.fma(qx, w, JomlMath.fma(qy, z, -qz * y))),
            JomlMath.fma(qw, y, JomlMath.fma(-qx, z, JomlMath.fma(qy, w, qz * x))),
            JomlMath.fma(qw, z, JomlMath.fma(qx, y, JomlMath.fma(-qy, x, qz * w))),
            JomlMath.fma(qw, w, JomlMath.fma(-qx, x, JomlMath.fma(-qy, y, -qz * z)))
        )
    }

    fun transform(vec: Vector3d): Vector3d {
        return this.transform(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverse(vec: Vector3d): Vector3d {
        return this.transformInverse(vec.x, vec.y, vec.z, vec)
    }

    fun transformUnit(vec: Vector3d): Vector3d {
        return this.transformUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverseUnit(vec: Vector3d): Vector3d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformPositiveX(dst: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dst.x = ww + xx - zz - yy
        dst.y = xy + zw + zw + xy
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformPositiveX(dst: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dst.x = ww + xx - zz - yy
        dst.y = xy + zw + zw + xy
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformUnitPositiveX(dst: Vector3d): Vector3d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = 1.0 - yy - yy - zz - zz
        dst.y = xy + zw + xy + zw
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformUnitPositiveX(dst: Vector4d): Vector4d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = 1.0 - yy - yy - zz - zz
        dst.y = xy + zw + xy + zw
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformPositiveY(dst: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dst.x = -zw + xy - zw + xy
        dst.y = yy - zz + ww - xx
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformPositiveY(dst: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dst.x = -zw + xy - zw + xy
        dst.y = yy - zz + ww - xx
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformUnitPositiveY(dst: Vector4d): Vector4d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = xy - zw + xy - zw
        dst.y = 1.0 - xx - xx - zz - zz
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformUnitPositiveY(dst: Vector3d): Vector3d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = xy - zw + xy - zw
        dst.y = 1.0 - xx - xx - zz - zz
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformPositiveZ(dst: Vector3d): Vector3d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dst.x = yw + xz + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = zz - yy - xx + ww
        return dst
    }

    fun transformPositiveZ(dst: Vector4d): Vector4d {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dst.x = yw + xz + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = zz - yy - xx + ww
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector4d): Vector4d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = xz + yw + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = 1.0 - xx - xx - yy - yy
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector3d): Vector3d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = xz + yw + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = 1.0 - xx - xx - yy - yy
        return dst
    }

    fun transform(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transform(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverse(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformInverse(vec.x, vec.y, vec.z, dst)
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)),
            JomlMath.fma(2.0 * (xy + zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)),
            JomlMath.fma(2.0 * (xz - yw) * k, x, JomlMath.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        val n = 1.0 / JomlMath.fma(this.x, this.x, JomlMath.fma(this.y, this.y, JomlMath.fma(this.z, this.z, w * w)))
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)),
            JomlMath.fma(2.0 * (xy - zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)),
            JomlMath.fma(2.0 * (xz + yw) * k, x, JomlMath.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    @JvmOverloads
    fun transform(vec: Vector4d, dst: Vector4d = vec): Vector4d {
        return this.transform(vec.x, vec.y, vec.z, dst)
    }

    @JvmOverloads
    fun transformInverse(vec: Vector4d, dst: Vector4d = vec): Vector4d {
        return this.transformInverse(vec.x, vec.y, vec.z, dst)
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)),
            JomlMath.fma(2.0 * (xy + zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)),
            JomlMath.fma(2.0 * (xz - yw) * k, x, JomlMath.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)),
            dst.w
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
        val n = 1.0 / JomlMath.fma(this.x, this.x, JomlMath.fma(this.y, this.y, JomlMath.fma(this.z, this.z, w * w)))
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)),
            JomlMath.fma(2.0 * (xy - zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)),
            JomlMath.fma(2.0 * (xz + yw) * k, x, JomlMath.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transform(vec: Vector3f): Vector3f {
        return this.transform(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), vec)
    }

    fun transformInverse(vec: Vector3f): Vector3f {
        return this.transformInverse(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), vec)
    }

    fun transformUnit(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverseUnit(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)),
            JomlMath.fma(
                2.0 * (xy + zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z
                )
            ),
            JomlMath.fma(2.0 * (xz - yw), x, JomlMath.fma(2.0 * (yz + xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)),
            JomlMath.fma(
                2.0 * (xy - zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z
                )
            ),
            JomlMath.fma(2.0 * (xz + yw), x, JomlMath.fma(2.0 * (yz - xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
        )
    }

    @JvmOverloads
    fun transformUnit(vec: Vector4d, dst: Vector4d = vec): Vector4d {
        return this.transformUnit(vec.x, vec.y, vec.z, dst)
    }

    @JvmOverloads
    fun transformInverseUnit(vec: Vector4d, dst: Vector4d = vec): Vector4d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)),
            JomlMath.fma(
                2.0 * (xy + zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z
                )
            ),
            JomlMath.fma(2.0 * (xz - yw), x, JomlMath.fma(2.0 * (yz + xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z)),
            dst.w
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)),
            JomlMath.fma(
                2.0 * (xy - zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z
                )
            ),
            JomlMath.fma(2.0 * (xz + yw), x, JomlMath.fma(2.0 * (yz - xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z)),
            dst.w
        )
    }

    fun transformUnit(vec: Vector3f): Vector3f {
        return this.transformUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), vec)
    }

    fun transformInverseUnit(vec: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), vec)
    }

    fun transformPositiveX(dst: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dst.x = (ww + xx - zz - yy).toFloat()
        dst.y = (xy + zw + zw + xy).toFloat()
        dst.z = (xz - yw + xz - yw).toFloat()
        return dst
    }

    fun transformPositiveX(dst: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val xz = x * z
        val yw = y * w
        dst.x = (ww + xx - zz - yy).toFloat()
        dst.y = (xy + zw + zw + xy).toFloat()
        dst.z = (xz - yw + xz - yw).toFloat()
        return dst
    }

    fun transformUnitPositiveX(dst: Vector3f): Vector3f {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = (1.0 - yy - yy - zz - zz).toFloat()
        dst.y = (xy + zw + xy + zw).toFloat()
        dst.z = (xz - yw + xz - yw).toFloat()
        return dst
    }

    fun transformUnitPositiveX(dst: Vector4f): Vector4f {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = (1.0 - yy - yy - zz - zz).toFloat()
        dst.y = (xy + zw + xy + zw).toFloat()
        dst.z = (xz - yw + xz - yw).toFloat()
        return dst
    }

    fun transformPositiveY(dst: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dst.x = (-zw + xy - zw + xy).toFloat()
        dst.y = (yy - zz + ww - xx).toFloat()
        dst.z = (yz + yz + xw + xw).toFloat()
        return dst
    }

    fun transformPositiveY(dst: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val zw = z * w
        val xy = x * y
        val yz = y * z
        val xw = x * w
        dst.x = (-zw + xy - zw + xy).toFloat()
        dst.y = (yy - zz + ww - xx).toFloat()
        dst.z = (yz + yz + xw + xw).toFloat()
        return dst
    }

    fun transformUnitPositiveY(dst: Vector4f): Vector4f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = (xy - zw + xy - zw).toFloat()
        dst.y = (1.0 - xx - xx - zz - zz).toFloat()
        dst.z = (yz + yz + xw + xw).toFloat()
        return dst
    }

    fun transformUnitPositiveY(dst: Vector3f): Vector3f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = (xy - zw + xy - zw).toFloat()
        dst.y = (1.0 - xx - xx - zz - zz).toFloat()
        dst.z = (yz + yz + xw + xw).toFloat()
        return dst
    }

    fun transformPositiveZ(dst: Vector3f): Vector3f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dst.x = (yw + xz + xz + yw).toFloat()
        dst.y = (yz + yz - xw - xw).toFloat()
        dst.z = (zz - yy - xx + ww).toFloat()
        return dst
    }

    fun transformPositiveZ(dst: Vector4f): Vector4f {
        val ww = w * w
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xz = x * z
        val yw = y * w
        val yz = y * z
        val xw = x * w
        dst.x = (yw + xz + xz + yw).toFloat()
        dst.y = (yz + yz - xw - xw).toFloat()
        dst.z = (zz - yy - xx + ww).toFloat()
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector4f): Vector4f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = (xz + yw + xz + yw).toFloat()
        dst.y = (yz + yz - xw - xw).toFloat()
        dst.z = (1.0 - xx - xx - yy - yy).toFloat()
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector3f): Vector3f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = (xz + yw + xz + yw).toFloat()
        dst.y = (yz + yz - xw - xw).toFloat()
        dst.z = (1.0 - xx - xx - yy - yy).toFloat()
        return dst
    }

    fun transform(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transform(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transformInverse(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformInverse(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector3f): Vector3f {
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)),
            JomlMath.fma(2.0 * (xy + zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)),
            JomlMath.fma(2.0 * (xz - yw) * k, x, JomlMath.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector3f): Vector3f {
        val n = 1.0 / JomlMath.fma(this.x, this.x, JomlMath.fma(this.y, this.y, JomlMath.fma(this.z, this.z, w * w)))
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)),
            JomlMath.fma(2.0 * (xy - zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)),
            JomlMath.fma(2.0 * (xz + yw) * k, x, JomlMath.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z))
        )
    }

    @JvmOverloads
    fun transform(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transform(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    @JvmOverloads
    fun transformInverse(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transformInverse(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector4f): Vector4f {
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z))
                .toFloat(),
            JomlMath.fma(2.0 * (xy + zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z))
                .toFloat(),
            JomlMath.fma(2.0 * (xz - yw) * k, x, JomlMath.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z))
                .toFloat(),
            dst.w
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector4f): Vector4f {
        val n = 1.0 / JomlMath.fma(this.x, this.x, JomlMath.fma(this.y, this.y, JomlMath.fma(this.z, this.z, w * w)))
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
        val k = 1.0 / (xx + yy + zz + ww)
        return dst.set(
            JomlMath.fma((xx - yy - zz + ww) * k, x, JomlMath.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)),
            JomlMath.fma(2.0 * (xy - zw) * k, x, JomlMath.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)),
            JomlMath.fma(2.0 * (xz + yw) * k, x, JomlMath.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)),
            dst.w.toDouble()
        )
    }

    fun transformUnit(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transformInverseUnit(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dst: Vector3f): Vector3f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z))
                .toFloat(),
            JomlMath.fma(
                2.0 * (xy + zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z
                )
            ).toFloat(),
            JomlMath.fma(2.0 * (xz - yw), x, JomlMath.fma(2.0 * (yz + xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
                .toFloat()
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dst: Vector3f): Vector3f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z))
                .toFloat(),
            JomlMath.fma(
                2.0 * (xy - zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z
                )
            ).toFloat(),
            JomlMath.fma(2.0 * (xz + yw), x, JomlMath.fma(2.0 * (yz - xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
                .toFloat()
        )
    }

    @JvmOverloads
    fun transformUnit(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transformUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    @JvmOverloads
    fun transformInverseUnit(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transformInverseUnit(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble(), dst)
    }

    fun transformUnit(x: Double, y: Double, z: Double, dst: Vector4f): Vector4f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z))
                .toFloat(),
            JomlMath.fma(
                2.0 * (xy + zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z
                )
            ).toFloat(),
            JomlMath.fma(2.0 * (xz - yw), x, JomlMath.fma(2.0 * (yz + xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
                .toFloat()
        )
    }

    fun transformInverseUnit(x: Double, y: Double, z: Double, dst: Vector4f): Vector4f {
        val xx = this.x * this.x
        val xy = this.x * this.y
        val xz = this.x * this.z
        val xw = this.x * w
        val yy = this.y * this.y
        val yz = this.y * this.z
        val yw = this.y * w
        val zz = this.z * this.z
        val zw = this.z * w
        return dst.set(
            JomlMath.fma(JomlMath.fma(-2.0, yy + zz, 1.0), x, JomlMath.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z))
                .toFloat(),
            JomlMath.fma(
                2.0 * (xy - zw), x, JomlMath.fma(
                    JomlMath.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z
                )
            ).toFloat(),
            JomlMath.fma(2.0 * (xz + yw), x, JomlMath.fma(2.0 * (yz - xw), y, JomlMath.fma(-2.0, xx + yy, 1.0) * z))
                .toFloat()
        )
    }

    @JvmOverloads
    fun invert(dst: Quaterniond = this): Quaterniond {
        val invNorm = 1.0 / lengthSquared()
        dst.x = -x * invNorm
        dst.y = -y * invNorm
        dst.z = -z * invNorm
        dst.w = w * invNorm
        return dst
    }

    @JvmOverloads
    fun div(b: Quaterniond, dst: Quaterniond = this): Quaterniond {
        val invNorm = 1.0 / JomlMath.fma(b.x, b.x, JomlMath.fma(b.y, b.y, JomlMath.fma(b.z, b.z, b.w * b.w)))
        val x = -b.x * invNorm
        val y = -b.y * invNorm
        val z = -b.z * invNorm
        val w = b.w * invNorm
        return dst.set(
            JomlMath.fma(this.w, x, JomlMath.fma(this.x, w, JomlMath.fma(this.y, z, -this.z * y))),
            JomlMath.fma(this.w, y, JomlMath.fma(-this.x, z, JomlMath.fma(this.y, w, this.z * x))),
            JomlMath.fma(this.w, z, JomlMath.fma(this.x, y, JomlMath.fma(-this.y, x, this.z * w))),
            JomlMath.fma(this.w, w, JomlMath.fma(-this.x, x, JomlMath.fma(-this.y, y, -this.z * z)))
        )
    }

    fun conjugate(): Quaterniond {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun conjugate(dst: Quaterniond): Quaterniond {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = w
        return dst
    }

    fun identity(): Quaterniond {
        x = 0.0
        y = 0.0
        z = 0.0
        w = 1.0
        return this
    }

    fun lengthSquared(): Double {
        return JomlMath.fma(x, x, JomlMath.fma(y, y, JomlMath.fma(z, z, w * w)))
    }

    fun rotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
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

    fun rotationZYX(angleZ: Double, angleY: Double, angleX: Double): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
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

    fun rotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
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
    fun slerp(target: Quaterniond, alpha: Double, dst: Quaterniond = this): Quaterniond {
        val cosom = JomlMath.fma(x, target.x, JomlMath.fma(y, target.y, JomlMath.fma(z, target.z, w * target.w)))
        val absCosom = abs(cosom)
        val scale0: Double
        var scale1: Double
        if (1.0 - absCosom > 1.0E-6) {
            val sinSqr = 1.0 - absCosom * absCosom
            val sinom = JomlMath.invsqrt(sinSqr)
            val omega = atan2(sinSqr * sinom, absCosom)
            scale0 = sin((1.0 - alpha) * omega) * sinom
            scale1 = sin(alpha * omega) * sinom
        } else {
            scale0 = 1.0 - alpha
            scale1 = alpha
        }
        scale1 = if (cosom >= 0.0) scale1 else -scale1
        dst.x = JomlMath.fma(scale0, x, scale1 * target.x)
        dst.y = JomlMath.fma(scale0, y, scale1 * target.y)
        dst.z = JomlMath.fma(scale0, z, scale1 * target.z)
        dst.w = JomlMath.fma(scale0, w, scale1 * target.w)
        return dst
    }

    @JvmOverloads
    fun scale(factor: Double, dst: Quaterniond = this): Quaterniond {
        val sqrt = sqrt(factor)
        dst.x = sqrt * x
        dst.y = sqrt * y
        dst.z = sqrt * z
        dst.w = sqrt * w
        return dst
    }

    fun scaling(factor: Double): Quaterniond {
        val sqrt = sqrt(factor)
        x = 0.0
        y = 0.0
        z = 0.0
        w = sqrt
        return this
    }

    @JvmOverloads
    fun integrate(dt: Double, vx: Double, vy: Double, vz: Double, dst: Quaterniond = this): Quaterniond {
        val thetaX = dt * vx * 0.5
        val thetaY = dt * vy * 0.5
        val thetaZ = dt * vz * 0.5
        val thetaMagSq = thetaX * thetaX + thetaY * thetaY + thetaZ * thetaZ
        val s: Double
        val dqW: Double
        if (thetaMagSq * thetaMagSq / 24.0 < 1.0E-8) {
            dqW = 1.0 - thetaMagSq * 0.5
            s = 1.0 - thetaMagSq / 6.0
        } else {
            val thetaMag = sqrt(thetaMagSq)
            val sin = sin(thetaMag)
            s = sin / thetaMag
            dqW = cos(thetaMag)
        }
        val dqX = thetaX * s
        val dqY = thetaY * s
        val dqZ = thetaZ * s
        return dst.set(
            JomlMath.fma(dqW, x, JomlMath.fma(dqX, w, JomlMath.fma(dqY, z, -dqZ * y))),
            JomlMath.fma(dqW, y, JomlMath.fma(-dqX, z, JomlMath.fma(dqY, w, dqZ * x))),
            JomlMath.fma(dqW, z, JomlMath.fma(dqX, y, JomlMath.fma(-dqY, x, dqZ * w))),
            JomlMath.fma(dqW, w, JomlMath.fma(-dqX, x, JomlMath.fma(-dqY, y, -dqZ * z)))
        )
    }

    @JvmOverloads
    fun nlerp(q: Quaterniond, factor: Double, dst: Quaterniond = this): Quaterniond {
        val cosom = JomlMath.fma(x, q.x, JomlMath.fma(y, q.y, JomlMath.fma(z, q.z, w * q.w)))
        val scale0 = 1.0 - factor
        val scale1 = if (cosom >= 0.0) factor else -factor
        dst.x = JomlMath.fma(scale0, x, scale1 * q.x)
        dst.y = JomlMath.fma(scale0, y, scale1 * q.y)
        dst.z = JomlMath.fma(scale0, z, scale1 * q.z)
        dst.w = JomlMath.fma(scale0, w, scale1 * q.w)
        val s =
            JomlMath.invsqrt(
                JomlMath.fma(
                    dst.x,
                    dst.x,
                    JomlMath.fma(dst.y, dst.y, JomlMath.fma(dst.z, dst.z, dst.w * dst.w))
                )
            )
        dst.x *= s
        dst.y *= s
        dst.z *= s
        dst.w *= s
        return dst
    }

    @JvmOverloads
    fun nlerpIterative(q: Quaterniond, alpha: Double, dotThreshold: Double, dst: Quaterniond = this): Quaterniond {
        var q1x = x
        var q1y = y
        var q1z = z
        var q1w = w
        var q2x = q.x
        var q2y = q.y
        var q2z = q.z
        var q2w = q.w
        var dot = JomlMath.fma(q1x, q2x, JomlMath.fma(q1y, q2y, JomlMath.fma(q1z, q2z, q1w * q2w)))
        var absDot = abs(dot)
        return if (0.999999 < absDot) {
            dst.set(this)
        } else {
            var alphaN: Double
            var scale0: Double
            var scale1: Double
            alphaN = alpha
            while (absDot < dotThreshold) {
                scale0 = 0.5
                scale1 = if (dot >= 0.0) 0.5 else -0.5
                var s: Float
                if (alphaN < 0.5) {
                    q2x = JomlMath.fma(scale0, q2x, scale1 * q1x)
                    q2y = JomlMath.fma(scale0, q2y, scale1 * q1y)
                    q2z = JomlMath.fma(scale0, q2z, scale1 * q1z)
                    q2w = JomlMath.fma(scale0, q2w, scale1 * q1w)
                    s = JomlMath.invsqrt(
                        JomlMath.fma(
                            q2x,
                            q2x,
                            JomlMath.fma(q2y, q2y, JomlMath.fma(q2z, q2z, q2w * q2w))
                        )
                    ).toFloat()
                    q2x *= s.toDouble()
                    q2y *= s.toDouble()
                    q2z *= s.toDouble()
                    q2w *= s.toDouble()
                    alphaN += alphaN
                } else {
                    q1x = JomlMath.fma(scale0, q1x, scale1 * q2x)
                    q1y = JomlMath.fma(scale0, q1y, scale1 * q2y)
                    q1z = JomlMath.fma(scale0, q1z, scale1 * q2z)
                    q1w = JomlMath.fma(scale0, q1w, scale1 * q2w)
                    s = JomlMath.invsqrt(
                        JomlMath.fma(
                            q1x,
                            q1x,
                            JomlMath.fma(q1y, q1y, JomlMath.fma(q1z, q1z, q1w * q1w))
                        )
                    ).toFloat()
                    q1x *= s.toDouble()
                    q1y *= s.toDouble()
                    q1z *= s.toDouble()
                    q1w *= s.toDouble()
                    alphaN = alphaN + alphaN - 1.0
                }
                dot = q1x * q2x + q1y * q2y + q1z * q2z + q1w * q2w
                absDot = abs(dot)
            }
            scale0 = 1.0 - alphaN
            scale1 = if (dot >= 0.0) alphaN else -alphaN
            val resX = JomlMath.fma(scale0, q1x, scale1 * q2x)
            val resY = JomlMath.fma(scale0, q1y, scale1 * q2y)
            val resZ = JomlMath.fma(scale0, q1z, scale1 * q2z)
            val resW = JomlMath.fma(scale0, q1w, scale1 * q2w)
            val s = JomlMath.invsqrt(
                JomlMath.fma(
                    resX,
                    resX,
                    JomlMath.fma(resY, resY, JomlMath.fma(resZ, resZ, resW * resW))
                )
            )
            dst.x = resX * s
            dst.y = resY * s
            dst.z = resZ * s
            dst.w = resW * s
            dst
        }
    }

    fun lookAlong(dir: Vector3d, up: Vector3d): Quaterniond {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3d, up: Vector3d, dst: Quaterniond): Quaterniond {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dst: Quaterniond = this
    ): Quaterniond {
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val dirnX = -dirX * invDirLength
        val dirnY = -dirY * invDirLength
        val dirnZ = -dirZ * invDirLength
        var leftX = upY * dirnZ - upZ * dirnY
        var leftY = upZ * dirnX - upX * dirnZ
        var leftZ = upX * dirnY - upY * dirnX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirnY * leftZ - dirnZ * leftY
        val upnY = dirnZ * leftX - dirnX * leftZ
        val upnZ = dirnX * leftY - dirnY * leftX
        val tr = leftX + upnY + dirnZ
        val x: Double
        val y: Double
        val z: Double
        val w: Double
        var t: Double
        if (tr >= 0.0) {
            t = sqrt(tr + 1.0)
            w = t * 0.5
            t = 0.5 / t
            x = (dirnY - upnZ) * t
            y = (leftZ - dirnX) * t
            z = (upnX - leftY) * t
        } else if (leftX > upnY && leftX > dirnZ) {
            t = sqrt(1.0 + leftX - upnY - dirnZ)
            x = t * 0.5
            t = 0.5 / t
            y = (leftY + upnX) * t
            z = (dirnX + leftZ) * t
            w = (dirnY - upnZ) * t
        } else if (upnY > dirnZ) {
            t = sqrt(1.0 + upnY - leftX - dirnZ)
            y = t * 0.5
            t = 0.5 / t
            x = (leftY + upnX) * t
            z = (upnZ + dirnY) * t
            w = (leftZ - dirnX) * t
        } else {
            t = sqrt(1.0 + dirnZ - leftX - upnY)
            z = t * 0.5
            t = 0.5 / t
            x = (dirnX + leftZ) * t
            y = (upnZ + dirnY) * t
            w = (upnX - leftY) * t
        }
        return dst.set(
            JomlMath.fma(this.w, x, JomlMath.fma(this.x, w, JomlMath.fma(this.y, z, -this.z * y))),
            JomlMath.fma(this.w, y, JomlMath.fma(-this.x, z, JomlMath.fma(this.y, w, this.z * x))),
            JomlMath.fma(this.w, z, JomlMath.fma(this.x, y, JomlMath.fma(-this.y, x, this.z * w))),
            JomlMath.fma(this.w, w, JomlMath.fma(-this.x, x, JomlMath.fma(-this.y, y, -this.z * z)))
        )
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
        else if (other !is Quaterniond) false
        else x == other.x && y == other.y && z == other.z && w == other.w
    }

    @JvmOverloads
    fun difference(other: Quaterniond, dst: Quaterniond = this): Quaterniond {
        val invNorm = 1.0 / lengthSquared()
        val x = -x * invNorm
        val y = -y * invNorm
        val z = -z * invNorm
        val w = w * invNorm
        dst.set(
            JomlMath.fma(w, other.x, JomlMath.fma(x, other.w, JomlMath.fma(y, other.z, -z * other.y))),
            JomlMath.fma(w, other.y, JomlMath.fma(-x, other.z, JomlMath.fma(y, other.w, z * other.x))),
            JomlMath.fma(w, other.z, JomlMath.fma(x, other.y, JomlMath.fma(-y, other.x, z * other.w))),
            JomlMath.fma(w, other.w, JomlMath.fma(-x, other.x, JomlMath.fma(-y, other.y, -z * other.z)))
        )
        return dst
    }

    fun rotationTo(
        fromDirX: Double,
        fromDirY: Double,
        fromDirZ: Double,
        toDirX: Double,
        toDirY: Double,
        toDirZ: Double
    ): Quaterniond {
        val fn =
            JomlMath.invsqrt(JomlMath.fma(fromDirX, fromDirX, JomlMath.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)))
        val tn = JomlMath.invsqrt(JomlMath.fma(toDirX, toDirX, JomlMath.fma(toDirY, toDirY, toDirZ * toDirZ)))
        val fx = fromDirX * fn
        val fy = fromDirY * fn
        val fz = fromDirZ * fn
        val tx = toDirX * tn
        val ty = toDirY * tn
        val tz = toDirZ * tn
        val dot = fx * tx + fy * ty + fz * tz
        var x: Double
        var y: Double
        var z: Double
        val w: Double
        if (dot < -0.999999) {
            x = fy
            y = -fx
            z = 0.0
            if (fy * fy + y * y == 0.0) {
                x = 0.0
                y = fz
                z = -fy
            }
            this.x = x
            this.y = y
            this.z = z
            this.w = 0.0
        } else {
            val sd2 = sqrt((1.0 + dot) * 2.0)
            val isd2 = 1.0 / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5
            val n2 = JomlMath.invsqrt(JomlMath.fma(x, x, JomlMath.fma(y, y, JomlMath.fma(z, z, w * w))))
            this.x = x * n2
            this.y = y * n2
            this.z = z * n2
            this.w = w * n2
        }
        return this
    }

    fun rotationTo(fromDir: Vector3d, toDir: Vector3d): Quaterniond {
        return this.rotationTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z)
    }

    @JvmOverloads
    fun rotateTo(
        fromDirX: Double,
        fromDirY: Double,
        fromDirZ: Double,
        toDirX: Double,
        toDirY: Double,
        toDirZ: Double,
        dst: Quaterniond = this
    ): Quaterniond {
        val fn =
            JomlMath.invsqrt(JomlMath.fma(fromDirX, fromDirX, JomlMath.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)))
        val tn = JomlMath.invsqrt(JomlMath.fma(toDirX, toDirX, JomlMath.fma(toDirY, toDirY, toDirZ * toDirZ)))
        val fx = fromDirX * fn
        val fy = fromDirY * fn
        val fz = fromDirZ * fn
        val tx = toDirX * tn
        val ty = toDirY * tn
        val tz = toDirZ * tn
        val dot = fx * tx + fy * ty + fz * tz
        var x: Double
        var y: Double
        var z: Double
        var w: Double
        if (dot < -0.999999) {
            x = fy
            y = -fx
            z = 0.0
            w = 0.0
            if (fy * fy + y * y == 0.0) {
                x = 0.0
                y = fz
                z = -fy
                w = 0.0
            }
        } else {
            val sd2 = sqrt((1.0 + dot) * 2.0)
            val isd2 = 1.0 / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5
            val n2 = JomlMath.invsqrt(JomlMath.fma(x, x, JomlMath.fma(y, y, JomlMath.fma(z, z, w * w))))
            x *= n2
            y *= n2
            z *= n2
            w *= n2
        }
        return dst.set(
            JomlMath.fma(this.w, x, JomlMath.fma(this.x, w, JomlMath.fma(this.y, z, -this.z * y))),
            JomlMath.fma(this.w, y, JomlMath.fma(-this.x, z, JomlMath.fma(this.y, w, this.z * x))),
            JomlMath.fma(this.w, z, JomlMath.fma(this.x, y, JomlMath.fma(-this.y, x, this.z * w))),
            JomlMath.fma(this.w, w, JomlMath.fma(-this.x, x, JomlMath.fma(-this.y, y, -this.z * z)))
        )
    }

    fun rotationAxis(axisAngle: AxisAngle4f): Quaterniond {
        return this.rotationAxis(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble()
        )
    }

    fun rotationAxis(angle: Double, axisX: Double, axisY: Double, axisZ: Double): Quaterniond {
        val halfAngle = angle / 2.0
        val sinAngle = sin(halfAngle)
        val invVLength = JomlMath.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        return this.set(
            axisX * invVLength * sinAngle,
            axisY * invVLength * sinAngle,
            axisZ * invVLength * sinAngle,
            cos(halfAngle)
        )
    }

    fun rotationX(angle: Double): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return this.set(sin, 0.0, cos, 0.0)
    }

    fun rotationY(angle: Double): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return this.set(0.0, sin, 0.0, cos)
    }

    fun rotationZ(angle: Double): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return this.set(0.0, 0.0, sin, cos)
    }

    fun rotateTo(fromDir: Vector3d, toDir: Vector3d, dst: Quaterniond): Quaterniond {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, dst)
    }

    fun rotateTo(fromDir: Vector3d, toDir: Vector3d): Quaterniond {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, this)
    }

    @JvmOverloads
    fun rotateX(angle: Double, dst: Quaterniond = this): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return dst.set(w * sin + x * cos, y * cos + z * sin, z * cos - y * sin, w * cos - x * sin)
    }

    @JvmOverloads
    fun rotateY(angle: Double, dst: Quaterniond = this): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return dst.set(x * cos - z * sin, w * sin + y * cos, x * sin + z * cos, w * cos - y * sin)
    }

    @JvmOverloads
    fun rotateZ(angle: Double, dst: Quaterniond = this): Quaterniond {
        val sin = sin(angle * 0.5)
        val cos = cos(angle * 0.5)
        return dst.set(x * cos + y * sin, y * cos - x * sin, w * sin + z * cos, w * cos - z * sin)
    }

    @JvmOverloads
    fun rotateLocalX(angle: Double, dst: Quaterniond = this): Quaterniond {
        val halfAngle = angle * 0.5
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x + s * w, c * y - s * z, c * z + s * y, c * w - s * x)
        return dst
    }

    @JvmOverloads
    fun rotateLocalY(angle: Double, dst: Quaterniond = this): Quaterniond {
        val halfAngle = angle * 0.5
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x + s * z, c * y + s * w, c * z - s * x, c * w - s * y)
        return dst
    }

    @JvmOverloads
    fun rotateLocalZ(angle: Double, dst: Quaterniond = this): Quaterniond {
        val halfAngle = angle * 0.5
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x - s * y, c * y + s * x, c * z + s * w, c * w - s * z)
        return dst
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Double, angleY: Double, angleZ: Double, dst: Quaterniond = this): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        val w = cx * cycz - sx * sysz
        val x = sx * cycz + cx * sysz
        val y = cx * sycz - sx * cysz
        val z = cx * cysz + sx * sycz
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Double, angleY: Double, angleX: Double, dst: Quaterniond = this): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
        val cycz = cy * cz
        val sysz = sy * sz
        val sycz = sy * cz
        val cysz = cy * sz
        val w = cx * cycz + sx * sysz
        val x = sx * cycz - cx * sysz
        val y = cx * sycz + sx * cysz
        val z = cx * cysz - sx * sycz
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Double, angleX: Double, angleZ: Double, dst: Quaterniond = this): Quaterniond {
        val sx = sin(angleX * 0.5)
        val cx = cos(angleX * 0.5)
        val sy = sin(angleY * 0.5)
        val cy = cos(angleY * 0.5)
        val sz = sin(angleZ * 0.5)
        val cz = cos(angleZ * 0.5)
        val yx = cy * sx
        val yy = sy * cx
        val yz = sy * sx
        val yw = cy * cx
        val x = yx * cz + yy * sz
        val y = yy * cz - yx * sz
        val z = yw * sz - yz * cz
        val w = yw * cz + yz * sz
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    fun getEulerAnglesXYZ(eulerAngles: Vector3d): Vector3d {
        eulerAngles.x = atan2(x * w - y * z, 0.5 - x * x - y * y)
        eulerAngles.y = JomlMath.safeAsin(2.0 * (x * z + y * w))
        eulerAngles.z = atan2(z * w - x * y, 0.5 - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZYX(eulerAngles: Vector3d): Vector3d {
        eulerAngles.x = atan2(y * z + w * x, 0.5 - x * x + y * y)
        eulerAngles.y = JomlMath.safeAsin(-2.0 * (x * z - w * y))
        eulerAngles.z = atan2(x * y + w * z, 0.5 - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZXY(eulerAngles: Vector3d): Vector3d {
        eulerAngles.x = JomlMath.safeAsin(2.0 * (w * x + y * z))
        eulerAngles.y = atan2(w * y - x * z, 0.5 - y * y - x * x)
        eulerAngles.z = atan2(w * z - x * y, 0.5 - z * z - x * x)
        return eulerAngles
    }

    fun getEulerAnglesYXZ(eulerAngles: Vector3d): Vector3d {
        eulerAngles.x = JomlMath.safeAsin(-2.0 * (y * z - w * x))
        eulerAngles.y = atan2(x * z + y * w, 0.5 - y * y - x * x)
        eulerAngles.z = atan2(y * x + w * z, 0.5 - x * x - z * z)
        return eulerAngles
    }

    @JvmOverloads
    fun rotateAxis(angle: Double, axisX: Double, axisY: Double, axisZ: Double, dst: Quaterniond = this): Quaterniond {
        val halfAngle = angle / 2.0
        val sinAngle = sin(halfAngle)
        val invVLength = JomlMath.invsqrt(JomlMath.fma(axisX, axisX, JomlMath.fma(axisY, axisY, axisZ * axisZ)))
        val rx = axisX * invVLength * sinAngle
        val ry = axisY * invVLength * sinAngle
        val rz = axisZ * invVLength * sinAngle
        val rw = cos(halfAngle)
        return dst.set(
            JomlMath.fma(w, rx, JomlMath.fma(x, rw, JomlMath.fma(y, rz, -z * ry))), JomlMath.fma(
                w, ry, JomlMath.fma(-x, rz, JomlMath.fma(y, rw, z * rx))
            ), JomlMath.fma(
                w, rz, JomlMath.fma(
                    x, ry, JomlMath.fma(-y, rx, z * rw)
                )
            ), JomlMath.fma(w, rw, JomlMath.fma(-x, rx, JomlMath.fma(-y, ry, -z * rz)))
        )
    }

    fun rotateAxis(angle: Double, axis: Vector3d, dst: Quaterniond): Quaterniond {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, dst)
    }

    fun rotateAxis(angle: Double, axis: Vector3d): Quaterniond {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, this)
    }

    fun positiveX(dir: Vector3d): Vector3d {
        val invNorm = 1.0 / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dy = ny + ny
        val dz = nz + nz
        dir.x = -ny * dy - nz * dz + 1.0
        dir.y = nx * dy + nw * dz
        dir.z = nx * dz - nw * dy
        return dir
    }

    fun normalizedPositiveX(dir: Vector3d): Vector3d {
        val dy = y + y
        val dz = z + z
        dir.x = -y * dy - z * dz + 1.0
        dir.y = x * dy - w * dz
        dir.z = x * dz + w * dy
        return dir
    }

    fun positiveY(dir: Vector3d): Vector3d {
        val invNorm = 1.0 / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dx = nx + nx
        val dy = ny + ny
        val dz = nz + nz
        dir.x = nx * dy - nw * dz
        dir.y = -nx * dx - nz * dz + 1.0
        dir.z = ny * dz + nw * dx
        return dir
    }

    fun normalizedPositiveY(dir: Vector3d): Vector3d {
        val dx = x + x
        val dy = y + y
        val dz = z + z
        dir.x = x * dy + w * dz
        dir.y = -x * dx - z * dz + 1.0
        dir.z = y * dz - w * dx
        return dir
    }

    fun positiveZ(dir: Vector3d): Vector3d {
        val invNorm = 1.0 / lengthSquared()
        val nx = -x * invNorm
        val ny = -y * invNorm
        val nz = -z * invNorm
        val nw = w * invNorm
        val dx = nx + nx
        val dy = ny + ny
        val dz = nz + nz
        dir.x = nx * dz + nw * dy
        dir.y = ny * dz - nw * dx
        dir.z = -nx * dx - ny * dy + 1.0
        return dir
    }

    fun normalizedPositiveZ(dir: Vector3d): Vector3d {
        val dx = x + x
        val dy = y + y
        val dz = z + z
        dir.x = x * dz - w * dy
        dir.y = y * dz + w * dx
        dir.z = -x * dx - y * dy + 1.0
        return dir
    }

    @JvmOverloads
    fun conjugateBy(q: Quaterniond, dst: Quaterniond = this): Quaterniond {
        val invNorm = 1.0 / q.lengthSquared()
        val qix = -q.x * invNorm
        val qiy = -q.y * invNorm
        val qiz = -q.z * invNorm
        val qiw = q.w * invNorm
        val qpx = JomlMath.fma(q.w, x, JomlMath.fma(q.x, w, JomlMath.fma(q.y, z, -q.z * y)))
        val qpy = JomlMath.fma(q.w, y, JomlMath.fma(-q.x, z, JomlMath.fma(q.y, w, q.z * x)))
        val qpz = JomlMath.fma(q.w, z, JomlMath.fma(q.x, y, JomlMath.fma(-q.y, x, q.z * w)))
        val qpw = JomlMath.fma(q.w, w, JomlMath.fma(-q.x, x, JomlMath.fma(-q.y, y, -q.z * z)))
        return dst.set(
            JomlMath.fma(qpw, qix, JomlMath.fma(qpx, qiw, JomlMath.fma(qpy, qiz, -qpz * qiy))),
            JomlMath.fma(qpw, qiy, JomlMath.fma(-qpx, qiz, JomlMath.fma(qpy, qiw, qpz * qix))),
            JomlMath.fma(qpw, qiz, JomlMath.fma(qpx, qiy, JomlMath.fma(-qpy, qix, qpz * qiw))),
            JomlMath.fma(qpw, qiw, JomlMath.fma(-qpx, qix, JomlMath.fma(-qpy, qiy, -qpz * qiz)))
        )
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    fun equals(q: Quaterniond?, delta: Double): Boolean {
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

    fun equals(x: Double, y: Double, z: Double, w: Double): Boolean {
        return if ((this.x) != (x)) {
            false
        } else if ((this.y) != (y)) {
            false
        } else if ((this.z) != (z)) {
            false
        } else {
            (this.w) == (w)
        }
    }

    companion object {
        @JvmStatic
        fun slerp(qs: Array<Quaterniond>, weights: DoubleArray, dst: Quaterniond): Quaterniond {
            dst.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dst.slerp(qs[i], rw1)
            }
            return dst
        }

        @JvmStatic
        fun nlerp(qs: Array<Quaterniond>, weights: DoubleArray, dst: Quaterniond): Quaterniond {
            dst.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dst.nlerp(qs[i], rw1)
            }
            return dst
        }

        @JvmStatic
        fun nlerpIterative(
            qs: Array<Quaterniond>,
            weights: DoubleArray,
            dotThreshold: Double,
            dst: Quaterniond
        ): Quaterniond {
            dst.set(qs[0])
            var w = weights[0]
            for (i in 1 until qs.size) {
                val w1 = weights[i]
                val rw1 = w1 / (w + w1)
                w += w1
                dst.nlerpIterative(qs[i], rw1, dotThreshold)
            }
            return dst
        }
    }
}