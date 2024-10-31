package org.joml

import org.joml.JomlMath.hash
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Quaterniond(
    @JvmField var x: Double,
    @JvmField var y: Double,
    @JvmField var z: Double,
    @JvmField var w: Double
) : Vector {

    constructor() : this(0.0, 0.0, 0.0, 1.0)

    constructor(source: Quaterniond) : this(source.x, source.y, source.z, source.w)
    constructor(source: Quaternionf) : this(
        source.x.toDouble(),
        source.y.toDouble(),
        source.z.toDouble(),
        source.w.toDouble()
    )

    constructor(axisAngle: AxisAngle4f) : this() {
        val s = sin(axisAngle.angle.toDouble() * 0.5)
        x = axisAngle.x.toDouble() * s
        y = axisAngle.y.toDouble() * s
        z = axisAngle.z.toDouble() * s
        w = cos(axisAngle.angle.toDouble() * 0.5)
    }

    constructor(axisAngle: AxisAngle4d) : this() {
        val s = sin(axisAngle.angle * 0.5)
        x = axisAngle.x * s
        y = axisAngle.y * s
        z = axisAngle.z * s
        w = cos(axisAngle.angle * 0.5)
    }

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = when (i) {
        0 -> x
        1 -> y
        2 -> z
        else -> w
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> x = v
            1 -> y = v
            2 -> z = v
            else -> w = v
        }
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z
    operator fun component4() = w

    @JvmOverloads
    fun normalize(dst: Quaterniond = this): Quaterniond {
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

    @JvmOverloads
    fun add(q2: Quaterniond, dst: Quaterniond = this): Quaterniond {
        return add(q2.x, q2.y, q2.z, q2.w, dst)
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
        // allocation is fine here, because d->f
        return dst.set(get(AxisAngle4d()))
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
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): Quaterniond {
        val lenX = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val lenY = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val lenZ = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return setFromNormalized(
            m00 * lenX, m01 * lenX, m02 * lenX,
            m10 * lenY, m11 * lenY, m12 * lenY,
            m20 * lenZ, m21 * lenZ, m22 * lenZ
        )
    }

    private fun setFromNormalized(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): Quaterniond {
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
        return this
    }

    fun setFromUnnormalized(mat: Matrix4f): Quaterniond {
        return setFromUnnormalized(
            mat.m00.toDouble(), mat.m01.toDouble(), mat.m02.toDouble(),
            mat.m10.toDouble(), mat.m11.toDouble(), mat.m12.toDouble(),
            mat.m20.toDouble(), mat.m21.toDouble(), mat.m22.toDouble()
        )
    }

    fun setFromUnnormalized(mat: Matrix4x3f): Quaterniond {
        return setFromUnnormalized(
            mat.m00.toDouble(), mat.m01.toDouble(), mat.m02.toDouble(),
            mat.m10.toDouble(), mat.m11.toDouble(), mat.m12.toDouble(),
            mat.m20.toDouble(), mat.m21.toDouble(), mat.m22.toDouble()
        )
    }

    fun setFromUnnormalized(mat: Matrix4x3d): Quaterniond {
        return setFromUnnormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
    }

    fun setFromNormalized(mat: Matrix4f): Quaterniond {
        return setFromNormalized(
            mat.m00.toDouble(), mat.m01.toDouble(), mat.m02.toDouble(),
            mat.m10.toDouble(), mat.m11.toDouble(), mat.m12.toDouble(),
            mat.m20.toDouble(), mat.m21.toDouble(), mat.m22.toDouble()
        )
    }

    fun setFromNormalized(mat: Matrix4x3f): Quaterniond {
        return setFromNormalized(
            mat.m00.toDouble(), mat.m01.toDouble(), mat.m02.toDouble(),
            mat.m10.toDouble(), mat.m11.toDouble(), mat.m12.toDouble(),
            mat.m20.toDouble(), mat.m21.toDouble(), mat.m22.toDouble()
        )
    }

    fun setFromNormalized(mat: Matrix4x3d): Quaterniond {
        return setFromNormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
    }

    fun setFromUnnormalized(mat: Matrix4d): Quaterniond {
        return setFromUnnormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
    }

    fun setFromNormalized(mat: Matrix4d): Quaterniond {
        return setFromNormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
    }

    fun setFromUnnormalized(mat: Matrix3f): Quaterniond {
        return setFromUnnormalized(
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
    }

    fun setFromNormalized(mat: Matrix3f): Quaterniond {
        return setFromNormalized(
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
    }

    fun setFromUnnormalized(mat: Matrix3d): Quaterniond {
        return setFromUnnormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
    }

    fun setFromNormalized(mat: Matrix3d): Quaterniond {
        return setFromNormalized(
            mat.m00, mat.m01, mat.m02,
            mat.m10, mat.m11, mat.m12,
            mat.m20, mat.m21, mat.m22
        )
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
    fun mul(qx: Float, qy: Float, qz: Float, qw: Float, dst: Quaterniond = this): Quaterniond {
        return mul(qx.toDouble(), qy.toDouble(), qz.toDouble(), qw.toDouble(), dst)
    }

    @JvmOverloads
    fun mul(qx: Double, qy: Double, qz: Double, qw: Double, dst: Quaterniond = this): Quaterniond {
        return dst.set(
            w * qx + x * qw + y * qz - z * qy,
            w * qy - x * qz + y * qw + z * qx,
            w * qz + x * qy - y * qx + z * qw,
            w * qw - x * qx - y * qy - z * qz
        )
    }

    @JvmOverloads
    fun premul(q: Quaterniond, dst: Quaterniond = this): Quaterniond {
        return this.premul(q.x, q.y, q.z, q.w, dst)
    }

    @JvmOverloads
    fun premul(qx: Double, qy: Double, qz: Double, qw: Double, dst: Quaterniond = this): Quaterniond {
        return dst.set(
            qw * x + qx * w + qy * z - qz * y,
            qw * y - qx * z + qy * w + qz * x,
            qw * z + qx * y - qy * x + qz * w,
            qw * w - qx * x - qy * y - qz * z
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

    fun transform(vx: Double, vy: Double, vz: Double, dst: Vector3d): Vector3d {
        return transform(x, y, z, w, vx, vy, vz, dst)
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        val n = 1.0 / lengthSquared()
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy + zw) * k * y + 2.0 * (xz - yw) * k * z),
            2.0 * (xy - zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz + xw) * k * z),
            2.0 * (xz + yw) * k * x + (2.0 * (yz - xw) * k * y + (zz - xx - yy + ww) * k * z)
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy - zw) * k * y + 2.0 * (xz + yw) * k * z),
            2.0 * (xy + zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz - xw) * k * z),
            2.0 * (xz - yw) * k * x + (2.0 * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z),
            dst.w
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector4d): Vector4d {
        val n = 1.0 / lengthSquared()
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy + zw) * k * y + 2.0 * (xz - yw) * k * z),
            2.0 * (xy - zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz + xw) * k * z),
            2.0 * (xz + yw) * k * x + (2.0 * (yz - xw) * k * y + (zz - xx - yy + ww) * k * z)
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
            (-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy - zw) * y + 2.0 * (xz + yw) * z),
            2.0 * (xy + zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz - xw) * z),
            2.0 * (xz - yw) * x + (2.0 * (yz + xw) * y + (-2.0 * (xx + yy) + 1.0) * z)
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
            (-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy + zw) * y + 2.0 * (xz - yw) * z),
            2.0 * (xy - zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz + xw) * z),
            2.0 * (xz + yw) * x + (2.0 * (yz - xw) * y + (-2.0 * (xx + yy) + 1.0) * z)
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
            (-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy - zw) * y + 2.0 * (xz + yw) * z),
            2.0 * (xy + zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz - xw) * z),
            2.0 * (xz - yw) * x + (2.0 * (yz + xw) * y + (-2.0 * (xx + yy) + 1.0) * z),
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
            (-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy + zw) * y + 2.0 * (xz - yw) * z),
            2.0 * (xy - zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz + xw) * z),
            2.0 * (xz + yw) * x + (2.0 * (yz - xw) * y + (-2.0 * (xx + yy) + 1.0) * z),
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy - zw) * k * y + 2.0 * (xz + yw) * k * z),
            2.0 * (xy + zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz - xw) * k * z),
            2.0 * (xz - yw) * k * x + (2.0 * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z)
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector3f): Vector3f {
        val n = 1.0 / lengthSquared()
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy + zw) * k * y + 2.0 * (xz - yw) * k * z),
            2.0 * (xy - zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz + xw) * k * z),
            2.0 * (xz + yw) * k * x + (2.0 * (yz - xw) * k * y + (zz - xx - yy + ww) * k * z)
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
            ((xx - yy - zz + ww) * k * x + (2.0 * (xy - zw) * k * y + 2.0 * (xz + yw) * k * z)).toFloat(),
            (2.0 * (xy + zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz - xw) * k * z)).toFloat(),
            (2.0 * (xz - yw) * k * x + (2.0 * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z)).toFloat(),
            dst.w
        )
    }

    fun transformInverse(x: Double, y: Double, z: Double, dst: Vector4f): Vector4f {
        val n = 1.0 / lengthSquared()
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
            (xx - yy - zz + ww) * k * x + (2.0 * (xy + zw) * k * y + 2.0 * (xz - yw) * k * z),
            2.0 * (xy - zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz + xw) * k * z),
            2.0 * (xz + yw) * k * x + (2.0 * (yz - xw) * k * y + (zz - xx - yy + ww) * k * z),
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
            ((-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy - zw) * y + 2.0 * (xz + yw) * z)).toFloat(),
            (2.0 * (xy + zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz - xw) * z)).toFloat(),
            (2.0 * (xz - yw) * x + (2.0 * (yz + xw) * y + (-2.0 * (xx + yy) + 1.0) * z)).toFloat()
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
            ((-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy + zw) * y + 2.0 * (xz - yw) * z)).toFloat(),
            (2.0 * (xy - zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz + xw) * z)).toFloat(),
            (2.0 * (xz + yw) * x + (2.0 * (yz - xw) * y + (-2.0 * (xx + yy) + 1.0) * z)).toFloat()
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
            ((-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy - zw) * y + 2.0 * (xz + yw) * z)).toFloat(),
            (2.0 * (xy + zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz - xw) * z)).toFloat(),
            (2.0 * (xz - yw) * x + (2.0 * (yz + xw) * y + (-2.0 * (xx + yy) + 1.0) * z)).toFloat()
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
            ((-2.0 * (yy + zz) + 1.0) * x + (2.0 * (xy + zw) * y + 2.0 * (xz - yw) * z)).toFloat(),
            (2.0 * (xy - zw) * x + ((-2.0 * (xx + zz) + 1.0) * y + 2.0 * (yz + xw) * z)).toFloat(),
            (2.0 * (xz + yw) * x + (2.0 * (yz - xw) * y + (-2.0 * (xx + yy) + 1.0) * z)).toFloat()
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
        val invNorm = 1.0 / b.lengthSquared()
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
        return x * x + y * y + z * z + w * w
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
        val cosom = dot(target)
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
        dst.x = scale0 * x + scale1 * target.x
        dst.y = scale0 * y + scale1 * target.y
        dst.z = scale0 * z + scale1 * target.z
        dst.w = scale0 * w + scale1 * target.w
        return dst
    }

    @JvmOverloads
    fun scale(factor: Double, dst: Quaterniond = this): Quaterniond {
        val sqrt = sqrt(factor)
        return dst.set(sqrt * x, sqrt * y, sqrt * z, sqrt * w)
    }

    fun scaling(factor: Double): Quaterniond {
        return set(0.0, 0.0, 0.0, sqrt(factor))
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
            dqW * x + dqX * w + dqY * z - dqZ * y,
            dqW * y - dqX * z + dqY * w + dqZ * x,
            dqW * z + dqX * y - dqY * x + dqZ * w,
            dqW * w - dqX * x - dqY * y - dqZ * z
        )
    }

    @JvmOverloads
    fun nlerp(q: Quaterniond, factor: Double, dst: Quaterniond = this): Quaterniond {
        val cosom = dot(q)
        val scale0 = 1.0 - factor
        val scale1 = if (cosom >= 0.0) factor else -factor
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
    fun nlerpIterative(q: Quaterniond, alpha: Double, dotThreshold: Double, dst: Quaterniond = this): Quaterniond {
        var q1x = x
        var q1y = y
        var q1z = z
        var q1w = w
        var q2x = q.x
        var q2y = q.y
        var q2z = q.z
        var q2w = q.w
        var dot = q1x * q2x + q1y * q2y + q1z * q2z + q1w * q2w
        var absDot = abs(dot)
        if (1.0 - 1E-6 < absDot) {
            return dst.set(this)
        }
        var alphaN = alpha
        while (absDot < dotThreshold) {
            val scale0 = 0.5
            val scale1 = if (dot >= 0.0) 0.5 else -0.5
            if (alphaN < 0.5) {
                q2x = scale0 * q2x + scale1 * q1x
                q2y = scale0 * q2y + scale1 * q1y
                q2z = scale0 * q2z + scale1 * q1z
                q2w = scale0 * q2w + scale1 * q1w
                val s = JomlMath.invsqrt(q2x * q2x + q2y * q2y + q2z * q2z + q2w * q2w)
                q2x *= s
                q2y *= s
                q2z *= s
                q2w *= s
                alphaN = alphaN + alphaN
            } else {
                q1x = scale0 * q1x + scale1 * q2x
                q1y = scale0 * q1y + scale1 * q2y
                q1z = scale0 * q1z + scale1 * q2z
                q1w = scale0 * q1w + scale1 * q2w
                val s = JomlMath.invsqrt(q1x * q1x + q1y * q1y + q1z * q1z + q1w * q1w)
                q1x *= s
                q1y *= s
                q1z *= s
                q1w *= s
                alphaN = alphaN + alphaN - 1.0
            }
            dot = q1x * q2x + q1y * q2y + q1z * q2z + q1w * q2w
            absDot = abs(dot)
        }
        val scale0 = 1.0 - alphaN
        val scale1 = if (dot >= 0.0) alphaN else -alphaN
        val resX = scale0 * q1x + scale1 * q2x
        val resY = scale0 * q1y + scale1 * q2y
        val resZ = scale0 * q1z + scale1 * q2z
        val resW = scale0 * q1w + scale1 * q2w
        val s = JomlMath.invsqrt(resX * resX + resY * resY + resZ * resZ + resW * resW)
        dst.x = resX * s
        dst.y = resY * s
        dst.z = resZ * s
        dst.w = resW * s
        return dst
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
            this.w * x + this.x * w + this.y * z - this.z * y,
            this.w * y - this.x * z + this.y * w + this.z * x,
            this.w * z + this.x * y - this.y * x + this.z * w,
            this.w * w - this.x * x - this.y * y - this.z * z
        )
    }

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + hash(x)
        result = 31 * result + hash(y)
        result = 31 * result + hash(z)
        result = 31 * result + hash(w)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Quaterniond && equals(other.x, other.y, other.z, other.w)
    }

    @JvmOverloads
    fun difference(other: Quaterniond, dst: Quaterniond = this): Quaterniond {
        val invNorm = 1.0 / lengthSquared()
        val x = -x * invNorm
        val y = -y * invNorm
        val z = -z * invNorm
        val w = w * invNorm
        dst.set(
            w * other.x + x * other.w + y * other.z + -z * other.y,
            w * other.y - x * other.z + y * other.w + z * other.x,
            w * other.z + x * other.y - y * other.x + z * other.w,
            w * other.w - x * other.x - y * other.y + -z * other.z
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
        val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
        val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
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
            val n2 = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
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
        val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
        val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
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
        return set(sin, 0.0, 0.0, cos)
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
        eulerAngles.x = atan2(y * z + w * x, 0.5 - x * x - y * y);
        eulerAngles.y = JomlMath.safeAsin(-2.0 * (x * z - w * y));
        eulerAngles.z = atan2(x * y + w * z, 0.5 - y * y - z * z);
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

    @JvmOverloads
    fun rotateAxis(angle: Double, axis: Vector3d, dst: Quaterniond = this): Quaterniond {
        return rotateAxis(angle, axis.x, axis.y, axis.z, dst)
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
        val qpx = q.w * x + q.x * w + q.y * z - q.z * y
        val qpy = q.w * y - q.x * z + q.y * w + q.z * x
        val qpz = q.w * z + q.x * y - q.y * x + q.z * w
        val qpw = q.w * w - q.x * x - q.y * y - q.z * z
        return dst.set(
            qpw * qix + qpx * qiw + qpy * qiz - qpz * qiy,
            qpw * qiy - qpx * qiz + qpy * qiw + qpz * qix,
            qpw * qiz + qpx * qiy - qpy * qix + qpz * qiw,
            qpw * qiw - qpx * qix - qpy * qiy - qpz * qiz
        )
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    fun equals(q: Quaterniond?, delta: Double): Boolean {
        return q != null && (strictEquals(q.x, q.y, q.z, q.w, delta) || strictEquals(-q.x, -q.y, -q.z, -q.w, delta))
    }

    private fun strictEquals(qx: Double, qy: Double, qz: Double, qw: Double, delta: Double): Boolean {
        return Runtime.equals(x, qx, delta) &&
                Runtime.equals(y, qy, delta) &&
                Runtime.equals(z, qz, delta) &&
                Runtime.equals(w, qw, delta)
    }

    fun equals(x: Double, y: Double, z: Double, w: Double): Boolean {
        return strictEquals(x, y, z, w) || strictEquals(-x, -y, -z, -w)
    }

    private fun strictEquals(qx: Double, qy: Double, qz: Double, qw: Double): Boolean {
        return x == qx && y == qy && z == qz && w == qw
    }

    fun toEulerAnglesDegrees(dst: Vector3d = Vector3d()): Vector3d {
        return toEulerAnglesRadians(dst).mul(180.0 / PI)
    }

    fun toEulerAnglesRadians(dst: Vector3d = Vector3d()): Vector3d {
        return getEulerAnglesYXZ(dst)
    }

    fun mul(q: Quaternionf): Quaterniond {// why ever this function is missing :annoyed:
        return mul(q.x.toDouble(), q.y.toDouble(), q.z.toDouble(), q.w.toDouble())
    }

    companion object {

        @JvmStatic
        fun transform(
            tx: Double, ty: Double, tz: Double, tw: Double,
            x: Double, y: Double, z: Double, dst: Vector3d
        ): Vector3d {
            val xx = tx * tx
            val yy = ty * ty
            val zz = tz * tz
            val ww = tw * tw
            val xy = tx * ty
            val xz = tx * tz
            val yz = ty * tz
            val xw = tx * tw
            val zw = tz * tw
            val yw = ty * tw
            val k = 1.0 / (xx + yy + zz + ww)
            return dst.set(
                (xx - yy - zz + ww) * k * x + (2.0 * (xy - zw) * k * y + 2.0 * (xz + yw) * k * z),
                2.0 * (xy + zw) * k * x + ((yy - xx - zz + ww) * k * y + 2.0 * (yz - xw) * k * z),
                2.0 * (xz - yw) * k * x + (2.0 * (yz + xw) * k * y + (zz - xx - yy + ww) * k * z)
            )
        }

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