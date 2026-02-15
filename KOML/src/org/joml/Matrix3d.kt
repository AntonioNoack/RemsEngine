package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix3d : Matrix<Matrix3d, Vector3d, Vector3d> {

    var m00 = 0.0
    var m01 = 0.0
    var m02 = 0.0
    var m10 = 0.0
    var m11 = 0.0
    var m12 = 0.0
    var m20 = 0.0
    var m21 = 0.0
    var m22 = 0.0

    constructor() {
        m00 = 1.0
        m11 = 1.0
        m22 = 1.0
    }

    constructor(mat: Matrix2d) {
        this.set(mat)
    }

    constructor(mat: Matrix3d) {
        this.set(mat)
    }

    constructor(mat: Matrix3f) {
        this.set(mat)
    }

    constructor(mat: Matrix4d) {
        this.set(mat)
    }

    constructor(
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
        this.m00 = m00
        this.m01 = m01
        this.m02 = m02
        this.m10 = m10
        this.m11 = m11
        this.m12 = m12
        this.m20 = m20
        this.m21 = m21
        this.m22 = m22
    }

    constructor(col0: Vector3d, col1: Vector3d, col2: Vector3d) {
        set(col0, col1, col2)
    }

    override val numCols: Int get() = 3
    override val numRows: Int get() = 3

    fun _m00(m00: Double): Matrix3d {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Double): Matrix3d {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Double): Matrix3d {
        this.m02 = m02
        return this
    }

    fun _m10(m10: Double): Matrix3d {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Double): Matrix3d {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Double): Matrix3d {
        this.m12 = m12
        return this
    }

    fun _m20(m20: Double): Matrix3d {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Double): Matrix3d {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Double): Matrix3d {
        this.m22 = m22
        return this
    }

    fun set(m: Matrix3d): Matrix3d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix3f): Matrix3d {
        return set(
            m.m00.toDouble(), m.m01.toDouble(), m.m02.toDouble(),
            m.m10.toDouble(), m.m11.toDouble(), m.m12.toDouble(),
            m.m20.toDouble(), m.m21.toDouble(), m.m22.toDouble()
        )
    }

    fun set(m: Matrix4x3d): Matrix3d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4x3): Matrix3d {
        return set(
            m.m00.toDouble(), m.m01.toDouble(), m.m02.toDouble(),
            m.m10.toDouble(), m.m11.toDouble(), m.m12.toDouble(),
            m.m20.toDouble(), m.m21.toDouble(), m.m22.toDouble()
        )
    }

    fun set(mat: Matrix4d): Matrix3d {
        m00 = mat.m00
        m01 = mat.m01
        m02 = mat.m02
        m10 = mat.m10
        m11 = mat.m11
        m12 = mat.m12
        m20 = mat.m20
        m21 = mat.m21
        m22 = mat.m22
        return this
    }

    fun set(mat: Matrix2d): Matrix3d {
        return set(
            mat.m00, mat.m01, 0.0,
            mat.m10, mat.m11, 0.0,
            0.0, 0.0, 1.0
        )
    }

    fun setTransposed(m: Matrix3d): Matrix3d {
        val nm10 = m.m01
        val nm12 = m.m21
        val nm20 = m.m02
        val nm21 = m.m12
        return _m00(m.m00)._m01(m.m10)._m02(m.m20)._m10(nm10)._m11(m.m11)._m12(nm12)._m20(nm20)._m21(nm21)._m22(m.m22)
    }

    fun set(axisAngle: AxisAngle4f): Matrix3d {
        var x = axisAngle.x.toDouble()
        var y = axisAngle.y.toDouble()
        var z = axisAngle.z.toDouble()
        val angle = axisAngle.angle.toDouble()
        val invLength = JomlMath.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        val s = sin(angle)
        val c = cos(angle)
        val omc = 1.0 - c
        m00 = c + x * x * omc
        m11 = c + y * y * omc
        m22 = c + z * z * omc
        var tmp1 = x * y * omc
        var tmp2 = z * s
        m10 = tmp1 - tmp2
        m01 = tmp1 + tmp2
        tmp1 = x * z * omc
        tmp2 = y * s
        m20 = tmp1 + tmp2
        m02 = tmp1 - tmp2
        tmp1 = y * z * omc
        tmp2 = x * s
        m21 = tmp1 - tmp2
        m12 = tmp1 + tmp2
        return this
    }

    fun set(axisAngle: AxisAngle4d): Matrix3d {
        var x = axisAngle.x
        var y = axisAngle.y
        var z = axisAngle.z
        val angle = axisAngle.angle
        val invLength = JomlMath.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        val s = sin(angle)
        val c = cos(angle)
        val omc = 1.0 - c
        m00 = c + x * x * omc
        m11 = c + y * y * omc
        m22 = c + z * z * omc
        var tmp1 = x * y * omc
        var tmp2 = z * s
        m10 = tmp1 - tmp2
        m01 = tmp1 + tmp2
        tmp1 = x * z * omc
        tmp2 = y * s
        m20 = tmp1 + tmp2
        m02 = tmp1 - tmp2
        tmp1 = y * z * omc
        tmp2 = x * s
        m21 = tmp1 - tmp2
        m12 = tmp1 + tmp2
        return this
    }

    fun set(q: Quaterniond): Matrix3d {
        return rotation(q)
    }

    @JvmOverloads
    fun mul(right: Matrix3d, dst: Matrix3d = this): Matrix3d {
        return mul(
            right.m00, right.m01, right.m02, right.m10, right.m11, right.m12,
            right.m20, right.m21, right.m22, dst
        )
    }

    fun mul(
        r00: Double, r01: Double, r02: Double, r10: Double, r11: Double, r12: Double,
        r20: Double, r21: Double, r22: Double, dst: Matrix3d = this
    ): Matrix3d {
        val nm00 = m00 * r00 + m10 * r01 + m20 * r02
        val nm01 = m01 * r00 + m11 * r01 + m21 * r02
        val nm02 = m02 * r00 + m12 * r01 + m22 * r02
        val nm10 = m00 * r10 + m10 * r11 + m20 * r12
        val nm11 = m01 * r10 + m11 * r11 + m21 * r12
        val nm12 = m02 * r10 + m12 * r11 + m22 * r12
        val nm20 = m00 * r20 + m10 * r21 + m20 * r22
        val nm21 = m01 * r20 + m11 * r21 + m21 * r22
        val nm22 = m02 * r20 + m12 * r21 + m22 * r22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    fun mul(other: Matrix4d): Matrix3d {
        return mul(
            other.m00, other.m01, other.m02,
            other.m10, other.m11, other.m12,
            other.m20, other.m21, other.m22
        )
    }

    @JvmOverloads
    fun mulLocal(left: Matrix3d, dst: Matrix3d = this): Matrix3d {
        val nm00 = left.m00 * m00 + left.m10 * m01 + left.m20 * m02
        val nm01 = left.m01 * m00 + left.m11 * m01 + left.m21 * m02
        val nm02 = left.m02 * m00 + left.m12 * m01 + left.m22 * m02
        val nm10 = left.m00 * m10 + left.m10 * m11 + left.m20 * m12
        val nm11 = left.m01 * m10 + left.m11 * m11 + left.m21 * m12
        val nm12 = left.m02 * m10 + left.m12 * m11 + left.m22 * m12
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20 * m22
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21 * m22
        val nm22 = left.m02 * m20 + left.m12 * m21 + left.m22 * m22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun mul(right: Matrix3f, dst: Matrix3d = this): Matrix3d {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    fun set(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): Matrix3d {
        this.m00 = m00
        this.m01 = m01
        this.m02 = m02
        this.m10 = m10
        this.m11 = m11
        this.m12 = m12
        this.m20 = m20
        this.m21 = m21
        this.m22 = m22
        return this
    }

    fun set(m: DoubleArray, i: Int): Matrix3d {
        return set(m[i], m[i + 1], m[i + 2], m[i + 3], m[i + 4], m[i + 5], m[i + 6], m[i + 7], m[i + 8])
    }

    fun determinant(): Double {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dst: Matrix3d = this): Matrix3d {
        val a = m00 * m11 - m01 * m10
        val b = m02 * m10 - m00 * m12
        val c = m01 * m12 - m02 * m11
        val s = 1.0 / (a * m22 + b * m21 + c * m20)
        val nm00 = (m11 * m22 - m21 * m12) * s
        val nm01 = (m21 * m02 - m01 * m22) * s
        val nm02 = c * s
        val nm10 = (m20 * m12 - m10 * m22) * s
        val nm11 = (m00 * m22 - m20 * m02) * s
        val nm12 = b * s
        val nm20 = (m10 * m21 - m20 * m11) * s
        val nm21 = (m20 * m01 - m00 * m21) * s
        val nm22 = a * s
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun transpose(dst: Matrix3d = this): Matrix3d {
        return dst.set(m00, m10, m20, m01, m11, m21, m02, m12, m22)
    }

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)}] " +
                "[${f(m02)} ${f(m12)} ${f(m22)}]]").addSigns()

    fun get(dst: Matrix3d): Matrix3d {
        return dst.set(this)
    }

    fun getRotation(dst: AxisAngle4f): AxisAngle4f {
        return dst.set(this)
    }

    fun getUnnormalizedRotation(dst: Quaterniond): Quaterniond {
        return dst.setFromUnnormalized(this)
    }

    fun getNormalizedRotation(dst: Quaterniond): Quaterniond {
        return dst.setFromNormalized(this)
    }

    @JvmOverloads
    fun get(arr: DoubleArray, offset: Int = 0): DoubleArray {
        arr[offset] = m00
        arr[offset + 1] = m01
        arr[offset + 2] = m02
        arr[offset + 3] = m10
        arr[offset + 4] = m11
        arr[offset + 5] = m12
        arr[offset + 6] = m20
        arr[offset + 7] = m21
        arr[offset + 8] = m22
        return arr
    }

    fun set(col0: Vector3d, col1: Vector3d, col2: Vector3d): Matrix3d {
        return set(col0.x, col0.y, col0.z, col1.x, col1.y, col1.z, col2.x, col2.y, col2.z)
    }

    fun zero(): Matrix3d {
        return scaling(0.0)
    }

    fun identity(): Matrix3d {
        return scaling(1.0)
    }

    fun scaling(factor: Double): Matrix3d {
        return scaling(factor, factor, factor)
    }

    fun scaling(x: Double, y: Double, z: Double): Matrix3d {
        return set(x, 0.0, 0.0, 0.0, y, 0.0, 0.0, 0.0, z)
    }

    fun scaling(v: Vector3d): Matrix3d {
        return scaling(v.x, v.y, v.z)
    }

    @JvmOverloads
    fun scale(xyz: Vector3d, dst: Matrix3d = this): Matrix3d {
        return scale(xyz.x, xyz.y, xyz.z, dst)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, z: Double, dst: Matrix3d = this): Matrix3d {
        return dst.set(
            m00 * x, m01 * x, m02 * x,
            m10 * y, m11 * y, m12 * y,
            m20 * z, m21 * z, m22 * z
        )
    }

    @JvmOverloads
    fun scale(xyz: Double, dst: Matrix3d = this): Matrix3d {
        return scale(xyz, xyz, xyz, dst)
    }

    @JvmOverloads
    fun scaleLocal(scale: Vector3d, dst: Matrix3d = this): Matrix3d {
        return scaleLocal(scale.x, scale.y, scale.z, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, z: Double, dst: Matrix3d = this): Matrix3d {
        return dst.set(
            m00 * x, m01 * y, m02 * z,
            m10 * x, m11 * y, m12 * z,
            m20 * x, m21 * y, m22 * z
        )
    }

    fun rotation(angle: Double, axis: Vector3d): Matrix3d {
        return rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(axisAngle: AxisAngle4d): Matrix3d {
        return rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotation(angle: Double, x: Double, y: Double, z: Double): Matrix3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1.0 - cos
        val xy = x * y
        val xz = x * z
        val yz = y * z
        m00 = cos + x * x * C
        m10 = xy * C - z * sin
        m20 = xz * C + y * sin
        m01 = xy * C + z * sin
        m11 = cos + y * y * C
        m21 = yz * C - x * sin
        m02 = xz * C - y * sin
        m12 = yz * C + x * sin
        m22 = cos + z * z * C
        return this
    }

    fun rotationX(ang: Double): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = 1.0
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = cos
        m12 = sin
        m20 = 0.0
        m21 = -sin
        m22 = cos
        return this
    }

    fun rotationY(ang: Double): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = cos
        m01 = 0.0
        m02 = -sin
        m10 = 0.0
        m11 = 1.0
        m12 = 0.0
        m20 = sin
        m21 = 0.0
        m22 = cos
        return this
    }

    fun rotationZ(ang: Double): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = cos
        m01 = sin
        m02 = 0.0
        m10 = -sin
        m11 = cos
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = 1.0
        return this
    }

    fun rotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinX = -sinX
        val m_sinY = -sinY
        val m_sinZ = -sinZ
        val nm01 = m_sinX * m_sinY
        val nm02 = cosX * m_sinY
        m20 = sinY
        m21 = m_sinX * cosY
        m22 = cosX * cosY
        m00 = cosY * cosZ
        m01 = nm01 * cosZ + cosX * sinZ
        m02 = nm02 * cosZ + sinX * sinZ
        m10 = cosY * m_sinZ
        m11 = nm01 * m_sinZ + cosX * cosZ
        m12 = nm02 * m_sinZ + sinX * cosZ
        return this
    }

    fun rotationZYX(angleZ: Double, angleY: Double, angleX: Double): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinZ = -sinZ
        val m_sinY = -sinY
        val m_sinX = -sinX
        val nm20 = cosZ * sinY
        val nm21 = sinZ * sinY
        m00 = cosZ * cosY
        m01 = sinZ * cosY
        m02 = m_sinY
        m10 = m_sinZ * cosX + nm20 * sinX
        m11 = cosZ * cosX + nm21 * sinX
        m12 = cosY * sinX
        m20 = m_sinZ * m_sinX + nm20 * cosX
        m21 = cosZ * m_sinX + nm21 * cosX
        m22 = cosY * cosX
        return this
    }

    fun rotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinY = -sinY
        val m_sinX = -sinX
        val m_sinZ = -sinZ
        val nm10 = sinY * sinX
        val nm12 = cosY * sinX
        m20 = sinY * cosX
        m21 = m_sinX
        m22 = cosY * cosX
        m00 = cosY * cosZ + nm10 * sinZ
        m01 = cosX * sinZ
        m02 = m_sinY * cosZ + nm12 * sinZ
        m10 = cosY * m_sinZ + nm10 * cosZ
        m11 = cosX * cosZ
        m12 = m_sinY * m_sinZ + nm12 * cosZ
        return this
    }

    fun rotation(quat: Quaterniond): Matrix3d {
        val w2 = quat.w * quat.w
        val x2 = quat.x * quat.x
        val y2 = quat.y * quat.y
        val z2 = quat.z * quat.z
        val zw = quat.z * quat.w
        val dzw = zw + zw
        val xy = quat.x * quat.y
        val dxy = xy + xy
        val xz = quat.x * quat.z
        val dxz = xz + xz
        val yw = quat.y * quat.w
        val dyw = yw + yw
        val yz = quat.y * quat.z
        val dyz = yz + yz
        val xw = quat.x * quat.w
        val dxw = xw + xw
        m00 = w2 + x2 - z2 - y2
        m01 = dxy + dzw
        m02 = dxz - dyw
        m10 = -dzw + dxy
        m11 = y2 - z2 + w2 - x2
        m12 = dyz + dxw
        m20 = dyw + dxz
        m21 = dyz - dxw
        m22 = z2 - y2 - x2 + w2
        return this
    }

    fun transform(v: Vector3d): Vector3d {
        return v.mul(this)
    }

    fun transform(v: Vector3d, dst: Vector3d): Vector3d {
        v.mul(this, dst)
        return dst
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        return dst.set(
            m00 * x + m10 * y + m20 * z,
            m01 * x + m11 * y + m21 * z,
            m02 * x + m12 * y + m22 * z
        )
    }

    fun transformTranspose(v: Vector3d): Vector3d {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector3d, dst: Vector3d): Vector3d {
        return v.mulTranspose(this, dst)
    }

    fun transformTranspose(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        return dst.set(
            m00 * x + m01 * y + m02 * z,
            m10 * x + m11 * y + m12 * z,
            m20 * x + m21 * y + m22 * z
        )
    }

    /**
     * inverts this matrix without saving the result, and then transforming v as a direction
     * */
    fun transformInverse(v: Vector3d, dst: Vector3d = v): Vector3d {
        val a = m00 * m11 - m01 * m10
        val b = m02 * m10 - m00 * m12
        val c = m01 * m12 - m02 * m11
        val nm00 = m11 * m22 - m21 * m12
        val nm01 = m21 * m02 - m01 * m22
        val nm10 = m20 * m12 - m10 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm20 = m10 * m21 - m20 * m11
        val nm21 = m20 * m01 - m00 * m21
        val s = 1.0 / (a * m22 + b * m21 + c * m20)
        if (!s.isFinite()) return dst.set(0.0)

        val rx = v.dot(nm00, nm10, nm20) * s
        val ry = v.dot(nm01, nm11, nm21) * s
        val rz = v.dot(c, b, a) * s
        return dst.set(rx, ry, rz)
    }

    @JvmOverloads
    fun rotateX(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val rm21 = -sin
        val nm10 = m10 * cos + m20 * sin
        val nm11 = m11 * cos + m21 * sin
        val nm12 = m12 * cos + m22 * sin
        dst.m20 = m10 * rm21 + m20 * cos
        dst.m21 = m11 * rm21 + m21 * cos
        dst.m22 = m12 * rm21 + m22 * cos
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m00 = m00
        dst.m01 = m01
        dst.m02 = m02
        return dst
    }

    @JvmOverloads
    fun rotateY(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val rm02 = -sin
        val nm00 = m00 * cos + m20 * rm02
        val nm01 = m01 * cos + m21 * rm02
        val nm02 = m02 * cos + m22 * rm02
        dst.m20 = m00 * sin + m20 * cos
        dst.m21 = m01 * sin + m21 * cos
        dst.m22 = m02 * sin + m22 * cos
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = m10
        dst.m11 = m11
        dst.m12 = m12
        return dst
    }

    @JvmOverloads
    fun rotateZ(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val rm10 = -sin
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        val nm02 = m02 * cos + m12 * sin
        dst.m10 = m00 * rm10 + m10 * cos
        dst.m11 = m01 * rm10 + m11 * cos
        dst.m12 = m02 * rm10 + m12 * cos
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m20 = m20
        dst.m21 = m21
        dst.m22 = m22
        return dst
    }

    @JvmOverloads
    fun rotateXYZ(euler: Vector3d, dst: Matrix3d = this): Matrix3d {
        return rotateXYZ(euler.x, euler.y, euler.z, dst)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Double, angleY: Double, angleZ: Double, dst: Matrix3d = this): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinX = -sinX
        val m_sinY = -sinY
        val m_sinZ = -sinZ
        val nm10 = m10 * cosX + m20 * sinX
        val nm11 = m11 * cosX + m21 * sinX
        val nm12 = m12 * cosX + m22 * sinX
        val nm20 = m10 * m_sinX + m20 * cosX
        val nm21 = m11 * m_sinX + m21 * cosX
        val nm22 = m12 * m_sinX + m22 * cosX
        val nm00 = m00 * cosY + nm20 * m_sinY
        val nm01 = m01 * cosY + nm21 * m_sinY
        val nm02 = m02 * cosY + nm22 * m_sinY
        dst.m20 = m00 * sinY + nm20 * cosY
        dst.m21 = m01 * sinY + nm21 * cosY
        dst.m22 = m02 * sinY + nm22 * cosY
        dst.m00 = nm00 * cosZ + nm10 * sinZ
        dst.m01 = nm01 * cosZ + nm11 * sinZ
        dst.m02 = nm02 * cosZ + nm12 * sinZ
        dst.m10 = nm00 * m_sinZ + nm10 * cosZ
        dst.m11 = nm01 * m_sinZ + nm11 * cosZ
        dst.m12 = nm02 * m_sinZ + nm12 * cosZ
        return dst
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Double, angleY: Double, angleX: Double, dst: Matrix3d = this): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinZ = -sinZ
        val m_sinY = -sinY
        val m_sinX = -sinX
        val nm00 = m00 * cosZ + m10 * sinZ
        val nm01 = m01 * cosZ + m11 * sinZ
        val nm02 = m02 * cosZ + m12 * sinZ
        val nm10 = m00 * m_sinZ + m10 * cosZ
        val nm11 = m01 * m_sinZ + m11 * cosZ
        val nm12 = m02 * m_sinZ + m12 * cosZ
        val nm20 = nm00 * sinY + m20 * cosY
        val nm21 = nm01 * sinY + m21 * cosY
        val nm22 = nm02 * sinY + m22 * cosY
        dst.m00 = nm00 * cosY + m20 * m_sinY
        dst.m01 = nm01 * cosY + m21 * m_sinY
        dst.m02 = nm02 * cosY + m22 * m_sinY
        dst.m10 = nm10 * cosX + nm20 * sinX
        dst.m11 = nm11 * cosX + nm21 * sinX
        dst.m12 = nm12 * cosX + nm22 * sinX
        dst.m20 = nm10 * m_sinX + nm20 * cosX
        dst.m21 = nm11 * m_sinX + nm21 * cosX
        dst.m22 = nm12 * m_sinX + nm22 * cosX
        return dst
    }

    fun rotateYXZ(angles: Vector3d): Matrix3d {
        return rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Double, angleX: Double, angleZ: Double, dst: Matrix3d = this): Matrix3d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinY = -sinY
        val m_sinX = -sinX
        val m_sinZ = -sinZ
        val nm20 = m00 * sinY + m20 * cosY
        val nm21 = m01 * sinY + m21 * cosY
        val nm22 = m02 * sinY + m22 * cosY
        val nm00 = m00 * cosY + m20 * m_sinY
        val nm01 = m01 * cosY + m21 * m_sinY
        val nm02 = m02 * cosY + m22 * m_sinY
        val nm10 = m10 * cosX + nm20 * sinX
        val nm11 = m11 * cosX + nm21 * sinX
        val nm12 = m12 * cosX + nm22 * sinX
        dst.m20 = m10 * m_sinX + nm20 * cosX
        dst.m21 = m11 * m_sinX + nm21 * cosX
        dst.m22 = m12 * m_sinX + nm22 * cosX
        dst.m00 = nm00 * cosZ + nm10 * sinZ
        dst.m01 = nm01 * cosZ + nm11 * sinZ
        dst.m02 = nm02 * cosZ + nm12 * sinZ
        dst.m10 = nm00 * m_sinZ + nm10 * cosZ
        dst.m11 = nm01 * m_sinZ + nm11 * cosZ
        dst.m12 = nm02 * m_sinZ + nm12 * cosZ
        return dst
    }

    @JvmOverloads
    fun rotate(ang: Double, x: Double, y: Double, z: Double, dst: Matrix3d = this): Matrix3d {
        val s = sin(ang)
        val c = cos(ang)
        val C = 1.0 - c
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val yy = y * y
        val yz = y * z
        val zz = z * z
        val rm00 = xx * C + c
        val rm01 = xy * C + z * s
        val rm02 = xz * C - y * s
        val rm10 = xy * C - z * s
        val rm11 = yy * C + c
        val rm12 = yz * C + x * s
        val rm20 = xz * C + y * s
        val rm21 = yz * C - x * s
        val rm22 = zz * C + c
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dst.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22
        dst.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22
        dst.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        return dst
    }

    @JvmOverloads
    fun rotateLocal(ang: Double, x: Double, y: Double, z: Double, dst: Matrix3d = this): Matrix3d {
        val s = sin(ang)
        val c = cos(ang)
        val C = 1.0 - c
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val yy = y * y
        val yz = y * z
        val zz = z * z
        val lm00 = xx * C + c
        val lm01 = xy * C + z * s
        val lm02 = xz * C - y * s
        val lm10 = xy * C - z * s
        val lm11 = yy * C + c
        val lm12 = yz * C + x * s
        val lm20 = xz * C + y * s
        val lm21 = yz * C - x * s
        val lm22 = zz * C + c
        val nm00 = lm00 * m00 + lm10 * m01 + lm20 * m02
        val nm01 = lm01 * m00 + lm11 * m01 + lm21 * m02
        val nm02 = lm02 * m00 + lm12 * m01 + lm22 * m02
        val nm10 = lm00 * m10 + lm10 * m11 + lm20 * m12
        val nm11 = lm01 * m10 + lm11 * m11 + lm21 * m12
        val nm12 = lm02 * m10 + lm12 * m11 + lm22 * m12
        val nm20 = lm00 * m20 + lm10 * m21 + lm20 * m22
        val nm21 = lm01 * m20 + lm11 * m21 + lm21 * m22
        val nm22 = lm02 * m20 + lm12 * m21 + lm22 * m22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun rotateLocalX(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm01 = cos * m01 - sin * m02
        val nm02 = sin * m01 + cos * m02
        val nm11 = cos * m11 - sin * m12
        val nm12 = sin * m11 + cos * m12
        val nm21 = cos * m21 - sin * m22
        val nm22 = sin * m21 + cos * m22
        return dst.set(m00, nm01, nm02, m10, nm11, nm12, m20, nm21, nm22)
    }

    @JvmOverloads
    fun rotateLocalY(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 + sin * m02
        val nm02 = -sin * m00 + cos * m02
        val nm10 = cos * m10 + sin * m12
        val nm12 = -sin * m10 + cos * m12
        val nm20 = cos * m20 + sin * m22
        val nm22 = -sin * m20 + cos * m22
        return dst.set(nm00, m01, nm02, nm10, m11, nm12, nm20, m21, nm22)
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Double, dst: Matrix3d = this): Matrix3d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        return dst.set(nm00, nm01, m02, nm10, nm11, m12, nm20, nm21, m22)
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaterniond, dst: Matrix3d = this): Matrix3d {
        val w2 = quat.w * quat.w
        val x2 = quat.x * quat.x
        val y2 = quat.y * quat.y
        val z2 = quat.z * quat.z
        val zw = quat.z * quat.w
        val dzw = zw + zw
        val xy = quat.x * quat.y
        val dxy = xy + xy
        val xz = quat.x * quat.z
        val dxz = xz + xz
        val yw = quat.y * quat.w
        val dyw = yw + yw
        val yz = quat.y * quat.z
        val dyz = yz + yz
        val xw = quat.x * quat.w
        val dxw = xw + xw
        val lm00 = w2 + x2 - z2 - y2
        val lm01 = dxy + dzw
        val lm02 = dxz - dyw
        val lm10 = dxy - dzw
        val lm11 = y2 - z2 + w2 - x2
        val lm12 = dyz + dxw
        val lm20 = dyw + dxz
        val lm21 = dyz - dxw
        val lm22 = z2 - y2 - x2 + w2
        val nm00 = lm00 * m00 + lm10 * m01 + lm20 * m02
        val nm01 = lm01 * m00 + lm11 * m01 + lm21 * m02
        val nm02 = lm02 * m00 + lm12 * m01 + lm22 * m02
        val nm10 = lm00 * m10 + lm10 * m11 + lm20 * m12
        val nm11 = lm01 * m10 + lm11 * m11 + lm21 * m12
        val nm12 = lm02 * m10 + lm12 * m11 + lm22 * m12
        val nm20 = lm00 * m20 + lm10 * m21 + lm20 * m22
        val nm21 = lm01 * m20 + lm11 * m21 + lm21 * m22
        val nm22 = lm02 * m20 + lm12 * m21 + lm22 * m22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun rotate(quat: Quaterniond, dst: Matrix3d = this): Matrix3d {
        val w2 = quat.w * quat.w
        val x2 = quat.x * quat.x
        val y2 = quat.y * quat.y
        val z2 = quat.z * quat.z
        val zw = quat.z * quat.w
        val dzw = zw + zw
        val xy = quat.x * quat.y
        val dxy = xy + xy
        val xz = quat.x * quat.z
        val dxz = xz + xz
        val yw = quat.y * quat.w
        val dyw = yw + yw
        val yz = quat.y * quat.z
        val dyz = yz + yz
        val xw = quat.x * quat.w
        val dxw = xw + xw
        val rm00 = w2 + x2 - z2 - y2
        val rm01 = dxy + dzw
        val rm02 = dxz - dyw
        val rm10 = dxy - dzw
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = dyz + dxw
        val rm20 = dyw + dxz
        val rm21 = dyz - dxw
        val rm22 = z2 - y2 - x2 + w2
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm20 = m00 * rm20 + m10 * rm21 + m20 * rm22
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    fun rotate(axisAngle: AxisAngle4d): Matrix3d {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4d, dst: Matrix3d): Matrix3d {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dst)
    }

    fun rotate(angle: Double, axis: Vector3d): Matrix3d {
        return rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Double, axis: Vector3d, dst: Matrix3d): Matrix3d {
        return rotate(angle, axis.x, axis.y, axis.z, dst)
    }

    fun rotate(angle: Double, axis: Vector3f): Matrix3d {
        return rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())
    }

    fun rotate(angle: Double, axis: Vector3f, dst: Matrix3d): Matrix3d {
        return rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble(), dst)
    }

    override fun getRow(row: Int, dst: Vector3d): Vector3d {
        return when (row) {
            0 -> dst.set(m00, m10, m20)
            1 -> dst.set(m01, m11, m21)
            else -> dst.set(m02, m12, m22)
        }
    }

    override fun setRow(row: Int, src: Vector3d): Matrix3d {
        return setRow(row, src.x, src.y, src.z)
    }

    fun setRow(row: Int, x: Double, y: Double, z: Double): Matrix3d {
        when (row) {
            0 -> {
                m00 = x
                m10 = y
                m20 = z
            }
            1 -> {
                m01 = x
                m11 = y
                m21 = z
            }
            else -> {
                m02 = x
                m12 = y
                m22 = z
            }
        }
        return this
    }

    override fun getColumn(column: Int, dst: Vector3d): Vector3d {
        return when (column) {
            0 -> dst.set(m00, m01, m02)
            1 -> dst.set(m10, m11, m12)
            else -> dst.set(m20, m21, m22)
        }
    }

    override fun setColumn(column: Int, src: Vector3d): Matrix3d {
        return setColumn(column, src.x, src.y, src.z)
    }

    fun setColumn(column: Int, x: Double, y: Double, z: Double): Matrix3d {
        when (column) {
            0 -> {
                m00 = x
                m01 = y
                m02 = z
            }
            1 -> {
                m10 = x
                m11 = y
                m12 = z
            }
            else -> {
                m20 = x
                m21 = y
                m22 = z
            }
        }
        return this
    }

    override operator fun get(column: Int, row: Int): Double {
        return when (column * 3 + row) {
            0 -> m00
            1 -> m01
            2 -> m02
            3 -> m10
            4 -> m11
            5 -> m12
            6 -> m20
            7 -> m21
            else -> m22
        }
    }

    override operator fun set(column: Int, row: Int, value: Double): Matrix3d {
        when (column * 3 + row) {
            0 -> m00 = value
            1 -> m01 = value
            2 -> m02 = value
            3 -> m10 = value
            4 -> m11 = value
            5 -> m12 = value
            6 -> m20 = value
            7 -> m21 = value
            else -> m22 = value
        }
        return this
    }

    fun getRowColumn(row: Int, column: Int): Double {
        return get(column, row)
    }

    fun setRowColumn(row: Int, column: Int, value: Double): Matrix3d {
        return set(column, row, value)
    }

    @JvmOverloads
    fun normal(dst: Matrix3d = this): Matrix3d {
        val m00m11 = m00 * m11
        val m01m10 = m01 * m10
        val m02m10 = m02 * m10
        val m00m12 = m00 * m12
        val m01m12 = m01 * m12
        val m02m11 = m02 * m11
        val det = (m00m11 - m01m10) * m22 + (m02m10 - m00m12) * m21 + (m01m12 - m02m11) * m20
        val s = 1.0 / det
        val nm00 = (m11 * m22 - m21 * m12) * s
        val nm01 = (m20 * m12 - m10 * m22) * s
        val nm02 = (m10 * m21 - m20 * m11) * s
        val nm10 = (m21 * m02 - m01 * m22) * s
        val nm11 = (m00 * m22 - m20 * m02) * s
        val nm12 = (m20 * m01 - m00 * m21) * s
        val nm20 = (m01m12 - m02m11) * s
        val nm21 = (m02m10 - m00m12) * s
        val nm22 = (m00m11 - m01m10) * s
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun cofactor(dst: Matrix3d = this): Matrix3d {
        val nm00 = m11 * m22 - m21 * m12
        val nm01 = m20 * m12 - m10 * m22
        val nm02 = m10 * m21 - m20 * m11
        val nm10 = m21 * m02 - m01 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm12 = m20 * m01 - m00 * m21
        val nm20 = m01 * m12 - m11 * m02
        val nm21 = m02 * m10 - m12 * m00
        val nm22 = m00 * m11 - m10 * m01
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    @JvmOverloads
    fun lookAlong(dir: Vector3d, up: Vector3d, dst: Matrix3d = this): Matrix3d {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Double, dirY: Double, dirZ: Double,
        upX: Double, upY: Double, upZ: Double,
        dst: Matrix3d = this
    ): Matrix3d {
        var dirXi = dirX
        var dirYi = dirY
        var dirZi = dirZ
        val invDirLength = JomlMath.invsqrt(dirXi * dirXi + dirYi * dirYi + dirZi * dirZi)
        dirXi *= -invDirLength
        dirYi *= -invDirLength
        dirZi *= -invDirLength
        var leftX = upY * dirZi - upZ * dirYi
        var leftY = upZ * dirXi - upX * dirZi
        var leftZ = upX * dirYi - upY * dirXi
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirYi * leftZ - dirZi * leftY
        val upnY = dirZi * leftX - dirXi * leftZ
        val upnZ = dirXi * leftY - dirYi * leftX
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirXi
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirXi
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirXi
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirYi
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirYi
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirYi
        val nm20 = m00 * leftZ + m10 * upnZ + m20 * dirZi
        val nm21 = m01 * leftZ + m11 * upnZ + m21 * dirZi
        val nm22 = m02 * leftZ + m12 * upnZ + m22 * dirZi
        return dst.set(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22)
    }

    fun setLookAlong(dir: Vector3d, up: Vector3d): Matrix3d {
        return setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix3d {
        var dirXi = dirX
        var dirYi = dirY
        var dirZi = dirZ
        val invDirLength = JomlMath.invsqrt(dirXi * dirXi + dirYi * dirYi + dirZi * dirZi)
        dirXi *= -invDirLength
        dirYi *= -invDirLength
        dirZi *= -invDirLength
        var leftX = upY * dirZi - upZ * dirYi
        var leftY = upZ * dirXi - upX * dirZi
        var leftZ = upX * dirYi - upY * dirXi
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirYi * leftZ - dirZi * leftY
        val upnY = dirZi * leftX - dirXi * leftZ
        val upnZ = dirXi * leftY - dirYi * leftX
        m00 = leftX
        m01 = upnX
        m02 = dirXi
        m10 = leftY
        m11 = upnY
        m12 = dirYi
        m20 = leftZ
        m21 = upnZ
        m22 = dirZi
        return this
    }

    fun getScale(dst: Vector3d): Vector3d {
        dst.x = sqrt(m00 * m00 + m01 * m01 + m02 * m02)
        dst.y = sqrt(m10 * m10 + m11 * m11 + m12 * m12)
        dst.z = sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst
    }

    fun positiveZ(dir: Vector3d): Vector3d {
        dir.x = m10 * m21 - m11 * m20
        dir.y = m20 * m01 - m21 * m00
        dir.z = m00 * m11 - m01 * m10
        return dir.normalize(dir)
    }

    fun normalizedPositiveZ(dir: Vector3d): Vector3d {
        dir.x = m02
        dir.y = m12
        dir.z = m22
        return dir
    }

    fun positiveX(dir: Vector3d): Vector3d {
        dir.x = m11 * m22 - m12 * m21
        dir.y = m02 * m21 - m01 * m22
        dir.z = m01 * m12 - m02 * m11
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector3d): Vector3d {
        dir.x = m00
        dir.y = m10
        dir.z = m20
        return dir
    }

    fun positiveY(dir: Vector3d): Vector3d {
        dir.x = m12 * m20 - m10 * m22
        dir.y = m00 * m22 - m02 * m20
        dir.z = m02 * m10 - m00 * m12
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector3d): Vector3d {
        dir.x = m01
        dir.y = m11
        dir.z = m21
        return dir
    }

    override fun hashCode(): Int {
        var result = m00.hashCode()
        result = 31 * result + m01.hashCode()
        result = 31 * result + m02.hashCode()
        result = 31 * result + m10.hashCode()
        result = 31 * result + m11.hashCode()
        result = 31 * result + m12.hashCode()
        result = 31 * result + m20.hashCode()
        result = 31 * result + m21.hashCode()
        result = 31 * result + m22.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Matrix3d &&
                m00 == other.m00 && m01 == other.m01 && m02 == other.m02 &&
                m10 == other.m10 && m11 == other.m11 && m12 == other.m12 &&
                m20 == other.m20 && m21 == other.m21 && m22 == other.m22
    }

    override fun equals(other: Matrix3d?, threshold: Double): Boolean {
        if (other === this) return true
        return other != null &&
                Runtime.equals(m00, other.m00, threshold) && Runtime.equals(m01, other.m01, threshold) &&
                Runtime.equals(m02, other.m02, threshold) && Runtime.equals(m10, other.m10, threshold) &&
                Runtime.equals(m11, other.m11, threshold) && Runtime.equals(m12, other.m12, threshold) &&
                Runtime.equals(m20, other.m20, threshold) && Runtime.equals(m21, other.m21, threshold) &&
                Runtime.equals(m22, other.m22, threshold)
    }

    fun swap(other: Matrix3d): Matrix3d {
        var tmp = m00
        m00 = other.m00
        other.m00 = tmp
        tmp = m01
        m01 = other.m01
        other.m01 = tmp
        tmp = m02
        m02 = other.m02
        other.m02 = tmp
        tmp = m10
        m10 = other.m10
        other.m10 = tmp
        tmp = m11
        m11 = other.m11
        other.m11 = tmp
        tmp = m12
        m12 = other.m12
        other.m12 = tmp
        tmp = m20
        m20 = other.m20
        other.m20 = tmp
        tmp = m21
        m21 = other.m21
        other.m21 = tmp
        tmp = m22
        m22 = other.m22
        other.m22 = tmp
        return this
    }

    @JvmOverloads
    fun add(other: Matrix3d, dst: Matrix3d = this): Matrix3d {
        dst.m00 = m00 + other.m00
        dst.m01 = m01 + other.m01
        dst.m02 = m02 + other.m02
        dst.m10 = m10 + other.m10
        dst.m11 = m11 + other.m11
        dst.m12 = m12 + other.m12
        dst.m20 = m20 + other.m20
        dst.m21 = m21 + other.m21
        dst.m22 = m22 + other.m22
        return dst
    }

    @JvmOverloads
    fun sub(subtrahend: Matrix3d, dst: Matrix3d = this): Matrix3d {
        dst.m00 = m00 - subtrahend.m00
        dst.m01 = m01 - subtrahend.m01
        dst.m02 = m02 - subtrahend.m02
        dst.m10 = m10 - subtrahend.m10
        dst.m11 = m11 - subtrahend.m11
        dst.m12 = m12 - subtrahend.m12
        dst.m20 = m20 - subtrahend.m20
        dst.m21 = m21 - subtrahend.m21
        dst.m22 = m22 - subtrahend.m22
        return dst
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix3d, dst: Matrix3d = this): Matrix3d {
        dst.m00 = m00 * other.m00
        dst.m01 = m01 * other.m01
        dst.m02 = m02 * other.m02
        dst.m10 = m10 * other.m10
        dst.m11 = m11 * other.m11
        dst.m12 = m12 * other.m12
        dst.m20 = m20 * other.m20
        dst.m21 = m21 * other.m21
        dst.m22 = m22 * other.m22
        return dst
    }

    fun setSkewSymmetric(a: Double, b: Double, c: Double): Matrix3d {
        m22 = 0.0
        m11 = m22
        m00 = m11
        m01 = -a
        m02 = b
        m10 = a
        m12 = -c
        m20 = -b
        m21 = c
        return this
    }

    @JvmOverloads
    fun mix(other: Matrix3d, t: Double, dst: Matrix3d = this): Matrix3d {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m02 = (other.m02 - m02) * t + m02
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        dst.m12 = (other.m12 - m12) * t + m12
        dst.m20 = (other.m20 - m20) * t + m20
        dst.m21 = (other.m21 - m21) * t + m21
        dst.m22 = (other.m22 - m22) * t + m22
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix3d, t: Double, dst: Matrix3d = this): Matrix3d {
        return mix(other, t, dst)
    }

    fun rotateTowards(direction: Vector3d, up: Vector3d, dst: Matrix3d): Matrix3d {
        return rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dst)
    }

    fun rotateTowards(direction: Vector3d, up: Vector3d): Matrix3d {
        return rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Double, dirY: Double, dirZ: Double,
        upX: Double, upY: Double, upZ: Double, dst: Matrix3d = this
    ): Matrix3d {
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val ndirX = dirX * invDirLength
        val ndirY = dirY * invDirLength
        val ndirZ = dirZ * invDirLength
        var leftX = upY * ndirZ - upZ * ndirY
        var leftY = upZ * ndirX - upX * ndirZ
        var leftZ = upX * ndirY - upY * ndirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = ndirY * leftZ - ndirZ * leftY
        val upnY = ndirZ * leftX - ndirX * leftZ
        val upnZ = ndirX * leftY - ndirY * leftX
        val nm00 = m00 * leftX + m10 * leftY + m20 * leftZ
        val nm01 = m01 * leftX + m11 * leftY + m21 * leftZ
        val nm02 = m02 * leftX + m12 * leftY + m22 * leftZ
        val nm10 = m00 * upnX + m10 * upnY + m20 * upnZ
        val nm11 = m01 * upnX + m11 * upnY + m21 * upnZ
        val nm12 = m02 * upnX + m12 * upnY + m22 * upnZ
        dst.m20 = m00 * ndirX + m10 * ndirY + m20 * ndirZ
        dst.m21 = m01 * ndirX + m11 * ndirY + m21 * ndirZ
        dst.m22 = m02 * ndirX + m12 * ndirY + m22 * ndirZ
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        return dst
    }

    fun rotationTowards(dir: Vector3d, up: Vector3d): Matrix3d {
        return rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix3d {
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val ndirX = dirX * invDirLength
        val ndirY = dirY * invDirLength
        val ndirZ = dirZ * invDirLength
        var leftX = upY * ndirZ - upZ * ndirY
        var leftY = upZ * ndirX - upX * ndirZ
        var leftZ = upX * ndirY - upY * ndirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = ndirY * leftZ - ndirZ * leftY
        val upnY = ndirZ * leftX - ndirX * leftZ
        val upnZ = ndirX * leftY - ndirY * leftX
        m00 = leftX
        m01 = leftY
        m02 = leftZ
        m10 = upnX
        m11 = upnY
        m12 = upnZ
        m20 = ndirX
        m21 = ndirY
        m22 = ndirZ
        return this
    }

    fun getEulerAnglesZYX(dst: Vector3d): Vector3d {
        dst.x = atan2(m12, m22)
        dst.y = atan2(-m02, sqrt(1.0 - m02 * m02))
        dst.z = atan2(m01, m00)
        return dst
    }

    fun getEulerAnglesXYZ(dst: Vector3d): Vector3d {
        dst.x = atan2(-m21, m22)
        dst.y = atan2(m20, sqrt(1.0 - m20 * m20))
        dst.z = atan2(-m10, m00)
        return dst
    }

    @JvmOverloads
    fun obliqueZ(a: Double, b: Double, dst: Matrix3d = this): Matrix3d {
        dst.m00 = m00
        dst.m01 = m01
        dst.m02 = m02
        dst.m10 = m10
        dst.m11 = m11
        dst.m12 = m12
        dst.m20 = m00 * a + m10 * b + m20
        dst.m21 = m01 * a + m11 * b + m21
        dst.m22 = m02 * a + m12 * b + m22
        return dst
    }

    @JvmOverloads
    fun reflect(nx: Double, ny: Double, nz: Double, dst: Matrix3d = this): Matrix3d {
        val da = nx + nx
        val db = ny + ny
        val dc = nz + nz
        val rm00 = 1.0 - da * nx
        val rm01 = -da * ny
        val rm02 = -da * nz
        val rm10 = -db * nx
        val rm11 = 1.0 - db * ny
        val rm12 = -db * nz
        val rm20 = -dc * nx
        val rm21 = -dc * ny
        val rm22 = 1.0 - dc * nz
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)
    }

    @JvmOverloads
    fun reflect(orientation: Quaterniond, dst: Matrix3d = this): Matrix3d {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2)
        return reflect(normalX, normalY, normalZ, dst)
    }

    @JvmOverloads
    fun reflect(normal: Vector3d, dst: Matrix3d = this): Matrix3d {
        return reflect(normal.x, normal.y, normal.z, dst)
    }

    fun reflection(nx: Double, ny: Double, nz: Double): Matrix3d {
        val da = nx + nx
        val db = ny + ny
        val dc = nz + nz
        _m00(1.0 - da * nx)
        _m01(-da * ny)
        _m02(-da * nz)
        _m10(-db * nx)
        _m11(1.0 - db * ny)
        _m12(-db * nz)
        _m20(-dc * nx)
        _m21(-dc * ny)
        _m22(1.0 - dc * nz)
        return this
    }

    fun reflection(normal: Vector3d): Matrix3d {
        return reflection(normal.x, normal.y, normal.z)
    }

    fun reflection(orientation: Quaterniond): Matrix3d {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2)
        return reflection(normalX, normalY, normalZ)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) &&
                JomlMath.isFinite(m10) && JomlMath.isFinite(m11) && JomlMath.isFinite(m12) &&
                JomlMath.isFinite(m20) && JomlMath.isFinite(m21) && JomlMath.isFinite(m22)

    fun quadraticFormProduct(x: Double, y: Double, z: Double): Double {
        val Axx = m00 * x + m10 * y + m20 * z
        val Axy = m01 * x + m11 * y + m21 * z
        val Axz = m02 * x + m12 * y + m22 * z
        return x * Axx + y * Axy + z * Axz
    }

    fun quadraticFormProduct(v: Vector3d): Double {
        return quadraticFormProduct(v.x, v.y, v.z)
    }

    fun quadraticFormProduct(v: Vector3f): Double {
        return quadraticFormProduct(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
    }
}