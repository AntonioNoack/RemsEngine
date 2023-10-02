package org.joml

import kotlin.math.*

@Suppress("unused")
open class Quaternionf {
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
        val sin = sin(axisAngle.angle * 0.5f)
        val cos = cos(axisAngle.angle * 0.5f)
        x = axisAngle.x * sin
        y = axisAngle.y * sin
        z = axisAngle.z * sin
        w = cos
    }

    constructor(axisAngle: AxisAngle4d) {
        val sin = sin(axisAngle.angle * 0.5)
        val cos = cos(axisAngle.angle * 0.5)
        x = (axisAngle.x * sin).toFloat()
        y = (axisAngle.y * sin).toFloat()
        z = (axisAngle.z * sin).toFloat()
        w = cos.toFloat()
    }

    @JvmOverloads
    fun normalize(dst: Quaternionf = this): Quaternionf {
        val invNorm = JomlMath.invsqrt(lengthSquared())
        dst.x = x * invNorm
        dst.y = y * invNorm
        dst.z = z * invNorm
        dst.w = w * invNorm
        return dst
    }

    @JvmOverloads
    fun add(x: Float, y: Float, z: Float, w: Float, dst: Quaternionf = this): Quaternionf {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    @JvmOverloads
    fun add(q2: Quaternionf, dst: Quaternionf = this): Quaternionf {
        dst.x = x + q2.x
        dst.y = y + q2.y
        dst.z = z + q2.z
        dst.w = w + q2.w
        return dst
    }

    fun dot(o: Quaternionf): Float {
        return x * o.x + y * o.y + z * o.z + w * o.w
    }

    fun angle(): Float {
        return (2.0 * JomlMath.safeAcos(w).toDouble()).toFloat()
    }

    fun get(dst: Matrix3f): Matrix3f {
        return dst.set(this)
    }

    fun get(dst: Matrix3d): Matrix3d {
        return dst.set(this)
    }

    fun get(dst: Matrix4f): Matrix4f {
        return dst.set(this)
    }

    fun get(dst: Matrix4d): Matrix4d {
        return dst.set(this)
    }

    fun get(dst: Matrix4x3f): Matrix4x3f {
        return dst.set(this)
    }

    fun get(dst: Matrix4x3d): Matrix4x3d {
        return dst.set(this)
    }

    fun get(dst: AxisAngle4f): AxisAngle4f {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Float
        if (w > 1f) {
            s = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dst.angle = 2f * acos(w)
        s = sqrt(1f - w * w)
        if (s < 0.001f) {
            dst.x = x
            dst.y = y
            dst.z = z
        } else {
            s = 1f / s
            dst.x = x * s
            dst.y = y * s
            dst.z = z * s
        }
        return dst
    }

    fun get(dst: AxisAngle4d): AxisAngle4d {
        var x = x
        var y = y
        var z = z
        var w = w
        var s: Float
        if (w > 1f) {
            s = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
            x *= s
            y *= s
            z *= s
            w *= s
        }
        dst.angle = (2f * acos(w)).toDouble()
        s = sqrt(1f - w * w)
        if (s < 0.001f) {
            dst.x = x.toDouble()
            dst.y = y.toDouble()
            dst.z = z.toDouble()
        } else {
            s = 1f / s
            dst.x = (x * s).toDouble()
            dst.y = (y * s).toDouble()
            dst.z = (z * s).toDouble()
        }
        return dst
    }

    fun get(dst: Quaterniond): Quaterniond {
        return dst.set(this)
    }

    fun get(dst: Quaternionf): Quaternionf {
        return dst.set(this)
    }

    fun set(x: Float, y: Float, z: Float, w: Float): Quaternionf {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    fun set(q: Quaternionf) = set(q.x, q.y, q.z, q.w)
    fun set(q: Quaterniond) = set(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())
    fun set(src: FloatArray, i: Int) = set(src[i], src[i + 1], src[i + 2], src[i + 3])
    fun get(dst: FloatArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
        dst[i + 3] = w
    }

    fun set(axisAngle: AxisAngle4f): Quaternionf {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun set(axisAngle: AxisAngle4d): Quaternionf {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun setAngleAxis(angle: Float, x: Float, y: Float, z: Float): Quaternionf {
        val s = sin(angle * 0.5f)
        this.x = x * s
        this.y = y * s
        this.z = z * s
        w = cos(angle * 0.5f)
        return this
    }

    fun setAngleAxis(angle: Double, x: Double, y: Double, z: Double): Quaternionf {
        val s = sin(angle * 0.5)
        this.x = (x * s).toFloat()
        this.y = (y * s).toFloat()
        this.z = (z * s).toFloat()
        w = cos(angle * 0.5).toFloat()
        return this
    }

    fun rotationAxis(axisAngle: AxisAngle4f): Quaternionf {
        return this.rotationAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotationAxis(angle: Float, axisX: Float, axisY: Float, axisZ: Float): Quaternionf {
        val halfAngle = angle / 2f
        val sinAngle = sin(halfAngle)
        val invVLength = JomlMath.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        return this.set(
            axisX * invVLength * sinAngle,
            axisY * invVLength * sinAngle,
            axisZ * invVLength * sinAngle,
            cos(halfAngle)
        )
    }

    fun rotationAxis(angle: Float, axis: Vector3f): Quaternionf {
        return this.rotationAxis(angle, axis.x, axis.y, axis.z)
    }

    fun rotationX(angle: Float): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
        return this.set(sin, 0f, 0f, cos)
    }

    fun rotationY(angle: Float): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
        return this.set(0f, sin, 0f, cos)
    }

    fun rotationZ(angle: Float): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
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
            t = sqrt(tr + 1f)
            w = t * 0.5f
            t = 0.5f / t
            x = (m12 - m21) * t
            y = (m20 - m02) * t
            z = (m01 - m10) * t
        } else if (m00 >= m11 && m00 >= m22) {
            t = sqrt(m00 - (m11 + m22) + 1f)
            x = t * 0.5f
            t = 0.5f / t
            y = (m10 + m01) * t
            z = (m02 + m20) * t
            w = (m12 - m21) * t
        } else if (m11 > m22) {
            t = sqrt(m11 - (m22 + m00) + 1f)
            y = t * 0.5f
            t = 0.5f / t
            z = (m21 + m12) * t
            x = (m10 + m01) * t
            w = (m20 - m02) * t
        } else {
            t = sqrt(m22 - (m00 + m11) + 1f)
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
            w = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((m12 - m21) * t).toFloat()
            y = ((m20 - m02) * t).toFloat()
            z = ((m01 - m10) * t).toFloat()
        } else if (m00 >= m11 && m00 >= m22) {
            t = sqrt(m00 - (m11 + m22) + 1.0)
            x = (t * 0.5).toFloat()
            t = 0.5 / t
            y = ((m10 + m01) * t).toFloat()
            z = ((m02 + m20) * t).toFloat()
            w = ((m12 - m21) * t).toFloat()
        } else if (m11 > m22) {
            t = sqrt(m11 - (m22 + m00) + 1.0)
            y = (t * 0.5).toFloat()
            t = 0.5 / t
            z = ((m21 + m12) * t).toFloat()
            x = ((m10 + m01) * t).toFloat()
            w = ((m20 - m02) * t).toFloat()
        } else {
            t = sqrt(m22 - (m00 + m11) + 1.0)
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
        val sinAngle = sin(halfAngle)
        val vLength = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        x = axisX / vLength * sinAngle
        y = axisY / vLength * sinAngle
        z = axisZ / vLength * sinAngle
        w = cos(halfAngle)
        return this
    }

    @JvmOverloads
    fun mul(q: Quaternionf, dst: Quaternionf = this): Quaternionf {
        return dst.set(
            w * q.x + x * q.w + y * q.z - z * q.y,
            w * q.y - x * q.z + y * q.w + z * q.x,
            w * q.z + x * q.y - y * q.x + z * q.w,
            w * q.w - x * q.x - y * q.y - z * q.z
        )
    }

    @JvmOverloads
    fun mul(qx: Float, qy: Float, qz: Float, qw: Float, dst: Quaternionf = this): Quaternionf {
        return dst.set(
            w * qx + x * qw + y * qz - z * qy,
            w * qy - x * qz + y * qw + z * qx,
            w * qz + x * qy - y * qx + z * qw,
            w * qw - x * qx - y * qy - z * qz
        )
    }

    @JvmOverloads
    fun premul(q: Quaternionf, dst: Quaternionf = this): Quaternionf {
        return dst.set(
            w * q.x + x * q.w + y * q.z - z * q.y,
            w * q.y - x * q.z + y * q.w + z * q.x,
            w * q.z + x * q.y - y * q.x + z * q.w,
            w * q.w - x * q.x - y * q.y - z * q.z
        )
    }

    @JvmOverloads
    fun premul(qx: Float, qy: Float, qz: Float, qw: Float, dst: Quaternionf = this): Quaternionf {
        return dst.set(
            w * qx + x * qw + y * qz - z * qy,
            w * qy - x * qz + y * qw + z * qx,
            w * qz + x * qy - y * qx + z * qw,
            w * qw - x * qx - y * qy - z * qz
        )
    }

    fun transform(vec: Vector3f): Vector3f {
        return this.transform(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverse(vec: Vector3f): Vector3f {
        return this.transformInverse(vec.x, vec.y, vec.z, vec)
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
        dst.x = ww + xx - zz - yy
        dst.y = xy + zw + zw + xy
        dst.z = xz - yw + xz - yw
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
        dst.x = ww + xx - zz - yy
        dst.y = xy + zw + zw + xy
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformUnitPositiveX(dst: Vector3f): Vector3f {
        val xy = x * y
        val xz = x * z
        val yy = y * y
        val yw = y * w
        val zz = z * z
        val zw = z * w
        dst.x = 1f - yy - zz - yy - zz
        dst.y = xy + zw + xy + zw
        dst.z = xz - yw + xz - yw
        return dst
    }

    fun transformUnitPositiveX(dst: Vector4f): Vector4f {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = 1f - yy - yy - zz - zz
        dst.y = xy + zw + xy + zw
        dst.z = xz - yw + xz - yw
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
        dst.x = -zw + xy - zw + xy
        dst.y = yy - zz + ww - xx
        dst.z = yz + yz + xw + xw
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
        dst.x = -zw + xy - zw + xy
        dst.y = yy - zz + ww - xx
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformUnitPositiveY(dst: Vector4f): Vector4f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = xy - zw + xy - zw
        dst.y = 1f - xx - xx - zz - zz
        dst.z = yz + yz + xw + xw
        return dst
    }

    fun transformUnitPositiveY(dst: Vector3f): Vector3f {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = xy - zw + xy - zw
        dst.y = 1f - xx - xx - zz - zz
        dst.z = yz + yz + xw + xw
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
        dst.x = yw + xz + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = zz - yy - xx + ww
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
        dst.x = yw + xz + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = zz - yy - xx + ww
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector4f): Vector4f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = xz + yw + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = 1f - xx - xx - yy - yy
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector3f): Vector3f {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = xz + yw + xz + yw
        dst.y = yz + yz - xw - xw
        dst.z = 1f - xx - xx - yy - yy
        return dst
    }

    fun transform(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transform(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverse(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformInverse(vec.x, vec.y, vec.z, dst)
    }

    fun transform(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
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
        return dst.set(
            (xx - yy - zz + ww) * k * x + 2f * (xy - zw) * k * y + 2f * (xz + yw) * k * z,
            2f * (xy + zw) * k * x + (yy - xx - zz + ww) * k * y + 2f * (yz - xw) * k * z,
            2f * (xz - yw) * k * x + 2f * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z
        )
    }

    fun transformInverse(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        val n = 1f / lengthSquared()
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
        return dst.set(
            ((xx - yy - zz + ww) * k) * x + ((2f * (xy + zw) * k) * y + (2f * (xz - yw) * k) * z),
            (2f * (xy - zw) * k) * x + (((yy - xx - zz + ww) * k) * y + (2f * (yz + xw) * k) * z),
            (2f * (xz + yw) * k) * x + ((2f * (yz - xw) * k) * y + ((zz - xx - yy + ww) * k) * z)
        )
    }

    fun transformUnit(vec: Vector3f): Vector3f {
        return this.transformUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformInverseUnit(vec: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec)
    }

    fun transformUnit(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverseUnit(vec: Vector3f, dst: Vector3f): Vector3f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy - zw) * y + 2f * (xz + yw) * z),
            2f * (xy + zw) * x + ((-2f * (xx + zz) + 1f) * y + 2f * (yz - xw) * z),
            2f * (xz - yw) * x + (2f * (yz + xw) * y + (-2f * (xx + yy) + 1f) * z)
        )
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy + zw) * y + 2f * (xz - yw) * z),
            2f * (xy - zw) * x + ((-2f * (xx + zz) + 1f) * y + 2f * (yz + xw) * z),
            2f * (xz + yw) * x + (2f * (yz - xw) * y + (-2f * (xx + yy) + 1f) * z)
        )
    }

    @JvmOverloads
    fun transform(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transform(vec.x, vec.y, vec.z, dst)
    }

    @JvmOverloads
    fun transformInverse(vec: Vector4f, dst: Vector4f = vec): Vector4f {
        return this.transformInverse(vec.x, vec.y, vec.z, dst)
    }

    fun transform(x: Float, y: Float, z: Float, dst: Vector4f): Vector4f {
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
        return dst.set(
            (xx - yy - zz + ww) * k * x + 2f * (xy - zw) * k * y + 2f * (xz + yw) * k * z,
            2f * (xy + zw) * k * x + (yy - xx - zz + ww) * k * y + 2f * (yz - xw) * k * z,
            2f * (xz - yw) * k * x + 2f * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z
        )
    }

    fun transformInverse(x: Float, y: Float, z: Float, dst: Vector4f): Vector4f {
        val n = 1f / lengthSquared()
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
        return dst.set(
            ((xx - yy - zz + ww) * k) * x + ((2f * (xy + zw) * k) * y + (2f * (xz - yw) * k) * z),
            (2f * (xy - zw) * k) * x + (((yy - xx - zz + ww) * k) * y + (2f * (yz + xw) * k) * z),
            (2f * (xz + yw) * k) * x + ((2f * (yz - xw) * k) * y + ((zz - xx - yy + ww) * k) * z)
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

    fun transformUnit(vec: Vector4f, dst: Vector4f): Vector4f {
        return this.transformUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverseUnit(vec: Vector4f, dst: Vector4f): Vector4f {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dst: Vector4f): Vector4f {
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy - zw)) * y + (2f * (xz + yw)) * z,
            (2f * (xy + zw)) * x + (-2f * (xx + zz) + 1f) * y + (2f * (yz - xw)) * z,
            (2f * (xz - yw)) * x + (2f * (yz + xw)) * y + (-2f * (xx + yy) + 1f) * z
        )
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dst: Vector4f): Vector4f {
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy + zw) * y + 2f * (xz - yw) * z),
            2f * (xy - zw) * x + ((-2f * (xx + zz) + 1f) * y + 2f * (yz + xw) * z),
            2f * (xz + yw) * x + (2f * (yz - xw) * y + (-2f * (xx + yy) + 1f) * z)
        )
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
        dst.x = (ww + xx - zz - yy).toDouble()
        dst.y = (xy + zw + zw + xy).toDouble()
        dst.z = (xz - yw + xz - yw).toDouble()
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
        dst.x = (ww + xx - zz - yy).toDouble()
        dst.y = (xy + zw + zw + xy).toDouble()
        dst.z = (xz - yw + xz - yw).toDouble()
        return dst
    }

    fun transformUnitPositiveX(dst: Vector3d): Vector3d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = (1f - yy - yy - zz - zz).toDouble()
        dst.y = (xy + zw + xy + zw).toDouble()
        dst.z = (xz - yw + xz - yw).toDouble()
        return dst
    }

    fun transformUnitPositiveX(dst: Vector4d): Vector4d {
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yw = y * w
        val zw = z * w
        dst.x = (1f - yy - yy - zz - zz).toDouble()
        dst.y = (xy + zw + xy + zw).toDouble()
        dst.z = (xz - yw + xz - yw).toDouble()
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
        dst.x = (-zw + xy - zw + xy).toDouble()
        dst.y = (yy - zz + ww - xx).toDouble()
        dst.z = (yz + yz + xw + xw).toDouble()
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
        dst.x = (-zw + xy - zw + xy).toDouble()
        dst.y = (yy - zz + ww - xx).toDouble()
        dst.z = (yz + yz + xw + xw).toDouble()
        return dst
    }

    fun transformUnitPositiveY(dst: Vector4d): Vector4d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = (xy - zw + xy - zw).toDouble()
        dst.y = (1f - xx - xx - zz - zz).toDouble()
        dst.z = (yz + yz + xw + xw).toDouble()
        return dst
    }

    fun transformUnitPositiveY(dst: Vector3d): Vector3d {
        val xx = x * x
        val zz = z * z
        val xy = x * y
        val yz = y * z
        val xw = x * w
        val zw = z * w
        dst.x = (xy - zw + xy - zw).toDouble()
        dst.y = (1f - xx - xx - zz - zz).toDouble()
        dst.z = (yz + yz + xw + xw).toDouble()
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
        dst.x = (yw + xz + xz + yw).toDouble()
        dst.y = (yz + yz - xw - xw).toDouble()
        dst.z = (zz - yy - xx + ww).toDouble()
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
        dst.x = (yw + xz + xz + yw).toDouble()
        dst.y = (yz + yz - xw - xw).toDouble()
        dst.z = (zz - yy - xx + ww).toDouble()
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector4d): Vector4d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = (xz + yw + xz + yw).toDouble()
        dst.y = (yz + yz - xw - xw).toDouble()
        dst.z = (1f - xx - xx - yy - yy).toDouble()
        return dst
    }

    fun transformUnitPositiveZ(dst: Vector3d): Vector3d {
        val xx = x * x
        val yy = y * y
        val xz = x * z
        val yz = y * z
        val xw = x * w
        val yw = y * w
        dst.x = (xz + yw + xz + yw).toDouble()
        dst.y = (yz + yz - xw - xw).toDouble()
        dst.z = (1f - xx - xx - yy - yy).toDouble()
        return dst
    }

    fun transform(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transform(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverse(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformInverse(vec.x, vec.y, vec.z, dst)
    }

    fun transform(x: Float, y: Float, z: Float, dst: Vector3d): Vector3d {
        return this.transform(x.toDouble(), y.toDouble(), z.toDouble(), dst)
    }

    fun transformInverse(x: Float, y: Float, z: Float, dst: Vector3d): Vector3d {
        return this.transformInverse(x.toDouble(), y.toDouble(), z.toDouble(), dst)
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
        val k = 1f / (xx + yy + zz + ww)
        return dst.set(
            (xx - yy - zz + ww) * k * x + 2f * (xy - zw) * k * y + 2f * (xz + yw) * k * z,
            2f * (xy + zw) * k * x + (yy - xx - zz + ww) * k * y + 2f * (yz - xw) * k * z,
            2f * (xz - yw) * k * x + 2f * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        val n = 1f / lengthSquared()
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
        return dst.set(
            ((xx - yy - zz + ww) * k) * x + ((2f * (xy + zw) * k) * y + (2f * (xz - yw) * k) * z),
            (2f * (xy - zw) * k) * x + (((yy - xx - zz + ww) * k) * y + (2f * (yz + xw) * k) * z),
            (2f * (xz + yw) * k) * x + ((2f * (yz - xw) * k) * y + ((zz - xx - yy + ww) * k) * z)
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
        val k = 1f / (xx + yy + zz + ww)
        return dst.set(
            (xx - yy - zz + ww) * k * x + 2f * (xy - zw) * k * y + 2f * (xz + yw) * k * z,
            2f * (xy + zw) * k * x + (yy - xx - zz + ww) * k * y + 2f * (yz - xw) * k * z,
            2f * (xz - yw) * k * x + 2f * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
        val n = 1f / lengthSquared()
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
        return dst.set(
            ((xx - yy - zz + ww) * k) * x + ((2f * (xy + zw) * k) * y + (2f * (xz - yw) * k) * z),
            (2f * (xy - zw) * k) * x + (((yy - xx - zz + ww) * k) * y + (2f * (yz + xw) * k) * z),
            (2f * (xz + yw) * k) * x + ((2f * (yz - xw) * k) * y + ((zz - xx - yy + ww) * k) * z)
        )
    }

    fun transformUnit(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformInverseUnit(vec: Vector3d, dst: Vector3d): Vector3d {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dst)
    }

    fun transformUnit(x: Float, y: Float, z: Float, dst: Vector3d): Vector3d {
        return this.transformUnit(x.toDouble(), y.toDouble(), z.toDouble(), dst)
    }

    fun transformInverseUnit(x: Float, y: Float, z: Float, dst: Vector3d): Vector3d {
        return this.transformInverseUnit(x.toDouble(), y.toDouble(), z.toDouble(), dst)
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy - zw)) * y + (2f * (xz + yw)) * z,
            (2f * (xy + zw)) * x + (-2f * (xx + zz) + 1f) * y + (2f * (yz - xw)) * z,
            (2f * (xz - yw)) * x + (2f * (yz + xw)) * y + (-2f * (xx + yy) + 1f) * z
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy + zw)) * y + (2f * (xz - yw)) * z,
            (2f * (xy - zw)) * x + (-2f * (xx + zz) + 1f) * y + (2f * (yz + xw)) * z,
            (2f * (xz + yw)) * x + (2f * (yz - xw)) * y + (-2f * (xx + yy) + 1f) * z
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy - zw)) * y + (2f * (xz + yw)) * z,
            (2f * (xy + zw)) * x + (-2f * (xx + zz) + 1f) * y + (2f * (yz - xw)) * z,
            (2f * (xz - yw)) * x + (2f * (yz + xw)) * y + (-2f * (xx + yy) + 1f) * z
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
            (-2f * (yy + zz) + 1f) * x + (2f * (xy + zw)) * y + (2f * (xz - yw)) * z,
            (2f * (xy - zw)) * x + (-2f * (xx + zz) + 1f) * y + (2f * (yz + xw)) * z,
            (2f * (xz + yw)) * x + (2f * (yz - xw)) * y + (-2f * (xx + yy) + 1f) * z
        )
    }

    @JvmOverloads
    fun invert(dst: Quaternionf = this): Quaternionf {
        val invNorm = 1f / lengthSquared()
        dst.x = -x * invNorm
        dst.y = -y * invNorm
        dst.z = -z * invNorm
        dst.w = w * invNorm
        return dst
    }

    @JvmOverloads
    fun div(b: Quaternionf, dst: Quaternionf = this): Quaternionf {
        val invNorm = 1f / lengthSquared()
        val x = -b.x * invNorm
        val y = -b.y * invNorm
        val z = -b.z * invNorm
        val w = b.w * invNorm
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    @JvmOverloads
    fun conjugate(dst: Quaternionf = this): Quaternionf {
        dst.x = -x
        dst.y = -y
        dst.z = -z
        dst.w = w
        return dst
    }

    fun identity(): Quaternionf {
        x = 0f
        y = 0f
        z = 0f
        w = 1f
        return this
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dst: Quaternionf = this): Quaternionf {
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dst: Quaternionf = this): Quaternionf {
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dst: Quaternionf = this): Quaternionf {
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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

    fun getEulerAnglesXYZ(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = atan2(x * w - y * z, 0.5f - x * x - y * y)
        eulerAngles.y = JomlMath.safeAsin(2f * (x * z + y * w))
        eulerAngles.z = atan2(z * w - x * y, 0.5f - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZYX(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = atan2(y * z + w * x, 0.5f - x * x + y * y)
        eulerAngles.y = JomlMath.safeAsin(-2f * (x * z - w * y))
        eulerAngles.z = atan2(x * y + w * z, 0.5f - y * y - z * z)
        return eulerAngles
    }

    fun getEulerAnglesZXY(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = JomlMath.safeAsin(2f * (w * x + y * z))
        eulerAngles.y = atan2(w * y - x * z, 0.5f - y * y - x * x)
        eulerAngles.z = atan2(w * z - x * y, 0.5f - z * z - x * x)
        return eulerAngles
    }

    fun getEulerAnglesYXZ(eulerAngles: Vector3f): Vector3f {
        eulerAngles.x = JomlMath.safeAsin(-2f * (y * z - w * x))
        eulerAngles.y = atan2(x * z + y * w, 0.5f - y * y - x * x)
        eulerAngles.z = atan2(y * x + w * z, 0.5f - x * x - z * z)
        return eulerAngles
    }

    fun lengthSquared(): Float {
        return x * x + y * y + z * z + w * w
    }

    fun rotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Quaternionf {
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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
        val sx = sin(angleX * 0.5f)
        val cx = cos(angleX * 0.5f)
        val sy = sin(angleY * 0.5f)
        val cy = cos(angleY * 0.5f)
        val sz = sin(angleZ * 0.5f)
        val cz = cos(angleZ * 0.5f)
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
    fun slerp(target: Quaternionf, alpha: Float, dst: Quaternionf = this): Quaternionf {
        val cosom = dot(target)
        val absCosom = abs(cosom)
        val scale0: Float
        var scale1: Float
        if (1f - absCosom > 1.0E-6f) {
            val sinSqr = 1f - absCosom * absCosom
            val sinom = JomlMath.invsqrt(sinSqr)
            val omega = atan2(sinSqr * sinom, absCosom)
            scale0 = (sin((1.0 - alpha.toDouble()) * omega.toDouble()) * sinom.toDouble()).toFloat()
            scale1 = sin(alpha * omega) * sinom
        } else {
            scale0 = 1f - alpha
            scale1 = alpha
        }
        scale1 = if (cosom >= 0f) scale1 else -scale1
        dst.x = scale0 * x + scale1 * target.x
        dst.y = scale0 * y + scale1 * target.y
        dst.z = scale0 * z + scale1 * target.z
        dst.w = scale0 * w + scale1 * target.w
        return dst
    }

    @JvmOverloads
    fun scale(factor: Float, dst: Quaternionf = this): Quaternionf {
        val sqrt = sqrt(factor)
        dst.x = sqrt * x
        dst.y = sqrt * y
        dst.z = sqrt * z
        dst.w = sqrt * w
        return dst
    }

    fun scaling(factor: Float): Quaternionf {
        val sqrt = sqrt(factor)
        x = 0f
        y = 0f
        z = 0f
        w = sqrt
        return this
    }

    @JvmOverloads
    fun integrate(dt: Float, vx: Float, vy: Float, vz: Float, dst: Quaternionf = this): Quaternionf {
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
            val thetaMag = sqrt(thetaMagSq)
            s = sin(thetaMag) / thetaMag
            dqW = cos(thetaMag)
        }
        val dqX = thetaX * s
        val dqY = thetaY * s
        val dqZ = thetaZ * s
        return dst.set(
            dqW * x + dqX * w + dqY * z - dqZ * y,
            dqW * y - dqX * z + dqY * w + dqZ * x,
            dqW * z + dqX * y - dqY * x + dqZ * w,
            dqW * w - dqX * x - dqY * y - dqZ * z
        )
    }

    @JvmOverloads
    fun nlerp(q: Quaternionf, factor: Float, dst: Quaternionf = this): Quaternionf {
        val cosom = dot(q)
        val scale0 = 1f - factor
        val scale1 = if (cosom >= 0f) factor else -factor
        dst.x = scale0 * x + scale1 * q.x
        dst.y = scale0 * y + scale1 * q.y
        dst.z = scale0 * z + scale1 * q.z
        dst.w = scale0 * w + scale1 * q.w
        val s = JomlMath.invsqrt(dst.lengthSquared())
        dst.x *= s
        dst.y *= s
        dst.z *= s
        dst.w *= s
        return dst
    }

    @JvmOverloads
    fun nlerpIterative(q: Quaternionf, alpha: Float, dotThreshold: Float, dst: Quaternionf = this): Quaternionf {
        var q1x = x
        var q1y = y
        var q1z = z
        var q1w = w
        var q2x = q.x
        var q2y = q.y
        var q2z = q.z
        var q2w = q.w
        var dot = dot(q)
        var absDot = abs(dot)
        return if (0.999999f < absDot) {
            dst.set(this)
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
                    q2x = scale0 * q2x + scale1 * q1x
                    q2y = scale0 * q2y + scale1 * q1y
                    q2z = scale0 * q2z + scale1 * q1z
                    q2w = scale0 * q2w + scale1 * q1w
                    s = JomlMath.invsqrt(q2x * q2x + q2y * q2y + q2z * q2z + q2w * q2w)
                    q2x *= s
                    q2y *= s
                    q2z *= s
                    q2w *= s
                    alphaN += alphaN
                } else {
                    q1x = scale0 * q1x + scale1 * q2x
                    q1y = scale0 * q1y + scale1 * q2y
                    q1z = scale0 * q1z + scale1 * q2z
                    q1w = scale0 * q1w + scale1 * q2w
                    s = JomlMath.invsqrt(q1x * q1x + q1y * q1y + q1z * q1z + q1w * q1w)
                    q1x *= s
                    q1y *= s
                    q1z *= s
                    q1w *= s
                    alphaN = alphaN + alphaN - 1f
                }
                dot = q1x * q2x + q1y * q2y + q1z * q2z + q1w * q2w
                absDot = abs(dot)
            }
            scale0 = 1f - alphaN
            scale1 = if (dot >= 0f) alphaN else -alphaN
            s = scale0 * q1x + scale1 * q2x
            val resY = scale0 * q1y + scale1 * q2y
            val resZ = scale0 * q1z + scale1 * q2z
            val resW = scale0 * q1w + scale1 * q2w
            s = JomlMath.invsqrt(s * s + resY * resY + resZ * resZ + resW * resW)
            dst.x = s * s
            dst.y = resY * s
            dst.z = resZ * s
            dst.w = resW * s
            dst
        }
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Quaternionf {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dst: Quaternionf): Quaternionf {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Quaternionf = this
    ): Quaternionf {
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
        val tr = (leftX + upnY + dirnZ).toDouble()
        val x: Float
        val y: Float
        val z: Float
        val w: Float
        var t: Double
        if (tr >= 0.0) {
            t = sqrt(tr + 1.0)
            w = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((dirnY - upnZ) * t).toFloat()
            y = ((leftZ - dirnX) * t).toFloat()
            z = ((upnX - leftY) * t).toFloat()
        } else if (leftX > upnY && leftX > dirnZ) {
            t = sqrt(1.0 + leftX.toDouble() - upnY.toDouble() - dirnZ.toDouble())
            x = (t * 0.5).toFloat()
            t = 0.5 / t
            y = ((leftY + upnX) * t).toFloat()
            z = ((dirnX + leftZ) * t).toFloat()
            w = ((dirnY - upnZ) * t).toFloat()
        } else if (upnY > dirnZ) {
            t = sqrt(1.0 + upnY.toDouble() - leftX.toDouble() - dirnZ.toDouble())
            y = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((leftY + upnX) * t).toFloat()
            z = ((upnZ + dirnY) * t).toFloat()
            w = ((leftZ - dirnX) * t).toFloat()
        } else {
            t = sqrt(1.0 + dirnZ.toDouble() - leftX.toDouble() - upnY.toDouble())
            z = (t * 0.5).toFloat()
            t = 0.5 / t
            x = ((dirnX + leftZ) * t).toFloat()
            y = ((upnZ + dirnY) * t).toFloat()
            w = ((upnX - leftY) * t).toFloat()
        }
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
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
        val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
        val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
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
            val sd2 = sqrt((1f + dot) * 2f)
            val isd2 = 1f / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5f
            val n2 = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
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
        dst: Quaternionf = this
    ): Quaternionf {
        val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
        val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
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
            val sd2 = sqrt((1f + dot) * 2f)
            val isd2 = 1f / sd2
            val cx = fy * tz - fz * ty
            val cy = fz * tx - fx * tz
            val cz = fx * ty - fy * tx
            x = cx * isd2
            y = cy * isd2
            z = cz * isd2
            w = sd2 * 0.5f
            val n2 = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
            x *= n2
            y *= n2
            z *= n2
            w *= n2
        }
        return dst.set(
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    fun rotateTo(fromDir: Vector3f, toDir: Vector3f, dst: Quaternionf): Quaternionf {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, dst)
    }

    fun rotateTo(fromDir: Vector3f, toDir: Vector3f): Quaternionf {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, this)
    }

    @JvmOverloads
    fun rotateX(angle: Float, dst: Quaternionf = this): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
        return dst.set(w * sin + x * cos, y * cos + z * sin, z * cos - y * sin, w * cos - x * sin)
    }

    @JvmOverloads
    fun rotateY(angle: Float, dst: Quaternionf = this): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
        return dst.set(x * cos - z * sin, w * sin + y * cos, x * sin + z * cos, w * cos - y * sin)
    }

    @JvmOverloads
    fun rotateZ(angle: Float, dst: Quaternionf = this): Quaternionf {
        val sin = sin(angle * 0.5f)
        val cos = cos(angle * 0.5f)
        return dst.set(x * cos + y * sin, y * cos - x * sin, w * sin + z * cos, w * cos - z * sin)
    }

    @JvmOverloads
    fun rotateLocalX(angle: Float, dst: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x + s * w, c * y - s * z, c * z + s * y, c * w - s * x)
        return dst
    }

    @JvmOverloads
    fun rotateLocalY(angle: Float, dst: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x + s * z, c * y + s * w, c * z - s * x, c * w - s * y)
        return dst
    }

    @JvmOverloads
    fun rotateLocalZ(angle: Float, dst: Quaternionf = this): Quaternionf {
        val halfAngle = angle * 0.5f
        val s = sin(halfAngle)
        val c = cos(halfAngle)
        dst.set(c * x - s * y, c * y + s * x, c * z + s * w, c * w - s * z)
        return dst
    }

    @JvmOverloads
    fun rotateAxis(angle: Float, axisX: Float, axisY: Float, axisZ: Float, dst: Quaternionf = this): Quaternionf {
        val halfAngle = angle / 2f
        val sinAngle = sin(halfAngle)
        val invVLength = JomlMath.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        val rx = axisX * invVLength * sinAngle
        val ry = axisY * invVLength * sinAngle
        val rz = axisZ * invVLength * sinAngle
        val rw = cos(halfAngle)
        return dst.set(
            w * rx + x * rw + y * rz - z * ry,
            w * ry - x * rz + y * rw + z * rx,
            w * rz + x * ry - y * rx + z * rw,
            w * rw - x * rx - y * ry - z * rz
        )
    }

    fun rotateAxis(angle: Float, axis: Vector3f, dst: Quaternionf): Quaternionf {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, dst)
    }

    fun rotateAxis(angle: Float, axis: Vector3f): Quaternionf {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, this)
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + (w).toBits()
        result = 31 * result + (x).toBits()
        result = 31 * result + (y).toBits()
        result = 31 * result + (z).toBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is Quaternionf) false
        else x == other.x && y == other.y && z == other.z && w == other.w
    }

    @JvmOverloads
    fun difference(other: Quaternionf, dst: Quaternionf = this): Quaternionf {
        val invNorm = 1f / lengthSquared()
        val x = -x * invNorm
        val y = -y * invNorm
        val z = -z * invNorm
        val w = w * invNorm
        dst.set(
            w * other.x + x * other.w + y * other.z - z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y - z * other.z
        )
        return dst
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
    fun conjugateBy(q: Quaternionf, dst: Quaternionf = this): Quaternionf {
        val invNorm = 1f / q.lengthSquared()
        val qix = -q.x * invNorm
        val qiy = -q.y * invNorm
        val qiz = -q.z * invNorm
        val qiw = q.w * invNorm
        val qpx = q.w * x + q.x * w + q.y * z - q.z * y
        val qpy = q.w * y + -q.x * z + q.y * w + q.z * x
        val qpz = q.w * z + q.x * y + -q.y * x + q.z * w
        val qpw = q.w * w + -q.x * x + -q.y * y - q.z * z
        return dst.set(
            qpw * qix + qpx * qiw + qpy * qiz - qpz * qiy,
            qpw * qiy - qpx * qiz + qpy * qiw + qpz * qix,
            qpw * qiz + qpx * qiy - qpy * qix + qpz * qiw,
            qpw * qiw - qpx * qix - qpy * qiy - qpz * qiz
        )
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    fun equals(q: Quaternionf?, delta: Float): Boolean {
        if (this === q) return true
        return q != null &&
                Runtime.equals(x, q.x, delta) && Runtime.equals(y, q.y, delta) &&
                Runtime.equals(z, q.z, delta) && Runtime.equals(w, q.w, delta)
    }

    fun equals(vx: Float, vy: Float, vz: Float, vw: Float): Boolean {
        return x == vx && y == vy && z == vz && w == vw
    }

    fun toEulerAnglesDegrees(dst: Vector3f = Vector3f()): Vector3f {
        return toEulerAnglesRadians(dst).mul((180.0 / PI).toFloat())
    }

    fun toEulerAnglesRadians(dst: Vector3f = Vector3f()): Vector3f {
        return getEulerAnglesYXZ(dst)
    }

    companion object {
        @JvmStatic
        fun slerp(qs: Array<Quaternionf>, weights: FloatArray, dst: Quaternionf): Quaternionf {
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
        fun nlerp(qs: Array<Quaternionf>, weights: FloatArray, dst: Quaternionf): Quaternionf {
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
            qs: Array<Quaternionf>,
            weights: FloatArray,
            dotThreshold: Float,
            dst: Quaternionf
        ): Quaternionf {
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