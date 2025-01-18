package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import java.nio.FloatBuffer
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

@Suppress("unused")
open class Matrix4f : Matrix {

    var m00 = 0f
    var m01 = 0f
    var m02 = 0f
    var m03 = 0f
    var m10 = 0f
    var m11 = 0f
    var m12 = 0f
    var m13 = 0f
    var m20 = 0f
    var m21 = 0f
    var m22 = 0f
    var m23 = 0f
    var m30 = 0f
    var m31 = 0f
    var m32 = 0f
    var m33 = 0f

    var flags = 0

    constructor() {
        _m00(1f)._m11(1f)._m22(1f)._m33(1f)._properties(30)
    }

    constructor(mat: Matrix3f) {
        this.set(mat)
    }

    constructor(mat: Matrix4f) {
        this.set(mat)
    }

    constructor(mat: Matrix4x3f) {
        this.set(mat)
    }

    constructor(mat: Matrix4d) {
        this.set(mat)
    }

    constructor(
        m00: Float,
        m01: Float,
        m02: Float,
        m03: Float,
        m10: Float,
        m11: Float,
        m12: Float,
        m13: Float,
        m20: Float,
        m21: Float,
        m22: Float,
        m23: Float,
        m30: Float,
        m31: Float,
        m32: Float,
        m33: Float
    ) {
        _m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(m20)._m21(m21)._m22(m22)
            ._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33).determineProperties()
    }

    constructor(col0: Vector4f, col1: Vector4f, col2: Vector4f, col3: Vector4f) {
        set(col0, col1, col2, col3)
    }

    override val numCols: Int get() = 4
    override val numRows: Int get() = 4

    fun _properties(properties: Int): Matrix4f {
        this.flags = properties
        return this
    }

    fun assume(properties: Int): Matrix4f {
        _properties(properties)
        return this
    }

    fun determineProperties(): Matrix4f {
        var flags = 0
        if (m03 == 0f && m13 == 0f) {
            if (m23 == 0f && m33 == 1f) {
                flags = flags or 2
                if (m00 == 1f && m01 == 0f && m02 == 0f && m10 == 0f && m11 == 1f && m12 == 0f && m20 == 0f && m21 == 0f && m22 == 1f) {
                    flags = flags or 24
                    if (m30 == 0f && m31 == 0f && m32 == 0f) {
                        flags = flags or 4
                    }
                }
            } else if (m01 == 0f && m02 == 0f && m10 == 0f && m12 == 0f && m20 == 0f && m21 == 0f && m30 == 0f && m31 == 0f && m33 == 0f) {
                flags = flags or 1
            }
        }
        this.flags = flags
        return this
    }

    fun properties(): Int {
        return flags
    }

    fun _m00(m00: Float): Matrix4f {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Float): Matrix4f {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Float): Matrix4f {
        this.m02 = m02
        return this
    }

    fun _m03(m03: Float): Matrix4f {
        this.m03 = m03
        return this
    }

    fun _m10(m10: Float): Matrix4f {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Float): Matrix4f {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Float): Matrix4f {
        this.m12 = m12
        return this
    }

    fun _m13(m13: Float): Matrix4f {
        this.m13 = m13
        return this
    }

    fun _m20(m20: Float): Matrix4f {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Float): Matrix4f {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Float): Matrix4f {
        this.m22 = m22
        return this
    }

    fun _m23(m23: Float): Matrix4f {
        this.m23 = m23
        return this
    }

    fun _m30(m30: Float): Matrix4f {
        this.m30 = m30
        return this
    }

    fun _m31(m31: Float): Matrix4f {
        this.m31 = m31
        return this
    }

    fun _m32(m32: Float): Matrix4f {
        this.m32 = m32
        return this
    }

    fun _m33(m33: Float): Matrix4f {
        this.m33 = m33
        return this
    }

    fun identity(): Matrix4f {
        return if (flags and 4 != 0) this else _m00(1f)._m01(0f)._m02(0f)._m03(0f)._m10(0f)._m11(1f)._m12(0f)
            ._m13(0f)._m20(0f)._m21(0f)._m22(1f)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(30)
    }

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01).put(m02).put(m03)
        arr.put(m10).put(m11).put(m12).put(m13)
        arr.put(m20).put(m21).put(m22).put(m23)
        arr.put(m30).put(m31).put(m32).put(m33)
        return arr
    }

    fun set(m: Matrix4f): Matrix4f {
        return _m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(m.m03)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(m.m13)
            ._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(m.m23)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(m.m33)
            ._properties(m.properties())
    }

    fun set(other: Matrix4x3d): Matrix4f {
        return set(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), 0f,
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), 0f,
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), 0f,
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), 1f
        )
    }

    fun setTransposed(m: Matrix4f): Matrix4f {
        return if (m.properties() and 4 != 0) identity() else setTransposedInternal(m)
    }

    private fun setTransposedInternal(m: Matrix4f): Matrix4f {
        val nm10 = m.m01
        val nm12 = m.m21
        val nm13 = m.m31
        val nm20 = m.m02
        val nm21 = m.m12
        val nm30 = m.m03
        val nm31 = m.m13
        val nm32 = m.m23
        return _m00(m.m00)._m01(m.m10)._m02(m.m20)._m03(m.m30)._m10(nm10)._m11(m.m11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(m.m22)._m23(m.m32)._m30(nm30)._m31(nm31)._m32(nm32)._m33(m.m33)
            ._properties(m.properties() and 4)
    }

    fun set(m: Matrix4x3f): Matrix4f {
        return _m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(0f)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(0f)._m20(m.m20)
            ._m21(m.m21)._m22(m.m22)._m23(0f)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(1f)
            ._properties(m.properties() or 2)
    }

    fun set(m: Matrix4d): Matrix4f {
        return _m00(m.m00.toFloat())._m01(m.m01.toFloat())._m02(m.m02.toFloat())._m03(m.m03.toFloat())
            ._m10(m.m10.toFloat())._m11(m.m11.toFloat())._m12(m.m12.toFloat())._m13(m.m13.toFloat())
            ._m20(m.m20.toFloat())._m21(m.m21.toFloat())._m22(m.m22.toFloat())._m23(m.m23.toFloat())
            ._m30(m.m30.toFloat())._m31(m.m31.toFloat())._m32(m.m32.toFloat())._m33(m.m33.toFloat())
            ._properties(m.properties())
    }

    fun set(mat: Matrix3f): Matrix4f {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m03(0f)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m13(0f)
            ._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(2)
    }

    fun set(axisAngle: AxisAngle4f): Matrix4f {
        var x = axisAngle.x
        var y = axisAngle.y
        var z = axisAngle.z
        val angle = axisAngle.angle
        var n = sqrt(x * x + y * y + z * z).toDouble()
        n = 1.0 / n
        x = (x.toDouble() * n).toFloat()
        y = (y.toDouble() * n).toFloat()
        z = (z.toDouble() * n).toFloat()
        val s = sin(angle)
        val c = cos(angle)
        val omc = 1f - c
        _m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc)
        var tmp1 = x * y * omc
        var tmp2 = z * s
        _m10(tmp1 - tmp2)._m01(tmp1 + tmp2)
        tmp1 = x * z * omc
        tmp2 = y * s
        _m20(tmp1 + tmp2)._m02(tmp1 - tmp2)
        tmp1 = y * z * omc
        tmp2 = x * s
        return _m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0f)._m13(0f)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)
            ._properties(18)
    }

    fun set(axisAngle: AxisAngle4d): Matrix4f {
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
        _m00((c + x * x * omc).toFloat())._m11((c + y * y * omc).toFloat())._m22((c + z * z * omc).toFloat())
        var tmp1 = x * y * omc
        var tmp2 = z * s
        _m10((tmp1 - tmp2).toFloat())._m01((tmp1 + tmp2).toFloat())
        tmp1 = x * z * omc
        tmp2 = y * s
        _m20((tmp1 + tmp2).toFloat())._m02((tmp1 - tmp2).toFloat())
        tmp1 = y * z * omc
        tmp2 = x * s
        return _m21((tmp1 - tmp2).toFloat())._m12((tmp1 + tmp2).toFloat())._m03(0f)._m13(0f)._m23(0f)._m30(0f)._m31(0f)
            ._m32(0f)._m33(1f)._properties(18)
    }

    fun set(q: Quaternionf): Matrix4f {
        return rotation(q)
    }

    fun set4x3(mat: Matrix4x3f): Matrix4f {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)
            ._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)._properties(
                flags and mat.properties() and -2
            )
    }

    /*fun set4x3(mat: Matrix4f): Matrix4f {
        MemUtil.INSTANCE.copy4x3(mat, this)
        return _properties(properties and mat.properties and -2)
    }*/

    @JvmOverloads
    fun mul(right: Matrix4f, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else if (flags and 8 != 0 && right.properties() and 2 != 0) {
            mulTranslationAffine(right, dst)
        } else if (flags and 2 != 0 && right.properties() and 2 != 0) {
            this.mulAffine(right, dst)
        } else if (flags and 1 != 0 && right.properties() and 2 != 0) {
            this.mulPerspectiveAffine(right, dst)
        } else {
            if (right.properties() and 2 != 0) mulAffineR(right, dst) else mul0(right, dst)
        }
    }

    @JvmOverloads
    fun mul0(right: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02 + m30 * right.m03
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02 + m31 * right.m03
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02 + m32 * right.m03
        val nm03 = m03 * right.m00 + m13 * right.m01 + m23 * right.m02 + m33 * right.m03
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12 + m30 * right.m13
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12 + m31 * right.m13
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12 + m32 * right.m13
        val nm13 = m03 * right.m10 + m13 * right.m11 + m23 * right.m12 + m33 * right.m13
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22 + m30 * right.m23
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22 + m31 * right.m23
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22 + m32 * right.m23
        val nm23 = m03 * right.m20 + m13 * right.m21 + m23 * right.m22 + m33 * right.m23
        val nm30 = m00 * right.m30 + m10 * right.m31 + m20 * right.m32 + m30 * right.m33
        val nm31 = m01 * right.m30 + m11 * right.m31 + m21 * right.m32 + m31 * right.m33
        val nm32 = m02 * right.m30 + m12 * right.m31 + m22 * right.m32 + m32 * right.m33
        val nm33 = m03 * right.m30 + m13 * right.m31 + m23 * right.m32 + m33 * right.m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mul(
        r00: Float, r01: Float, r02: Float, r03: Float, r10: Float, r11: Float, r12: Float, r13: Float,
        r20: Float, r21: Float, r22: Float, r23: Float, r30: Float, r31: Float, r32: Float, r33: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) {
            dst.set(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33)
        } else {
            if (flags and 2 != 0) mulAffineL(
                r00, r01, r02, r03, r10, r11, r12, r13,
                r20, r21, r22, r23, r30, r31, r32, r33, dst
            ) else this.mulGeneric(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dst)
        }
    }

    fun mul(other: Matrix4d): Matrix4f {
        return mul(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), other.m03.toFloat(),
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), other.m13.toFloat(),
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), other.m23.toFloat(),
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), other.m33.toFloat()
        )
    }

    fun mul(other: Matrix4x3d): Matrix4f {
        return mul(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), 0f,
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), 0f,
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), 0f,
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), 1f
        )
    }

    private fun mulAffineL(
        r00: Float, r01: Float, r02: Float, r03: Float, r10: Float, r11: Float, r12: Float, r13: Float,
        r20: Float, r21: Float, r22: Float, r23: Float, r30: Float, r31: Float, r32: Float, r33: Float, dst: Matrix4f
    ): Matrix4f {
        val nm00 = m00 * r00 + m10 * r01 + m20 * r02 + m30 * r03
        val nm01 = m01 * r00 + m11 * r01 + m21 * r02 + m31 * r03
        val nm02 = m02 * r00 + m12 * r01 + m22 * r02 + m32 * r03
        val nm10 = m00 * r10 + m10 * r11 + m20 * r12 + m30 * r13
        val nm11 = m01 * r10 + m11 * r11 + m21 * r12 + m31 * r13
        val nm12 = m02 * r10 + m12 * r11 + m22 * r12 + m32 * r13
        val nm20 = m00 * r20 + m10 * r21 + m20 * r22 + m30 * r23
        val nm21 = m01 * r20 + m11 * r21 + m21 * r22 + m31 * r23
        val nm22 = m02 * r20 + m12 * r21 + m22 * r22 + m32 * r23
        val nm30 = m00 * r30 + m10 * r31 + m20 * r32 + m30 * r33
        val nm31 = m01 * r30 + m11 * r31 + m21 * r32 + m31 * r33
        val nm32 = m02 * r30 + m12 * r31 + m22 * r32 + m32 * r33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(r03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(r13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(r23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(r33)._properties(2)
    }

    private fun mulGeneric(
        r00: Float, r01: Float, r02: Float, r03: Float, r10: Float, r11: Float, r12: Float, r13: Float,
        r20: Float, r21: Float, r22: Float, r23: Float, r30: Float, r31: Float, r32: Float, r33: Float, dst: Matrix4f
    ): Matrix4f {
        val nm00 = m00 * r00 + m10 * r01 + m20 * r02 + m30 * r03
        val nm01 = m01 * r00 + m11 * r01 + m21 * r02 + m31 * r03
        val nm02 = m02 * r00 + m12 * r01 + m22 * r02 + m32 * r03
        val nm03 = m03 * r00 + m13 * r01 + m23 * r02 + m33 * r03
        val nm10 = m00 * r10 + m10 * r11 + m20 * r12 + m30 * r13
        val nm11 = m01 * r10 + m11 * r11 + m21 * r12 + m31 * r13
        val nm12 = m02 * r10 + m12 * r11 + m22 * r12 + m32 * r13
        val nm13 = m03 * r10 + m13 * r11 + m23 * r12 + m33 * r13
        val nm20 = m00 * r20 + m10 * r21 + m20 * r22 + m30 * r23
        val nm21 = m01 * r20 + m11 * r21 + m21 * r22 + m31 * r23
        val nm22 = m02 * r20 + m12 * r21 + m22 * r22 + m32 * r23
        val nm23 = m03 * r20 + m13 * r21 + m23 * r22 + m33 * r23
        val nm30 = m00 * r30 + m10 * r31 + m20 * r32 + m30 * r33
        val nm31 = m01 * r30 + m11 * r31 + m21 * r32 + m31 * r33
        val nm32 = m02 * r30 + m12 * r31 + m22 * r32 + m32 * r33
        val nm33 = m03 * r30 + m13 * r31 + m23 * r32 + m33 * r33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mul3x3(
        r00: Float, r01: Float, r02: Float, r10: Float, r11: Float, r12: Float,
        r20: Float, r21: Float, r22: Float, dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.set(
            r00, r01, r02, 0f, r10, r11, r12, 0f,
            r20, r21, r22, 0f, 0f, 0f, 0f, 1f
        ) else mulGeneric3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, dst)
    }

    private fun mulGeneric3x3(
        r00: Float, r01: Float, r02: Float, r10: Float, r11: Float, r12: Float,
        r20: Float, r21: Float, r22: Float, dst: Matrix4f
    ): Matrix4f {
        val nm00 = m00 * r00 + m10 * r01 + m20 * r02
        val nm01 = m01 * r00 + m11 * r01 + m21 * r02
        val nm02 = m02 * r00 + m12 * r01 + m22 * r02
        val nm03 = m03 * r00 + m13 * r01 + m23 * r02
        val nm10 = m00 * r10 + m10 * r11 + m20 * r12
        val nm11 = m01 * r10 + m11 * r11 + m21 * r12
        val nm12 = m02 * r10 + m12 * r11 + m22 * r12
        val nm13 = m03 * r10 + m13 * r11 + m23 * r12
        val nm20 = m00 * r20 + m10 * r21 + m20 * r22
        val nm21 = m01 * r20 + m11 * r21 + m21 * r22
        val nm22 = m02 * r20 + m12 * r21 + m22 * r22
        val nm23 = m03 * r20 + m13 * r21 + m23 * r22
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(flags and 2)
    }

    @JvmOverloads
    fun mulLocal(left: Matrix4f, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.set(left)
        } else if (left.properties() and 4 != 0) {
            dst.set(this)
        } else {
            if (flags and 2 != 0 && left.properties() and 2 != 0) mulLocalAffine(
                left,
                dst
            ) else mulLocalGeneric(left, dst)
        }
    }

    private fun mulLocalGeneric(left: Matrix4f, dst: Matrix4f): Matrix4f {
        val nm00 = left.m00 * m00 + left.m10 * m01 + left.m20 * m02 + left.m30 * m03
        val nm01 = left.m01 * m00 + left.m11 * m01 + left.m21 * m02 + left.m31 * m03
        val nm02 = left.m02 * m00 + left.m12 * m01 + left.m22 * m02 + left.m32 * m03
        val nm03 = left.m03 * m00 + left.m13 * m01 + left.m23 * m02 + left.m33 * m03
        val nm10 = left.m00 * m10 + left.m10 * m11 + left.m20 * m12 + left.m30 * m13
        val nm11 = left.m01 * m10 + left.m11 * m11 + left.m21 * m12 + left.m31 * m13
        val nm12 = left.m02 * m10 + left.m12 * m11 + left.m22 * m12 + left.m32 * m13
        val nm13 = left.m03 * m10 + left.m13 * m11 + left.m23 * m12 + left.m33 * m13
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20 * m22 + left.m30 * m23
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21 * m22 + left.m31 * m23
        val nm22 = left.m02 * m20 + left.m12 * m21 + left.m22 * m22 + left.m32 * m23
        val nm23 = left.m03 * m20 + left.m13 * m21 + left.m23 * m22 + left.m33 * m23
        val nm30 = left.m00 * m30 + left.m10 * m31 + left.m20 * m32 + left.m30 * m33
        val nm31 = left.m01 * m30 + left.m11 * m31 + left.m21 * m32 + left.m31 * m33
        val nm32 = left.m02 * m30 + left.m12 * m31 + left.m22 * m32 + left.m32 * m33
        val nm33 = left.m03 * m30 + left.m13 * m31 + left.m23 * m32 + left.m33 * m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mulLocalAffine(left: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val nm00 = left.m00 * m00 + left.m10 * m01 + left.m20 * m02
        val nm01 = left.m01 * m00 + left.m11 * m01 + left.m21 * m02
        val nm02 = left.m02 * m00 + left.m12 * m01 + left.m22 * m02
        val nm03 = left.m03
        val nm10 = left.m00 * m10 + left.m10 * m11 + left.m20 * m12
        val nm11 = left.m01 * m10 + left.m11 * m11 + left.m21 * m12
        val nm12 = left.m02 * m10 + left.m12 * m11 + left.m22 * m12
        val nm13 = left.m13
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20 * m22
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21 * m22
        val nm22 = left.m02 * m20 + left.m12 * m21 + left.m22 * m22
        val nm23 = left.m23
        val nm30 = left.m00 * m30 + left.m10 * m31 + left.m20 * m32 + left.m30
        val nm31 = left.m01 * m30 + left.m11 * m31 + left.m21 * m32 + left.m31
        val nm32 = left.m02 * m30 + left.m12 * m31 + left.m22 * m32 + left.m32
        val nm33 = left.m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(2 or (properties() and left.properties() and 16))
    }

    @JvmOverloads
    fun mul(right: Matrix4x3f, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else if (flags and 8 != 0) {
            mulTranslation(right, dst)
        } else if (flags and 2 != 0) {
            this.mulAffine(right, dst)
        } else {
            if (flags and 1 != 0) this.mulPerspectiveAffine(right, dst) else this.mulGeneric(right, dst)
        }
    }

    private fun mulTranslation(right: Matrix4x3f, dst: Matrix4f): Matrix4f {
        return dst._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(m03)._m10(right.m10)._m11(right.m11)
            ._m12(right.m12)._m13(
                m13
            )._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(m23)._m30(right.m30 + m30)._m31(right.m31 + m31)
            ._m32(right.m32 + m32)._m33(
                m33
            )._properties(2 or (right.properties() and 16))
    }

    private fun mulAffine(right: Matrix4x3f, dst: Matrix4f): Matrix4f {
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
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)._m03(m03)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)._m13(m13)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(m23)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)
            ._m33(m33)._properties(2 or (flags and right.properties() and 16))
    }

    private fun mulGeneric(right: Matrix4x3f, dst: Matrix4f): Matrix4f {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02
        val nm03 = m03 * right.m00 + m13 * right.m01 + m23 * right.m02
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12
        val nm13 = m03 * right.m10 + m13 * right.m11 + m23 * right.m12
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22
        val nm23 = m03 * right.m20 + m13 * right.m21 + m23 * right.m22
        val nm30 = m00 * right.m30 + m10 * right.m31 + m20 * right.m32 + m30
        val nm31 = m01 * right.m30 + m11 * right.m31 + m21 * right.m32 + m31
        val nm32 = m02 * right.m30 + m12 * right.m31 + m22 * right.m32 + m32
        val nm33 = m03 * right.m30 + m13 * right.m31 + m23 * right.m32 + m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(flags and -30)
    }

    @JvmOverloads
    fun mul(right: Matrix3x2f, dst: Matrix4f = this): Matrix4f {
        val nm00 = m00 * right.m00 + m10 * right.m01
        val nm01 = m01 * right.m00 + m11 * right.m01
        val nm02 = m02 * right.m00 + m12 * right.m01
        val nm03 = m03 * right.m00 + m13 * right.m01
        val nm10 = m00 * right.m10 + m10 * right.m11
        val nm11 = m01 * right.m10 + m11 * right.m11
        val nm12 = m02 * right.m10 + m12 * right.m11
        val nm13 = m03 * right.m10 + m13 * right.m11
        val nm30 = m00 * right.m20 + m10 * right.m21 + m30
        val nm31 = m01 * right.m20 + m11 * right.m21 + m31
        val nm32 = m02 * right.m20 + m12 * right.m21 + m32
        val nm33 = m03 * right.m20 + m13 * right.m21 + m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(flags and -30)
    }

    @JvmOverloads
    fun mulPerspectiveAffine(view: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val nm00 = m00 * view.m00
        val nm01 = m11 * view.m01
        val nm02 = m22 * view.m02
        val nm03 = m23 * view.m02
        val nm10 = m00 * view.m10
        val nm11 = m11 * view.m11
        val nm12 = m22 * view.m12
        val nm13 = m23 * view.m12
        val nm20 = m00 * view.m20
        val nm21 = m11 * view.m21
        val nm22 = m22 * view.m22
        val nm23 = m23 * view.m22
        val nm30 = m00 * view.m30
        val nm31 = m11 * view.m31
        val nm32 = m22 * view.m32 + m32
        val nm33 = m23 * view.m32
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mulPerspectiveAffine(view: Matrix4x3f, dst: Matrix4f = this): Matrix4f {
        val lm00 = m00
        val lm11 = m11
        val lm22 = m22
        val lm23 = m23
        return dst._m00(lm00 * view.m00)._m01(lm11 * view.m01)._m02(lm22 * view.m02)._m03(lm23 * view.m02)
            ._m10(lm00 * view.m10)._m11(lm11 * view.m11)._m12(lm22 * view.m12)._m13(lm23 * view.m12)
            ._m20(lm00 * view.m20)._m21(lm11 * view.m21)._m22(lm22 * view.m22)._m23(lm23 * view.m22)
            ._m30(lm00 * view.m30)._m31(lm11 * view.m31)._m32(lm22 * view.m32 + m32)._m33(lm23 * view.m32)
            ._properties(0)
    }

    @JvmOverloads
    fun mulAffineR(right: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val nm00 = m00 * right.m00 + m10 * right.m01 + m20 * right.m02
        val nm01 = m01 * right.m00 + m11 * right.m01 + m21 * right.m02
        val nm02 = m02 * right.m00 + m12 * right.m01 + m22 * right.m02
        val nm03 = m03 * right.m00 + m13 * right.m01 + m23 * right.m02
        val nm10 = m00 * right.m10 + m10 * right.m11 + m20 * right.m12
        val nm11 = m01 * right.m10 + m11 * right.m11 + m21 * right.m12
        val nm12 = m02 * right.m10 + m12 * right.m11 + m22 * right.m12
        val nm13 = m03 * right.m10 + m13 * right.m11 + m23 * right.m12
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20 * right.m22
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21 * right.m22
        val nm22 = m02 * right.m20 + m12 * right.m21 + m22 * right.m22
        val nm23 = m03 * right.m20 + m13 * right.m21 + m23 * right.m22
        val nm30 = m00 * right.m30 + m10 * right.m31 + m20 * right.m32 + m30
        val nm31 = m01 * right.m30 + m11 * right.m31 + m21 * right.m32 + m31
        val nm32 = m02 * right.m30 + m12 * right.m31 + m22 * right.m32 + m32
        val nm33 = m03 * right.m30 + m13 * right.m31 + m23 * right.m32 + m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(
                flags and -30
            )
    }

    @JvmOverloads
    fun mulAffine(right: Matrix4f, dst: Matrix4f = this): Matrix4f {
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
        return dst._m00(m00 * rm00 + (m10 * rm01 + m20 * rm02))._m01(m01 * rm00 + (m11 * rm01 + m21 * rm02))
            ._m02(m02 * rm00 + (m12 * rm01 + m22 * rm02))._m03(m03)
            ._m10(m00 * rm10 + (m10 * rm11 + m20 * rm12))._m11(m01 * rm10 + (m11 * rm11 + m21 * rm12))
            ._m12(m02 * rm10 + (m12 * rm11 + m22 * rm12))._m13(m13)
            ._m20(m00 * rm20 + (m10 * rm21 + m20 * rm22))._m21(m01 * rm20 + (m11 * rm21 + m21 * rm22))
            ._m22(m02 * rm20 + (m12 * rm21 + m22 * rm22))._m23(m23)
            ._m30(m00 * rm30 + (m10 * rm31 + (m20 * rm32 + m30)))
            ._m31(m01 * rm30 + (m11 * rm31 + (m21 * rm32 + m31)))
            ._m32(m02 * rm30 + (m12 * rm31 + (m22 * rm32 + m32)))
            ._m33(m33)._properties(2 or (flags and right.properties() and 16))
    }

    fun mulTranslationAffine(right: Matrix4f, dst: Matrix4f): Matrix4f {
        return dst
            ._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(m03)
            ._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(m13)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(m23)
            ._m30(right.m30 + m30)._m31(right.m31 + m31)
            ._m32(right.m32 + m32)._m33(m33)
            ._properties(2 or (right.properties() and 16))
    }

    @JvmOverloads
    fun mulOrthoAffine(view: Matrix4f, dst: Matrix4f = this): Matrix4f {
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
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(0f)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)._properties(2)
    }

    @JvmOverloads
    fun fma4x3(o: Matrix4f, f: Float, dst: Matrix4f = this): Matrix4f {
        dst._m00(o.m00 * f + m00)._m01(o.m01 * f + m01)._m02(o.m02 * f + m02)._m03(m03)
            ._m10(o.m10 * f + m10)._m11(o.m11 * f + m11)._m12(o.m12 * f + m12)._m13(m13)
            ._m20(o.m20 * f + m20)._m21(o.m21 * f + m21)._m22(o.m22 * f + m22)._m23(m23)
            ._m30(o.m30 * f + m30)._m31(o.m31 * f + m31)._m32(o.m32 * f + m32)._m33(m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun add(other: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 + other.m00)._m01(m01 + other.m01)._m02(m02 + other.m02)._m03(m03 + other.m03)
            ._m10(m10 + other.m10)._m11(m11 + other.m11)._m12(m12 + other.m12)._m13(m13 + other.m13)
            ._m20(m20 + other.m20)._m21(m21 + other.m21)._m22(m22 + other.m22)._m23(m23 + other.m23)
            ._m30(m30 + other.m30)._m31(m31 + other.m31)._m32(m32 + other.m32)._m33(m33 + other.m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun sub(s: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 - s.m00)._m01(m01 - s.m01)._m02(m02 - s.m02)._m03(m03 - s.m03)
            ._m10(m10 - s.m10)._m11(m11 - s.m11)._m12(m12 - s.m12)._m13(m13 - s.m13)
            ._m20(m20 - s.m20)._m21(m21 - s.m21)._m22(m22 - s.m22)._m23(m23 - s.m23)
            ._m30(m30 - s.m30)._m31(m31 - s.m31)._m32(m32 - s.m32)._m33(m33 - s.m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun mulComponentWise(o: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 * o.m00)._m01(m01 * o.m01)._m02(m02 * o.m02)._m03(m03 * o.m03)
            ._m10(m10 * o.m10)._m11(m11 * o.m11)._m12(m12 * o.m12)._m13(m13 * o.m13)
            ._m20(m20 * o.m20)._m21(m21 * o.m21)._m22(m22 * o.m22)._m23(m23 * o.m23)
            ._m30(m30 * o.m30)._m31(m31 * o.m31)._m32(m32 * o.m32)._m33(m33 * o.m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun add4x3(o: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 + o.m00)._m01(m01 + o.m01)._m02(m02 + o.m02)._m03(m03)
            ._m10(m10 + o.m10)._m11(m11 + o.m11)._m12(m12 + o.m12)._m13(m13)
            ._m20(m20 + o.m20)._m21(m21 + o.m21)._m22(m22 + o.m22)._m23(m23)
            ._m30(m30 + o.m30)._m31(m31 + o.m31)._m32(m32 + o.m32)._m33(m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun sub4x3(s: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 - s.m00)._m01(m01 - s.m01)._m02(m02 - s.m02)._m03(m03)
            ._m10(m10 - s.m10)._m11(m11 - s.m11)._m12(m12 - s.m12)._m13(m13)
            ._m20(m20 - s.m20)._m21(m21 - s.m21)._m22(m22 - s.m22)._m23(m23)
            ._m30(m30 - s.m30)._m31(m31 - s.m31)._m32(m32 - s.m32)._m33(m33)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun mul4x3ComponentWise(o: Matrix4f, dst: Matrix4f = this): Matrix4f {
        dst._m00(m00 * o.m00)._m01(m01 * o.m01)._m02(m02 * o.m02)._m03(m03)._m10(m10 * o.m10)
            ._m11(m11 * o.m11)._m12(m12 * o.m12)._m13(m13)._m20(m20 * o.m20)._m21(m21 * o.m21)
            ._m22(m22 * o.m22)._m23(m23)._m30(m30 * o.m30)._m31(m31 * o.m31)._m32(m32 * o.m32)
            ._m33(m33)._properties(0)
        return dst
    }

    fun set(
        m00: Float, m01: Float, m02: Float, m03: Float,
        m10: Float, m11: Float, m12: Float, m13: Float,
        m20: Float, m21: Float, m22: Float, m23: Float,
        m30: Float, m31: Float, m32: Float, m33: Float
    ): Matrix4f {
        return _m00(m00)._m10(m10)._m20(m20)._m30(m30)._m01(m01)._m11(m11)._m21(m21)._m31(m31)._m02(m02)._m12(m12)
            ._m22(m22)._m32(m32)._m03(m03)._m13(m13)._m23(m23)._m33(m33).determineProperties()
    }

    fun set(col0: Vector4f, col1: Vector4f, col2: Vector4f, col3: Vector4f): Matrix4f {
        return _m00(col0.x)._m01(col0.y)._m02(col0.z)._m03(col0.w)
            ._m10(col1.x)._m11(col1.y)._m12(col1.z)._m13(col1.w)
            ._m20(col2.x)._m21(col2.y)._m22(col2.z)._m23(col2.w)
            ._m30(col3.x)._m31(col3.y)._m32(col3.z)._m33(col3.w)
            .determineProperties()
    }

    fun determinant(): Float {
        return if (flags and 2 != 0) determinantAffine() else (m00 * m11 - m01 * m10) * (m22 * m33 - m23 * m32) + (m02 * m10 - m00 * m12) * (m21 * m33 - m23 * m31) + (m00 * m13 - m03 * m10) * (m21 * m32 - m22 * m31) + (m01 * m12 - m02 * m11) * (m20 * m33 - m23 * m30) + (m03 * m11 - m01 * m13) * (m20 * m32 - m22 * m30) + (m02 * m13 - m03 * m12) * (m20 * m31 - m21 * m30)
    }

    fun determinant3x3(): Float {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    fun determinantAffine(): Float {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.identity()
        } else if (flags and 8 != 0) {
            invertTranslation(dst)
        } else if (flags and 16 != 0) {
            invertOrthonormal(dst)
        } else if (flags and 2 != 0) {
            invertAffine(dst)
        } else {
            if (flags and 1 != 0) invertPerspective(dst) else invertGeneric(dst)
        }
    }

    private fun invertTranslation(dst: Matrix4f): Matrix4f {
        if (dst !== this) {
            dst.set(this)
        }
        return dst._m30(-m30)._m31(-m31)._m32(-m32)._properties(26)
    }

    private fun invertOrthonormal(dst: Matrix4f): Matrix4f {
        val nm30 = -(m00 * m30 + m01 * m31 + m02 * m32)
        val nm31 = -(m10 * m30 + m11 * m31 + m12 * m32)
        val nm32 = -(m20 * m30 + m21 * m31 + m22 * m32)
        val m01 = m01
        val m02 = m02
        val m12 = m12
        return dst
            ._m00(m00)._m01(m10)._m02(m20)._m03(0f)
            ._m10(m01)._m11(m11)._m12(m21)._m13(0f)
            ._m20(m02)._m21(m12)._m22(m22)._m23(0f)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(1f)
            ._properties(18)
    }

    private fun invertGeneric(dst: Matrix4f): Matrix4f {
        return if (this !== dst) invertGenericNonThis(dst) else invertGenericThis(dst)
    }

    private fun invertGenericNonThis(dst: Matrix4f): Matrix4f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        var det = a * l - b * k + c * j + d * i - e * h + f * g
        det = 1f / det
        return dst._m00((m11 * l + (-m12 * k + m13 * j)) * det)
            ._m01((-m01 * l + m02 * k + -m03 * j) * det)
            ._m02((m31 * f - m32 * e + m33 * d) * det)
            ._m03((-m21 * f + m22 * e - m23 * d) * det)
            ._m10((-m10 * l + m12 * i - m13 * h) * det)
            ._m11((m00 * l - m02 * i + m03 * h) * det)
            ._m12((-m30 * f + m32 * c - m33 * b) * det)
            ._m13((m20 * f - m22 * c + m23 * b) * det)
            ._m20((m10 * k - m11 * i + m13 * g) * det)
            ._m21((-m00 * k + m01 * i - m03 * g) * det)
            ._m22((m30 * e - m31 * c + m33 * a) * det)
            ._m23((-m20 * e + m21 * c - m23 * a) * det)
            ._m30((-m10 * j + m11 * h - m12 * g) * det)
            ._m31((m00 * j - m01 * h + m02 * g) * det)
            ._m32((-m30 * d + m31 * b - m32 * a) * det)
            ._m33((m20 * d - m21 * b + m22 * a) * det)._properties(0)
    }

    private fun invertGenericThis(dst: Matrix4f): Matrix4f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        var det = a * l - b * k + c * j + d * i - e * h + f * g
        det = 1f / det
        val nm00 = (m11 * l + (-m12 * k + m13 * j)) * det
        val nm01 = (-m01 * l + (m02 * k + -m03 * j)) * det
        val nm02 = (m31 * f + (-m32 * e + m33 * d)) * det
        val nm03 = (-m21 * f + (m22 * e + -m23 * d)) * det
        val nm10 = (-m10 * l + (m12 * i + -m13 * h)) * det
        val nm11 = (m00 * l + (-m02 * i + m03 * h)) * det
        val nm12 = (-m30 * f + (m32 * c + -m33 * b)) * det
        val nm13 = (m20 * f + (-m22 * c + m23 * b)) * det
        val nm20 = (m10 * k + (-m11 * i + m13 * g)) * det
        val nm21 = (-m00 * k + (m01 * i + -m03 * g)) * det
        val nm22 = (m30 * e + (-m31 * c + m33 * a)) * det
        val nm23 = (-m20 * e + (m21 * c + -m23 * a)) * det
        val nm30 = (-m10 * j + (m11 * h + -m12 * g)) * det
        val nm31 = (m00 * j + (-m01 * h + m02 * g)) * det
        val nm32 = (-m30 * d + (m31 * b + -m32 * a)) * det
        val nm33 = (m20 * d + (-m21 * b + m22 * a)) * det
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun invertPerspective(dst: Matrix4f = this): Matrix4f {
        val a = 1f / (m00 * m11)
        val l = -1f / (m23 * m32)
        return dst.set(m11 * a, 0f, 0f, 0f, 0f, m00 * a, 0f, 0f, 0f, 0f, 0f, -m23 * l, 0f, 0f, -m32 * l, m22 * l)
            ._properties(0)
    }

    @JvmOverloads
    fun invertFrustum(dst: Matrix4f = this): Matrix4f {
        val invM00 = 1f / m00
        val invM11 = 1f / m11
        val invM23 = 1f / m23
        val invM32 = 1f / m32
        return dst.set(
            invM00, 0f, 0f, 0f,
            0f, invM11, 0f, 0f,
            0f, 0f, 0f, invM32,
            -m20 * invM00 * invM23,
            -m21 * invM11 * invM23,
            invM23,
            -m22 * invM23 * invM32
        )
    }

    @JvmOverloads
    fun invertOrtho(dst: Matrix4f = this): Matrix4f {
        val invM00 = 1f / m00
        val invM11 = 1f / m11
        val invM22 = 1f / m22
        return dst.set(
            invM00, 0f, 0f, 0f,
            0f, invM11, 0f, 0f,
            0f, 0f, invM22, 0f,
            -m30 * invM00,
            -m31 * invM11,
            -m32 * invM22, 1f
        )._properties(2 or (flags and 16))
    }

    fun invertPerspectiveView(view: Matrix4f, dst: Matrix4f): Matrix4f {
        val a = 1f / (m00 * m11)
        val l = -1f / (m23 * m32)
        val pm00 = m11 * a
        val pm11 = m00 * a
        val pm23 = -m23 * l
        val pm32 = -m32 * l
        val pm33 = m22 * l
        val vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32
        val vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32
        val vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32
        val nm10 = view.m01 * pm11
        val nm30 = view.m02 * pm32 + vm30 * pm33
        val nm31 = view.m12 * pm32 + vm31 * pm33
        val nm32 = view.m22 * pm32 + vm32 * pm33
        return dst._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0f)._m10(nm10)
            ._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0f)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)
            ._m23(pm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(pm33)._properties(0)
    }

    fun invertPerspectiveView(view: Matrix4x3f, dst: Matrix4f): Matrix4f {
        val a = 1f / (m00 * m11)
        val l = -1f / (m23 * m32)
        val pm00 = m11 * a
        val pm11 = m00 * a
        val pm23 = -m23 * l
        val pm32 = -m32 * l
        val pm33 = m22 * l
        val vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32
        val vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32
        val vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32
        return dst._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0f)._m10(view.m01 * pm11)
            ._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0f)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)
            ._m23(pm23)._m30(view.m02 * pm32 + vm30 * pm33)._m31(view.m12 * pm32 + vm31 * pm33)
            ._m32(view.m22 * pm32 + vm32 * pm33)._m33(pm33)._properties(0)
    }

    @JvmOverloads
    fun invertAffine(dst: Matrix4f = this): Matrix4f {
        val m11m00 = m00 * m11
        val m10m01 = m01 * m10
        val m10m02 = m02 * m10
        val m12m00 = m00 * m12
        val m12m01 = m01 * m12
        val m11m02 = m02 * m11
        val det = (m11m00 - m10m01) * m22 + (m10m02 - m12m00) * m21 + (m12m01 - m11m02) * m20
        val s = 1f / det
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
        val nm31 = (m20m02 * m31 - m20m01 * m32 + m21m00 * m32 - m21m02 * m30 + m22m01 * m30 - m22m00 * m31) * s
        val nm32 = (m11m02 * m30 - m12m01 * m30 + m12m00 * m31 - m10m02 * m31 + m10m01 * m32 - m11m00 * m32) * s
        return dst._m00((m11m22 - m12m21) * s)._m01((m21m02 - m22m01) * s)._m02((m12m01 - m11m02) * s)._m03(0f)
            ._m10((m12m20 - m10m22) * s)._m11((m22m00 - m20m02) * s)._m12((m10m02 - m12m00) * s)._m13(0f)
            ._m20((m10m21 - m11m20) * s)._m21((m20m01 - m21m00) * s)._m22((m11m00 - m10m01) * s)._m23(0f)
            ._m30((m10m22 * m31 - m10m21 * m32 + m11m20 * m32 - m11m22 * m30 + m12m21 * m30 - m12m20 * m31) * s)
            ._m31(nm31)._m32(nm32)._m33(1f)._properties(2)
    }

    @JvmOverloads
    fun transpose(dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (this !== dst) transposeNonThisGeneric(dst) else transposeThisGeneric(dst)
        }
    }

    private fun transposeNonThisGeneric(dst: Matrix4f): Matrix4f {
        return dst._m00(m00)._m01(m10)._m02(m20)._m03(m30)._m10(m01)._m11(m11)._m12(m21)._m13(m31)._m20(m02)._m21(m12)
            ._m22(
                m22
            )._m23(m32)._m30(m03)._m31(m13)._m32(m23)._m33(m33)._properties(0)
    }

    private fun transposeThisGeneric(dst: Matrix4f): Matrix4f {
        val nm10 = m01
        val nm20 = m02
        val nm21 = m12
        val nm30 = m03
        val nm31 = m13
        val nm32 = m23
        return dst._m01(m10)._m02(m20)._m03(m30)._m10(nm10)._m12(m21)._m13(m31)._m20(nm20)._m21(nm21)._m23(m32)
            ._m30(nm30)._m31(nm31)._m32(nm32)._properties(0)
    }

    @JvmOverloads
    fun transpose3x3(dst: Matrix4f = this): Matrix4f {
        val nm10 = m01
        val nm20 = m02
        val nm21 = m12
        return dst._m00(m00)._m01(m10)._m02(m20)._m10(nm10)._m11(m11)._m12(m21)._m20(nm20)._m21(nm21)._m22(m22)
            ._properties(
                flags and 30
            )
    }

    fun transpose3x3(dst: Matrix3f): Matrix3f {
        return dst._m00(m00)._m01(m10)._m02(m20)._m10(m01)._m11(m11)._m12(m21)._m20(m02)._m21(m12)._m22(m22)
    }

    fun translation(x: Float, y: Float, z: Float): Matrix4f {
        identity()
        return _m30(x)._m31(y)._m32(z)._properties(26)
    }

    fun translation(offset: Vector3f): Matrix4f {
        return translation(offset.x, offset.y, offset.z)
    }

    fun setTranslation(x: Float, y: Float, z: Float): Matrix4f {
        return _m30(x)._m31(y)._m32(z)._properties(flags and -6)
    }

    fun setTranslation(xyz: Vector3f): Matrix4f {
        return setTranslation(xyz.x, xyz.y, xyz.z)
    }

    fun getTranslation(dst: Vector3f): Vector3f {
        dst.x = m30
        dst.y = m31
        dst.z = m32
        return dst
    }

    fun getTranslation(dst: Vector3d): Vector3d {
        dst.x = m30.toDouble()
        dst.y = m31.toDouble()
        dst.z = m32.toDouble()
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

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)} ${f(m30)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)} ${f(m31)}] " +
                "[${f(m02)} ${f(m12)} ${f(m22)} ${f(m32)}] " +
                "[${f(m03)} ${f(m13)} ${f(m23)} ${f(m33)}]]").addSigns()

    fun get(dst: Matrix4f): Matrix4f {
        return dst.set(this)
    }

    fun get4x3(dst: Matrix4x3f): Matrix4x3f {
        return dst.set(this)
    }

    fun get(dst: Matrix4d): Matrix4d {
        return dst.set(this)
    }

    fun get3x3(dst: Matrix3f): Matrix3f {
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
        arr[offset + 3] = m03
        arr[offset + 4] = m10
        arr[offset + 5] = m11
        arr[offset + 6] = m12
        arr[offset + 7] = m13
        arr[offset + 8] = m20
        arr[offset + 9] = m21
        arr[offset + 10] = m22
        arr[offset + 11] = m23
        arr[offset + 12] = m30
        arr[offset + 13] = m31
        arr[offset + 14] = m32
        arr[offset + 15] = m33
        return arr
    }

    fun set(arr: FloatArray, offset: Int = 0): Matrix4f {
        return set(
            arr[offset], arr[offset + 1], arr[offset + 2], arr[offset + 3],
            arr[offset + 4], arr[offset + 5], arr[offset + 6], arr[offset + 7],
            arr[offset + 8], arr[offset + 9], arr[offset + 10], arr[offset + 11],
            arr[offset + 12], arr[offset + 13], arr[offset + 14], arr[offset + 15],
        )
    }

    fun zero(): Matrix4f {
        m00 = 0f
        m01 = 0f
        m02 = 0f
        m03 = 0f
        m10 = 0f
        m11 = 0f
        m12 = 0f
        m13 = 0f
        m20 = 0f
        m21 = 0f
        m22 = 0f
        m23 = 0f
        m30 = 0f
        m31 = 0f
        m32 = 0f
        m33 = 0f
        return _properties(0)
    }

    fun scaling(factor: Float): Matrix4f {
        return scaling(factor, factor, factor)
    }

    fun scaling(x: Float, y: Float, z: Float): Matrix4f {
        identity()
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        return _m00(x)._m11(y)._m22(z)._properties(2 or if (one) 16 else 0)
    }

    fun scaling(xyz: Vector3f): Matrix4f {
        return scaling(xyz.x, xyz.y, xyz.z)
    }

    fun rotation(angle: Float, axis: Vector3f): Matrix4f {
        return rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(axisAngle: AxisAngle4f): Matrix4f {
        return rotation(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotation(angle: Float, x: Float, y: Float, z: Float): Matrix4f {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotationX(x * angle)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotationY(y * angle)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotationZ(z * angle)
            else rotationInternal(angle, x, y, z)
        }
    }

    private fun rotationInternal(angle: Float, x: Float, y: Float, z: Float): Matrix4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1f - cos
        val xy = x * y
        val xz = x * z
        val yz = y * z
        identity()
        return _m00(cos + x * x * C)._m10(xy * C - z * sin)._m20(xz * C + y * sin)._m01(xy * C + z * sin)
            ._m11(cos + y * y * C)._m21(yz * C - x * sin)._m02(xz * C - y * sin)._m12(yz * C + x * sin)
            ._m22(cos + z * z * C)._properties(18)
    }

    fun rotationX(ang: Float): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        identity()
        _m11(cos)._m12(sin)._m21(-sin)._m22(cos)._properties(18)
        return this
    }

    fun rotationY(ang: Float): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        identity()
        _m00(cos)._m02(-sin)._m20(sin)._m22(cos)._properties(18)
        return this
    }

    fun rotationZ(ang: Float): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        identity()
        return _m00(cos)._m01(sin)._m10(-sin)._m11(cos)._properties(18)
    }

    fun rotationTowardsXY(dirX: Float, dirY: Float): Matrix4f {
        identity()
        return _m00(dirY)._m01(dirX)._m10(-dirX)._m11(dirY)._properties(18)
    }

    fun rotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        identity()
        val nm01 = -sinX * -sinY
        val nm02 = cosX * -sinY
        return _m20(sinY)._m21(-sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)
            ._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * -sinZ)._m11(nm01 * -sinZ + cosX * cosZ)
            ._m12(nm02 * -sinZ + sinX * cosZ)._properties(18)
    }

    fun rotationZYX(angleZ: Float, angleY: Float, angleX: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val nm20 = cosZ * sinY
        val nm21 = sinZ * sinY
        return _m00(cosZ * cosY)._m01(sinZ * cosY)._m02(-sinY)._m03(0f)._m10(-sinZ * cosX + nm20 * sinX)
            ._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m13(0f)._m20(-sinZ * -sinX + nm20 * cosX)
            ._m21(cosZ * -sinX + nm21 * cosX)._m22(cosY * cosX)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)
            ._properties(18)
    }

    fun rotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val nm10 = sinY * sinX
        val nm12 = cosY * sinX
        return _m20(sinY * cosX)._m21(-sinX)._m22(cosY * cosX)._m23(0f)._m00(cosY * cosZ + nm10 * sinZ)
            ._m01(cosX * sinZ)._m02(-sinY * cosZ + nm12 * sinZ)._m03(0f)._m10(cosY * -sinZ + nm10 * cosZ)
            ._m11(cosX * cosZ)._m12(-sinY * -sinZ + nm12 * cosZ)._m13(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)
            ._properties(18)
    }

    fun setRotationXYZ(angleX: Float, angleY: Float, angleZ: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val nm01 = -sinX * -sinY
        val nm02 = cosX * -sinY
        return _m20(sinY)._m21(-sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)
            ._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * -sinZ)._m11(nm01 * -sinZ + cosX * cosZ)
            ._m12(nm02 * -sinZ + sinX * cosZ)._properties(
                flags and -14
            )
    }

    fun setRotationZYX(angleZ: Float, angleY: Float, angleX: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val nm20 = cosZ * sinY
        val nm21 = sinZ * sinY
        return _m00(cosZ * cosY)._m01(sinZ * cosY)._m02(-sinY)._m10(-sinZ * cosX + nm20 * sinX)
            ._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(-sinZ * -sinX + nm20 * cosX)
            ._m21(cosZ * -sinX + nm21 * cosX)._m22(cosY * cosX)._properties(
                flags and -14
            )
    }

    fun setRotationYXZ(angleY: Float, angleX: Float, angleZ: Float): Matrix4f {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val nm10 = sinY * sinX
        val nm12 = cosY * sinX
        return _m20(sinY * cosX)._m21(-sinX)._m22(cosY * cosX)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)
            ._m02(-sinY * cosZ + nm12 * sinZ)._m10(cosY * -sinZ + nm10 * cosZ)._m11(cosX * cosZ)
            ._m12(-sinY * -sinZ + nm12 * cosZ)._properties(
                flags and -14
            )
    }

    fun rotation(quat: Quaternionf): Matrix4f {
        return rotationQ(quat.x, quat.y, quat.z, quat.w)
    }

    fun rotationQ(qx: Float, qy: Float, qz: Float, qw: Float): Matrix4f {
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
        identity()
        return _m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)
            ._m12(dyz + dxw)._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18)
    }

    fun translationRotateScale(
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float
    ): Matrix4f {
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
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return _m00(sx - (q11 + q22) * sx)._m01((q01 + q23) * sx)._m02((q02 - q13) * sx)._m03(0f)._m10((q01 - q23) * sy)
            ._m11(sy - (q22 + q00) * sy)._m12((q12 + q03) * sy)._m13(0f)._m20((q02 + q13) * sz)._m21((q12 - q03) * sz)
            ._m22(sz - (q11 + q00) * sz)._m23(0f)._m30(tx)._m31(ty)._m32(tz)._m33(1f)
            ._properties(2 or if (one) 16 else 0)
    }

    fun translationRotateScale(translation: Vector3f, quat: Quaternionf, scale: Vector3f): Matrix4f {
        return translationRotateScale(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale.x, scale.y, scale.z
        )
    }

    fun translationRotateScale(
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        scale: Float
    ): Matrix4f {
        return translationRotateScale(tx, ty, tz, qx, qy, qz, qw, scale, scale, scale)
    }

    fun translationRotateScale(translation: Vector3f, quat: Quaternionf, scale: Float): Matrix4f {
        return translationRotateScale(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale, scale, scale
        )
    }

    fun translationRotateScaleInvert(
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float
    ): Matrix4f {
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return if (one) {
            this.translationRotateInvert(tx, ty, tz, qx, qy, qz, qw)
        } else {
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
            val isx = 1f / sx
            val isy = 1f / sy
            val isz = 1f / sz
            _m00(isx * (1f - q11 - q22))._m01(isy * (q01 + q23))._m02(isz * (q02 - q13))._m03(0f)
                ._m10(isx * (q01 - q23))._m11(isy * (1f - q22 - q00))._m12(isz * (q12 + q03))._m13(0f)
                ._m20(isx * (q02 + q13))._m21(isy * (q12 - q03))._m22(isz * (1f - q11 - q00))._m23(0f)
                ._m30(-m00 * tx - m10 * ty - m20 * tz)._m31(-m01 * tx - m11 * ty - m21 * tz)
                ._m32(-m02 * tx - m12 * ty - m22 * tz)._m33(1f)._properties(2)
        }
    }

    fun translationRotateScaleInvert(translation: Vector3f, quat: Quaternionf, scale: Vector3f): Matrix4f {
        return translationRotateScaleInvert(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale.x, scale.y, scale.z
        )
    }

    fun translationRotateScaleInvert(translation: Vector3f, quat: Quaternionf, scale: Float): Matrix4f {
        return translationRotateScaleInvert(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale, scale, scale
        )
    }

    fun translationRotateScaleMulAffine(
        tx: Float, ty: Float, tz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float, m: Matrix4f
    ): Matrix4f {
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
        val m00 = nm00 * m.m00 + nm10 * m.m01 + nm20 * m.m02
        val m01 = nm01 * m.m00 + nm11 * m.m01 + nm21 * m.m02
        _m02(nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02)._m00(m00)._m01(m01)._m03(0f)
        val m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12
        val m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12
        _m12(nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12)._m10(m10)._m11(m11)._m13(0f)
        val m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22
        val m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22
        _m22(nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22)._m20(m20)._m21(m21)._m23(0f)
        val m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx
        val m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty
        _m32(nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz)._m30(m30)._m31(m31)._m33(1f)
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return _properties(2 or if (one && m.flags and 16 != 0) 16 else 0)
    }

    fun translationRotateScaleMulAffine(
        translation: Vector3f,
        quat: Quaternionf,
        scale: Vector3f,
        m: Matrix4f
    ): Matrix4f {
        return translationRotateScaleMulAffine(
            translation.x, translation.y, translation.z,
            quat.x, quat.y, quat.z, quat.w,
            scale.x, scale.y, scale.z, m
        )
    }

    fun translationRotate(tx: Float, ty: Float, tz: Float, qx: Float, qy: Float, qz: Float, qw: Float): Matrix4f {
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
        return _m00(w2 + x2 - z2 - y2)._m01(xy + zw + zw + xy)._m02(xz - yw + xz - yw)._m10(-zw + xy - zw + xy)
            ._m11(y2 - z2 + w2 - x2)._m12(yz + yz + xw + xw)._m20(yw + xz + xz + yw)._m21(
                yz + yz - xw - xw
            )._m22(z2 - y2 - x2 + w2)._m30(tx)._m31(ty)._m32(tz)._m33(1f)._properties(18)
    }

    fun translationRotate(tx: Float, ty: Float, tz: Float, quat: Quaternionf): Matrix4f {
        return translationRotate(tx, ty, tz, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotate(translation: Vector3f, quat: Quaternionf): Matrix4f {
        return translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotateInvert(tx: Float, ty: Float, tz: Float, qx: Float, qy: Float, qz: Float, qw: Float): Matrix4f {
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
        return _m00(1f - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m03(0f)._m10(q01 - q23)._m11(1f - q22 - q00)
            ._m12(q12 + q03)._m13(0f)._m20(q02 + q13)._m21(q12 - q03)._m22(1f - q11 - q00)._m23(0f)._m30(
                -m00 * tx - m10 * ty - m20 * tz
            )._m31(-m01 * tx - m11 * ty - m21 * tz)._m32(-m02 * tx - m12 * ty - m22 * tz)._m33(1f)._properties(18)
    }

    fun translationRotateInvert(translation: Vector3f, quat: Quaternionf): Matrix4f {
        return translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun set3x3(mat: Matrix3f): Matrix4f {
        return set3x3Matrix3f(mat)._properties(flags and -30)
    }

    private fun set3x3Matrix3f(mat: Matrix3f): Matrix4f {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)
            ._m21(mat.m21)._m22(mat.m22)
    }

    fun transform(v: Vector4f): Vector4f {
        return v.mul(this)
    }

    fun transform(v: Vector4f, dst: Vector4f): Vector4f {
        return v.mul(this, dst)
    }

    fun transform(x: Float, y: Float, z: Float, w: Float, dst: Vector4f): Vector4f {
        return dst.set(x, y, z, w).mul(this)
    }

    fun transformTranspose(v: Vector4f): Vector4f {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector4f, dst: Vector4f): Vector4f {
        return v.mulTranspose(this, dst)
    }

    fun transformTranspose(x: Float, y: Float, z: Float, w: Float, dst: Vector4f): Vector4f {
        return dst.set(x, y, z, w).mulTranspose(this)
    }

    fun transformProject(v: Vector4f): Vector4f {
        return v.mulProject(this)
    }

    fun transformProject(v: Vector4f, dst: Vector4f): Vector4f {
        return v.mulProject(this, dst)
    }

    fun transformProject(x: Float, y: Float, z: Float, w: Float, dst: Vector4f): Vector4f {
        return dst.set(x, y, z, w).mulProject(this)
    }

    fun transformProject(v: Vector4f, dst: Vector3f): Vector3f {
        return v.mulProject(this, dst)
    }

    fun transformProject(x: Float, y: Float, z: Float, w: Float, dst: Vector3f): Vector3f {
        return dst.set(x, y, z).mulProject(this, w, dst)
    }

    fun transformProject(v: Vector3f): Vector3f {
        return v.mulProject(this)
    }

    fun transformProject(v: Vector3f, dst: Vector3f): Vector3f {
        return v.mulProject(this, dst)
    }

    fun transformProject(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        return dst.set(x, y, z).mulProject(this)
    }

    fun transformPosition(v: Vector3f): Vector3f {
        return v.mulPosition(this)
    }

    fun transformPosition(v: Vector3f, dst: Vector3f): Vector3f {
        return transformPosition(v.x, v.y, v.z, dst)
    }

    fun transformPosition(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        return dst.set(x, y, z).mulPosition(this)
    }

    fun transformDirection(v: Vector3f): Vector3f {
        return transformDirection(v.x, v.y, v.z, v)
    }

    fun transformDirection(v: Vector3f, dst: Vector3f): Vector3f {
        return transformDirection(v.x, v.y, v.z, dst)
    }

    fun transformDirection(x: Float, y: Float, z: Float, dst: Vector3f): Vector3f {
        return dst.set(x, y, z).mulDirection(this)
    }

    fun transformAffine(v: Vector4f): Vector4f {
        return v.mulAffine(this, v)
    }

    fun transformAffine(v: Vector4f, dst: Vector4f): Vector4f {
        return transformAffine(v.x, v.y, v.z, v.w, dst)
    }

    fun transformAffine(x: Float, y: Float, z: Float, w: Float, dst: Vector4f): Vector4f {
        return dst.set(x, y, z, w).mulAffine(this, dst)
    }

    fun scale(xyz: Vector3f, dst: Matrix4f): Matrix4f {
        return scale(xyz.x, xyz.y, xyz.z, dst)
    }

    fun scale(xyz: Vector3f): Matrix4f {
        return scale(xyz.x, xyz.y, xyz.z, this)
    }

    fun scale(xyz: Float, dst: Matrix4f): Matrix4f {
        return scale(xyz, xyz, xyz, dst)
    }

    fun scale(xyz: Float): Matrix4f {
        return scale(xyz, xyz, xyz)
    }

    fun scaleXY(x: Float, y: Float, dst: Matrix4f): Matrix4f {
        return scale(x, y, 1f, dst)
    }

    fun scaleXY(x: Float, y: Float): Matrix4f {
        return scale(x, y, 1f)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.scaling(x, y, z) else scaleGeneric(x, y, z, dst)
    }

    private fun scaleGeneric(x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        return dst._m00(m00 * x)._m01(m01 * x)._m02(m02 * x)._m03(m03 * x)
            ._m10(m10 * y)._m11(m11 * y)._m12(m12 * y)._m13(m13 * y)
            ._m20(m20 * z)._m21(m21 * z)._m22(m22 * z)._m23(m23 * z)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(flags and (13 or if (one) 0 else 16).inv())
    }

    @JvmOverloads
    fun scaleAround(sx: Float, sy: Float, sz: Float, ox: Float, oy: Float, oz: Float, dst: Matrix4f = this): Matrix4f {
        val nm30 = m00 * ox + m10 * oy + m20 * oz + m30
        val nm31 = m01 * ox + m11 * oy + m21 * oz + m31
        val nm32 = m02 * ox + m12 * oy + m22 * oz + m32
        val nm33 = m03 * ox + m13 * oy + m23 * oz + m33
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return dst._m00(m00 * sx)._m01(m01 * sx)._m02(m02 * sx)._m03(m03 * sx)
            ._m10(m10 * sy)._m11(m11 * sy)._m12(m12 * sy)._m13(m13 * sy)
            ._m20(m20 * sz)._m21(m21 * sz)._m22(m22 * sz)._m23(m23 * sz)
            ._m30(-dst.m00 * ox - dst.m10 * oy - dst.m20 * oz + nm30)
            ._m31(-dst.m01 * ox - dst.m11 * oy - dst.m21 * oz + nm31)
            ._m32(-dst.m02 * ox - dst.m12 * oy - dst.m22 * oz + nm32)
            ._m33(-dst.m03 * ox - dst.m13 * oy - dst.m23 * oz + nm33)
            ._properties(flags and (13 or if (one) 0 else 16).inv())
    }

    fun scaleAround(factor: Float, ox: Float, oy: Float, oz: Float): Matrix4f {
        return scaleAround(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAround(factor: Float, ox: Float, oy: Float, oz: Float, dst: Matrix4f): Matrix4f {
        return scaleAround(factor, factor, factor, ox, oy, oz, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.scaling(x, y, z) else scaleLocalGeneric(x, y, z, dst)
    }

    private fun scaleLocalGeneric(x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(flags and (13 or if (one) 0 else 16).inv())
    }

    @JvmOverloads
    fun scaleLocal(xyz: Float, dst: Matrix4f = this): Matrix4f {
        return scaleLocal(xyz, xyz, xyz, dst)
    }

    @JvmOverloads
    fun scaleAroundLocal(
        sx: Float,
        sy: Float,
        sz: Float,
        ox: Float,
        oy: Float,
        oz: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return dst._m00(sx * (m00 - ox * m03) + ox * m03)
            ._m01(sy * (m01 - oy * m03) + oy * m03)
            ._m02(sz * (m02 - oz * m03) + oz * m03)._m03(m03)
            ._m10(sx * (m10 - ox * m13) + ox * m13)
            ._m11(sy * (m11 - oy * m13) + oy * m13)
            ._m12(sz * (m12 - oz * m13) + oz * m13)._m13(m13)
            ._m20(sx * (m20 - ox * m23) + ox * m23)
            ._m21(sy * (m21 - oy * m23) + oy * m23)
            ._m22(sz * (m22 - oz * m23) + oz * m23)._m23(m23)
            ._m30(sx * (m30 - ox * m33) + ox * m33)
            ._m31(sy * (m31 - oy * m33) + oy * m33)
            ._m32(sz * (m32 - oz * m33) + oz * m33)._m33(m33)
            ._properties(flags and (13 or if (one) 0 else 16).inv())
    }

    fun scaleAroundLocal(factor: Float, ox: Float, oy: Float, oz: Float): Matrix4f {
        return scaleAroundLocal(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAroundLocal(factor: Float, ox: Float, oy: Float, oz: Float, dst: Matrix4f): Matrix4f {
        return scaleAroundLocal(factor, factor, factor, ox, oy, oz, dst)
    }

    @JvmOverloads
    fun rotateX(ang: Float, dst: Matrix4f = this): Matrix4f {
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

    private fun rotateXInternal(ang: Float, dst: Matrix4f): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val lm10 = m10
        val lm11 = m11
        val lm12 = m12
        val lm13 = m13
        val lm20 = m20
        val lm21 = m21
        val lm22 = m22
        val lm23 = m23
        return dst._m20(lm10 * -sin + lm20 * cos)._m21(lm11 * -sin + lm21 * cos)
            ._m22(lm12 * -sin + lm22 * cos)._m23(lm13 * -sin + lm23 * cos)
            ._m10(lm10 * cos + lm20 * sin)._m11(lm11 * cos + lm21 * sin)
            ._m12(lm12 * cos + lm22 * sin)._m13(lm13 * cos + lm23 * sin)
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateY(ang: Float, dst: Matrix4f = this): Matrix4f {
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

    private fun rotateYInternal(ang: Float, dst: Matrix4f): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = m00 * cos + m20 * -sin
        val nm01 = m01 * cos + m21 * -sin
        val nm02 = m02 * cos + m22 * -sin
        val nm03 = m03 * cos + m23 * -sin
        return dst._m20(m00 * sin + m20 * cos)._m21(m01 * sin + m21 * cos)
            ._m22(m02 * sin + m22 * cos)._m23(m03 * sin + m23 * cos)
            ._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateZ(ang: Float, dst: Matrix4f = this): Matrix4f {
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

    private fun rotateZInternal(ang: Float, dst: Matrix4f): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        return rotateTowardsXY(sin, cos, dst)
    }

    @JvmOverloads
    fun rotateTowardsXY(dirX: Float, dirY: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationTowardsXY(dirX, dirY)
        } else {
            val nm00 = m00 * dirY + m10 * dirX
            val nm01 = m01 * dirY + m11 * dirX
            val nm02 = m02 * dirY + m12 * dirX
            val nm03 = m03 * dirY + m13 * dirX
            dst._m10(m00 * -dirX + m10 * dirY)
                ._m11(m01 * -dirX + m11 * dirY)
                ._m12(m02 * -dirX + m12 * dirY)
                ._m13(m03 * -dirX + m13 * dirY)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
                ._m20(m20)._m21(m21)._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)
                ._m33(m33)._properties(flags and -14)
        }
    }

    fun rotateXYZ(angles: Vector3f): Matrix4f {
        return rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationXYZ(angleX, angleY, angleZ)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz)
        } else {
            if (flags and 2 != 0) dst.rotateAffineXYZ(angleX, angleY, angleZ)
            else rotateXYZInternal(angleX, angleY, angleZ, dst)
        }
    }

    private fun rotateXYZInternal(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4f): Matrix4f {
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
        val nm13 = m13 * cosX + m23 * sinX
        val nm20 = m10 * m_sinX + m20 * cosX
        val nm21 = m11 * m_sinX + m21 * cosX
        val nm22 = m12 * m_sinX + m22 * cosX
        val nm23 = m13 * m_sinX + m23 * cosX
        val nm00 = m00 * cosY + nm20 * m_sinY
        val nm01 = m01 * cosY + nm21 * m_sinY
        val nm02 = m02 * cosY + nm22 * m_sinY
        val nm03 = m03 * cosY + nm23 * m_sinY
        return dst._m20(m00 * sinY + nm20 * cosY)._m21(m01 * sinY + nm21 * cosY)
            ._m22(m02 * sinY + nm22 * cosY)._m23(m03 * sinY + nm23 * cosY)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)
            ._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAffineXYZ(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationXYZ(angleX, angleY, angleZ)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz)
        } else {
            rotateAffineXYZInternal(angleX, angleY, angleZ, dst)
        }
    }

    private fun rotateAffineXYZInternal(angleX: Float, angleY: Float, angleZ: Float, dst: Matrix4f): Matrix4f {
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
        return dst._m20(m00 * sinY + nm20 * cosY)._m21(m01 * sinY + nm21 * cosY)
            ._m22(m02 * sinY + nm22 * cosY)._m23(0f)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)
            ._m02(nm02 * cosZ + nm12 * sinZ)._m03(0f)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0f)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(flags and -14)
    }

    fun rotateZYX(angles: Vector3f): Matrix4f {
        return rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationZYX(angleZ, angleY, angleX)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz)
        } else {
            if (flags and 2 != 0) dst.rotateAffineZYX(angleZ, angleY, angleX)
            else rotateZYXInternal(angleZ, angleY, angleX, dst)
        }
    }

    private fun rotateZYXInternal(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix4f): Matrix4f {
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
        val nm03 = m03 * cosZ + m13 * sinZ
        val nm10 = m00 * m_sinZ + m10 * cosZ
        val nm11 = m01 * m_sinZ + m11 * cosZ
        val nm12 = m02 * m_sinZ + m12 * cosZ
        val nm13 = m03 * m_sinZ + m13 * cosZ
        val nm20 = nm00 * sinY + m20 * cosY
        val nm21 = nm01 * sinY + m21 * cosY
        val nm22 = nm02 * sinY + m22 * cosY
        val nm23 = nm03 * sinY + m23 * cosY
        return dst._m00(nm00 * cosY + m20 * m_sinY)._m01(nm01 * cosY + m21 * m_sinY)
            ._m02(nm02 * cosY + m22 * m_sinY)._m03(nm03 * cosY + m23 * m_sinY)
            ._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)
            ._m12(nm12 * cosX + nm22 * sinX)._m13(nm13 * cosX + nm23 * sinX)
            ._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)
            ._m22(nm12 * m_sinX + nm22 * cosX)._m23(nm13 * m_sinX + nm23 * cosX)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAffineZYX(angleZ: Float, angleY: Float, angleX: Float, dst: Matrix4f = this): Matrix4f {
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
        return dst._m00(nm00 * cosY + m20 * m_sinY)._m01(nm01 * cosY + m21 * m_sinY)
            ._m02(nm02 * cosY + m22 * m_sinY)._m03(0f)._m10(nm10 * cosX + nm20 * sinX)
            ._m11(nm11 * cosX + nm21 * sinX)._m12(nm12 * cosX + nm22 * sinX)
            ._m13(0f)._m20(nm10 * m_sinX + nm20 * cosX)
            ._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(0f)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    fun rotateYXZ(angles: Vector3f): Matrix4f {
        return rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationYXZ(angleY, angleX, angleZ)
        } else if (flags and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dst.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz)
        } else {
            if (flags and 2 != 0) dst.rotateAffineYXZ(angleY, angleX, angleZ)
            else rotateYXZInternal(angleY, angleX, angleZ, dst)
        }
    }

    private fun rotateYXZInternal(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix4f): Matrix4f {
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
        val nm23 = m03 * sinY + m23 * cosY
        val nm00 = m00 * cosY + m20 * m_sinY
        val nm01 = m01 * cosY + m21 * m_sinY
        val nm02 = m02 * cosY + m22 * m_sinY
        val nm03 = m03 * cosY + m23 * m_sinY
        val nm10 = m10 * cosX + nm20 * sinX
        val nm11 = m11 * cosX + nm21 * sinX
        val nm12 = m12 * cosX + nm22 * sinX
        val nm13 = m13 * cosX + nm23 * sinX
        return dst._m20(m10 * m_sinX + nm20 * cosX)._m21(m11 * m_sinX + nm21 * cosX)._m22(m12 * m_sinX + nm22 * cosX)
            ._m23(
                m13 * m_sinX + nm23 * cosX
            )._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)
            ._m03(nm03 * cosZ + nm13 * sinZ)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAffineYXZ(angleY: Float, angleX: Float, angleZ: Float, dst: Matrix4f = this): Matrix4f {
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
        return dst._m20(m10 * m_sinX + nm20 * cosX)._m21(m11 * m_sinX + nm21 * cosX)._m22(m12 * m_sinX + nm22 * cosX)
            ._m23(0f)._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)
            ._m03(0f)._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0f)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotate(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotation(ang, x, y, z)
        } else if (flags and 8 != 0) {
            this.rotateTranslation(ang, x, y, z, dst)
        } else {
            if (flags and 2 != 0) this.rotateAffine(ang, x, y, z, dst)
            else this.rotateGeneric(ang, x, y, z, dst)
        }
    }

    private fun rotateGeneric(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dst)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dst)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotateZ(z * ang, dst)
            else rotateGenericInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateGenericInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    fun rotateTranslation(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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

    private fun rotateTranslationInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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
        return dst._m20(rm20)._m21(rm21)._m22(rm22)._m23(0f)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0f)._m10(rm10)
            ._m11(rm11)._m12(rm12)._m13(0f)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(1f)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAffine(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dst)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dst)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotateZ(
                z * ang,
                dst
            ) else rotateAffineInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateAffineInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m30(m30)._m31(
            m31
        )._m32(m32)._m33(1f)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateLocal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.rotation(ang, x, y, z) else rotateLocalGeneric(ang, x, y, z, dst)
    }

    private fun rotateLocalGeneric(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        return if (y == 0f && z == 0f && JomlMath.absEqualsOne(x)) {
            rotateLocalX(x * ang, dst)
        } else if (x == 0f && z == 0f && JomlMath.absEqualsOne(y)) {
            rotateLocalY(y * ang, dst)
        } else {
            if (x == 0f && y == 0f && JomlMath.absEqualsOne(z)) rotateLocalZ(
                z * ang,
                dst
            ) else rotateLocalGenericInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateLocalGenericInternal(ang: Float, x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
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
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateLocalX(ang: Float, dst: Matrix4f = this): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm02 = sin * m01 + cos * m02
        val nm12 = sin * m11 + cos * m12
        val nm22 = sin * m21 + cos * m22
        val nm32 = sin * m31 + cos * m32
        return dst._m00(m00)._m01(cos * m01 - sin * m02)._m02(nm02)._m03(m03)._m10(m10)._m11(cos * m11 - sin * m12)
            ._m12(nm12)._m13(
                m13
            )._m20(m20)._m21(cos * m21 - sin * m22)._m22(nm22)._m23(m23)._m30(m30)._m31(cos * m31 - sin * m32)
            ._m32(nm32)
            ._m33(
                m33
            )._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateLocalY(ang: Float, dst: Matrix4f = this): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm02 = -sin * m00 + cos * m02
        val nm12 = -sin * m10 + cos * m12
        val nm22 = -sin * m20 + cos * m22
        val nm32 = -sin * m30 + cos * m32
        return dst._m00(cos * m00 + sin * m02)._m01(m01)._m02(nm02)._m03(m03)._m10(cos * m10 + sin * m12)._m11(m11)
            ._m12(nm12)._m13(
                m13
            )._m20(cos * m20 + sin * m22)._m21(m21)._m22(nm22)._m23(m23)._m30(cos * m30 + sin * m32)._m31(m31)
            ._m32(nm32)
            ._m33(
                m33
            )._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Float, dst: Matrix4f = this): Matrix4f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm01 = sin * m00 + cos * m01
        val nm11 = sin * m10 + cos * m11
        val nm21 = sin * m20 + cos * m21
        val nm31 = sin * m30 + cos * m31
        return dst._m00(cos * m00 - sin * m01)._m01(nm01)._m02(m02)._m03(m03)._m10(cos * m10 - sin * m11)._m11(nm11)
            ._m12(
                m12
            )._m13(m13)._m20(cos * m20 - sin * m21)._m21(nm21)._m22(m22)._m23(m23)._m30(cos * m30 - sin * m31)
            ._m31(nm31)._m32(
                m32
            )._m33(m33)._properties(flags and -14)
    }

    fun translate(offset: Vector3f): Matrix4f {
        return translate(offset.x, offset.y, offset.z)
    }

    fun translate(offset: Vector3f, dst: Matrix4f): Matrix4f {
        return translate(offset.x, offset.y, offset.z, dst)
    }

    fun translate(x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        return if (flags and 4 != 0) dst.translation(x, y, z) else this.translateGeneric(x, y, z, dst)
    }

    private fun translateGeneric(x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        dst.set(this)
        return dst._m30(m00 * x + m10 * y + m20 * z + m30)
            ._m31(m01 * x + m11 * y + m21 * z + m31)
            ._m32(m02 * x + m12 * y + m22 * z + m32)
            ._m33(m03 * x + m13 * y + m23 * z + m33)
            ._properties(flags and -6)
    }

    fun translate(x: Float, y: Float, z: Float): Matrix4f {
        return if (flags and 4 != 0) this.translation(x, y, z) else this.translateGeneric(x, y, z)
    }

    private fun translateGeneric(x: Float, y: Float, z: Float): Matrix4f {
        return _m30(m00 * x + m10 * y + m20 * z + m30)
            ._m31(m01 * x + m11 * y + m21 * z + m31)
            ._m32(m02 * x + m12 * y + m22 * z + m32)
            ._m33(m03 * x + m13 * y + m23 * z + m33)
            ._properties(flags and -6)
    }

    fun translateLocal(offset: Vector3f): Matrix4f {
        return translateLocal(offset.x, offset.y, offset.z)
    }

    fun translateLocal(offset: Vector3f, dst: Matrix4f): Matrix4f {
        return translateLocal(offset.x, offset.y, offset.z, dst)
    }

    @JvmOverloads
    fun translateLocal(x: Float, y: Float, z: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.translation(x, y, z) else translateLocalGeneric(x, y, z, dst)
    }

    private fun translateLocalGeneric(x: Float, y: Float, z: Float, dst: Matrix4f): Matrix4f {
        val nm00 = m00 + x * m03
        val nm01 = m01 + y * m03
        val nm02 = m02 + z * m03
        val nm10 = m10 + x * m13
        val nm11 = m11 + y * m13
        val nm12 = m12 + z * m13
        val nm20 = m20 + x * m23
        val nm21 = m21 + y * m23
        val nm22 = m22 + z * m23
        val nm30 = m30 + x * m33
        val nm31 = m31 + y * m33
        val nm32 = m32 + z * m33
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(m23)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)
            ._properties(flags and -6)
    }

    @JvmOverloads
    fun ortho(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setOrtho(left, right, bottom, top, zNear, zFar, zZeroToOne)
        else orthoGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dst)
    }

    private fun orthoGeneric(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)
            ._m03(m03 * rm00)._m10(
                m10 * rm11
            )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20 * rm22)._m21(m21 * rm22)._m22(m22 * rm22)
            ._m23(
                m23 * rm22
            )._properties(flags and -30)
        return dst
    }

    fun ortho(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f
    ): Matrix4f {
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
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setOrthoLH(left, right, bottom, top, zNear, zFar, zZeroToOne)
        else orthoLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dst)
    }

    private fun orthoLHGeneric(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)
            ._m03(m03 * rm00)._m10(
                m10 * rm11
            )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20 * rm22)._m21(m21 * rm22)._m22(m22 * rm22)
            ._m23(
                m23 * rm22
            )._properties(flags and -30)
        return dst
    }

    fun orthoLH(
        left: Float, right: Float,
        bottom: Float, top: Float,
        zNear: Float, zFar: Float,
        dst: Matrix4f
    ): Matrix4f {
        return orthoLH(left, right, bottom, top, zNear, zFar, false, dst)
    }

    fun setOrtho(
        left: Float, right: Float,
        bottom: Float, top: Float,
        zNear: Float, zFar: Float,
        zZeroToOne: Boolean
    ): Matrix4f {
        identity()
        _m00(2f / (right - left))._m11(2f / (top - bottom))._m22((if (zZeroToOne) 1f else 2f) / (zNear - zFar))
            ._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar))._properties(2)
        return this
    }

    fun setOrtho(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4f {
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
    ): Matrix4f {
        identity()
        _m00(2f / (right - left))._m11(2f / (top - bottom))._m22((if (zZeroToOne) 1f else 2f) / (zFar - zNear))
            ._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar))._properties(2)
        return this
    }

    fun setOrthoLH(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4f {
        return setOrthoLH(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun orthoSymmetric(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setOrthoSymmetric(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoSymmetricGeneric(width, height, zNear, zFar, zZeroToOne, dst)
    }

    private fun orthoSymmetricGeneric(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = 2f / width
        val rm11 = 2f / height
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zNear - zFar)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst._m30(m20 * rm32 + m30)._m31(m21 * rm32 + m31)._m32(m22 * rm32 + m32)._m33(m23 * rm32 + m33)
            ._m00(m00 * rm00)._m01(
                m01 * rm00
            )._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)
            ._m20(
                m20 * rm22
            )._m21(m21 * rm22)._m22(m22 * rm22)._m23(m23 * rm22)._properties(flags and -30)
        return dst
    }

    fun orthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float, dst: Matrix4f): Matrix4f {
        return orthoSymmetric(width, height, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun orthoSymmetricLH(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean = false,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setOrthoSymmetricLH(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoSymmetricLHGeneric(width, height, zNear, zFar, zZeroToOne, dst)
    }

    private fun orthoSymmetricLHGeneric(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = 2f / width
        val rm11 = 2f / height
        val rm22 = (if (zZeroToOne) 1f else 2f) / (zFar - zNear)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dst._m30(m20 * rm32 + m30)._m31(m21 * rm32 + m31)._m32(m22 * rm32 + m32)._m33(m23 * rm32 + m33)
            ._m00(m00 * rm00)._m01(
                m01 * rm00
            )._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)
            ._m20(
                m20 * rm22
            )._m21(m21 * rm22)._m22(m22 * rm22)._m23(m23 * rm22)._properties(flags and -30)
        return dst
    }

    fun orthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float, dst: Matrix4f): Matrix4f {
        return orthoSymmetricLH(width, height, zNear, zFar, false, dst)
    }

    fun setOrthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4f {
        identity()
        _m00(2f / width)._m11(2f / height)._m22((if (zZeroToOne) 1f else 2f) / (zNear - zFar))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar))._properties(2)
        return this
    }

    fun setOrthoSymmetric(width: Float, height: Float, zNear: Float, zFar: Float): Matrix4f {
        return setOrthoSymmetric(width, height, zNear, zFar, false)
    }

    fun setOrthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4f {
        identity()
        _m00(2f / width)._m11(2f / height)._m22((if (zZeroToOne) 1f else 2f) / (zFar - zNear))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar))._properties(2)
        return this
    }

    fun setOrthoSymmetricLH(width: Float, height: Float, zNear: Float, zFar: Float): Matrix4f {
        return setOrthoSymmetricLH(width, height, zNear, zFar, false)
    }

    @JvmOverloads
    fun ortho2D(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.setOrtho2D(left, right, bottom, top) else ortho2DGeneric(
            left,
            right,
            bottom,
            top,
            dst
        )
    }

    private fun ortho2DGeneric(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4f): Matrix4f {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm30 = (right + left) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        dst._m30(m00 * rm30 + m10 * rm31 + m30)._m31(m01 * rm30 + m11 * rm31 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(
            m10 * rm11
        )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(-m20)._m21(-m21)._m22(-m22)._m23(-m23)._properties(
            flags and -30
        )
        return dst
    }

    @JvmOverloads
    fun ortho2DLH(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) dst.setOrtho2DLH(left, right, bottom, top) else ortho2DLHGeneric(
            left,
            right,
            bottom,
            top,
            dst
        )
    }

    private fun ortho2DLHGeneric(left: Float, right: Float, bottom: Float, top: Float, dst: Matrix4f): Matrix4f {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm30 = (right + left) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        dst._m30(m00 * rm30 + m10 * rm31 + m30)._m31(m01 * rm30 + m11 * rm31 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(
            m10 * rm11
        )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20)._m21(m21)._m22(m22)._m23(m23)._properties(
            flags and -30
        )
        return dst
    }

    fun setOrtho2D(left: Float, right: Float, bottom: Float, top: Float): Matrix4f {
        identity()
        _m00(2f / (right - left))._m11(2f / (top - bottom))._m22(-1f)._m30((right + left) / (left - right))
            ._m31((top + bottom) / (bottom - top))._properties(2)
        return this
    }

    fun setOrtho2DLH(left: Float, right: Float, bottom: Float, top: Float): Matrix4f {
        identity()
        _m00(2f / (right - left))._m11(2f / (top - bottom))._m30((right + left) / (left - right))
            ._m31((top + bottom) / (bottom - top))._properties(2)
        return this
    }

    fun lookAlong(dir: Vector3f, up: Vector3f): Matrix4f {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3f, up: Vector3f, dst: Matrix4f): Matrix4f {
        return lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ) else lookAlongGeneric(
            dirX,
            dirY,
            dirZ,
            upX,
            upY,
            upZ,
            dst
        )
    }

    private fun lookAlongGeneric(
        dirX: Float,
        dirY: Float,
        dirZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f
    ): Matrix4f {
        var dirX0 = dirX
        var dirY0 = dirY
        var dirZ0 = dirZ
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
        val nm03 = m03 * leftX + m13 * upnX + m23 * dirX0
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY0
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY0
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY0
        val nm13 = m03 * leftY + m13 * upnY + m23 * dirY0
        return dst._m20(m00 * leftZ + m10 * upnZ + m20 * dirZ0)._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ0)._m22(
            m02 * leftZ + m12 * upnZ + m22 * dirZ0
        )._m23(m03 * leftZ + m13 * upnZ + m23 * dirZ0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    fun setLookAlong(dir: Vector3f, up: Vector3f): Matrix4f {
        return setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix4f {
        var dirX0 = dirX
        var dirY0 = dirY
        var dirZ0 = dirZ
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
        _m00(leftX)._m01(dirY0 * leftZ - dirZ0 * leftY)._m02(dirX0)._m03(0f)._m10(leftY)
            ._m11(dirZ0 * leftX - dirX0 * leftZ)
            ._m12(dirY0)._m13(0f)._m20(leftZ)._m21(dirX0 * leftY - dirY0 * leftX)._m22(dirZ0)._m23(0f)._m30(0f)._m31(0f)
            ._m32(0f)._m33(1f)._properties(18)
        return this
    }

    fun setLookAt(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        return setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAt(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ): Matrix4f {
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
        return _m00(leftX)._m01(upnX)._m02(dirX)._m03(0f)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0f)._m20(leftZ)
            ._m21(upnZ)._m22(dirZ)._m23(0f)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))
            ._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1f)
            ._properties(18)
    }

    fun lookAt(eye: Vector3f, center: Vector3f, up: Vector3f, dst: Matrix4f): Matrix4f {
        return lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAt(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        return lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAt(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) {
            dst.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        } else {
            if (flags and 1 != 0)
                lookAtPerspective(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
            else lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
        }
    }

    private fun lookAtGeneric(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f
    ): Matrix4f {
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
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm03 = m03 * leftX + m13 * upnX + m23 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        val nm13 = m03 * leftY + m13 * upnY + m23 * dirY
        return dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(
                m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
            )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m20(
                m00 * leftZ + m10 * upnZ + m20 * dirZ
            )._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ)._m22(m02 * leftZ + m12 * upnZ + m22 * dirZ)._m23(
                m03 * leftZ + m13 * upnZ + m23 * dirZ
            )._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._properties(flags and -14)
    }

    fun lookAtPerspective(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f
    ): Matrix4f {
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
        val nm10 = m00 * leftY
        val nm20 = m00 * leftZ
        val nm21 = m11 * upnZ
        val nm30 = m00 * rm30
        val nm31 = m11 * rm31
        val nm32 = m22 * rm32 + m32
        val nm33 = m23 * rm32
        return dst._m00(m00 * leftX)._m01(m11 * upnX)._m02(m22 * dirX)._m03(m23 * dirX)._m10(nm10)._m11(m11 * upnY)
            ._m12(
                m22 * dirY
            )._m13(m23 * dirY)._m20(nm20)._m21(nm21)._m22(m22 * dirZ)._m23(m23 * dirZ)._m30(nm30)._m31(nm31)._m32(nm32)
            ._m33(nm33)._properties(0)
    }

    fun setLookAtLH(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        return setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAtLH(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ): Matrix4f {
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
        _m00(leftX)._m01(upnX)._m02(dirX)._m03(0f)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0f)._m20(leftZ)._m21(upnZ)
            ._m22(dirZ)._m23(0f)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))
            ._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1f)
            ._properties(18)
        return this
    }

    fun lookAtLH(eye: Vector3f, center: Vector3f, up: Vector3f, dst: Matrix4f): Matrix4f {
        return lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAtLH(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        return lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAtLH(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) {
            dst.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        } else {
            if (flags and 1 != 0) lookAtPerspectiveLH(
                eyeX,
                eyeY,
                eyeZ,
                centerX,
                centerY,
                centerZ,
                upX,
                upY,
                upZ,
                dst
            ) else lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
        }
    }

    private fun lookAtLHGeneric(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f
    ): Matrix4f {
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
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm03 = m03 * leftX + m13 * upnX + m23 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        val nm13 = m03 * leftY + m13 * upnY + m23 * dirY
        return dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(
                m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
            )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m20(
                m00 * leftZ + m10 * upnZ + m20 * dirZ
            )._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ)._m22(m02 * leftZ + m12 * upnZ + m22 * dirZ)._m23(
                m03 * leftZ + m13 * upnZ + m23 * dirZ
            )._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._properties(flags and -14)
    }

    fun lookAtPerspectiveLH(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float,
        dst: Matrix4f
    ): Matrix4f {
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
        val nm00 = m00 * leftX
        val nm01 = m11 * upnX
        val nm02 = m22 * dirX
        val nm03 = m23 * dirX
        val nm10 = m00 * leftY
        val nm11 = m11 * upnY
        val nm12 = m22 * dirY
        val nm13 = m23 * dirY
        val nm20 = m00 * leftZ
        val nm21 = m11 * upnZ
        val nm22 = m22 * dirZ
        val nm23 = m23 * dirZ
        val nm30 = m00 * rm30
        val nm31 = m11 * rm31
        val nm32 = m22 * rm32 + m32
        val nm33 = m23 * rm32
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun tile(x: Int, y: Int, w: Int, h: Int, dst: Matrix4f = this): Matrix4f {
        val tx = (w - 1 - (x shl 1)).toFloat()
        val ty = (h - 1 - (y shl 1)).toFloat()
        return dst._m30(m00 * tx + m10 * ty + m30)
            ._m31(m01 * tx + m11 * ty + m31)
            ._m32(m02 * tx + m12 * ty + m32)
            ._m33(m03 * tx + m13 * ty + m33)
            ._m00(m00 * w.toFloat())._m01(m01 * w.toFloat())
            ._m02(m02 * w.toFloat())._m03(m03 * w.toFloat())
            ._m10(m10 * h.toFloat())._m11(m11 * h.toFloat())
            ._m12(m12 * h.toFloat())._m13(m13 * h.toFloat())
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._properties(flags and -30)
    }

    @JvmOverloads
    fun perspective(
        fovy: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setPerspective(
            fovy,
            aspect,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dst)
    }

    private fun perspectiveGeneric(
        fovy: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val h = tan(fovy * 0.5f)
        val rm00 = 1f / (h * aspect)
        val rm11 = 1f / h
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = e - 1f
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 - m30
        val nm21 = m21 * rm22 - m31
        val nm22 = m22 * rm22 - m32
        val nm23 = m23 * rm22 - m33
        dst._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                flags and -31
            )
        return dst
    }

    @JvmOverloads
    fun perspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float, dst: Matrix4f = this): Matrix4f {
        return perspective(fovy, aspect, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun perspectiveRect(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setPerspectiveRect(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveRectGeneric(width, height, zNear, zFar, zZeroToOne, dst)
    }

    private fun perspectiveRectGeneric(
        width: Float,
        height: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = (zNear + zNear) / width
        val rm11 = (zNear + zNear) / height
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = e - 1f
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 - m30
        val nm21 = m21 * rm22 - m31
        val nm22 = m22 * rm22 - m32
        val nm23 = m23 * rm22 - m33
        dst._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                flags and -31
            )
        return dst
    }

    @JvmOverloads
    fun perspectiveRect(width: Float, height: Float, zNear: Float, zFar: Float, dst: Matrix4f = this): Matrix4f {
        return perspectiveRect(width, height, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun perspectiveOffCenter(
        fovy: Float,
        offAngleX: Float,
        offAngleY: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setPerspectiveOffCenter(
            fovy,
            offAngleX,
            offAngleY,
            aspect,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveOffCenterGeneric(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, dst)
    }

    private fun perspectiveOffCenterGeneric(
        fovy: Float,
        offAngleX: Float,
        offAngleY: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val h = tan(fovy * 0.5f)
        val xScale = 1f / (h * aspect)
        val yScale = 1f / h
        val offX = tan(offAngleX)
        val offY = tan(offAngleY)
        val rm20 = offX * xScale
        val rm21 = offY * yScale
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = e - 1f
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 - m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 - m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 - m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 - m33
        dst._m00(m00 * xScale)._m01(m01 * xScale)._m02(m02 * xScale)._m03(m03 * xScale)._m10(m10 * yScale)
            ._m11(m11 * yScale)._m12(
                m12 * yScale
            )._m13(m13 * yScale)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                flags and (30 or if (rm20 == 0f && rm21 == 0f) 0 else 1).inv()
            )
        return dst
    }

    @JvmOverloads
    fun perspectiveOffCenter(
        fovy: Float,
        offAngleX: Float,
        offAngleY: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun perspectiveOffCenterFov(
        angleLeft: Float,
        angleRight: Float,
        angleDown: Float,
        angleUp: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustum(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne,
            dst
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFov(
        angleLeft: Float,
        angleRight: Float,
        angleDown: Float,
        angleUp: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustum(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            dst
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFovLH(
        angleLeft: Float,
        angleRight: Float,
        angleDown: Float,
        angleUp: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne,
            dst
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFovLH(
        angleLeft: Float,
        angleRight: Float,
        angleDown: Float,
        angleUp: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            dst
        )
    }

    fun setPerspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4f {
        zero()
        val h = tan(fovy * 0.5f)
        _m00(1f / (h * aspect))._m11(1f / h)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(e - 1f)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        return _m23(-1f)._properties(1)
    }

    fun setPerspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float): Matrix4f {
        return setPerspective(fovy, aspect, zNear, zFar, false)
    }

    fun setPerspectiveRect(width: Float, height: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4f {
        zero()
        _m00((zNear + zNear) / width)._m11((zNear + zNear) / height)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(e - 1f)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1f)._properties(1)
        return this
    }

    fun setPerspectiveRect(width: Float, height: Float, zNear: Float, zFar: Float): Matrix4f {
        return setPerspectiveRect(width, height, zNear, zFar, false)
    }

    fun setPerspectiveOffCenter(
        fovy: Float, offAngleX: Float, offAngleY: Float,
        aspect: Float, zNear: Float, zFar: Float
    ): Matrix4f {
        return setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false)
    }

    fun setPerspectiveOffCenter(
        fovy: Float, offAngleX: Float, offAngleY: Float, aspect: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean
    ): Matrix4f {
        zero()
        val h = tan(fovy * 0.5f)
        val xScale = 1f / (h * aspect)
        val yScale = 1f / h
        val offX = tan(offAngleX)
        val offY = tan(offAngleY)
        _m00(xScale)._m11(yScale)._m20(offX * xScale)._m21(offY * yScale)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(e - 1f)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1f)._properties(if (offAngleX == 0f && offAngleY == 0f) 1 else 0)
        return this
    }

    fun setPerspectiveOffCenterFov(
        angleLeft: Float, angleRight: Float,
        angleDown: Float, angleUp: Float,
        zNear: Float, zFar: Float
    ): Matrix4f {
        return setPerspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false)
    }

    fun setPerspectiveOffCenterFov(
        angleLeft: Float, angleRight: Float,
        angleDown: Float, angleUp: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean
    ): Matrix4f {
        return setFrustum(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne
        )
    }

    fun setPerspectiveOffCenterFovLH(
        angleLeft: Float, angleRight: Float,
        angleDown: Float, angleUp: Float,
        zNear: Float, zFar: Float
    ): Matrix4f {
        return setPerspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false)
    }

    fun setPerspectiveOffCenterFovLH(
        angleLeft: Float, angleRight: Float,
        angleDown: Float, angleUp: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean
    ): Matrix4f {
        return setFrustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear, zFar, zZeroToOne
        )
    }

    @JvmOverloads
    fun perspectiveLH(
        fovy: Float, aspect: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean, dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setPerspectiveLH(fovy, aspect, zNear, zFar, zZeroToOne)
        else perspectiveLHGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dst)
    }

    private fun perspectiveLHGeneric(
        fovy: Float, aspect: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean, dst: Matrix4f
    ): Matrix4f {
        val h = tan(fovy * 0.5f)
        val rm00 = 1f / (h * aspect)
        val rm11 = 1f / h
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = 1f - e
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 + m30
        val nm21 = m21 * rm22 + m31
        val nm22 = m22 * rm22 + m32
        val nm23 = m23 * rm22 + m33
        dst._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                flags and -31
            )
        return dst
    }

    @JvmOverloads
    fun perspectiveLH(fovy: Float, aspect: Float, zNear: Float, zFar: Float, dst: Matrix4f = this): Matrix4f {
        return perspectiveLH(fovy, aspect, zNear, zFar, false, dst)
    }

    fun setPerspectiveLH(fovy: Float, aspect: Float, zNear: Float, zFar: Float, zZeroToOne: Boolean): Matrix4f {
        zero()
        val h = tan(fovy * 0.5f)
        _m00(1f / (h * aspect))._m11(1f / h)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(1f - e)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(1f)._properties(1)
        return this
    }

    fun setPerspectiveLH(fovy: Float, aspect: Float, zNear: Float, zFar: Float): Matrix4f {
        return setPerspectiveLH(fovy, aspect, zNear, zFar, false)
    }

    @JvmOverloads
    fun frustum(
        left: Float, right: Float, bottom: Float, top: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean, dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setFrustum(left, right, bottom, top, zNear, zFar, zZeroToOne)
        else frustumGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dst)
    }

    private fun frustumGeneric(
        left: Float, right: Float, bottom: Float, top: Float,
        zNear: Float, zFar: Float, zZeroToOne: Boolean, dst: Matrix4f
    ): Matrix4f {
        val rm00 = (zNear + zNear) / (right - left)
        val rm11 = (zNear + zNear) / (top - bottom)
        val rm20 = (right + left) / (right - left)
        val rm21 = (top + bottom) / (top - bottom)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = e - 1f
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 - m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 - m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 - m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 - m33
        dst._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(0)
        return dst
    }

    @JvmOverloads
    fun frustum(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustum(left, right, bottom, top, zNear, zFar, false, dst)
    }

    fun setFrustum(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean
    ): Matrix4f {
        identity()
        _m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))
            ._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom))
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(e - 1f)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1f)._m33(0f)._properties(if (m20 == 0f && m21 == 0f) 1 else 0)
        return this
    }

    fun setFrustum(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4f {
        return setFrustum(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun frustumLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f = this
    ): Matrix4f {
        return if (flags and 4 != 0) dst.setFrustumLH(
            left,
            right,
            bottom,
            top,
            zNear,
            zFar,
            zZeroToOne
        ) else frustumLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dst)
    }

    private fun frustumLHGeneric(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean,
        dst: Matrix4f
    ): Matrix4f {
        val rm00 = (zNear + zNear) / (right - left)
        val rm11 = (zNear + zNear) / (top - bottom)
        val rm20 = (right + left) / (right - left)
        val rm21 = (top + bottom) / (top - bottom)
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val rm22: Float
        val rm32: Float
        var e: Float
        if (farInf) {
            e = 1.0E-6f
            rm22 = 1f - e
            rm32 = (e - if (zZeroToOne) 1f else 2f) * zNear
        } else if (nearInf) {
            e = 1.0E-6f
            rm22 = (if (zZeroToOne) 0f else 1f) - e
            rm32 = ((if (zZeroToOne) 1f else 2f) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 + m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 + m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 + m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 + m33
        dst._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(0)
        return dst
    }

    @JvmOverloads
    fun frustumLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        return frustumLH(left, right, bottom, top, zNear, zFar, false, dst)
    }

    fun setFrustumLH(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float,
        zNear: Float,
        zFar: Float,
        zZeroToOne: Boolean
    ): Matrix4f {
        identity()
        _m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))
            ._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom))
        val farInf = zFar > 0f && zFar.isInfinite()
        val nearInf = zNear > 0f && zNear.isInfinite()
        val e: Float
        if (farInf) {
            e = 1.0E-6f
            _m22(1f - e)._m32((e - if (zZeroToOne) 1f else 2f) * zNear)
        } else if (nearInf) {
            e = 1.0E-6f
            _m22((if (zZeroToOne) 0f else 1f) - e)._m32(((if (zZeroToOne) 1f else 2f) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        return _m23(1f)._m33(0f)._properties(0)
    }

    fun setFrustumLH(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float): Matrix4f {
        return setFrustumLH(left, right, bottom, top, zNear, zFar, false)
    }

    fun setFromIntrinsic(
        alphaX: Float,
        alphaY: Float,
        gamma: Float,
        u0: Float,
        v0: Float,
        imgWidth: Int,
        imgHeight: Int,
        near: Float,
        far: Float
    ): Matrix4f {
        val l00 = 2f / imgWidth.toFloat()
        val l11 = 2f / imgHeight.toFloat()
        val l22 = 2f / (near - far)
        return _m00(l00 * alphaX)._m01(0f)._m02(0f)._m03(0f)._m10(l00 * gamma)._m11(l11 * alphaY)._m12(0f)._m13(0f)
            ._m20(l00 * u0 - 1f)._m21(l11 * v0 - 1f)._m22(l22 * -(near + far) + (far + near) / (near - far))._m23(-1f)
            ._m30(0f)._m31(0f)._m32(l22 * -near * far)._m33(0f)._properties(1)
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dst: Matrix4f = this): Matrix4f {
        return rotateQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    @JvmOverloads
    fun rotate(quat: Quaterniond, dst: Matrix4f = this): Matrix4f {
        return rotateQ(quat.x.toFloat(), quat.y.toFloat(), quat.z.toFloat(), quat.w.toFloat(), dst)
    }

    @JvmOverloads
    fun rotateInv(quat: Quaternionf, dst: Matrix4f = this): Matrix4f {
        return rotateQ(quat.x, quat.y, quat.z, -quat.w, dst)
    }

    @JvmOverloads
    fun rotateInv(quat: Quaterniond, dst: Matrix4f = this): Matrix4f {
        return rotateQ(quat.x.toFloat(), quat.y.toFloat(), quat.z.toFloat(), -quat.w.toFloat(), dst)
    }

    fun rotateQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.rotationQ(qx, qy, qz, qw)
        } else if (flags and 8 != 0) {
            this.rotateTranslationQ(qx, qy, qz, qw, dst)
        } else if (flags and 2 != 0) {
            this.rotateAffineQ(qx, qy, qz, qw, dst)
        } else this.rotateGenericQ(qx, qy, qz, qw, dst)
    }

    private fun rotateGeneric(quat: Quaternionf, dst: Matrix4f): Matrix4f {
        return rotateGenericQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    private fun rotateGenericQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4f): Matrix4f {
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
        val rm10 = -dzw + dxy
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = dyz + dxw
        val rm20 = dyw + dxz
        val rm21 = dyz - dxw
        val rm22 = z2 - y2 - x2 + w2
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAffine(quat: Quaternionf, dst: Matrix4f = this): Matrix4f {
        return rotateAffineQ(quat.x, quat.y, quat.z, quat.w, dst)
    }

    fun rotateAffineQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4f = this): Matrix4f {
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
        val rm10 = -dzw + dxy
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
        return dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m30(m30)._m31(
            m31
        )._m32(m32)._m33(m33)._properties(flags and -14)
    }

    fun rotateTranslation(quat: Quaternionf, dst: Matrix4f): Matrix4f {
        return rotateTranslation(quat.x, quat.y, quat.z, quat.w, dst)
    }

    fun rotateTranslationQ(qx: Float, qy: Float, qz: Float, qw: Float, dst: Matrix4f): Matrix4f {
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
        val rm10 = -dzw + dxy
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = dyz + dxw
        val rm20 = dyw + dxz
        val rm21 = dyz - dxw
        val rm22 = z2 - y2 - x2 + w2
        return dst._m20(rm20)._m21(rm21)._m22(rm22)._m23(0f)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0f)._m10(rm10)
            ._m11(rm11)._m12(rm12)._m13(0f)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(flags and -14)
    }

    fun rotateAroundAffine(quat: Quaternionf, ox: Float, oy: Float, oz: Float, dst: Matrix4f): Matrix4f {
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
        val rm10 = -dzw + dxy
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
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)
            ._m30(-nm00 * ox - nm10 * oy - m20 * oz + tm30)._m31(
                -nm01 * ox - nm11 * oy - m21 * oz + tm31
            )._m32(-nm02 * ox - nm12 * oy - m22 * oz + tm32)._m33(1f)._properties(
                flags and -14
            )
        return dst
    }

    @JvmOverloads
    fun rotateAround(quat: Quaternionf, ox: Float, oy: Float, oz: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            rotationAround(quat, ox, oy, oz)
        } else {
            if (flags and 2 != 0) rotateAroundAffine(quat, ox, oy, oz, dst) else rotateAroundGeneric(
                quat,
                ox,
                oy,
                oz,
                dst
            )
        }
    }

    private fun rotateAroundGeneric(quat: Quaternionf, ox: Float, oy: Float, oz: Float, dst: Matrix4f): Matrix4f {
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
        val rm10 = -dzw + dxy
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
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                -nm00 * ox - nm10 * oy - m20 * oz + tm30
            )._m31(-nm01 * ox - nm11 * oy - m21 * oz + tm31)._m32(-nm02 * ox - nm12 * oy - m22 * oz + tm32)._m33(
                m33
            )._properties(flags and -14)
        return dst
    }

    fun rotationAround(quat: Quaternionf, ox: Float, oy: Float, oz: Float): Matrix4f {
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
        _m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._m23(0f)._m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)
            ._m02(dxz - dyw)._m03(0f)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)._m13(0f)._m30(
                -m00 * ox - m10 * oy - m20 * oz + ox
            )._m31(-m01 * ox - m11 * oy - m21 * oz + oy)._m32(
                -m02 * ox - m12 * oy - m22 * oz + oz
            )._m33(1f)._properties(18)
        return this
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaternionf, dst: Matrix4f = this): Matrix4f {
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
        val lm10 = -dzw + dxy
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
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(flags and -14)
    }

    @JvmOverloads
    fun rotateAroundLocal(quat: Quaternionf, ox: Float, oy: Float, oz: Float, dst: Matrix4f = this): Matrix4f {
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
        val tm00 = m00 - ox * m03
        val tm01 = m01 - oy * m03
        val tm02 = m02 - oz * m03
        val tm10 = m10 - ox * m13
        val tm11 = m11 - oy * m13
        val tm12 = m12 - oz * m13
        val tm20 = m20 - ox * m23
        val tm21 = m21 - oy * m23
        val tm22 = m22 - oz * m23
        val tm30 = m30 - ox * m33
        val tm31 = m31 - oy * m33
        val tm32 = m32 - oz * m33
        dst._m00(lm00 * tm00 + lm10 * tm01 + lm20 * tm02 + ox * m03)
            ._m01(lm01 * tm00 + lm11 * tm01 + lm21 * tm02 + oy * m03)._m02(
                lm02 * tm00 + lm12 * tm01 + lm22 * tm02 + oz * m03
            )._m03(m03)._m10(lm00 * tm10 + lm10 * tm11 + lm20 * tm12 + ox * m13)
            ._m11(lm01 * tm10 + lm11 * tm11 + lm21 * tm12 + oy * m13)._m12(
                lm02 * tm10 + lm12 * tm11 + lm22 * tm12 + oz * m13
            )._m13(m13)._m20(lm00 * tm20 + lm10 * tm21 + lm20 * tm22 + ox * m23)
            ._m21(lm01 * tm20 + lm11 * tm21 + lm21 * tm22 + oy * m23)._m22(
                lm02 * tm20 + lm12 * tm21 + lm22 * tm22 + oz * m23
            )._m23(m23)._m30(lm00 * tm30 + lm10 * tm31 + lm20 * tm32 + ox * m33)
            ._m31(lm01 * tm30 + lm11 * tm31 + lm21 * tm32 + oy * m33)._m32(
                lm02 * tm30 + lm12 * tm31 + lm22 * tm32 + oz * m33
            )._m33(m33)._properties(flags and -14)
        return dst
    }

    fun rotate(axisAngle: AxisAngle4f): Matrix4f {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4f, dst: Matrix4f): Matrix4f {
        return rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dst)
    }

    fun rotate(angle: Float, axis: Vector3f): Matrix4f {
        return rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Float, axis: Vector3f, dst: Matrix4f): Matrix4f {
        return rotate(angle, axis.x, axis.y, axis.z, dst)
    }

    fun unproject(winX: Float, winY: Float, winZ: Float, viewport: IntArray, dst: Vector4f): Vector4f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        var det = a * l - b * k + c * j + d * i - e * h + f * g
        det = 1f / det
        val im00 = (m11 * l - m12 * k + m13 * j) * det
        val im01 = (-m01 * l + m02 * k - m03 * j) * det
        val im02 = (m31 * f - m32 * e + m33 * d) * det
        val im03 = (-m21 * f + m22 * e - m23 * d) * det
        val im10 = (-m10 * l + m12 * i - m13 * h) * det
        val im11 = (m00 * l - m02 * i + m03 * h) * det
        val im12 = (-m30 * f + m32 * c - m33 * b) * det
        val im13 = (m20 * f - m22 * c + m23 * b) * det
        val im20 = (m10 * k - m11 * i + m13 * g) * det
        val im21 = (-m00 * k + m01 * i - m03 * g) * det
        val im22 = (m30 * e - m31 * c + m33 * a) * det
        val im23 = (-m20 * e + m21 * c - m23 * a) * det
        val im30 = (-m10 * j + m11 * h - m12 * g) * det
        val im31 = (m00 * j - m01 * h + m02 * g) * det
        val im32 = (-m30 * d + m31 * b - m32 * a) * det
        val im33 = (m20 * d - m21 * b + m22 * a) * det
        val ndcX = (winX - viewport[0].toFloat()) / viewport[2].toFloat() * 2f - 1f
        val ndcY = (winY - viewport[1].toFloat()) / viewport[3].toFloat() * 2f - 1f
        val ndcZ = winZ + winZ - 1f
        val invW = 1f / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33)
        return dst.set(
            (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW,
            (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW,
            (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW,
            1f
        )
    }

    fun unproject(winX: Float, winY: Float, winZ: Float, viewport: IntArray, dst: Vector3f): Vector3f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        var det = a * l - b * k + c * j + d * i - e * h + f * g
        det = 1f / det
        val im00 = (m11 * l - m12 * k + m13 * j) * det
        val im01 = (-m01 * l + m02 * k - m03 * j) * det
        val im02 = (m31 * f - m32 * e + m33 * d) * det
        val im03 = (-m21 * f + m22 * e - m23 * d) * det
        val im10 = (-m10 * l + m12 * i - m13 * h) * det
        val im11 = (m00 * l - m02 * i + m03 * h) * det
        val im12 = (-m30 * f + m32 * c - m33 * b) * det
        val im13 = (m20 * f - m22 * c + m23 * b) * det
        val im20 = (m10 * k - m11 * i + m13 * g) * det
        val im21 = (-m00 * k + m01 * i - m03 * g) * det
        val im22 = (m30 * e - m31 * c + m33 * a) * det
        val im23 = (-m20 * e + m21 * c - m23 * a) * det
        val im30 = (-m10 * j + m11 * h - m12 * g) * det
        val im31 = (m00 * j - m01 * h + m02 * g) * det
        val im32 = (-m30 * d + m31 * b - m32 * a) * det
        val im33 = (m20 * d - m21 * b + m22 * a) * det
        val ndcX = (winX - viewport[0].toFloat()) / viewport[2].toFloat() * 2f - 1f
        val ndcY = (winY - viewport[1].toFloat()) / viewport[3].toFloat() * 2f - 1f
        val ndcZ = winZ + winZ - 1f
        val invW = 1f / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33)
        return dst.set(
            (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW,
            (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW,
            (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW
        )
    }

    fun unproject(winCoords: Vector3f, viewport: IntArray, dst: Vector4f): Vector4f {
        return unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dst)
    }

    fun unproject(winCoords: Vector3f, viewport: IntArray, dst: Vector3f): Vector3f {
        return unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dst)
    }

    fun unprojectRay(winX: Float, winY: Float, viewport: IntArray, origindst: Vector3f, dirdst: Vector3f): Matrix4f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        var det = a * l - b * k + c * j + d * i - e * h + f * g
        det = 1f / det
        val im00 = (m11 * l - m12 * k + m13 * j) * det
        val im01 = (-m01 * l + m02 * k - m03 * j) * det
        val im02 = (m31 * f - m32 * e + m33 * d) * det
        val im03 = (-m21 * f + m22 * e - m23 * d) * det
        val im10 = (-m10 * l + m12 * i - m13 * h) * det
        val im11 = (m00 * l - m02 * i + m03 * h) * det
        val im12 = (-m30 * f + m32 * c - m33 * b) * det
        val im13 = (m20 * f - m22 * c + m23 * b) * det
        val im20 = (m10 * k - m11 * i + m13 * g) * det
        val im21 = (-m00 * k + m01 * i - m03 * g) * det
        val im22 = (m30 * e - m31 * c + m33 * a) * det
        val im23 = (-m20 * e + m21 * c - m23 * a) * det
        val im30 = (-m10 * j + m11 * h - m12 * g) * det
        val im31 = (m00 * j - m01 * h + m02 * g) * det
        val im32 = (-m30 * d + m31 * b - m32 * a) * det
        val im33 = (m20 * d - m21 * b + m22 * a) * det
        val ndcX = (winX - viewport[0].toFloat()) / viewport[2].toFloat() * 2f - 1f
        val ndcY = (winY - viewport[1].toFloat()) / viewport[3].toFloat() * 2f - 1f
        val px = im00 * ndcX + im10 * ndcY + im30
        val py = im01 * ndcX + im11 * ndcY + im31
        val pz = im02 * ndcX + im12 * ndcY + im32
        val invNearW = 1f / (im03 * ndcX + im13 * ndcY - im23 + im33)
        val nearX = (px - im20) * invNearW
        val nearY = (py - im21) * invNearW
        val nearZ = (pz - im22) * invNearW
        val invW0 = 1f / (im03 * ndcX + im13 * ndcY + im33)
        val x0 = px * invW0
        val y0 = py * invW0
        val z0 = pz * invW0
        origindst.x = nearX
        origindst.y = nearY
        origindst.z = nearZ
        dirdst.x = x0 - nearX
        dirdst.y = y0 - nearY
        dirdst.z = z0 - nearZ
        return this
    }

    fun unprojectRay(winCoords: Vector2f, viewport: IntArray, origindst: Vector3f, dirdst: Vector3f): Matrix4f {
        return unprojectRay(winCoords.x, winCoords.y, viewport, origindst, dirdst)
    }

    fun unprojectInv(winCoords: Vector3f, viewport: IntArray, dst: Vector4f): Vector4f {
        return unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dst)
    }

    fun unprojectInv(winX: Float, winY: Float, winZ: Float, viewport: IntArray, dst: Vector4f): Vector4f {
        val ndcX = (winX - viewport[0].toFloat()) / viewport[2].toFloat() * 2f - 1f
        val ndcY = (winY - viewport[1].toFloat()) / viewport[3].toFloat() * 2f - 1f
        val ndcZ = winZ + winZ - 1f
        val invW = 1f / (m03 * ndcX + m13 * ndcY + m23 * ndcZ + m33)
        return dst.set(
            (m00 * ndcX + m10 * ndcY + m20 * ndcZ + m30) * invW,
            (m01 * ndcX + m11 * ndcY + m21 * ndcZ + m31) * invW,
            (m02 * ndcX + m12 * ndcY + m22 * ndcZ + m32) * invW,
            1f
        )
    }

    fun unprojectInvRay(winCoords: Vector2f, viewport: IntArray, origindst: Vector3f, dirdst: Vector3f): Matrix4f {
        return unprojectInvRay(winCoords.x, winCoords.y, viewport, origindst, dirdst)
    }

    fun unprojectInvRay(
        winX: Float,
        winY: Float,
        viewport: IntArray,
        origindst: Vector3f?,
        dirdst: Vector3f?
    ): Matrix4f {
        return unprojectInvRay(
            winX, winY,
            viewport[0].toFloat(), viewport[1].toFloat(),
            viewport[2].toFloat(), viewport[3].toFloat(),
            origindst, dirdst
        )
    }

    fun unprojectInvRay(
        winX: Float, winY: Float,
        windowX: Float, windowY: Float,
        windowW: Float, windowH: Float,
        origindst: Vector3f?,
        dirdst: Vector3f?
    ): Matrix4f {
        val ndcX = (winX - windowX) / windowW * 2f - 1f
        val ndcY = (winY - windowY) / windowH * 2f - 1f
        val px = m00 * ndcX + m10 * ndcY + m30
        val py = m01 * ndcX + m11 * ndcY + m31
        val pz = m02 * ndcX + m12 * ndcY + m32
        val invNearW = 1f / (m03 * ndcX + m13 * ndcY - m23 + m33)
        val nearX = (px - m20) * invNearW
        val nearY = (py - m21) * invNearW
        val nearZ = (pz - m22) * invNearW
        val invW0 = 1f / (m03 * ndcX + m13 * ndcY + m33)
        val x0 = px * invW0
        val y0 = py * invW0
        val z0 = pz * invW0
        if (origindst != null) {
            origindst.x = nearX
            origindst.y = nearY
            origindst.z = nearZ
        }
        if (dirdst != null) {
            dirdst.x = x0 - nearX
            dirdst.y = y0 - nearY
            dirdst.z = z0 - nearZ
        }
        return this
    }

    fun unprojectInv(winCoords: Vector3f, viewport: IntArray, dst: Vector3f): Vector3f {
        return unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dst)
    }

    fun unprojectInv(winX: Float, winY: Float, winZ: Float, viewport: IntArray, dst: Vector3f): Vector3f {
        val ndcX = (winX - viewport[0].toFloat()) / viewport[2].toFloat() * 2f - 1f
        val ndcY = (winY - viewport[1].toFloat()) / viewport[3].toFloat() * 2f - 1f
        val ndcZ = winZ + winZ - 1f
        val invW = 1f / (m03 * ndcX + m13 * ndcY + m23 * ndcZ + m33)
        return dst.set(
            (m00 * ndcX + m10 * ndcY + m20 * ndcZ + m30) * invW,
            (m01 * ndcX + m11 * ndcY + m21 * ndcZ + m31) * invW,
            (m02 * ndcX + m12 * ndcY + m22 * ndcZ + m32) * invW
        )
    }

    fun project(x: Float, y: Float, z: Float, viewport: IntArray, winCoordsdst: Vector4f): Vector4f {
        val invW = 1f / (m03 * x + m13 * y + m23 * z + m33)
        val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
        val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
        val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
        return winCoordsdst.set(
            (nx * 0.5f + 0.5f) * viewport[2] + viewport[0],
            (ny * 0.5f + 0.5f) * viewport[3] + viewport[1],
            0.5f * nz + 0.5f, 1f
        )
    }

    fun project(x: Float, y: Float, z: Float, viewport: IntArray, winCoordsdst: Vector3f): Vector3f {
        val invW = 1f / (m03 * x + m13 * y + m23 * z + m33)
        val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
        val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
        val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
        winCoordsdst.x = (nx * 0.5f + 0.5f) * viewport[2] + viewport[0]
        winCoordsdst.y = (ny * 0.5f + 0.5f) * viewport[3] + viewport[1]
        winCoordsdst.z = 0.5f * nz + 0.5f
        return winCoordsdst
    }

    fun project(position: Vector3f, viewport: IntArray, winCoordsdst: Vector4f): Vector4f {
        return project(position.x, position.y, position.z, viewport, winCoordsdst)
    }

    fun project(position: Vector3f, viewport: IntArray, winCoordsdst: Vector3f): Vector3f {
        return project(position.x, position.y, position.z, viewport, winCoordsdst)
    }

    @JvmOverloads
    fun reflect(a: Float, b: Float, c: Float, d: Float, dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.reflection(a, b, c, d)
        } else {
            if (flags and 2 != 0) reflectAffine(a, b, c, d, dst)
            else reflectGeneric(a, b, c, d, dst)
        }
    }

    private fun reflectAffine(a: Float, b: Float, c: Float, d: Float, dst: Matrix4f): Matrix4f {
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
        dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m33)
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0f)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._properties(
            flags and -14
        )
        return dst
    }

    private fun reflectGeneric(a: Float, b: Float, c: Float, d: Float, dst: Matrix4f): Matrix4f {
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
        dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._properties(
                flags and -14
            )
        return dst
    }

    @JvmOverloads
    fun reflect(nx: Float, ny: Float, nz: Float, px: Float, py: Float, pz: Float, dst: Matrix4f = this): Matrix4f {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dst)
    }

    fun reflect(normal: Vector3f, point: Vector3f): Matrix4f {
        return reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    @JvmOverloads
    fun reflect(orientation: Quaternionf, point: Vector3f, dst: Matrix4f = this): Matrix4f {
        val num1 = (orientation.x + orientation.x).toDouble()
        val num2 = (orientation.y + orientation.y).toDouble()
        val num3 = (orientation.z + orientation.z).toDouble()
        val normalX = (orientation.x.toDouble() * num3 + orientation.w.toDouble() * num2).toFloat()
        val normalY = (orientation.y.toDouble() * num3 - orientation.w.toDouble() * num1).toFloat()
        val normalZ = (1.0 - (orientation.x.toDouble() * num1 + orientation.y.toDouble() * num2)).toFloat()
        return reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dst)
    }

    fun reflect(normal: Vector3f, point: Vector3f, dst: Matrix4f): Matrix4f {
        return reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dst)
    }

    fun reflection(a: Float, b: Float, c: Float, d: Float): Matrix4f {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        _m00(1f - da * a)._m01(-da * b)._m02(-da * c)._m03(0f)._m10(-db * a)._m11(1f - db * b)._m12(-db * c)._m13(0f)
            ._m20(-dc * a)._m21(-dc * b)._m22(1f - dc * c)._m23(0f)._m30(-dd * a)._m31(-dd * b)._m32(-dd * c)._m33(1f)
            ._properties(18)
        return this
    }

    fun reflection(nx: Float, ny: Float, nz: Float, px: Float, py: Float, pz: Float): Matrix4f {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz)
    }

    fun reflection(normal: Vector3f, point: Vector3f): Matrix4f {
        return reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    fun reflection(orientation: Quaternionf, point: Vector3f): Matrix4f {
        val num1 = (orientation.x + orientation.x).toDouble()
        val num2 = (orientation.y + orientation.y).toDouble()
        val num3 = (orientation.z + orientation.z).toDouble()
        val normalX = (orientation.x.toDouble() * num3 + orientation.w.toDouble() * num2).toFloat()
        val normalY = (orientation.y.toDouble() * num3 - orientation.w.toDouble() * num1).toFloat()
        val normalZ = (1.0 - (orientation.x.toDouble() * num1 + orientation.y.toDouble() * num2)).toFloat()
        return reflection(normalX, normalY, normalZ, point.x, point.y, point.z)
    }

    fun getRow(row: Int, dst: Vector4f): Vector4f {
        return when (row) {
            0 -> dst.set(m00, m10, m20, m30)
            1 -> dst.set(m01, m11, m21, m31)
            2 -> dst.set(m02, m12, m22, m32)
            else -> dst.set(m03, m13, m23, m33)
        }
    }

    fun getRow(row: Int, dst: Vector3f): Vector3f {
        return when (row) {
            0 -> dst.set(m00, m10, m20)
            1 -> dst.set(m01, m11, m21)
            2 -> dst.set(m02, m12, m22)
            else -> dst.set(m03, m13, m23)
        }
    }

    fun setRow(row: Int, src: Vector4f): Matrix4f {
        return when (row) {
            0 -> _m00(src.x)._m10(src.y)._m20(src.z)._m30(src.w)._properties(0)
            1 -> _m01(src.x)._m11(src.y)._m21(src.z)._m31(src.w)._properties(0)
            2 -> _m02(src.x)._m12(src.y)._m22(src.z)._m32(src.w)._properties(0)
            else -> _m03(src.x)._m13(src.y)._m23(src.z)._m33(src.w)._properties(0)
        }
    }

    /*
    fun getColumn(column: Int, dst: Vector4f): Vector4f {
        return MemUtil.INSTANCE.getColumn(this, column, dst)
    }

    fun getColumn(column: Int, dst: Vector3f): Vector3f {
        return when (column) {
            0 -> dst.set(m00, m01, m02)
            1 -> dst.set(m10, m11, m12)
            2 -> dst.set(m20, m21, m22)
            3 -> dst.set(m30, m31, m32)
            else -> throw IndexOutOfBoundsException()
        }
    }

    fun setColumn(column: Int, src: Vector4f?): Matrix4f {
        return MemUtil.INSTANCE.setColumn(src, column, this)._properties(0)
    }

    operator fun get(column: Int, row: Int): Float {
        return MemUtil.INSTANCE[this, column, row]
    }

    operator fun set(column: Int, row: Int, value: Float): Matrix4f {
        return MemUtil.INSTANCE.set(this, column, row, value)
    }

    fun getRowColumn(row: Int, column: Int): Float {
        return get(column, row)
    }

    fun setRowColumn(row: Int, column: Int, value: Float): Matrix4f {
        return set(column, row, value)
    }*/

    @JvmOverloads
    fun normal(dst: Matrix4f = this): Matrix4f {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
        }
    }

    private fun normalOrthonormal(dst: Matrix4f): Matrix4f {
        if (dst !== this) {
            dst.set(this)
        }
        return dst._properties(18)
    }

    private fun normalGeneric(dst: Matrix4f): Matrix4f {
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
        return dst._m00(nm00)._m01(nm01)._m02(nm02)._m03(0f)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(0f)._m30(0f)._m31(0f)._m32(0f)._m33(1f)._properties(
                flags or 2 and -10
            )
    }

    fun normal(dst: Matrix3f): Matrix3f {
        return if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
    }

    private fun normalOrthonormal(dst: Matrix3f): Matrix3f {
        dst.set(this)
        return dst
    }

    private fun normalGeneric(dst: Matrix3f): Matrix3f {
        val det = (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
        val s = 1f / det
        return dst._m00((m11 * m22 - m21 * m12) * s)._m01((m20 * m12 - m10 * m22) * s)
            ._m02((m10 * m21 - m20 * m11) * s)._m10((m21 * m02 - m01 * m22) * s)._m11((m00 * m22 - m20 * m02) * s)
            ._m12((m20 * m01 - m00 * m21) * s)._m20((m01 * m12 - m02 * m11) * s)._m21((m02 * m10 - m00 * m12) * s)
            ._m22((m00 * m11 - m01 * m10) * s)
    }

    fun cofactor3x3(dst: Matrix3f): Matrix3f {
        return dst._m00(m11 * m22 - m21 * m12)._m01(m20 * m12 - m10 * m22)._m02(m10 * m21 - m20 * m11)
            ._m10(m21 * m02 - m01 * m22)._m11(
                m00 * m22 - m20 * m02
            )._m12(m20 * m01 - m00 * m21)._m20(m01 * m12 - m02 * m11)._m21(m02 * m10 - m00 * m12)
            ._m22(m00 * m11 - m01 * m10)
    }

    @JvmOverloads
    fun cofactor3x3(dst: Matrix4f = this): Matrix4f {
        val nm10 = m21 * m02 - m01 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm12 = m20 * m01 - m00 * m21
        val nm20 = m01 * m12 - m11 * m02
        val nm21 = m02 * m10 - m12 * m00
        val nm22 = m00 * m11 - m10 * m01
        return dst._m00(m11 * m22 - m21 * m12)._m01(m20 * m12 - m10 * m22)._m02(m10 * m21 - m20 * m11)._m03(0f)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(0f)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0f)._m30(0f)._m31(0f)
            ._m32(0f)._m33(1f)._properties(
                flags or 2 and -10
            )
    }

    @JvmOverloads
    fun normalize3x3(dst: Matrix4f = this): Matrix4f {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst._m00(m00 * invXlen)._m01(m01 * invXlen)._m02(m02 * invXlen)._m10(m10 * invYlen)._m11(m11 * invYlen)
            ._m12(
                m12 * invYlen
            )._m20(m20 * invZlen)._m21(m21 * invZlen)._m22(m22 * invZlen)._m30(m30)._m31(m31)._m32(m32)._m33(
                m33
            )._properties(flags)
    }

    fun normalize3x3(dst: Matrix3f): Matrix3f {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst._m00(m00 * invXlen)._m01(m01 * invXlen)._m02(m02 * invXlen)._m10(m10 * invYlen)._m11(m11 * invYlen)
            ._m12(
                m12 * invYlen
            )._m20(m20 * invZlen)._m21(m21 * invZlen)._m22(m22 * invZlen)
    }

    fun frustumPlane(plane: Int, dst: Vector4f): Vector4f {
        when (plane) {
            0 -> dst.set(m03 + m00, m13 + m10, m23 + m20, m33 + m30).normalize3()
            1 -> dst.set(m03 - m00, m13 - m10, m23 - m20, m33 - m30).normalize3()
            2 -> dst.set(m03 + m01, m13 + m11, m23 + m21, m33 + m31).normalize3()
            3 -> dst.set(m03 - m01, m13 - m11, m23 - m21, m33 - m31).normalize3()
            4 -> dst.set(m03 + m02, m13 + m12, m23 + m22, m33 + m32).normalize3()
            else -> dst.set(m03 - m02, m13 - m12, m23 - m22, m33 - m32).normalize3()
        }
        return dst
    }

    fun frustumCorner(corner: Int, point: Vector3f): Vector3f {
        val d1: Float
        val d2: Float
        val d3: Float
        val n1x: Float
        val n1y: Float
        val n1z: Float
        val n2x: Float
        val n2y: Float
        val n2z: Float
        val n3x: Float
        val n3y: Float
        val n3z: Float
        when (corner) {
            0 -> {
                n1x = m03 + m00
                n1y = m13 + m10
                n1z = m23 + m20
                d1 = m33 + m30
                n2x = m03 + m01
                n2y = m13 + m11
                n2z = m23 + m21
                d2 = m33 + m31
                n3x = m03 + m02
                n3y = m13 + m12
                n3z = m23 + m22
                d3 = m33 + m32
            }
            1 -> {
                n1x = m03 - m00
                n1y = m13 - m10
                n1z = m23 - m20
                d1 = m33 - m30
                n2x = m03 + m01
                n2y = m13 + m11
                n2z = m23 + m21
                d2 = m33 + m31
                n3x = m03 + m02
                n3y = m13 + m12
                n3z = m23 + m22
                d3 = m33 + m32
            }
            2 -> {
                n1x = m03 - m00
                n1y = m13 - m10
                n1z = m23 - m20
                d1 = m33 - m30
                n2x = m03 - m01
                n2y = m13 - m11
                n2z = m23 - m21
                d2 = m33 - m31
                n3x = m03 + m02
                n3y = m13 + m12
                n3z = m23 + m22
                d3 = m33 + m32
            }
            3 -> {
                n1x = m03 + m00
                n1y = m13 + m10
                n1z = m23 + m20
                d1 = m33 + m30
                n2x = m03 - m01
                n2y = m13 - m11
                n2z = m23 - m21
                d2 = m33 - m31
                n3x = m03 + m02
                n3y = m13 + m12
                n3z = m23 + m22
                d3 = m33 + m32
            }
            4 -> {
                n1x = m03 - m00
                n1y = m13 - m10
                n1z = m23 - m20
                d1 = m33 - m30
                n2x = m03 + m01
                n2y = m13 + m11
                n2z = m23 + m21
                d2 = m33 + m31
                n3x = m03 - m02
                n3y = m13 - m12
                n3z = m23 - m22
                d3 = m33 - m32
            }
            5 -> {
                n1x = m03 + m00
                n1y = m13 + m10
                n1z = m23 + m20
                d1 = m33 + m30
                n2x = m03 + m01
                n2y = m13 + m11
                n2z = m23 + m21
                d2 = m33 + m31
                n3x = m03 - m02
                n3y = m13 - m12
                n3z = m23 - m22
                d3 = m33 - m32
            }
            6 -> {
                n1x = m03 + m00
                n1y = m13 + m10
                n1z = m23 + m20
                d1 = m33 + m30
                n2x = m03 - m01
                n2y = m13 - m11
                n2z = m23 - m21
                d2 = m33 - m31
                n3x = m03 - m02
                n3y = m13 - m12
                n3z = m23 - m22
                d3 = m33 - m32
            }
            else -> {
                n1x = m03 - m00
                n1y = m13 - m10
                n1z = m23 - m20
                d1 = m33 - m30
                n2x = m03 - m01
                n2y = m13 - m11
                n2z = m23 - m21
                d2 = m33 - m31
                n3x = m03 - m02
                n3y = m13 - m12
                n3z = m23 - m22
                d3 = m33 - m32
            }
        }
        val c23x = n2y * n3z - n2z * n3y
        val c23y = n2z * n3x - n2x * n3z
        val c23z = n2x * n3y - n2y * n3x
        val c31x = n3y * n1z - n3z * n1y
        val c31y = n3z * n1x - n3x * n1z
        val c31z = n3x * n1y - n3y * n1x
        val c12x = n1y * n2z - n1z * n2y
        val c12y = n1z * n2x - n1x * n2z
        val c12z = n1x * n2y - n1y * n2x
        val invDot = 1f / (n1x * c23x + n1y * c23y + n1z * c23z)
        point.x = -c23x * d1 - c31x * d2 - c12x * d3 * invDot
        point.y = -c23y * d1 - c31y * d2 - c12y * d3 * invDot
        point.z = -c23z * d1 - c31z * d2 - c12z * d3 * invDot
        return point
    }

    fun perspectiveOrigin(dst: Vector3f): Vector3f {
        val n1x = m03 + m00
        val n1y = m13 + m10
        val n1z = m23 + m20
        val d1 = m33 + m30
        val n2x = m03 - m00
        val n2y = m13 - m10
        val n2z = m23 - m20
        val d2 = m33 - m30
        val n3x = m03 - m01
        val n3y = m13 - m11
        val n3z = m23 - m21
        val d3 = m33 - m31
        val c23x = n2y * n3z - n2z * n3y
        val c23y = n2z * n3x - n2x * n3z
        val c23z = n2x * n3y - n2y * n3x
        val c31x = n3y * n1z - n3z * n1y
        val c31y = n3z * n1x - n3x * n1z
        val c31z = n3x * n1y - n3y * n1x
        val c12x = n1y * n2z - n1z * n2y
        val c12y = n1z * n2x - n1x * n2z
        val c12z = n1x * n2y - n1y * n2x
        val invDot = 1f / (n1x * c23x + n1y * c23y + n1z * c23z)
        dst.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot
        dst.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot
        dst.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot
        return dst
    }

    fun perspectiveInvOrigin(dst: Vector3f): Vector3f {
        val invW = 1f / m23
        dst.x = m20 * invW
        dst.y = m21 * invW
        dst.z = m22 * invW
        return dst
    }

    fun perspectiveFov(): Float {
        val n1x = m03 + m01
        val n1y = m13 + m11
        val n1z = m23 + m21 // bottom
        val n2x = m01 - m03
        val n2y = m11 - m13
        val n2z = m21 - m23 // top
        val n1len = sqrt(n1x * n1x + n1y * n1y + n1z * n1z)
        val n2len = sqrt(n2x * n2x + n2y * n2y + n2z * n2z)
        return acos((n1x * n2x + n1y * n2y + n1z * n2z) / (n1len * n2len))
    }

    fun perspectiveNear(): Float {
        return m32 / (m23 + m22)
    }

    fun perspectiveFar(): Float {
        return m32 / (m22 - m23)
    }

    fun frustumRayDir(x: Float, y: Float, dir: Vector3f): Vector3f {
        val a = m10 * m23
        val b = m13 * m21
        val c = m10 * m21
        val d = m11 * m23
        val e = m13 * m20
        val f = m11 * m20
        val g = m03 * m20
        val h = m01 * m23
        val i = m01 * m20
        val j = m03 * m21
        val k = m00 * m23
        val l = m00 * m21
        val m = m00 * m13
        val n = m03 * m11
        val o = m00 * m11
        val p = m01 * m13
        val q = m03 * m10
        val r = m01 * m10
        val m1x = (d + e + f - a - b - c) * (1f - y) + (a - b - c + d - e + f) * y
        val m1y = (j + k + l - g - h - i) * (1f - y) + (g - h - i + j - k + l) * y
        val m1z = (p + q + r - m - n - o) * (1f - y) + (m - n - o + p - q + r) * y
        val m2x = (b - c - d + e + f - a) * (1f - y) + (a + b - c - d - e + f) * y
        val m2y = (h - i - j + k + l - g) * (1f - y) + (g + h - i - j - k + l) * y
        val m2z = (n - o - p + q + r - m) * (1f - y) + (m + n - o - p - q + r) * y
        dir.x = m1x + (m2x - m1x) * x
        dir.y = m1y + (m2y - m1y) * x
        dir.z = m1z + (m2z - m1z) * x
        return dir.normalize(dir)
    }

    fun positiveZ(dir: Vector3f): Vector3f {
        return if (flags and 16 != 0) normalizedPositiveZ(dir) else positiveZGeneric(dir)
    }

    private fun positiveZGeneric(dir: Vector3f): Vector3f {
        return dir.set(m10 * m21 - m11 * m20, m20 * m01 - m21 * m00, m00 * m11 - m01 * m10).normalize()
    }

    fun normalizedPositiveZ(dir: Vector3f): Vector3f {
        return dir.set(m02, m12, m22)
    }

    fun positiveX(dir: Vector3f): Vector3f {
        return if (flags and 16 != 0) normalizedPositiveX(dir) else positiveXGeneric(dir)
    }

    private fun positiveXGeneric(dir: Vector3f): Vector3f {
        return dir.set(m11 * m22 - m12 * m21, m02 * m21 - m01 * m22, m01 * m12 - m02 * m11).normalize()
    }

    fun normalizedPositiveX(dir: Vector3f): Vector3f {
        return dir.set(m00, m10, m20)
    }

    fun positiveY(dir: Vector3f): Vector3f {
        return if (flags and 16 != 0) normalizedPositiveY(dir) else positiveYGeneric(dir)
    }

    private fun positiveYGeneric(dir: Vector3f): Vector3f {
        return dir.set(m12 * m20 - m10 * m22, m00 * m22 - m02 * m20, m02 * m10 - m00 * m12).normalize()
    }

    fun normalizedPositiveY(dir: Vector3f): Vector3f {
        return dir.set(m01, m11, m21)
    }

    fun originAffine(origin: Vector3f): Vector3f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val d = m01 * m12 - m02 * m11
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val j = m21 * m32 - m22 * m31
        return origin.set(-m10 * j + m11 * h - m12 * g, m00 * j - m01 * h + m02 * g, -m30 * d + m31 * b - m32 * a)
    }

    fun origin(dst: Vector3f): Vector3f {
        return if (flags and 2 != 0) originAffine(dst) else originGeneric(dst)
    }

    private fun originGeneric(dst: Vector3f): Vector3f {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val c = m00 * m13 - m03 * m10
        val d = m01 * m12 - m02 * m11
        val e = m01 * m13 - m03 * m11
        val f = m02 * m13 - m03 * m12
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val i = m20 * m33 - m23 * m30
        val j = m21 * m32 - m22 * m31
        val k = m21 * m33 - m23 * m31
        val l = m22 * m33 - m23 * m32
        val det = a * l - b * k + c * j + d * i - e * h + f * g
        val invDet = 1f / det
        val nm30 = (-m10 * j + m11 * h - m12 * g) * invDet
        val nm31 = (m00 * j - m01 * h + m02 * g) * invDet
        val nm32 = (-m30 * d + m31 * b - m32 * a) * invDet
        val nm33 = det / (m20 * d - m21 * b + m22 * a)
        return dst.set(nm30 * nm33, nm31 * nm33, nm32 * nm33)
    }

    fun shadow(light: Vector4f, a: Float, b: Float, c: Float, d: Float): Matrix4f {
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, this)
    }

    fun shadow(light: Vector4f, a: Float, b: Float, c: Float, d: Float, dst: Matrix4f): Matrix4f {
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
    }

    @JvmOverloads
    fun shadow(
        lightX: Float, lightY: Float, lightZ: Float, lightW: Float,
        a: Float, b: Float, c: Float, d: Float, dst: Matrix4f = this
    ): Matrix4f {
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
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02 + m33 * rm03
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12 + m30 * rm13
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12 + m31 * rm13
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12 + m32 * rm13
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12 + m33 * rm13
        val nm20 = m00 * rm20 + m10 * rm21 + m20 * rm22 + m30 * rm23
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 + m31 * rm23
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 + m32 * rm23
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 + m33 * rm23
        dst._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30 * rm33)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31 * rm33)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32 * rm33)
            ._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33 * rm33)
            ._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._properties(flags and -30)
        return dst
    }

    @JvmOverloads
    fun shadow(light: Vector4f, planeTransform: Matrix4f, dst: Matrix4f = this): Matrix4f {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
    }

    @JvmOverloads
    fun shadow(
        lightX: Float,
        lightY: Float,
        lightZ: Float,
        lightW: Float,
        planeTransform: Matrix4f,
        dst: Matrix4f = this
    ): Matrix4f {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dst)
    }

    fun billboardCylindrical(objPos: Vector3f, targetPos: Vector3f, up: Vector3f): Matrix4f {
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
        _m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(up.x)._m11(up.y)._m12(up.z)._m13(0f)._m20(dirX)._m21(dirY)
            ._m22(dirZ)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18)
        return this
    }

    fun billboardSpherical(objPos: Vector3f, targetPos: Vector3f, up: Vector3f): Matrix4f {
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
        _m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(upX)._m11(upY)._m12(upZ)._m13(0f)._m20(dirX)._m21(dirY)
            ._m22(dirZ)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18)
        return this
    }

    fun billboardSpherical(objPos: Vector3f, targetPos: Vector3f): Matrix4f {
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
        _m00(1f - q11)._m01(q01)._m02(-q13)._m03(0f)._m10(q01)._m11(1f - q00)._m12(q03)._m13(0f)._m20(q13)._m21(-q03)
            ._m22(1f - q11 - q00)._m23(0f)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1f)._properties(18)
        return this
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + m00.toRawBits()
        result = 31 * result + m01.toRawBits()
        result = 31 * result + m02.toRawBits()
        result = 31 * result + m03.toRawBits()
        result = 31 * result + m10.toRawBits()
        result = 31 * result + m11.toRawBits()
        result = 31 * result + m12.toRawBits()
        result = 31 * result + m13.toRawBits()
        result = 31 * result + m20.toRawBits()
        result = 31 * result + m21.toRawBits()
        result = 31 * result + m22.toRawBits()
        result = 31 * result + m23.toRawBits()
        result = 31 * result + m30.toRawBits()
        result = 31 * result + m31.toRawBits()
        result = 31 * result + m32.toRawBits()
        result = 31 * result + m33.toRawBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Matrix4f &&
                m00 == other.m00 && m01 == other.m01 && m02 == other.m02 && m03 == other.m03 &&
                m10 == other.m10 && m11 == other.m11 && m12 == other.m12 && m13 == other.m13 &&
                m20 == other.m20 && m21 == other.m21 && m22 == other.m22 && m23 == other.m23 &&
                m30 == other.m30 && m31 == other.m31 && m32 == other.m32 && m33 == other.m33
    }

    override fun equals1(other: Matrix, threshold: Double): Boolean {
        return equals(other as? Matrix4f, threshold.toFloat())
    }

    fun equals(m: Matrix4f?, delta: Float): Boolean {
        if (this === m) return true
        return m != null &&
                Runtime.equals(m00, m.m00, delta) && Runtime.equals(m01, m.m01, delta) &&
                Runtime.equals(m02, m.m02, delta) && Runtime.equals(m03, m.m03, delta) &&
                Runtime.equals(m10, m.m10, delta) && Runtime.equals(m11, m.m11, delta) &&
                Runtime.equals(m12, m.m12, delta) && Runtime.equals(m13, m.m13, delta) &&
                Runtime.equals(m20, m.m20, delta) && Runtime.equals(m21, m.m21, delta) &&
                Runtime.equals(m22, m.m22, delta) && Runtime.equals(m23, m.m23, delta) &&
                Runtime.equals(m30, m.m30, delta) && Runtime.equals(m31, m.m31, delta) &&
                Runtime.equals(m32, m.m32, delta) && Runtime.equals(m33, m.m33, delta)
    }

    @JvmOverloads
    fun pick(x: Float, y: Float, width: Float, height: Float, viewport: IntArray, dst: Matrix4f = this): Matrix4f {
        val sx = viewport[2].toFloat() / width
        val sy = viewport[3].toFloat() / height
        val tx = (viewport[2].toFloat() + 2f * (viewport[0].toFloat() - x)) / width
        val ty = (viewport[3].toFloat() + 2f * (viewport[1].toFloat() - y)) / height
        dst._m30(m00 * tx + m10 * ty + m30)._m31(m01 * tx + m11 * ty + m31)._m32(m02 * tx + m12 * ty + m32)._m33(
            m03 * tx + m13 * ty + m33
        )._m00(m00 * sx)._m01(m01 * sx)._m02(m02 * sx)._m03(m03 * sx)._m10(m10 * sy)._m11(m11 * sy)._m12(m12 * sy)._m13(
            m13 * sy
        )._properties(0)
        return dst
    }

    val isAffine: Boolean
        get() = m03 == 0f && m13 == 0f && m23 == 0f && m33 == 1f

    /*fun swap(other: Matrix4f): Matrix4f {
        MemUtil.INSTANCE.swap(this, other)
        val props = properties
        properties = other.properties()
        other.properties = props
        return this
    }*/

    @JvmOverloads
    fun arcball(
        radius: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        angleX: Float,
        angleY: Float,
        dst: Matrix4f = this
    ): Matrix4f {
        val m30 = m20 * -radius + m30
        val m31 = m21 * -radius + m31
        val m32 = m22 * -radius + m32
        val m33 = m23 * -radius + m33
        var sin = sin(angleX)
        var cos = cos(angleX)
        val nm10 = m10 * cos + m20 * sin
        val nm11 = m11 * cos + m21 * sin
        val nm12 = m12 * cos + m22 * sin
        val nm13 = m13 * cos + m23 * sin
        val m20 = m20 * cos - m10 * sin
        val m21 = m21 * cos - m11 * sin
        val m22 = m22 * cos - m12 * sin
        val m23 = m23 * cos - m13 * sin
        sin = sin(angleY)
        cos = cos(angleY)
        val nm00 = m00 * cos - m20 * sin
        val nm01 = m01 * cos - m21 * sin
        val nm02 = m02 * cos - m22 * sin
        val nm03 = m03 * cos - m23 * sin
        val nm20 = m00 * sin + m20 * cos
        val nm21 = m01 * sin + m21 * cos
        val nm22 = m02 * sin + m22 * cos
        val nm23 = m03 * sin + m23 * cos
        dst._m30(-nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30)
            ._m31(-nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31)._m32(
                -nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32
            )._m33(-nm03 * centerX - nm13 * centerY - nm23 * centerZ + m33)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._properties(
                flags and -14
            )
        return dst
    }

    fun arcball(radius: Float, center: Vector3f, angleX: Float, angleY: Float, dst: Matrix4f): Matrix4f {
        return arcball(radius, center.x, center.y, center.z, angleX, angleY, dst)
    }

    fun arcball(radius: Float, center: Vector3f, angleX: Float, angleY: Float): Matrix4f {
        return arcball(radius, center.x, center.y, center.z, angleX, angleY, this)
    }

    fun frustumAabb(min: Vector3f, max: Vector3f): Matrix4f {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (t in 0..7) {
            val x = (t and 1 shl 1).toFloat() - 1f
            val y = (t ushr 1 and 1 shl 1).toFloat() - 1f
            val z = (t ushr 2 and 1 shl 1).toFloat() - 1f
            val invW = 1f / (m03 * x + m13 * y + m23 * z + m33)
            val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
            val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
            val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
            minX = kotlin.math.min(minX, nx)
            minY = kotlin.math.min(minY, ny)
            minZ = kotlin.math.min(minZ, nz)
            maxX = kotlin.math.max(maxX, nx)
            maxY = kotlin.math.max(maxY, ny)
            maxZ = kotlin.math.max(maxZ, nz)
        }
        min.x = minX
        min.y = minY
        min.z = minZ
        max.x = maxX
        max.y = maxY
        max.z = maxZ
        return this
    }

    fun projectedGridRange(projector: Matrix4f, sLower: Float, sUpper: Float, dst: Matrix4f): Matrix4f? {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var intersection = false
        for (t in 0..11) {
            var c0X: Float
            var c0Y: Float
            var c0Z: Float
            var c1X: Float
            var c1Y: Float
            var c1Z: Float
            if (t < 4) {
                c0X = -1f
                c1X = 1f
                c1Y = (t and 1 shl 1).toFloat() - 1f
                c0Y = c1Y
                c1Z = (t ushr 1 and 1 shl 1).toFloat() - 1f
                c0Z = c1Z
            } else if (t < 8) {
                c0Y = -1f
                c1Y = 1f
                c1X = (t and 1 shl 1).toFloat() - 1f
                c0X = c1X
                c1Z = (t ushr 1 and 1 shl 1).toFloat() - 1f
                c0Z = c1Z
            } else {
                c0Z = -1f
                c1Z = 1f
                c1X = (t and 1 shl 1).toFloat() - 1f
                c0X = c1X
                c1Y = (t ushr 1 and 1 shl 1).toFloat() - 1f
                c0Y = c1Y
            }
            var invW = 1f / (m03 * c0X + m13 * c0Y + m23 * c0Z + m33)
            val p0x = (m00 * c0X + m10 * c0Y + m20 * c0Z + m30) * invW
            val p0y = (m01 * c0X + m11 * c0Y + m21 * c0Z + m31) * invW
            val p0z = (m02 * c0X + m12 * c0Y + m22 * c0Z + m32) * invW
            invW = 1f / (m03 * c1X + m13 * c1Y + m23 * c1Z + m33)
            val p1x = (m00 * c1X + m10 * c1Y + m20 * c1Z + m30) * invW
            val p1y = (m01 * c1X + m11 * c1Y + m21 * c1Z + m31) * invW
            val p1z = (m02 * c1X + m12 * c1Y + m22 * c1Z + m32) * invW
            val dirX = p1x - p0x
            val dirY = p1y - p0y
            val dirZ = p1z - p0z
            val invDenom = 1f / dirY
            for (s in 0..1) {
                val isectT = -(p0y + if (s == 0) sLower else sUpper) * invDenom
                if (isectT >= 0f && isectT <= 1f) {
                    intersection = true
                    val ix = p0x + isectT * dirX
                    val iz = p0z + isectT * dirZ
                    invW = 1f / (projector.m03 * ix + projector.m23 * iz + projector.m33)
                    val px = (projector.m00 * ix + projector.m20 * iz + projector.m30) * invW
                    val py = (projector.m01 * ix + projector.m21 * iz + projector.m31) * invW
                    minX = kotlin.math.min(minX, px)
                    minY = kotlin.math.min(minY, py)
                    maxX = kotlin.math.max(maxX, px)
                    maxY = kotlin.math.max(maxY, py)
                }
            }
        }
        return if (!intersection) {
            null
        } else {
            dst.set(maxX - minX, 0f, 0f, 0f, 0f, maxY - minY, 0f, 0f, 0f, 0f, 1f, 0f, minX, minY, 0f, 1f)
            dst._properties(2)
            dst
        }
    }

    fun perspectiveFrustumSlice(near: Float, far: Float, dst: Matrix4f): Matrix4f {
        val invOldNear = (m23 + m22) / m32
        val invNearFar = 1f / (near - far)
        dst._m00(m00 * invOldNear * near)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11 * invOldNear * near)._m12(
            m12
        )._m13(m13)._m20(m20)._m21(m21)._m22((far + near) * invNearFar)._m23(m23)._m30(m30)._m31(m31)
            ._m32((far + far) * near * invNearFar)._m33(
                m33
            )._properties(flags and -29)
        return dst
    }

    fun orthoCrop(view: Matrix4f, dst: Matrix4f): Matrix4f {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (t in 0..7) {
            val x = (t and 1 shl 1).toFloat() - 1f
            val y = (t ushr 1 and 1 shl 1).toFloat() - 1f
            val z = (t ushr 2 and 1 shl 1).toFloat() - 1f
            var invW = 1f / (m03 * x + m13 * y + m23 * z + m33)
            val wx = (m00 * x + m10 * y + m20 * z + m30) * invW
            val wy = (m01 * x + m11 * y + m21 * z + m31) * invW
            val wz = (m02 * x + m12 * y + m22 * z + m32) * invW
            invW = 1f / (view.m03 * wx + view.m13 * wy + view.m23 * wz + view.m33)
            val vx = view.m00 * wx + view.m10 * wy + view.m20 * wz + view.m30
            val vy = view.m01 * wx + view.m11 * wy + view.m21 * wz + view.m31
            val vz = (view.m02 * wx + view.m12 * wy + view.m22 * wz + view.m32) * invW
            minX = kotlin.math.min(minX, vx)
            maxX = kotlin.math.max(maxX, vx)
            minY = kotlin.math.min(minY, vy)
            maxY = kotlin.math.max(maxY, vy)
            minZ = kotlin.math.min(minZ, vz)
            maxZ = kotlin.math.max(maxZ, vz)
        }
        return dst.setOrtho(minX, maxX, minY, maxY, -maxZ, -minZ)
    }

    fun trapezoidCrop(
        p0x: Float, p0y: Float, p1x: Float,
        p1y: Float, p2x: Float, p2y: Float,
        p3x: Float, p3y: Float
    ): Matrix4f {
        val aX = p1y - p0y
        val aY = p0x - p1x
        var nm10 = -aX
        var nm30 = aX * p0y - aY * p0x
        var nm31 = -(aX * p0x + aY * p0y)
        val c3x = aY * p3x + nm10 * p3y + nm30
        val c3y = aX * p3x + aY * p3y + nm31
        val s = -c3x / c3y
        var nm00 = aY + s * aX
        nm10 += s * aY
        nm30 += s * nm31
        val d1x = nm00 * p1x + nm10 * p1y + nm30
        val d2x = nm00 * p2x + nm10 * p2y + nm30
        val d = d1x * c3y / (d2x - d1x)
        nm31 += d
        val sx = 2f / d2x
        val sy = 1f / (c3y + d)
        val u = (sy + sy) * d / (1f - sy * d)
        val m03 = aX * sy
        val m13 = aY * sy
        val m33 = nm31 * sy
        val nm01 = (u + 1f) * m03
        val nm11 = (u + 1f) * m13
        nm31 = (u + 1f) * m33 - u
        nm00 = sx * nm00 - m03
        nm10 = sx * nm10 - m13
        nm30 = sx * nm30 - m33
        set(nm00, nm01, 0f, m03, nm10, nm11, 0f, m13, 0f, 0f, 1f, 0f, nm30, nm31, 0f, m33)
        _properties(0)
        return this
    }

    fun transformAab(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        outMin: Vector3f, outMax: Vector3f
    ): Matrix4f {
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
        val xminx: Float
        val xmaxx: Float
        if (xax < xbx) {
            xminx = xax
            xmaxx = xbx
        } else {
            xminx = xbx
            xmaxx = xax
        }
        val xminy: Float
        val xmaxy: Float
        if (xay < xby) {
            xminy = xay
            xmaxy = xby
        } else {
            xminy = xby
            xmaxy = xay
        }
        val xminz: Float
        val xmaxz: Float
        if (xaz < xbz) {
            xminz = xaz
            xmaxz = xbz
        } else {
            xminz = xbz
            xmaxz = xaz
        }
        val yminx: Float
        val ymaxx: Float
        if (yax < ybx) {
            yminx = yax
            ymaxx = ybx
        } else {
            yminx = ybx
            ymaxx = yax
        }
        val yminy: Float
        val ymaxy: Float
        if (yay < yby) {
            yminy = yay
            ymaxy = yby
        } else {
            yminy = yby
            ymaxy = yay
        }
        val yminz: Float
        val ymaxz: Float
        if (yaz < ybz) {
            yminz = yaz
            ymaxz = ybz
        } else {
            yminz = ybz
            ymaxz = yaz
        }
        val zminx: Float
        val zmaxx: Float
        if (zax < zbx) {
            zminx = zax
            zmaxx = zbx
        } else {
            zminx = zbx
            zmaxx = zax
        }
        val zminy: Float
        val zmaxy: Float
        if (zay < zby) {
            zminy = zay
            zmaxy = zby
        } else {
            zminy = zby
            zmaxy = zay
        }
        val zminz: Float
        val zmaxz: Float
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

    fun transformAab(min: Vector3f, max: Vector3f, outMin: Vector3f, outMax: Vector3f): Matrix4f {
        return transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax)
    }

    fun mix(other: Matrix4f, t: Float, dst: Matrix4f = this): Matrix4f {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m02 = (other.m02 - m02) * t + m02
        dst.m03 = (other.m03 - m03) * t + m03
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        dst.m12 = (other.m12 - m12) * t + m12
        dst.m13 = (other.m13 - m13) * t + m13
        dst.m20 = (other.m20 - m20) * t + m20
        dst.m21 = (other.m21 - m21) * t + m21
        dst.m22 = (other.m22 - m22) * t + m22
        dst.m23 = (other.m23 - m23) * t + m23
        dst.m30 = (other.m30 - m30) * t + m30
        dst.m31 = (other.m31 - m31) * t + m31
        dst.m32 = (other.m32 - m32) * t + m32
        dst.m33 = (other.m33 - m33) * t + m33
        dst.flags = flags and other.properties()
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix4f, t: Float, dst: Matrix4f = this): Matrix4f {
        return mix(other, t, dst)
    }

    fun rotateTowards(dir: Vector3f, up: Vector3f, dst: Matrix4f): Matrix4f {
        return rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    fun rotateTowards(dir: Vector3f, up: Vector3f): Matrix4f {
        return rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float,
        dst: Matrix4f = this
    ): Matrix4f {
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
        dst._m30(m30)._m31(m31)._m32(m32)._m33(m33)
        val nm00 = m00 * leftX + m10 * leftY + m20 * leftZ
        val nm01 = m01 * leftX + m11 * leftY + m21 * leftZ
        val nm02 = m02 * leftX + m12 * leftY + m22 * leftZ
        val nm03 = m03 * leftX + m13 * leftY + m23 * leftZ
        val nm10 = m00 * upnX + m10 * upnY + m20 * upnZ
        val nm11 = m01 * upnX + m11 * upnY + m21 * upnZ
        val nm12 = m02 * upnX + m12 * upnY + m22 * upnZ
        val nm13 = m03 * upnX + m13 * upnY + m23 * upnZ
        dst._m20(m00 * ndirX + m10 * ndirY + m20 * ndirZ)._m21(m01 * ndirX + m11 * ndirY + m21 * ndirZ)._m22(
            m02 * ndirX + m12 * ndirY + m22 * ndirZ
        )._m23(m03 * ndirX + m13 * ndirY + m23 * ndirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)
            ._m11(nm11)._m12(nm12)._m13(nm13)._properties(
                flags and -14
            )
        return dst
    }

    fun rotationTowards(dir: Vector3f, up: Vector3f): Matrix4f {
        return rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Float, dirY: Float, dirZ: Float, upX: Float, upY: Float, upZ: Float): Matrix4f {
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
        identity()
        _m00(leftX)._m01(leftY)._m02(leftZ)._m10(upnX)._m11(upnY)._m12(upnZ)._m20(ndirX)._m21(ndirY)._m22(ndirZ)
            ._properties(18)
        return this
    }

    fun translationRotateTowards(pos: Vector3f, dir: Vector3f, up: Vector3f): Matrix4f {
        return translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun translationRotateTowards(
        posX: Float, posY: Float, posZ: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4f {
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
        _m00(leftX)._m01(leftY)._m02(leftZ)._m03(0f)._m10(upnX)._m11(upnY)._m12(upnZ)._m13(0f)._m20(ndirX)._m21(ndirY)
            ._m22(ndirZ)._m23(0f)._m30(posX)._m31(posY)._m32(posZ)._m33(1f)._properties(18)
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

    fun affineSpan(corner: Vector3f, xDir: Vector3f, yDir: Vector3f, zDir: Vector3f): Matrix4f {
        val a = m10 * m22
        val b = m10 * m21
        val c = m10 * m02
        val d = m10 * m01
        val e = m11 * m22
        val f = m11 * m20
        val g = m11 * m02
        val h = m11 * m00
        val i = m12 * m21
        val j = m12 * m20
        val k = m12 * m01
        val l = m12 * m00
        val m = m20 * m02
        val n = m20 * m01
        val o = m21 * m02
        val p = m21 * m00
        val q = m22 * m01
        val r = m22 * m00
        val s = 1f / (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
        val nm00 = (e - i) * s
        val nm01 = (o - q) * s
        val nm02 = (k - g) * s
        val nm10 = (j - a) * s
        val nm11 = (r - m) * s
        val nm12 = (c - l) * s
        val nm20 = (b - f) * s
        val nm21 = (n - p) * s
        val nm22 = (h - d) * s
        corner.x = -nm00 - nm10 - nm20 + (a * m31 - b * m32 + f * m32 - e * m30 + i * m30 - j * m31) * s
        corner.y = -nm01 - nm11 - nm21 + (m * m31 - n * m32 + p * m32 - o * m30 + q * m30 - r * m31) * s
        corner.z = -nm02 - nm12 - nm22 + (g * m30 - k * m30 + l * m31 - c * m31 + d * m32 - h * m32) * s
        xDir.x = 2f * nm00
        xDir.y = 2f * nm01
        xDir.z = 2f * nm02
        yDir.x = 2f * nm10
        yDir.y = 2f * nm11
        yDir.z = 2f * nm12
        zDir.x = 2f * nm20
        zDir.y = 2f * nm21
        zDir.z = 2f * nm22
        return this
    }

    fun testPoint(x: Float, y: Float, z: Float): Boolean {
        val nxX = m03 + m00
        val nxY = m13 + m10
        val nxZ = m23 + m20
        val nxW = m33 + m30
        val pxX = m03 - m00
        val pxY = m13 - m10
        val pxZ = m23 - m20
        val pxW = m33 - m30
        val nyX = m03 + m01
        val nyY = m13 + m11
        val nyZ = m23 + m21
        val nyW = m33 + m31
        val pyX = m03 - m01
        val pyY = m13 - m11
        val pyZ = m23 - m21
        val pyW = m33 - m31
        val nzX = m03 + m02
        val nzY = m13 + m12
        val nzZ = m23 + m22
        val nzW = m33 + m32
        val pzX = m03 - m02
        val pzY = m13 - m12
        val pzZ = m23 - m22
        val pzW = m33 - m32
        return nxX * x + nxY * y + nxZ * z + nxW >= 0f && pxX * x + pxY * y + pxZ * z + pxW >= 0f &&
                nyX * x + nyY * y + nyZ * z + nyW >= 0f && pyX * x + pyY * y + pyZ * z + pyW >= 0f &&
                nzX * x + nzY * y + nzZ * z + nzW >= 0f && pzX * x + pzY * y + pzZ * z + pzW >= 0f
    }

    fun testSphere(x: Float, y: Float, z: Float, r: Float): Boolean {
        var nxX = m03 + m00
        var nxY = m13 + m10
        var nxZ = m23 + m20
        var nxW = m33 + m30
        var invl = JomlMath.invsqrt(nxX * nxX + nxY * nxY + nxZ * nxZ)
        nxX *= invl
        nxY *= invl
        nxZ *= invl
        nxW *= invl
        var pxX = m03 - m00
        var pxY = m13 - m10
        var pxZ = m23 - m20
        var pxW = m33 - m30
        invl = JomlMath.invsqrt(pxX * pxX + pxY * pxY + pxZ * pxZ)
        pxX *= invl
        pxY *= invl
        pxZ *= invl
        pxW *= invl
        var nyX = m03 + m01
        var nyY = m13 + m11
        var nyZ = m23 + m21
        var nyW = m33 + m31
        invl = JomlMath.invsqrt(nyX * nyX + nyY * nyY + nyZ * nyZ)
        nyX *= invl
        nyY *= invl
        nyZ *= invl
        nyW *= invl
        var pyX = m03 - m01
        var pyY = m13 - m11
        var pyZ = m23 - m21
        var pyW = m33 - m31
        invl = JomlMath.invsqrt(pyX * pyX + pyY * pyY + pyZ * pyZ)
        pyX *= invl
        pyY *= invl
        pyZ *= invl
        pyW *= invl
        var nzX = m03 + m02
        var nzY = m13 + m12
        var nzZ = m23 + m22
        var nzW = m33 + m32
        invl = JomlMath.invsqrt(nzX * nzX + nzY * nzY + nzZ * nzZ)
        nzX *= invl
        nzY *= invl
        nzZ *= invl
        nzW *= invl
        var pzX = m03 - m02
        var pzY = m13 - m12
        var pzZ = m23 - m22
        var pzW = m33 - m32
        invl = JomlMath.invsqrt(pzX * pzX + pzY * pzY + pzZ * pzZ)
        pzX *= invl
        pzY *= invl
        pzZ *= invl
        pzW *= invl
        return nxX * x + nxY * y + nxZ * z + nxW >= -r && pxX * x + pxY * y + pxZ * z + pxW >= -r &&
                nyX * x + nyY * y + nyZ * z + nyW >= -r && pyX * x + pyY * y + pyZ * z + pyW >= -r &&
                nzX * x + nzY * y + nzZ * z + nzW >= -r && pzX * x + pzY * y + pzZ * z + pzW >= -r
    }

    fun testAab(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): Boolean {
        val nxX = m03 + m00
        val nxY = m13 + m10
        val nxZ = m23 + m20
        val nxW = m33 + m30
        val pxX = m03 - m00
        val pxY = m13 - m10
        val pxZ = m23 - m20
        val pxW = m33 - m30
        val nyX = m03 + m01
        val nyY = m13 + m11
        val nyZ = m23 + m21
        val nyW = m33 + m31
        val pyX = m03 - m01
        val pyY = m13 - m11
        val pyZ = m23 - m21
        val pyW = m33 - m31
        val nzX = m03 + m02
        val nzY = m13 + m12
        val nzZ = m23 + m22
        val nzW = m33 + m32
        val pzX = m03 - m02
        val pzY = m13 - m12
        val pzZ = m23 - m22
        val pzW = m33 - m32
        return nxX * (if (nxX < 0f) minX else maxX) + nxY * (if (nxY < 0f) minY else maxY) + nxZ * (if (nxZ < 0f) minZ else maxZ) >= -nxW &&
                pxX * (if (pxX < 0f) minX else maxX) + pxY * (if (pxY < 0f) minY else maxY) + pxZ * (if (pxZ < 0f) minZ else maxZ) >= -pxW &&
                nyX * (if (nyX < 0f) minX else maxX) + nyY * (if (nyY < 0f) minY else maxY) + nyZ * (if (nyZ < 0f) minZ else maxZ) >= -nyW &&
                pyX * (if (pyX < 0f) minX else maxX) + pyY * (if (pyY < 0f) minY else maxY) + pyZ * (if (pyZ < 0f) minZ else maxZ) >= -pyW &&
                nzX * (if (nzX < 0f) minX else maxX) + nzY * (if (nzY < 0f) minY else maxY) + nzZ * (if (nzZ < 0f) minZ else maxZ) >= -nzW &&
                pzX * (if (pzX < 0f) minX else maxX) + pzY * (if (pzY < 0f) minY else maxY) + pzZ * (if (pzZ < 0f) minZ else maxZ) >= -pzW
    }

    fun obliqueZ(a: Float, b: Float): Matrix4f {
        return _m20(m00 * a + m10 * b + m20)._m21(m01 * a + m11 * b + m21)._m22(m02 * a + m12 * b + m22)._properties(
            flags and 2
        )
    }

    fun obliqueZ(a: Float, b: Float, dst: Matrix4f): Matrix4f {
        dst._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(
            m00 * a + m10 * b + m20
        )._m21(m01 * a + m11 * b + m21)._m22(m02 * a + m12 * b + m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(
                flags and 2
            )
        return dst
    }

    fun withLookAtUp(up: Vector3f): Matrix4f {
        return withLookAtUp(up.x, up.y, up.z, this)
    }

    fun withLookAtUp(up: Vector3f, dst: Matrix4f = this): Matrix4f {
        return withLookAtUp(up.x, up.y, up.z, dst)
    }

    @JvmOverloads
    fun withLookAtUp(upX: Float, upY: Float, upZ: Float, dst: Matrix4f = this): Matrix4f {
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
            dst._m02(m02)._m12(m12)._m22(m22)._m32(m32)._m03(m03)._m13(m13)._m23(m23)._m33(m33)
        }
        dst._properties(flags and -14)
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) && JomlMath.isFinite(m03) &&
                JomlMath.isFinite(m10) && JomlMath.isFinite(m11) && JomlMath.isFinite(m12) && JomlMath.isFinite(m13) &&
                JomlMath.isFinite(m20) && JomlMath.isFinite(m21) && JomlMath.isFinite(m22) && JomlMath.isFinite(m23) &&
                JomlMath.isFinite(m30) && JomlMath.isFinite(m31) && JomlMath.isFinite(m32) && JomlMath.isFinite(m33)

    fun skew(v: Vector2f) {
        mul3x3(// works
            1f, v.y, 0f,
            v.x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun skew(x: Float, y: Float) {
        mul3x3(// works
            1f, y, 0f,
            x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun isIdentity(): Boolean {
        return flags.and(PROPERTY_IDENTITY) != 0
    }

    companion object {
        const val PROPERTY_IDENTITY = 4

        @JvmStatic
        fun projViewFromRectangle(
            eye: Vector3f,
            p: Vector3f,
            x: Vector3f,
            y: Vector3f,
            nearFarDist: Float,
            zeroToOne: Boolean,
            projdst: Matrix4f,
            viewdst: Matrix4f
        ) {
            var zx = y.y * x.z - y.z * x.y
            var zy = y.z * x.x - y.x * x.z
            var zz = y.x * x.y - y.y * x.x
            var zd = zx * (p.x - eye.x) + zy * (p.y - eye.y) + zz * (p.z - eye.z)
            val zs = if (zd >= 0f) 1f else -1f
            zx *= zs
            zy *= zs
            zz *= zs
            zd *= zs
            viewdst.setLookAt(eye.x, eye.y, eye.z, eye.x + zx, eye.y + zy, eye.z + zz, y.x, y.y, y.z)
            val px = viewdst.m00 * p.x + viewdst.m10 * p.y + viewdst.m20 * p.z + viewdst.m30
            val py = viewdst.m01 * p.x + viewdst.m11 * p.y + viewdst.m21 * p.z + viewdst.m31
            val tx = viewdst.m00 * x.x + viewdst.m10 * x.y + viewdst.m20 * x.z
            val ty = viewdst.m01 * y.x + viewdst.m11 * y.y + viewdst.m21 * y.z
            val len = sqrt(zx * zx + zy * zy + zz * zz)
            var near = zd / len
            val far: Float
            if ((nearFarDist.isInfinite()) && nearFarDist < 0f) {
                far = near
                near = Float.POSITIVE_INFINITY
            } else if ((nearFarDist.isInfinite()) && nearFarDist > 0f) {
                far = Float.POSITIVE_INFINITY
            } else if (nearFarDist < 0f) {
                far = near
                near += nearFarDist
            } else {
                far = near + nearFarDist
            }
            projdst.setFrustum(px, px + tx, py, py + ty, near, far, zeroToOne)
        }
    }
}