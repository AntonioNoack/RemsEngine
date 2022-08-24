package org.joml

import java.nio.FloatBuffer
import java.text.NumberFormat

class Matrix3f {
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
        this[col0, col1] = col2
    }

    fun m00(m00: Float): Matrix3f {
        this.m00 = m00
        return this
    }

    fun m01(m01: Float): Matrix3f {
        this.m01 = m01
        return this
    }

    fun m02(m02: Float): Matrix3f {
        this.m02 = m02
        return this
    }

    fun m10(m10: Float): Matrix3f {
        this.m10 = m10
        return this
    }

    fun m11(m11: Float): Matrix3f {
        this.m11 = m11
        return this
    }

    fun m12(m12: Float): Matrix3f {
        this.m12 = m12
        return this
    }

    fun m20(m20: Float): Matrix3f {
        this.m20 = m20
        return this
    }

    fun m21(m21: Float): Matrix3f {
        this.m21 = m21
        return this
    }

    fun m22(m22: Float): Matrix3f {
        this.m22 = m22
        return this
    }

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
        val invLength = Math.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        val s = Math.sin(angle)
        val c = Math.cosFromSin(s, angle)
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
        val invLength = Math.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        val s = Math.sin(angle)
        val c = Math.cosFromSin(s, angle)
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
        return this.rotation(q)
    }

    fun set(q: Quaterniond): Matrix3f {
        val w2 = q.w * q.w
        val x2 = q.x * q.x
        val y2 = q.y * q.y
        val z2 = q.z * q.z
        val zw = q.z * q.w
        val xy = q.x * q.y
        val xz = q.x * q.z
        val yw = q.y * q.w
        val yz = q.y * q.z
        val xw = q.x * q.w
        m00 = (w2 + x2 - z2 - y2).toFloat()
        m01 = (xy + zw + zw + xy).toFloat()
        m02 = (xz - yw + xz - yw).toFloat()
        m10 = (-zw + xy - zw + xy).toFloat()
        m11 = (y2 - z2 + w2 - x2).toFloat()
        m12 = (yz + yz + xw + xw).toFloat()
        m20 = (yw + xz + xz + yw).toFloat()
        m21 = (yz + yz - xw - xw).toFloat()
        m22 = (z2 - y2 - x2 + w2).toFloat()
        return this
    }

    @JvmOverloads
    fun mul(right: Matrix3f, dest: Matrix3f = this): Matrix3f {
        val nm00 = Math.fma(m00, right.m00, Math.fma(m10, right.m01, m20 * right.m02))
        val nm01 = Math.fma(m01, right.m00, Math.fma(m11, right.m01, m21 * right.m02))
        val nm02 = Math.fma(m02, right.m00, Math.fma(m12, right.m01, m22 * right.m02))
        val nm10 = Math.fma(m00, right.m10, Math.fma(m10, right.m11, m20 * right.m12))
        val nm11 = Math.fma(m01, right.m10, Math.fma(m11, right.m11, m21 * right.m12))
        val nm12 = Math.fma(m02, right.m10, Math.fma(m12, right.m11, m22 * right.m12))
        val nm20 = Math.fma(m00, right.m20, Math.fma(m10, right.m21, m20 * right.m22))
        val nm21 = Math.fma(m01, right.m20, Math.fma(m11, right.m21, m21 * right.m22))
        val nm22 = Math.fma(m02, right.m20, Math.fma(m12, right.m21, m22 * right.m22))
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun mulLocal(left: Matrix3f, dest: Matrix3f = this): Matrix3f {
        val nm00 = left.m00 * m00 + left.m10 * m01 + left.m20 * m02
        val nm01 = left.m01 * m00 + left.m11 * m01 + left.m21 * m02
        val nm02 = left.m02 * m00 + left.m12 * m01 + left.m22 * m02
        val nm10 = left.m00 * m10 + left.m10 * m11 + left.m20 * m12
        val nm11 = left.m01 * m10 + left.m11 * m11 + left.m21 * m12
        val nm12 = left.m02 * m10 + left.m12 * m11 + left.m22 * m12
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20 * m22
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21 * m22
        val nm22 = left.m02 * m20 + left.m12 * m21 + left.m22 * m22
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
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

    fun set(m: FloatArray?): Matrix3f {
        MemUtil.INSTANCE.copy(m, 0, this)
        return this
    }

    operator fun set(col0: Vector3f, col1: Vector3f, col2: Vector3f): Matrix3f {
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
    fun invert(dest: Matrix3f = this): Matrix3f {
        val a = Math.fma(m00, m11, -m01 * m10)
        val b = Math.fma(m02, m10, -m00 * m12)
        val c = Math.fma(m01, m12, -m02 * m11)
        val d = Math.fma(a, m22, Math.fma(b, m21, c * m20))
        val s = 1f / d
        val nm00 = Math.fma(m11, m22, -m21 * m12) * s
        val nm01 = Math.fma(m21, m02, -m01 * m22) * s
        val nm02 = c * s
        val nm10 = Math.fma(m20, m12, -m10 * m22) * s
        val nm11 = Math.fma(m00, m22, -m20 * m02) * s
        val nm12 = b * s
        val nm20 = Math.fma(m10, m21, -m20 * m11) * s
        val nm21 = Math.fma(m20, m01, -m00 * m21) * s
        val nm22 = a * s
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun transpose(dest: Matrix3f = this): Matrix3f {
        return dest.set(m00, m10, m20, m01, m11, m21, m02, m12, m22)
    }

    override fun toString(): String {
        val str = this.toString(Options.NUMBER_FORMAT)
        val res = StringBuilder()
        var eIndex = Int.MIN_VALUE
        for (i in 0 until str.length) {
            val c = str[i]
            if (c == 'E') {
                eIndex = i
            } else {
                if (c == ' ' && eIndex == i - 1) {
                    res.append('+')
                    continue
                }
                if (Character.isDigit(c) && eIndex == i - 1) {
                    res.append('+')
                }
            }
            res.append(c)
        }
        return res.toString()
    }

    fun toString(formatter: NumberFormat?): String {
        return """${Runtime.format(m00.toDouble(), formatter)} ${Runtime.format(m10.toDouble(), formatter)} ${
            Runtime.format(
                m20.toDouble(), formatter
            )
        }
${Runtime.format(m01.toDouble(), formatter)} ${Runtime.format(m11.toDouble(), formatter)} ${
            Runtime.format(
                m21.toDouble(), formatter
            )
        }
${Runtime.format(m02.toDouble(), formatter)} ${Runtime.format(m12.toDouble(), formatter)} ${
            Runtime.format(
                m22.toDouble(), formatter
            )
        }
"""
    }

    operator fun get(dest: Matrix3f): Matrix3f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix4f): Matrix4f {
        return dest.set(this)
    }

    fun getRotation(dest: AxisAngle4f): AxisAngle4f {
        return dest.set(this)
    }

    fun getUnnormalizedRotation(dest: Quaternionf): Quaternionf {
        return dest.setFromUnnormalized(this)
    }

    fun getNormalizedRotation(dest: Quaternionf): Quaternionf {
        return dest.setFromNormalized(this)
    }

    fun getUnnormalizedRotation(dest: Quaterniond): Quaterniond {
        return dest.setFromUnnormalized(this)
    }

    fun getNormalizedRotation(dest: Quaterniond): Quaterniond {
        return dest.setFromNormalized(this)
    }

    @JvmOverloads
    operator fun get(arr: FloatArray, offset: Int = 0): FloatArray {
        MemUtil.INSTANCE.copy(this, arr, offset)
        return arr
    }

    fun zero(): Matrix3f {
        MemUtil.INSTANCE.zero(this)
        return this
    }

    fun identity(): Matrix3f {
        MemUtil.INSTANCE.identity(this)
        return this
    }

    fun scale(xyz: Vector3f, dest: Matrix3f): Matrix3f {
        return this.scale(xyz.x, xyz.y, xyz.z, dest)
    }

    fun scale(xyz: Vector3f): Matrix3f {
        return this.scale(xyz.x, xyz.y, xyz.z, this)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, z: Float, dest: Matrix3f = this): Matrix3f {
        dest.m00 = m00 * x
        dest.m01 = m01 * x
        dest.m02 = m02 * x
        dest.m10 = m10 * y
        dest.m11 = m11 * y
        dest.m12 = m12 * y
        dest.m20 = m20 * z
        dest.m21 = m21 * z
        dest.m22 = m22 * z
        return dest
    }

    fun scale(xyz: Float, dest: Matrix3f): Matrix3f {
        return this.scale(xyz, xyz, xyz, dest)
    }

    fun scale(xyz: Float): Matrix3f {
        return this.scale(xyz, xyz, xyz)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, z: Float, dest: Matrix3f = this): Matrix3f {
        val nm00 = x * m00
        val nm01 = y * m01
        val nm02 = z * m02
        val nm10 = x * m10
        val nm11 = y * m11
        val nm12 = z * m12
        val nm20 = x * m20
        val nm21 = y * m21
        val nm22 = z * m22
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    fun scaling(factor: Float): Matrix3f {
        MemUtil.INSTANCE.zero(this)
        m00 = factor
        m11 = factor
        m22 = factor
        return this
    }

    fun scaling(x: Float, y: Float, z: Float): Matrix3f {
        MemUtil.INSTANCE.zero(this)
        m00 = x
        m11 = y
        m22 = z
        return this
    }

    fun scaling(xyz: Vector3f): Matrix3f {
        return this.scaling(xyz.x, xyz.y, xyz.z)
    }

    fun rotation(angle: Float, axis: Vector3f): Matrix3f {
        return this.rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(axisAngle: AxisAngle4f): Matrix3f {
        return this.rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotation(angle: Float, x: Float, y: Float, z: Float): Matrix3f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
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
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
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
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
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
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
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
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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

    fun transform(v: Vector3f): Vector3f {
        return v.mul(this)
    }

    fun transform(v: Vector3f, dest: Vector3f?): Vector3f {
        return v.mul(this, dest!!)
    }

    fun transform(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        return dest.set(
            Math.fma(m00, x, Math.fma(m10, y, m20 * z)), Math.fma(
                m01, x, Math.fma(
                    m11, y, m21 * z
                )
            ), Math.fma(m02, x, Math.fma(m12, y, m22 * z))
        )
    }

    fun transformTranspose(v: Vector3f): Vector3f {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector3f, dest: Vector3f?): Vector3f {
        return v.mulTranspose(this, dest!!)
    }

    fun transformTranspose(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        return dest.set(
            Math.fma(m00, x, Math.fma(m01, y, m02 * z)), Math.fma(
                m10, x, Math.fma(
                    m11, y, m12 * z
                )
            ), Math.fma(m20, x, Math.fma(m21, y, m22 * z))
        )
    }

    @JvmOverloads
    fun rotateX(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val rm21 = -sin
        val nm10 = m10 * cos + m20 * sin
        val nm11 = m11 * cos + m21 * sin
        val nm12 = m12 * cos + m22 * sin
        dest.m20 = m10 * rm21 + m20 * cos
        dest.m21 = m11 * rm21 + m21 * cos
        dest.m22 = m12 * rm21 + m22 * cos
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m00 = m00
        dest.m01 = m01
        dest.m02 = m02
        return dest
    }

    @JvmOverloads
    fun rotateY(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val rm02 = -sin
        val nm00 = m00 * cos + m20 * rm02
        val nm01 = m01 * cos + m21 * rm02
        val nm02 = m02 * cos + m22 * rm02
        dest.m20 = m00 * sin + m20 * cos
        dest.m21 = m01 * sin + m21 * cos
        dest.m22 = m02 * sin + m22 * cos
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = m10
        dest.m11 = m11
        dest.m12 = m12
        return dest
    }

    @JvmOverloads
    fun rotateZ(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val rm10 = -sin
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        val nm02 = m02 * cos + m12 * sin
        dest.m10 = m00 * rm10 + m10 * cos
        dest.m11 = m01 * rm10 + m11 * cos
        dest.m12 = m02 * rm10 + m12 * cos
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m20 = m20
        dest.m21 = m21
        dest.m22 = m22
        return dest
    }

    fun rotateXYZ(angles: Vector3f): Matrix3f {
        return this.rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dest: Matrix3f = this): Matrix3f {
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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
        dest.m20 = m00 * sinY + nm20 * cosY
        dest.m21 = m01 * sinY + nm21 * cosY
        dest.m22 = m02 * sinY + nm22 * cosY
        dest.m00 = nm00 * cosZ + nm10 * sinZ
        dest.m01 = nm01 * cosZ + nm11 * sinZ
        dest.m02 = nm02 * cosZ + nm12 * sinZ
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ
        return dest
    }

    fun rotateZYX(angles: Vector3f): Matrix3f {
        return this.rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dest: Matrix3f = this): Matrix3f {
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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
        dest.m00 = nm00 * cosY + m20 * m_sinY
        dest.m01 = nm01 * cosY + m21 * m_sinY
        dest.m02 = nm02 * cosY + m22 * m_sinY
        dest.m10 = nm10 * cosX + nm20 * sinX
        dest.m11 = nm11 * cosX + nm21 * sinX
        dest.m12 = nm12 * cosX + nm22 * sinX
        dest.m20 = nm10 * m_sinX + nm20 * cosX
        dest.m21 = nm11 * m_sinX + nm21 * cosX
        dest.m22 = nm12 * m_sinX + nm22 * cosX
        return dest
    }

    fun rotateYXZ(angles: Vector3f): Matrix3f {
        return this.rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dest: Matrix3f = this): Matrix3f {
        val sinX = Math.sin(angleX)
        val cosX = Math.cosFromSin(sinX, angleX)
        val sinY = Math.sin(angleY)
        val cosY = Math.cosFromSin(sinY, angleY)
        val sinZ = Math.sin(angleZ)
        val cosZ = Math.cosFromSin(sinZ, angleZ)
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
        dest.m20 = m10 * m_sinX + nm20 * cosX
        dest.m21 = m11 * m_sinX + nm21 * cosX
        dest.m22 = m12 * m_sinX + nm22 * cosX
        dest.m00 = nm00 * cosZ + nm10 * sinZ
        dest.m01 = nm01 * cosZ + nm11 * sinZ
        dest.m02 = nm02 * cosZ + nm12 * sinZ
        dest.m10 = nm00 * m_sinZ + nm10 * cosZ
        dest.m11 = nm01 * m_sinZ + nm11 * cosZ
        dest.m12 = nm02 * m_sinZ + nm12 * cosZ
        return dest
    }

    @JvmOverloads
    fun rotate(ang: Float, x: Float, y: Float, z: Float, dest: Matrix3f = this): Matrix3f {
        val s = Math.sin(ang)
        val c = Math.cosFromSin(s, ang)
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
        dest.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22
        dest.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22
        dest.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        return dest
    }

    @JvmOverloads
    fun rotateLocal(ang: Float, x: Float, y: Float, z: Float, dest: Matrix3f = this): Matrix3f {
        val s = Math.sin(ang)
        val c = Math.cosFromSin(s, ang)
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
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun rotateLocalX(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val nm01 = cos * m01 - sin * m02
        val nm02 = sin * m01 + cos * m02
        val nm11 = cos * m11 - sin * m12
        val nm12 = sin * m11 + cos * m12
        val nm21 = cos * m21 - sin * m22
        val nm22 = sin * m21 + cos * m22
        dest.m00 = m00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = m10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = m20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun rotateLocalY(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val nm00 = cos * m00 + sin * m02
        val nm02 = -sin * m00 + cos * m02
        val nm10 = cos * m10 + sin * m12
        val nm12 = -sin * m10 + cos * m12
        val nm20 = cos * m20 + sin * m22
        val nm22 = -sin * m20 + cos * m22
        dest.m00 = nm00
        dest.m01 = m01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = m11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = m21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Float, dest: Matrix3f = this): Matrix3f {
        val sin = Math.sin(ang)
        val cos = Math.cosFromSin(sin, ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = m02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = m12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = m22
        return dest
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dest: Matrix3f = this): Matrix3f {
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
        dest.m20 = m00 * rm20 + m10 * rm21 + m20 * rm22
        dest.m21 = m01 * rm20 + m11 * rm21 + m21 * rm22
        dest.m22 = m02 * rm20 + m12 * rm21 + m22 * rm22
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        return dest
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaternionf, dest: Matrix3f = this): Matrix3f {
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
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    fun rotate(axisAngle: AxisAngle4f): Matrix3f {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4f, dest: Matrix3f): Matrix3f {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest)
    }

    fun rotate(angle: Float, axis: Vector3f): Matrix3f {
        return this.rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Float, axis: Vector3f, dest: Matrix3f): Matrix3f {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Matrix3f {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dest: Matrix3f): Matrix3f {
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
        dest: Matrix3f = this
    ): Matrix3f {
        var dirX = dirX
        var dirY = dirY
        var dirZ = dirZ
        val invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= -invDirLength
        dirY *= -invDirLength
        dirZ *= -invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        dest.m20 = m00 * leftZ + m10 * upnZ + m20 * dirZ
        dest.m21 = m01 * leftZ + m11 * upnZ + m21 * dirZ
        dest.m22 = m02 * leftZ + m12 * upnZ + m22 * dirZ
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        return dest
    }

    fun setLookAlong(dir: Vector3f, up: Vector3f): Matrix3f {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix3f {
        var dirX = dirX
        var dirY = dirY
        var dirZ = dirZ
        val invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= -invDirLength
        dirY *= -invDirLength
        dirZ *= -invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        m00 = leftX
        m01 = upnX
        m02 = dirX
        m10 = leftY
        m11 = upnY
        m12 = dirY
        m20 = leftZ
        m21 = upnZ
        m22 = dirZ
        return this
    }

    @Throws(IndexOutOfBoundsException::class)
    fun getRow(row: Int, dest: Vector3f): Vector3f {
        return when (row) {
            0 -> dest.set(m00, m10, m20)
            1 -> dest.set(m01, m11, m21)
            2 -> dest.set(m02, m12, m22)
            else -> throw IndexOutOfBoundsException()
        }
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setRow(row: Int, src: Vector3f): Matrix3f {
        return this.setRow(row, src.x, src.y, src.z)
    }

    @Throws(IndexOutOfBoundsException::class)
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
            2 -> {
                m02 = x
                m12 = y
                m22 = z
            }
            else -> throw IndexOutOfBoundsException()
        }
        return this
    }

    @Throws(IndexOutOfBoundsException::class)
    fun getColumn(column: Int, dest: Vector3f): Vector3f {
        return when (column) {
            0 -> dest.set(m00, m01, m02)
            1 -> dest.set(m10, m11, m12)
            2 -> dest.set(m20, m21, m22)
            else -> throw IndexOutOfBoundsException()
        }
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setColumn(column: Int, src: Vector3f): Matrix3f {
        return this.setColumn(column, src.x, src.y, src.z)
    }

    @Throws(IndexOutOfBoundsException::class)
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
            2 -> {
                m20 = x
                m21 = y
                m22 = z
            }
            else -> throw IndexOutOfBoundsException()
        }
        return this
    }

    operator fun get(column: Int, row: Int): Float {
        return MemUtil.INSTANCE[this, column, row]
    }

    operator fun set(column: Int, row: Int, value: Float): Matrix3f {
        return MemUtil.INSTANCE.set(this, column, row, value)
    }

    fun getRowColumn(row: Int, column: Int): Float {
        return MemUtil.INSTANCE[this, column, row]
    }

    fun setRowColumn(row: Int, column: Int, value: Float): Matrix3f {
        return MemUtil.INSTANCE.set(this, column, row, value)
    }

    @JvmOverloads
    fun normal(dest: Matrix3f = this): Matrix3f {
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
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    @JvmOverloads
    fun cofactor(dest: Matrix3f = this): Matrix3f {
        val nm00 = m11 * m22 - m21 * m12
        val nm01 = m20 * m12 - m10 * m22
        val nm02 = m10 * m21 - m20 * m11
        val nm10 = m21 * m02 - m01 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm12 = m20 * m01 - m00 * m21
        val nm20 = m01 * m12 - m11 * m02
        val nm21 = m02 * m10 - m12 * m00
        val nm22 = m00 * m11 - m10 * m01
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        dest.m20 = nm20
        dest.m21 = nm21
        dest.m22 = nm22
        return dest
    }

    fun getScale(dest: Vector3f): Vector3f {
        return dest.set(
            Math.sqrt(m00 * m00 + m01 * m01 + m02 * m02), Math.sqrt(
                m10 * m10 + m11 * m11 + m12 * m12
            ), Math.sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        )
    }

    fun positiveZ(dir: Vector3f): Vector3f {
        dir.x = m10 * m21 - m11 * m20
        dir.y = m20 * m01 - m21 * m00
        dir.z = m00 * m11 - m01 * m10
        return dir.normalize(dir)
    }

    fun normalizedPositiveZ(dir: Vector3f): Vector3f {
        dir.x = m02
        dir.y = m12
        dir.z = m22
        return dir
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
        result = 31 * result + java.lang.Float.floatToIntBits(m00)
        result = 31 * result + java.lang.Float.floatToIntBits(m01)
        result = 31 * result + java.lang.Float.floatToIntBits(m02)
        result = 31 * result + java.lang.Float.floatToIntBits(m10)
        result = 31 * result + java.lang.Float.floatToIntBits(m11)
        result = 31 * result + java.lang.Float.floatToIntBits(m12)
        result = 31 * result + java.lang.Float.floatToIntBits(m20)
        result = 31 * result + java.lang.Float.floatToIntBits(m21)
        result = 31 * result + java.lang.Float.floatToIntBits(m22)
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
            val other = obj as Matrix3f
            if (java.lang.Float.floatToIntBits(m00) != java.lang.Float.floatToIntBits(other.m00)) {
                false
            } else if (java.lang.Float.floatToIntBits(m01) != java.lang.Float.floatToIntBits(other.m01)) {
                false
            } else if (java.lang.Float.floatToIntBits(m02) != java.lang.Float.floatToIntBits(other.m02)) {
                false
            } else if (java.lang.Float.floatToIntBits(m10) != java.lang.Float.floatToIntBits(other.m10)) {
                false
            } else if (java.lang.Float.floatToIntBits(m11) != java.lang.Float.floatToIntBits(other.m11)) {
                false
            } else if (java.lang.Float.floatToIntBits(m12) != java.lang.Float.floatToIntBits(other.m12)) {
                false
            } else if (java.lang.Float.floatToIntBits(m20) != java.lang.Float.floatToIntBits(other.m20)) {
                false
            } else if (java.lang.Float.floatToIntBits(m21) != java.lang.Float.floatToIntBits(other.m21)) {
                false
            } else {
                java.lang.Float.floatToIntBits(m22) == java.lang.Float.floatToIntBits(other.m22)
            }
        }
    }

    fun equals(m: Matrix3f?, delta: Float): Boolean {
        return if (this === m) {
            true
        } else if (m == null) {
            false
        } else if (!Runtime.equals(m00, m.m00, delta)) {
            false
        } else if (!Runtime.equals(m01, m.m01, delta)) {
            false
        } else if (!Runtime.equals(m02, m.m02, delta)) {
            false
        } else if (!Runtime.equals(m10, m.m10, delta)) {
            false
        } else if (!Runtime.equals(m11, m.m11, delta)) {
            false
        } else if (!Runtime.equals(m12, m.m12, delta)) {
            false
        } else if (!Runtime.equals(m20, m.m20, delta)) {
            false
        } else if (!Runtime.equals(m21, m.m21, delta)) {
            false
        } else {
            Runtime.equals(m22, m.m22, delta)
        }
    }

    fun swap(other: Matrix3f?): Matrix3f {
        MemUtil.INSTANCE.swap(this, other)
        return this
    }

    @JvmOverloads
    fun add(other: Matrix3f, dest: Matrix3f = this): Matrix3f {
        dest.m00 = m00 + other.m00
        dest.m01 = m01 + other.m01
        dest.m02 = m02 + other.m02
        dest.m10 = m10 + other.m10
        dest.m11 = m11 + other.m11
        dest.m12 = m12 + other.m12
        dest.m20 = m20 + other.m20
        dest.m21 = m21 + other.m21
        dest.m22 = m22 + other.m22
        return dest
    }

    @JvmOverloads
    fun sub(subtrahend: Matrix3f, dest: Matrix3f = this): Matrix3f {
        dest.m00 = m00 - subtrahend.m00
        dest.m01 = m01 - subtrahend.m01
        dest.m02 = m02 - subtrahend.m02
        dest.m10 = m10 - subtrahend.m10
        dest.m11 = m11 - subtrahend.m11
        dest.m12 = m12 - subtrahend.m12
        dest.m20 = m20 - subtrahend.m20
        dest.m21 = m21 - subtrahend.m21
        dest.m22 = m22 - subtrahend.m22
        return dest
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix3f, dest: Matrix3f = this): Matrix3f {
        dest.m00 = m00 * other.m00
        dest.m01 = m01 * other.m01
        dest.m02 = m02 * other.m02
        dest.m10 = m10 * other.m10
        dest.m11 = m11 * other.m11
        dest.m12 = m12 * other.m12
        dest.m20 = m20 * other.m20
        dest.m21 = m21 * other.m21
        dest.m22 = m22 * other.m22
        return dest
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

    @JvmOverloads
    fun lerp(other: Matrix3f, t: Float, dest: Matrix3f = this): Matrix3f {
        dest.m00 = Math.fma(other.m00 - m00, t, m00)
        dest.m01 = Math.fma(other.m01 - m01, t, m01)
        dest.m02 = Math.fma(other.m02 - m02, t, m02)
        dest.m10 = Math.fma(other.m10 - m10, t, m10)
        dest.m11 = Math.fma(other.m11 - m11, t, m11)
        dest.m12 = Math.fma(other.m12 - m12, t, m12)
        dest.m20 = Math.fma(other.m20 - m20, t, m20)
        dest.m21 = Math.fma(other.m21 - m21, t, m21)
        dest.m22 = Math.fma(other.m22 - m22, t, m22)
        return dest
    }

    fun rotateTowards(direction: Vector3f, up: Vector3f, dest: Matrix3f): Matrix3f {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dest)
    }

    fun rotateTowards(direction: Vector3f, up: Vector3f): Matrix3f {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dest: Matrix3f = this
    ): Matrix3f {
        val invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val ndirX = dirX * invDirLength
        val ndirY = dirY * invDirLength
        val ndirZ = dirZ * invDirLength
        var leftX = upY * ndirZ - upZ * ndirY
        var leftY = upZ * ndirX - upX * ndirZ
        var leftZ = upX * ndirY - upY * ndirX
        val invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
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
        dest.m20 = m00 * ndirX + m10 * ndirY + m20 * ndirZ
        dest.m21 = m01 * ndirX + m11 * ndirY + m21 * ndirZ
        dest.m22 = m02 * ndirX + m12 * ndirY + m22 * ndirZ
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m02 = nm02
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m12 = nm12
        return dest
    }

    fun rotationTowards(dir: Vector3f, up: Vector3f): Matrix3f {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix3f {
        val invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        val ndirX = dirX * invDirLength
        val ndirY = dirY * invDirLength
        val ndirZ = dirZ * invDirLength
        var leftX = upY * ndirZ - upZ * ndirY
        var leftY = upZ * ndirX - upX * ndirZ
        var leftZ = upX * ndirY - upY * ndirX
        val invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
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

    fun getEulerAnglesZYX(dest: Vector3f): Vector3f {
        dest.x = Math.atan2(m12, m22)
        dest.y = Math.atan2(-m02, Math.sqrt(1f - m02 * m02))
        dest.z = Math.atan2(m01, m00)
        return dest
    }

    fun getEulerAnglesXYZ(dest: Vector3f): Vector3f {
        dest.x = Math.atan2(-m21, m22)
        dest.y = Math.atan2(m20, Math.sqrt(1f - m20 * m20))
        dest.z = Math.atan2(-m10, m00)
        return dest
    }

    fun obliqueZ(a: Float, b: Float): Matrix3f {
        m20 += m00 * a + m10 * b
        m21 += m01 * a + m11 * b
        m22 += m02 * a + m12 * b
        return this
    }

    fun obliqueZ(a: Float, b: Float, dest: Matrix3f): Matrix3f {
        dest.m00 = m00
        dest.m01 = m01
        dest.m02 = m02
        dest.m10 = m10
        dest.m11 = m11
        dest.m12 = m12
        dest.m20 = m00 * a + m10 * b + m20
        dest.m21 = m01 * a + m11 * b + m21
        dest.m22 = m02 * a + m12 * b + m22
        return dest
    }

    @JvmOverloads
    fun reflect(nx: Float, ny: Float, nz: Float, dest: Matrix3f = this): Matrix3f {
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
        return dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)
    }

    fun reflect(normal: Vector3f): Matrix3f {
        return this.reflect(normal.x, normal.y, normal.z)
    }

    @JvmOverloads
    fun reflect(orientation: Quaternionf, dest: Matrix3f = this): Matrix3f {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1f - (orientation.x * num1 + orientation.y * num2)
        return this.reflect(normalX, normalY, normalZ, dest)
    }

    fun reflect(normal: Vector3f, dest: Matrix3f): Matrix3f {
        return this.reflect(normal.x, normal.y, normal.z, dest)
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
        return this.reflection(normal.x, normal.y, normal.z)
    }

    fun reflection(orientation: Quaternionf): Matrix3f {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1f - (orientation.x * num1 + orientation.y * num2)
        return this.reflection(normalX, normalY, normalZ)
    }

    val isFinite: Boolean
        get() = Math.isFinite(m00) && Math.isFinite(m01) && Math.isFinite(m02) && Math.isFinite(
            m10
        ) && Math.isFinite(m11) && Math.isFinite(m12) && Math.isFinite(m20) && Math.isFinite(
            m21
        ) && Math.isFinite(m22)

    fun quadraticFormProduct(x: Float, y: Float, z: Float): Float {
        val Axx = m00 * x + m10 * y + m20 * z
        val Axy = m01 * x + m11 * y + m21 * z
        val Axz = m02 * x + m12 * y + m22 * z
        return x * Axx + y * Axy + z * Axz
    }

    fun quadraticFormProduct(v: Vector3f): Float {
        return this.quadraticFormProduct(v.x, v.y, v.z)
    }

    @JvmOverloads
    fun mapXZY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(m20)._m11(m21)._m12(m22)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapXZnY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(m20)._m11(m21)._m12(m22)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapXnYnZ(dest: Matrix3f = this): Matrix3f {
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapXnZY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapXnZnY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapYXZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(m00)._m11(m01)._m12(m02)._m20(m20)._m21(m21)._m22(m22)
    }

    @JvmOverloads
    fun mapYXnZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(m00)._m11(m01)._m12(m02)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapYZX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(m20)._m11(m21)._m12(m22)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapYZnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(m20)._m11(m21)._m12(m22)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapYnXZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m20)._m21(m21)._m22(m22)
    }

    @JvmOverloads
    fun mapYnXnZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapYnZX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapYnZnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m10)._m01(m11)._m02(m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapZXY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapZXnY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapZYX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(m10)._m11(m11)._m12(m12)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapZYnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(m10)._m11(m11)._m12(m12)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapZnXY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapZnXnY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapZnYX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapZnYnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapnXYnZ(dest: Matrix3f = this): Matrix3f {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(m10)._m11(m11)._m12(m12)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapnXZY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(m20)._m11(m21)._m12(m22)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapnXZnY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(m20)._m11(m21)._m12(m22)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapnXnYZ(dest: Matrix3f = this): Matrix3f {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m20)._m21(m21)._m22(m22)
    }

    @JvmOverloads
    fun mapnXnYnZ(dest: Matrix3f = this): Matrix3f {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapnXnZY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapnXnZnY(dest: Matrix3f = this): Matrix3f {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapnYXZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(m00)._m11(m01)._m12(m02)._m20(m20)._m21(m21)._m22(m22)
    }

    @JvmOverloads
    fun mapnYXnZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(m00)._m11(m01)._m12(m02)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapnYZX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(m20)._m11(m21)._m12(m22)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapnYZnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(m20)._m11(m21)._m12(m22)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapnYnXZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m20)._m21(m21)._m22(m22)
    }

    @JvmOverloads
    fun mapnYnXnZ(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m20)._m21(-m21)._m22(-m22)
    }

    @JvmOverloads
    fun mapnYnZX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapnYnZnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapnZXY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapnZXnY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapnZYX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(m10)._m11(m11)._m12(m12)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapnZYnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(m10)._m11(m11)._m12(m12)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    @JvmOverloads
    fun mapnZnXY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)
    }

    @JvmOverloads
    fun mapnZnXnY(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)
    }

    @JvmOverloads
    fun mapnZnYX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m00)._m21(m01)._m22(m02)
    }

    @JvmOverloads
    fun mapnZnYnX(dest: Matrix3f = this): Matrix3f {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m00)._m21(-m01)._m22(-m02)
    }

    fun negateX(): Matrix3f {
        return _m00(-m00)._m01(-m01)._m02(-m02)
    }

    fun negateX(dest: Matrix3f): Matrix3f {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m10(m10)._m11(m11)._m12(m12)._m20(m20)._m21(m21)._m22(m22)
    }

    fun negateY(): Matrix3f {
        return _m10(-m10)._m11(-m11)._m12(-m12)
    }

    fun negateY(dest: Matrix3f): Matrix3f {
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m20)._m21(m21)._m22(m22)
    }

    fun negateZ(): Matrix3f {
        return _m20(-m20)._m21(-m21)._m22(-m22)
    }

    fun negateZ(dest: Matrix3f): Matrix3f {
        return dest._m00(m00)._m01(m01)._m02(m02)._m10(m10)._m11(m11)._m12(m12)._m20(-m20)._m21(-m21)._m22(-m22)
    }
}