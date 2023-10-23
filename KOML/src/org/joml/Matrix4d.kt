package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import kotlin.math.*

@Suppress("unused")
open class Matrix4d {

    var m00 = 0.0
    var m01 = 0.0
    var m02 = 0.0
    var m03 = 0.0
    var m10 = 0.0
    var m11 = 0.0
    var m12 = 0.0
    var m13 = 0.0
    var m20 = 0.0
    var m21 = 0.0
    var m22 = 0.0
    var m23 = 0.0
    var m30 = 0.0
    var m31 = 0.0
    var m32 = 0.0
    var m33 = 0.0

    var properties = 0

    constructor() {
        _m00(1.0)._m11(1.0)._m22(1.0)._m33(1.0).properties = 30
    }

    constructor(mat: Matrix4d) {
        this.set(mat)
    }

    constructor(mat: Matrix4f) {
        this.set(mat)
    }

    constructor(mat: Matrix4x3d) {
        this.set(mat)
    }

    constructor(mat: Matrix4x3f) {
        this.set(mat)
    }

    constructor(mat: Matrix3d) {
        this.set(mat)
    }

    constructor(
        m00: Double, m01: Double, m02: Double, m03: Double,
        m10: Double, m11: Double, m12: Double, m13: Double,
        m20: Double, m21: Double, m22: Double, m23: Double,
        m30: Double, m31: Double, m32: Double, m33: Double
    ) : super() {
        this.m00 = m00
        this.m01 = m01
        this.m02 = m02
        this.m03 = m03
        this.m10 = m10
        this.m11 = m11
        this.m12 = m12
        this.m13 = m13
        this.m20 = m20
        this.m21 = m21
        this.m22 = m22
        this.m23 = m23
        this.m30 = m30
        this.m31 = m31
        this.m32 = m32
        this.m33 = m33
        determineProperties()
    }

    constructor(col0: Vector4d, col1: Vector4d, col2: Vector4d, col3: Vector4d) {
        set(col0, col1, col2, col3)
    }

    fun assume(properties: Int): Matrix4d {
        this.properties = properties.toByte().toInt()
        return this
    }

    fun determineProperties(): Matrix4d {
        var properties = 0
        if (m03 == 0.0 && m13 == 0.0) {
            if (m23 == 0.0 && m33 == 1.0) {
                properties = properties or 2
                if (m00 == 1.0 && m01 == 0.0 && m02 == 0.0 && m10 == 0.0 && m11 == 1.0 && m12 == 0.0 && m20 == 0.0 && m21 == 0.0 && m22 == 1.0) {
                    properties = properties or 24
                    if (m30 == 0.0 && m31 == 0.0 && m32 == 0.0) {
                        properties = properties or 4
                    }
                }
            } else if (m01 == 0.0 && m02 == 0.0 && m10 == 0.0 && m12 == 0.0 && m20 == 0.0 && m21 == 0.0 && m30 == 0.0 && m31 == 0.0 && m33 == 0.0) {
                properties = properties or 1
            }
        }
        this.properties = properties
        return this
    }

    fun properties(): Int {
        return properties
    }

    fun m00(m00: Double): Matrix4d {
        this.m00 = m00
        properties = properties and -17
        if (m00 != 1.0) {
            properties = properties and -13
        }
        return this
    }

    fun m01(m01: Double): Matrix4d {
        this.m01 = m01
        properties = properties and -17
        if (m01 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m02(m02: Double): Matrix4d {
        this.m02 = m02
        properties = properties and -17
        if (m02 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m03(m03: Double): Matrix4d {
        this.m03 = m03
        if (m03 != 0.0) {
            properties = 0
        }
        return this
    }

    fun m10(m10: Double): Matrix4d {
        this.m10 = m10
        properties = properties and -17
        if (m10 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m11(m11: Double): Matrix4d {
        this.m11 = m11
        properties = properties and -17
        if (m11 != 1.0) {
            properties = properties and -13
        }
        return this
    }

    fun m12(m12: Double): Matrix4d {
        this.m12 = m12
        properties = properties and -17
        if (m12 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m13(m13: Double): Matrix4d {
        this.m13 = m13
        if (m03 != 0.0) {
            properties = 0
        }
        return this
    }

    fun m20(m20: Double): Matrix4d {
        this.m20 = m20
        properties = properties and -17
        if (m20 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m21(m21: Double): Matrix4d {
        this.m21 = m21
        properties = properties and -17
        if (m21 != 0.0) {
            properties = properties and -14
        }
        return this
    }

    fun m22(m22: Double): Matrix4d {
        this.m22 = m22
        properties = properties and -17
        if (m22 != 1.0) {
            properties = properties and -13
        }
        return this
    }

    fun m23(m23: Double): Matrix4d {
        this.m23 = m23
        if (m23 != 0.0) {
            properties = properties and -31
        }
        return this
    }

    fun m30(m30: Double): Matrix4d {
        this.m30 = m30
        if (m30 != 0.0) {
            properties = properties and -6
        }
        return this
    }

    fun m31(m31: Double): Matrix4d {
        this.m31 = m31
        if (m31 != 0.0) {
            properties = properties and -6
        }
        return this
    }

    fun m32(m32: Double): Matrix4d {
        this.m32 = m32
        if (m32 != 0.0) {
            properties = properties and -6
        }
        return this
    }

    fun m33(m33: Double): Matrix4d {
        this.m33 = m33
        if (m33 != 0.0) {
            properties = properties and -2
        }
        if (m33 != 1.0) {
            properties = properties and -31
        }
        return this
    }

    fun _properties(properties: Int): Matrix4d {
        this.properties = properties
        return this
    }

    fun _m00(m00: Double): Matrix4d {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Double): Matrix4d {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Double): Matrix4d {
        this.m02 = m02
        return this
    }

    fun _m03(m03: Double): Matrix4d {
        this.m03 = m03
        return this
    }

    fun _m10(m10: Double): Matrix4d {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Double): Matrix4d {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Double): Matrix4d {
        this.m12 = m12
        return this
    }

    fun _m13(m13: Double): Matrix4d {
        this.m13 = m13
        return this
    }

    fun _m20(m20: Double): Matrix4d {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Double): Matrix4d {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Double): Matrix4d {
        this.m22 = m22
        return this
    }

    fun _m23(m23: Double): Matrix4d {
        this.m23 = m23
        return this
    }

    fun _m30(m30: Double): Matrix4d {
        this.m30 = m30
        return this
    }

    fun _m31(m31: Double): Matrix4d {
        this.m31 = m31
        return this
    }

    fun _m32(m32: Double): Matrix4d {
        this.m32 = m32
        return this
    }

    fun _m33(m33: Double): Matrix4d {
        this.m33 = m33
        return this
    }

    fun identity(): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
            properties = 30
        }
        return this
    }

    private fun _identity() {
        _m00(1.0)._m10(0.0)._m20(0.0)._m30(0.0)
            ._m01(0.0)._m11(1.0)._m21(0.0)._m31(0.0)
            ._m02(0.0)._m12(0.0)._m22(1.0)._m32(0.0)
            ._m03(0.0)._m13(0.0)._m23(0.0)._m33(1.0)
    }

    fun set(m: Matrix4d): Matrix4d {
        return _m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(m.m03)._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(m.m13)
            ._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(m.m23)._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(m.m33)
            ._properties(m.properties())
    }

    fun set(m: Matrix4f): Matrix4d {
        return _m00(m.m00.toDouble())._m01(m.m01.toDouble())._m02(m.m02.toDouble())._m03(m.m03.toDouble())
            ._m10(m.m10.toDouble())._m11(m.m11.toDouble())._m12(m.m12.toDouble())._m13(m.m13.toDouble())
            ._m20(m.m20.toDouble())._m21(m.m21.toDouble())._m22(m.m22.toDouble())._m23(m.m23.toDouble())
            ._m30(m.m30.toDouble())._m31(m.m31.toDouble())._m32(m.m32.toDouble())._m33(m.m33.toDouble())
            ._properties(m.properties())
    }

    fun setTransposed(m: Matrix4d): Matrix4d {
        return if (m.properties() and 4 != 0) identity() else setTransposedInternal(m)
    }

    private fun setTransposedInternal(m: Matrix4d): Matrix4d {
        val nm10 = m.m01
        val nm12 = m.m21
        val nm13 = m.m31
        val nm20 = m.m02
        val nm21 = m.m12
        val nm30 = m.m03
        val nm31 = m.m13
        val nm32 = m.m23
        return _m00(m.m00)._m01(m.m10)._m02(m.m20)._m03(m.m30)
            ._m10(nm10)._m11(m.m11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(m.m22)._m23(m.m32)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(m.m33)
            ._properties(m.properties() and 4)
    }

    fun set(m: Matrix4x3d): Matrix4d {
        return _m00(m.m00)._m01(m.m01)._m02(m.m02)._m03(0.0)
            ._m10(m.m10)._m11(m.m11)._m12(m.m12)._m13(0.0)
            ._m20(m.m20)._m21(m.m21)._m22(m.m22)._m23(0.0)
            ._m30(m.m30)._m31(m.m31)._m32(m.m32)._m33(1.0)
            ._properties(m.properties() or 2)
    }

    fun set(m: Matrix4x3f): Matrix4d {
        return _m00(m.m00.toDouble())._m01(m.m01.toDouble())._m02(m.m02.toDouble())._m03(0.0)
            ._m10(m.m10.toDouble())._m11(m.m11.toDouble())._m12(m.m12.toDouble())._m13(0.0)
            ._m20(m.m20.toDouble())._m21(m.m21.toDouble())._m22(m.m22.toDouble())._m23(0.0)
            ._m30(m.m30.toDouble())._m31(m.m31.toDouble())._m32(m.m32.toDouble())._m33(1.0)
            ._properties(m.properties() or 2)
    }

    fun set(mat: Matrix3d): Matrix4d {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m03(0.0)
            ._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m13(0.0)
            ._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m23(0.0)
            ._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0)
            ._properties(2)
    }

    fun set3x3(mat: Matrix4d): Matrix4d {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)
            ._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)
            ._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)
            ._properties(properties and mat.properties() and -2)
    }

    fun set4x3(mat: Matrix4x3d): Matrix4d {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)
            ._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)
            ._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)
            ._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)
            ._properties(properties and mat.properties() and -2)
    }

    fun set4x3(mat: Matrix4x3f): Matrix4d {
        return _m00(mat.m00.toDouble())._m01(mat.m01.toDouble())._m02(mat.m02.toDouble())
            ._m10(mat.m10.toDouble())._m11(mat.m11.toDouble())._m12(mat.m12.toDouble())
            ._m20(mat.m20.toDouble())._m21(mat.m21.toDouble())._m22(mat.m22.toDouble())
            ._m30(mat.m30.toDouble())._m31(mat.m31.toDouble())._m32(mat.m32.toDouble())
            ._properties(properties and mat.properties() and -2)
    }

    fun set4x3(mat: Matrix4d): Matrix4d {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)
            ._m20(mat.m20)._m21(mat.m21)._m22(mat.m22)._m30(mat.m30)._m31(mat.m31)._m32(mat.m32)
            ._properties(properties and mat.properties() and -2)
    }

    fun set(axisAngle: AxisAngle4f): Matrix4d {
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
        _m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc)
        var tmp1 = x * y * omc
        var tmp2 = z * s
        _m10(tmp1 - tmp2)._m01(tmp1 + tmp2)
        tmp1 = x * z * omc
        tmp2 = y * s
        _m20(tmp1 + tmp2)._m02(tmp1 - tmp2)
        tmp1 = y * z * omc
        tmp2 = x * s
        _m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0.0)._m13(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)
            ._m33(1.0).properties = 18
        return this
    }

    fun set(axisAngle: AxisAngle4d): Matrix4d {
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
        _m00(c + x * x * omc)._m11(c + y * y * omc)._m22(c + z * z * omc)
        var tmp1 = x * y * omc
        var tmp2 = z * s
        _m10(tmp1 - tmp2)._m01(tmp1 + tmp2)
        tmp1 = x * z * omc
        tmp2 = y * s
        _m20(tmp1 + tmp2)._m02(tmp1 - tmp2)
        tmp1 = y * z * omc
        tmp2 = x * s
        _m21(tmp1 - tmp2)._m12(tmp1 + tmp2)._m03(0.0)._m13(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)
            ._m33(1.0).properties = 18
        return this
    }

    fun set(q: Quaternionf): Matrix4d {
        return this.rotation(q)
    }

    fun set(q: Quaterniond): Matrix4d {
        return this.rotation(q)
    }

    @JvmOverloads
    fun mul(right: Matrix4d, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(right)
        } else if (right.properties() and 4 != 0) {
            dest.set(this)
        } else if (properties and 8 != 0 && right.properties() and 2 != 0) {
            mulTranslationAffine(right, dest)
        } else if (properties and 2 != 0 && right.properties() and 2 != 0) {
            this.mulAffine(right, dest)
        } else if (properties and 1 != 0 && right.properties() and 2 != 0) {
            this.mulPerspectiveAffine(right, dest)
        } else {
            if (right.properties() and 2 != 0) mulAffineR(right, dest) else mul0(right, dest)
        }
    }

    @JvmOverloads
    fun mul0(right: Matrix4d, dest: Matrix4d = this): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(0)
    }

    @JvmOverloads
    fun mul(
        r00: Double, r01: Double, r02: Double, r03: Double,
        r10: Double, r11: Double, r12: Double, r13: Double,
        r20: Double, r21: Double, r22: Double, r23: Double,
        r30: Double, r31: Double, r32: Double, r33: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33)
        } else {
            if (properties and 2 != 0) mulAffineL(
                r00, r01, r02, r03,
                r10, r11, r12, r13,
                r20, r21, r22, r23,
                r30, r31, r32, r33, dest
            ) else this.mulGeneric(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33, dest)
        }
    }

    private fun mulAffineL(
        r00: Double, r01: Double, r02: Double, r03: Double,
        r10: Double, r11: Double, r12: Double, r13: Double,
        r20: Double, r21: Double, r22: Double, r23: Double,
        r30: Double, r31: Double, r32: Double, r33: Double,
        dest: Matrix4d
    ): Matrix4d {
        val nm00 = m00 * r00 * m10 * r01 * m20 * r02 * m30 * r03
        val nm01 = m01 * r00 * m11 * r01 * m21 * r02 * m31 * r03
        val nm02 = m02 * r00 * m12 * r01 * m22 * r02 * m32 * r03
        val nm10 = m00 * r10 * m10 * r11 * m20 * r12 * m30 * r13
        val nm11 = m01 * r10 * m11 * r11 * m21 * r12 * m31 * r13
        val nm12 = m02 * r10 * m12 * r11 * m22 * r12 * m32 * r13
        val nm20 = m00 * r20 * m10 * r21 * m20 * r22 * m30 * r23
        val nm21 = m01 * r20 * m11 * r21 * m21 * r22 * m31 * r23
        val nm22 = m02 * r20 * m12 * r21 * m22 * r22 * m32 * r23
        val nm30 = m00 * r30 * m10 * r31 * m20 * r32 * m30 * r33
        val nm31 = m01 * r30 * m11 * r31 * m21 * r32 * m31 * r33
        val nm32 = m02 * r30 * m12 * r31 * m22 * r32 * m32 * r33
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(r03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(r13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(r23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(r33)
            ._properties(2)
    }

    private fun mulGeneric(
        r00: Double, r01: Double, r02: Double, r03: Double,
        r10: Double, r11: Double, r12: Double, r13: Double,
        r20: Double, r21: Double, r22: Double, r23: Double,
        r30: Double, r31: Double, r32: Double, r33: Double,
        dest: Matrix4d
    ): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mul3x3(
        r00: Double, r01: Double, r02: Double,
        r10: Double, r11: Double, r12: Double,
        r20: Double, r21: Double, r22: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0)
            dest.set(r00, r01, r02, 0.0, r10, r11, r12, 0.0, r20, r21, r22, 0.0, 0.0, 0.0, 0.0, 1.0)
        else mulGeneric3x3(r00, r01, r02, r10, r11, r12, r20, r21, r22, dest)
    }

    private fun mulGeneric3x3(
        r00: Double, r01: Double, r02: Double, r10: Double, r11: Double, r12: Double,
        r20: Double, r21: Double, r22: Double, dest: Matrix4d
    ): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 2)
    }

    @JvmOverloads
    fun mulLocal(left: Matrix4d, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(left)
        } else if (left.properties() and 4 != 0) {
            dest.set(this)
        } else {
            if (properties and 2 != 0 && left.properties() and 2 != 0) mulLocalAffine(
                left,
                dest
            ) else mulLocalGeneric(left, dest)
        }
    }

    private fun mulLocalGeneric(left: Matrix4d, dest: Matrix4d): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun mulLocalAffine(left: Matrix4d, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(2)
        return dest
    }

    @JvmOverloads
    fun mul(right: Matrix4x3d, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(right)
        } else if (right.properties() and 4 != 0) {
            dest.set(this)
        } else if (properties and 8 != 0) {
            mulTranslation(right, dest)
        } else if (properties and 2 != 0) {
            this.mulAffine(right, dest)
        } else {
            if (properties and 1 != 0) this.mulPerspectiveAffine(right, dest) else this.mulGeneric(right, dest)
        }
    }

    private fun mulTranslation(right: Matrix4x3d, dest: Matrix4d): Matrix4d {
        return dest._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(m03)
            ._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(m13)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(m23)
            ._m30(right.m30 + m30)._m31(right.m31 + m31)._m32(right.m32 + m32)._m33(m33)
            ._properties(2 or (right.properties() and 16))
    }

    private fun mulAffine(right: Matrix4x3d, dest: Matrix4d): Matrix4d {
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
        return dest
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)
            ._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)._m03(m03)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)
            ._m11(m01 * rm10 + m11 * rm11 + m21 * rm12)
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)._m13(m13)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(m23)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)._m33(m33)
            ._properties(2 or (properties and right.properties() and 16))
    }

    private fun mulGeneric(right: Matrix4x3d, dest: Matrix4d): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(
                properties and -30
            )
        return dest
    }

    fun mulPerspectiveAffine(view: Matrix4x3d, dest: Matrix4d): Matrix4d {
        val lm00 = m00
        val lm11 = m11
        val lm22 = m22
        val lm23 = m23
        dest._m00(lm00 * view.m00)._m01(lm11 * view.m01)._m02(lm22 * view.m02)._m03(lm23 * view.m02)
            ._m10(lm00 * view.m10)._m11(lm11 * view.m11)._m12(lm22 * view.m12)._m13(lm23 * view.m12)
            ._m20(lm00 * view.m20)._m21(lm11 * view.m21)._m22(lm22 * view.m22)._m23(lm23 * view.m22)
            ._m30(lm00 * view.m30)._m31(lm11 * view.m31)._m32(lm22 * view.m32 + m32)._m33(lm23 * view.m32)
            ._properties(0)
        return dest
    }

    fun mul(right: Matrix4x3f, dest: Matrix4d): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(right)
        } else {
            if (right.properties() and 4 != 0) dest.set(this) else this.mulGeneric(right, dest)
        }
    }

    private fun mulGeneric(right: Matrix4x3f, dest: Matrix4d): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(properties and -30)
        return dest
    }

    @JvmOverloads
    fun mul(right: Matrix3x2d, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(properties and -30)
        return dest
    }

    @JvmOverloads
    fun mul(right: Matrix3x2f, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(properties and -30)
        return dest
    }

    @JvmOverloads
    fun mul(right: Matrix4f, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.set(right)
        } else {
            if (right.properties() and 4 != 0) dest.set(this) else this.mulGeneric(right, dest)
        }
    }

    private fun mulGeneric(right: Matrix4f, dest: Matrix4d): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(0)
        return dest
    }

    @JvmOverloads
    fun mulPerspectiveAffine(view: Matrix4d, dest: Matrix4d = this): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(0)
    }

    @JvmOverloads
    fun mulAffineR(right: Matrix4d, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)
            ._properties(properties and -30)
        return dest
    }

    @JvmOverloads
    fun mulAffine(right: Matrix4d, dest: Matrix4d = this): Matrix4d {
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
        return dest
            ._m00(m00 * rm00 + m10 * rm01 + m20 * rm02)._m01(m01 * rm00 + m11 * rm01 + m21 * rm02)
            ._m02(m02 * rm00 + m12 * rm01 + m22 * rm02)._m03(m03)
            ._m10(m00 * rm10 + m10 * rm11 + m20 * rm12)._m11(m01 * rm10 + (m11 * rm11 + m21 * rm12))
            ._m12(m02 * rm10 + m12 * rm11 + m22 * rm12)._m13(m13)
            ._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + (m11 * rm21 + m21 * rm22))
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(m23)
            ._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)
            ._m32(m02 * rm30 + m12 * rm31 + m22 * rm32 + m32)._m33(m33)
            ._properties(2 or (properties and right.properties() and 16))
    }

    fun mulTranslationAffine(right: Matrix4d, dest: Matrix4d): Matrix4d {
        return dest
            ._m00(right.m00)._m01(right.m01)._m02(right.m02)._m03(m03)
            ._m10(right.m10)._m11(right.m11)._m12(right.m12)._m13(m13)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)._m23(m23)
            ._m30(right.m30 + m30)._m31(right.m31 + m31)._m32(right.m32 + m32)._m33(m33)
            ._properties(2 or (right.properties() and 16))
    }

    @JvmOverloads
    fun mulOrthoAffine(view: Matrix4d, dest: Matrix4d = this): Matrix4d {
        val nm00 = m00 * view.m00
        val nm01 = m11 * view.m01
        val nm02 = m22 * view.m02
        val nm03 = 0.0
        val nm10 = m00 * view.m10
        val nm11 = m11 * view.m11
        val nm12 = m22 * view.m12
        val nm13 = 0.0
        val nm20 = m00 * view.m20
        val nm21 = m11 * view.m21
        val nm22 = m22 * view.m22
        val nm23 = 0.0
        val nm30 = m00 * view.m30 + m30
        val nm31 = m11 * view.m31 + m31
        val nm32 = m22 * view.m32 + m32
        val nm33 = 1.0
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(2)
        return dest
    }

    @JvmOverloads
    fun fma4x3(other: Matrix4d, otherFactor: Double, dest: Matrix4d = this): Matrix4d {
        dest._m00(other.m00 * otherFactor + m00)
            ._m01(other.m01 * otherFactor + m01)
            ._m02(other.m02 * otherFactor + m02)._m03(m03)
            ._m10(other.m10 * otherFactor + m10)
            ._m11(other.m11 * otherFactor + m11)
            ._m12(other.m12 * otherFactor + m12)._m13(m13)
            ._m20(other.m20 * otherFactor + m20)
            ._m21(other.m21 * otherFactor + m21)
            ._m22(other.m22 * otherFactor + m22)._m23(m23)
            ._m30(other.m30 * otherFactor + m30)
            ._m31(other.m31 * otherFactor + m31)
            ._m32(other.m32 * otherFactor + m32)._m33(m33)._properties(0)
        return dest
    }

    @JvmOverloads
    fun add(other: Matrix4d, dest: Matrix4d = this): Matrix4d {
        dest._m00(m00 + other.m00)._m01(m01 + other.m01)._m02(m02 + other.m02)._m03(m03 + other.m03)
            ._m10(m10 + other.m10)._m11(m11 + other.m11)._m12(m12 + other.m12)._m13(m13 + other.m13)
            ._m20(m20 + other.m20)._m21(m21 + other.m21)._m22(m22 + other.m22)._m23(m23 + other.m23)
            ._m30(m30 + other.m30)._m31(m31 + other.m31)._m32(m32 + other.m32)._m33(m33 + other.m33)
            ._properties(0)
        return dest
    }

    @JvmOverloads
    fun sub(s: Matrix4d, dest: Matrix4d = this): Matrix4d {
        dest._m00(m00 - s.m00)._m01(m01 - s.m01)._m02(m02 - s.m02)._m03(m03 - s.m03)
            ._m10(m10 - s.m10)._m11(m11 - s.m11)._m12(m12 - s.m12)._m13(m13 - s.m13)
            ._m20(m20 - s.m20)._m21(m21 - s.m21)._m22(m22 - s.m22)._m23(m23 - s.m23)
            ._m30(m30 - s.m30)._m31(m31 - s.m31)._m32(m32 - s.m32)._m33(m33 - s.m33)._properties(0)
        return dest
    }

    @JvmOverloads
    fun mulComponentWise(o: Matrix4d, dest: Matrix4d = this): Matrix4d {
        dest._m00(m00 * o.m00)._m01(m01 * o.m01)._m02(m02 * o.m02)._m03(m03 * o.m03)
            ._m10(m10 * o.m10)._m11(m11 * o.m11)._m12(m12 * o.m12)._m13(m13 * o.m13)
            ._m20(m20 * o.m20)._m21(m21 * o.m21)._m22(m22 * o.m22)._m23(m23 * o.m23)
            ._m30(m30 * o.m30)._m31(m31 * o.m31)._m32(m32 * o.m32)._m33(m33 * o.m33)
            ._properties(0)
        return dest
    }

    @JvmOverloads
    fun add4x3(other: Matrix4d, dest: Matrix4d = this): Matrix4d {
        return dest._m00(m00 + other.m00)._m01(m01 + other.m01)._m02(m02 + other.m02)._m03(m03)
            ._m10(m10 + other.m10)._m11(m11 + other.m11)._m12(m12 + other.m12)._m13(m13)
            ._m20(m20 + other.m20)._m21(m21 + other.m21)._m22(m22 + other.m22)._m23(m23)
            ._m30(m30 + other.m30)._m31(m31 + other.m31)._m32(m32 + other.m32)._m33(m33)
            ._properties(0)
    }

    @JvmOverloads
    fun add4x3(o: Matrix4f, dest: Matrix4d = this): Matrix4d {
        return dest._m00(m00 + o.m00)._m01(m01 + o.m01)._m02(m02 + o.m02)._m03(m03)
            ._m10(m10 + o.m10)._m11(m11 + o.m11)._m12(m12 + o.m12)._m13(m13)
            ._m20(m20 + o.m20)._m21(m21 + o.m21)._m22(m22 + o.m22)._m23(m23)
            ._m30(m30 + o.m30)._m31(m31 + o.m31)._m32(m32 + o.m32)._m33(m33)
            ._properties(0)
    }

    @JvmOverloads
    fun sub4x3(subtrahend: Matrix4d, dest: Matrix4d = this): Matrix4d {
        dest._m00(m00 - subtrahend.m00)._m01(m01 - subtrahend.m01)._m02(m02 - subtrahend.m02)._m03(m03)
            ._m10(m10 - subtrahend.m10)._m11(
                m11 - subtrahend.m11
            )._m12(m12 - subtrahend.m12)._m13(m13)._m20(m20 - subtrahend.m20)._m21(m21 - subtrahend.m21)._m22(
                m22 - subtrahend.m22
            )._m23(m23)._m30(m30 - subtrahend.m30)._m31(m31 - subtrahend.m31)._m32(m32 - subtrahend.m32)._m33(
                m33
            )._properties(0)
        return dest
    }

    @JvmOverloads
    fun mul4x3ComponentWise(other: Matrix4d, dest: Matrix4d = this): Matrix4d {
        dest._m00(m00 * other.m00)._m01(m01 * other.m01)._m02(m02 * other.m02)._m03(m03)._m10(m10 * other.m10)
            ._m11(m11 * other.m11)._m12(
                m12 * other.m12
            )._m13(m13)._m20(m20 * other.m20)._m21(m21 * other.m21)._m22(m22 * other.m22)._m23(m23)._m30(
                m30 * other.m30
            )._m31(m31 * other.m31)._m32(m32 * other.m32)._m33(m33)._properties(0)
        return dest
    }

    operator fun set(
        m00: Double, m01: Double, m02: Double, m03: Double,
        m10: Double, m11: Double, m12: Double, m13: Double,
        m20: Double, m21: Double, m22: Double, m23: Double,
        m30: Double, m31: Double, m32: Double, m33: Double
    ): Matrix4d {
        this.m00 = m00
        this.m10 = m10
        this.m20 = m20
        this.m30 = m30
        this.m01 = m01
        this.m11 = m11
        this.m21 = m21
        this.m31 = m31
        this.m02 = m02
        this.m12 = m12
        this.m22 = m22
        this.m32 = m32
        this.m03 = m03
        this.m13 = m13
        this.m23 = m23
        this.m33 = m33
        return determineProperties()
    }

    @JvmOverloads
    fun set(m: DoubleArray, off: Int = 0): Matrix4d {
        return _m00(m[off])._m01(m[off + 1])._m02(m[off + 2])._m03(m[off + 3])._m10(m[off + 4])._m11(m[off + 5])._m12(
            m[off + 6]
        )._m13(m[off + 7])._m20(m[off + 8])._m21(m[off + 9])._m22(m[off + 10])._m23(m[off + 11])._m30(m[off + 12])._m31(
            m[off + 13]
        )._m32(m[off + 14])._m33(m[off + 15]).determineProperties()
    }

    @JvmOverloads
    fun set(m: FloatArray, off: Int = 0): Matrix4d {
        return _m00(m[off].toDouble())._m01(m[off + 1].toDouble())._m02(m[off + 2].toDouble())
            ._m03(m[off + 3].toDouble())._m10(
                m[off + 4].toDouble()
            )._m11(m[off + 5].toDouble())._m12(m[off + 6].toDouble())._m13(m[off + 7].toDouble())._m20(
                m[off + 8].toDouble()
            )._m21(m[off + 9].toDouble())._m22(m[off + 10].toDouble())._m23(m[off + 11].toDouble())._m30(
                m[off + 12].toDouble()
            )._m31(m[off + 13].toDouble())._m32(m[off + 14].toDouble())._m33(m[off + 15].toDouble())
            .determineProperties()
    }

    fun set(col0: Vector4d, col1: Vector4d, col2: Vector4d, col3: Vector4d): Matrix4d {
        return _m00(col0.x)._m01(col0.y)._m02(col0.z)._m03(col0.w)._m10(col1.x)._m11(col1.y)._m12(col1.z)._m13(col1.w)
            ._m20(col2.x)._m21(col2.y)._m22(col2.z)._m23(col2.w)._m30(col3.x)._m31(col3.y)._m32(col3.z)._m33(col3.w)
            .determineProperties()
    }

    fun determinant(): Double {
        return if (properties and 2 != 0) determinantAffine() else (m00 * m11 - m01 * m10) * (m22 * m33 - m23 * m32) + (m02 * m10 - m00 * m12) * (m21 * m33 - m23 * m31) + (m00 * m13 - m03 * m10) * (m21 * m32 - m22 * m31) + (m01 * m12 - m02 * m11) * (m20 * m33 - m23 * m30) + (m03 * m11 - m01 * m13) * (m20 * m32 - m22 * m30) + (m02 * m13 - m03 * m12) * (m20 * m31 - m21 * m30)
    }

    fun determinant3x3(): Double {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    fun determinantAffine(): Double {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.identity()
        } else if (properties and 8 != 0) {
            invertTranslation(dest)
        } else if (properties and 16 != 0) {
            invertOrthonormal(dest)
        } else if (properties and 2 != 0) {
            invertAffine(dest)
        } else {
            if (properties and 1 != 0) invertPerspective(dest) else invertGeneric(dest)
        }
    }

    private fun invertTranslation(dest: Matrix4d): Matrix4d {
        if (dest !== this) {
            dest.set(this)
        }
        dest._m30(-m30)._m31(-m31)._m32(-m32)._properties(26)
        return dest
    }

    private fun invertOrthonormal(dest: Matrix4d): Matrix4d {
        val nm30 = -(m00 * m30 + m01 * m31 + m02 * m32)
        val nm31 = -(m10 * m30 + m11 * m31 + m12 * m32)
        val nm32 = -(m20 * m30 + m21 * m31 + m22 * m32)
        val m01 = m01
        val m02 = m02
        val m12 = m12
        dest._m00(m00)._m01(m10)._m02(m20)._m03(0.0)._m10(m01)._m11(m11)._m12(m21)._m13(0.0)._m20(m02)._m21(m12)._m22(
            m22
        )._m23(0.0)._m30(nm30)._m31(nm31)._m32(nm32)._m33(1.0)._properties(18)
        return dest
    }

    private fun invertGeneric(dest: Matrix4d): Matrix4d {
        return if (this !== dest) invertGenericNonThis(dest) else invertGenericThis(dest)
    }

    private fun invertGenericNonThis(dest: Matrix4d): Matrix4d {
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
        det = 1.0 / det
        return dest._m00((m11 * l - m12 * k + m13 * j) * det)
            ._m01((-m01 * l + m02 * k + -m03 * j) * det)
            ._m02((m31 * f - m32 * e + m33 * d) * det)
            ._m03((-m21 * f + m22 * e + -m23 * d) * det)
            ._m10((-m10 * l + m12 * i + -m13 * h) * det)
            ._m11((m00 * l - m02 * i + m03 * h) * det)
            ._m12((-m30 * f + m32 * c + -m33 * b) * det)
            ._m13((m20 * f - m22 * c + m23 * b) * det)
            ._m20((m10 * k - m11 * i + m13 * g) * det)
            ._m21((-m00 * k + m01 * i + -m03 * g) * det)
            ._m22((m30 * e - m31 * c + m33 * a) * det)
            ._m23((-m20 * e + m21 * c + -m23 * a) * det)
            ._m30((-m10 * j + m11 * h + -m12 * g) * det)
            ._m31((m00 * j - m01 * h + m02 * g) * det)
            ._m32((-m30 * d + m31 * b + -m32 * a) * det)
            ._m33((m20 * d - m21 * b + m22 * a) * det)
            ._properties(0)
    }

    private fun invertGenericThis(dest: Matrix4d): Matrix4d {
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
        val det = 1.0 / (a * l - b * k + c * j + d * i - e * h + f * g)
        val nm00 = (m11 * l - m12 * k + m13 * j) * det
        val nm01 = (-m01 * l + m02 * k + -m03 * j) * det
        val nm02 = (m31 * f - m32 * e + m33 * d) * det
        val nm03 = (-m21 * f + m22 * e + -m23 * d) * det
        val nm10 = (-m10 * l + m12 * i + -m13 * h) * det
        val nm11 = (m00 * l - m02 * i + m03 * h) * det
        val nm12 = (-m30 * f + m32 * c + -m33 * b) * det
        val nm13 = (m20 * f - m22 * c + m23 * b) * det
        val nm20 = (m10 * k - m11 * i + m13 * g) * det
        val nm21 = (-m00 * k + m01 * i + -m03 * g) * det
        val nm22 = (m30 * e - m31 * c + m33 * a) * det
        val nm23 = (-m20 * e + m21 * c + -m23 * a) * det
        val nm30 = (-m10 * j + m11 * h + -m12 * g) * det
        val nm31 = (m00 * j - m01 * h + m02 * g) * det
        val nm32 = (-m30 * d + m31 * b + -m32 * a) * det
        val nm33 = (m20 * d - m21 * b + m22 * a) * det
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
    }

    @JvmOverloads
    fun invertPerspective(dest: Matrix4d = this): Matrix4d {
        val a = 1.0 / (m00 * m11)
        val l = -1.0 / (m23 * m32)
        dest[m11 * a, 0.0, 0.0, 0.0, 0.0, m00 * a, 0.0, 0.0, 0.0, 0.0, 0.0, -m23 * l, 0.0, 0.0, -m32 * l] = m22 * l
        return dest
    }

    @JvmOverloads
    fun invertFrustum(dest: Matrix4d = this): Matrix4d {
        val invM00 = 1.0 / m00
        val invM11 = 1.0 / m11
        val invM23 = 1.0 / m23
        val invM32 = 1.0 / m32
        dest[invM00, 0.0, 0.0, 0.0, 0.0, invM11, 0.0, 0.0, 0.0, 0.0, 0.0, invM32, -m20 * invM00 * invM23, -m21 * invM11 * invM23, invM23] =
            -m22 * invM23 * invM32
        return dest
    }

    @JvmOverloads
    fun invertOrtho(dest: Matrix4d = this): Matrix4d {
        val invM00 = 1.0 / m00
        val invM11 = 1.0 / m11
        val invM22 = 1.0 / m22
        dest.set(
            invM00,
            0.0,
            0.0,
            0.0,
            0.0,
            invM11,
            0.0,
            0.0,
            0.0,
            0.0,
            invM22,
            0.0,
            -m30 * invM00,
            -m31 * invM11,
            -m32 * invM22,
            1.0
        )._properties(2 or (properties and 16))
        return dest
    }

    fun invertPerspectiveView(view: Matrix4d, dest: Matrix4d): Matrix4d {
        val a = 1.0 / (m00 * m11)
        val l = -1.0 / (m23 * m32)
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
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0.0)._m10(nm10)
            ._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0.0)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)
            ._m23(pm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(pm33)._properties(0)
    }

    fun invertPerspectiveView(view: Matrix4x3d, dest: Matrix4d): Matrix4d {
        val a = 1.0 / (m00 * m11)
        val l = -1.0 / (m23 * m32)
        val pm00 = m11 * a
        val pm11 = m00 * a
        val pm23 = -m23 * l
        val pm32 = -m32 * l
        val pm33 = m22 * l
        val vm30 = -view.m00 * view.m30 - view.m01 * view.m31 - view.m02 * view.m32
        val vm31 = -view.m10 * view.m30 - view.m11 * view.m31 - view.m12 * view.m32
        val vm32 = -view.m20 * view.m30 - view.m21 * view.m31 - view.m22 * view.m32
        return dest._m00(view.m00 * pm00)._m01(view.m10 * pm00)._m02(view.m20 * pm00)._m03(0.0)._m10(view.m01 * pm11)
            ._m11(view.m11 * pm11)._m12(view.m21 * pm11)._m13(0.0)._m20(vm30 * pm23)._m21(vm31 * pm23)._m22(vm32 * pm23)
            ._m23(pm23)._m30(view.m02 * pm32 + vm30 * pm33)._m31(view.m12 * pm32 + vm31 * pm33)
            ._m32(view.m22 * pm32 + vm32 * pm33)._m33(pm33)._properties(0)
    }

    @JvmOverloads
    fun invertAffine(dest: Matrix4d = this): Matrix4d {
        val m11m00 = m00 * m11
        val m10m01 = m01 * m10
        val m10m02 = m02 * m10
        val m12m00 = m00 * m12
        val m12m01 = m01 * m12
        val m11m02 = m02 * m11
        val s = 1.0 / ((m11m00 - m10m01) * m22 + (m10m02 - m12m00) * m21 + (m12m01 - m11m02) * m20)
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)
            ._m20(nm20)._m21(nm21)._m22(nm22)._m23(0.0)
            ._m30(nm30)._m31(nm31)._m32(nm32)._m33(1.0)
            ._properties(2)
        return dest
    }

    @JvmOverloads
    fun transpose(dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.identity()
        } else {
            if (this !== dest) transposeNonThisGeneric(dest) else transposeThisGeneric(dest)
        }
    }

    private fun transposeNonThisGeneric(dest: Matrix4d): Matrix4d {
        return dest._m00(m00)._m01(m10)._m02(m20)._m03(m30)
            ._m10(m01)._m11(m11)._m12(m21)._m13(m31)
            ._m20(m02)._m21(m12)._m22(m22)._m23(m32)
            ._m30(m03)._m31(m13)._m32(m23)._m33(m33)
            ._properties(0)
    }

    private fun transposeThisGeneric(dest: Matrix4d): Matrix4d {
        val nm10 = m01
        val nm20 = m02
        val nm21 = m12
        val nm30 = m03
        val nm31 = m13
        val nm32 = m23
        return dest._m01(m10)._m02(m20)._m03(m30)
            ._m10(nm10)._m12(m21)._m13(m31)
            ._m20(nm20)._m21(nm21)._m23(m32)
            ._m30(nm30)._m31(nm31)._m32(nm32)
            ._properties(0)
    }

    @JvmOverloads
    fun transpose3x3(dest: Matrix4d = this): Matrix4d {
        val nm10 = m01
        val nm20 = m02
        val nm21 = m12
        return dest._m00(m00)._m01(m10)._m02(m20)
            ._m10(nm10)._m11(m11)._m12(m21)
            ._m20(nm20)._m21(nm21)._m22(m22)
            ._properties(properties and 30)
    }

    fun transpose3x3(dest: Matrix3d): Matrix3d {
        return dest._m00(m00)._m01(m10)._m02(m20)._m10(m01)._m11(m11)._m12(m21)._m20(m02)._m21(m12)._m22(m22)
    }

    fun translation(x: Double, y: Double, z: Double): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        return _m30(x)._m31(y)._m32(z)._m33(1.0)._properties(26)
    }

    fun translation(offset: Vector3f): Matrix4d {
        return this.translation(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translation(offset: Vector3d): Matrix4d {
        return this.translation(offset.x, offset.y, offset.z)
    }

    fun setTranslation(x: Double, y: Double, z: Double): Matrix4d {
        val var10000 = _m30(x)._m31(y)._m32(z)
        var10000.properties = var10000.properties and -6
        return this
    }

    fun setTranslation(xyz: Vector3d): Matrix4d {
        return this.setTranslation(xyz.x, xyz.y, xyz.z)
    }

    fun getTranslation(dest: Vector3d): Vector3d {
        dest.x = m30
        dest.y = m31
        dest.z = m32
        return dest
    }

    fun getScale(dest: Vector3d): Vector3d {
        dest.x = sqrt(m00 * m00 + m01 * m01 + m02 * m02)
        dest.y = sqrt(m10 * m10 + m11 * m11 + m12 * m12)
        dest.z = sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dest
    }

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)} ${f(m30)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)} ${f(m31)}] " +
                "[${f(m02)} ${f(m12)} ${f(m22)} ${f(m32)}] " +
                "[${f(m03)} ${f(m13)} ${f(m23)} ${f(m33)}]]").addSigns()

    fun get(dest: Matrix4d): Matrix4d {
        return dest.set(this)
    }

    fun get4x3(dest: Matrix4x3d): Matrix4x3d {
        return dest.set(this)
    }

    fun get3x3(dest: Matrix3d): Matrix3d {
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
    operator fun get(dest: DoubleArray, offset: Int = 0): DoubleArray {
        dest[offset] = m00
        dest[offset + 1] = m01
        dest[offset + 2] = m02
        dest[offset + 3] = m03
        dest[offset + 4] = m10
        dest[offset + 5] = m11
        dest[offset + 6] = m12
        dest[offset + 7] = m13
        dest[offset + 8] = m20
        dest[offset + 9] = m21
        dest[offset + 10] = m22
        dest[offset + 11] = m23
        dest[offset + 12] = m30
        dest[offset + 13] = m31
        dest[offset + 14] = m32
        dest[offset + 15] = m33
        return dest
    }

    @JvmOverloads
    operator fun get(dest: FloatArray, offset: Int = 0): FloatArray {
        dest[offset] = m00.toFloat()
        dest[offset + 1] = m01.toFloat()
        dest[offset + 2] = m02.toFloat()
        dest[offset + 3] = m03.toFloat()
        dest[offset + 4] = m10.toFloat()
        dest[offset + 5] = m11.toFloat()
        dest[offset + 6] = m12.toFloat()
        dest[offset + 7] = m13.toFloat()
        dest[offset + 8] = m20.toFloat()
        dest[offset + 9] = m21.toFloat()
        dest[offset + 10] = m22.toFloat()
        dest[offset + 11] = m23.toFloat()
        dest[offset + 12] = m30.toFloat()
        dest[offset + 13] = m31.toFloat()
        dest[offset + 14] = m32.toFloat()
        dest[offset + 15] = m33.toFloat()
        return dest
    }

    fun zero(): Matrix4d {
        return _m00(0.0)._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(0.0)._m12(0.0)._m13(0.0)._m20(0.0)._m21(0.0)
            ._m22(0.0)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(0.0)._properties(0)
    }

    fun scaling(factor: Double): Matrix4d {
        return this.scaling(factor, factor, factor)
    }

    fun scaling(x: Double, y: Double, z: Double): Matrix4d {
        if (properties and 4 == 0) {
            identity()
        }
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        _m00(x)._m11(y)._m22(z).properties = 2 or if (one) 16 else 0
        return this
    }

    fun scaling(xyz: Vector3d): Matrix4d {
        return this.scaling(xyz.x, xyz.y, xyz.z)
    }

    fun rotation(angle: Double, x: Double, y: Double, z: Double): Matrix4d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotationX(x * angle)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotationY(y * angle)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotationZ(z * angle) else rotationInternal(
                angle,
                x,
                y,
                z
            )
        }
    }

    private fun rotationInternal(angle: Double, x: Double, y: Double, z: Double): Matrix4d {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1.0 - cos
        val xy = x * y
        val xz = x * z
        val yz = y * z
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(cos + x * x * C)._m10(xy * C - z * sin)._m20(xz * C + y * sin)._m01(xy * C + z * sin)._m11(cos + y * y * C)
            ._m21(yz * C - x * sin)._m02(xz * C - y * sin)._m12(yz * C + x * sin)._m22(cos + z * z * C).properties = 18
        return this
    }

    fun rotationX(ang: Double): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        if (properties and 4 == 0) {
            _identity()
        }
        _m11(cos)._m12(sin)._m21(-sin)._m22(cos).properties = 18
        return this
    }

    fun rotationY(ang: Double): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(cos)._m02(-sin)._m20(sin)._m22(cos).properties = 18
        return this
    }

    fun rotationZ(ang: Double): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(cos)._m01(sin)._m10(-sin)._m11(cos).properties = 18
        return this
    }

    fun rotationTowardsXY(dirX: Double, dirY: Double): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        m00 = dirY
        m01 = dirX
        m10 = -dirX
        m11 = dirY
        properties = 18
        return this
    }

    fun rotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Matrix4d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinX = -sinX
        val m_sinY = -sinY
        val m_sinZ = -sinZ
        if (properties and 4 == 0) {
            _identity()
        }
        val nm01 = m_sinX * m_sinY
        val nm02 = cosX * m_sinY
        _m20(sinY)._m21(m_sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)
            ._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * m_sinZ)._m11(nm01 * m_sinZ + cosX * cosZ)
            ._m12(nm02 * m_sinZ + sinX * cosZ).properties = 18
        return this
    }

    fun rotationZYX(angleZ: Double, angleY: Double, angleX: Double): Matrix4d {
        val sinX = sin(angleX)
        val cosX = cos(angleX)
        val sinY = sin(angleY)
        val cosY = cos(angleY)
        val sinZ = sin(angleZ)
        val cosZ = cos(angleZ)
        val m_sinZ = -sinZ
        val m_sinY = -sinY
        val m_sinX = -sinX
        if (properties and 4 == 0) {
            _identity()
        }
        val nm20 = cosZ * sinY
        val nm21 = sinZ * sinY
        _m00(cosZ * cosY)._m01(sinZ * cosY)._m02(m_sinY)._m10(m_sinZ * cosX + nm20 * sinX)
            ._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(m_sinZ * m_sinX + nm20 * cosX)
            ._m21(cosZ * m_sinX + nm21 * cosX)._m22(cosY * cosX).properties = 18
        return this
    }

    fun rotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Matrix4d {
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
        _m20(sinY * cosX)._m21(m_sinX)._m22(cosY * cosX)._m23(0.0)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)
            ._m02(m_sinY * cosZ + nm12 * sinZ)._m03(0.0)._m10(cosY * m_sinZ + nm10 * cosZ)._m11(cosX * cosZ)
            ._m12(m_sinY * m_sinZ + nm12 * cosZ)._m13(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18
        return this
    }

    fun setRotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Matrix4d {
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
        val var10000 =
            _m20(sinY)._m21(m_sinX * cosY)._m22(cosX * cosY)._m00(cosY * cosZ)._m01(nm01 * cosZ + cosX * sinZ)
                ._m02(nm02 * cosZ + sinX * sinZ)._m10(cosY * m_sinZ)._m11(nm01 * m_sinZ + cosX * cosZ)
                ._m12(nm02 * m_sinZ + sinX * cosZ)
        var10000.properties = var10000.properties and -14
        return this
    }

    fun setRotationZYX(angleZ: Double, angleY: Double, angleX: Double): Matrix4d {
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
        val var10000 = _m00(cosZ * cosY)._m01(sinZ * cosY)._m02(m_sinY)._m10(m_sinZ * cosX + nm20 * sinX)
            ._m11(cosZ * cosX + nm21 * sinX)._m12(cosY * sinX)._m20(m_sinZ * m_sinX + nm20 * cosX)
            ._m21(cosZ * m_sinX + nm21 * cosX)._m22(cosY * cosX)
        var10000.properties = var10000.properties and -14
        return this
    }

    fun setRotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Matrix4d {
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
        val var10000 =
            _m20(sinY * cosX)._m21(m_sinX)._m22(cosY * cosX)._m00(cosY * cosZ + nm10 * sinZ)._m01(cosX * sinZ)
                ._m02(m_sinY * cosZ + nm12 * sinZ)._m10(cosY * m_sinZ + nm10 * cosZ)._m11(cosX * cosZ)
                ._m12(m_sinY * m_sinZ + nm12 * cosZ)
        var10000.properties = var10000.properties and -14
        return this
    }

    fun rotation(angle: Double, axis: Vector3d): Matrix4d {
        return this.rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(angle: Double, axis: Vector3f): Matrix4d {
        return this.rotation(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())
    }

    fun transform(v: Vector4d): Vector4d {
        return v.mul(this)
    }

    fun transform(v: Vector4d, dest: Vector4d?): Vector4d {
        return v.mul(this, dest!!)
    }

    fun transform(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        return dest.set(
            m00 * x + m10 * y + m20 * z + m30 * w,
            m01 * x + m11 * y + m21 * z + m31 * w,
            m02 * x + m12 * y + m22 * z + m32 * w,
            m03 * x + m13 * y + m23 * z + m33 * w
        )
    }

    fun transformTranspose(v: Vector4d): Vector4d {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector4d, dest: Vector4d?): Vector4d {
        return v.mulTranspose(this, dest!!)
    }

    fun transformTranspose(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        return dest.set(x, y, z, w).mulTranspose(this)
    }

    fun transformProject(v: Vector4d): Vector4d {
        return v.mulProject(this)
    }

    fun transformProject(v: Vector4d, dest: Vector4d?): Vector4d {
        return v.mulProject(this, dest!!)
    }

    fun transformProject(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        val invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33 * w)
        return dest.set(
            m00 * x + m10 * y + m20 * z + m30 * w * invW,
            m01 * x + m11 * y + m21 * z + m31 * w * invW,
            m02 * x + m12 * y + m22 * z + m32 * w * invW,
            1.0
        )
    }

    fun transformProject(v: Vector3d): Vector3d {
        return v.mulProject(this)
    }

    fun transformProject(v: Vector3d, dest: Vector3d?): Vector3d {
        return v.mulProject(this, dest!!)
    }

    fun transformProject(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        val invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33)
        return dest.set(
            (m00 * x + m10 * y + m20 * z + m30) * invW,
            (m01 * x + m11 * y + m21 * z + m31) * invW,
            (m02 * x + m12 * y + m22 * z + m32) * invW
        )
    }

    fun transformProject(v: Vector4d, dest: Vector3d?): Vector3d {
        return v.mulProject(this, dest!!)
    }

    fun transformProject(x: Double, y: Double, z: Double, w: Double, dest: Vector3d): Vector3d {
        dest.x = x
        dest.y = y
        dest.z = z
        return dest.mulProject(this, w, dest)
    }

    fun transformPosition(dest: Vector3d): Vector3d {
        return dest.set(
            m00 * dest.x + m10 * dest.y + m20 * dest.z + m30,
            m01 * dest.x + m11 * dest.y + m21 * dest.z + m31,
            m02 * dest.x + m12 * dest.y + m22 * dest.z + m32
        )
    }

    fun transformPosition(v: Vector3d, dest: Vector3d): Vector3d {
        return this.transformPosition(v.x, v.y, v.z, dest)
    }

    fun transformPosition(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        return dest.set(
            m00 * x + m10 * y + m20 * z + m30,
            m01 * x + m11 * y + m21 * z + m31,
            m02 * x + m12 * y + m22 * z + m32
        )
    }

    fun transformDirection(dest: Vector3d): Vector3d {
        return dest.set(
            m00 * dest.x + m10 * dest.y + m20 * dest.z,
            m01 * dest.x + m11 * dest.y + m21 * dest.z,
            m02 * dest.x + m12 * dest.y + m22 * dest.z
        )
    }

    fun transformDirection(v: Vector3d, dest: Vector3d): Vector3d {
        return dest.set(
            m00 * v.x + m10 * v.y + m20 * v.z,
            m01 * v.x + m11 * v.y + m21 * v.z,
            m02 * v.x + m12 * v.y + m22 * v.z
        )
    }

    fun transformDirection(x: Double, y: Double, z: Double, dest: Vector3d): Vector3d {
        return dest.set(m00 * x + m10 * y + m20 * z, m01 * x + m11 * y + m21 * z, m02 * x + m12 * y + m22 * z)
    }

    fun transformDirection(dest: Vector3f): Vector3f {
        return dest.mulDirection(this)
    }

    fun transformDirection(v: Vector3f, dest: Vector3f?): Vector3f {
        return v.mulDirection(this, dest!!)
    }

    fun transformDirection(x: Double, y: Double, z: Double, dest: Vector3f): Vector3f {
        val rx = (m00 * x + m10 * y + m20 * z).toFloat()
        val ry = (m01 * x + m11 * y + m21 * z).toFloat()
        val rz = (m02 * x + m12 * y + m22 * z).toFloat()
        dest.x = rx
        dest.y = ry
        dest.z = rz
        return dest
    }

    fun transformAffine(dest: Vector4d): Vector4d {
        return dest.mulAffine(this, dest)
    }

    fun transformAffine(v: Vector4d, dest: Vector4d): Vector4d {
        return this.transformAffine(v.x, v.y, v.z, v.w, dest)
    }

    fun transformAffine(x: Double, y: Double, z: Double, w: Double, dest: Vector4d): Vector4d {
        val rx = m00 * x + m10 * y + m20 * z + m30 * w
        val ry = m01 * x + m11 * y + m21 * z + m31 * w
        val rz = m02 * x + m12 * y + m22 * z + m32 * w
        dest.x = rx
        dest.y = ry
        dest.z = rz
        dest.w = w
        return dest
    }

    fun set3x3(mat: Matrix3d): Matrix4d {
        return _m00(mat.m00)._m01(mat.m01)._m02(mat.m02)._m10(mat.m10)._m11(mat.m11)._m12(mat.m12)._m20(mat.m20)
            ._m21(mat.m21)._m22(mat.m22)._properties(
                properties and -30
            )
    }

    fun scale(xyz: Vector3d, dest: Matrix4d): Matrix4d {
        return this.scale(xyz.x, xyz.y, xyz.z, dest)
    }

    fun scale(xyz: Vector3d): Matrix4d {
        return this.scale(xyz.x, xyz.y, xyz.z, this)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.scaling(x, y, z) else scaleGeneric(x, y, z, dest)
    }

    private fun scaleGeneric(x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        val one = JomlMath.absEqualsOne(x) && JomlMath.absEqualsOne(y) && JomlMath.absEqualsOne(z)
        dest._m00(m00 * x)._m01(m01 * x)._m02(m02 * x)._m03(m03 * x)._m10(m10 * y)._m11(m11 * y)._m12(m12 * y)
            ._m13(m13 * y)._m20(
                m20 * z
            )._m21(m21 * z)._m22(m22 * z)._m23(m23 * z)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(
                properties and (13 or if (one) 0 else 16).inv()
            )
        return dest
    }

    fun scale(xyz: Double, dest: Matrix4d): Matrix4d {
        return this.scale(xyz, xyz, xyz, dest)
    }

    fun scale(xyz: Double): Matrix4d {
        return this.scale(xyz, xyz, xyz)
    }

    fun scaleXY(x: Double, y: Double, dest: Matrix4d): Matrix4d {
        return this.scale(x, y, 1.0, dest)
    }

    fun scaleXY(x: Double, y: Double): Matrix4d {
        return this.scale(x, y, 1.0)
    }

    @JvmOverloads
    fun scaleAround(
        sx: Double,
        sy: Double,
        sz: Double,
        ox: Double,
        oy: Double,
        oz: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        val nm30 = m00 * ox + m10 * oy + m20 * oz + m30
        val nm31 = m01 * ox + m11 * oy + m21 * oz + m31
        val nm32 = m02 * ox + m12 * oy + m22 * oz + m32
        val nm33 = m03 * ox + m13 * oy + m23 * oz + m33
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        return dest._m00(m00 * sx)._m01(m01 * sx)._m02(m02 * sx)._m03(m03 * sx)._m10(m10 * sy)._m11(m11 * sy)
            ._m12(m12 * sy)._m13(
                m13 * sy
            )._m20(m20 * sz)._m21(m21 * sz)._m22(m22 * sz)._m23(m23 * sz)._m30(
                -dest.m00 * ox - dest.m10 * oy - dest.m20 * oz + nm30
            )._m31(-dest.m01 * ox - dest.m11 * oy - dest.m21 * oz + nm31)._m32(
                -dest.m02 * ox - dest.m12 * oy - dest.m22 * oz + nm32
            )._m33(-dest.m03 * ox - dest.m13 * oy - dest.m23 * oz + nm33)._properties(
                properties and (13 or if (one) 0 else 16).inv()
            )
    }

    fun scaleAround(factor: Double, ox: Double, oy: Double, oz: Double): Matrix4d {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAround(factor: Double, ox: Double, oy: Double, oz: Double, dest: Matrix4d): Matrix4d {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dest)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.scaling(x, y, z) else scaleLocalGeneric(x, y, z, dest)
    }

    private fun scaleLocalGeneric(x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)._m21(nm21)
            ._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(properties and (13 or if (one) 0 else 16).inv())
        return dest
    }

    @JvmOverloads
    fun scaleLocal(xyz: Double, dest: Matrix4d = this): Matrix4d {
        return this.scaleLocal(xyz, xyz, xyz, dest)
    }

    @JvmOverloads
    fun scaleAroundLocal(
        sx: Double,
        sy: Double,
        sz: Double,
        ox: Double,
        oy: Double,
        oz: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        dest._m00(sx * (m00 - ox * m03) + ox * m03)._m01(sy * (m01 - oy * m03) + oy * m03)
            ._m02(sz * (m02 - oz * m03) + oz * m03)._m03(
                m03
            )._m10(sx * (m10 - ox * m13) + ox * m13)._m11(sy * (m11 - oy * m13) + oy * m13)
            ._m12(sz * (m12 - oz * m13) + oz * m13)._m13(
                m13
            )._m20(sx * (m20 - ox * m23) + ox * m23)._m21(sy * (m21 - oy * m23) + oy * m23)
            ._m22(sz * (m22 - oz * m23) + oz * m23)._m23(
                m23
            )._m30(sx * (m30 - ox * m33) + ox * m33)._m31(sy * (m31 - oy * m33) + oy * m33)
            ._m32(sz * (m32 - oz * m33) + oz * m33)._m33(
                m33
            )._properties(properties and (13 or if (one) 0 else 16).inv())
        return dest
    }

    fun scaleAroundLocal(factor: Double, ox: Double, oy: Double, oz: Double): Matrix4d {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAroundLocal(factor: Double, ox: Double, oy: Double, oz: Double, dest: Matrix4d): Matrix4d {
        return this.scaleAroundLocal(factor, factor, factor, ox, oy, oz, dest)
    }

    @JvmOverloads
    fun rotate(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotation(ang, x, y, z)
        } else if (properties and 8 != 0) {
            this.rotateTranslation(ang, x, y, z, dest)
        } else {
            if (properties and 2 != 0) this.rotateAffine(ang, x, y, z, dest) else this.rotateGeneric(
                ang,
                x,
                y,
                z,
                dest
            )
        }
    }

    private fun rotateGeneric(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dest)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dest)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotateZ(
                z * ang,
                dest
            ) else rotateGenericInternal(ang, x, y, z, dest)
        }
    }

    private fun rotateGenericInternal(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotateTranslation(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        val tx = m30
        val ty = m31
        val tz = m32
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            dest.rotationX(x * ang).setTranslation(tx, ty, tz)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            dest.rotationY(y * ang).setTranslation(tx, ty, tz)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) dest.rotationZ(z * ang)
                .setTranslation(tx, ty, tz) else rotateTranslationInternal(ang, x, y, z, dest)
        }
    }

    private fun rotateTranslationInternal(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        return dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)
            ._m11(rm11)._m12(rm12)._m13(0.0)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(1.0)._properties(properties and -14)
    }

    @JvmOverloads
    fun rotateAffine(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dest)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dest)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotateZ(
                z * ang,
                dest
            ) else rotateAffineInternal(ang, x, y, z, dest)
        }
    }

    private fun rotateAffineInternal(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotateAroundAffine(quat: Quaterniond, ox: Double, oy: Double, oz: Double, dest: Matrix4d): Matrix4d {
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
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)
            ._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)
            ._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(0.0)
            ._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)
            ._m30(-nm00 * ox - nm10 * oy - m20 * oz + tm30)
            ._m31(-nm01 * ox - nm11 * oy - m21 * oz + tm31)
            ._m32(-nm02 * ox - nm12 * oy - m22 * oz + tm32)._m33(1.0)
            ._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAround(quat: Quaterniond, ox: Double, oy: Double, oz: Double, dst: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dst.set(this).rotationAround(quat, ox, oy, oz)
        } else {
            if (properties and 2 != 0) rotateAroundAffine(quat, ox, oy, oz, dst)
            else rotateAroundGeneric(quat, ox, oy, oz, dst)
        }
    }

    private fun rotateAroundGeneric(quat: Quaterniond, ox: Double, oy: Double, oz: Double, dst: Matrix4d): Matrix4d {
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
            )._properties(properties and -14)
        return dst
    }

    fun rotationAround(quat: Quaterniond, ox: Double, oy: Double, oz: Double): Matrix4d {
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
        _m23(0.0)
        _m00(w2 + x2 - z2 - y2)
        _m01(dxy + dzw)
        _m02(dxz - dyw)
        _m03(0.0)
        _m10(-dzw + dxy)
        _m11(y2 - z2 + w2 - x2)
        _m12(dyz + dxw)
        _m13(0.0)
        _m30(-m00 * ox - m10 * oy - m20 * oz + ox)
        _m31(-m01 * ox - m11 * oy - m21 * oz + oy)
        _m32(-m02 * ox - m12 * oy - m22 * oz + oz)
        _m33(1.0)
        properties = 18
        return this
    }

    @JvmOverloads
    fun rotateLocal(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.rotation(ang, x, y, z) else rotateLocalGeneric(ang, x, y, z, dest)
    }

    private fun rotateLocalGeneric(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotateLocalX(x * ang, dest)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotateLocalY(y * ang, dest)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotateLocalZ(
                z * ang,
                dest
            ) else rotateLocalGenericInternal(ang, x, y, z, dest)
        }
    }

    private fun rotateLocalGenericInternal(ang: Double, x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)._m21(nm21)
            ._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAroundLocal(quat: Quaterniond, ox: Double, oy: Double, oz: Double, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(lm00 * tm00 + lm10 * tm01 + lm20 * tm02 + ox * m03)
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
            )._m33(m33)._properties(properties and -14)
        return dest
    }

    fun translate(offset: Vector3d): Matrix4d {
        return this.translate(offset.x, offset.y, offset.z)
    }

    fun translate(offset: Vector3d, dest: Matrix4d): Matrix4d {
        return this.translate(offset.x, offset.y, offset.z, dest)
    }

    fun translate(offset: Vector3f): Matrix4d {
        return this.translate(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translate(offset: Vector3f, dest: Matrix4d): Matrix4d {
        return this.translate(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble(), dest)
    }

    fun translate(x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        return if (properties and 4 != 0) dest.translation(x, y, z) else translateGeneric(x, y, z, dest)
    }

    private fun translateGeneric(x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
        dest._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(m10)._m11(m11)._m12(m12)._m13(m13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._m30(m00 * x + m10 * y + m20 * z + m30)
            ._m31(m01 * x + m11 * y + m21 * z + m31)
            ._m32(m02 * x + m12 * y + m22 * z + m32)
            ._m33(m03 * x + m13 * y + m23 * z + m33)
            ._properties(properties and -6)
        return dest
    }

    fun translate(x: Double, y: Double, z: Double): Matrix4d {
        return if (properties and 4 != 0) {
            this.translation(x, y, z)
        } else {
            _m30(m00 * x + m10 * y + m20 * z + m30)
            _m31(m01 * x + m11 * y + m21 * z + m31)
            _m32(m02 * x + m12 * y + m22 * z + m32)
            _m33(m03 * x + m13 * y + m23 * z + m33)
            properties = properties and -6
            this
        }
    }

    fun translateLocal(offset: Vector3f): Matrix4d {
        return this.translateLocal(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translateLocal(offset: Vector3f, dest: Matrix4d): Matrix4d {
        return this.translateLocal(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble(), dest)
    }

    fun translateLocal(offset: Vector3d): Matrix4d {
        return this.translateLocal(offset.x, offset.y, offset.z)
    }

    fun translateLocal(offset: Vector3d, dest: Matrix4d): Matrix4d {
        return this.translateLocal(offset.x, offset.y, offset.z, dest)
    }

    @JvmOverloads
    fun translateLocal(x: Double, y: Double, z: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.translation(x, y, z) else translateLocalGeneric(x, y, z, dest)
    }

    private fun translateLocalGeneric(x: Double, y: Double, z: Double, dest: Matrix4d): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(m03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(m13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(
                m23
            )._m30(nm30)._m31(nm31)._m32(nm32)._m33(m33)._properties(properties and -6)
    }

    @JvmOverloads
    fun rotateLocalX(ang: Double, dest: Matrix4d = this): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm02 = sin * m01 + cos * m02
        val nm12 = sin * m11 + cos * m12
        val nm22 = sin * m21 + cos * m22
        val nm32 = sin * m31 + cos * m32
        dest._m00(m00)._m01(cos * m01 - sin * m02)._m02(nm02)._m03(m03)._m10(m10)._m11(cos * m11 - sin * m12)._m12(nm12)
            ._m13(
                m13
            )._m20(m20)._m21(cos * m21 - sin * m22)._m22(nm22)._m23(m23)._m30(m30)._m31(cos * m31 - sin * m32)
            ._m32(nm32)._m33(
                m33
            )._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateLocalY(ang: Double, dest: Matrix4d = this): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm02 = -sin * m00 + cos * m02
        val nm12 = -sin * m10 + cos * m12
        val nm22 = -sin * m20 + cos * m22
        val nm32 = -sin * m30 + cos * m32
        dest._m00(cos * m00 + sin * m02)._m01(m01)._m02(nm02)._m03(m03)._m10(cos * m10 + sin * m12)._m11(m11)._m12(nm12)
            ._m13(
                m13
            )._m20(cos * m20 + sin * m22)._m21(m21)._m22(nm22)._m23(m23)._m30(cos * m30 + sin * m32)._m31(m31)
            ._m32(nm32)._m33(
                m33
            )._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Double, dest: Matrix4d = this): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm01 = sin * m00 + cos * m01
        val nm11 = sin * m10 + cos * m11
        val nm21 = sin * m20 + cos * m21
        val nm31 = sin * m30 + cos * m31
        dest._m00(cos * m00 - sin * m01)._m01(nm01)._m02(m02)._m03(m03)._m10(cos * m10 - sin * m11)._m11(nm11)._m12(m12)
            ._m13(
                m13
            )._m20(cos * m20 - sin * m21)._m21(nm21)._m22(m22)._m23(m23)._m30(cos * m30 - sin * m31)._m31(nm31)
            ._m32(m32)._m33(
                m33
            )._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateX(ang: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationX(ang)
        } else if (properties and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dest.rotationX(ang).setTranslation(x, y, z)
        } else {
            rotateXInternal(ang, dest)
        }
    }

    private fun rotateXInternal(ang: Double, dest: Matrix4d): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        val rm21 = -sin
        val nm10 = m10 * cos + m20 * sin
        val nm11 = m11 * cos + m21 * sin
        val nm12 = m12 * cos + m22 * sin
        val nm13 = m13 * cos + m23 * sin
        dest._m20(m10 * rm21 + m20 * cos)._m21(m11 * rm21 + m21 * cos)._m22(m12 * rm21 + m22 * cos)
            ._m23(m13 * rm21 + m23 * cos)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(
                m00
            )._m01(m01)._m02(m02)._m03(m03)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateY(ang: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationY(ang)
        } else if (properties and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dest.rotationY(ang).setTranslation(x, y, z)
        } else {
            rotateYInternal(ang, dest)
        }
    }

    private fun rotateYInternal(ang: Double, dest: Matrix4d): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        val rm02 = -sin
        val nm00 = m00 * cos + m20 * rm02
        val nm01 = m01 * cos + m21 * rm02
        val nm02 = m02 * cos + m22 * rm02
        val nm03 = m03 * cos + m23 * rm02
        dest._m20(m00 * sin + m20 * cos)._m21(m01 * sin + m21 * cos)._m22(m02 * sin + m22 * cos)
            ._m23(m03 * sin + m23 * cos)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(
                m10
            )._m11(m11)._m12(m12)._m13(m13)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateZ(ang: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationZ(ang)
        } else if (properties and 8 != 0) {
            val x = m30
            val y = m31
            val z = m32
            dest.rotationZ(ang).setTranslation(x, y, z)
        } else {
            rotateZInternal(ang, dest)
        }
    }

    private fun rotateZInternal(ang: Double, dest: Matrix4d): Matrix4d {
        val sin = sin(ang)
        val cos = cos(ang)
        return rotateTowardsXY(sin, cos, dest)
    }

    @JvmOverloads
    fun rotateTowardsXY(dirX: Double, dirY: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationTowardsXY(dirX, dirY)
        } else {
            val rm10 = -dirX
            val nm00 = m00 * dirY + m10 * dirX
            val nm01 = m01 * dirY + m11 * dirX
            val nm02 = m02 * dirY + m12 * dirX
            val nm03 = m03 * dirY + m13 * dirX
            dest._m10(m00 * rm10 + m10 * dirY)._m11(m01 * rm10 + m11 * dirY)
                ._m12(m02 * rm10 + m12 * dirY)._m13(m03 * rm10 + m13 * dirY)._m00(nm00)._m01(nm01)
                ._m02(nm02)._m03(nm03)._m20(m20)._m21(m21)._m22(m22)._m23(m23)._m30(m30)
                ._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
            dest
        }
    }

    fun rotateXYZ(angles: Vector3d): Matrix4d {
        return this.rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Double, angleY: Double, angleZ: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationXYZ(angleX, angleY, angleZ)
        } else if (properties and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz)
        } else {
            if (properties and 2 != 0) dest.rotateAffineXYZ(angleX, angleY, angleZ) else rotateXYZInternal(
                angleX,
                angleY,
                angleZ,
                dest
            )
        }
    }

    private fun rotateXYZInternal(angleX: Double, angleY: Double, angleZ: Double, dest: Matrix4d): Matrix4d {
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
        dest._m20(m00 * sinY + nm20 * cosY)._m21(m01 * sinY + nm21 * cosY)
            ._m22(m02 * sinY + nm22 * cosY)._m23(m03 * sinY + nm23 * cosY)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)
            ._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAffineXYZ(angleX: Double, angleY: Double, angleZ: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationXYZ(angleX, angleY, angleZ)
        } else if (properties and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dest.rotationXYZ(angleX, angleY, angleZ).setTranslation(tx, ty, tz)
        } else {
            rotateAffineXYZInternal(angleX, angleY, angleZ, dest)
        }
    }

    private fun rotateAffineXYZInternal(angleX: Double, angleY: Double, angleZ: Double, dest: Matrix4d): Matrix4d {
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
        dest._m20(m00 * sinY + nm20 * cosY)._m21(m01 * sinY + nm21 * cosY)._m22(m02 * sinY + nm22 * cosY)._m23(0.0)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)._m02(nm02 * cosZ + nm12 * sinZ)._m03(0.0)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)._m12(nm02 * m_sinZ + nm12 * cosZ)
            ._m13(0.0)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotateZYX(angles: Vector3d): Matrix4d {
        return this.rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Double, angleY: Double, angleX: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationZYX(angleZ, angleY, angleX)
        } else if (properties and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dest.rotationZYX(angleZ, angleY, angleX).setTranslation(tx, ty, tz)
        } else {
            if (properties and 2 != 0) dest.rotateAffineZYX(angleZ, angleY, angleX)
            else rotateZYXInternal(angleZ, angleY, angleX, dest)
        }
    }

    private fun rotateZYXInternal(angleZ: Double, angleY: Double, angleX: Double, dest: Matrix4d): Matrix4d {
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
        dest._m00(nm00 * cosY + m20 * m_sinY)._m01(nm01 * cosY + m21 * m_sinY)._m02(nm02 * cosY + m22 * m_sinY)
            ._m03(nm03 * cosY + m23 * m_sinY)._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)
            ._m12(nm12 * cosX + nm22 * sinX)._m13(nm13 * cosX + nm23 * sinX)._m20(nm10 * m_sinX + nm20 * cosX)
            ._m21(nm11 * m_sinX + nm21 * cosX)._m22(nm12 * m_sinX + nm22 * cosX)._m23(nm13 * m_sinX + nm23 * cosX)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAffineZYX(angleZ: Double, angleY: Double, angleX: Double, dest: Matrix4d = this): Matrix4d {
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
        dest._m00(nm00 * cosY + m20 * m_sinY)._m01(nm01 * cosY + m21 * m_sinY)
            ._m02(nm02 * cosY + m22 * m_sinY)._m03(0.0)
            ._m10(nm10 * cosX + nm20 * sinX)._m11(nm11 * cosX + nm21 * sinX)
            ._m12(nm12 * cosX + nm22 * sinX)._m13(0.0)
            ._m20(nm10 * m_sinX + nm20 * cosX)._m21(nm11 * m_sinX + nm21 * cosX)
            ._m22(nm12 * m_sinX + nm22 * cosX)._m23(0.0)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotateYXZ(angles: Vector3d): Matrix4d {
        return this.rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Double, angleX: Double, angleZ: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotationYXZ(angleY, angleX, angleZ)
        } else if (properties and 8 != 0) {
            val tx = m30
            val ty = m31
            val tz = m32
            dest.rotationYXZ(angleY, angleX, angleZ).setTranslation(tx, ty, tz)
        } else {
            if (properties and 2 != 0) dest.rotateAffineYXZ(angleY, angleX, angleZ)
            else rotateYXZInternal(angleY, angleX, angleZ, dest)
        }
    }

    private fun rotateYXZInternal(angleY: Double, angleX: Double, angleZ: Double, dest: Matrix4d): Matrix4d {
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
        dest._m20(m10 * m_sinX + nm20 * cosX)._m21(m11 * m_sinX + nm21 * cosX)
            ._m22(m12 * m_sinX + nm22 * cosX)._m23(m13 * m_sinX + nm23 * cosX)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)
            ._m02(nm02 * cosZ + nm12 * sinZ)._m03(nm03 * cosZ + nm13 * sinZ)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(nm03 * m_sinZ + nm13 * cosZ)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAffineYXZ(angleY: Double, angleX: Double, angleZ: Double, dest: Matrix4d = this): Matrix4d {
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
        dest._m20(m10 * m_sinX + nm20 * cosX)._m21(m11 * m_sinX + nm21 * cosX)
            ._m22(m12 * m_sinX + nm22 * cosX)._m23(0.0)
            ._m00(nm00 * cosZ + nm10 * sinZ)._m01(nm01 * cosZ + nm11 * sinZ)
            ._m02(nm02 * cosZ + nm12 * sinZ)._m03(0.0)
            ._m10(nm00 * m_sinZ + nm10 * cosZ)._m11(nm01 * m_sinZ + nm11 * cosZ)
            ._m12(nm02 * m_sinZ + nm12 * cosZ)._m13(0.0)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotation(angleAxis: AxisAngle4f): Matrix4d {
        return this.rotation(
            angleAxis.angle.toDouble(),
            angleAxis.x.toDouble(),
            angleAxis.y.toDouble(),
            angleAxis.z.toDouble()
        )
    }

    fun rotation(angleAxis: AxisAngle4d): Matrix4d {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z)
    }

    fun rotation(quat: Quaterniond): Matrix4d {
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
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)
            ._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18)
        return this
    }

    fun rotation(quat: Quaternionf): Matrix4d {
        val w2 = (quat.w * quat.w).toDouble()
        val x2 = (quat.x * quat.x).toDouble()
        val y2 = (quat.y * quat.y).toDouble()
        val z2 = (quat.z * quat.z).toDouble()
        val zw = (quat.z * quat.w).toDouble()
        val dzw = zw + zw
        val xy = (quat.x * quat.y).toDouble()
        val dxy = xy + xy
        val xz = (quat.x * quat.z).toDouble()
        val dxz = xz + xz
        val yw = (quat.y * quat.w).toDouble()
        val dyw = yw + yw
        val yz = (quat.y * quat.z).toDouble()
        val dyz = yz + yz
        val xw = (quat.x * quat.w).toDouble()
        val dxw = xw + xw
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(w2 + x2 - z2 - y2)._m01(dxy + dzw)._m02(dxz - dyw)._m10(-dzw + dxy)._m11(y2 - z2 + w2 - x2)._m12(dyz + dxw)
            ._m20(dyw + dxz)._m21(dyz - dxw)._m22(z2 - y2 - x2 + w2)._properties(18)
        return this
    }

    fun translationRotateScale(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double,
        sx: Double,
        sy: Double,
        sz: Double
    ): Matrix4d {
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
        _m00(sx - (q11 + q22) * sx)._m01((q01 + q23) * sx)._m02((q02 - q13) * sx)._m03(0.0)._m10((q01 - q23) * sy)
            ._m11(sy - (q22 + q00) * sy)._m12((q12 + q03) * sy)._m13(0.0)._m20((q02 + q13) * sz)._m21((q12 - q03) * sz)
            ._m22(sz - (q11 + q00) * sz)._m23(0.0)._m30(tx)._m31(ty)._m32(tz)._m33(1.0).properties =
            2 or if (one) 16 else 0
        return this
    }

    fun translationRotateScale(translation: Vector3f, quat: Quaternionf, scale: Vector3f): Matrix4d {
        return this.translationRotateScale(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x.toDouble(),
            quat.y.toDouble(),
            quat.z.toDouble(),
            quat.w.toDouble(),
            scale.x.toDouble(),
            scale.y.toDouble(),
            scale.z.toDouble()
        )
    }

    fun translationRotateScale(translation: Vector3d, quat: Quaterniond, scale: Vector3d): Matrix4d {
        return this.translationRotateScale(
            translation.x,
            translation.y,
            translation.z,
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale.x,
            scale.y,
            scale.z
        )
    }

    fun translationRotateScale(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double,
        scale: Double
    ): Matrix4d {
        return this.translationRotateScale(tx, ty, tz, qx, qy, qz, qw, scale, scale, scale)
    }

    fun translationRotateScale(translation: Vector3d, quat: Quaterniond, scale: Double): Matrix4d {
        return this.translationRotateScale(
            translation.x,
            translation.y,
            translation.z,
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale,
            scale,
            scale
        )
    }

    fun translationRotateScale(translation: Vector3f, quat: Quaternionf, scale: Double): Matrix4d {
        return this.translationRotateScale(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x.toDouble(),
            quat.y.toDouble(),
            quat.z.toDouble(),
            quat.w.toDouble(),
            scale,
            scale,
            scale
        )
    }

    fun translationRotateScaleInvert(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double,
        sx: Double,
        sy: Double,
        sz: Double
    ): Matrix4d {
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
            val isx = 1.0 / sx
            val isy = 1.0 / sy
            val isz = 1.0 / sz
            _m00(isx * (1.0 - q11 - q22))._m01(isy * (q01 + q23))._m02(isz * (q02 - q13))._m03(0.0)
                ._m10(isx * (q01 - q23))._m11(isy * (1.0 - q22 - q00))._m12(isz * (q12 + q03))._m13(0.0)
                ._m20(isx * (q02 + q13))._m21(isy * (q12 - q03))._m22(isz * (1.0 - q11 - q00))._m23(0.0)
                ._m30(-m00 * tx - m10 * ty - m20 * tz)._m31(-m01 * tx - m11 * ty - m21 * tz)
                ._m32(-m02 * tx - m12 * ty - m22 * tz)._m33(1.0).properties = 2
            this
        }
    }

    fun translationRotateScaleInvert(translation: Vector3d, quat: Quaterniond, scale: Vector3d): Matrix4d {
        return this.translationRotateScaleInvert(
            translation.x,
            translation.y,
            translation.z,
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale.x,
            scale.y,
            scale.z
        )
    }

    fun translationRotateScaleInvert(translation: Vector3f, quat: Quaternionf, scale: Vector3f): Matrix4d {
        return this.translationRotateScaleInvert(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x.toDouble(),
            quat.y.toDouble(),
            quat.z.toDouble(),
            quat.w.toDouble(),
            scale.x.toDouble(),
            scale.y.toDouble(),
            scale.z.toDouble()
        )
    }

    fun translationRotateScaleInvert(translation: Vector3d, quat: Quaterniond, scale: Double): Matrix4d {
        return this.translationRotateScaleInvert(
            translation.x,
            translation.y,
            translation.z,
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale,
            scale,
            scale
        )
    }

    fun translationRotateScaleInvert(translation: Vector3f, quat: Quaternionf, scale: Double): Matrix4d {
        return this.translationRotateScaleInvert(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x.toDouble(),
            quat.y.toDouble(),
            quat.z.toDouble(),
            quat.w.toDouble(),
            scale,
            scale,
            scale
        )
    }

    fun translationRotateScaleMulAffine(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double,
        sx: Double,
        sy: Double,
        sz: Double,
        m: Matrix4d
    ): Matrix4d {
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
        m02 = nm02 * m.m00 + nm12 * m.m01 + nm22 * m.m02
        this.m00 = m00
        this.m01 = m01
        m03 = 0.0
        val m10 = nm00 * m.m10 + nm10 * m.m11 + nm20 * m.m12
        val m11 = nm01 * m.m10 + nm11 * m.m11 + nm21 * m.m12
        m12 = nm02 * m.m10 + nm12 * m.m11 + nm22 * m.m12
        this.m10 = m10
        this.m11 = m11
        m13 = 0.0
        val m20 = nm00 * m.m20 + nm10 * m.m21 + nm20 * m.m22
        val m21 = nm01 * m.m20 + nm11 * m.m21 + nm21 * m.m22
        m22 = nm02 * m.m20 + nm12 * m.m21 + nm22 * m.m22
        this.m20 = m20
        this.m21 = m21
        m23 = 0.0
        val m30 = nm00 * m.m30 + nm10 * m.m31 + nm20 * m.m32 + tx
        val m31 = nm01 * m.m30 + nm11 * m.m31 + nm21 * m.m32 + ty
        m32 = nm02 * m.m30 + nm12 * m.m31 + nm22 * m.m32 + tz
        this.m30 = m30
        this.m31 = m31
        m33 = 1.0
        val one = JomlMath.absEqualsOne(sx) && JomlMath.absEqualsOne(sy) && JomlMath.absEqualsOne(sz)
        properties = 2 or if (one && m.properties and 16 != 0) 16 else 0
        return this
    }

    fun translationRotateScaleMulAffine(
        translation: Vector3f,
        quat: Quaterniond,
        scale: Vector3f,
        m: Matrix4d
    ): Matrix4d {
        return this.translationRotateScaleMulAffine(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale.x.toDouble(),
            scale.y.toDouble(),
            scale.z.toDouble(),
            m
        )
    }

    fun translationRotate(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double
    ): Matrix4d {
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
        m33 = 1.0
        properties = 18
        return this
    }

    fun translationRotate(tx: Double, ty: Double, tz: Double, quat: Quaterniond): Matrix4d {
        return this.translationRotate(tx, ty, tz, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotate(translation: Vector3d, quat: Quaterniond): Matrix4d {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotateInvert(
        tx: Double,
        ty: Double,
        tz: Double,
        qx: Double,
        qy: Double,
        qz: Double,
        qw: Double
    ): Matrix4d {
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
        return _m00(1.0 - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m03(0.0)._m10(q01 - q23)._m11(1.0 - q22 - q00)
            ._m12(q12 + q03)._m13(0.0)._m20(q02 + q13)._m21(q12 - q03)._m22(1.0 - q11 - q00)._m23(0.0)._m30(
                -m00 * tx - m10 * ty - m20 * tz
            )._m31(-m01 * tx - m11 * ty - m21 * tz)._m32(-m02 * tx - m12 * ty - m22 * tz)._m33(1.0)._properties(18)
    }

    fun translationRotateInvert(translation: Vector3f, quat: Quaternionf): Matrix4d {
        return this.translationRotateInvert(
            translation.x.toDouble(),
            translation.y.toDouble(),
            translation.z.toDouble(),
            quat.x.toDouble(),
            quat.y.toDouble(),
            quat.z.toDouble(),
            quat.w.toDouble()
        )
    }

    @JvmOverloads
    fun rotate(quat: Quaterniond, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotation(quat)
        } else if (properties and 8 != 0) {
            this.rotateTranslation(quat, dest)
        } else {
            if (properties and 2 != 0) this.rotateAffine(quat, dest) else this.rotateGeneric(quat, dest)
        }
    }

    private fun rotateGeneric(quat: Quaterniond, dest: Matrix4d): Matrix4d {
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
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.rotation(quat)
        } else if (properties and 8 != 0) {
            this.rotateTranslation(quat, dest)
        } else {
            if (properties and 2 != 0) this.rotateAffine(quat, dest) else this.rotateGeneric(quat, dest)
        }
    }

    private fun rotateGeneric(quat: Quaternionf, dest: Matrix4d): Matrix4d {
        val w2 = (quat.w * quat.w).toDouble()
        val x2 = (quat.x * quat.x).toDouble()
        val y2 = (quat.y * quat.y).toDouble()
        val z2 = (quat.z * quat.z).toDouble()
        val zw = (quat.z * quat.w).toDouble()
        val xy = (quat.x * quat.y).toDouble()
        val xz = (quat.x * quat.z).toDouble()
        val yw = (quat.y * quat.w).toDouble()
        val yz = (quat.y * quat.z).toDouble()
        val xw = (quat.x * quat.w).toDouble()
        val rm00 = w2 + x2 - z2 - y2
        val rm01 = xy + zw + zw + xy
        val rm02 = xz - yw + xz - yw
        val rm10 = -zw + xy - zw + xy
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = yz + yz + xw + xw
        val rm20 = yw + xz + xz + yw
        val rm21 = yz + yz - xw - xw
        val rm22 = z2 - y2 - x2 + w2
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(m03 * rm20 + m13 * rm21 + m23 * rm22)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAffine(quat: Quaterniond, dest: Matrix4d = this): Matrix4d {
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
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun rotateTranslation(quat: Quaterniond, dest: Matrix4d): Matrix4d {
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
        dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)._m11(rm11)
            ._m12(rm12)._m13(0.0)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(1.0)._properties(properties and -14)
        return dest
    }

    fun rotateTranslation(quat: Quaternionf, dest: Matrix4d): Matrix4d {
        val w2 = (quat.w * quat.w).toDouble()
        val x2 = (quat.x * quat.x).toDouble()
        val y2 = (quat.y * quat.y).toDouble()
        val z2 = (quat.z * quat.z).toDouble()
        val zw = (quat.z * quat.w).toDouble()
        val xy = (quat.x * quat.y).toDouble()
        val xz = (quat.x * quat.z).toDouble()
        val yw = (quat.y * quat.w).toDouble()
        val yz = (quat.y * quat.z).toDouble()
        val xw = (quat.x * quat.w).toDouble()
        val rm00 = w2 + x2 - z2 - y2
        val rm01 = xy + zw + zw + xy
        val rm02 = xz - yw + xz - yw
        val rm10 = -zw + xy - zw + xy
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = yz + yz + xw + xw
        val rm20 = yw + xz + xz + yw
        val rm21 = yz + yz - xw - xw
        val rm22 = z2 - y2 - x2 + w2
        dest._m20(rm20)._m21(rm21)._m22(rm22)._m23(0.0)._m00(rm00)._m01(rm01)._m02(rm02)._m03(0.0)._m10(rm10)._m11(rm11)
            ._m12(rm12)._m13(0.0)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(1.0)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaterniond, dest: Matrix4d = this): Matrix4d {
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
        val nm03 = m03
        val nm10 = lm00 * m10 + lm10 * m11 + lm20 * m12
        val nm11 = lm01 * m10 + lm11 * m11 + lm21 * m12
        val nm12 = lm02 * m10 + lm12 * m11 + lm22 * m12
        val nm13 = m13
        val nm20 = lm00 * m20 + lm10 * m21 + lm20 * m22
        val nm21 = lm01 * m20 + lm11 * m21 + lm21 * m22
        val nm22 = lm02 * m20 + lm12 * m21 + lm22 * m22
        val nm23 = m23
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(
                m33
            )._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateAffine(quat: Quaternionf, dest: Matrix4d = this): Matrix4d {
        val w2 = (quat.w * quat.w).toDouble()
        val x2 = (quat.x * quat.x).toDouble()
        val y2 = (quat.y * quat.y).toDouble()
        val z2 = (quat.z * quat.z).toDouble()
        val zw = (quat.z * quat.w).toDouble()
        val xy = (quat.x * quat.y).toDouble()
        val xz = (quat.x * quat.z).toDouble()
        val yw = (quat.y * quat.w).toDouble()
        val yz = (quat.y * quat.z).toDouble()
        val xw = (quat.x * quat.w).toDouble()
        val rm00 = w2 + x2 - z2 - y2
        val rm01 = xy + zw + zw + xy
        val rm02 = xz - yw + xz - yw
        val rm10 = -zw + xy - zw + xy
        val rm11 = y2 - z2 + w2 - x2
        val rm12 = yz + yz + xw + xw
        val rm20 = yw + xz + xz + yw
        val rm21 = yz + yz - xw - xw
        val rm22 = z2 - y2 - x2 + w2
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dest._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun rotateLocal(quat: Quaternionf, dest: Matrix4d = this): Matrix4d {
        val w2 = (quat.w * quat.w).toDouble()
        val x2 = (quat.x * quat.x).toDouble()
        val y2 = (quat.y * quat.y).toDouble()
        val z2 = (quat.z * quat.z).toDouble()
        val zw = (quat.z * quat.w).toDouble()
        val xy = (quat.x * quat.y).toDouble()
        val xz = (quat.x * quat.z).toDouble()
        val yw = (quat.y * quat.w).toDouble()
        val yz = (quat.y * quat.z).toDouble()
        val xw = (quat.x * quat.w).toDouble()
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
        val nm03 = m03
        val nm10 = lm00 * m10 + lm10 * m11 + lm20 * m12
        val nm11 = lm01 * m10 + lm11 * m11 + lm21 * m12
        val nm12 = lm02 * m10 + lm12 * m11 + lm22 * m12
        val nm13 = m13
        val nm20 = lm00 * m20 + lm10 * m21 + lm20 * m22
        val nm21 = lm01 * m20 + lm11 * m21 + lm21 * m22
        val nm22 = lm02 * m20 + lm12 * m21 + lm22 * m22
        val nm23 = m23
        val nm30 = lm00 * m30 + lm10 * m31 + lm20 * m32
        val nm31 = lm01 * m30 + lm11 * m31 + lm21 * m32
        val nm32 = lm02 * m30 + lm12 * m31 + lm22 * m32
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(
                m33
            )._properties(properties and -14)
        return dest
    }

    fun rotate(axisAngle: AxisAngle4f): Matrix4d {
        return this.rotate(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble()
        )
    }

    fun rotate(axisAngle: AxisAngle4f, dest: Matrix4d): Matrix4d {
        return this.rotate(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble(),
            dest
        )
    }

    fun rotate(axisAngle: AxisAngle4d): Matrix4d {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4d, dest: Matrix4d): Matrix4d {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dest)
    }

    fun rotate(angle: Double, axis: Vector3d): Matrix4d {
        return this.rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Double, axis: Vector3d, dest: Matrix4d): Matrix4d {
        return this.rotate(angle, axis.x, axis.y, axis.z, dest)
    }

    fun rotate(angle: Double, axis: Vector3f): Matrix4d {
        return this.rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())
    }

    fun rotate(angle: Double, axis: Vector3f, dest: Matrix4d): Matrix4d {
        return this.rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble(), dest)
    }

    fun getRow(row: Int, dest: Vector4d): Vector4d {
        when (row) {
            0 -> {
                dest.x = m00
                dest.y = m10
                dest.z = m20
                dest.w = m30
            }
            1 -> {
                dest.x = m01
                dest.y = m11
                dest.z = m21
                dest.w = m31
            }
            2 -> {
                dest.x = m02
                dest.y = m12
                dest.z = m22
                dest.w = m32
            }
            3 -> {
                dest.x = m03
                dest.y = m13
                dest.z = m23
                dest.w = m33
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    fun getRow(row: Int, dest: Vector3d): Vector3d {
        when (row) {
            0 -> {
                dest.x = m00
                dest.y = m10
                dest.z = m20
            }
            1 -> {
                dest.x = m01
                dest.y = m11
                dest.z = m21
            }
            2 -> {
                dest.x = m02
                dest.y = m12
                dest.z = m22
            }
            3 -> {
                dest.x = m03
                dest.y = m13
                dest.z = m23
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    fun setRow(row: Int, src: Vector4d): Matrix4d {
        return when (row) {
            0 -> _m00(src.x)._m10(src.y)._m20(src.z)._m30(src.w)._properties(0)
            1 -> _m01(src.x)._m11(src.y)._m21(src.z)._m31(src.w)._properties(0)
            2 -> _m02(src.x)._m12(src.y)._m22(src.z)._m32(src.w)._properties(0)
            3 -> _m03(src.x)._m13(src.y)._m23(src.z)._m33(src.w)._properties(0)
            else -> throw IndexOutOfBoundsException()
        }
    }

    fun getColumn(column: Int, dest: Vector4d): Vector4d {
        when (column) {
            0 -> {
                dest.x = m00
                dest.y = m01
                dest.z = m02
                dest.w = m03
            }
            1 -> {
                dest.x = m10
                dest.y = m11
                dest.z = m12
                dest.w = m13
            }
            2 -> {
                dest.x = m20
                dest.y = m21
                dest.z = m22
                dest.w = m23
            }
            3 -> {
                dest.x = m30
                dest.y = m31
                dest.z = m32
                dest.w = m33
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    fun getColumn(column: Int, dest: Vector3d): Vector3d {
        when (column) {
            0 -> {
                dest.x = m00
                dest.y = m01
                dest.z = m02
            }
            1 -> {
                dest.x = m10
                dest.y = m11
                dest.z = m12
            }
            2 -> {
                dest.x = m20
                dest.y = m21
                dest.z = m22
            }
            3 -> {
                dest.x = m30
                dest.y = m31
                dest.z = m32
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    fun setColumn(column: Int, src: Vector4d): Matrix4d {
        return when (column) {
            0 -> _m00(src.x)._m01(src.y)._m02(src.z)._m03(src.w)._properties(0)
            1 -> _m10(src.x)._m11(src.y)._m12(src.z)._m13(src.w)._properties(0)
            2 -> _m20(src.x)._m21(src.y)._m22(src.z)._m23(src.w)._properties(0)
            3 -> _m30(src.x)._m31(src.y)._m32(src.z)._m33(src.w)._properties(0)
            else -> throw IndexOutOfBoundsException()
        }
    }

    /*operator fun get(column: Int, row: Int): Double {
        return MemUtil.INSTANCE[this, column, row]
    }

    operator fun set(column: Int, row: Int, value: Double): Matrix4d {
        return MemUtil.INSTANCE.set(this, column, row, value)
    }

    fun getRowColumn(row: Int, column: Int): Double {
        return MemUtil.INSTANCE[this, column, row]
    }

    fun setRowColumn(row: Int, column: Int, value: Double): Matrix4d {
        return MemUtil.INSTANCE.set(this, column, row, value)
    }*/

    @JvmOverloads
    fun normal(dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.identity()
        } else {
            if (properties and 16 != 0) this.normalOrthonormal(dest) else this.normalGeneric(dest)
        }
    }

    private fun normalOrthonormal(dest: Matrix4d): Matrix4d {
        if (dest !== this) {
            dest.set(this)
        }
        return dest._properties(18)
    }

    private fun normalGeneric(dest: Matrix4d): Matrix4d {
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
        return dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0)._properties(
                properties or 2 and -10
            )
    }

    fun normal(dest: Matrix3d): Matrix3d {
        return if (properties and 16 != 0) this.normalOrthonormal(dest) else this.normalGeneric(dest)
    }

    private fun normalOrthonormal(dest: Matrix3d): Matrix3d {
        dest.set(this)
        return dest
    }

    private fun normalGeneric(dest: Matrix3d): Matrix3d {
        val m00m11 = m00 * m11
        val m01m10 = m01 * m10
        val m02m10 = m02 * m10
        val m00m12 = m00 * m12
        val m01m12 = m01 * m12
        val m02m11 = m02 * m11
        val det = (m00m11 - m01m10) * m22 + (m02m10 - m00m12) * m21 + (m01m12 - m02m11) * m20
        val s = 1.0 / det
        return dest._m00((m11 * m22 - m21 * m12) * s)._m01((m20 * m12 - m10 * m22) * s)
            ._m02((m10 * m21 - m20 * m11) * s)._m10((m21 * m02 - m01 * m22) * s)._m11((m00 * m22 - m20 * m02) * s)
            ._m12((m20 * m01 - m00 * m21) * s)._m20((m01m12 - m02m11) * s)._m21((m02m10 - m00m12) * s)
            ._m22((m00m11 - m01m10) * s)
    }

    fun cofactor3x3(dest: Matrix3d): Matrix3d {
        return dest._m00(m11 * m22 - m21 * m12)._m01(m20 * m12 - m10 * m22)._m02(m10 * m21 - m20 * m11)
            ._m10(m21 * m02 - m01 * m22)._m11(
                m00 * m22 - m20 * m02
            )._m12(m20 * m01 - m00 * m21)._m20(m01 * m12 - m02 * m11)._m21(m02 * m10 - m00 * m12)
            ._m22(m00 * m11 - m01 * m10)
    }

    @JvmOverloads
    fun cofactor3x3(dest: Matrix4d = this): Matrix4d {
        val nm10 = m21 * m02 - m01 * m22
        val nm11 = m00 * m22 - m20 * m02
        val nm12 = m20 * m01 - m00 * m21
        val nm20 = m01 * m12 - m11 * m02
        val nm21 = m02 * m10 - m12 * m00
        val nm22 = m00 * m11 - m10 * m01
        return dest._m00(m11 * m22 - m21 * m12)._m01(m20 * m12 - m10 * m22)._m02(m10 * m21 - m20 * m11)._m03(0.0)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(0.0)._m20(nm20)._m21(nm21)._m22(nm22)._m23(0.0)._m30(0.0)._m31(0.0)
            ._m32(0.0)._m33(1.0)._properties(
                properties or 2 and -10
            )
    }

    @JvmOverloads
    fun normalize3x3(dest: Matrix4d = this): Matrix4d {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        dest._m00(m00 * invXlen)._m01(m01 * invXlen)._m02(m02 * invXlen)._m10(m10 * invYlen)._m11(m11 * invYlen)._m12(
            m12 * invYlen
        )._m20(m20 * invZlen)._m21(m21 * invZlen)._m22(m22 * invZlen)._m30(m30)._m31(m31)._m32(m32)._m33(
            m33
        )._properties(properties)
        return dest
    }

    fun normalize3x3(dest: Matrix3d): Matrix3d {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        dest.m00(m00 * invXlen)
        dest.m01(m01 * invXlen)
        dest.m02(m02 * invXlen)
        dest.m10(m10 * invYlen)
        dest.m11(m11 * invYlen)
        dest.m12(m12 * invYlen)
        dest.m20(m20 * invZlen)
        dest.m21(m21 * invZlen)
        dest.m22(m22 * invZlen)
        return dest
    }

    fun unproject(winX: Double, winY: Double, winZ: Double, viewport: IntArray, dest: Vector4d): Vector4d {
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
        det = 1.0 / det
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
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val ndcZ = winZ + winZ - 1.0
        val invW = 1.0 / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33)
        dest.x = (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW
        dest.y = (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW
        dest.z = (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW
        dest.w = 1.0
        return dest
    }

    fun unproject(winX: Double, winY: Double, winZ: Double, viewport: IntArray, dest: Vector3d): Vector3d {
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
        det = 1.0 / det
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
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val ndcZ = winZ + winZ - 1.0
        val invW = 1.0 / (im03 * ndcX + im13 * ndcY + im23 * ndcZ + im33)
        dest.x = (im00 * ndcX + im10 * ndcY + im20 * ndcZ + im30) * invW
        dest.y = (im01 * ndcX + im11 * ndcY + im21 * ndcZ + im31) * invW
        dest.z = (im02 * ndcX + im12 * ndcY + im22 * ndcZ + im32) * invW
        return dest
    }

    fun unproject(winCoords: Vector3d, viewport: IntArray, dest: Vector4d): Vector4d {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest)
    }

    fun unproject(winCoords: Vector3d, viewport: IntArray, dest: Vector3d): Vector3d {
        return this.unproject(winCoords.x, winCoords.y, winCoords.z, viewport, dest)
    }

    fun unprojectRay(
        winX: Double,
        winY: Double,
        viewport: IntArray,
        originDest: Vector3d,
        dirDest: Vector3d
    ): Matrix4d {
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
        det = 1.0 / det
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
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val px = im00 * ndcX + im10 * ndcY + im30
        val py = im01 * ndcX + im11 * ndcY + im31
        val pz = im02 * ndcX + im12 * ndcY + im32
        val invNearW = 1.0 / (im03 * ndcX + im13 * ndcY - im23 + im33)
        val nearX = (px - im20) * invNearW
        val nearY = (py - im21) * invNearW
        val nearZ = (pz - im22) * invNearW
        val invW0 = 1.0 / (im03 * ndcX + im13 * ndcY + im33)
        val x0 = px * invW0
        val y0 = py * invW0
        val z0 = pz * invW0
        originDest.x = nearX
        originDest.y = nearY
        originDest.z = nearZ
        dirDest.x = x0 - nearX
        dirDest.y = y0 - nearY
        dirDest.z = z0 - nearZ
        return this
    }

    fun unprojectRay(winCoords: Vector2d, viewport: IntArray, originDest: Vector3d, dirDest: Vector3d): Matrix4d {
        return this.unprojectRay(winCoords.x, winCoords.y, viewport, originDest, dirDest)
    }

    fun unprojectInv(winCoords: Vector3d, viewport: IntArray, dest: Vector4d): Vector4d {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest)
    }

    fun unprojectInv(winX: Double, winY: Double, winZ: Double, viewport: IntArray, dest: Vector4d): Vector4d {
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val ndcZ = winZ + winZ - 1.0
        val invW = 1.0 / (m03 * ndcX + m13 * ndcY + m23 * ndcZ + m33)
        dest.x = (m00 * ndcX + m10 * ndcY + m20 * ndcZ + m30) * invW
        dest.y = (m01 * ndcX + m11 * ndcY + m21 * ndcZ + m31) * invW
        dest.z = (m02 * ndcX + m12 * ndcY + m22 * ndcZ + m32) * invW
        dest.w = 1.0
        return dest
    }

    fun unprojectInv(winCoords: Vector3d, viewport: IntArray, dest: Vector3d): Vector3d {
        return this.unprojectInv(winCoords.x, winCoords.y, winCoords.z, viewport, dest)
    }

    fun unprojectInv(winX: Double, winY: Double, winZ: Double, viewport: IntArray, dest: Vector3d): Vector3d {
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val ndcZ = winZ + winZ - 1.0
        val invW = 1.0 / (m03 * ndcX + m13 * ndcY + m23 * ndcZ + m33)
        dest.x = (m00 * ndcX + m10 * ndcY + m20 * ndcZ + m30) * invW
        dest.y = (m01 * ndcX + m11 * ndcY + m21 * ndcZ + m31) * invW
        dest.z = (m02 * ndcX + m12 * ndcY + m22 * ndcZ + m32) * invW
        return dest
    }

    fun unprojectInvRay(winCoords: Vector2d, viewport: IntArray, originDest: Vector3d, dirDest: Vector3d): Matrix4d {
        return this.unprojectInvRay(winCoords.x, winCoords.y, viewport, originDest, dirDest)
    }

    fun unprojectInvRay(
        winX: Double,
        winY: Double,
        viewport: IntArray,
        originDest: Vector3d,
        dirDest: Vector3d
    ): Matrix4d {
        val ndcX = (winX - viewport[0]) / viewport[2] * 2.0 - 1.0
        val ndcY = (winY - viewport[1]) / viewport[3] * 2.0 - 1.0
        val px = m00 * ndcX + m10 * ndcY + m30
        val py = m01 * ndcX + m11 * ndcY + m31
        val pz = m02 * ndcX + m12 * ndcY + m32
        val invNearW = 1.0 / (m03 * ndcX + m13 * ndcY - m23 + m33)
        val nearX = (px - m20) * invNearW
        val nearY = (py - m21) * invNearW
        val nearZ = (pz - m22) * invNearW
        val invW0 = 1.0 / (m03 * ndcX + m13 * ndcY + m33)
        val x0 = px * invW0
        val y0 = py * invW0
        val z0 = pz * invW0
        originDest.x = nearX
        originDest.y = nearY
        originDest.z = nearZ
        dirDest.x = x0 - nearX
        dirDest.y = y0 - nearY
        dirDest.z = z0 - nearZ
        return this
    }

    fun project(x: Double, y: Double, z: Double, viewport: IntArray, winCoordsDest: Vector4d): Vector4d {
        val invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33)
        val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
        val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
        val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
        return winCoordsDest.set(
            (nx * 0.5 + 0.5) * viewport[2] + viewport[0],
            (ny * 0.5 + 0.5) * viewport[3] + viewport[1],
            0.5 * nz + 0.5, 1.0
        )
    }

    fun project(x: Double, y: Double, z: Double, viewport: IntArray, winCoordsDest: Vector3d): Vector3d {
        val invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33)
        val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
        val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
        val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
        return winCoordsDest.set(
            (nx * 0.5 + 0.5) * viewport[2] + viewport[0],
            (ny * 0.5 + 0.5) * viewport[3] + viewport[1],
            0.5 * nz + 0.5
        )
    }

    fun project(position: Vector3d, viewport: IntArray, dest: Vector4d): Vector4d {
        return this.project(position.x, position.y, position.z, viewport, dest)
    }

    fun project(position: Vector3d, viewport: IntArray, dest: Vector3d): Vector3d {
        return this.project(position.x, position.y, position.z, viewport, dest)
    }

    @JvmOverloads
    fun reflect(a: Double, b: Double, c: Double, d: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) {
            dest.reflection(a, b, c, d)
        } else {
            if (properties and 2 != 0) reflectAffine(a, b, c, d, dest)
            else reflectGeneric(a, b, c, d, dest)
        }
    }

    private fun reflectAffine(a: Double, b: Double, c: Double, d: Double, dest: Matrix4d): Matrix4d {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        val rm00 = 1.0 - da * a
        val rm01 = -da * b
        val rm02 = -da * c
        val rm10 = -db * a
        val rm11 = 1.0 - db * b
        val rm12 = -db * c
        val rm20 = -dc * a
        val rm21 = -dc * b
        val rm22 = 1.0 - dc * c
        val rm30 = -dd * a
        val rm31 = -dd * b
        val rm32 = -dd * c
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m33)._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(
            m01 * rm20 + m11 * rm21 + m21 * rm22
        )._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(0.0)._m00(nm00)._m01(nm01)._m02(nm02)._m03(0.0)._m10(nm10)
            ._m11(nm11)._m12(nm12)._m13(0.0)._properties(
                properties and -14
            )
        return dest
    }

    private fun reflectGeneric(a: Double, b: Double, c: Double, d: Double, dest: Matrix4d): Matrix4d {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        val rm00 = 1.0 - da * a
        val rm01 = -da * b
        val rm02 = -da * c
        val rm10 = -db * a
        val rm11 = 1.0 - db * b
        val rm12 = -db * c
        val rm20 = -dc * a
        val rm21 = -dc * b
        val rm22 = 1.0 - dc * c
        val rm30 = -dd * a
        val rm31 = -dd * b
        val rm32 = -dd * c
        val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
        val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
        val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
        val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
        val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
        val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
        val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
        val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m20(
            m00 * rm20 + m10 * rm21 + m20 * rm22
        )._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(m02 * rm20 + m12 * rm21 + m22 * rm22)._m23(
            m03 * rm20 + m13 * rm21 + m23 * rm22
        )._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun reflect(
        nx: Double, ny: Double, nz: Double,
        px: Double, py: Double, pz: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dest)
    }

    fun reflect(normal: Vector3d, point: Vector3d): Matrix4d {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    @JvmOverloads
    fun reflect(orientation: Quaterniond, point: Vector3d, dest: Matrix4d = this): Matrix4d {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2)
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dest)
    }

    fun reflect(normal: Vector3d, point: Vector3d, dest: Matrix4d): Matrix4d {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dest)
    }

    fun reflection(a: Double, b: Double, c: Double, d: Double): Matrix4d {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        _m00(1.0 - da * a)._m01(-da * b)._m02(-da * c)._m03(0.0)._m10(-db * a)._m11(1.0 - db * b)._m12(-db * c)
            ._m13(0.0)._m20(-dc * a)._m21(-dc * b)._m22(1.0 - dc * c)._m23(0.0)._m30(-dd * a)._m31(-dd * b)
            ._m32(-dd * c)._m33(1.0).properties = 18
        return this
    }

    fun reflection(nx: Double, ny: Double, nz: Double, px: Double, py: Double, pz: Double): Matrix4d {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz)
    }

    fun reflection(normal: Vector3d, point: Vector3d): Matrix4d {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    fun reflection(orientation: Quaterniond, point: Vector3d): Matrix4d {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2)
        return this.reflection(normalX, normalY, normalZ, point.x, point.y, point.z)
    }

    @JvmOverloads
    fun ortho(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setOrtho(
            left,
            right,
            bottom,
            top,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest)
    }

    private fun orthoGeneric(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)
            ._m03(m03 * rm00)._m10(
                m10 * rm11
            )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20 * rm22)._m21(m21 * rm22)._m22(m22 * rm22)
            ._m23(
                m23 * rm22
            )._properties(properties and -30)
        return dest
    }

    fun ortho(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d
    ): Matrix4d {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dest)
    }

    @JvmOverloads
    fun orthoLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setOrthoLH(
            left,
            right,
            bottom,
            top,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest)
    }

    private fun orthoLHGeneric(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
        val rm30 = (left + right) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)
            ._m03(m03 * rm00)._m10(
                m10 * rm11
            )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20 * rm22)._m21(m21 * rm22)._m22(m22 * rm22)
            ._m23(
                m23 * rm22
            )._properties(properties and -30)
        return dest
    }

    fun orthoLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d
    ): Matrix4d {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dest)
    }

    fun setOrtho(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22((if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar))
            ._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)).properties = 2
        return this
    }

    fun setOrtho(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setOrtho(left, right, bottom, top, zNear, zFar, false)
    }

    fun setOrthoLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22((if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear))
            ._m30((right + left) / (left - right))._m31((top + bottom) / (bottom - top))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)).properties = 2
        return this
    }

    fun setOrthoLH(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun orthoSymmetric(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setOrthoSymmetric(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoSymmetricGeneric(width, height, zNear, zFar, zZeroToOne, dest)
    }

    private fun orthoSymmetricGeneric(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = 2.0 / width
        val rm11 = 2.0 / height
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dest._m30(m20 * rm32 + m30)._m31(m21 * rm32 + m31)._m32(m22 * rm32 + m32)._m33(m23 * rm32 + m33)
            ._m00(m00 * rm00)._m01(
                m01 * rm00
            )._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)
            ._m20(
                m20 * rm22
            )._m21(m21 * rm22)._m22(m22 * rm22)._m23(m23 * rm22)._properties(properties and -30)
        return dest
    }

    fun orthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double, dest: Matrix4d): Matrix4d {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dest)
    }

    @JvmOverloads
    fun orthoSymmetricLH(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setOrthoSymmetricLH(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else orthoSymmetricLHGeneric(width, height, zNear, zFar, zZeroToOne, dest)
    }

    private fun orthoSymmetricLHGeneric(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = 2.0 / width
        val rm11 = 2.0 / height
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
        val rm32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        dest._m30(m20 * rm32 + m30)._m31(m21 * rm32 + m31)._m32(m22 * rm32 + m32)._m33(m23 * rm32 + m33)
            ._m00(m00 * rm00)._m01(
                m01 * rm00
            )._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)
            ._m20(
                m20 * rm22
            )._m21(m21 * rm22)._m22(m22 * rm22)._m23(m23 * rm22)._properties(properties and -30)
        return dest
    }

    fun orthoSymmetricLH(width: Double, height: Double, zNear: Double, zFar: Double, dest: Matrix4d): Matrix4d {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dest)
    }

    fun setOrthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / width)._m11(2.0 / height)._m22((if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)).properties = 2
        return this
    }

    fun setOrthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false)
    }

    fun setOrthoSymmetricLH(width: Double, height: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / width)._m11(2.0 / height)._m22((if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear))
            ._m32((if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)).properties = 2
        return this
    }

    fun setOrthoSymmetricLH(width: Double, height: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false)
    }

    @JvmOverloads
    fun ortho2D(left: Double, right: Double, bottom: Double, top: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.setOrtho2D(left, right, bottom, top) else ortho2DGeneric(
            left,
            right,
            bottom,
            top,
            dest
        )
    }

    private fun ortho2DGeneric(left: Double, right: Double, bottom: Double, top: Double, dest: Matrix4d): Matrix4d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm30 = (right + left) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        dest._m30(m00 * rm30 + m10 * rm31 + m30)._m31(m01 * rm30 + m11 * rm31 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(
            m10 * rm11
        )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(-m20)._m21(-m21)._m22(-m22)._m23(-m23)._properties(
            properties and -30
        )
        return dest
    }

    @JvmOverloads
    fun ortho2DLH(left: Double, right: Double, bottom: Double, top: Double, dest: Matrix4d = this): Matrix4d {
        return if (properties and 4 != 0) dest.setOrtho2DLH(left, right, bottom, top) else ortho2DLHGeneric(
            left,
            right,
            bottom,
            top,
            dest
        )
    }

    private fun ortho2DLHGeneric(left: Double, right: Double, bottom: Double, top: Double, dest: Matrix4d): Matrix4d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm30 = (right + left) / (left - right)
        val rm31 = (top + bottom) / (bottom - top)
        dest._m30(m00 * rm30 + m10 * rm31 + m30)._m31(m01 * rm30 + m11 * rm31 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m33)._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(
            m10 * rm11
        )._m11(m11 * rm11)._m12(m12 * rm11)._m13(m13 * rm11)._m20(m20)._m21(m21)._m22(m22)._m23(m23)._properties(
            properties and -30
        )
        return dest
    }

    fun setOrtho2D(left: Double, right: Double, bottom: Double, top: Double): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m22(-1.0)._m30((right + left) / (left - right))
            ._m31((top + bottom) / (bottom - top)).properties = 2
        return this
    }

    fun setOrtho2DLH(left: Double, right: Double, bottom: Double, top: Double): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00(2.0 / (right - left))._m11(2.0 / (top - bottom))._m30((right + left) / (left - right))
            ._m31((top + bottom) / (bottom - top)).properties = 2
        return this
    }

    fun lookAlong(dir: Vector3d, up: Vector3d): Matrix4d {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3d, up: Vector3d, dest: Matrix4d): Matrix4d {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest)
    }

    @JvmOverloads
    fun lookAlong(
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setLookAlong(dirX, dirY, dirZ, upX, upY, upZ) else lookAlongGeneric(
            dirX,
            dirY,
            dirZ,
            upX,
            upY,
            upZ,
            dest
        )
    }

    private fun lookAlongGeneric(
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d
    ): Matrix4d {
        var dirX = dirX
        var dirY = dirY
        var dirZ = dirZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= -invDirLength
        dirY *= -invDirLength
        dirZ *= -invDirLength
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
        val nm00 = m00 * leftX + m10 * upnX + m20 * dirX
        val nm01 = m01 * leftX + m11 * upnX + m21 * dirX
        val nm02 = m02 * leftX + m12 * upnX + m22 * dirX
        val nm03 = m03 * leftX + m13 * upnX + m23 * dirX
        val nm10 = m00 * leftY + m10 * upnY + m20 * dirY
        val nm11 = m01 * leftY + m11 * upnY + m21 * dirY
        val nm12 = m02 * leftY + m12 * upnY + m22 * dirY
        val nm13 = m03 * leftY + m13 * upnY + m23 * dirY
        dest._m20(m00 * leftZ + m10 * upnZ + m20 * dirZ)._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ)._m22(
            m02 * leftZ + m12 * upnZ + m22 * dirZ
        )._m23(m03 * leftZ + m13 * upnZ + m23 * dirZ)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)
            ._m12(nm12)._m13(nm13)._m30(
                m30
            )._m31(m31)._m32(m32)._m33(m33)._properties(properties and -14)
        return dest
    }

    fun setLookAlong(dir: Vector3d, up: Vector3d): Matrix4d {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix4d {
        var dirX = dirX
        var dirY = dirY
        var dirZ = dirZ
        val invDirLength = JomlMath.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= -invDirLength
        dirY *= -invDirLength
        dirZ *= -invDirLength
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
        _m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)._m21(upnZ)
            ._m22(dirZ)._m23(0.0)._m30(0.0)._m31(0.0)._m32(0.0)._m33(1.0).properties = 18
        return this
    }

    fun setLookAt(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4d {
        return this.setLookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAt(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double
    ): Matrix4d {
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
        return _m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)
            ._m21(upnZ)._m22(dirZ)._m23(0.0)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))
            ._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))._m33(1.0)
            ._properties(18)
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3d, dest: Matrix4d): Matrix4d {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest)
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4d {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAt(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) {
            dest.setLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        } else {
            if (properties and 1 != 0) lookAtPerspective(
                eyeX,
                eyeY,
                eyeZ,
                centerX,
                centerY,
                centerZ,
                upX,
                upY,
                upZ,
                dest
            ) else lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest)
        }
    }

    private fun lookAtGeneric(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d
    ): Matrix4d {
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
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m20(
            m00 * leftZ + m10 * upnZ + m20 * dirZ
        )._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ)._m22(m02 * leftZ + m12 * upnZ + m22 * dirZ)._m23(
            m03 * leftZ + m13 * upnZ + m23 * dirZ
        )._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._properties(properties and -14)
        return dest
    }

    fun lookAtPerspective(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d
    ): Matrix4d {
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
        return dest._m00(m00 * leftX)._m01(m11 * upnX)._m02(m22 * dirX)._m03(m23 * dirX)._m10(nm10)._m11(m11 * upnY)
            ._m12(
                m22 * dirY
            )._m13(m23 * dirY)._m20(nm20)._m21(nm21)._m22(m22 * dirZ)._m23(m23 * dirZ)._m30(nm30)._m31(nm31)._m32(nm32)
            ._m33(nm33)._properties(0)
    }

    fun setLookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4d {
        return this.setLookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z)
    }

    fun setLookAtLH(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double
    ): Matrix4d {
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
        _m00(leftX)._m01(upnX)._m02(dirX)._m03(0.0)._m10(leftY)._m11(upnY)._m12(dirY)._m13(0.0)._m20(leftZ)._m21(upnZ)
            ._m22(dirZ)._m23(0.0)._m30(-(leftX * eyeX + leftY * eyeY + leftZ * eyeZ))
            ._m31(-(upnX * eyeX + upnY * eyeY + upnZ * eyeZ))._m32(-(dirX * eyeX + dirY * eyeY + dirZ * eyeZ))
            ._m33(1.0).properties = 18
        return this
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d, dest: Matrix4d): Matrix4d {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dest)
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4d {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun lookAtLH(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) {
            dest.setLookAtLH(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        } else {
            if (properties and 1 != 0) lookAtPerspectiveLH(
                eyeX,
                eyeY,
                eyeZ,
                centerX,
                centerY,
                centerZ,
                upX,
                upY,
                upZ,
                dest
            ) else lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dest)
        }
    }

    private fun lookAtLHGeneric(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d
    ): Matrix4d {
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
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30)._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31)._m32(
            m02 * rm30 + m12 * rm31 + m22 * rm32 + m32
        )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33)._m20(
            m00 * leftZ + m10 * upnZ + m20 * dirZ
        )._m21(m01 * leftZ + m11 * upnZ + m21 * dirZ)._m22(m02 * leftZ + m12 * upnZ + m22 * dirZ)._m23(
            m03 * leftZ + m13 * upnZ + m23 * dirZ
        )._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)
            ._properties(properties and -14)
        return dest
    }

    fun lookAtPerspectiveLH(
        eyeX: Double,
        eyeY: Double,
        eyeZ: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d
    ): Matrix4d {
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
        dest._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)
            ._m21(nm21)._m22(nm22)._m23(nm23)._m30(nm30)._m31(nm31)._m32(nm32)._m33(nm33)._properties(0)
        return dest
    }

    @JvmOverloads
    fun tile(x: Int, y: Int, w: Int, h: Int, dest: Matrix4d = this): Matrix4d {
        val tx = (w - 1 - (x shl 1)).toFloat()
        val ty = (h - 1 - (y shl 1)).toFloat()
        return dest
            ._m30(m00 * tx + m10 * ty + m30)
            ._m31(m01 * tx + m11 * ty + m31)
            ._m32(m02 * tx + m12 * ty + m32)
            ._m33(m03 * tx + m13 * ty + m33)
            ._m00(m00 * w)._m01(m01 * w)._m02(m02 * w)._m03(m03 * w)
            ._m10(m10 * h)._m11(m11 * h)._m12(m12 * h)._m13(m13 * h)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._properties(properties and -30)
    }

    @JvmOverloads
    fun perspective(
        fovy: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setPerspective(
            fovy,
            aspect,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest)
    }

    private fun perspectiveGeneric(
        fovy: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val h = tan(fovy * 0.5)
        val rm00 = 1.0 / (h * aspect)
        val rm11 = 1.0 / h
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 1.0E-6
            rm22 = e - 1.0
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 1.0E-6
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 - m30
        val nm21 = m21 * rm22 - m31
        val nm22 = m22 * rm22 - m32
        val nm23 = m23 * rm22 - m33
        dest._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                properties and -31
            )
        return dest
    }

    @JvmOverloads
    fun perspective(fovy: Double, aspect: Double, zNear: Double, zFar: Double, dest: Matrix4d = this): Matrix4d {
        return this.perspective(fovy, aspect, zNear, zFar, false, dest)
    }

    @JvmOverloads
    fun perspectiveRect(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setPerspectiveRect(
            width,
            height,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveRectGeneric(width, height, zNear, zFar, zZeroToOne, dest)
    }

    private fun perspectiveRectGeneric(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = (zNear + zNear) / width
        val rm11 = (zNear + zNear) / height
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 9.999999974752427E-7
            rm22 = e - 1.0
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 9.999999974752427E-7
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 - m30
        val nm21 = m21 * rm22 - m31
        val nm22 = m22 * rm22 - m32
        val nm23 = m23 * rm22 - m33
        dest._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                properties and -31
            )
        return dest
    }

    @JvmOverloads
    fun perspectiveRect(width: Double, height: Double, zNear: Double, zFar: Double, dest: Matrix4d = this): Matrix4d {
        return this.perspectiveRect(width, height, zNear, zFar, false, dest)
    }

    @JvmOverloads
    fun perspectiveOffCenter(
        fovy: Double,
        offAngleX: Double,
        offAngleY: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setPerspectiveOffCenter(
            fovy,
            offAngleX,
            offAngleY,
            aspect,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveOffCenterGeneric(fovy, offAngleX, offAngleY, aspect, zNear, zFar, zZeroToOne, dest)
    }

    private fun perspectiveOffCenterGeneric(
        fovy: Double,
        offAngleX: Double,
        offAngleY: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val h = tan(fovy * 0.5)
        val xScale = 1.0 / (h * aspect)
        val yScale = 1.0 / h
        val offX = tan(offAngleX)
        val offY = tan(offAngleY)
        val rm20 = offX * xScale
        val rm21 = offY * yScale
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 1.0E-6
            rm22 = e - 1.0
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 1.0E-6
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 - m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 - m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 - m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 - m33
        dest._m00(m00 * xScale)._m01(m01 * xScale)._m02(m02 * xScale)._m03(m03 * xScale)._m10(m10 * yScale)
            ._m11(m11 * yScale)._m12(
                m12 * yScale
            )._m13(m13 * yScale)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                properties and (30 or if (rm20 == 0.0 && rm21 == 0.0) 0 else 1).inv()
            )
        return dest
    }

    @JvmOverloads
    fun perspectiveOffCenter(
        fovy: Double,
        offAngleX: Double,
        offAngleY: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.perspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false, dest)
    }

    @JvmOverloads
    fun perspectiveOffCenterFov(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustum(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne,
            dest
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFov(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustum(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            dest
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFovLH(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne,
            dest
        )
    }

    @JvmOverloads
    fun perspectiveOffCenterFovLH(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            dest
        )
    }

    fun setPerspective(fovy: Double, aspect: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4d {
        val h = tan(fovy * 0.5)
        _m00(1.0 / (h * aspect))._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(1.0 / h)._m12(0.0)._m13(0.0)._m20(0.0)
            ._m21(0.0)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(e - 1.0)._m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 1.0E-6
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)._m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = 1
        return this
    }

    fun setPerspective(fovy: Double, aspect: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setPerspective(fovy, aspect, zNear, zFar, false)
    }

    fun setPerspectiveRect(width: Double, height: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4d {
        zero()
        _m00((zNear + zNear) / width)
        _m11((zNear + zNear) / height)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(e - 1.0)
            _m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 9.999999974752427E-7
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)
            _m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))
            _m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1.0)
        properties = 1
        return this
    }

    fun setPerspectiveRect(width: Double, height: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setPerspectiveRect(width, height, zNear, zFar, false)
    }

    fun setPerspectiveOffCenter(
        fovy: Double,
        offAngleX: Double,
        offAngleY: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double
    ): Matrix4d {
        return this.setPerspectiveOffCenter(fovy, offAngleX, offAngleY, aspect, zNear, zFar, false)
    }

    fun setPerspectiveOffCenter(
        fovy: Double,
        offAngleX: Double,
        offAngleY: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        zero()
        val h = tan(fovy * 0.5)
        val xScale = 1.0 / (h * aspect)
        val yScale = 1.0 / h
        _m00(xScale)._m11(yScale)
        val offX = tan(offAngleX)
        val offY = tan(offAngleY)
        _m20(offX * xScale)._m21(offY * yScale)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(e - 1.0)._m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 1.0E-6
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)._m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = if (offAngleX == 0.0 && offAngleY == 0.0) 1 else 0
        return this
    }

    fun setPerspectiveOffCenterFov(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double
    ): Matrix4d {
        return this.setPerspectiveOffCenterFov(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false)
    }

    fun setPerspectiveOffCenterFov(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        return this.setFrustum(
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
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double
    ): Matrix4d {
        return this.setPerspectiveOffCenterFovLH(angleLeft, angleRight, angleDown, angleUp, zNear, zFar, false)
    }

    fun setPerspectiveOffCenterFovLH(
        angleLeft: Double,
        angleRight: Double,
        angleDown: Double,
        angleUp: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        return this.setFrustumLH(
            tan(angleLeft) * zNear,
            tan(angleRight) * zNear,
            tan(angleDown) * zNear,
            tan(angleUp) * zNear,
            zNear,
            zFar,
            zZeroToOne
        )
    }

    @JvmOverloads
    fun perspectiveLH(
        fovy: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setPerspectiveLH(
            fovy,
            aspect,
            zNear,
            zFar,
            zZeroToOne
        ) else perspectiveLHGeneric(fovy, aspect, zNear, zFar, zZeroToOne, dest)
    }

    private fun perspectiveLHGeneric(
        fovy: Double,
        aspect: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val h = tan(fovy * 0.5)
        val rm00 = 1.0 / (h * aspect)
        val rm11 = 1.0 / h
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 1.0E-6
            rm22 = 1.0 - e
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 1.0E-6
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m20 * rm22 + m30
        val nm21 = m21 * rm22 + m31
        val nm22 = m22 * rm22 + m32
        val nm23 = m23 * rm22 + m33
        dest._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(
                properties and -31
            )
        return dest
    }

    @JvmOverloads
    fun perspectiveLH(fovy: Double, aspect: Double, zNear: Double, zFar: Double, dest: Matrix4d = this): Matrix4d {
        return this.perspectiveLH(fovy, aspect, zNear, zFar, false, dest)
    }

    fun setPerspectiveLH(fovy: Double, aspect: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4d {
        val h = tan(fovy * 0.5)
        _m00(1.0 / (h * aspect))._m01(0.0)._m02(0.0)._m03(0.0)._m10(0.0)._m11(1.0 / h)._m12(0.0)._m13(0.0)._m20(0.0)
            ._m21(0.0)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(1.0 - e)._m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 1.0E-6
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)._m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(1.0)._m30(0.0)._m31(0.0)._m33(0.0).properties = 1
        return this
    }

    fun setPerspectiveLH(fovy: Double, aspect: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setPerspectiveLH(fovy, aspect, zNear, zFar, false)
    }

    @JvmOverloads
    fun frustum(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setFrustum(
            left,
            right,
            bottom,
            top,
            zNear,
            zFar,
            zZeroToOne
        ) else frustumGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest)
    }

    private fun frustumGeneric(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = (zNear + zNear) / (right - left)
        val rm11 = (zNear + zNear) / (top - bottom)
        val rm20 = (right + left) / (right - left)
        val rm21 = (top + bottom) / (top - bottom)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 1.0E-6
            rm22 = e - 1.0
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 1.0E-6
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 - m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 - m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 - m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 - m33
        dest._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(0)
        return dest
    }

    @JvmOverloads
    fun frustum(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustum(left, right, bottom, top, zNear, zFar, false, dest)
    }

    fun setFrustum(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))
            ._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom))
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(e - 1.0)._m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 1.0E-6
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)._m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zNear - zFar))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(-1.0)._m33(0.0).properties = if (m20 == 0.0 && m21 == 0.0) 1 else 0
        return this
    }

    fun setFrustum(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setFrustum(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun frustumLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d = this
    ): Matrix4d {
        return if (properties and 4 != 0) dest.setFrustumLH(
            left,
            right,
            bottom,
            top,
            zNear,
            zFar,
            zZeroToOne
        ) else frustumLHGeneric(left, right, bottom, top, zNear, zFar, zZeroToOne, dest)
    }

    private fun frustumLHGeneric(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean,
        dest: Matrix4d
    ): Matrix4d {
        val rm00 = (zNear + zNear) / (right - left)
        val rm11 = (zNear + zNear) / (top - bottom)
        val rm20 = (right + left) / (right - left)
        val rm21 = (top + bottom) / (top - bottom)
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val rm22: Double
        val rm32: Double
        var e: Double
        if (farInf) {
            e = 1.0E-6
            rm22 = 1.0 - e
            rm32 = (e - if (zZeroToOne) 1.0 else 2.0) * zNear
        } else if (nearInf) {
            e = 1.0E-6
            rm22 = (if (zZeroToOne) 0.0 else 1.0) - e
            rm32 = ((if (zZeroToOne) 1.0 else 2.0) - e) * zFar
        } else {
            rm22 = (if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear)
            rm32 = (if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar)
        }
        e = m00 * rm20 + m10 * rm21 + m20 * rm22 + m30
        val nm21 = m01 * rm20 + m11 * rm21 + m21 * rm22 + m31
        val nm22 = m02 * rm20 + m12 * rm21 + m22 * rm22 + m32
        val nm23 = m03 * rm20 + m13 * rm21 + m23 * rm22 + m33
        dest._m00(m00 * rm00)._m01(m01 * rm00)._m02(m02 * rm00)._m03(m03 * rm00)._m10(m10 * rm11)._m11(m11 * rm11)._m12(
            m12 * rm11
        )._m13(m13 * rm11)._m30(m20 * rm32)._m31(m21 * rm32)._m32(m22 * rm32)._m33(m23 * rm32)._m20(e)._m21(nm21)
            ._m22(nm22)._m23(nm23)._properties(0)
        return dest
    }

    @JvmOverloads
    fun frustumLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dest: Matrix4d = this
    ): Matrix4d {
        return this.frustumLH(left, right, bottom, top, zNear, zFar, false, dest)
    }

    fun setFrustumLH(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4d {
        if (properties and 4 == 0) {
            _identity()
        }
        _m00((zNear + zNear) / (right - left))._m11((zNear + zNear) / (top - bottom))
            ._m20((right + left) / (right - left))._m21((top + bottom) / (top - bottom))
        val farInf = zFar > 0.0 && zFar.isInfinite()
        val nearInf = zNear > 0.0 && zNear.isInfinite()
        val e: Double
        if (farInf) {
            e = 1.0E-6
            _m22(1.0 - e)._m32((e - if (zZeroToOne) 1.0 else 2.0) * zNear)
        } else if (nearInf) {
            e = 1.0E-6
            _m22((if (zZeroToOne) 0.0 else 1.0) - e)._m32(((if (zZeroToOne) 1.0 else 2.0) - e) * zFar)
        } else {
            _m22((if (zZeroToOne) zFar else zFar + zNear) / (zFar - zNear))._m32((if (zZeroToOne) zFar else zFar + zFar) * zNear / (zNear - zFar))
        }
        _m23(1.0)._m33(0.0).properties = if (m20 == 0.0 && m21 == 0.0) 1 else 0
        return this
    }

    fun setFrustumLH(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4d {
        return this.setFrustumLH(left, right, bottom, top, zNear, zFar, false)
    }

    fun setFromIntrinsic(
        alphaX: Double,
        alphaY: Double,
        gamma: Double,
        u0: Double,
        v0: Double,
        imgWidth: Int,
        imgHeight: Int,
        near: Double,
        far: Double
    ): Matrix4d {
        val l00 = 2.0 / imgWidth
        val l11 = 2.0 / imgHeight
        val l22 = 2.0 / (near - far)
        m00 = l00 * alphaX
        m01 = 0.0
        m02 = 0.0
        m03 = 0.0
        m10 = l00 * gamma
        m11 = l11 * alphaY
        m12 = 0.0
        m13 = 0.0
        m20 = l00 * u0 - 1.0
        m21 = l11 * v0 - 1.0
        m22 = l22 * -(near + far) + (far + near) / (near - far)
        m23 = -1.0
        m30 = 0.0
        m31 = 0.0
        m32 = l22 * -near * far
        m33 = 0.0
        properties = 1
        return this
    }

    fun frustumPlane(plane: Int, dest: Vector4d): Vector4d {
        when (plane) {
            0 -> dest.set(m03 + m00, m13 + m10, m23 + m20, m33 + m30).normalize3()
            1 -> dest.set(m03 - m00, m13 - m10, m23 - m20, m33 - m30).normalize3()
            2 -> dest.set(m03 + m01, m13 + m11, m23 + m21, m33 + m31).normalize3()
            3 -> dest.set(m03 - m01, m13 - m11, m23 - m21, m33 - m31).normalize3()
            4 -> dest.set(m03 + m02, m13 + m12, m23 + m22, m33 + m32).normalize3()
            5 -> dest.set(m03 - m02, m13 - m12, m23 - m22, m33 - m32).normalize3()
            else -> throw IllegalArgumentException("dest")
        }
        return dest
    }

    fun frustumCorner(corner: Int, dest: Vector3d): Vector3d {
        val d1: Double
        val d2: Double
        val d3: Double
        val n1x: Double
        val n1y: Double
        val n1z: Double
        val n2x: Double
        val n2y: Double
        val n2z: Double
        val n3x: Double
        val n3y: Double
        val n3z: Double
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
            7 -> {
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
            else -> throw IllegalArgumentException("corner")
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
        val invDot = 1.0 / (n1x * c23x + n1y * c23y + n1z * c23z)
        dest.x = -c23x * d1 - c31x * d2 - c12x * d3 * invDot
        dest.y = -c23y * d1 - c31y * d2 - c12y * d3 * invDot
        dest.z = -c23z * d1 - c31z * d2 - c12z * d3 * invDot
        return dest
    }

    fun perspectiveOrigin(dst: Vector3d): Vector3d {
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
        val invDot = 1.0 / (n1x * c23x + n1y * c23y + n1z * c23z)
        dst.x = (-c23x * d1 - c31x * d2 - c12x * d3) * invDot
        dst.y = (-c23y * d1 - c31y * d2 - c12y * d3) * invDot
        dst.z = (-c23z * d1 - c31z * d2 - c12z * d3) * invDot
        return dst
    }

    fun perspectiveInvOrigin(dst: Vector3d): Vector3d {
        val invW = 1.0 / m23
        dst.x = m20 * invW
        dst.y = m21 * invW
        dst.z = m22 * invW
        return dst
    }

    fun perspectiveFov(): Double {
        val n1x = m03 + m01
        val n1y = m13 + m11
        val n1z = m23 + m21 // bottom
        val n2x = m01 - m03
        val n2y = m11 - m13
        val n2z = m21 - m23 // top
        val n1len = Math.sqrt(n1x * n1x + n1y * n1y + n1z * n1z)
        val n2len = Math.sqrt(n2x * n2x + n2y * n2y + n2z * n2z)
        return Math.acos((n1x * n2x + n1y * n2y + n1z * n2z) / (n1len * n2len))
    }

    fun perspectiveNear(): Double {
        return m32 / (m23 + m22)
    }

    fun perspectiveFar(): Double {
        return m32 / (m22 - m23)
    }

    fun frustumRayDir(x: Double, y: Double, dest: Vector3d): Vector3d {
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
        val m1x = (d + e + f - a - b - c) * (1.0 - y) + (a - b - c + d - e + f) * y
        val m1y = (j + k + l - g - h - i) * (1.0 - y) + (g - h - i + j - k + l) * y
        val m1z = (p + q + r - m - n - o) * (1.0 - y) + (m - n - o + p - q + r) * y
        val m2x = (b - c - d + e + f - a) * (1.0 - y) + (a + b - c - d - e + f) * y
        val m2y = (h - i - j + k + l - g) * (1.0 - y) + (g + h - i - j - k + l) * y
        val m2z = (n - o - p + q + r - m) * (1.0 - y) + (m + n - o - p - q + r) * y
        dest.x = m1x * (1.0 - x) + m2x * x
        dest.y = m1y * (1.0 - x) + m2y * x
        dest.z = m1z * (1.0 - x) + m2z * x
        return dest.normalize(dest)
    }

    fun positiveZ(dir: Vector3d): Vector3d {
        return if (properties and 16 != 0) normalizedPositiveZ(dir) else positiveZGeneric(dir)
    }

    private fun positiveZGeneric(dir: Vector3d): Vector3d {
        return dir.set(m10 * m21 - m11 * m20, m20 * m01 - m21 * m00, m00 * m11 - m01 * m10).normalize()
    }

    fun normalizedPositiveZ(dir: Vector3d): Vector3d {
        return dir.set(m02, m12, m22)
    }

    fun positiveX(dir: Vector3d): Vector3d {
        return if (properties and 16 != 0) normalizedPositiveX(dir) else positiveXGeneric(dir)
    }

    private fun positiveXGeneric(dir: Vector3d): Vector3d {
        return dir.set(m11 * m22 - m12 * m21, m02 * m21 - m01 * m22, m01 * m12 - m02 * m11).normalize()
    }

    fun normalizedPositiveX(dir: Vector3d): Vector3d {
        return dir.set(m00, m10, m20)
    }

    fun positiveY(dir: Vector3d): Vector3d {
        return if (properties and 16 != 0) normalizedPositiveY(dir) else positiveYGeneric(dir)
    }

    private fun positiveYGeneric(dir: Vector3d): Vector3d {
        return dir.set(m12 * m20 - m10 * m22, m00 * m22 - m02 * m20, m02 * m10 - m00 * m12).normalize()
    }

    fun normalizedPositiveY(dir: Vector3d): Vector3d {
        return dir.set(m01, m11, m21)
    }

    fun originAffine(dest: Vector3d): Vector3d {
        val a = m00 * m11 - m01 * m10
        val b = m00 * m12 - m02 * m10
        val d = m01 * m12 - m02 * m11
        val g = m20 * m31 - m21 * m30
        val h = m20 * m32 - m22 * m30
        val j = m21 * m32 - m22 * m31
        dest.x = -m10 * j + m11 * h - m12 * g
        dest.y = m00 * j - m01 * h + m02 * g
        dest.z = -m30 * d + m31 * b - m32 * a
        return dest
    }

    fun origin(dest: Vector3d): Vector3d {
        return if (properties and 2 != 0) originAffine(dest) else originGeneric(dest)
    }

    private fun originGeneric(dest: Vector3d): Vector3d {
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
        val invDet = 1.0 / det
        val nm30 = (-m10 * j + m11 * h - m12 * g) * invDet
        val nm31 = (m00 * j - m01 * h + m02 * g) * invDet
        val nm32 = (-m30 * d + m31 * b - m32 * a) * invDet
        val nm33 = det / (m20 * d - m21 * b + m22 * a)
        val x = nm30 * nm33
        val y = nm31 * nm33
        val z = nm32 * nm33
        return dest.set(x, y, z)
    }

    fun shadow(light: Vector4d, a: Double, b: Double, c: Double, d: Double): Matrix4d {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this)
    }

    fun shadow(light: Vector4d, a: Double, b: Double, c: Double, d: Double, dest: Matrix4d): Matrix4d {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest)
    }

    @JvmOverloads
    fun shadow(
        lightX: Double,
        lightY: Double,
        lightZ: Double,
        lightW: Double,
        a: Double,
        b: Double,
        c: Double,
        d: Double,
        dest: Matrix4d = this
    ): Matrix4d {
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
        dest._m30(m00 * rm30 + m10 * rm31 + m20 * rm32 + m30 * rm33)
            ._m31(m01 * rm30 + m11 * rm31 + m21 * rm32 + m31 * rm33)._m32(
                m02 * rm30 + m12 * rm31 + m22 * rm32 + m32 * rm33
            )._m33(m03 * rm30 + m13 * rm31 + m23 * rm32 + m33 * rm33)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)
            ._m10(nm10)
            ._m11(nm11)._m12(nm12)._m13(nm13)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)._properties(
                properties and -30
            )
        return dest
    }

    @JvmOverloads
    fun shadow(light: Vector4d, planeTransform: Matrix4d, dest: Matrix4d = this): Matrix4d {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dest)
    }

    @JvmOverloads
    fun shadow(
        lightX: Double,
        lightY: Double,
        lightZ: Double,
        lightW: Double,
        planeTransform: Matrix4d,
        dest: Matrix4d = this
    ): Matrix4d {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dest)
    }

    fun billboardCylindrical(objPos: Vector3d, targetPos: Vector3d, up: Vector3d): Matrix4d {
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
        _m00(leftX)._m01(leftY)._m02(leftZ)._m03(0.0)._m10(up.x)._m11(up.y)._m12(up.z)._m13(0.0)._m20(dirX)._m21(dirY)
            ._m22(dirZ)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1.0).properties = 18
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d, up: Vector3d): Matrix4d {
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
        _m00(leftX)._m01(leftY)._m02(leftZ)._m03(0.0)._m10(upX)._m11(upY)._m12(upZ)._m13(0.0)._m20(dirX)._m21(dirY)
            ._m22(dirZ)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)._m33(1.0).properties = 18
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d): Matrix4d {
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
        _m00(1.0 - q11)._m01(q01)._m02(-q13)._m03(0.0)._m10(q01)._m11(1.0 - q00)._m12(q03)._m13(0.0)._m20(q13)
            ._m21(-q03)._m22(1.0 - q11 - q00)._m23(0.0)._m30(objPos.x)._m31(objPos.y)._m32(objPos.z)
            ._m33(1.0).properties = 18
        return this
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = (m00).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m01).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m02).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m03).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m10).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m11).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m12).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m13).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m20).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m21).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m22).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m23).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m30).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m31).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m32).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m33).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj !is Matrix4d) {
            false
        } else {
            val other = obj
            if ((m00) != (other.m00)) {
                false
            } else if ((m01) != (other.m01)) {
                false
            } else if ((m02) != (other.m02)) {
                false
            } else if ((m03) != (other.m03)) {
                false
            } else if ((m10) != (other.m10)) {
                false
            } else if ((m11) != (other.m11)) {
                false
            } else if ((m12) != (other.m12)) {
                false
            } else if ((m13) != (other.m13)) {
                false
            } else if ((m20) != (other.m20)) {
                false
            } else if ((m21) != (other.m21)) {
                false
            } else if ((m22) != (other.m22)) {
                false
            } else if ((m23) != (other.m23)) {
                false
            } else if ((m30) != (other.m30)) {
                false
            } else if ((m31) != (other.m31)) {
                false
            } else if ((m32) != (other.m32)) {
                false
            } else {
                (m33) == (other.m33)
            }
        }
    }

    fun equals(m: Matrix4d?, delta: Double): Boolean {
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
        } else if (!Runtime.equals(m03, m.m03, delta)) {
            false
        } else if (!Runtime.equals(m10, m.m10, delta)) {
            false
        } else if (!Runtime.equals(m11, m.m11, delta)) {
            false
        } else if (!Runtime.equals(m12, m.m12, delta)) {
            false
        } else if (!Runtime.equals(m13, m.m13, delta)) {
            false
        } else if (!Runtime.equals(m20, m.m20, delta)) {
            false
        } else if (!Runtime.equals(m21, m.m21, delta)) {
            false
        } else if (!Runtime.equals(m22, m.m22, delta)) {
            false
        } else if (!Runtime.equals(m23, m.m23, delta)) {
            false
        } else if (!Runtime.equals(m30, m.m30, delta)) {
            false
        } else if (!Runtime.equals(m31, m.m31, delta)) {
            false
        } else if (!Runtime.equals(m32, m.m32, delta)) {
            false
        } else {
            Runtime.equals(m33, m.m33, delta)
        }
    }

    @JvmOverloads
    fun pick(x: Double, y: Double, width: Double, height: Double, viewport: IntArray, dest: Matrix4d = this): Matrix4d {
        val sx = viewport[2] / width
        val sy = viewport[3] / height
        val tx = (viewport[2] + 2.0 * (viewport[0] - x)) / width
        val ty = (viewport[3] + 2.0 * (viewport[1] - y)) / height
        dest._m30(m00 * tx + m10 * ty + m30)._m31(m01 * tx + m11 * ty + m31)._m32(m02 * tx + m12 * ty + m32)._m33(
            m03 * tx + m13 * ty + m33
        )._m00(m00 * sx)._m01(m01 * sx)._m02(m02 * sx)._m03(m03 * sx)._m10(m10 * sy)._m11(m11 * sy)._m12(m12 * sy)._m13(
            m13 * sy
        )._properties(0)
        return dest
    }

    val isAffine: Boolean
        get() = m03 == 0.0 && m13 == 0.0 && m23 == 0.0 && m33 == 1.0

    fun swap(other: Matrix4d): Matrix4d {
        var tmp = m00
        m00 = other.m00
        other.m00 = tmp
        tmp = m01
        m01 = other.m01
        other.m01 = tmp
        tmp = m02
        m02 = other.m02
        other.m02 = tmp
        tmp = m03
        m03 = other.m03
        other.m03 = tmp
        tmp = m10
        m10 = other.m10
        other.m10 = tmp
        tmp = m11
        m11 = other.m11
        other.m11 = tmp
        tmp = m12
        m12 = other.m12
        other.m12 = tmp
        tmp = m13
        m13 = other.m13
        other.m13 = tmp
        tmp = m20
        m20 = other.m20
        other.m20 = tmp
        tmp = m21
        m21 = other.m21
        other.m21 = tmp
        tmp = m22
        m22 = other.m22
        other.m22 = tmp
        tmp = m23
        m23 = other.m23
        other.m23 = tmp
        tmp = m30
        m30 = other.m30
        other.m30 = tmp
        tmp = m31
        m31 = other.m31
        other.m31 = tmp
        tmp = m32
        m32 = other.m32
        other.m32 = tmp
        tmp = m33
        m33 = other.m33
        other.m33 = tmp
        val props = properties
        properties = other.properties
        other.properties = props
        return this
    }

    @JvmOverloads
    fun arcball(
        radius: Double,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        angleX: Double,
        angleY: Double,
        dest: Matrix4d = this
    ): Matrix4d {
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
        dest._m30(-nm00 * centerX - nm10 * centerY - nm20 * centerZ + m30)
            ._m31(-nm01 * centerX - nm11 * centerY - nm21 * centerZ + m31)._m32(
                -nm02 * centerX - nm12 * centerY - nm22 * centerZ + m32
            )._m33(-nm03 * centerX - nm13 * centerY - nm23 * centerZ + m33)._m20(nm20)._m21(nm21)._m22(nm22)._m23(nm23)
            ._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._m00(nm00)._m01(nm01)._m02(nm02)._m03(nm03)._properties(
                properties and -14
            )
        return dest
    }

    fun arcball(radius: Double, center: Vector3d, angleX: Double, angleY: Double, dest: Matrix4d): Matrix4d {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dest)
    }

    fun arcball(radius: Double, center: Vector3d, angleX: Double, angleY: Double): Matrix4d {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this)
    }

    fun frustumAabb(min: Vector3d, max: Vector3d): Matrix4d {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (t in 0..7) {
            val x = (t and 1 shl 1) - 1.0
            val y = (t ushr 1 and 1 shl 1) - 1.0
            val z = (t ushr 2 and 1 shl 1) - 1.0
            val invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33)
            val nx = (m00 * x + m10 * y + m20 * z + m30) * invW
            val ny = (m01 * x + m11 * y + m21 * z + m31) * invW
            val nz = (m02 * x + m12 * y + m22 * z + m32) * invW
            minX = min(minX, nx)
            minY = min(minY, ny)
            minZ = min(minZ, nz)
            maxX = max(maxX, nx)
            maxY = max(maxY, ny)
            maxZ = max(maxZ, nz)
        }
        min.x = minX
        min.y = minY
        min.z = minZ
        max.x = maxX
        max.y = maxY
        max.z = maxZ
        return this
    }

    fun projectedGridRange(projector: Matrix4d, sLower: Double, sUpper: Double, dest: Matrix4d): Matrix4d? {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var intersection = false
        for (t in 0..11) {
            var c0X: Double
            var c0Y: Double
            var c0Z: Double
            var c1X: Double
            var c1Y: Double
            var c1Z: Double
            if (t < 4) {
                c0X = -1.0
                c1X = 1.0
                c1Y = (t and 1 shl 1) - 1.0
                c0Y = c1Y
                c1Z = (t ushr 1 and 1 shl 1) - 1.0
                c0Z = c1Z
            } else if (t < 8) {
                c0Y = -1.0
                c1Y = 1.0
                c1X = (t and 1 shl 1) - 1.0
                c0X = c1X
                c1Z = (t ushr 1 and 1 shl 1) - 1.0
                c0Z = c1Z
            } else {
                c0Z = -1.0
                c1Z = 1.0
                c1X = (t and 1 shl 1) - 1.0
                c0X = c1X
                c1Y = (t ushr 1 and 1 shl 1) - 1.0
                c0Y = c1Y
            }
            var invW = 1.0 / (m03 * c0X + m13 * c0Y + m23 * c0Z + m33)
            val p0x = (m00 * c0X + m10 * c0Y + m20 * c0Z + m30) * invW
            val p0y = (m01 * c0X + m11 * c0Y + m21 * c0Z + m31) * invW
            val p0z = (m02 * c0X + m12 * c0Y + m22 * c0Z + m32) * invW
            invW = 1.0 / (m03 * c1X + m13 * c1Y + m23 * c1Z + m33)
            val p1x = (m00 * c1X + m10 * c1Y + m20 * c1Z + m30) * invW
            val p1y = (m01 * c1X + m11 * c1Y + m21 * c1Z + m31) * invW
            val p1z = (m02 * c1X + m12 * c1Y + m22 * c1Z + m32) * invW
            val dirX = p1x - p0x
            val dirY = p1y - p0y
            val dirZ = p1z - p0z
            val invDenom = 1.0 / dirY
            for (s in 0..1) {
                val isectT = -(p0y + if (s == 0) sLower else sUpper) * invDenom
                if (isectT >= 0.0 && isectT <= 1.0) {
                    intersection = true
                    val ix = p0x + isectT * dirX
                    val iz = p0z + isectT * dirZ
                    invW = 1.0 / (projector.m03 * ix + projector.m23 * iz + projector.m33)
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
            dest.set(maxX - minX, 0.0, 0.0, 0.0, 0.0, maxY - minY, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, minX, minY, 0.0, 1.0)
                ._properties(2)
            dest
        }
    }

    fun perspectiveFrustumSlice(near: Double, far: Double, dest: Matrix4d): Matrix4d {
        val invOldNear = (m23 + m22) / m32
        val invNearFar = 1.0 / (near - far)
        dest._m00(m00 * invOldNear * near)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11 * invOldNear * near)._m12(
            m12
        )._m13(m13)._m20(m20)._m21(m21)._m22((far + near) * invNearFar)._m23(m23)._m30(m30)._m31(m31)
            ._m32((far + far) * near * invNearFar)._m33(
                m33
            )._properties(properties and -29)
        return dest
    }

    fun orthoCrop(view: Matrix4d, dest: Matrix4d): Matrix4d {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY
        for (t in 0..7) {
            val x = (t and 1 shl 1) - 1.0
            val y = (t ushr 1 and 1 shl 1) - 1.0
            val z = (t ushr 2 and 1 shl 1) - 1.0
            var invW = 1.0 / (m03 * x + m13 * y + m23 * z + m33)
            val wx = (m00 * x + m10 * y + m20 * z + m30) * invW
            val wy = (m01 * x + m11 * y + m21 * z + m31) * invW
            val wz = (m02 * x + m12 * y + m22 * z + m32) * invW
            invW = 1.0 / (view.m03 * wx + view.m13 * wy + view.m23 * wz + view.m33)
            val vx = view.m00 * wx + view.m10 * wy + view.m20 * wz + view.m30
            val vy = view.m01 * wx + view.m11 * wy + view.m21 * wz + view.m31
            val vz = (view.m02 * wx + view.m12 * wy + view.m22 * wz + view.m32) * invW
            minX = min(minX, vx)
            maxX = max(maxX, vx)
            minY = min(minY, vy)
            maxY = max(maxY, vy)
            minZ = min(minZ, vz)
            maxZ = max(maxZ, vz)
        }
        return dest.setOrtho(minX, maxX, minY, maxY, -maxZ, -minZ)
    }

    fun trapezoidCrop(
        p0x: Double,
        p0y: Double,
        p1x: Double,
        p1y: Double,
        p2x: Double,
        p2y: Double,
        p3x: Double,
        p3y: Double
    ): Matrix4d {
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
        val sx = 2.0 / d2x
        val sy = 1.0 / (c3y + d)
        val u = (sy + sy) * d / (1.0 - sy * d)
        val m03 = aX * sy
        val m13 = aY * sy
        val m33 = nm31 * sy
        val nm01 = (u + 1.0) * m03
        val nm11 = (u + 1.0) * m13
        nm31 = (u + 1.0) * m33 - u
        nm00 = sx * nm00 - m03
        nm10 = sx * nm10 - m13
        nm30 = sx * nm30 - m33
        this[nm00, nm01, 0.0, m03, nm10, nm11, 0.0, m13, 0.0, 0.0, 1.0, 0.0, nm30, nm31, 0.0] = m33
        properties = 0
        return this
    }

    fun transformAab(
        minX: Double,
        minY: Double,
        minZ: Double,
        maxX: Double,
        maxY: Double,
        maxZ: Double,
        outMin: Vector3d,
        outMax: Vector3d
    ): Matrix4d {
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
        val xmaxx: Double
        val xminx: Double
        if (xax < xbx) {
            xminx = xax
            xmaxx = xbx
        } else {
            xminx = xbx
            xmaxx = xax
        }
        val xmaxy: Double
        val xminy: Double
        if (xay < xby) {
            xminy = xay
            xmaxy = xby
        } else {
            xminy = xby
            xmaxy = xay
        }
        val xmaxz: Double
        val xminz: Double
        if (xaz < xbz) {
            xminz = xaz
            xmaxz = xbz
        } else {
            xminz = xbz
            xmaxz = xaz
        }
        val ymaxx: Double
        val yminx: Double
        if (yax < ybx) {
            yminx = yax
            ymaxx = ybx
        } else {
            yminx = ybx
            ymaxx = yax
        }
        val ymaxy: Double
        val yminy: Double
        if (yay < yby) {
            yminy = yay
            ymaxy = yby
        } else {
            yminy = yby
            ymaxy = yay
        }
        val ymaxz: Double
        val yminz: Double
        if (yaz < ybz) {
            yminz = yaz
            ymaxz = ybz
        } else {
            yminz = ybz
            ymaxz = yaz
        }
        val zmaxx: Double
        val zminx: Double
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

    fun transformAab(min: Vector3d, max: Vector3d, outMin: Vector3d, outMax: Vector3d): Matrix4d {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax)
    }

    @JvmOverloads
    fun lerp(b: Matrix4d, t: Double, dest: Matrix4d = this): Matrix4d {
        dest._m00((b.m00 - m00) * t + m00)._m01((b.m01 - m01) * t + m01)
            ._m02((b.m02 - m02) * t + m02)._m03((b.m03 - m03) * t + m03)
            ._m10((b.m10 - m10) * t + m10)._m11((b.m11 - m11) * t + m11)
            ._m12((b.m12 - m12) * t + m12)._m13((b.m13 - m13) * t + m13)
            ._m20((b.m20 - m20) * t + m20)._m21((b.m21 - m21) * t + m21)
            ._m22((b.m22 - m22) * t + m22)._m23((b.m23 - m23) * t + m23)
            ._m30((b.m30 - m30) * t + m30)._m31((b.m31 - m31) * t + m31)
            ._m32((b.m32 - m32) * t + m32)._m33((b.m33 - m33) * t + m33)
            ._properties(properties and b.properties())
        return dest
    }

    fun rotateTowards(direction: Vector3d, up: Vector3d, dest: Matrix4d): Matrix4d {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, dest)
    }

    fun rotateTowards(direction: Vector3d, up: Vector3d): Matrix4d {
        return this.rotateTowards(direction.x, direction.y, direction.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dest: Matrix4d = this
    ): Matrix4d {
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
        val nm03 = m03 * leftX + m13 * leftY + m23 * leftZ
        val nm10 = m00 * upnX + m10 * upnY + m20 * upnZ
        val nm11 = m01 * upnX + m11 * upnY + m21 * upnZ
        val nm12 = m02 * upnX + m12 * upnY + m22 * upnZ
        val nm13 = m03 * upnX + m13 * upnY + m23 * upnZ
        dest._m30(m30)._m31(m31)._m32(m32)._m33(m33)._m20(m00 * ndirX + m10 * ndirY + m20 * ndirZ)._m21(
            m01 * ndirX + m11 * ndirY + m21 * ndirZ
        )._m22(m02 * ndirX + m12 * ndirY + m22 * ndirZ)._m23(m03 * ndirX + m13 * ndirY + m23 * ndirZ)._m00(nm00)
            ._m01(nm01)._m02(nm02)._m03(nm03)._m10(nm10)._m11(nm11)._m12(nm12)._m13(nm13)._properties(
                properties and -14
            )
        return dest
    }

    fun rotationTowards(dir: Vector3d, up: Vector3d): Matrix4d {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix4d {
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
        if (properties and 4 == 0) {
            _identity()
        }
        m00 = leftX
        m01 = leftY
        m02 = leftZ
        m10 = upnX
        m11 = upnY
        m12 = upnZ
        m20 = ndirX
        m21 = ndirY
        m22 = ndirZ
        properties = 18
        return this
    }

    fun translationRotateTowards(pos: Vector3d, dir: Vector3d, up: Vector3d): Matrix4d {
        return this.translationRotateTowards(pos.x, pos.y, pos.z, dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun translationRotateTowards(
        posX: Double,
        posY: Double,
        posZ: Double,
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double
    ): Matrix4d {
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
        m03 = 0.0
        m10 = upnX
        m11 = upnY
        m12 = upnZ
        m13 = 0.0
        m20 = ndirX
        m21 = ndirY
        m22 = ndirZ
        m23 = 0.0
        m30 = posX
        m31 = posY
        m32 = posZ
        m33 = 1.0
        properties = 18
        return this
    }

    fun getEulerAnglesZYX(dest: Vector3d): Vector3d {
        dest.x = atan2(m12, m22)
        dest.y = atan2(-m02, sqrt(1.0 - m02 * m02))
        dest.z = atan2(m01, m00)
        return dest
    }

    fun getEulerAnglesXYZ(dest: Vector3d): Vector3d {
        dest.x = atan2(-m21, m22)
        dest.y = atan2(m20, sqrt(1.0 - m20 * m20))
        dest.z = atan2(-m10, m00)
        return dest
    }

    fun affineSpan(corner: Vector3d, xDir: Vector3d, yDir: Vector3d, zDir: Vector3d): Matrix4d {
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
        val s = 1.0 / (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
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
        xDir.x = 2.0 * nm00
        xDir.y = 2.0 * nm01
        xDir.z = 2.0 * nm02
        yDir.x = 2.0 * nm10
        yDir.y = 2.0 * nm11
        yDir.z = 2.0 * nm12
        zDir.x = 2.0 * nm20
        zDir.y = 2.0 * nm21
        zDir.z = 2.0 * nm22
        return this
    }

    fun testPoint(x: Double, y: Double, z: Double): Boolean {
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
        return nxX * x + nxY * y + nxZ * z + nxW >= 0.0 && pxX * x + pxY * y + pxZ * z + pxW >= 0.0 && nyX * x + nyY * y + nyZ * z + nyW >= 0.0 && pyX * x + pyY * y + pyZ * z + pyW >= 0.0 && nzX * x + nzY * y + nzZ * z + nzW >= 0.0 && pzX * x + pzY * y + pzZ * z + pzW >= 0.0
    }

    fun testSphere(x: Double, y: Double, z: Double, r: Double): Boolean {
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

    fun testAab(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
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
        return nxX * (if (nxX < 0.0) minX else maxX) + nxY * (if (nxY < 0.0) minY else maxY) + nxZ * (if (nxZ < 0.0) minZ else maxZ) >= -nxW &&
                pxX * (if (pxX < 0.0) minX else maxX) + pxY * (if (pxY < 0.0) minY else maxY) + pxZ * (if (pxZ < 0.0) minZ else maxZ) >= -pxW &&
                nyX * (if (nyX < 0.0) minX else maxX) + nyY * (if (nyY < 0.0) minY else maxY) + nyZ * (if (nyZ < 0.0) minZ else maxZ) >= -nyW &&
                pyX * (if (pyX < 0.0) minX else maxX) + pyY * (if (pyY < 0.0) minY else maxY) + pyZ * (if (pyZ < 0.0) minZ else maxZ) >= -pyW &&
                nzX * (if (nzX < 0.0) minX else maxX) + nzY * (if (nzY < 0.0) minY else maxY) + nzZ * (if (nzZ < 0.0) minZ else maxZ) >= -nzW &&
                pzX * (if (pzX < 0.0) minX else maxX) + pzY * (if (pzY < 0.0) minY else maxY) + pzZ * (if (pzZ < 0.0) minZ else maxZ) >= -pzW
    }

    fun obliqueZ(a: Double, b: Double): Matrix4d {
        m20 += m00 * a + m10 * b
        m21 += m01 * a + m11 * b
        m22 += m02 * a + m12 * b
        properties = properties and 2
        return this
    }

    fun obliqueZ(a: Double, b: Double, dest: Matrix4d): Matrix4d {
        dest._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)
            ._m20(m00 * a + m10 * b + m20)._m21(m01 * a + m11 * b + m21)._m22(m02 * a + m12 * b + m22)
            ._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 2)
        return dest
    }

    @JvmOverloads
    fun withLookAtUp(up: Vector3d, dest: Matrix4d = this): Matrix4d {
        return withLookAtUp(up.x, up.y, up.z, dest)
    }

    @JvmOverloads
    fun withLookAtUp(upX: Double, upY: Double, upZ: Double, dest: Matrix4d = this): Matrix4d {
        val y = (upY * m21 - upZ * m11) * m02 + (upZ * m01 - upX * m21) * m12 + (upX * m11 - upY * m01) * m22
        var x = upX * m01 + upY * m11 + upZ * m21
        if (properties and 16 == 0) {
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
        dest._m00(nm00)._m10(nm10)._m20(nm20)._m30(nm30)._m01(nm01)._m11(nm11)._m21(nm21)._m31(nm31)
        if (dest !== this) {
            dest._m02(m02)._m12(m12)._m22(m22)._m32(m32)._m03(m03)._m13(m13)._m23(m23)._m33(m33)
        }
        dest._properties(properties and -14)
        return dest
    }

    @JvmOverloads
    fun mapXZY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(m20)._m11(m21)._m12(m22)._m13(m13)
            ._m20(m10)._m21(m11)._m22(m12)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapXZnY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(m20)._m11(m21)._m12(m22)._m13(m13)
            ._m20(-m10)._m21(-m11)._m22(-m12)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapXnYnZ(dest: Matrix4d = this): Matrix4d {
        return dest
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)
            ._m20(-m20)._m21(-m21)._m22(-m22)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapXnZY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)
            ._m20(m10)._m21(m11)._m22(m12)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapXnZnY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest
            ._m00(m00)._m01(m01)._m02(m02)._m03(m03)
            ._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)
            ._m20(-m10)._m21(-m11)._m22(-m12)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYXZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(m00)._m11(m01)._m12(m02)._m13(m13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYXnZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYZX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(m20)._m11(m21)._m12(m22)._m13(m13)
            ._m20(m00)._m21(m01)._m22(m02)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYZnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(m20)._m11(m21)._m12(m22)._m13(m13)
            ._m20(-m00)._m21(-m01)._m22(-m02)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYnXZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)
            ._m20(m20)._m21(m21)._m22(m22)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYnXnZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)
            ._m20(-m20)._m21(-m21)._m22(-m22)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYnZX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)
            ._m20(m00)._m21(m01)._m22(m02)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapYnZnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest
            ._m00(m10)._m01(m11)._m02(m12)._m03(m03)
            ._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)
            ._m20(-m00)._m21(-m01)._m22(-m02)._m23(m23)
            ._m30(m30)._m31(m31)._m32(m32)._m33(m33)
            ._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZXY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(m10)._m21(m11)
            ._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZXnY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZYX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(m00)._m21(m01)
            ._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZYnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZnXY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(m10)
            ._m21(m11)._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZnXnY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZnYX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(m00)
            ._m21(m01)._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapZnYnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(m20)._m01(m21)._m02(m22)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXYnZ(dest: Matrix4d = this): Matrix4d {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXZY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(m20)._m11(m21)._m12(m22)._m13(m13)._m20(m10)
            ._m21(m11)._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXZnY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(m20)._m11(m21)._m12(m22)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXnYZ(dest: Matrix4d = this): Matrix4d {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(m20)
            ._m21(
                m21
            )._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXnYnZ(dest: Matrix4d = this): Matrix4d {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXnZY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)._m20(m10)
            ._m21(m11)._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnXnZnY(dest: Matrix4d = this): Matrix4d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYXZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(m20)._m21(
            m21
        )._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYXnZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYZX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(m20)._m11(m21)._m12(m22)._m13(m13)._m20(m00)
            ._m21(m01)._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYZnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(m20)._m11(m21)._m12(m22)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYnXZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(m20)
            ._m21(
                m21
            )._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYnXnZ(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYnZX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)._m20(m00)
            ._m21(m01)._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnYnZnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m10)._m01(-m11)._m02(-m12)._m03(m03)._m10(-m20)._m11(-m21)._m12(-m22)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZXY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(m10)
            ._m21(m11)._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZXnY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(m00)._m11(m01)._m12(m02)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZYX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(m00)
            ._m21(m01)._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZYnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZnXY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(m10)
            ._m21(m11)._m22(m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZnXnY(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(-m00)._m11(-m01)._m12(-m02)._m13(m13)._m20(-m10)
            ._m21(-m11)._m22(-m12)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZnYX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(m00)
            ._m21(m01)._m22(m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    @JvmOverloads
    fun mapnZnYnX(dest: Matrix4d = this): Matrix4d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dest._m00(-m20)._m01(-m21)._m02(-m22)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(-m00)
            ._m21(-m01)._m22(-m02)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    fun negateX(): Matrix4d {
        return _m00(-m00)._m01(-m01)._m02(-m02)._properties(properties and 18)
    }

    fun negateX(dest: Matrix4d): Matrix4d {
        return dest._m00(-m00)._m01(-m01)._m02(-m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(m20)._m21(
            m21
        )._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    fun negateY(): Matrix4d {
        return _m10(-m10)._m11(-m11)._m12(-m12)._properties(properties and 18)
    }

    fun negateY(dest: Matrix4d): Matrix4d {
        return dest._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(-m10)._m11(-m11)._m12(-m12)._m13(m13)._m20(m20)._m21(
            m21
        )._m22(m22)._m23(m23)._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    fun negateZ(): Matrix4d {
        return _m20(-m20)._m21(-m21)._m22(-m22)._properties(properties and 18)
    }

    fun negateZ(dest: Matrix4d): Matrix4d {
        return dest._m00(m00)._m01(m01)._m02(m02)._m03(m03)._m10(m10)._m11(m11)._m12(m12)._m13(m13)._m20(-m20)
            ._m21(-m21)._m22(-m22)._m23(
                m23
            )._m30(m30)._m31(m31)._m32(m32)._m33(m33)._properties(properties and 18)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) &&
                JomlMath.isFinite(m03) && JomlMath.isFinite(m10) && JomlMath.isFinite(m11) &&
                JomlMath.isFinite(m12) && JomlMath.isFinite(m13) && JomlMath.isFinite(m20) &&
                JomlMath.isFinite(m21) && JomlMath.isFinite(m22) && JomlMath.isFinite(m23) &&
                JomlMath.isFinite(m30) && JomlMath.isFinite(m31) && JomlMath.isFinite(m32) && JomlMath.isFinite(m33)

    fun mirror(pos: Vector3d, normal: Vector3d): Matrix4d {
        return reflect(normal.x, normal.y, normal.z, pos.x, pos.y, pos.z)
    }

    fun skew(x: Double, y: Double): Matrix4d {
        return mul3x3(// works
            1.0, y, 0.0,
            x, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

    companion object {
        @JvmStatic
        fun projViewFromRectangle(
            eye: Vector3d,
            p: Vector3d,
            x: Vector3d,
            y: Vector3d,
            nearFarDist: Double,
            zeroToOne: Boolean,
            projDest: Matrix4d,
            viewDest: Matrix4d
        ) {
            var zx = y.y * x.z - y.z * x.y
            var zy = y.z * x.x - y.x * x.z
            var zz = y.x * x.y - y.y * x.x
            var zd = zx * (p.x - eye.x) + zy * (p.y - eye.y) + zz * (p.z - eye.z)
            val zs = if (zd >= 0.0) 1.0 else -1.0
            zx *= zs
            zy *= zs
            zz *= zs
            zd *= zs
            viewDest.setLookAt(eye.x, eye.y, eye.z, eye.x + zx, eye.y + zy, eye.z + zz, y.x, y.y, y.z)
            val px = viewDest.m00 * p.x + viewDest.m10 * p.y + viewDest.m20 * p.z + viewDest.m30
            val py = viewDest.m01 * p.x + viewDest.m11 * p.y + viewDest.m21 * p.z + viewDest.m31
            val tx = viewDest.m00 * x.x + viewDest.m10 * x.y + viewDest.m20 * x.z
            val ty = viewDest.m01 * y.x + viewDest.m11 * y.y + viewDest.m21 * y.z
            val len = sqrt(zx * zx + zy * zy + zz * zz)
            var near = zd / len
            val far: Double
            if (nearFarDist.isInfinite() && nearFarDist < 0.0) {
                far = near
                near = Double.POSITIVE_INFINITY
            } else if (nearFarDist.isInfinite() && nearFarDist > 0.0) {
                far = Double.POSITIVE_INFINITY
            } else if (nearFarDist < 0.0) {
                far = near
                near += nearFarDist
            } else {
                far = near + nearFarDist
            }
            projDest.setFrustum(px, px + tx, py, py + ty, near, far, zeroToOne)
        }
    }
}