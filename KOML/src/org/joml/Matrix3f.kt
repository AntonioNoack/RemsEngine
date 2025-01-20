package org.joml

import org.joml.JomlMath.addSigns
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix3f : Matrix<Matrix3f, Vector3f, Vector3f> {

    var m00 = 0f
    var m01 = 0f
    var m02 = 0f
    var m10 = 0f
    var m11 = 0f
    var m12 = 0f
    var m20 = 0f
    var m21 = 0f
    var m22 = 0f

    constructor() {
        m00 = 1f
        m11 = 1f
        m22 = 1f
    }

    constructor(mat: Matrix2f) {
        this.set(mat)
    }

    constructor(mat: Matrix3f) {
        this.set(mat)
    }

    constructor(mat: Matrix4f) {
        this.set(mat)
    }

    constructor(
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

    constructor(col0: Vector3f, col1: Vector3f, col2: Vector3f) {
        set(col0, col1, col2)
    }

    override val numCols: Int get() = 3
    override val numRows: Int get() = 3

    fun _m00(m00: Float): Matrix3f {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Float): Matrix3f {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Float): Matrix3f {
        this.m02 = m02
        return this
    }

    fun _m10(m10: Float): Matrix3f {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Float): Matrix3f {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Float): Matrix3f {
        this.m12 = m12
        return this
    }

    fun _m20(m20: Float): Matrix3f {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Float): Matrix3f {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Float): Matrix3f {
        this.m22 = m22
        return this
    }

    fun set(m: Matrix3f): Matrix3f {
        return _m00(m.m00)._m01(m.m01)._m02(m.m02)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m20(m.m20)._m21(m.m21)
            ._m22(m.m22)
    }

    fun setTransposed(m: Matrix3f): Matrix3f {
        val nm10 = m.m01
        val nm12 = m.m21
        val nm20 = m.m02
        val nm21 = m.m12
        return _m00(m.m00)._m01(m.m10)._m02(m.m20)._m10(nm10)._m11(m.m11)._m12(nm12)._m20(nm20)._m21(nm21)._m22(m.m22)
    }

    fun set(m: Matrix4x3f): Matrix3f {
        m00 = m.m00
        m01 = m.m01
        m02 = m.m02
        m10 = m.m10
        m11 = m.m11
        m12 = m.m12
        m20 = m.m20
        m21 = m.m21
        m22 = m.m22
        return this
    }

    fun set(m: Matrix4x3d): Matrix3f {
        return set(Matrix4x3f().set(m))
    }

    fun set(mat: Matrix4f): Matrix3f {
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

    fun set(mat: Matrix2f): Matrix3f {
        m00 = mat.m00
        m01 = mat.m01
        m02 = 0f
        m10 = mat.m10
        m11 = mat.m11
        m12 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 1f
        return this
    }

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01).put(m02)
        arr.put(m10).put(m11).put(m12)
        arr.put(m20).put(m21).put(m22)
        return arr
    }

    fun set(axisAngle: AxisAngle4f): Matrix3f {
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
        val omc = 1f - c
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

    fun set(axisAngle: AxisAngle4d): Matrix3f {
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
        m00 = (c + x * x * omc).toFloat()
        m11 = (c + y * y * omc).toFloat()
        m22 = (c + z * z * omc).toFloat()
        var tmp1 = x * y * omc
        var tmp2 = z * s
        m10 = (tmp1 - tmp2).toFloat()
        m01 = (tmp1 + tmp2).toFloat()
        tmp1 = x * z * omc
        tmp2 = y * s
        m20 = (tmp1 + tmp2).toFloat()
        m02 = (tmp1 - tmp2).toFloat()
        tmp1 = y * z * omc
        tmp2 = x * s
        m21 = (tmp1 - tmp2).toFloat()
        m12 = (tmp1 + tmp2).toFloat()
        return this
    }

    fun set(q: Quaternionf): Matrix3f {
        return rotation(q)
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
        }.toDouble()
    }

    override operator fun set(column: Int, row: Int, value: Double): Matrix3f {
        return set(column, row, value.toFloat())
    }

    operator fun set(column: Int, row: Int, value: Float): Matrix3f {
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

    @JvmOverloads
    fun mul(right: Matrix3f, dst: Matrix3f = this): Matrix3f {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    @JvmOverloads
    fun mulLocal(left: Matrix3f, dst: Matrix3f = this): Matrix3f {
        val nm00 = left.m00 * m00 + left.m10 * m01 + left.m20 * m02
        val nm01 = left.m01 * m00 + left.m11 * m01 + left.m21 * m02
        val nm02 = left.m02 * m00 + left.m12 * m01 + left.m22 * m02
        val nm10 = left.m00 * m10 + left.m10 * m11 + left.m20 * m12
        val nm11 = left.m01 * m10 + left.m11 * m11 + left.m21 * m12
        val nm12 = left.m02 * m10 + left.m12 * m11 + left.m22 * m12
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20 * m22
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21 * m22
        val nm22 = left.m02 * m20 + left.m12 * m21 + left.m22 * m22
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    operator fun set(
        m00: Float,
        m01: Float,
        m02: Float,
        m10: Float,
        m11: Float,
        m12: Float,
        m20: Float,
        m21: Float,
        m22: Float
    ): Matrix3f {
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

    /*fun set(m: FloatArray): Matrix3f {
        MemUtil.INSTANCE.copy(m, 0, this)
        return this
    }*/

    fun set(col0: Vector3f, col1: Vector3f, col2: Vector3f): Matrix3f {
        m00 = col0.x
        m01 = col0.y
        m02 = col0.z
        m10 = col1.x
        m11 = col1.y
        m12 = col1.z
        m20 = col2.x
        m21 = col2.y
        m22 = col2.z
        return this
    }

    fun determinant(): Float {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dst: Matrix3f = this): Matrix3f {
        val a = m00 * m11 - m01 * m10
        val b = m02 * m10 - m00 * m12
        val c = m01 * m12 - m02 * m11
        val d = a * m22 + b * m21 + c * m20
        val s = 1f / d
        val nm00 = (m11 * m22 - m21 * m12) * s
        val nm01 = (m21 * m02 - m01 * m22) * s
        val nm02 = c * s
        val nm10 = (m20 * m12 - m10 * m22) * s
        val nm11 = (m00 * m22 - m20 * m02) * s
        val nm12 = b * s
        val nm20 = (m10 * m21 - m20 * m11) * s
        val nm21 = (m20 * m01 - m00 * m21) * s
        val nm22 = a * s
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    @JvmOverloads
    fun transpose(dst: Matrix3f = this): Matrix3f {
        return dst.set(m00, m10, m20, m01, m11, m21, m02, m12, m22)
    }

    override fun toString() =
        ("[[${Runtime.f(m00)} ${Runtime.f(m10)} ${Runtime.f(m20)}] " +
                "[${Runtime.f(m01)} ${Runtime.f(m11)} ${Runtime.f(m21)}] " +
                "[${Runtime.f(m02)} ${Runtime.f(m12)} ${Runtime.f(m22)}]]").addSigns()

    fun get(dst: Matrix3f): Matrix3f {
        return dst.set(this)
    }

    fun get(dst: Matrix4f): Matrix4f {
        return dst.set(this)
    }

    fun getRotation(dst: AxisAngle4f): AxisAngle4f {
        return dst.set(this)
    }

    fun getUnnormalizedRotation(dst: Quaternionf): Quaternionf {
        return dst.setFromUnnormalized(this)
    }

    fun getNormalizedRotation(dst: Quaternionf): Quaternionf {
        return dst.setFromNormalized(this)
    }

    @JvmOverloads
    fun get(arr: FloatArray, offset: Int = 0): FloatArray {
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

    fun zero(): Matrix3f {
        m00 = 0f
        m01 = 0f
        m02 = 0f
        m10 = 0f
        m11 = 0f
        m12 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 0f
        return this
    }

    fun identity(): Matrix3f {
        m00 = 1f
        m01 = 0f
        m02 = 0f
        m10 = 0f
        m11 = 1f
        m12 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 1f
        return this
    }

    fun scale(xyz: Vector3f, dst: Matrix3f): Matrix3f {
        return scale(xyz.x, xyz.y, xyz.z, dst)
    }

    fun scale(xyz: Vector3f): Matrix3f {
        return scale(xyz.x, xyz.y, xyz.z, this)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, z: Float, dst: Matrix3f = this): Matrix3f {
        dst.m00 = m00 * x
        dst.m01 = m01 * x
        dst.m02 = m02 * x
        dst.m10 = m10 * y
        dst.m11 = m11 * y
        dst.m12 = m12 * y
        dst.m20 = m20 * z
        dst.m21 = m21 * z
        dst.m22 = m22 * z
        return dst
    }

    fun scale(xyz: Float, dst: Matrix3f): Matrix3f {
        return scale(xyz, xyz, xyz, dst)
    }

    fun scale(xyz: Float): Matrix3f {
        return scale(xyz, xyz, xyz)
    }

    @JvmOverloads
    fun scaleLocal(scale: Vector3f, dst: Matrix3f = this): Matrix3f {
        return scaleLocal(scale.x, scale.y, scale.z, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, z: Float, dst: Matrix3f = this): Matrix3f {
        val nm00 = x * m00
        val nm01 = y * m01
        val nm02 = z * m02
        val nm10 = x * m10
        val nm11 = y * m11
        val nm12 = z * m12
        val nm20 = x * m20
        val nm21 = y * m21
        val nm22 = z * m22
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    fun scaling(factor: Float) = scaling(factor, factor, factor)
    fun scaling(x: Float, y: Float, z: Float): Matrix3f {
        zero()
        m00 = x
        m11 = y
        m22 = z
        return this
    }

    fun scaling(xyz: Vector3f) = this.scaling(xyz.x, xyz.y, xyz.z)

    fun rotation(angle: Float, axis: Vector3f): Matrix3f {
        return rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(axisAngle: AxisAngle4f): Matrix3f {
        return rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotation(angle: Float, x: Float, y: Float, z: Float): Matrix3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1f - cos
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

    fun rotationX(ang: Float): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = 1f
        m01 = 0f
        m02 = 0f
        m10 = 0f
        m11 = cos
        m12 = sin
        m20 = 0f
        m21 = -sin
        m22 = cos
        return this
    }

    fun rotationY(ang: Float): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = cos
        m01 = 0f
        m02 = -sin
        m10 = 0f
        m11 = 1f
        m12 = 0f
        m20 = sin
        m21 = 0f
        m22 = cos
        return this
    }

    fun rotationZ(ang: Float): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        m00 = cos
        m01 = sin
        m02 = 0f
        m10 = -sin
        m11 = cos
        m12 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 1f
        return this
    }

    fun rotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Matrix3f {
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

    fun rotationZYX(angleZ: Float, angleY: Float, angleX: Float): Matrix3f {
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

    fun rotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Matrix3f {
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

    fun rotation(quat: Quaternionf): Matrix3f {
        return rotationQ(quat.x, quat.y, quat.z, quat.w)
    }

    fun rotationQ(qx: Float, qy: Float, qz: Float, qw: Float): Matrix3f {
        val w2 = qw * qw
        val x2 = qx * qx
        val y2 = qy * qy
        val z2 = qz * qz
        val zw = qz * qw
        val dzw = zw + zw
        val xy = qx * qy
        val dxy = xy + xy
        val xz = qx * qz
        val dxz = xz + xz
        val yw = qy * qw
        val dyw = yw + yw
        val yz = qy * qz
        val dyz = yz + yz
        val xw = qx * qw
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

    fun transform(v: Vector3f): Vector3f {
        return v.mul(this)
    }

    fun transform(v: Vector3f, dst: Vector3f): Vector3f {
        return v.mul(this, dst)
    }

    fun transform(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        return dst.set(
            (m00 * x + (m10 * y + m20 * z)),
            (m01 * x + (m11 * y + m21 * z)),
            (m02 * x + (m12 * y + m22 * z))
        )
    }

    fun transformTranspose(v: Vector3f): Vector3f {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector3f, dst: Vector3f): Vector3f {
        return v.mulTranspose(this, dst)
    }

    fun transformTranspose(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        return dst.set(
            (m00 * x + (m01 * y + m02 * z)),
            (m10 * x + (m11 * y + m12 * z)),
            (m20 * x + (m21 * y + m22 * z))
        )
    }

    @JvmOverloads
    fun rotateX(ang: Float, dst: Matrix3f = this): Matrix3f {
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
    fun rotateY(ang: Float, dst: Matrix3f = this): Matrix3f {
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
    fun rotateZ(ang: Float, dst: Matrix3f = this): Matrix3f {
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

    fun rotateXYZ(angles: Vector3f): Matrix3f {
        return rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix3f = this): Matrix3f {
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

    fun rotateZYX(angles: Vector3f): Matrix3f {
        return rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix3f = this): Matrix3f {
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

    fun rotateYXZ(angles: Vector3f): Matrix3f {
        return rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix3f = this): Matrix3f {
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
    fun rotate(ang: Float, x: Float, y: Float, z: Float, dst: Matrix3f = this): Matrix3f {
        val s = sin(ang)
        val c = cos(ang)
        val C = 1f - c
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
    fun rotateLocal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix3f = this): Matrix3f {
        val s = sin(ang)
        val c = cos(ang)
        val C = 1f - c
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
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    @JvmOverloads
    fun rotateLocalX(ang: Float, dst: Matrix3f = this): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm01 = cos * m01 - sin * m02
        val nm02 = sin * m01 + cos * m02
        val nm11 = cos * m11 - sin * m12
        val nm12 = sin * m11 + cos * m12
        val nm21 = cos * m21 - sin * m22
        val nm22 = sin * m21 + cos * m22
        dst.m00 = m00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = m10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = m20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    @JvmOverloads
    fun rotateLocalY(ang: Float, dst: Matrix3f = this): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 + sin * m02
        val nm02 = -sin * m00 + cos * m02
        val nm10 = cos * m10 + sin * m12
        val nm12 = -sin * m10 + cos * m12
        val nm20 = cos * m20 + sin * m22
        val nm22 = -sin * m20 + cos * m22
        dst.m00 = nm00
        dst.m01 = m01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = m11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = m21
        dst.m22 = nm22
        return dst
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Float, dst: Matrix3f = this): Matrix3f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = m02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = m12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = m22
        return dst
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dst: Matrix3f = this): Matrix3f {
        return rotateQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    fun rotateQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix3f = this): Matrix3f {
        val w2 = qw * qw
        val x2 = qx * qx
        val y2 = qy * qy
        val z2 = qz * qz
        val zw = qz * qw
        val dzw = zw + zw
        val xy = qx * qy
        val dxy = xy + xy
        val xz = qx * qz
        val dxz = xz + xz
        val yw = qy * qw
        val dyw = yw + yw
        val yz = qy * qz
        val dyz = yz + yz
        val xw = qx * qw
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
    fun rotateLocal(quat: Quaternionf, dst: Matrix3f = this): Matrix3f {
        return rotateLocalQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    private fun rotateLocalQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix3f = this): Matrix3f {
        val w2 = qw * qw
        val x2 = qx * qx
        val y2 = qy * qy
        val z2 = qz * qz
        val zw = qz * qw
        val dzw = zw + zw
        val xy = qx * qy
        val dxy = xy + xy
        val xz = qx * qz
        val dxz = xz + xz
        val yw = qy * qw
        val dyw = yw + yw
        val yz = qy * qz
        val dyz = yz + yz
        val xw = qx * qw
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
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        return dst
    }

    fun rotate(axisAngle: AxisAngle4f): Matrix3f {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4f, dst: Matrix3f): Matrix3f {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dst)
    }

    fun rotate(angle: Float, axis: Vector3f): Matrix3f {
        return rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Float, axis: Vector3f, dst: Matrix3f): Matrix3f {
        return rotate(angle, axis.x, axis.y, axis.z, dst)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Matrix3f {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dst: Matrix3f): Matrix3f {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix3f = this
    ): Matrix3f {
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

    fun setLookAlong(dir: Vector3f, up: Vector3f): Matrix3f {
        return setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix3f {
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
        return set(leftX, upnX, dirXi, leftY, upnY, dirYi, leftZ, upnZ, dirZi)
    }

    override fun getRow(row: Int, dst: Vector3f): Vector3f {
        return when (row) {
            0 -> dst.set(m00, m10, m20)
            1 -> dst.set(m01, m11, m21)
            else -> dst.set(m02, m12, m22)
        }
    }

    override fun setRow(row: Int, src: Vector3f): Matrix3f {
        return setRow(row, src.x, src.y, src.z)
    }

    fun setRow(row: Int, x: Float, y: Float, z: Float): Matrix3f {
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

    override fun getColumn(column: Int, dst: Vector3f): Vector3f {
        return when (column) {
            0 -> dst.set(m00, m01, m02)
            1 -> dst.set(m10, m11, m12)
            else -> dst.set(m20, m21, m22)
        }
    }

    override fun setColumn(column: Int, src: Vector3f): Matrix3f {
        return setColumn(column, src.x, src.y, src.z)
    }

    fun setColumn(column: Int, x: Float, y: Float, z: Float): Matrix3f {
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

    @JvmOverloads
    fun normal(dst: Matrix3f = this): Matrix3f {
        val m00m11 = m00 * m11
        val m01m10 = m01 * m10
        val m02m10 = m02 * m10
        val m00m12 = m00 * m12
        val m01m12 = m01 * m12
        val m02m11 = m02 * m11
        val det = (m00m11 - m01m10) * m22 + (m02m10 - m00m12) * m21 + (m01m12 - m02m11) * m20
        val s = 1f / det
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
    fun cofactor(dst: Matrix3f = this): Matrix3f {
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

    fun getScale(dst: Vector3f): Vector3f {
        return dst.set(
            sqrt(m00 * m00 + m01 * m01 + m02 * m02),
            sqrt(m10 * m10 + m11 * m11 + m12 * m12),
            sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        )
    }

    fun positiveZ(dir: Vector3f): Vector3f {
        dir.x = m10 * m21 - m11 * m20
        dir.y = m20 * m01 - m21 * m00
        dir.z = m00 * m11 - m01 * m10
        return dir.normalize(dir)
    }

    fun normalizedPositiveZ(dir: Vector3f): Vector3f {
        return dir.set(m02, m12, m22)
    }

    fun positiveX(dir: Vector3f): Vector3f {
        dir.x = m11 * m22 - m12 * m21
        dir.y = m02 * m21 - m01 * m22
        dir.z = m01 * m12 - m02 * m11
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector3f): Vector3f {
        dir.x = m00
        dir.y = m10
        dir.z = m20
        return dir
    }

    fun positiveY(dir: Vector3f): Vector3f {
        dir.x = m12 * m20 - m10 * m22
        dir.y = m00 * m22 - m02 * m20
        dir.z = m02 * m10 - m00 * m12
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector3f): Vector3f {
        dir.x = m01
        dir.y = m11
        dir.z = m21
        return dir
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + m00.toRawBits()
        result = 31 * result + m01.toRawBits()
        result = 31 * result + m02.toRawBits()
        result = 31 * result + m10.toRawBits()
        result = 31 * result + m11.toRawBits()
        result = 31 * result + m12.toRawBits()
        result = 31 * result + m20.toRawBits()
        result = 31 * result + m21.toRawBits()
        result = 31 * result + m22.toRawBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Matrix3f &&
                other.m00 == m00 && other.m01 == m01 && other.m02 == m02 &&
                other.m10 == m10 && other.m11 == m11 && other.m12 == m12 &&
                other.m20 == m20 && other.m21 == m21 && other.m22 == m22
    }

    override fun equals(other: Matrix3f?, threshold: Double): Boolean {
        return equals(other, threshold.toFloat())
    }

    fun equals(m: Matrix3f?, delta: Float): Boolean {
        if (m === this) return true
        return m is Matrix3f &&
                Runtime.equals(m00, m.m00, delta) && Runtime.equals(m01, m.m01, delta) &&
                Runtime.equals(m02, m.m02, delta) && Runtime.equals(m10, m.m10, delta) &&
                Runtime.equals(m11, m.m11, delta) && Runtime.equals(m12, m.m12, delta) &&
                Runtime.equals(m20, m.m20, delta) && Runtime.equals(m21, m.m21, delta) &&
                Runtime.equals(m22, m.m22, delta)
    }

    @JvmOverloads
    fun add(other: Matrix3f, dst: Matrix3f = this): Matrix3f {
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
    fun sub(subtrahend: Matrix3f, dst: Matrix3f = this): Matrix3f {
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
    fun mulComponentWise(other: Matrix3f, dst: Matrix3f = this): Matrix3f {
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

    fun setSkewSymmetric(a: Float, b: Float, c: Float): Matrix3f {
        m22 = 0f
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

    fun mix(other: Matrix3f, t: Float, dst: Matrix3f = this): Matrix3f {
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
    fun lerp(other: Matrix3f, t: Float, dst: Matrix3f = this): Matrix3f {
        return mix(other, t, dst)
    }

    fun rotateTowards(direction: Vector3f, up: Vector3f, dst: Matrix3f): Matrix3f {
        return rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dst)
    }

    fun rotateTowards(direction: Vector3f, up: Vector3f): Matrix3f {
        return rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix3f = this
    ): Matrix3f {
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

    fun rotationTowards(dir: Vector3f, up: Vector3f): Matrix3f {
        return rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix3f {
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

    fun getEulerAnglesZYX(dst: Vector3f): Vector3f {
        dst.x = atan2(m12, m22)
        dst.y = atan2(-m02, sqrt(1f - m02 * m02))
        dst.z = atan2(m01, m00)
        return dst
    }

    fun getEulerAnglesXYZ(dst: Vector3f): Vector3f {
        dst.x = atan2(-m21, m22)
        dst.y = atan2(m20, sqrt(1f - m20 * m20))
        dst.z = atan2(-m10, m00)
        return dst
    }

    fun obliqueZ(a: Float, b: Float): Matrix3f {
        m20 += m00 * a + m10 * b
        m21 += m01 * a + m11 * b
        m22 += m02 * a + m12 * b
        return this
    }

    fun obliqueZ(a: Float, b: Float, dst: Matrix3f): Matrix3f {
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
    fun reflect(nx: Float, ny: Float, nz: Float, dst: Matrix3f = this): Matrix3f {
        val da = nx + nx
        val db = ny + ny
        val dc = nz + nz
        val rm00 = 1f - da * nx
        val rm01 = -da * ny
        val rm02 = -da * nz
        val rm10 = -db * nx
        val rm11 = 1f - db * ny
        val rm12 = -db * nz
        val rm20 = -dc * nx
        val rm21 = -dc * ny
        val rm22 = 1f - dc * nz
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)
    }

    @JvmOverloads
    fun reflect(orientation: Quaternionf, dst: Matrix3f = this): Matrix3f {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1f - (orientation.x * num1 + orientation.y * num2)
        return reflect(normalX, normalY, normalZ, dst)
    }

    @JvmOverloads
    fun reflect(normal: Vector3f, dst: Matrix3f = this): Matrix3f {
        return reflect(normal.x, normal.y, normal.z, dst)
    }

    fun reflection(nx: Float, ny: Float, nz: Float): Matrix3f {
        val da = nx + nx
        val db = ny + ny
        val dc = nz + nz
        _m00(1f - da * nx)
        _m01(-da * ny)
        _m02(-da * nz)
        _m10(-db * nx)
        _m11(1f - db * ny)
        _m12(-db * nz)
        _m20(-dc * nx)
        _m21(-dc * ny)
        _m22(1f - dc * nz)
        return this
    }

    fun reflection(normal: Vector3f): Matrix3f {
        return reflection(normal.x, normal.y, normal.z)
    }

    fun reflection(orientation: Quaternionf): Matrix3f {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1f - (orientation.x * num1 + orientation.y * num2)
        return reflection(normalX, normalY, normalZ)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) &&
                JomlMath.isFinite(m10) && JomlMath.isFinite(m11) && JomlMath.isFinite(m12) &&
                JomlMath.isFinite(m20) && JomlMath.isFinite(m21) && JomlMath.isFinite(m22)

    fun quadraticFormProduct(x: Float, y: Float, z: Float): Float {
        val Axx = m00 * x + m10 * y + m20 * z
        val Axy = m01 * x + m11 * y + m21 * z
        val Axz = m02 * x + m12 * y + m22 * z
        return x * Axx + y * Axy + z * Axz
    }

    fun quadraticFormProduct(v: Vector3f): Float {
        return quadraticFormProduct(v.x, v.y, v.z)
    }
}