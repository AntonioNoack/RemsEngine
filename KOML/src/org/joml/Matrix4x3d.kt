package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import org.joml.Vector3d.Companion.lengthSquared
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix4x3d {

    var m00 = 0.0
    var m01 = 0.0
    var m02 = 0.0
    var m10 = 0.0
    var m11 = 0.0
    var m12 = 0.0
    var m20 = 0.0
    var m21 = 0.0
    var m22 = 0.0
    var m30 = 0.0
    var m31 = 0.0
    var m32 = 0.0

    var flags = 0

    constructor() {
        m00 = 1.0
        m11 = 1.0
        m22 = 1.0
        flags = 28
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

    constructor(mat: Matrix3f) {
        this.set(mat)
    }

    constructor(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double,
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

    constructor(col0: Vector3d, col1: Vector3d, col2: Vector3d, col3: Vector3d) {
        this.set(col0, col1, col2, col3).determineProperties()
    }

    fun assume(properties: Int): Matrix4x3d {
        this.flags = properties
        return this
    }

    fun determineProperties(): Matrix4x3d {
        var flags = 0
        if (m00 == 1.0 && m01 == 0.0 && m02 == 0.0 && m10 == 0.0 && m11 == 1.0 && m12 == 0.0 && m20 == 0.0 && m21 == 0.0 && m22 == 1.0) {
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

    fun _properties(properties: Int): Matrix4x3d {
        this.flags = properties
        return this
    }

    fun _m00(m00: Double): Matrix4x3d {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Double): Matrix4x3d {
        this.m01 = m01
        return this
    }

    fun _m02(m02: Double): Matrix4x3d {
        this.m02 = m02
        return this
    }

    fun _m10(m10: Double): Matrix4x3d {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Double): Matrix4x3d {
        this.m11 = m11
        return this
    }

    fun _m12(m12: Double): Matrix4x3d {
        this.m12 = m12
        return this
    }

    fun _m20(m20: Double): Matrix4x3d {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Double): Matrix4x3d {
        this.m21 = m21
        return this
    }

    fun _m22(m22: Double): Matrix4x3d {
        this.m22 = m22
        return this
    }

    fun _m30(m30: Double): Matrix4x3d {
        this.m30 = m30
        return this
    }

    fun _m31(m31: Double): Matrix4x3d {
        this.m31 = m31
        return this
    }

    fun _m32(m32: Double): Matrix4x3d {
        this.m32 = m32
        return this
    }

    fun m00(m00: Double): Matrix4x3d {
        this.m00 = m00
        flags = flags and -17
        if (m00 != 1.0) {
            flags = flags and -13
        }
        return this
    }

    fun m01(m01: Double): Matrix4x3d {
        this.m01 = m01
        flags = flags and -17
        if (m01 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m02(m02: Double): Matrix4x3d {
        this.m02 = m02
        flags = flags and -17
        if (m02 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m10(m10: Double): Matrix4x3d {
        this.m10 = m10
        flags = flags and -17
        if (m10 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m11(m11: Double): Matrix4x3d {
        this.m11 = m11
        flags = flags and -17
        if (m11 != 1.0) {
            flags = flags and -13
        }
        return this
    }

    fun m12(m12: Double): Matrix4x3d {
        this.m12 = m12
        flags = flags and -17
        if (m12 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m20(m20: Double): Matrix4x3d {
        this.m20 = m20
        flags = flags and -17
        if (m20 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m21(m21: Double): Matrix4x3d {
        this.m21 = m21
        flags = flags and -17
        if (m21 != 0.0) {
            flags = flags and -13
        }
        return this
    }

    fun m22(m22: Double): Matrix4x3d {
        this.m22 = m22
        flags = flags and -17
        if (m22 != 1.0) {
            flags = flags and -13
        }
        return this
    }

    fun m30(m30: Double): Matrix4x3d {
        this.m30 = m30
        if (m30 != 0.0) {
            flags = flags and -5
        }
        return this
    }

    fun m31(m31: Double): Matrix4x3d {
        this.m31 = m31
        if (m31 != 0.0) {
            flags = flags and -5
        }
        return this
    }

    fun m32(m32: Double): Matrix4x3d {
        this.m32 = m32
        if (m32 != 0.0) {
            flags = flags and -5
        }
        return this
    }

    fun identity(): Matrix4x3d {
        if (flags and 4 == 0) {
            m00 = 1.0
            m01 = 0.0
            m02 = 0.0
            m10 = 0.0
            m11 = 1.0
            m12 = 0.0
            m20 = 0.0
            m21 = 0.0
            m22 = 1.0
            m30 = 0.0
            m31 = 0.0
            m32 = 0.0
            flags = 28
        }
        return this
    }

    fun set(m: Matrix4x3d): Matrix4x3d {
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

    fun set(m: Matrix4x3f): Matrix4x3d {
        m00 = m.m00.toDouble()
        m01 = m.m01.toDouble()
        m02 = m.m02.toDouble()
        m10 = m.m10.toDouble()
        m11 = m.m11.toDouble()
        m12 = m.m12.toDouble()
        m20 = m.m20.toDouble()
        m21 = m.m21.toDouble()
        m22 = m.m22.toDouble()
        m30 = m.m30.toDouble()
        m31 = m.m31.toDouble()
        m32 = m.m32.toDouble()
        flags = m.properties()
        return this
    }

    fun set(m: Matrix4d): Matrix4x3d {
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
        flags = m.flags and 28
        return this
    }

    fun get(dst: Matrix4d): Matrix4d {
        return dst.set4x3(this)
    }

    fun set(mat: Matrix3d): Matrix4x3d {
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

    fun set(mat: Matrix3f): Matrix4x3d {
        m00 = mat.m00.toDouble()
        m01 = mat.m01.toDouble()
        m02 = mat.m02.toDouble()
        m10 = mat.m10.toDouble()
        m11 = mat.m11.toDouble()
        m12 = mat.m12.toDouble()
        m20 = mat.m20.toDouble()
        m21 = mat.m21.toDouble()
        m22 = mat.m22.toDouble()
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        return determineProperties()
    }

    fun set(col0: Vector3d, col1: Vector3d, col2: Vector3d, col3: Vector3d): Matrix4x3d {
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

    fun set3x3(mat: Matrix4x3d): Matrix4x3d {
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

    fun set(axisAngle: AxisAngle4f): Matrix4x3d {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun set(axisAngle: AxisAngle4d): Matrix4x3d {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun set(q: Quaternionf): Matrix4x3d {
        return this.rotation(q)
    }

    fun set(q: Quaterniond): Matrix4x3d {
        return this.rotation(q)
    }

    @JvmOverloads
    fun mul(right: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else {
            if (flags and 8 != 0) this.mulTranslation(right, dst) else this.mulGeneric(right, dst)
        }
    }

    private fun mulGeneric(right: Matrix4x3d, dst: Matrix4x3d): Matrix4x3d {
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

    @JvmOverloads
    fun mul(right: Matrix4x3f, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.set(right)
        } else if (right.properties() and 4 != 0) {
            dst.set(this)
        } else {
            if (flags and 8 != 0) this.mulTranslation(right, dst) else this.mulGeneric(right, dst)
        }
    }

    private fun mulGeneric(right: Matrix4x3f, dst: Matrix4x3d): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        val m20 = m20
        val m21 = m21
        val m22 = m22
        val rm00 = right.m00.toDouble()
        val rm01 = right.m01.toDouble()
        val rm02 = right.m02.toDouble()
        val rm10 = right.m10.toDouble()
        val rm11 = right.m11.toDouble()
        val rm12 = right.m12.toDouble()
        val rm20 = right.m20.toDouble()
        val rm21 = right.m21.toDouble()
        val rm22 = right.m22.toDouble()
        val rm30 = right.m30.toDouble()
        val rm31 = right.m31.toDouble()
        val rm32 = right.m32.toDouble()
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

    fun mulTranslation(right: Matrix4x3d, dst: Matrix4x3d): Matrix4x3d {
        return dst._m00(right.m00)._m01(right.m01)._m02(right.m02)
            ._m10(right.m10)._m11(right.m11)._m12(right.m12)
            ._m20(right.m20)._m21(right.m21)._m22(right.m22)
            ._m30(right.m30 + m30)._m31(right.m31 + m31)._m32(right.m32 + m32)
            ._properties(right.properties() and 16)
    }

    fun mulTranslation(right: Matrix4x3f, dst: Matrix4x3d): Matrix4x3d {
        return dst._m00(right.m00.toDouble())._m01(right.m01.toDouble())._m02(right.m02.toDouble())
            ._m10(right.m10.toDouble())._m11(right.m11.toDouble())._m12(right.m12.toDouble())._m20(right.m20.toDouble())
            ._m21(right.m21.toDouble())._m22(right.m22.toDouble())._m30(right.m30.toDouble() + m30)
            ._m31(right.m31.toDouble() + m31)._m32(right.m32.toDouble() + m32)._properties(right.properties() and 16)
    }

    @JvmOverloads
    fun mulOrtho(view: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
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
        rm00: Double, rm01: Double, rm02: Double,
        rm10: Double, rm11: Double, rm12: Double,
        rm20: Double, rm21: Double, rm22: Double,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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

    @JvmOverloads
    fun fma(other: Matrix4x3d, otherFactor: Double, dst: Matrix4x3d = this): Matrix4x3d {
        dst._m00(other.m00 * otherFactor + m00)._m01(other.m01 * otherFactor + m01)._m02(other.m02 * otherFactor + m02)
            ._m10(other.m10 * otherFactor + m10)._m11(other.m11 * otherFactor + m11)._m12(other.m12 * otherFactor + m12)
            ._m20(other.m20 * otherFactor + m20)._m21(other.m21 * otherFactor + m21)._m22(other.m22 * otherFactor + m22)
            ._m30(other.m30 * otherFactor + m30)._m31(other.m31 * otherFactor + m31)._m32(other.m32 * otherFactor + m32)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun fma(other: Matrix4x3f, otherFactor: Double, dst: Matrix4x3d = this): Matrix4x3d {
        dst._m00(other.m00 * otherFactor + m00)._m01(other.m01 * otherFactor + m01)._m02(other.m02 * otherFactor + m02)
            ._m10(other.m10 * otherFactor + m10)._m11(other.m11 * otherFactor + m11)._m12(other.m12 * otherFactor + m12)
            ._m20(other.m20 * otherFactor + m20)._m21(other.m21 * otherFactor + m21)._m22(other.m22 * otherFactor + m22)
            ._m30(other.m30 * otherFactor + m30)._m31(other.m31 * otherFactor + m31)._m32(other.m32 * otherFactor + m32)
            ._properties(0)
        return dst
    }

    @JvmOverloads
    fun add(other: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun add(other: Matrix4x3f, dst: Matrix4x3d = this): Matrix4x3d {
        dst.m00 = m00 + other.m00.toDouble()
        dst.m01 = m01 + other.m01.toDouble()
        dst.m02 = m02 + other.m02.toDouble()
        dst.m10 = m10 + other.m10.toDouble()
        dst.m11 = m11 + other.m11.toDouble()
        dst.m12 = m12 + other.m12.toDouble()
        dst.m20 = m20 + other.m20.toDouble()
        dst.m21 = m21 + other.m21.toDouble()
        dst.m22 = m22 + other.m22.toDouble()
        dst.m30 = m30 + other.m30.toDouble()
        dst.m31 = m31 + other.m31.toDouble()
        dst.m32 = m32 + other.m32.toDouble()
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun sub(subtrahend: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun sub(subtrahend: Matrix4x3f, dst: Matrix4x3d = this): Matrix4x3d {
        dst.m00 = m00 - subtrahend.m00.toDouble()
        dst.m01 = m01 - subtrahend.m01.toDouble()
        dst.m02 = m02 - subtrahend.m02.toDouble()
        dst.m10 = m10 - subtrahend.m10.toDouble()
        dst.m11 = m11 - subtrahend.m11.toDouble()
        dst.m12 = m12 - subtrahend.m12.toDouble()
        dst.m20 = m20 - subtrahend.m20.toDouble()
        dst.m21 = m21 - subtrahend.m21.toDouble()
        dst.m22 = m22 - subtrahend.m22.toDouble()
        dst.m30 = m30 - subtrahend.m30.toDouble()
        dst.m31 = m31 - subtrahend.m31.toDouble()
        dst.m32 = m32 - subtrahend.m32.toDouble()
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
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
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double,
        m30: Double, m31: Double, m32: Double
    ): Matrix4x3d {
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
        return determineProperties()
    }

    @JvmOverloads
    fun set(m: DoubleArray, off: Int = 0): Matrix4x3d {
        return set(
            m[off], m[off + 1], m[off + 2],
            m[off + 3], m[off + 4], m[off + 5],
            m[off + 6], m[off + 7], m[off + 8],
            m[off + 9], m[off + 10], m[off + 11]
        )
    }

    @JvmOverloads
    fun set(m: FloatArray, off: Int = 0): Matrix4x3d {
        return set(
            m[off].toDouble(), m[off + 1].toDouble(), m[off + 2].toDouble(),
            m[off + 3].toDouble(), m[off + 4].toDouble(), m[off + 5].toDouble(),
            m[off + 6].toDouble(), m[off + 7].toDouble(), m[off + 8].toDouble(),
            m[off + 9].toDouble(), m[off + 10].toDouble(), m[off + 11].toDouble()
        )
    }

    fun determinant(): Double {
        return (m00 * m11 - m01 * m10) * m22 + (m02 * m10 - m00 * m12) * m21 + (m01 * m12 - m02 * m11) * m20
    }

    @JvmOverloads
    fun invert(dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (flags and 16 != 0) invertOrthonormal(dst) else invertGeneric(dst)
        }
    }

    private fun invertGeneric(dst: Matrix4x3d): Matrix4x3d {
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

    private fun invertOrthonormal(dst: Matrix4x3d): Matrix4x3d {
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
    fun invertOrtho(dst: Matrix4x3d = this): Matrix4x3d {
        val invM00 = 1.0 / m00
        val invM11 = 1.0 / m11
        val invM22 = 1.0 / m22
        dst.set(
            invM00, 0.0, 0.0,
            0.0, invM11, 0.0,
            0.0, 0.0, invM22,
            -m30 * invM00, -m31 * invM11, -m32 * invM22
        )
        dst.flags = 0
        return dst
    }

    @JvmOverloads
    fun transpose3x3(dst: Matrix4x3d = this): Matrix4x3d {
        val nm00 = m00
        val nm01 = m10
        val nm02 = m20
        val nm10 = m01
        val nm11 = m11
        val nm12 = m21
        val nm20 = m02
        val nm21 = m12
        val nm22 = m22
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.flags = flags
        return dst
    }

    fun transpose3x3(dst: Matrix3d): Matrix3d {
        dst.m00(m00)
        dst.m01(m10)
        dst.m02(m20)
        dst.m10(m01)
        dst.m11(m11)
        dst.m12(m21)
        dst.m20(m02)
        dst.m21(m12)
        dst.m22(m22)
        return dst
    }

    fun translation(x: Double, y: Double, z: Double): Matrix4x3d {
        if (flags and 4 == 0) {
            identity()
        }
        m30 = x
        m31 = y
        m32 = z
        flags = 24
        return this
    }

    fun translation(offset: Vector3f): Matrix4x3d {
        return this.translation(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translation(offset: Vector3d): Matrix4x3d {
        return this.translation(offset.x, offset.y, offset.z)
    }

    fun setTranslation(x: Double, y: Double, z: Double): Matrix4x3d {
        m30 = x
        m31 = y
        m32 = z
        flags = flags and -5
        return this
    }

    fun setTranslation(xyz: Vector3d): Matrix4x3d {
        return this.setTranslation(xyz.x, xyz.y, xyz.z)
    }

    fun getTranslation(dst: Vector3d): Vector3d {
        dst.x = m30
        dst.y = m31
        dst.z = m32
        return dst
    }

    fun getScale(dst: Vector3d): Vector3d {
        dst.x = sqrt(m00 * m00 + m01 * m01 + m02 * m02)
        dst.y = sqrt(m10 * m10 + m11 * m11 + m12 * m12)
        dst.z = sqrt(m20 * m20 + m21 * m21 + m22 * m22)
        return dst
    }

    fun getScaleLengthSquared(): Double {
        return m00 * m00 + m01 * m01 + m02 * m02 +
                m10 * m10 + m11 * m11 + m12 * m12 +
                m20 * m20 + m21 * m21 + m22 * m22
    }

    fun getScaleLength(): Double {
        return sqrt(getScaleLengthSquared())
    }

    fun distanceSquared(center: Vector3d): Double {
        val dx = center.x - m30
        val dy = center.y - m31
        val dz = center.z - m32
        return dx * dx + dy * dy + dz * dz
    }

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)} ${f(m30)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)} ${f(m31)}] " +
                "[${f(m02)} ${f(m12)} ${f(m22)} ${f(m32)}]]").addSigns()

    fun get(dst: Matrix4x3d): Matrix4x3d {
        return dst.set(this)
    }

    fun getUnnormalizedRotation(dst: Quaternionf): Quaternionf {
        return dst.setFromUnnormalized(this)
    }

    fun getNormalizedRotation(dst: Quaternionf): Quaternionf {
        return dst.setFromNormalized(this)
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
        arr[offset + 9] = m30
        arr[offset + 10] = m31
        arr[offset + 11] = m32
        return arr
    }

    @JvmOverloads
    fun get(arr: FloatArray, offset: Int = 0): FloatArray {
        arr[offset] = m00.toFloat()
        arr[offset + 1] = m01.toFloat()
        arr[offset + 2] = m02.toFloat()
        arr[offset + 3] = m10.toFloat()
        arr[offset + 4] = m11.toFloat()
        arr[offset + 5] = m12.toFloat()
        arr[offset + 6] = m20.toFloat()
        arr[offset + 7] = m21.toFloat()
        arr[offset + 8] = m22.toFloat()
        arr[offset + 9] = m30.toFloat()
        arr[offset + 10] = m31.toFloat()
        arr[offset + 11] = m32.toFloat()
        return arr
    }

    /*@JvmOverloads
    fun get4x4(arr: FloatArray, offset: Int = 0): FloatArray {
        MemUtil.INSTANCE.copy4x4(this, arr, offset)
        return arr
    }

    @JvmOverloads
    fun get4x4(arr: DoubleArray, offset: Int = 0): DoubleArray {
        MemUtil.INSTANCE.copy4x4(this, arr, offset)
        return arr
    }*/

    fun getTransposed(arr: DoubleArray, offset: Int): DoubleArray {
        arr[offset] = m00
        arr[offset + 1] = m10
        arr[offset + 2] = m20
        arr[offset + 3] = m30
        arr[offset + 4] = m01
        arr[offset + 5] = m11
        arr[offset + 6] = m21
        arr[offset + 7] = m31
        arr[offset + 8] = m02
        arr[offset + 9] = m12
        arr[offset + 10] = m22
        arr[offset + 11] = m32
        return arr
    }

    fun getTransposed(arr: DoubleArray): DoubleArray {
        return this.getTransposed(arr, 0)
    }

    fun zero(): Matrix4x3d {
        m00 = 0.0
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 0.0
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = 0.0
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 0
        return this
    }

    fun scaling(factor: Double): Matrix4x3d {
        return this.scaling(factor, factor, factor)
    }

    fun scaling(x: Double, y: Double, z: Double): Matrix4x3d {
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

    fun scaling(xyz: Vector3d): Matrix4x3d {
        return this.scaling(xyz.x, xyz.y, xyz.z)
    }

    fun rotation(angle: Double, x: Double, y: Double, z: Double): Matrix4x3d {
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

    private fun rotationInternal(angle: Double, x: Double, y: Double, z: Double): Matrix4x3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val C = 1.0 - cos
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

    fun rotationX(ang: Double): Matrix4x3d {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationY(ang: Double): Matrix4x3d {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationZ(ang: Double): Matrix4x3d {
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
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun rotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Matrix4x3d {
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

    fun rotationZYX(angleZ: Double, angleY: Double, angleX: Double): Matrix4x3d {
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

    fun rotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Matrix4x3d {
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

    fun setRotationXYZ(angleX: Double, angleY: Double, angleZ: Double): Matrix4x3d {
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

    fun setRotationZYX(angleZ: Double, angleY: Double, angleX: Double): Matrix4x3d {
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

    fun setRotationYXZ(angleY: Double, angleX: Double, angleZ: Double): Matrix4x3d {
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

    fun rotation(angle: Double, axis: Vector3d): Matrix4x3d {
        return this.rotation(angle, axis.x, axis.y, axis.z)
    }

    fun rotation(angle: Double, axis: Vector3f): Matrix4x3d {
        return this.rotation(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())
    }

    fun transform(v: Vector4d): Vector4d {
        return v.mul(this)
    }

    fun transform(v: Vector4d, dst: Vector4d?): Vector4d {
        return v.mul(this, dst!!)
    }

    fun transformPosition(v: Vector3d, dst: Vector3d = v): Vector3d {
        val vx = v.x
        val vy = v.y
        val vz = v.z
        return dst.set(
            m00 * vx + m10 * vy + m20 * vz + m30,
            m01 * vx + m11 * vy + m21 * vz + m31,
            m02 * vx + m12 * vy + m22 * vz + m32
        )
    }

    fun transformPosition(v: Vector3f, dst: Vector3f = v): Vector3f {
        val vx = v.x.toDouble()
        val vy = v.y.toDouble()
        val vz = v.z.toDouble()
        return dst.set(
            m00 * vx + m10 * vy + m20 * vz + m30,
            m01 * vx + m11 * vy + m21 * vz + m31,
            m02 * vx + m12 * vy + m22 * vz + m32
        )
    }

    fun transformDirection(v: Vector3d, dst: Vector3d = v): Vector3d {
        val vx = v.x
        val vy = v.y
        val vz = v.z
        return dst.set(
            m00 * vx + m10 * vy + m20 * vz,
            m01 * vx + m11 * vy + m21 * vz,
            m02 * vx + m12 * vy + m22 * vz
        )
    }

    fun transformDirection(v: Vector3f, dst: Vector3f = v): Vector3f {
        val vx = v.x.toDouble()
        val vy = v.y.toDouble()
        val vz = v.z.toDouble()
        return dst.set(
            m00 * vx + m10 * vy + m20 * vz,
            m01 * vx + m11 * vy + m21 * vz,
            m02 * vx + m12 * vy + m22 * vz
        )
    }

    fun set3x3(mat: Matrix3d): Matrix4x3d {
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

    fun set3x3(mat: Matrix3f): Matrix4x3d {
        m00 = mat.m00.toDouble()
        m01 = mat.m01.toDouble()
        m02 = mat.m02.toDouble()
        m10 = mat.m10.toDouble()
        m11 = mat.m11.toDouble()
        m12 = mat.m12.toDouble()
        m20 = mat.m20.toDouble()
        m21 = mat.m21.toDouble()
        m22 = mat.m22.toDouble()
        flags = 0
        return this
    }

    fun scale(xyz: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.scale(xyz.x, xyz.y, xyz.z, dst)
    }

    fun scale(xyz: Vector3d): Matrix4x3d {
        return this.scale(xyz.x, xyz.y, xyz.z, this)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, z: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) dst.scaling(x, y, z) else scaleGeneric(x, y, z, dst)
    }

    private fun scaleGeneric(x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
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

    fun scale(xyz: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.scale(xyz, xyz, xyz, dst)
    }

    fun scale(xyz: Double): Matrix4x3d {
        return this.scale(xyz, xyz, xyz)
    }

    fun scaleXY(x: Double, y: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.scale(x, y, 1.0, dst)
    }

    fun scaleXY(x: Double, y: Double): Matrix4x3d {
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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

    fun scaleAround(factor: Double, ox: Double, oy: Double, oz: Double): Matrix4x3d {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, this)
    }

    fun scaleAround(factor: Double, ox: Double, oy: Double, oz: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.scaleAround(factor, factor, factor, ox, oy, oz, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, z: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun rotate(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.rotation(ang, x, y, z)
        } else {
            if (flags and 8 != 0) this.rotateTranslation(ang, x, y, z, dst) else this.rotateGeneric(
                ang,
                x,
                y,
                z,
                dst
            )
        }
    }

    private fun rotateGeneric(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotateX(x * ang, dst)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotateY(y * ang, dst)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotateZ(
                z * ang,
                dst
            ) else rotateGenericInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateGenericInternal(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
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
        dst.m30 = m30
        dst.m31 = m31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    fun rotateTranslation(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
        val tx = m30
        val ty = m31
        val tz = m32
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            dst.rotationX(x * ang).setTranslation(tx, ty, tz)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            dst.rotationY(y * ang).setTranslation(tx, ty, tz)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) dst.rotationZ(z * ang)
                .setTranslation(tx, ty, tz) else rotateTranslationInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateTranslationInternal(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
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

    private fun rotateAroundAffine(
        quat: Quaterniond,
        ox: Double,
        oy: Double,
        oz: Double,
        dst: Matrix4x3d
    ): Matrix4x3d {
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
        dst._m20(m00 * rm20 + m10 * rm21 + m20 * rm22)._m21(m01 * rm20 + m11 * rm21 + m21 * rm22)._m22(
            m02 * rm20 + m12 * rm21 + m22 * rm22
        )._m00(nm00)._m01(nm01)._m02(nm02)._m10(nm10)._m11(nm11)._m12(nm12)
            ._m30(-nm00 * ox - nm10 * oy - m20 * oz + tm30)._m31(
                -nm01 * ox - nm11 * oy - m21 * oz + tm31
            )._m32(-nm02 * ox - nm12 * oy - m22 * oz + tm32)._properties(
                flags and -13
            )
        return dst
    }

    @JvmOverloads
    fun rotateAround(quat: Quaterniond, ox: Double, oy: Double, oz: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) rotationAround(quat, ox, oy, oz) else rotateAroundAffine(
            quat,
            ox,
            oy,
            oz,
            dst
        )
    }

    fun rotationAround(quat: Quaterniond, ox: Double, oy: Double, oz: Double): Matrix4x3d {
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
    fun rotateLocal(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return if (y == 0.0 && z == 0.0 && JomlMath.absEqualsOne(x)) {
            rotateLocalX(x * ang, dst)
        } else if (x == 0.0 && z == 0.0 && JomlMath.absEqualsOne(y)) {
            rotateLocalY(y * ang, dst)
        } else {
            if (x == 0.0 && y == 0.0 && JomlMath.absEqualsOne(z)) rotateLocalZ(
                z * ang,
                dst
            ) else rotateLocalInternal(ang, x, y, z, dst)
        }
    }

    private fun rotateLocalInternal(ang: Double, x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
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
    fun rotateLocalX(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
        dst.m00 = m00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = m10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = m20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.m30 = m30
        dst.m31 = nm31
        dst.m32 = nm32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateLocalY(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
        dst.m00 = nm00
        dst.m01 = m01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = m11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = m21
        dst.m22 = nm22
        dst.m30 = nm30
        dst.m31 = m31
        dst.m32 = nm32
        dst.flags = flags and -13
        return dst
    }

    @JvmOverloads
    fun rotateLocalZ(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = m02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = m12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = m22
        dst.m30 = nm30
        dst.m31 = nm31
        dst.m32 = m32
        dst.flags = flags and -13
        return dst
    }

    fun translate(offset: Vector3d): Matrix4x3d {
        return this.translate(offset.x, offset.y, offset.z)
    }

    fun translate(offset: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.translate(offset.x, offset.y, offset.z, dst)
    }

    fun translate(offset: Vector3f): Matrix4x3d {
        return this.translate(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translate(offset: Vector3f, dst: Matrix4x3d): Matrix4x3d {
        return this.translate(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble(), dst)
    }

    fun translate(x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
        return if (flags and 4 != 0) dst.translation(x, y, z) else translateGeneric(x, y, z, dst)
    }

    private fun translateGeneric(x: Double, y: Double, z: Double, dst: Matrix4x3d): Matrix4x3d {
        dst.m00 = m00
        dst.m01 = m01
        dst.m02 = m02
        dst.m10 = m10
        dst.m11 = m11
        dst.m12 = m12
        dst.m20 = m20
        dst.m21 = m21
        dst.m22 = m22
        dst.m30 = m00 * x + m10 * y + m20 * z + m30
        dst.m31 = m01 * x + m11 * y + m21 * z + m31
        dst.m32 = m02 * x + m12 * y + m22 * z + m32
        dst.flags = flags and -5
        return dst
    }

    fun translate(x: Double, y: Double, z: Double): Matrix4x3d {
        return if (flags and 4 != 0) {
            this.translation(x, y, z)
        } else {
            m30 += m00 * x + m10 * y + m20 * z
            m31 += m01 * x + m11 * y + m21 * z
            m32 += m02 * x + m12 * y + m22 * z
            flags = flags and -5
            this
        }
    }

    fun translateLocal(offset: Vector3f): Matrix4x3d {
        return this.translateLocal(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
    }

    fun translateLocal(offset: Vector3f, dst: Matrix4x3d): Matrix4x3d {
        return this.translateLocal(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble(), dst)
    }

    fun translateLocal(offset: Vector3d): Matrix4x3d {
        return this.translateLocal(offset.x, offset.y, offset.z)
    }

    fun translateLocal(offset: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.translateLocal(offset.x, offset.y, offset.z, dst)
    }

    @JvmOverloads
    fun translateLocal(x: Double, y: Double, z: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun rotateX(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateXInternal(ang: Double, dst: Matrix4x3d): Matrix4x3d {
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
    fun rotateY(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateYInternal(ang: Double, dst: Matrix4x3d): Matrix4x3d {
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
    fun rotateZ(ang: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateZInternal(ang: Double, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotateXYZ(angles: Vector3d): Matrix4x3d {
        return this.rotateXYZ(angles.x, angles.y, angles.z)
    }

    @JvmOverloads
    fun rotateXYZ(angleX: Double, angleY: Double, angleZ: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateXYZInternal(angleX: Double, angleY: Double, angleZ: Double, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotateZYX(angles: Vector3d): Matrix4x3d {
        return this.rotateZYX(angles.z, angles.y, angles.x)
    }

    @JvmOverloads
    fun rotateZYX(angleZ: Double, angleY: Double, angleX: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateZYXInternal(angleZ: Double, angleY: Double, angleX: Double, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotateYXZ(angles: Vector3d): Matrix4x3d {
        return this.rotateYXZ(angles.y, angles.x, angles.z)
    }

    @JvmOverloads
    fun rotateYXZ(angleY: Double, angleX: Double, angleZ: Double, dst: Matrix4x3d = this): Matrix4x3d {
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

    private fun rotateYXZInternal(angleY: Double, angleX: Double, angleZ: Double, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotation(angleAxis: AxisAngle4f): Matrix4x3d {
        return this.rotation(
            angleAxis.angle.toDouble(),
            angleAxis.x.toDouble(),
            angleAxis.y.toDouble(),
            angleAxis.z.toDouble()
        )
    }

    fun rotation(angleAxis: AxisAngle4d): Matrix4x3d {
        return this.rotation(angleAxis.angle, angleAxis.x, angleAxis.y, angleAxis.z)
    }

    fun rotation(quat: Quaterniond): Matrix4x3d {
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

    fun rotation(quat: Quaternionf): Matrix4x3d {
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
    ): Matrix4x3d {
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

    fun translationRotateScale(translation: Vector3f, quat: Quaternionf, scale: Vector3f): Matrix4x3d {
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

    fun translationRotateScale(translation: Vector3d, quat: Quaterniond, scale: Vector3d): Matrix4x3d {
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

    fun translationRotateScaleMul(
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
        m: Matrix4x3d
    ): Matrix4x3d {
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
        quat: Quaterniond,
        scale: Vector3d,
        m: Matrix4x3d
    ): Matrix4x3d {
        return this.translationRotateScaleMul(
            translation.x,
            translation.y,
            translation.z,
            quat.x,
            quat.y,
            quat.z,
            quat.w,
            scale.x,
            scale.y,
            scale.z,
            m
        )
    }

    fun translationRotate(tx: Double, ty: Double, tz: Double, quat: Quaterniond): Matrix4x3d {
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
        m00 = 1.0 - (q11 + q22)
        m01 = q01 + q23
        m02 = q02 - q13
        m10 = q01 - q23
        m11 = 1.0 - (q22 + q00)
        m12 = q12 + q03
        m20 = q02 + q13
        m21 = q12 - q03
        m22 = 1.0 - (q11 + q00)
        m30 = tx
        m31 = ty
        m32 = tz
        flags = 16
        return this
    }

    fun translationRotate(
        tx: Double, ty: Double, tz: Double,
        qx: Double, qy: Double, qz: Double, qw: Double
    ): Matrix4x3d {
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

    fun translationRotate(translation: Vector3d, quat: Quaterniond): Matrix4x3d {
        return this.translationRotate(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    fun translationRotateMul(tx: Double, ty: Double, tz: Double, quat: Quaternionf, mat: Matrix4x3d): Matrix4x3d {
        return this.translationRotateMul(
            tx, ty, tz, quat.x.toDouble(), quat.y.toDouble(), quat.z.toDouble(), quat.w.toDouble(), mat
        )
    }

    fun translationRotateMul(
        tx: Double, ty: Double, tz: Double,
        qx: Double, qy: Double, qz: Double, qw: Double, mat: Matrix4x3d
    ): Matrix4x3d {
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
        qx: Double, qy: Double, qz: Double, qw: Double
    ): Matrix4x3d {
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
        return _m00(1.0 - q11 - q22)._m01(q01 + q23)._m02(q02 - q13)._m10(q01 - q23)._m11(1.0 - q22 - q00)
            ._m12(q12 + q03)._m20(q02 + q13)._m21(q12 - q03)._m22(1.0 - q11 - q00)._m30(
                -m00 * tx - m10 * ty - m20 * tz
            )._m31(-m01 * tx - m11 * ty - m21 * tz)._m32(-m02 * tx - m12 * ty - m22 * tz)._properties(16)
    }

    fun translationRotateInvert(translation: Vector3d, quat: Quaterniond): Matrix4x3d {
        return this.translationRotateInvert(translation.x, translation.y, translation.z, quat.x, quat.y, quat.z, quat.w)
    }

    @JvmOverloads
    fun rotate(quat: Quaterniond, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.rotation(quat)
        } else {
            if (flags and 8 != 0) this.rotateTranslation(quat, dst) else this.rotateGeneric(quat, dst)
        }
    }

    private fun rotateGeneric(quat: Quaterniond, dst: Matrix4x3d): Matrix4x3d {
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
    fun rotate(quat: Quaternionf, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.rotation(quat)
        } else {
            if (flags and 8 != 0) this.rotateTranslation(quat, dst) else this.rotateGeneric(quat, dst)
        }
    }

    private fun rotateGeneric(quat: Quaternionf, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotateTranslation(quat: Quaterniond, dst: Matrix4x3d): Matrix4x3d {
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

    fun rotateTranslation(quat: Quaternionf, dst: Matrix4x3d): Matrix4x3d {
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
    fun rotateLocal(quat: Quaterniond, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun rotateLocal(quat: Quaternionf, dst: Matrix4x3d = this): Matrix4x3d {
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

    fun rotate(axisAngle: AxisAngle4f): Matrix4x3d {
        return this.rotate(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble()
        )
    }

    fun rotate(axisAngle: AxisAngle4f, dst: Matrix4x3d): Matrix4x3d {
        return this.rotate(
            axisAngle.angle.toDouble(),
            axisAngle.x.toDouble(),
            axisAngle.y.toDouble(),
            axisAngle.z.toDouble(),
            dst
        )
    }

    fun rotate(axisAngle: AxisAngle4d): Matrix4x3d {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z)
    }

    fun rotate(axisAngle: AxisAngle4d, dst: Matrix4x3d): Matrix4x3d {
        return this.rotate(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z, dst)
    }

    fun rotate(angle: Double, axis: Vector3d): Matrix4x3d {
        return this.rotate(angle, axis.x, axis.y, axis.z)
    }

    fun rotate(angle: Double, axis: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.rotate(angle, axis.x, axis.y, axis.z, dst)
    }

    fun rotate(angle: Double, axis: Vector3f): Matrix4x3d {
        return this.rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())
    }

    fun rotate(angle: Double, axis: Vector3f, dst: Matrix4x3d): Matrix4x3d {
        return this.rotate(angle, axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble(), dst)
    }

    fun getRow(row: Int, dst: Vector4d): Vector4d {
        when (row) {
            0 -> {
                dst.x = m00
                dst.y = m10
                dst.z = m20
                dst.w = m30
            }
            1 -> {
                dst.x = m01
                dst.y = m11
                dst.z = m21
                dst.w = m31
            }
            else -> {
                dst.x = m02
                dst.y = m12
                dst.z = m22
                dst.w = m32
            }
        }
        return dst
    }

    fun setRow(row: Int, src: Vector4d): Matrix4x3d {
        when (row) {
            0 -> {
                m00 = src.x
                m10 = src.y
                m20 = src.z
                m30 = src.w
            }
            1 -> {
                m01 = src.x
                m11 = src.y
                m21 = src.z
                m31 = src.w
            }
            else -> {
                m02 = src.x
                m12 = src.y
                m22 = src.z
                m32 = src.w
            }
        }
        flags = 0
        return this
    }

    fun getColumn(column: Int, dst: Vector3d): Vector3d {
        when (column) {
            0 -> {
                dst.x = m00
                dst.y = m01
                dst.z = m02
            }
            1 -> {
                dst.x = m10
                dst.y = m11
                dst.z = m12
            }
            2 -> {
                dst.x = m20
                dst.y = m21
                dst.z = m22
            }
            else -> {
                dst.x = m30
                dst.y = m31
                dst.z = m32
            }
        }
        return dst
    }

    fun setColumn(column: Int, src: Vector3d): Matrix4x3d {
        when (column) {
            0 -> {
                m00 = src.x
                m01 = src.y
                m02 = src.z
            }
            1 -> {
                m10 = src.x
                m11 = src.y
                m12 = src.z
            }
            2 -> {
                m20 = src.x
                m21 = src.y
                m22 = src.z
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
    fun normal(dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.identity()
        } else {
            if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
        }
    }

    private fun normalOrthonormal(dst: Matrix4x3d): Matrix4x3d {
        if (dst !== this) {
            dst.set(this)
        }
        return dst._properties(16)
    }

    private fun normalGeneric(dst: Matrix4x3d): Matrix4x3d {
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

    fun normal(dst: Matrix3d): Matrix3d {
        return if (flags and 16 != 0) this.normalOrthonormal(dst) else this.normalGeneric(dst)
    }

    private fun normalOrthonormal(dst: Matrix3d): Matrix3d {
        return dst.set(this)
    }

    private fun normalGeneric(dst: Matrix3d): Matrix3d {
        val m00m11 = m00 * m11
        val m01m10 = m01 * m10
        val m02m10 = m02 * m10
        val m00m12 = m00 * m12
        val m01m12 = m01 * m12
        val m02m11 = m02 * m11
        val det = (m00m11 - m01m10) * m22 + (m02m10 - m00m12) * m21 + (m01m12 - m02m11) * m20
        val s = 1.0 / det
        dst.m00((m11 * m22 - m21 * m12) * s)
        dst.m01((m20 * m12 - m10 * m22) * s)
        dst.m02((m10 * m21 - m20 * m11) * s)
        dst.m10((m21 * m02 - m01 * m22) * s)
        dst.m11((m00 * m22 - m20 * m02) * s)
        dst.m12((m20 * m01 - m00 * m21) * s)
        dst.m20((m01m12 - m02m11) * s)
        dst.m21((m02m10 - m00m12) * s)
        dst.m22((m00m11 - m01m10) * s)
        return dst
    }

    fun cofactor3x3(dst: Matrix3d): Matrix3d {
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
    fun cofactor3x3(dst: Matrix4x3d = this): Matrix4x3d {
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
    fun normalize3x3(dst: Matrix4x3d = this): Matrix4x3d {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        dst.m00 = m00 * invXlen
        dst.m01 = m01 * invXlen
        dst.m02 = m02 * invXlen
        dst.m10 = m10 * invYlen
        dst.m11 = m11 * invYlen
        dst.m12 = m12 * invYlen
        dst.m20 = m20 * invZlen
        dst.m21 = m21 * invZlen
        dst.m22 = m22 * invZlen
        return dst
    }

    fun normalize3x3(dst: Matrix3d): Matrix3d {
        val invXlen = JomlMath.invsqrt(m00 * m00 + m01 * m01 + m02 * m02)
        val invYlen = JomlMath.invsqrt(m10 * m10 + m11 * m11 + m12 * m12)
        val invZlen = JomlMath.invsqrt(m20 * m20 + m21 * m21 + m22 * m22)
        dst.m00(m00 * invXlen)
        dst.m01(m01 * invXlen)
        dst.m02(m02 * invXlen)
        dst.m10(m10 * invYlen)
        dst.m11(m11 * invYlen)
        dst.m12(m12 * invYlen)
        dst.m20(m20 * invZlen)
        dst.m21(m21 * invZlen)
        dst.m22(m22 * invZlen)
        return dst
    }

    @JvmOverloads
    fun reflect(a: Double, b: Double, c: Double, d: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return if (flags and 4 != 0) {
            dst.reflection(a, b, c, d)
        } else {
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
    fun reflect(
        nx: Double,
        ny: Double,
        nz: Double,
        px: Double,
        py: Double,
        pz: Double,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return this.reflect(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz, dst)
    }

    fun reflect(normal: Vector3d, point: Vector3d): Matrix4x3d {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    @JvmOverloads
    fun reflect(orientation: Quaterniond, point: Vector3d, dst: Matrix4x3d = this): Matrix4x3d {
        val num1 = orientation.x + orientation.x
        val num2 = orientation.y + orientation.y
        val num3 = orientation.z + orientation.z
        val normalX = orientation.x * num3 + orientation.w * num2
        val normalY = orientation.y * num3 - orientation.w * num1
        val normalZ = 1.0 - (orientation.x * num1 + orientation.y * num2)
        return this.reflect(normalX, normalY, normalZ, point.x, point.y, point.z, dst)
    }

    fun reflect(normal: Vector3d, point: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.reflect(normal.x, normal.y, normal.z, point.x, point.y, point.z, dst)
    }

    fun reflection(a: Double, b: Double, c: Double, d: Double): Matrix4x3d {
        val da = a + a
        val db = b + b
        val dc = c + c
        val dd = d + d
        m00 = 1.0 - da * a
        m01 = -da * b
        m02 = -da * c
        m10 = -db * a
        m11 = 1.0 - db * b
        m12 = -db * c
        m20 = -dc * a
        m21 = -dc * b
        m22 = 1.0 - dc * c
        m30 = -dd * a
        m31 = -dd * b
        m32 = -dd * c
        flags = 16
        return this
    }

    fun reflection(nx: Double, ny: Double, nz: Double, px: Double, py: Double, pz: Double): Matrix4x3d {
        val invLength = JomlMath.invsqrt(nx * nx + ny * ny + nz * nz)
        val nnx = nx * invLength
        val nny = ny * invLength
        val nnz = nz * invLength
        return this.reflection(nnx, nny, nnz, -nnx * px - nny * py - nnz * pz)
    }

    fun reflection(normal: Vector3d, point: Vector3d): Matrix4x3d {
        return this.reflection(normal.x, normal.y, normal.z, point.x, point.y, point.z)
    }

    fun reflection(orientation: Quaterniond, point: Vector3d): Matrix4x3d {
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
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
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dst: Matrix4x3d
    ): Matrix4x3d {
        return this.ortho(left, right, bottom, top, zNear, zFar, false, dst)
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
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
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        dst: Matrix4x3d
    ): Matrix4x3d {
        return this.orthoLH(left, right, bottom, top, zNear, zFar, false, dst)
    }

    fun setOrtho(
        left: Double,
        right: Double,
        bottom: Double,
        top: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4x3d {
        m00 = 2.0 / (right - left)
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / (top - bottom)
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
        m30 = (right + left) / (left - right)
        m31 = (top + bottom) / (bottom - top)
        m32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrtho(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4x3d {
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
    ): Matrix4x3d {
        m00 = 2.0 / (right - left)
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / (top - bottom)
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
        m30 = (right + left) / (left - right)
        m31 = (top + bottom) / (bottom - top)
        m32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoLH(left: Double, right: Double, bottom: Double, top: Double, zNear: Double, zFar: Double): Matrix4x3d {
        return this.setOrthoLH(left, right, bottom, top, zNear, zFar, false)
    }

    @JvmOverloads
    fun orthoSymmetric(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val rm00 = 2.0 / width
        val rm11 = 2.0 / height
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
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

    fun orthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.orthoSymmetric(width, height, zNear, zFar, false, dst)
    }

    @JvmOverloads
    fun orthoSymmetricLH(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean = false,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val rm00 = 2.0 / width
        val rm11 = 2.0 / height
        val rm22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
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

    fun orthoSymmetricLH(width: Double, height: Double, zNear: Double, zFar: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.orthoSymmetricLH(width, height, zNear, zFar, false, dst)
    }

    fun setOrthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double, zZeroToOne: Boolean): Matrix4x3d {
        m00 = 2.0 / width
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / height
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = (if (zZeroToOne) 1.0 else 2.0) / (zNear - zFar)
        m30 = 0.0
        m31 = 0.0
        m32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoSymmetric(width: Double, height: Double, zNear: Double, zFar: Double): Matrix4x3d {
        return this.setOrthoSymmetric(width, height, zNear, zFar, false)
    }

    fun setOrthoSymmetricLH(
        width: Double,
        height: Double,
        zNear: Double,
        zFar: Double,
        zZeroToOne: Boolean
    ): Matrix4x3d {
        m00 = 2.0 / width
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / height
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = (if (zZeroToOne) 1.0 else 2.0) / (zFar - zNear)
        m30 = 0.0
        m31 = 0.0
        m32 = (if (zZeroToOne) zNear else zFar + zNear) / (zNear - zFar)
        flags = 0
        return this
    }

    fun setOrthoSymmetricLH(width: Double, height: Double, zNear: Double, zFar: Double): Matrix4x3d {
        return this.setOrthoSymmetricLH(width, height, zNear, zFar, false)
    }

    @JvmOverloads
    fun ortho2D(left: Double, right: Double, bottom: Double, top: Double, dst: Matrix4x3d = this): Matrix4x3d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
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
    fun ortho2DLH(left: Double, right: Double, bottom: Double, top: Double, dst: Matrix4x3d = this): Matrix4x3d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
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

    fun setOrtho2D(left: Double, right: Double, bottom: Double, top: Double): Matrix4x3d {
        m00 = 2.0 / (right - left)
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / (top - bottom)
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = -1.0
        m30 = -(right + left) / (right - left)
        m31 = -(top + bottom) / (top - bottom)
        m32 = 0.0
        flags = 0
        return this
    }

    fun setOrtho2DLH(left: Double, right: Double, bottom: Double, top: Double): Matrix4x3d {
        m00 = 2.0 / (right - left)
        m01 = 0.0
        m02 = 0.0
        m10 = 0.0
        m11 = 2.0 / (top - bottom)
        m12 = 0.0
        m20 = 0.0
        m21 = 0.0
        m22 = 1.0
        m30 = -(right + left) / (right - left)
        m31 = -(top + bottom) / (top - bottom)
        m32 = 0.0
        flags = 0
        return this
    }

    fun lookAlong(dir: Vector3d, up: Vector3d): Matrix4x3d {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    fun lookAlong(dir: Vector3d, up: Vector3d, dst: Matrix4x3d): Matrix4x3d {
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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

    fun setLookAlong(dir: Vector3d, up: Vector3d): Matrix4x3d {
        return this.setLookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun setLookAlong(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix4x3d {
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
        m00 = leftX
        m01 = upnX
        m02 = dirX0
        m10 = leftY
        m11 = upnY
        m12 = dirY0
        m20 = leftZ
        m21 = upnZ
        m22 = dirZ0
        m30 = 0.0
        m31 = 0.0
        m32 = 0.0
        flags = 16
        return this
    }

    fun setLookAt(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4x3d {
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
    ): Matrix4x3d {
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
        m00 = leftX
        m01 = upnX
        m02 = dirX
        m10 = leftY
        m11 = upnY
        m12 = dirY
        m20 = leftZ
        m21 = upnZ
        m22 = dirZ
        m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        flags = 16
        return this
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.lookAt(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAt(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4x3d {
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        return if (flags and 4 != 0) dst.setLookAt(
            eyeX,
            eyeY,
            eyeZ,
            centerX,
            centerY,
            centerZ,
            upX,
            upY,
            upZ
        ) else lookAtGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
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
        dst: Matrix4x3d
    ): Matrix4x3d {
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
        dst.m20 = m00 * leftZ + m10 * upnZ + m20 * dirZ
        dst.m21 = m01 * leftZ + m11 * upnZ + m21 * dirZ
        dst.m22 = m02 * leftZ + m12 * upnZ + m22 * dirZ
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.flags = flags and -13
        return dst
    }

    fun setLookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4x3d {
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
    ): Matrix4x3d {
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
        m00 = leftX
        m01 = upnX
        m02 = dirX
        m10 = leftY
        m11 = upnY
        m12 = dirY
        m20 = leftZ
        m21 = upnZ
        m22 = dirZ
        m30 = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        m31 = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        m32 = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        flags = 16
        return this
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.lookAtLH(eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z, dst)
    }

    fun lookAtLH(eye: Vector3d, center: Vector3d, up: Vector3d): Matrix4x3d {
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        return if (flags and 4 != 0) dst.setLookAtLH(
            eyeX,
            eyeY,
            eyeZ,
            centerX,
            centerY,
            centerZ,
            upX,
            upY,
            upZ
        ) else lookAtLHGeneric(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ, dst)
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
        dst: Matrix4x3d
    ): Matrix4x3d {
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
        dst.m20 = m00 * leftZ + m10 * upnZ + m20 * dirZ
        dst.m21 = m01 * leftZ + m11 * upnZ + m21 * dirZ
        dst.m22 = m02 * leftZ + m12 * upnZ + m22 * dirZ
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.flags = flags and -13
        return dst
    }

    fun frustumPlane(which: Int, dst: Vector4d): Vector4d {
        when (which) {
            0 -> dst.set(m00, m10, m20, 1.0 + m30).normalize()
            1 -> dst.set(-m00, -m10, -m20, 1.0 - m30).normalize()
            2 -> dst.set(m01, m11, m21, 1.0 + m31).normalize()
            3 -> dst.set(-m01, -m11, -m21, 1.0 - m31).normalize()
            4 -> dst.set(m02, m12, m22, 1.0 + m32).normalize()
            else -> dst.set(-m02, -m12, -m22, 1.0 - m32).normalize()
        }
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

    fun shadow(light: Vector4d, a: Double, b: Double, c: Double, d: Double): Matrix4x3d {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, this)
    }

    fun shadow(light: Vector4d, a: Double, b: Double, c: Double, d: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m02 = nm02
        dst.m10 = nm10
        dst.m11 = nm11
        dst.m12 = nm12
        dst.m20 = nm20
        dst.m21 = nm21
        dst.m22 = nm22
        dst.flags = flags and -29
        return dst
    }

    @JvmOverloads
    fun shadow(light: Vector4d, planeTransform: Matrix4x3d, dst: Matrix4x3d = this): Matrix4x3d {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return this.shadow(light.x, light.y, light.z, light.w, a, b, c, d, dst)
    }

    @JvmOverloads
    fun shadow(
        lightX: Double,
        lightY: Double,
        lightZ: Double,
        lightW: Double,
        planeTransform: Matrix4x3d,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val a = planeTransform.m10
        val b = planeTransform.m11
        val c = planeTransform.m12
        val d = -a * planeTransform.m30 - b * planeTransform.m31 - c * planeTransform.m32
        return this.shadow(lightX, lightY, lightZ, lightW, a, b, c, d, dst)
    }

    fun billboardCylindrical(objPos: Vector3d, targetPos: Vector3d, up: Vector3d): Matrix4x3d {
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
        m00 = leftX
        m01 = leftY
        m02 = leftZ
        m10 = up.x
        m11 = up.y
        m12 = up.z
        m20 = dirX
        m21 = dirY
        m22 = dirZ
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d, up: Vector3d): Matrix4x3d {
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
        m00 = leftX
        m01 = leftY
        m02 = leftZ
        m10 = upX
        m11 = upY
        m12 = upZ
        m20 = dirX
        m21 = dirY
        m22 = dirZ
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
        return this
    }

    fun billboardSpherical(objPos: Vector3d, targetPos: Vector3d): Matrix4x3d {
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
        m00 = 1.0 - q11
        m01 = q01
        m02 = -q13
        m10 = q01
        m11 = 1.0 - q00
        m12 = q03
        m20 = q13
        m21 = -q03
        m22 = 1.0 - q11 - q00
        m30 = objPos.x
        m31 = objPos.y
        m32 = objPos.z
        flags = 16
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
        temp = (m10).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m11).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m12).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m20).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m21).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m22).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m30).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m31).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = (m32).toBits()
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null) {
            false
        } else if (other !is Matrix4x3d) {
            false
        } else {
            if ((m00) != (other.m00)) {
                false
            } else if ((m01) != (other.m01)) {
                false
            } else if ((m02) != (other.m02)) {
                false
            } else if ((m10) != (other.m10)) {
                false
            } else if ((m11) != (other.m11)) {
                false
            } else if ((m12) != (other.m12)) {
                false
            } else if ((m20) != (other.m20)) {
                false
            } else if ((m21) != (other.m21)) {
                false
            } else if ((m22) != (other.m22)) {
                false
            } else if ((m30) != (other.m30)) {
                false
            } else if ((m31) != (other.m31)) {
                false
            } else {
                (m32) == (other.m32)
            }
        }
    }

    fun equals(m: Matrix4x3d?, delta: Double): Boolean {
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
        } else if (!Runtime.equals(m22, m.m22, delta)) {
            false
        } else if (!Runtime.equals(m30, m.m30, delta)) {
            false
        } else if (!Runtime.equals(m31, m.m31, delta)) {
            false
        } else {
            Runtime.equals(m32, m.m32, delta)
        }
    }

    @JvmOverloads
    fun pick(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        viewport: IntArray,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
        val sx = viewport[2].toDouble() / width
        val sy = viewport[3].toDouble() / height
        val tx = (viewport[2].toDouble() + 2.0 * (viewport[0].toDouble() - x)) / width
        val ty = (viewport[3].toDouble() + 2.0 * (viewport[1].toDouble() - y)) / height
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

    fun swap(other: Matrix4x3d): Matrix4x3d {
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
        tmp = m30
        m30 = other.m30
        other.m30 = tmp
        tmp = m31
        m31 = other.m31
        other.m31 = tmp
        tmp = m32
        m32 = other.m32
        other.m32 = tmp
        val props = flags
        flags = other.flags
        other.flags = props
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
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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

    fun arcball(radius: Double, center: Vector3d, angleX: Double, angleY: Double, dst: Matrix4x3d): Matrix4x3d {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, dst)
    }

    fun arcball(radius: Double, center: Vector3d, angleX: Double, angleY: Double): Matrix4x3d {
        return this.arcball(radius, center.x, center.y, center.z, angleX, angleY, this)
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
    ): Matrix4x3d {
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

    fun transformAab(min: Vector3d, max: Vector3d, outMin: Vector3d, outMax: Vector3d): Matrix4x3d {
        return this.transformAab(min.x, min.y, min.z, max.x, max.y, max.z, outMin, outMax)
    }

    fun mix(other: Matrix4x3d, t: Double, dst: Matrix4x3d = this): Matrix4x3d {
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
    fun lerp(other: Matrix4x3d, t: Double, dst: Matrix4x3d = this): Matrix4x3d {
        return mix(other, t, dst)
    }

    fun rotateTowards(dir: Vector3d, up: Vector3d, dst: Matrix4x3d): Matrix4x3d {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, dst)
    }

    fun rotateTowards(dir: Vector3d, up: Vector3d): Matrix4x3d {
        return this.rotateTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z, this)
    }

    @JvmOverloads
    fun rotateTowards(
        dirX: Double,
        dirY: Double,
        dirZ: Double,
        upX: Double,
        upY: Double,
        upZ: Double,
        dst: Matrix4x3d = this
    ): Matrix4x3d {
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

    fun rotationTowards(dir: Vector3d, up: Vector3d): Matrix4x3d {
        return this.rotationTowards(dir.x, dir.y, dir.z, up.x, up.y, up.z)
    }

    fun rotationTowards(dirX: Double, dirY: Double, dirZ: Double, upX: Double, upY: Double, upZ: Double): Matrix4x3d {
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

    fun translationRotateTowards(pos: Vector3d, dir: Vector3d, up: Vector3d): Matrix4x3d {
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
    ): Matrix4x3d {
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

    fun obliqueZ(a: Double, b: Double): Matrix4x3d {
        m20 += m00 * a + m10 * b
        m21 += m01 * a + m11 * b
        m22 += m02 * a + m12 * b
        flags = 0
        return this
    }

    fun obliqueZ(a: Double, b: Double, dst: Matrix4x3d): Matrix4x3d {
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
    fun mapXZY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(m20)._m11(m21)._m12(m22)._m20(m10)._m21(m11)._m22(m12)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapXZnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(m20)._m11(m21)._m12(m22)._m20(-m10)._m21(-m11)._m22(-m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapXnYnZ(dst: Matrix4x3d = this): Matrix4x3d {
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapXnZY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m10)._m21(m11)._m22(m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapXnZnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYXZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(m00)._m11(m01)._m12(m02)._m20(m20)._m21(m21)._m22(m22)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYXnZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(m00)._m11(m01)._m12(m02)._m20(-m20)._m21(-m21)._m22(-m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYZX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(m20)._m11(m21)._m12(m22)._m20(m00)._m21(m01)._m22(m02)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYZnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(m20)._m11(m21)._m12(m22)._m20(-m00)._m21(-m01)._m22(-m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYnXZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m20)._m21(m21)._m22(m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYnXnZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYnZX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m00)._m21(m01)._m22(m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapYnZnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m10)._m01(m11)._m02(m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZXY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZXnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZYX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(m10)._m11(m11)._m12(m12)._m20(m00)._m21(m01)._m22(m02)._m30(m30)
            ._m31(
                m31
            )._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZYnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(m10)._m11(m11)._m12(m12)._m20(-m00)._m21(-m01)._m22(-m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZnXY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZnXnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZnYX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m00)._m21(m01)._m22(m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapZnYnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(m20)._m01(m21)._m02(m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXYnZ(dst: Matrix4x3d = this): Matrix4x3d {
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(m10)._m11(m11)._m12(m12)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXZY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(m20)._m11(m21)._m12(m22)._m20(m10)._m21(m11)._m22(m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXZnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(m20)._m11(m21)._m12(m22)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXnYZ(dst: Matrix4x3d = this): Matrix4x3d {
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m20)._m21(m21)._m22(m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXnYnZ(dst: Matrix4x3d = this): Matrix4x3d {
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXnZY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m10)._m21(m11)._m22(m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnXnZnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYXZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(m00)._m11(m01)._m12(m02)._m20(m20)._m21(m21)._m22(m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYXnZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(m00)._m11(m01)._m12(m02)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYZX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(m20)._m11(m21)._m12(m22)._m20(m00)._m21(m01)._m22(m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYZnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(m20)._m11(m21)._m12(m22)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYnXZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m20)._m21(m21)._m22(m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYnXnZ(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m20)._m21(-m21)._m22(-m22)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYnZX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(m00)._m21(m01)._m22(m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnYnZnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m10)._m01(-m11)._m02(-m12)._m10(-m20)._m11(-m21)._m12(-m22)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZXY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(m00)._m11(m01)._m12(m02)._m20(m10)._m21(m11)._m22(m12)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZXnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(m00)._m11(m01)._m12(m02)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZYX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(m10)._m11(m11)._m12(m12)._m20(m00)._m21(m01)._m22(m02)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZYnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(m10)._m11(m11)._m12(m12)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZnXY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(m10)._m21(m11)._m22(m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZnXnY(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        val m10 = m10
        val m11 = m11
        val m12 = m12
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m00)._m11(-m01)._m12(-m02)._m20(-m10)._m21(-m11)._m22(-m12)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZnYX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m00)._m21(m01)._m22(m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    @JvmOverloads
    fun mapnZnYnX(dst: Matrix4x3d = this): Matrix4x3d {
        val m00 = m00
        val m01 = m01
        val m02 = m02
        return dst._m00(-m20)._m01(-m21)._m02(-m22)._m10(-m10)._m11(-m11)._m12(-m12)._m20(-m00)._m21(-m01)._m22(-m02)
            ._m30(
                m30
            )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    fun negateX(): Matrix4x3d {
        return _m00(-m00)._m01(-m01)._m02(-m02)._properties(flags and 16)
    }

    fun negateX(dst: Matrix4x3d): Matrix4x3d {
        return dst._m00(-m00)._m01(-m01)._m02(-m02)._m10(m10)._m11(m11)._m12(m12)._m20(m20)._m21(m21)._m22(m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    fun negateY(): Matrix4x3d {
        return _m10(-m10)._m11(-m11)._m12(-m12)._properties(flags and 16)
    }

    fun negateY(dst: Matrix4x3d): Matrix4x3d {
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(-m10)._m11(-m11)._m12(-m12)._m20(m20)._m21(m21)._m22(m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    fun negateZ(): Matrix4x3d {
        return _m20(-m20)._m21(-m21)._m22(-m22)._properties(flags and 16)
    }

    fun negateZ(dst: Matrix4x3d): Matrix4x3d {
        return dst._m00(m00)._m01(m01)._m02(m02)._m10(m10)._m11(m11)._m12(m12)._m20(-m20)._m21(-m21)._m22(-m22)._m30(
            m30
        )._m31(m31)._m32(m32)._properties(flags and 16)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m02) && JomlMath.isFinite(
            m10
        ) && JomlMath.isFinite(m11) && JomlMath.isFinite(m12) && JomlMath.isFinite(m20) && JomlMath.isFinite(
            m21
        ) && JomlMath.isFinite(m22) && JomlMath.isFinite(m30) && JomlMath.isFinite(m31) && JomlMath.isFinite(
            m32
        )

    fun distanceSquared(other: Matrix4x3d): Double {
        return lengthSquared(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    fun distanceSquared(other: Matrix4x3f): Double {
        return lengthSquared(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    fun distance(other: Matrix4x3d): Double {
        return sqrt(distanceSquared(other))
    }

    fun distance(other: Matrix4x3f): Double {
        return sqrt(distanceSquared(other))
    }

    fun isIdentity(): Boolean {
        return (flags and 4) != 0
    }
}