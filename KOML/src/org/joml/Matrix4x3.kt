package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import org.joml.Vector3d.Companion.lengthSquared
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mixed precision matrix for open world/galaxy games:
 * - large position range with high accuracy
 * - scaling and rotating with normal accuracy
 * */
open class Matrix4x3 : Matrix<Matrix4x3, Vector3d, Vector4d> {

    var m00 = 1f
    var m01 = 0f
    var m02 = 0f
    var m10 = 0f
    var m11 = 1f
    var m12 = 0f
    var m20 = 0f
    var m21 = 0f
    var m22 = 1f
    var m30 = 0.0
    var m31 = 0.0
    var m32 = 0.0

    var flags = 28

    constructor()

    constructor(mat: Matrix3f) {
        this.set(mat)
    }

    constructor(mat: Matrix4x3) {
        this.set(mat)
    }

    constructor(mat: Matrix4x3d) {
        this.set(mat)
    }

    constructor(
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float,
        m30: Double, m31: Double, m32: Double
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
        this.m30 = m30
        this.m31 = m31
        this.m32 = m32
        determineProperties()
    }

    constructor(col0: Vector3f, col1: Vector3f, col2: Vector3f, col3: Vector3d) {
        this.set(col0, col1, col2, col3).determineProperties()
    }

    override val numCols: Int get() = 4
    override val numRows: Int get() = 3

    fun assume(properties: Int): Matrix4x3 {
        this.flags = properties
        return this
    }

    fun determineProperties(): Matrix4x3 {
        var flags = 0
        if (m00 == 1f && m01 == 0f && m02 == 0f && m10 == 0f && m11 == 1f && m12 == 0f && m20 == 0f && m21 == 0f && m22 == 1f) {
            flags = flags or 24
            if (m30 == 0.0 && m31 == 0.0 && m32 == 0.0) {
                flags = flags or 4
            }
        }
        this.flags = flags
        return this
    }

    fun properties(): Int {
        return flags
    }

    fun _properties(properties: Int): Matrix4x3 {
        this.flags = properties
        return this
    }

    fun _m00(m00: Float): Matrix4x3 {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Float): Matrix4x3 {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Float): Matrix4x3 {
        this.m02 = m02
        return this
    }

    fun _m10(m10: Float): Matrix4x3 {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Float): Matrix4x3 {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Float): Matrix4x3 {
        this.m12 = m12
        return this
    }

    fun _m20(m20: Float): Matrix4x3 {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Float): Matrix4x3 {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Float): Matrix4x3 {
        this.m22 = m22
        return this
    }

    fun _m30(m30: Double): Matrix4x3 {
        this.m30 = m30
        return this
    }

    fun _m31(m31: Double): Matrix4x3 {
        this.m31 = m31
        return this
    }

    fun _m32(m32: Double): Matrix4x3 {
        this.m32 = m32
        return this
    }

    fun identity(): Matrix4x3 {
        if (flags and 4 == 0) {
            m00 = 1f
            m01 = 0f
            m02 = 0f
            m10 = 0f
            m11 = 1f
            m12 = 0f
            m20 = 0f
            m21 = 0f
            m22 = 1f
            m30 = 0.0
            m31 = 0.0
            m32 = 0.0
            flags = 28
        }
        return this
    }

    fun set(m: Matrix4x3): Matrix4x3 {
        m00 = m.m00
        m01 = m.m01
        m02 = m.m02
        m10 = m.m10
        m11 = m.m11
        m12 = m.m12
        m20 = m.m20
        m21 = m.m21
        m22 = m.m22
        m30 = m.m30
        m31 = m.m31
        m32 = m.m32
        flags = m.properties()
        return this
    }

    fun set(m: Matrix4x3f): Matrix4x3 {
        m00 = m.m00
        m01 = m.m01
        m02 = m.m02
        m10 = m.m10
        m11 = m.m11
        m12 = m.m12
        m20 = m.m20
        m21 = m.m21
        m22 = m.m22
        m30 = m.m30.toDouble()
        m31 = m.m31.toDouble()
        m32 = m.m32.toDouble()
        flags = m.properties()
        return this
    }

    fun set(m: Matrix4f): Matrix4x3 {
        m00 = m.m00
        m01 = m.m01
        m02 = m.m02
        m10 = m.m10
        m11 = m.m11
        m12 = m.m12
        m20 = m.m20
        m21 = m.m21
        m22 = m.m22
        m30 = m.m30.toDouble()
        m31 = m.m31.toDouble()
        m32 = m.m32.toDouble()
        flags = m.flags and 28
        return this
    }

    fun get(dst: Matrix4f): Matrix4f {
        return dst.set4x3(this)
    }

    fun get(dst: Matrix4d): Matrix4d {
        return dst.set4x3(this)
    }

    fun set(mat: Matrix3f): Matrix4x3 {
        m00 = mat.m00
        m01 = mat.m01
        m02 = mat.m02
        m10 = mat.m10
        m11 = mat.m11
        m12 = mat.m12
        m20 = mat.m20
        m21 = mat.m21
        m22 = mat.m22
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        return determineProperties()
    }

    fun set(mat: Matrix3d): Matrix4x3 {
        m00 = mat.m00.toFloat()
        m01 = mat.m01.toFloat()
        m02 = mat.m02.toFloat()
        m10 = mat.m10.toFloat()
        m11 = mat.m11.toFloat()
        m12 = mat.m12.toFloat()
        m20 = mat.m20.toFloat()
        m21 = mat.m21.toFloat()
        m22 = mat.m22.toFloat()
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        return determineProperties()
    }

    fun set(axisAngle: AxisAngle4f): Matrix4x3 {
        var x = axisAngle.x
        var y = axisAngle.y
        var z = axisAngle.z
        val angle = axisAngle.angle
        var n = sqrt(x * x + y * y + z * z)
        n = 1f / n
        x *= n
        y *= n
        z *= n
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun set(axisAngle: AxisAngle4d): Matrix4x3 {
        var x = axisAngle.x
        var y = axisAngle.y
        var z = axisAngle.z
        val angle = axisAngle.angle
        var n = sqrt(x * x + y * y + z * z)
        n = 1.0 / n
        x *= n
        y *= n
        z *= n
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun set(q: Quaternionf): Matrix4x3 {
        return rotation(q)
    }

    fun set(q: Quaterniond): Matrix4x3 {
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
        flags = 16
        return this
    }

    fun set(col0: Vector3f, col1: Vector3f, col2: Vector3f, col3: Vector3d): Matrix4x3 {
        m00 = col0.x
        m01 = col0.y
        m02 = col0.z
        m10 = col1.x
        m11 = col1.y
        m12 = col1.z
        m20 = col2.x
        m21 = col2.y
        m22 = col2.z
        m30 = col3.x
        m31 = col3.y
        m32 = col3.z
        return determineProperties()
    }

    fun set3x3(mat: Matrix4x3): Matrix4x3 {
        m00 = mat.m00
        m01 = mat.m01
        m02 = mat.m02
        m10 = mat.m10
        m11 = mat.m11
        m12 = mat.m12
        m20 = mat.m20
        m21 = mat.m21
        m22 = mat.m22
        flags = flags and mat.properties()
        return this
    }

    override fun get(column: Int, row: Int): Double {
        return when (column * 3 + row) {
            0 -> m00.toDouble()
            1 -> m01.toDouble()
            2 -> m02.toDouble()
            3 -> m10.toDouble()
            4 -> m11.toDouble()
            5 -> m12.toDouble()
            6 -> m20.toDouble()
            7 -> m21.toDouble()
            8 -> m22.toDouble()
            9 -> m30
            10 -> m31
            else -> m32
        }
    }

    override fun set(column: Int, row: Int, value: Double): Matrix4x3 {
        when (column * 3 + row) {
            0 -> m00 = value.toFloat()
            1 -> m01 = value.toFloat()
            2 -> m02 = value.toFloat()
            3 -> m10 = value.toFloat()
            4 -> m11 = value.toFloat()
            5 -> m12 = value.toFloat()
            6 -> m20 = value.toFloat()
            7 -> m21 = value.toFloat()
            8 -> m22 = value.toFloat()
            9 -> m30 = value
            10 -> m31 = value
            else -> m32 = value
        }
        return _properties(0)
    }

    @JvmOverloads
    fun mul(right: Matrix4x3f, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else if (flags and 8 != 0) {
            mulTranslation(right, dst)
        } else mulGeneric(right, dst)
    }

    @JvmOverloads
    fun mul(right: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else if (flags and 8 != 0) {
            mulTranslation(right, dst)
        } else mulGeneric(right, dst)
    }

    private fun mulGeneric(right: Matrix4x3f, dst: Matrix4x3): Matrix4x3 {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        val m20 = m20
        val m21 = m21
        val m22 = m22
        val rm00 = right.m00
        val rm01 = right.m01
        val rm02 = right.m02
        val rm10 = right.m10
        val rm11 = right.m11
        val rm12 = right.m12
        val rm20 = right.m20
        val rm21 = right.m21
        val rm22 = right.m22
        val rm30 = right.m30
        val rm31 = right.m31
        val rm32 = right.m32
        return dst
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)
            ._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)
            ._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)
            ._properties(flags and right.properties() and 16)
    }

    private fun mulGeneric(right: Matrix4x3, dst: Matrix4x3): Matrix4x3 {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        val m20 = m20
        val m21 = m21
        val m22 = m22
        val rm00 = right.m00
        val rm01 = right.m01
        val rm02 = right.m02
        val rm10 = right.m10
        val rm11 = right.m11
        val rm12 = right.m12
        val rm20 = right.m20
        val rm21 = right.m21
        val rm22 = right.m22
        val rm30 = right.m30
        val rm31 = right.m31
        val rm32 = right.m32
        return dst
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)
            ._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)
            ._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)
            ._properties(flags and right.properties() and 16)
    }

    private fun mulGeneric(
        r00: Float, r01: Float, r02: Float,
        r10: Float, r11: Float, r12: Float,
        r20: Float, r21: Float, r22: Float,
        r30: Float, r31: Float, r32: Float,
        dst: Matrix4x3
    ): Matrix4x3 {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        val m20 = m20
        val m21 = m21
        val m22 = m22
        val rm00 = r00
        val rm01 = r01
        val rm02 = r02
        val rm10 = r10
        val rm11 = r11
        val rm12 = r12
        val rm20 = r20
        val rm21 = r21
        val rm22 = r22
        val rm30 = r30
        val rm31 = r31
        val rm32 = r32
        return dst._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)
            ._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)
            ._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)
            ._properties(0)
    }

    fun mulTranslation(right: Matrix4x3, dst: Matrix4x3): Matrix4x3 {
        return dst._m00(right.m00)._m01(right.m01)._m02(right.m02)._m10(right.m10)._m11(right.m11)._m12(right.m12)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)._m30(right.m30 + m30)._m31(right.m31 + m31)
            ._m32(right.m32 + m32)._properties(right.properties() and 16)
    }

    fun mulTranslation(right: Matrix4x3f, dst: Matrix4x3): Matrix4x3 {
        return dst._m00(right.m00)._m01(right.m01)._m02(right.m02)._m10(right.m10)._m11(right.m11)._m12(right.m12)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)._m30(right.m30 + m30)._m31(right.m31 + m31)
            ._m32(right.m32 + m32)._properties(right.properties() and 16)
    }

    @JvmOverloads
    fun mulOrtho(view: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        val nm00 = m00 * view.m00
        val nm01 = m11 * view.m01
        val nm02 = m22 * view.m02
        val nm10 = m00 * view.m10
        val nm11 = m11 * view.m11
        val nm12 = m22 * view.m12
        val nm20 = m00 * view.m20
        val nm21 = m11 * view.m21
        val nm22 = m22 * view.m22
        val nm30 = m00 * view.m30 + m30
        val nm31 = m11 * view.m31 + m31
        val nm32 = m22 * view.m32 + m32
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = flags and view.properties() and 16
        return dst
    }

    @JvmOverloads
    fun mul3x3(
        rm00: Float, rm01: Float, rm02: Float, rm10: Float, rm11: Float, rm12: Float,
        rm20: Float, rm21: Float, rm22: Float, dst: Matrix4x3 = this
    ): Matrix4x3 {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        val m20 = m20
        val m21 = m21
        val m22 = m22
        return dst
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)
            ._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)
            ._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m30(m30)._m31(m31)._m32(m32)._properties(0)
    }

    fun mul(right: Matrix4x3d, dst: Matrix4x3 = this): Matrix4x3 {
        return mulGeneric(
            right.m00.toFloat(), right.m01.toFloat(), right.m02.toFloat(),
            right.m10.toFloat(), right.m11.toFloat(), right.m12.toFloat(),
            right.m20.toFloat(), right.m21.toFloat(), right.m22.toFloat(),
            right.m30.toFloat(), right.m31.toFloat(), right.m32.toFloat(), dst
        )
    }

    @JvmOverloads
    fun fma(other: Matrix4x3, otherFactor: Float, dst: Matrix4x3 = this): Matrix4x3 {
        dst._m00(other.m00 * otherFactor + m00)._m01(other.m01 * otherFactor + m01)
            ._m02(other.m02 * otherFactor + m02)._m10(other.m10 * otherFactor + m10)
            ._m11(other.m11 * otherFactor + m11)._m12(other.m12 * otherFactor + m12)
            ._m20(other.m20 * otherFactor + m20)._m21(other.m21 * otherFactor + m21)
            ._m22(other.m22 * otherFactor + m22)._m30(other.m30 * otherFactor + m30)
            ._m31(other.m31 * otherFactor + m31)._m32(other.m32 * otherFactor + m32)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun add(other: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        dst.m00 = m00 + other.m00
        dst.m01 = m01 + other.m01
        dst.m02 = m02 + other.m02
        dst.m10 = m10 + other.m10
        dst.m11 = m11 + other.m11
        dst.m12 = m12 + other.m12
        dst.m20 = m20 + other.m20
        dst.m21 = m21 + other.m21
        dst.m22 = m22 + other.m22
        dst.m30 = m30 + other.m30
        dst.m31 = m31 + other.m31
        dst.m32 = m32 + other.m32
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun sub(subtrahend: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        dst.m00 = m00 - subtrahend.m00
        dst.m01 = m01 - subtrahend.m01
        dst.m02 = m02 - subtrahend.m02
        dst.m10 = m10 - subtrahend.m10
        dst.m11 = m11 - subtrahend.m11
        dst.m12 = m12 - subtrahend.m12
        dst.m20 = m20 - subtrahend.m20
        dst.m21 = m21 - subtrahend.m21
        dst.m22 = m22 - subtrahend.m22
        dst.m30 = m30 - subtrahend.m30
        dst.m31 = m31 - subtrahend.m31
        dst.m32 = m32 - subtrahend.m32
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        dst.m00 = m00 * other.m00
        dst.m01 = m01 * other.m01
        dst.m02 = m02 * other.m02
        dst.m10 = m10 * other.m10
        dst.m11 = m11 * other.m11
        dst.m12 = m12 * other.m12
        dst.m20 = m20 * other.m20
        dst.m21 = m21 * other.m21
        dst.m22 = m22 * other.m22
        dst.m30 = m30 * other.m30
        dst.m31 = m31 * other.m31
        dst.m32 = m32 * other.m32
        dst.flags = 0
        return dst
    }

    fun set(
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float,
        m30: Double, m31: Double, m32: Double
    ): Matrix4x3 {
        this.m00 = m00
        this.m01 = m01
        this.m02 = m02
        this.m10 = m10
        this.m11 = m11
        this.m12 = m12
        this.m20 = m20
        this.m21 = m21
        this.m22 = m22
        this.m30 = m30
        this.m31 = m31
        this.m32 = m32
        return determineProperties()
    }

    fun set(src: Matrix4x3d): Matrix4x3 {
        return set(
            src.m00.toFloat(), src.m01.toFloat(), src.m02.toFloat(),
            src.m10.toFloat(), src.m11.toFloat(), src.m12.toFloat(),
            src.m20.toFloat(), src.m21.toFloat(), src.m22.toFloat(),
            src.m30, src.m31, src.m32
        )
    }

    fun determinant(): Float {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (flags and 16 != 0) this.invertOrthonormal(dst)
            else this.invertGeneric(dst)
        }
    }

    private fun invertGeneric(dst: Matrix4x3): Matrix4x3 {
        val m11m00 = m00 * m11
        val m10m01 = m01 * m10
        val m10m02 = m02 * m10
        val m12m00 = m00 * m12
        val m12m01 = m01 * m12
        val m11m02 = m02 * m11
        val s = 1f / ((m11m00 - m10m01) * m22 + (m10m02 - m12m00) * m21 + (m12m01 - m11m02) * m20)
        val m10m22 = m10 * m22
        val m10m21 = m10 * m21
        val m11m22 = m11 * m22
        val m11m20 = m11 * m20
        val m12m21 = m12 * m21
        val m12m20 = m12 * m20
        val m20m02 = m20 * m02
        val m20m01 = m20 * m01
        val m21m02 = m21 * m02
        val m21m00 = m21 * m00
        val m22m01 = m22 * m01
        val m22m00 = m22 * m00
        val nm00 = (m11m22 - m12m21) * s
        val nm01 = (m21m02 - m22m01) * s
        val nm02 = (m12m01 - m11m02) * s
        val nm10 = (m12m20 - m10m22) * s
        val nm11 = (m22m00 - m20m02) * s
        val nm12 = (m10m02 - m12m00) * s
        val nm20 = (m10m21 - m11m20) * s
        val nm21 = (m20m01 - m21m00) * s
        val nm22 = (m11m00 - m10m01) * s
        val nm30 = (m10m22 * m31 - m10m21 * m32 + m11m20 * m32 - m11m22 * m30 + m12m21 * m30 - m12m20 * m31) * s
        val nm31 = (m20m02 * m31 - m20m01 * m32 + m21m00 * m32 - m21m02 * m30 + m22m01 * m30 - m22m00 * m31) * s
        val nm32 = (m11m02 * m30 - m12m01 * m30 + m12m00 * m31 - m10m02 * m31 + m10m01 * m32 - m11m00 * m32) * s
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = 0
        return dst
    }

    private fun invertOrthonormal(dst: Matrix4x3): Matrix4x3 {
        val nm30 = -(m00 * m30 + m01 * m31 + m02 * m32)
        val nm31 = -(m10 * m30 + m11 * m31 + m12 * m32)
        val nm32 = -(m20 * m30 + m21 * m31 + m22 * m32)
        val m01 = m01
        val m02 = m02
        val m12 = m12
        dst.m00 = m00
        dst.m01 = m10
        dst.m02 = m20
        dst.m10 = m01
        dst.m11 = m11
        dst.m12 = m21
        dst.m20 = m02
        dst.m21 = m12
        dst.m22 = m22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = 16
        return dst
    }

    @JvmOverloads
    fun invertOrtho(dst: Matrix4x3 = this): Matrix4x3 {
        val invM00 = 1f / m00
        val invM11 = 1f / m11
        val invM22 = 1f / m22
        dst.set(invM00, 0f, 0f, 0f, invM11, 0f, 0f, 0f, invM22, -m30 * invM00, -m31 * invM11, -m32 * invM22)
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun transpose3x3(dst: Matrix4x3 = this): Matrix4x3 {
        dst.set(m00, m10, m20, m01, m11, m21, m02, m12, m22, m30, m31, m32)
        dst.flags = flags
        return dst
    }

    fun transpose3x3(dst: Matrix3f): Matrix3f {
        return dst.set(m00, m10, m20, m01, m11, m21, m02, m12, m22)
    }

    fun translation(x: Double, y: Double, z: Double): Matrix4x3 {
        if (flags and 4 == 0) {
            identity()
        }
        m30 = x
        m31 = y
        m32 = z
        flags = 24
        return this
    }

    fun translation(offset: Vector3d): Matrix4x3 {
        return translation(offset.x, offset.y, offset.z)
    }

    fun setTranslation(x: Double, y: Double, z: Double): Matrix4x3 {
        m30 = x
        m31 = y
        m32 = z
        flags = flags and -5
        return this
    }

    fun setTranslation(xyz: Vector3d): Matrix4x3 {
        return setTranslation(xyz.x, xyz.y, xyz.z)
    }

    fun getTranslation(dst: Vector3d): Vector3d {
        dst.x = m30
        dst.y = m31
        dst.z = m32
        return dst
    }

    fun getScale(dst: Vector3f): Vector3f {
        dst.x = sqrt(m00 * m00 + m01 * m01 + m02 * m02)
        dst.y = sqrt(m10 * m10 + m11 * m11 + m12 * m12)
        dst.z = sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst
    }

    fun getScale(dst: Vector3d): Vector3d {
        val m00 = m00.toDouble()
        val m01 = m01.toDouble()
        val m02 = m02.toDouble()
        val m10 = m10.toDouble()
        val m11 = m11.toDouble()
        val m12 = m12.toDouble()
        val m20 = m20.toDouble()
        val m21 = m21.toDouble()
        val m22 = m22.toDouble()
        dst.x = sqrt(m00 * m00 + m01 * m01 + m02 * m02)
        dst.y = sqrt(m10 * m10 + m11 * m11 + m12 * m12)
        dst.z = sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst
    }

    fun getScaleLength(): Float {
        return sqrt(getScaleLengthSquared())
    }

    fun getScaleLengthSquared(): Float {
        return m00 * m00 + m01 * m01 + m02 * m02 +
                m10 * m10 + m11 * m11 + m12 * m12 +
                m20 * m20 + m21 * m21 + m22 * m22
    }

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)} ${f(m30)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)} ${f(m31)}] " +
                "[${f(m02)} ${f(m12)} ${f(m22)} ${f(m32)}]]").addSigns()

    fun get(dst: Matrix4x3): Matrix4x3 {
        return dst.set(this)
    }

    fun get(dst: Matrix4x3d): Matrix4x3d {
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
        arr[offset + 9] = m30.toFloat()
        arr[offset + 10] = m31.toFloat()
        arr[offset + 11] = m32.toFloat()
        return arr
    }

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01).put(m02)
        arr.put(m10).put(m11).put(m12)
        arr.put(m20).put(m21).put(m22)
        arr.put(m30.toFloat()).put(m31.toFloat()).put(m32.toFloat())
        return arr
    }

    fun getTransposed(arr: FloatArray, offset: Int): FloatArray {
        arr[offset] = m00
        arr[offset + 1] = m10
        arr[offset + 2] = m20
        arr[offset + 3] = m30.toFloat()
        arr[offset + 4] = m01
        arr[offset + 5] = m11
        arr[offset + 6] = m21
        arr[offset + 7] = m31.toFloat()
        arr[offset + 8] = m02
        arr[offset + 9] = m12
        arr[offset + 10] = m22
        arr[offset + 11] = m32.toFloat()
        return arr
    }

    fun getTransposed(arr: FloatArray): FloatArray {
        return getTransposed(arr, 0)
    }

    fun zero(): Matrix4x3 {
        return scaling(0f)
    }

    fun scaling(factor: Float): Matrix4x3 {
        return scaling(factor, factor, factor)
    }

    fun scaling(x: Float, y: Float, z: Float): Matrix4x3 {
        if (flags and 4 == 0) {
            identity()
        }
        m00 = x
        m11 = y
        m22 = z
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        flags = if (one) 16 else 0
        return this
    }

    fun scaling(xyz: Vector3f): Matrix4x3 {
        return scaling(xyz.x, xyz.y, xyz.z)
    }

    fun rotation(angle: Float, axis: Vector3f): Matrix4x3 {
        return rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(axisAngle: AxisAngle4f): Matrix4x3 {
        return rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotation(angle: Float, x: Float, y: Float, z: Float): Matrix4x3 {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotationX(x * angle)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotationY(y * angle)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotationZ(z * angle)
            else rotationInternal(angle, x, y, z)
        }
    }

    private fun rotationInternal(angle: Float, x: Float, y: Float, z: Float): Matrix4x3 {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1f - cos
        val xy = x * y
        val xz = x * z
        val yz = y * z
        m00 = cos + x * x * C
        m01 = xy * C + z * sin
        m02 = xz * C - y * sin
        m10 = xy * C - z * sin
        m11 = cos + y * y * C
        m12 = yz * C + x * sin
        m20 = xz * C + y * sin
        m21 = yz * C - x * sin
        m22 = cos + z * z * C
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationX(ang: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationY(ang: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationZ(ang: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationZYX(angleZ: Float, angleY: Float, angleX: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun setRotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Matrix4x3 {
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
        flags = flags and -13
        return this
    }

    fun setRotationZYX(angleZ: Float, angleY: Float, angleX: Float): Matrix4x3 {
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
        flags = flags and -13
        return this
    }

    fun setRotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Matrix4x3 {
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
        flags = flags and -13
        return this
    }

    fun rotation(quat: Quaternionf): Matrix4x3 {
        return rotationQ(quat.x, quat.y, quat.z, quat.w)
    }

    fun rotationQ(qx: Float, qy: Float, qz: Float, qw: Float): Matrix4x3 {
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
        _m00(w2 + x2 - z2 - y2)
        _m01(dxy + dzw)
        _m02(dxz - dyw)
        _m10(dxy - dzw)
        _m11(y2 - z2 + w2 - x2)
        _m12(dyz + dxw)
        _m20(dyw + dxz)
        _m21(dyz - dxw)
        _m22(z2 - y2 - x2 + w2)
        _m30(0.0)
        _m31(0.0)
        _m32(0.0)
        flags = 16
        return this
    }

    fun translationRotateScale(
        tx: Double, ty: Double, tz: Double,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float
    ): Matrix4x3 {
        val dqx = qx + qx
        val dqy = qy + qy
        val dqz = qz + qz
        val q00 = dqx * qx
        val q11 = dqy * qy
        val q22 = dqz * qz
        val q01 = dqx * qy
        val q02 = dqx * qz
        val q03 = dqx * qw
        val q12 = dqy * qz
        val q13 = dqy * qw
        val q23 = dqz * qw
        m00 = sx - (q11 + q22) * sx
        m01 = (q01 + q23) * sx
        m02 = (q02 - q13) * sx
        m10 = (q01 - q23) * sy
        m11 = sy - (q22 + q00) * sy
        m12 = (q12 + q03) * sy
        m20 = (q02 + q13) * sz
        m21 = (q12 - q03) * sz
        m22 = sz - (q11 + q00) * sz
        m30 = tx
        m31 = ty
        m32 = tz
        flags = 0
        return this
    }

    fun translationRotateScale(translation: Vector3d, quat: Quaternionf, scale: Vector3f): Matrix4x3 {
        return translationRotateScale(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale.x, scale.y, scale.z
        )
    }

    fun translationRotateScaleMul(
        tx: Double, ty: Double, tz: Double,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float, m: Matrix4x3
    ): Matrix4x3 {
        val dqx = qx + qx
        val dqy = qy + qy
        val dqz = qz + qz
        val q00 = dqx * qx
        val q11 = dqy * qy
        val q22 = dqz * qz
        val q01 = dqx * qy
        val q02 = dqx * qz
        val q03 = dqx * qw
        val q12 = dqy * qz
        val q13 = dqy * qw
        val q23 = dqz * qw
        val nm00 = sx - (q11 + q22) * sx
        val nm01 = (q01 + q23) * sx
        val nm02 = (q02 - q13) * sx
        val nm10 = (q01 - q23) * sy
        val nm11 = sy - (q22 + q00) * sy
        val nm12 = (q12 + q03) * sy
        val nm20 = (q02 + q13) * sz
        val nm21 = (q12 - q03) * sz
        val nm22 = sz - (q11 + q00) * sz
        val m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02
        val m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02
        m02 = nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02
        this.m00 = m00
        this.m01 = m01
        val m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12
        val m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12
        m12 = nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12
        this.m10 = m10
        this.m11 = m11
        val m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22
        val m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22
        m22 = nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22
        this.m20 = m20
        this.m21 = m21
        val m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx
        val m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty
        m32 = nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz
        this.m30 = m30
        this.m31 = m31
        flags = 0
        return this
    }

    fun translationRotateScaleMul(
        translation: Vector3d,
        quat: Quaternionf,
        scale: Vector3f,
        m: Matrix4x3
    ): Matrix4x3 {
        return translationRotateScaleMul(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale.x, scale.y, scale.z, m
        )
    }

    fun translationRotate(tx: Double, ty: Double, tz: Double, quat: Quaternionf): Matrix4x3 {
        val dqx = quat.x + quat.x
        val dqy = quat.y + quat.y
        val dqz = quat.z + quat.z
        val q00 = dqx * quat.x
        val q11 = dqy * quat.y
        val q22 = dqz * quat.z
        val q01 = dqx * quat.y
        val q02 = dqx * quat.z
        val q03 = dqx * quat.w
        val q12 = dqy * quat.z
        val q13 = dqy * quat.w
        val q23 = dqz * quat.w
        m00 = 1f - (q11 + q22)
        m01 = q01 + q23
        m02 = q02 - q13
        m10 = q01 - q23
        m11 = 1f - (q22 + q00)
        m12 = q12 + q03
        m20 = q02 + q13
        m21 = q12 - q03
        m22 = 1f - (q11 + q00)
        m30 = tx
        m31 = ty
        m32 = tz
        flags = 16
        return this
    }

    fun translationRotate(tx: Double, ty: Double, tz: Double, qx: Float, qy: Float, qz: Float, qw: Float): Matrix4x3 {
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
        m00 = w2 + x2 - z2 - y2
        m01 = xy + zw + zw + xy
        m02 = xz - yw + xz - yw
        m10 = -zw + xy - zw + xy
        m11 = y2 - z2 + w2 - x2
        m12 = yz + yz + xw + xw
        m20 = yw + xz + xz + yw
        m21 = yz + yz - xw - xw
        m22 = z2 - y2 - x2 + w2
        m30 = tx
        m31 = ty
        m32 = tz
        flags = 16
        return this
    }

    fun translationRotate(translation: Vector3d, quat: Quaternionf): Matrix4x3 {
        return translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotateMul(tx: Double, ty: Double, tz: Double, quat: Quaternionf, mat: Matrix4x3): Matrix4x3 {
        return translationRotateMul(tx, ty, tz, quat.x, quat.y, quat.z, quat.w, mat)
    }

    fun translationRotateMul(
        tx: Double, ty: Double, tz: Double,
        qx: Float, qy: Float, qz: Float, qw: Float,
        mat: Matrix4x3
    ): Matrix4x3 {
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
        val nm00 = w2 + x2 - z2 - y2
        val nm01 = xy + zw + zw + xy
        val nm02 = xz - yw + xz - yw
        val nm10 = -zw + xy - zw + xy
        val nm11 = y2 - z2 + w2 - x2
        val nm12 = yz + yz + xw + xw
        val nm20 = yw + xz + xz + yw
        val nm21 = yz + yz - xw - xw
        val nm22 = z2 - y2 - x2 + w2
        m00 = nm00 * mat.m00 + nm10 * mat.m01 + nm20 * mat.m02
        m01 = nm01 * mat.m00 + nm11 * mat.m01 + nm21 * mat.m02
        m02 = nm02 * mat.m00 + nm12 * mat.m01 + nm22 * mat.m02
        m10 = nm00 * mat.m10 + nm10 * mat.m11 + nm20 * mat.m12
        m11 = nm01 * mat.m10 + nm11 * mat.m11 + nm21 * mat.m12
        m12 = nm02 * mat.m10 + nm12 * mat.m11 + nm22 * mat.m12
        m20 = nm00 * mat.m20 + nm10 * mat.m21 + nm20 * mat.m22
        m21 = nm01 * mat.m20 + nm11 * mat.m21 + nm21 * mat.m22
        m22 = nm02 * mat.m20 + nm12 * mat.m21 + nm22 * mat.m22
        m30 = nm00 * mat.m30 + nm10 * mat.m31 + nm20 * mat.m32 + tx
        m31 = nm01 * mat.m30 + nm11 * mat.m31 + nm21 * mat.m32 + ty
        m32 = nm02 * mat.m30 + nm12 * mat.m31 + nm22 * mat.m32 + tz
        flags = 0
        return this
    }

    fun translationRotateInvert(
        tx: Double, ty: Double, tz: Double,
        qx: Float, qy: Float, qz: Float, qw: Float
    ): Matrix4x3 {
        val nqx = -qx
        val nqy = -qy
        val nqz = -qz
        val dqx = nqx + nqx
        val dqy = nqy + nqy
        val dqz = nqz + nqz
        val q00 = dqx * nqx
        val q11 = dqy * nqy
        val q22 = dqz * nqz
        val q01 = dqx * nqy
        val q02 = dqx * nqz
        val q03 = dqx * qw
        val q12 = dqy * nqz
        val q13 = dqy * qw
        val q23 = dqz * qw
        return _m00(1f - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m10(q01 - q23)._m11(1f - q22 - q00)._m12(q12 + q03)
            ._m20(q02 + q13)._m21(q12 - q03)._m22(1f - q11 - q00)
            ._m30(-m00 * tx - m10 * ty - m20 * tz)._m31(-m01 * tx - m11 * ty - m21 * tz)
            ._m32(-m02 * tx - m12 * ty - m22 * tz)._properties(16)
    }

    fun translationRotateInvert(translation: Vector3d, quat: Quaternionf): Matrix4x3 {
        return translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun set3x3(mat: Matrix3f): Matrix4x3 {
        m00 = mat.m00
        m01 = mat.m01
        m02 = mat.m02
        m10 = mat.m10
        m11 = mat.m11
        m12 = mat.m12
        m20 = mat.m20
        m21 = mat.m21
        m22 = mat.m22
        flags = 0
        return this
    }

    fun transform(v: Vector4f, dst: Vector4f = v): Vector4f {
        return v.mul(this, dst)
    }

    fun transformPosition(v: Vector3f, dst: Vector3f = v): Vector3f {
        return v.mulPosition(this, dst)
    }

    fun transformPosition(v: Vector3d, dst: Vector3d = v): Vector3d {
        return v.mulPosition(this, dst)
    }

    fun transformDirection(v: Vector3f, dst: Vector3f = v): Vector3f {
        return v.mulDirection(this, dst)
    }

    fun transformDirection(v: Vector3d, dst: Vector3d = v): Vector3d {
        return v.mulDirection(this, dst)
    }

    /**
     * inverts this matrix without saving the result, and then transforming v as a direction
     * */
    fun transformDirectionInverse(v: Vector3d, dst: Vector3d = v): Vector3d {
        val a = m00 * m11 - m01 * m10
        val b = m02 * m10 - m00 * m12
        val c = m01 * m12 - m02 * m11
        val nm00 = (m11 * m22 - m21 * m12)
        val nm01 = (m21 * m02 - m01 * m22)
        val nm02 = c
        val nm10 = (m20 * m12 - m10 * m22)
        val nm11 = (m00 * m22 - m20 * m02)
        val nm12 = b
        val nm20 = (m10 * m21 - m20 * m11)
        val nm21 = (m20 * m01 - m00 * m21)
        val nm22 = a
        val s = 1.0 / (a * m22 + b * m21 + c * m20)
        val rx = v.dot(nm00, nm10, nm20) * s
        val ry = v.dot(nm01, nm11, nm21) * s
        val rz = v.dot(nm02, nm12, nm22) * s
        return dst.set(rx, ry, rz)
    }

    fun scale(xyz: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return scale(xyz.x, xyz.y, xyz.z, dst)
    }

    fun scale(xyz: Vector3f): Matrix4x3 {
        return scale(xyz.x, xyz.y, xyz.z, this)
    }

    fun scale(xyz: Float, dst: Matrix4x3): Matrix4x3 {
        return scale(xyz, xyz, xyz, dst)
    }

    fun scale(xyz: Float): Matrix4x3 {
        return scale(xyz, xyz, xyz)
    }

    fun scaleXY(x: Float, y: Float, dst: Matrix4x3): Matrix4x3 {
        return scale(x, y, 1f, dst)
    }

    fun scaleXY(x: Float, y: Float): Matrix4x3 {
        return scale(x, y, 1f)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, z: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) dst.scaling(x, y, z) else scaleGeneric(x, y, z, dst)
    }

    private fun scaleGeneric(x: Float, y: Float, z: Float, dst: Matrix4x3): Matrix4x3 {
        dst.m00 = m00 * x
        dst.m01 = m01 * x
        dst.m02 = m02 * x
        dst.m10 = m10 * y
        dst.m11 = m11 * y
        dst.m12 = m12 * y
        dst.m20 = m20 * z
        dst.m21 = m21 * z
        dst.m22 = m22 * z
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -29
        return dst
    }

    @JvmOverloads
    fun scaleLocal(scale: Vector3f, dst: Matrix4x3 = this): Matrix4x3 {
        return scaleLocal(scale.x, scale.y, scale.z, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, z: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.scaling(x, y, z)
        } else {
            val nm00 = x * m00
            val nm01 = y * m01
            val nm02 = z * m02
            val nm10 = x * m10
            val nm11 = y * m11
            val nm12 = z * m12
            val nm20 = x * m20
            val nm21 = y * m21
            val nm22 = z * m22
            val nm30 = x * m30
            val nm31 = y * m31
            val nm32 = z * m32
            dst.m00 = nm00
            dst.m01 = nm01
            dst.m02 = nm02
            dst.m10 = nm10
            dst.m11 = nm11
            dst.m12 = nm12
            dst.m20 = nm20
            dst.m21 = nm21
            dst.m22 = nm22
            dst.m30 = nm30
            dst.m31 = nm31
            dst.m32 = nm32
            dst.flags = flags and -29
            dst
        }
    }

    @JvmOverloads
    fun scaleAround(
        sx: Float, sy: Float, sz: Float,
        ox: Double, oy: Double, oz: Double,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val nm30 = m00 * ox + m10 * oy + m20 * oz + m30
        val nm31 = m01 * ox + m11 * oy + m21 * oz + m31
        val nm32 = m02 * ox + m12 * oy + m22 * oz + m32
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return dst._m00(m00 * sx)._m01(m01 * sx)._m02(m02 * sx)._m10(m10 * sy)._m11(m11 * sy)._m12(m12 * sy)
            ._m20(m20 * sz)._m21(
                m21 * sz
            )._m22(m22 * sz)._m30(
                -dst.m00 * ox - dst.m10 * oy - dst.m20 * oz + nm30
            )._m31(-dst.m01 * ox - dst.m11 * oy - dst.m21 * oz + nm31)._m32(
                -dst.m02 * ox - dst.m12 * oy - dst.m22 * oz + nm32
            )._properties(flags and (12 or if (one) 0 else 16).inv())
    }

    fun scaleAround(factor: Float, ox: Double, oy: Double, oz: Double): Matrix4x3 {
        return scaleAround(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAround(factor: Float, ox: Double, oy: Double, oz: Double, dst: Matrix4x3): Matrix4x3 {
        return scaleAround(factor, factor, factor, ox, oy, oz, dst)
    }

    @JvmOverloads
    fun rotateX(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationX(ang)
        } else if (flags and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dst.rotationX(ang).setTranslation(x, y, z)
        } else {
            rotateXInternal(ang, dst)
        }
    }

    private fun rotateXInternal(ang: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateY(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationY(ang)
        } else if (flags and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dst.rotationY(ang).setTranslation(x, y, z)
        } else {
            rotateYInternal(ang, dst)
        }
    }

    private fun rotateYInternal(ang: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateZ(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationZ(ang)
        } else if (flags and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dst.rotationZ(ang).setTranslation(x, y, z)
        } else {
            rotateZInternal(ang, dst)
        }
    }

    private fun rotateZInternal(ang: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    fun rotateXYZ(angles: Vector3f): Matrix4x3 {
        return rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationXYZ(angleX, angleY, angleZ)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz)
        } else {
            rotateXYZInternal(angleX, angleY, angleZ, dst)
        }
    }

    private fun rotateXYZInternal(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    fun rotateZYX(angles: Vector3f): Matrix4x3 {
        return rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationZYX(angleZ, angleY, angleX)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz)
        } else {
            rotateZYXInternal(angleZ, angleY, angleX, dst)
        }
    }

    private fun rotateZYXInternal(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    fun rotateYXZ(angles: Vector3f): Matrix4x3 {
        return rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationYXZ(angleY, angleX, angleZ)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz)
        } else {
            rotateYXZInternal(angleY, angleX, angleZ, dst)
        }
    }

    private fun rotateYXZInternal(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotate(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotation(ang, x, y, z)
        } else {
            if (flags and 8 != 0) rotateTranslation(ang, x, y, z, dst)
            else rotateGeneric(ang, x, y, z, dst)
        }
    }

    private fun rotateGeneric(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3): Matrix4x3 {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dst)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dst)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotateZ(z * ang, dst)
            else rotateGenericInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateGenericInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateTranslation(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val tx = m30
        val ty = m31
        val tz = m32
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            dst.rotationX(x * ang).setTranslation(tx, ty, tz)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            dst.rotationY(y * ang).setTranslation(tx, ty, tz)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) dst.rotationZ(z * ang)
                .setTranslation(tx, ty, tz) else rotateTranslationInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateTranslationInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m20 = rm20
        dst.m21 = rm21
        dst.m22 = rm22
        dst.m00 = rm00
        dst.m01 = rm01
        dst.m02 = rm02
        dst.m10 = rm10
        dst.m11 = rm11
        dst.m12 = rm12
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    private fun rotateAroundAffine(quat: Quaternionf, ox: Double, oy: Double, oz: Double, dst: Matrix4x3): Matrix4x3 {
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
        val tm30 = m00 * ox + m10 * oy + m20 * oz + m30
        val tm31 = m01 * ox + m11 * oy + m21 * oz + m31
        val tm32 = m02 * ox + m12 * oy + m22 * oz + m32
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)
            ._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)
            ._m30(-nm00 * ox - nm10 * oy - m20 * oz + tm30)
            ._m31(-nm01 * ox - nm11 * oy - m21 * oz + tm31)
            ._m32(-nm02 * ox - nm12 * oy - m22 * oz + tm32)
            ._properties(flags and -13)
        return dst
    }

    @JvmOverloads
    fun rotateAround(quat: Quaternionf, ox: Double, oy: Double, oz: Double, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) rotationAround(quat, ox, oy, oz)
        else rotateAroundAffine(quat, ox, oy, oz, dst)
    }

    fun rotationAround(quat: Quaternionf, ox: Double, oy: Double, oz: Double): Matrix4x3 {
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
        _m20(dyw + dxz)
        _m21(dyz - dxw)
        _m22(z2 - y2 - x2 + w2)
        _m00(w2 + x2 - z2 - y2)
        _m01(dxy + dzw)
        _m02(dxz - dyw)
        _m10(dxy - dzw)
        _m11(y2 - z2 + w2 - x2)
        _m12(dyz + dxw)
        _m30(-m00 * ox - m10 * oy - m20 * oz + ox)
        _m31(-m01 * ox - m11 * oy - m21 * oz + oy)
        _m32(-m02 * ox - m12 * oy - m22 * oz + oz)
        flags = 16
        return this
    }

    @JvmOverloads
    fun rotateLocal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotateLocalX(x * ang, dst)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotateLocalY(y * ang, dst)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotateLocalZ(z * ang, dst)
            else rotateLocalInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateLocalInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4x3): Matrix4x3 {
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
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateLocalX(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm01 = cos * m01 - sin * m02
        val nm02 = sin * m01 + cos * m02
        val nm11 = cos * m11 - sin * m12
        val nm12 = sin * m11 + cos * m12
        val nm21 = cos * m21 - sin * m22
        val nm22 = sin * m21 + cos * m22
        val nm31 = cos * m31 - sin * m32
        val nm32 = sin * m31 + cos * m32
        dst._m00(m00)._m01(nm01)._m02(nm02)
            ._m10(m10)._m11(nm11)._m12(nm12)
            ._m20(m20)._m21(nm21)._m22(nm22)
            ._m30(m30)._m31(nm31)._m32(nm32)
            ._properties(flags and -13)
        return dst
    }

    @JvmOverloads
    fun rotateLocalY(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 + sin * m02
        val nm02 = -sin * m00 + cos * m02
        val nm10 = cos * m10 + sin * m12
        val nm12 = -sin * m10 + cos * m12
        val nm20 = cos * m20 + sin * m22
        val nm22 = -sin * m20 + cos * m22
        val nm30 = cos * m30 + sin * m32
        val nm32 = -sin * m30 + cos * m32
        dst._m00(nm00)._m01(m01)._m02(nm02)
            ._m10(nm10)._m11(m11)._m12(nm12)
            ._m20(nm20)._m21(m21)._m22(nm22)
            ._m30(nm30)._m31(m31)._m32(nm32)
            ._properties(flags and -13)
        return dst
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        val nm30 = cos * m30 - sin * m31
        val nm31 = sin * m30 + cos * m31
        dst._m00(nm00)._m01(nm01)._m02(m02)._m10(nm10)._m11(nm11)._m12(m12)
            ._m20(nm20)._m21(nm21)._m22(m22)._m30(nm30)._m31(nm31)._m32(m32)
            ._properties(flags and -13)
        return dst
    }

    fun translate(offset: Vector3d): Matrix4x3 {
        return translate(offset.x, offset.y, offset.z)
    }

    fun translate(offset: Vector3d, dst: Matrix4x3): Matrix4x3 {
        return translate(offset.x, offset.y, offset.z, dst)
    }

    fun translate(x: Double, y: Double, z: Double, dst: Matrix4x3): Matrix4x3 {
        return if (flags and 4 != 0) dst.translation(x, y, z) else translateGeneric(x, y, z, dst)
    }

    private fun translateGeneric(x: Double, y: Double, z: Double, dst: Matrix4x3): Matrix4x3 {
        dst.set(
            m00, m01, m02, m10, m11, m12, m20, m21, m22,
            m00 * x + m10 * y + m20 * z + m30,
            m01 * x + m11 * y + m21 * z + m31,
            m02 * x + m12 * y + m22 * z + m32
        )
        dst.flags = flags and -5
        return dst
    }

    fun translate(x: Double, y: Double, z: Double): Matrix4x3 {
        return if (flags and 4 != 0) {
            translation(x, y, z)
        } else {
            m30 += m00 * x + m10 * y + m20 * z
            m31 += m01 * x + m11 * y + m21 * z
            m32 += m02 * x + m12 * y + m22 * z
            flags = flags and -5
            this
        }
    }

    fun translateLocal(offset: Vector3d): Matrix4x3 {
        return translateLocal(offset.x, offset.y, offset.z)
    }

    fun translateLocal(offset: Vector3d, dst: Matrix4x3): Matrix4x3 {
        return translateLocal(offset.x, offset.y, offset.z, dst)
    }

    @JvmOverloads
    fun translateLocal(x: Double, y: Double, z: Double, dst: Matrix4x3 = this): Matrix4x3 {
        dst.m00 = m00
        dst.m01 = m01
        dst.m02 = m02
        dst.m10 = m10
        dst.m11 = m11
        dst.m12 = m12
        dst.m20 = m20
        dst.m21 = m21
        dst.m22 = m22
        dst.m30 = m30 + x
        dst.m31 = m31 + y
        dst.m32 = m32 + z
        dst.flags = flags and -5
        return dst
    }

    @JvmOverloads
    fun ortho(
        left: Float, right: Float,
        bottom: Float, top: Float,
        zNear: Float, zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = m20 * rm22
        dst.m21 = m21 * rm22
        dst.m22 = m22 * rm22
        dst.flags = flags and -29
        return dst
    }

    fun ortho(
        left: Float, right: Float,
        bottom: Float, top: Float,
        zNear: Float, zFar: Float,
        dst: Matrix4x3
    ): Matrix4x3 {
        return ortho(left, right, bottom, top, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun orthoLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = m20 * rm22
        dst.m21 = m21 * rm22
        dst.m22 = m22 * rm22
        dst.flags = flags and -29
        return dst
    }

    fun orthoLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4x3
    ): Matrix4x3 {
        return orthoLH(left, right, bottom, top, zNear, zFar, false, dst)
    }

    fun setOrtho(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean
    ): Matrix4x3 {
        identity()
        m00 = 2f / (right - left)
        m11 = 2f / (top - bottom)
        m22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        m30 = (right + left).toDouble() / (left - right)
        m31 = (top + bottom).toDouble() / (bottom - top)
        m32 = (if (zZeroToOne) zNear else zFar + zNear).toDouble() / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrtho(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4x3 {
        return setOrtho(left, right, bottom, top, zNear, zFar, false)
    }

    fun setOrthoLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean
    ): Matrix4x3 {
        identity()
        m00 = 2f / (right - left)
        m11 = 2f / (top - bottom)
        m22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        m30 = (right + left).toDouble() / (left - right)
        m31 = (top + bottom).toDouble() / (bottom - top)
        m32 = (if (zZeroToOne) zNear.toDouble() else zFar + zNear).toDouble() / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoLH(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4x3 {
        return setOrthoLH(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun orthoSymmetric(
        width: Float, height: Float,
        zNear: Float, zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val rm00 = 2f / width
        val rm11 = 2f / height
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst.m30 = m20 * rm32 + m30
        dst.m31 = m21 * rm32 + m31
        dst.m32 = m22 * rm32 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = m20 * rm22
        dst.m21 = m21 * rm22
        dst.m22 = m22 * rm22
        dst.flags = flags and -29
        return dst
    }

    fun orthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float, dst: Matrix4x3): Matrix4x3 {
        return orthoSymmetric(width, height, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun orthoSymmetricLH(
        width: Float, height: Float,
        zNear: Float, zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val rm00 = 2f / width
        val rm11 = 2f / height
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst.m30 = m20 * rm32 + m30
        dst.m31 = m21 * rm32 + m31
        dst.m32 = m22 * rm32 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = m20 * rm22
        dst.m21 = m21 * rm22
        dst.m22 = m22 * rm22
        dst.flags = flags and -29
        return dst
    }

    fun orthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float, dst: Matrix4x3): Matrix4x3 {
        return orthoSymmetricLH(width, height, zNear, zFar, false, dst)
    }

    fun setOrthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4x3 {
        identity()
        m00 = 2f / width
        m11 = 2f / height
        m22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        m32 = (if (zZeroToOne) zNear else zFar + zNear).toDouble() / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float): Matrix4x3 {
        return setOrthoSymmetric(width, height, zNear, zFar, false)
    }

    fun setOrthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4x3 {
        identity()
        m00 = 2f / width
        m11 = 2f / height
        m22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        m32 = (if (zZeroToOne) zNear else zFar + zNear).toDouble() / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float): Matrix4x3 {
        return setOrthoSymmetricLH(width, height, zNear, zFar, false)
    }

    @JvmOverloads
    fun ortho2D(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm30 = -(right + left) / (right - left)
        val rm31 = -(top + bottom) / (top - bottom)
        dst.m30 = m00 * rm30 + m10 * rm31 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = -m20
        dst.m21 = -m21
        dst.m22 = -m22
        dst.flags = flags and -29
        return dst
    }

    @JvmOverloads
    fun ortho2DLH(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm30 = -(right + left) / (right - left)
        val rm31 = -(top + bottom) / (top - bottom)
        dst.m30 = m00 * rm30 + m10 * rm31 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m32
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m02 = m02 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        dst.m12 = m12 * rm11
        dst.m20 = m20
        dst.m21 = m21
        dst.m22 = m22
        dst.flags = flags and -29
        return dst
    }

    fun setOrtho2D(left: Float, right: Float, bottom: Float, top: Float): Matrix4x3 {
        identity()
        m00 = 2f / (right - left)
        m11 = 2f / (top - bottom)
        m22 = -1f
        m30 = -(right + left).toDouble() / (right - left)
        m31 = -(top + bottom).toDouble() / (top - bottom)
        flags = 0
        return this
    }

    fun setOrtho2DLH(left: Float, right: Float, bottom: Float, top: Float): Matrix4x3 {
        identity()
        m00 = 2f / (right - left)
        m11 = 2f / (top - bottom)
        m22 = 1f
        m30 = -(right + left).toDouble() / (right - left)
        m31 = -(top + bottom).toDouble() / (top - bottom)
        flags = 0
        return this
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Matrix4x3 {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        var dirX0 = dirX
        var dirY0 = dirY
        var dirZ0 = dirZ
        return if (flags and 4 != 0) {
            this.setLookAlong(dirX0, dirY0, dirZ0, upX, upY, upZ)
        } else {
            val invDirLength = JomlMath.invsqrt(dirX0 * dirX0 + dirY0 * dirY0 + dirZ0 * dirZ0)
            dirX0 *= -invDirLength
            dirY0 *= -invDirLength
            dirZ0 *= -invDirLength
            var leftX = upY * dirZ0 - upZ * dirY0
            var leftY = upZ * dirX0 - upX * dirZ0
            var leftZ = upX * dirY0 - upY * dirX0
            val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
            leftX *= invLeftLength
            leftY *= invLeftLength
            leftZ *= invLeftLength
            val upnX = dirY0 * leftZ - dirZ0 * leftY
            val upnY = dirZ0 * leftX - dirX0 * leftZ
            val upnZ = dirX0 * leftY - dirY0 * leftX
            val nm00 = m00 * leftX + m10 * upnX + m20 * dirX0
            val nm01 = m01 * leftX + m11 * upnX + m21 * dirX0
            val nm02 = m02 * leftX + m12 * upnX + m22 * dirX0
            val nm10 = m00 * leftY + m10 * upnY + m20 * dirY0
            val nm11 = m01 * leftY + m11 * upnY + m21 * dirY0
            val nm12 = m02 * leftY + m12 * upnY + m22 * dirY0
            dst.m20 = m00 * leftZ + m10 * upnZ + m20 * dirZ0
            dst.m21 = m01 * leftZ + m11 * upnZ + m21 * dirZ0
            dst.m22 = m02 * leftZ + m12 * upnZ + m22 * dirZ0
            dst.m00 = nm00
            dst.m01 = nm01
            dst.m02 = nm02
            dst.m10 = nm10
            dst.m11 = nm11
            dst.m12 = nm12
            dst.m30 = m30
            dst.m31 = m31
            dst.m32 = m32
            dst.flags = flags and -13
            dst
        }
    }

    fun setLookAlong(dir: Vector3f, up: Vector3f): Matrix4x3 {
        return setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun setLookAt(eye: Vector3d, center: Vector3d, up: Vector3f): Matrix4x3 {
        return setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAt(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4x3 {
        var dirX = eyeX - centerX
        var dirY = eyeY - centerY
        var dirZ = eyeZ - centerZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength
        dirY *= invDirLength
        dirZ *= invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        m00 = leftX.toFloat()
        m01 = upnX.toFloat()
        m02 = dirX.toFloat()
        m10 = leftY.toFloat()
        m11 = upnY.toFloat()
        m12 = dirY.toFloat()
        m20 = leftZ.toFloat()
        m21 = upnZ.toFloat()
        m22 = dirZ.toFloat()
        m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        flags = 16
        return this
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3f): Matrix4x3 {
        return lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAt(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        return if (flags and 4 != 0) dst.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        else lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
    }

    private fun lookAtGeneric(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float, dst: Matrix4x3
    ): Matrix4x3 {
        var dirX = eyeX - centerX
        var dirY = eyeY - centerY
        var dirZ = eyeZ - centerZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength
        dirY *= invDirLength
        dirZ *= invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        val rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        val rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        val rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        val nm20 = m00 * leftZ + m10 * upnZ + m20 * dirZ
        val nm21 = m01 * leftZ + m11 * upnZ + m21 * dirZ
        val nm22 = m02 * leftZ + m12 * upnZ + m22 * dirZ
        dst.m00 = nm00.toFloat()
        dst.m01 = nm01.toFloat()
        dst.m02 = nm02.toFloat()
        dst.m10 = nm10.toFloat()
        dst.m11 = nm11.toFloat()
        dst.m12 = nm12.toFloat()
        dst.m20 = nm20.toFloat()
        dst.m21 = nm21.toFloat()
        dst.m22 = nm22.toFloat()
        dst.flags = flags and -13
        return dst
    }

    fun setLookAtLH(eye: Vector3d, center: Vector3d, up: Vector3f): Matrix4x3 {
        return setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAtLH(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4x3 {
        var dirX = centerX - eyeX
        var dirY = centerY - eyeY
        var dirZ = centerZ - eyeZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength
        dirY *= invDirLength
        dirZ *= invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        m00 = leftX.toFloat()
        m01 = upnX.toFloat()
        m02 = dirX.toFloat()
        m10 = leftY.toFloat()
        m11 = upnY.toFloat()
        m12 = dirY.toFloat()
        m20 = leftZ.toFloat()
        m21 = upnZ.toFloat()
        m22 = dirZ.toFloat()
        m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        flags = 16
        return this
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3f): Matrix4x3 {
        return lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAtLH(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        return if (flags and 4 != 0) dst.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        else lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
    }

    private fun lookAtLHGeneric(
        eyeX: Double, eyeY: Double, eyeZ: Double,
        centerX: Double, centerY: Double, centerZ: Double,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4x3
    ): Matrix4x3 {
        var dirX = centerX - eyeX
        var dirY = centerY - eyeY
        var dirZ = centerZ - eyeZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength
        dirY *= invDirLength
        dirZ *= invDirLength
        var leftX = upY * dirZ - upZ * dirY
        var leftY = upZ * dirX - upX * dirZ
        var leftZ = upX * dirY - upY * dirX
        val invLeftLength = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        val rm30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        val rm31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        val rm32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
        dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
        dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        val nm20 = m00 * leftZ + m10 * upnZ + m20 * dirZ
        val nm21 = m01 * leftZ + m11 * upnZ + m21 * dirZ
        val nm22 = m02 * leftZ + m12 * upnZ + m22 * dirZ
        dst.m00 = nm00.toFloat()
        dst.m01 = nm01.toFloat()
        dst.m02 = nm02.toFloat()
        dst.m10 = nm10.toFloat()
        dst.m11 = nm11.toFloat()
        dst.m12 = nm12.toFloat()
        dst.m20 = nm20.toFloat()
        dst.m21 = nm21.toFloat()
        dst.m22 = nm22.toFloat()
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dst: Matrix4x3 = this): Matrix4x3 {
        return rotateQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    fun rotateQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.rotationQ(qx, qy, qz, qw)
        } else {
            if (flags and 8 != 0) rotateTranslationQ(qx, qy, qz, qw, dst)
            else rotateGenericQ(qx, qy, qz, qw, dst)
        }
    }

    private fun rotateGenericQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4x3): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateTranslation(quat: Quaternionf, dst: Matrix4x3 = this): Matrix4x3 {
        return rotateTranslationQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    @JvmOverloads
    fun rotateTranslationQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4x3 = this): Matrix4x3 {
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
        dst.m20 = rm20
        dst.m21 = rm21
        dst.m22 = rm22
        dst.m00 = rm00
        dst.m01 = rm01
        dst.m02 = rm02
        dst.m10 = rm10
        dst.m11 = rm11
        dst.m12 = rm12
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaternionf, dst: Matrix4x3 = this): Matrix4x3 {
        val w2 = quat.w * quat.w
        val x2 = quat.x * quat.x
        val y2 = quat.y * quat.y
        val z2 = quat.z * quat.z
        val zw = quat.z * quat.w
        val xy = quat.x * quat.y
        val xz = quat.x * quat.z
        val yw = quat.y * quat.w
        val yz = quat.y * quat.z
        val xw = quat.x * quat.w
        val lm00 = w2 + x2 - z2 - y2
        val lm01 = xy + zw + zw + xy
        val lm02 = xz - yw + xz - yw
        val lm10 = -zw + xy - zw + xy
        val lm11 = y2 - z2 + w2 - x2
        val lm12 = yz + yz + xw + xw
        val lm20 = yw + xz + xz + yw
        val lm21 = yz + yz - xw - xw
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
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = flags and -13
        return dst
    }

    fun rotate(axisAngle: AxisAngle4f): Matrix4x3 {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4f, dst: Matrix4x3): Matrix4x3 {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dst)
    }

    fun rotate(angle: Float, axis: Vector3f): Matrix4x3 {
        return rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Float, axis: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return rotate(angle, axis.x, axis.y, axis.z, dst)
    }

    @JvmOverloads
    fun reflect(a: Float, b: Float, c: Float, d: Double, dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.reflection(a, b, c, d)
        } else {
            val da = a + a
            val db = b + b
            val dc = c + c
            val dd = d + d
            val rm00 = 1f - da * a
            val rm01 = -da * b
            val rm02 = -da * c
            val rm10 = -db * a
            val rm11 = 1f - db * b
            val rm12 = -db * c
            val rm20 = -dc * a
            val rm21 = -dc * b
            val rm22 = 1f - dc * c
            val rm30 = -dd * a
            val rm31 = -dd * b
            val rm32 = -dd * c
            dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30
            dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31
            dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
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
            dst.flags = flags and -13
            dst
        }
    }

    @JvmOverloads
    fun reflect(nx: Float, ny: Float, nz: Float, px: Double, py: Double, pz: Double, dst: Matrix4x3 = this): Matrix4x3 {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dst)
    }

    fun reflect(normal: Vector3f, point: Vector3d): Matrix4x3 {
        return reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    @JvmOverloads
    fun reflect(orientation: Quaternionf, point: Vector3d, dst: Matrix4x3 = this): Matrix4x3 {
        val num1 = (orientation.x + orientation.x).toDouble()
        val num2 = (orientation.y + orientation.y).toDouble()
        val num3 = (orientation.z + orientation.z).toDouble()
        val normalX = (orientation.x.toDouble() * num3 + orientation.w.toDouble() * num2).toFloat()
        val normalY = (orientation.y.toDouble() * num3 - orientation.w.toDouble() * num1).toFloat()
        val normalZ = (1.0 - (orientation.x.toDouble() * num1 + orientation.y.toDouble() * num2)).toFloat()
        return reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dst)
    }

    fun reflect(normal: Vector3f, point: Vector3d, dst: Matrix4x3): Matrix4x3 {
        return reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dst)
    }

    fun reflection(a: Float, b: Float, c: Float, d: Double): Matrix4x3 {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        m00 = 1f - da * a
        m01 = -da * b
        m02 = -da * c
        m10 = -db * a
        m11 = 1f - db * b
        m12 = -db * c
        m20 = -dc * a
        m21 = -dc * b
        m22 = 1f - dc * c
        m30 = -dd * a
        m31 = -dd * b
        m32 = -dd * c
        flags = 16
        return this
    }

    fun reflection(nx: Float, ny: Float, nz: Float, px: Double, py: Double, pz: Double): Matrix4x3 {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz)
    }

    fun reflection(normal: Vector3f, point: Vector3d): Matrix4x3 {
        return reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    fun reflection(orientation: Quaternionf, point: Vector3d): Matrix4x3 {
        val num1 = (orientation.x + orientation.x).toDouble()
        val num2 = (orientation.y + orientation.y).toDouble()
        val num3 = (orientation.z + orientation.z).toDouble()
        val normalX = (orientation.x.toDouble() * num3 + orientation.w.toDouble() * num2).toFloat()
        val normalY = (orientation.y.toDouble() * num3 - orientation.w.toDouble() * num1).toFloat()
        val normalZ = (1.0 - (orientation.x.toDouble() * num1 + orientation.y.toDouble() * num2)).toFloat()
        return reflection(normalX, normalY, normalZ, point.x, point.y, point.z)
    }

    override fun getRow(row: Int, dst: Vector4d): Vector4d {
        when (row) {
            0 -> {
                dst.x = m00.toDouble()
                dst.y = m10.toDouble()
                dst.z = m20.toDouble()
                dst.w = m30
            }
            1 -> {
                dst.x = m01.toDouble()
                dst.y = m11.toDouble()
                dst.z = m21.toDouble()
                dst.w = m31
            }
            else -> {
                dst.x = m02.toDouble()
                dst.y = m12.toDouble()
                dst.z = m22.toDouble()
                dst.w = m32
            }
        }
        return dst
    }

    override fun setRow(row: Int, src: Vector4d): Matrix4x3 {
        when (row) {
            0 -> {
                m00 = src.x.toFloat()
                m10 = src.y.toFloat()
                m20 = src.z.toFloat()
                m30 = src.w
            }
            1 -> {
                m01 = src.x.toFloat()
                m11 = src.y.toFloat()
                m21 = src.z.toFloat()
                m31 = src.w
            }
            else -> {
                m02 = src.x.toFloat()
                m12 = src.y.toFloat()
                m22 = src.z.toFloat()
                m32 = src.w
            }
        }
        flags = 0
        return this
    }

    override fun getColumn(column: Int, dst: Vector3d): Vector3d {
        when (column) {
            0 -> {
                dst.x = m00.toDouble()
                dst.y = m01.toDouble()
                dst.z = m02.toDouble()
            }
            1 -> {
                dst.x = m10.toDouble()
                dst.y = m11.toDouble()
                dst.z = m12.toDouble()
            }
            2 -> {
                dst.x = m20.toDouble()
                dst.y = m21.toDouble()
                dst.z = m22.toDouble()
            }
            else -> {
                dst.x = m30
                dst.y = m31
                dst.z = m32
            }
        }
        return dst
    }

    override fun setColumn(column: Int, src: Vector3d): Matrix4x3 {
        when (column) {
            0 -> {
                m00 = src.x.toFloat()
                m01 = src.y.toFloat()
                m02 = src.z.toFloat()
            }
            1 -> {
                m10 = src.x.toFloat()
                m11 = src.y.toFloat()
                m12 = src.z.toFloat()
            }
            2 -> {
                m20 = src.x.toFloat()
                m21 = src.y.toFloat()
                m22 = src.z.toFloat()
            }
            else -> {
                m30 = src.x
                m31 = src.y
                m32 = src.z
            }
        }
        flags = 0
        return this
    }

    @JvmOverloads
    fun normal(dst: Matrix4x3 = this): Matrix4x3 {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
        }
    }

    private fun normalOrthonormal(dst: Matrix4x3): Matrix4x3 {
        if (dst !== this) {
            dst.set(this)
        }
        return dst._properties(16)
    }

    private fun normalGeneric(dst: Matrix4x3): Matrix4x3 {
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
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = 0.0
        dst.m31 = 0.0
        dst.m32 = 0.0
        dst.flags = flags and -9
        return dst
    }

    fun normal(dst: Matrix3f): Matrix3f {
        return if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
    }

    private fun normalOrthonormal(dst: Matrix3f): Matrix3f {
        return dst.set(this)
    }

    private fun normalGeneric(dst: Matrix3f): Matrix3f {
        val m00m11 = m00 * m11
        val m01m10 = m01 * m10
        val m02m10 = m02 * m10
        val m00m12 = m00 * m12
        val m01m12 = m01 * m12
        val m02m11 = m02 * m11
        val det = (m00m11 - m01m10) * m22 + (m02m10 - m00m12) * m21 + (m01m12 - m02m11) * m20
        val s = 1f / det
        val nm00 = ((m11 * m22 - m21 * m12) * s)
        val nm01 = ((m20 * m12 - m10 * m22) * s)
        val nm02 = ((m10 * m21 - m20 * m11) * s)
        val nm10 = ((m21 * m02 - m01 * m22) * s)
        val nm11 = ((m00 * m22 - m20 * m02) * s)
        val nm12 = ((m20 * m01 - m00 * m21) * s)
        val nm20 = ((m01m12 - m02m11) * s)
        val nm21 = ((m02m10 - m00m12) * s)
        val nm22 = ((m00m11 - m01m10) * s)
        return dst.set(
            nm00, nm01, nm02,
            nm10, nm11, nm12,
            nm20, nm21, nm22
        )
    }

    fun cofactor3x3(dst: Matrix3f): Matrix3f {
        dst.m00 = m11 * m22 - m21 * m12
        dst.m01 = m20 * m12 - m10 * m22
        dst.m02 = m10 * m21 - m20 * m11
        dst.m10 = m21 * m02 - m01 * m22
        dst.m11 = m00 * m22 - m20 * m02
        dst.m12 = m20 * m01 - m00 * m21
        dst.m20 = m01 * m12 - m02 * m11
        dst.m21 = m02 * m10 - m00 * m12
        dst.m22 = m00 * m11 - m01 * m10
        return dst
    }

    @JvmOverloads
    fun cofactor3x3(dst: Matrix4x3 = this): Matrix4x3 {
        val nm00 = m11 * m22 - m21 * m12
        val nm01 = m20 * m12 - m10 * m22
        val nm02 = m10 * m21 - m20 * m11
        val nm10 = m21 * m02 - m01 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm12 = m20 * m01 - m00 * m21
        val nm20 = m01 * m12 - m11 * m02
        val nm21 = m02 * m10 - m12 * m00
        val nm22 = m00 * m11 - m10 * m01
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = 0.0
        dst.m31 = 0.0
        dst.m32 = 0.0
        dst.flags = flags and -9
        return dst
    }

    @JvmOverloads
    fun normalize3x3(dst: Matrix4x3 = this): Matrix4x3 {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst.set(
            m00 * invXlen, m01 * invXlen, m02 * invXlen,
            m10 * invYlen, m11 * invYlen, m12 * invYlen,
            m20 * invZlen, m21 * invZlen, m22 * invZlen,
            dst.m30, dst.m31, dst.m32
        )
    }

    fun normalize3x3(dst: Matrix3f): Matrix3f {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst.set(
            m00 * invXlen, m01 * invXlen, m02 * invXlen,
            m10 * invYlen, m11 * invYlen, m12 * invYlen,
            m20 * invZlen, m21 * invZlen, m22 * invZlen,
        )
    }

    fun frustumPlane(which: Int, dst: Vector4d): Vector4d {
        when (which) {
            0 -> dst.set(m00.toDouble(), m10.toDouble(), m20.toDouble(), 1f + m30).normalize()
            1 -> dst.set(-m00.toDouble(), -m10.toDouble(), -m20.toDouble(), 1f - m30).normalize()
            2 -> dst.set(m01.toDouble(), m11.toDouble(), m21.toDouble(), 1f + m31).normalize()
            3 -> dst.set(-m01.toDouble(), -m11.toDouble(), -m21.toDouble(), 1f - m31).normalize()
            4 -> dst.set(m02.toDouble(), m12.toDouble(), m22.toDouble(), 1f + m32).normalize()
            else -> dst.set(-m02.toDouble(), -m12.toDouble(), -m22.toDouble(), 1f - m32).normalize()
        }
        return dst
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

    fun origin(origin: Vector3d): Vector3d {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val d = m01 * m12 - m02 * m11
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val j = m21 * m32 - m22 * m31
        origin.x = -m10 * j + m11 * h - m12 * g
        origin.y = m00 * j - m01 * h + m02 * g
        origin.z = -m30 * d + m31 * b - m32 * a
        return origin
    }

    fun shadow(light: Vector4f, a: Float, b: Float, c: Float, d: Double): Matrix4x3 {
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, this)
    }

    fun shadow(light: Vector4f, a: Float, b: Float, c: Float, d: Double, dst: Matrix4x3): Matrix4x3 {
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
    }

    @JvmOverloads
    fun shadow(
        lightX: Float, lightY: Float, lightZ: Float, lightW: Float,
        a: Float, b: Float, c: Float, d: Double,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val invPlaneLen = JomlMath.invsqrt(a * a + b * b + c * c)
        val an = a * invPlaneLen
        val bn = b * invPlaneLen
        val cn = c * invPlaneLen
        val dn = d * invPlaneLen
        val dot = an * lightX + bn * lightY + cn * lightZ + dn * lightW
        val rm00 = dot - an * lightX
        val rm01 = -an * lightY
        val rm02 = -an * lightZ
        val rm03 = -an * lightW
        val rm10 = -bn * lightX
        val rm11 = dot - bn * lightY
        val rm12 = -bn * lightZ
        val rm13 = -bn * lightW
        val rm20 = -cn * lightX
        val rm21 = -cn * lightY
        val rm22 = dot - cn * lightZ
        val rm23 = -cn * lightW
        val rm30 = -dn * lightX
        val rm31 = -dn * lightY
        val rm32 = -dn * lightZ
        val rm33 = dot - dn * lightW
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02 + m30 * rm03
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02 + m31 * rm03
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02 + m32 * rm03
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12 + m30 * rm13
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12 + m31 * rm13
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12 + m32 * rm13
        val nm20 = m00 * rm20 + m10 * rm21 + m20 * rm22 + m30 * rm23
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 + m31 * rm23
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 + m32 * rm23
        dst.m30 = m00 * rm30 + m10 * rm31 + m20 * rm32 + m30 * rm33
        dst.m31 = m01 * rm30 + m11 * rm31 + m21 * rm32 + m31 * rm33
        dst.m32 = m02 * rm30 + m12 * rm31 + m22 * rm32 + m32 * rm33
        dst.m00 = nm00.toFloat()
        dst.m01 = nm01.toFloat()
        dst.m02 = nm02.toFloat()
        dst.m10 = nm10.toFloat()
        dst.m11 = nm11.toFloat()
        dst.m12 = nm12.toFloat()
        dst.m20 = nm20.toFloat()
        dst.m21 = nm21.toFloat()
        dst.m22 = nm22.toFloat()
        dst.flags = flags and -29
        return dst
    }

    @JvmOverloads
    fun shadow(light: Vector4f, planeTransform: Matrix4x3, dst: Matrix4x3 = this): Matrix4x3 {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
    }

    @JvmOverloads
    fun shadow(
        lightX: Float, lightY: Float, lightZ: Float, lightW: Float,
        planeTransform: Matrix4x3, dst: Matrix4x3 = this
    ): Matrix4x3 {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dst)
    }

    fun billboardCylindrical(objPos: Vector3d, targetPos: Vector3d, up: Vector3f): Matrix4x3 {
        var dirX = targetPos.x - objPos.x
        var dirY = targetPos.y - objPos.y
        var dirZ = targetPos.z - objPos.z
        var leftX = up.y * dirZ - up.z * dirY
        var leftY = up.z * dirX - up.x * dirZ
        var leftZ = up.x * dirY - up.y * dirX
        val invLeftLen = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLen
        leftY *= invLeftLen
        leftZ *= invLeftLen
        dirX = leftY * up.z - leftZ * up.y
        dirY = leftZ * up.x - leftX * up.z
        dirZ = leftX * up.y - leftY * up.x
        val invDirLen = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLen
        dirY *= invDirLen
        dirZ *= invDirLen
        m00 = leftX.toFloat()
        m01 = leftY.toFloat()
        m02 = leftZ.toFloat()
        m10 = up.x
        m11 = up.y
        m12 = up.z
        m20 = dirX.toFloat()
        m21 = dirY.toFloat()
        m22 = dirZ.toFloat()
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d, up: Vector3f): Matrix4x3 {
        var dirX = targetPos.x - objPos.x
        var dirY = targetPos.y - objPos.y
        var dirZ = targetPos.z - objPos.z
        val invDirLen = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLen
        dirY *= invDirLen
        dirZ *= invDirLen
        var leftX = up.y * dirZ - up.z * dirY
        var leftY = up.z * dirX - up.x * dirZ
        var leftZ = up.x * dirY - up.y * dirX
        val invLeftLen = JomlMath.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLen
        leftY *= invLeftLen
        leftZ *= invLeftLen
        val upX = dirY * leftZ - dirZ * leftY
        val upY = dirZ * leftX - dirX * leftZ
        val upZ = dirX * leftY - dirY * leftX
        m00 = leftX.toFloat()
        m01 = leftY.toFloat()
        m02 = leftZ.toFloat()
        m10 = upX.toFloat()
        m11 = upY.toFloat()
        m12 = upZ.toFloat()
        m20 = dirX.toFloat()
        m21 = dirY.toFloat()
        m22 = dirZ.toFloat()
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d): Matrix4x3 {
        val toDirX = targetPos.x - objPos.x
        val toDirY = targetPos.y - objPos.y
        val toDirZ = targetPos.z - objPos.z
        var x = -toDirY
        var w = sqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ) + toDirZ
        val invNorm = JomlMath.invsqrt(x * x + toDirX * toDirX + w * w)
        x *= invNorm
        val y = toDirX * invNorm
        w *= invNorm
        val q00 = (x + x) * x
        val q11 = (y + y) * y
        val q01 = (x + x) * y
        val q03 = (x + x) * w
        val q13 = (y + y) * w
        m00 = (1.0 - q11).toFloat()
        m01 = q01.toFloat()
        m02 = -q13.toFloat()
        m10 = q01.toFloat()
        m11 = (1.0 - q00).toFloat()
        m12 = q03.toFloat()
        m20 = q13.toFloat()
        m21 = -q03.toFloat()
        m22 = (1.0 - q11 - q00).toFloat()
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
        return this
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
        result = 31 * result + m30.hashCode()
        result = 31 * result + m31.hashCode()
        result = 31 * result + m32.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Matrix4x3 &&
                m00 == other.m00 && m01 == other.m01 && m02 == other.m02 &&
                m10 == other.m10 && m11 == other.m11 && m12 == other.m12 &&
                m20 == other.m20 && m21 == other.m21 && m22 == other.m22 &&
                m30 == other.m30 && m31 == other.m31 && m32 == other.m32
    }

    override fun equals(other: Matrix4x3?, threshold: Double): Boolean {
        return equals(other, threshold.toFloat())
    }

    fun equals(m: Matrix4x3?, delta: Float): Boolean {
        if (m === this) return true
        return m is Matrix4x3 &&
                Runtime.equals(m00, m.m00, delta) && Runtime.equals(m01, m.m01, delta) &&
                Runtime.equals(m02, m.m02, delta) && Runtime.equals(m10, m.m10, delta) &&
                Runtime.equals(m11, m.m11, delta) && Runtime.equals(m12, m.m12, delta) &&
                Runtime.equals(m20, m.m20, delta) && Runtime.equals(m21, m.m21, delta) &&
                Runtime.equals(m22, m.m22, delta) &&
                Runtime.equals(m30, m.m30, delta.toDouble()) &&
                Runtime.equals(m31, m.m31, delta.toDouble()) &&
                Runtime.equals(m32, m.m32, delta.toDouble())
    }

    @JvmOverloads
    fun pick(x: Float, y: Float, width: Float, height: Float, viewport: IntArray, dst: Matrix4x3 = this): Matrix4x3 {
        val sx = viewport[2].toFloat() / width
        val sy = viewport[3].toFloat() / height
        val tx = (viewport[2].toFloat() + 2f * (viewport[0].toFloat() - x)) / width
        val ty = (viewport[3].toFloat() + 2f * (viewport[1].toFloat() - y)) / height
        dst.m30 = m00 * tx + m10 * ty + m30
        dst.m31 = m01 * tx + m11 * ty + m31
        dst.m32 = m02 * tx + m12 * ty + m32
        dst.m00 = m00 * sx
        dst.m01 = m01 * sx
        dst.m02 = m02 * sx
        dst.m10 = m10 * sy
        dst.m11 = m11 * sy
        dst.m12 = m12 * sy
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun arcball(
        radius: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        angleX: Float, angleY: Float,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
        val m30 = m20 * -radius + m30
        val m31 = m21 * -radius + m31
        val m32 = m22 * -radius + m32
        var sin = sin(angleX)
        var cos = cos(angleX)
        val nm10 = m10 * cos + m20 * sin
        val nm11 = m11 * cos + m21 * sin
        val nm12 = m12 * cos + m22 * sin
        val m20 = m20 * cos - m10 * sin
        val m21 = m21 * cos - m11 * sin
        val m22 = m22 * cos - m12 * sin
        sin = sin(angleY)
        cos = cos(angleY)
        val nm00 = m00 * cos - m20 * sin
        val nm01 = m01 * cos - m21 * sin
        val nm02 = m02 * cos - m22 * sin
        val nm20 = m00 * sin + m20 * cos
        val nm21 = m01 * sin + m21 * cos
        val nm22 = m02 * sin + m22 * cos
        dst.m30 = -nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30
        dst.m31 = -nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31
        dst.m32 = -nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.flags = flags and -13
        return dst
    }

    fun arcball(radius: Float, center: Vector3f, angleX: Float, angleY: Float, dst: Matrix4x3): Matrix4x3 {
        return arcball(radius, center.x, center.y, center.z, angleX, angleY, dst)
    }

    fun arcball(radius: Float, center: Vector3f, angleX: Float, angleY: Float): Matrix4x3 {
        return arcball(radius, center.x, center.y, center.z, angleX, angleY, this)
    }

    fun transformAab(
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        outMin: Vector3d,
        outMax: Vector3d
    ): Matrix4x3 {
        val xax = m00 * minX
        val xay = m01 * minX
        val xaz = m02 * minX
        val xbx = m00 * maxX
        val xby = m01 * maxX
        val xbz = m02 * maxX
        val yax = m10 * minY
        val yay = m11 * minY
        val yaz = m12 * minY
        val ybx = m10 * maxY
        val yby = m11 * maxY
        val ybz = m12 * maxY
        val zax = m20 * minZ
        val zay = m21 * minZ
        val zaz = m22 * minZ
        val zbx = m20 * maxZ
        val zby = m21 * maxZ
        val zbz = m22 * maxZ
        val xminx: Double
        val xmaxx: Double
        if (xax < xbx) {
            xminx = xax
            xmaxx = xbx
        } else {
            xminx = xbx
            xmaxx = xax
        }
        val xminy: Double
        val xmaxy: Double
        if (xay < xby) {
            xminy = xay
            xmaxy = xby
        } else {
            xminy = xby
            xmaxy = xay
        }
        val xminz: Double
        val xmaxz: Double
        if (xaz < xbz) {
            xminz = xaz
            xmaxz = xbz
        } else {
            xminz = xbz
            xmaxz = xaz
        }
        val yminx: Double
        val ymaxx: Double
        if (yax < ybx) {
            yminx = yax
            ymaxx = ybx
        } else {
            yminx = ybx
            ymaxx = yax
        }
        val yminy: Double
        val ymaxy: Double
        if (yay < yby) {
            yminy = yay
            ymaxy = yby
        } else {
            yminy = yby
            ymaxy = yay
        }
        val yminz: Double
        val ymaxz: Double
        if (yaz < ybz) {
            yminz = yaz
            ymaxz = ybz
        } else {
            yminz = ybz
            ymaxz = yaz
        }
        val zminx: Double
        val zmaxx: Double
        if (zax < zbx) {
            zminx = zax
            zmaxx = zbx
        } else {
            zminx = zbx
            zmaxx = zax
        }
        val zminy: Double
        val zmaxy: Double
        if (zay < zby) {
            zminy = zay
            zmaxy = zby
        } else {
            zminy = zby
            zmaxy = zay
        }
        val zminz: Double
        val zmaxz: Double
        if (zaz < zbz) {
            zminz = zaz
            zmaxz = zbz
        } else {
            zminz = zbz
            zmaxz = zaz
        }
        outMin.x = xminx + yminx + zminx + m30
        outMin.y = xminy + yminy + zminy + m31
        outMin.z = xminz + yminz + zminz + m32
        outMax.x = xmaxx + ymaxx + zmaxx + m30
        outMax.y = xmaxy + ymaxy + zmaxy + m31
        outMax.z = xmaxz + ymaxz + zmaxz + m32
        return this
    }

    fun transformAab(min: Vector3d, max: Vector3d, outMin: Vector3d, outMax: Vector3d): Matrix4x3 {
        return transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax)
    }

    @JvmOverloads
    fun mix(other: Matrix4x3, t: Float, dst: Matrix4x3 = this): Matrix4x3 {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m02 = (other.m02 - m02) * t + m02
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        dst.m12 = (other.m12 - m12) * t + m12
        dst.m20 = (other.m20 - m20) * t + m20
        dst.m21 = (other.m21 - m21) * t + m21
        dst.m22 = (other.m22 - m22) * t + m22
        dst.m30 = (other.m30 - m30) * t + m30
        dst.m31 = (other.m31 - m31) * t + m31
        dst.m32 = (other.m32 - m32) * t + m32
        dst.flags = flags and other.properties()
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix4x3, t: Float, dst: Matrix4x3 = this): Matrix4x3 {
        return mix(other, t, dst)
    }

    fun rotateTowards(dir: Vector3f, up: Vector3f, dst: Matrix4x3): Matrix4x3 {
        return rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    fun rotateTowards(dir: Vector3f, up: Vector3f): Matrix4x3 {
        return rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4x3 = this
    ): Matrix4x3 {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
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
        dst.flags = flags and -13
        return dst
    }

    fun rotationTowards(dir: Vector3f, up: Vector3f): Matrix4x3 {
        return rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix4x3 {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun translationRotateTowards(pos: Vector3d, dir: Vector3f, up: Vector3f): Matrix4x3 {
        return translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun translationRotateTowards(
        posX: Double, posY: Double, posZ: Double,
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4x3 {
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
        m30 = posX
        m31 = posY
        m32 = posZ
        flags = 16
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

    fun obliqueZ(a: Float, b: Float): Matrix4x3 {
        m20 += m00 * a + m10 * b
        m21 += m01 * a + m11 * b
        m22 += m02 * a + m12 * b
        flags = 0
        return this
    }

    fun obliqueZ(a: Float, b: Float, dst: Matrix4x3): Matrix4x3 {
        dst.m00 = m00
        dst.m01 = m01
        dst.m02 = m02
        dst.m10 = m10
        dst.m11 = m11
        dst.m12 = m12
        dst.m20 = m00 * a + m10 * b + m20
        dst.m21 = m01 * a + m11 * b + m21
        dst.m22 = m02 * a + m12 * b + m22
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun withLookAtUp(up: Vector3f, dst: Matrix4x3 = this): Matrix4x3 {
        return withLookAtUp(up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun withLookAtUp(upX: Float, upY: Float, upZ: Float, dst: Matrix4x3 = this): Matrix4x3 {
        val y = (upY * m21 - upZ * m11) * m02 + (upZ * m01 - upX * m21) * m12 + (upX * m11 - upY * m01) * m22
        var x = upX * m01 + upY * m11 + upZ * m21
        if (flags and 16 == 0) {
            x *= sqrt(m01 * m01 + m11 * m11 + m21 * m21)
        }
        val invsqrt = JomlMath.invsqrt(y * y + x * x)
        val c = x * invsqrt
        val s = y * invsqrt
        val nm00 = c * m00 - s * m01
        val nm10 = c * m10 - s * m11
        val nm20 = c * m20 - s * m21
        val nm31 = s * m30 + c * m31
        val nm01 = s * m00 + c * m01
        val nm11 = s * m10 + c * m11
        val nm21 = s * m20 + c * m21
        val nm30 = c * m30 - s * m31
        dst._m00(nm00)._m10(nm10)._m20(nm20)._m30(nm30)._m01(nm01)._m11(nm11)._m21(nm21)._m31(nm31)
        if (dst !== this) {
            dst._m02(m02)._m12(m12)._m22(m22)._m32(m32)
        }
        dst.flags = flags and -13
        return dst
    }

    fun distanceSquared(other: Vector3d): Double {
        return lengthSquared(m30 - other.x, m31 - other.y, m32 - other.z)
    }

    fun distanceSquared(other: Matrix4x3): Double {
        return lengthSquared(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    fun distanceSquared(other: Matrix4x3f): Double {
        return lengthSquared(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    fun distance(other: Vector3d): Double {
        return sqrt(distanceSquared(other))
    }

    fun distance(other: Matrix4x3): Double {
        return sqrt(distanceSquared(other))
    }

    fun distance(other: Matrix4x3f): Double {
        return sqrt(distanceSquared(other))
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) && JomlMath.isFinite(m10) &&
                JomlMath.isFinite(m11) && JomlMath.isFinite(m12) && JomlMath.isFinite(m20) && JomlMath.isFinite(m21) &&
                JomlMath.isFinite(m22) && JomlMath.isFinite(m30) && JomlMath.isFinite(m31) && JomlMath.isFinite(m32)

    fun isIdentity(): Boolean {
        return flags.and(PROPERTY_IDENTITY) != 0
    }

    companion object {
        const val PROPERTY_IDENTITY = 4
    }
}