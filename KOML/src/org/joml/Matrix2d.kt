package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix2d {

    var m00 = 0.0
    var m01 = 0.0
    var m10 = 0.0
    var m11 = 0.0

    constructor() {
        m00 = 1.0
        m11 = 1.0
    }

    constructor(mat: Matrix2d) {
        setMatrix2d(mat)
    }

    constructor(mat: Matrix2f) {
        m00 = mat.m00.toDouble()
        m01 = mat.m01.toDouble()
        m10 = mat.m10.toDouble()
        m11 = mat.m11.toDouble()
    }

    constructor(mat: Matrix3d) {
        setMatrix3d(mat)
    }

    constructor(mat: Matrix3f) {
        m00 = mat.m00.toDouble()
        m01 = mat.m01.toDouble()
        m10 = mat.m10.toDouble()
        m11 = mat.m11.toDouble()
    }

    constructor(m00: Double, m01: Double, m10: Double, m11: Double) {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
    }

    constructor(col0: Vector2d, col1: Vector2d) {
        m00 = col0.x
        m01 = col0.y
        m10 = col1.x
        m11 = col1.y
    }

    fun m00(m00: Double): Matrix2d {
        this.m00 = m00
        return this
    }

    fun m01(m01: Double): Matrix2d {
        this.m01 = m01
        return this
    }

    fun m10(m10: Double): Matrix2d {
        this.m10 = m10
        return this
    }

    fun m11(m11: Double): Matrix2d {
        this.m11 = m11
        return this
    }

    fun _m00(m00: Double): Matrix2d {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Double): Matrix2d {
        this.m01 = m01
        return this
    }

    fun _m10(m10: Double): Matrix2d {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Double): Matrix2d {
        this.m11 = m11
        return this
    }

    fun set(m: Matrix2d): Matrix2d {
        setMatrix2d(m)
        return this
    }

    private fun setMatrix2d(mat: Matrix2d) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    fun set(m: Matrix2f): Matrix2d {
        m00 = m.m00.toDouble()
        m01 = m.m01.toDouble()
        m10 = m.m10.toDouble()
        m11 = m.m11.toDouble()
        return this
    }

    fun set(m: Matrix3x2d): Matrix2d {
        m00 = m.m00
        m01 = m.m01
        m10 = m.m10
        m11 = m.m11
        return this
    }

    fun set(m: Matrix3x2f): Matrix2d {
        m00 = m.m00.toDouble()
        m01 = m.m01.toDouble()
        m10 = m.m10.toDouble()
        m11 = m.m11.toDouble()
        return this
    }

    fun set(m: Matrix3d): Matrix2d {
        setMatrix3d(m)
        return this
    }

    private fun setMatrix3d(mat: Matrix3d) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    fun set(m: Matrix3f): Matrix2d {
        m00 = m.m00.toDouble()
        m01 = m.m01.toDouble()
        m10 = m.m10.toDouble()
        m11 = m.m11.toDouble()
        return this
    }

    @JvmOverloads
    fun mul(right: Matrix2d, dst: Matrix2d = this): Matrix2d {
        val nm00 = m00 * right.m00 + m10 * right.m01
        val nm01 = m01 * right.m00 + m11 * right.m01
        val nm10 = m00 * right.m10 + m10 * right.m11
        val nm11 = m01 * right.m10 + m11 * right.m11
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dst: Matrix2d = this): Matrix2d {
        val nm00 = m00 * right.m00.toDouble() + m10 * right.m01.toDouble()
        val nm01 = m01 * right.m00.toDouble() + m11 * right.m01.toDouble()
        val nm10 = m00 * right.m10.toDouble() + m10 * right.m11.toDouble()
        val nm11 = m01 * right.m10.toDouble() + m11 * right.m11.toDouble()
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    @JvmOverloads
    fun mulLocal(left: Matrix2d, dst: Matrix2d = this): Matrix2d {
        val nm00 = left.m00 * m00 + left.m10 * m01
        val nm01 = left.m01 * m00 + left.m11 * m01
        val nm10 = left.m00 * m10 + left.m10 * m11
        val nm11 = left.m01 * m10 + left.m11 * m11
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    fun set(m00: Double, m01: Double, m10: Double, m11: Double): Matrix2d {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m: DoubleArray) = set(m[0], m[1], m[2], m[3])

    fun set(col0: Vector2d, col1: Vector2d): Matrix2d {
        m00 = col0.x
        m01 = col0.y
        m10 = col1.x
        m11 = col1.y
        return this
    }

    fun determinant(): Double {
        return m00 * m11 - m10 * m01
    }

    @JvmOverloads
    fun invert(dst: Matrix2d = this): Matrix2d {
        val s = 1.0 / determinant()
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    @JvmOverloads
    fun transpose(dst: Matrix2d = this) = dst.set(m00, m10, m01, m11)

    override fun toString() = "[[${f(m00)} ${f(m10)}] [${f(m01)} ${f(m11)}]]".addSigns()

    fun get(dst: Matrix2d): Matrix2d {
        return dst.set(this)
    }

    fun get(dst: Matrix3x2d): Matrix3x2d {
        return dst.set(this)
    }

    fun get(dst: Matrix3d): Matrix3d {
        return dst.set(this)
    }

    val rotation: Double
        get() = atan2(m01, m11)

    @JvmOverloads
    fun get(arr: DoubleArray, offset: Int = 0): DoubleArray {
        arr[offset] = m00
        arr[offset + 1] = m01
        arr[offset + 2] = m10
        arr[offset + 3] = m11
        return arr
    }

    fun zero() = set(0.0, 0.0, 0.0, 0.0)

    fun identity(): Matrix2d {
        m00 = 1.0
        m01 = 0.0
        m10 = 0.0
        m11 = 1.0
        return this
    }

    fun scale(xy: Vector2d, dst: Matrix2d): Matrix2d {
        return this.scale(xy.x, xy.y, dst)
    }

    fun scale(xy: Vector2d): Matrix2d {
        return this.scale(xy.x, xy.y, this)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, dst: Matrix2d = this): Matrix2d {
        dst.m00 = m00 * x
        dst.m01 = m01 * x
        dst.m10 = m10 * y
        dst.m11 = m11 * y
        return dst
    }

    fun scale(xy: Double, dst: Matrix2d): Matrix2d {
        return this.scale(xy, xy, dst)
    }

    fun scale(xy: Double): Matrix2d {
        return this.scale(xy, xy)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, dst: Matrix2d = this): Matrix2d {
        dst.m00 = x * m00
        dst.m01 = y * m01
        dst.m10 = x * m10
        dst.m11 = y * m11
        return dst
    }

    fun scaling(factor: Double): Matrix2d {
        m00 = factor
        m01 = 0.0
        m10 = 0.0
        m11 = factor
        return this
    }

    fun scaling(x: Double, y: Double): Matrix2d {
        m00 = x
        m01 = 0.0
        m10 = 0.0
        m11 = y
        return this
    }

    fun scaling(xy: Vector2d): Matrix2d {
        return this.scaling(xy.x, xy.y)
    }

    fun rotation(angle: Double): Matrix2d {
        val sin = sin(angle)
        val cos = cos(angle)
        m00 = cos
        m01 = sin
        m10 = -sin
        m11 = cos
        return this
    }

    fun transform(v: Vector2d): Vector2d {
        return v.mul(this)
    }

    fun transform(v: Vector2d, dst: Vector2d): Vector2d {
        v.mul(this, dst)
        return dst
    }

    fun transform(x: Double, y: Double, dst: Vector2d): Vector2d {
        dst.set(m00 * x + m10 * y, m01 * x + m11 * y)
        return dst
    }

    fun transformTranspose(v: Vector2d): Vector2d {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector2d, dst: Vector2d): Vector2d {
        v.mulTranspose(this, dst)
        return dst
    }

    fun transformTranspose(x: Double, y: Double, dst: Vector2d): Vector2d {
        dst.set(m00 * x + m01 * y, m10 * x + m11 * y)
        return dst
    }

    @JvmOverloads
    fun rotate(angle: Double, dst: Matrix2d = this): Matrix2d {
        val s = sin(angle)
        val c = cos(angle)
        val nm00 = m00 * c + m10 * s
        val nm01 = m01 * c + m11 * s
        val nm10 = m10 * c - m00 * s
        val nm11 = m11 * c - m01 * s
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    @JvmOverloads
    fun rotateLocal(angle: Double, dst: Matrix2d = this): Matrix2d {
        val s = sin(angle)
        val c = cos(angle)
        val nm00 = c * m00 - s * m01
        val nm01 = s * m00 + c * m01
        val nm10 = c * m10 - s * m11
        val nm11 = s * m10 + c * m11
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    fun getRow(row: Int, dst: Vector2d): Vector2d {
        when (row) {
            0 -> {
                dst.x = m00
                dst.y = m10
            }
            1 -> {
                dst.x = m01
                dst.y = m11
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dst
    }

    fun setRow(row: Int, src: Vector2d): Matrix2d {
        return this.setRow(row, src.x, src.y)
    }

    fun setRow(row: Int, x: Double, y: Double): Matrix2d {
        when (row) {
            0 -> {
                m00 = x
                m10 = y
            }
            1 -> {
                m01 = x
                m11 = y
            }
            else -> throw IndexOutOfBoundsException()
        }
        return this
    }

    fun getColumn(column: Int, dst: Vector2d): Vector2d {
        when (column) {
            0 -> {
                dst.x = m00
                dst.y = m01
            }
            1 -> {
                dst.x = m10
                dst.y = m11
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dst
    }

    fun setColumn(column: Int, src: Vector2d): Matrix2d {
        return this.setColumn(column, src.x, src.y)
    }

    fun setColumn(column: Int, x: Double, y: Double): Matrix2d {
        when (column) {
            0 -> {
                m00 = x
                m01 = y
            }
            1 -> {
                m10 = x
                m11 = y
            }
            else -> throw IndexOutOfBoundsException()
        }
        return this
    }

    operator fun get(column: Int, row: Int): Double {
        when (column) {
            0 -> return when (row) {
                0 -> m00
                1 -> m01
                else -> throw IndexOutOfBoundsException()
            }
            1 -> when (row) {
                0 -> return m10
                1 -> return m11
            }
        }
        throw IndexOutOfBoundsException()
    }

    operator fun set(column: Int, row: Int, value: Double): Matrix2d {
        when (column) {
            0 -> return when (row) {
                0 -> {
                    m00 = value
                    this
                }
                1 -> {
                    m01 = value
                    this
                }
                else -> throw IndexOutOfBoundsException()
            }
            1 -> when (row) {
                0 -> {
                    m10 = value
                    return this
                }
                1 -> {
                    m11 = value
                    return this
                }
            }
        }
        throw IndexOutOfBoundsException()
    }

    @JvmOverloads
    fun normal(dst: Matrix2d = this): Matrix2d {
        val det = m00 * m11 - m10 * m01
        val s = 1.0 / det
        val nm00 = m11 * s
        val nm01 = -m10 * s
        val nm10 = -m01 * s
        val nm11 = m00 * s
        dst.m00 = nm00
        dst.m01 = nm01
        dst.m10 = nm10
        dst.m11 = nm11
        return dst
    }

    fun getScale(dst: Vector2d): Vector2d {
        dst.x = sqrt(m00 * m00 + m01 * m01)
        dst.y = sqrt(m10 * m10 + m11 * m11)
        return dst
    }

    fun positiveX(dir: Vector2d): Vector2d {
        if (m00 * m11 < m01 * m10) {
            dir.x = -m11
            dir.y = m01
        } else {
            dir.x = m11
            dir.y = -m01
        }
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector2d): Vector2d {
        if (m00 * m11 < m01 * m10) {
            dir.x = -m11
            dir.y = m01
        } else {
            dir.x = m11
            dir.y = -m01
        }
        return dir
    }

    fun positiveY(dir: Vector2d): Vector2d {
        if (m00 * m11 < m01 * m10) {
            dir.x = m10
            dir.y = -m00
        } else {
            dir.x = -m10
            dir.y = m00
        }
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector2d): Vector2d {
        if (m00 * m11 < m01 * m10) {
            dir.x = m10
            dir.y = -m00
        } else {
            dir.x = -m10
            dir.y = m00
        }
        return dir
    }

    override fun hashCode(): Int {
        var result = 1
        var temp = (m00).toBits()
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = (m01).toBits()
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = (m10).toBits()
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = (m11).toBits()
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is Matrix2d) false
        else m00 == other.m00 && m01 == other.m01 && m10 == other.m10 && m11 == other.m11
    }

    fun equals(m: Matrix2d?, delta: Double): Boolean {
        return if (this === m) {
            true
        } else if (m == null) {
            false
        } else if (!Runtime.equals(m00, m.m00, delta)) {
            false
        } else if (!Runtime.equals(m01, m.m01, delta)) {
            false
        } else if (!Runtime.equals(m10, m.m10, delta)) {
            false
        } else {
            Runtime.equals(m11, m.m11, delta)
        }
    }

    /*fun swap(other: Matrix2d?): Matrix2d {
        MemUtil.INSTANCE.swap(this, other)
        return this
    }*/

    @JvmOverloads
    fun add(other: Matrix2d, dst: Matrix2d = this): Matrix2d {
        dst.m00 = m00 + other.m00
        dst.m01 = m01 + other.m01
        dst.m10 = m10 + other.m10
        dst.m11 = m11 + other.m11
        return dst
    }

    fun sub(subtrahend: Matrix2d): Matrix2d {
        return this.sub(subtrahend, this)
    }

    fun sub(other: Matrix2d, dst: Matrix2d): Matrix2d {
        dst.m00 = m00 - other.m00
        dst.m01 = m01 - other.m01
        dst.m10 = m10 - other.m10
        dst.m11 = m11 - other.m11
        return dst
    }

    fun mulComponentWise(other: Matrix2d): Matrix2d {
        return this.sub(other, this)
    }

    fun mulComponentWise(other: Matrix2d, dst: Matrix2d): Matrix2d {
        dst.m00 = m00 * other.m00
        dst.m01 = m01 * other.m01
        dst.m10 = m10 * other.m10
        dst.m11 = m11 * other.m11
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix2d, t: Double, dst: Matrix2d = this): Matrix2d {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        return dst
    }

    val isFinite: Boolean
        get() = m00.isFinite() && m01.isFinite() && m10.isFinite() && m11.isFinite()
}