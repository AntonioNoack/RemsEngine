package org.joml

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Matrix2d {
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
    fun mul(right: Matrix2d, dest: Matrix2d = this): Matrix2d {
        val nm00 = m00 * right.m00 + m10 * right.m01
        val nm01 = m01 * right.m00 + m11 * right.m01
        val nm10 = m00 * right.m10 + m10 * right.m11
        val nm11 = m01 * right.m10 + m11 * right.m11
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dest: Matrix2d = this): Matrix2d {
        val nm00 = m00 * right.m00.toDouble() + m10 * right.m01.toDouble()
        val nm01 = m01 * right.m00.toDouble() + m11 * right.m01.toDouble()
        val nm10 = m00 * right.m10.toDouble() + m10 * right.m11.toDouble()
        val nm11 = m01 * right.m10.toDouble() + m11 * right.m11.toDouble()
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    @JvmOverloads
    fun mulLocal(left: Matrix2d, dest: Matrix2d = this): Matrix2d {
        val nm00 = left.m00 * m00 + left.m10 * m01
        val nm01 = left.m01 * m00 + left.m11 * m01
        val nm10 = left.m00 * m10 + left.m10 * m11
        val nm11 = left.m01 * m10 + left.m11 * m11
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    operator fun set(m00: Double, m01: Double, m10: Double, m11: Double): Matrix2d {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m: DoubleArray) = set(m[0], m[1], m[2], m[3])

    operator fun set(col0: Vector2d, col1: Vector2d): Matrix2d {
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
    fun invert(dest: Matrix2d = this): Matrix2d {
        val s = 1.0 / determinant()
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    @JvmOverloads
    fun transpose(dest: Matrix2d = this): Matrix2d {
        dest[m00, m10, m01] = m11
        return dest
    }

    override fun toString(): String {
        val str = this.toString(Options.NUMBER_FORMAT)
        val res = StringBuilder()
        var eIndex = Int.MIN_VALUE
        for (i in str.indices) {
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

    fun toString(formatter: Int): String {
        return """${Runtime.format(m00, formatter)} ${Runtime.format(m10, formatter)}
${Runtime.format(m01, formatter)} ${Runtime.format(m11, formatter)}
"""
    }

    fun get(dest: Matrix2d): Matrix2d {
        return dest.set(this)
    }

    fun get(dest: Matrix3x2d): Matrix3x2d {
        return dest.set(this)
    }

    fun get(dest: Matrix3d): Matrix3d {
        return dest.set(this)
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

    fun scale(xy: Vector2d, dest: Matrix2d): Matrix2d {
        return this.scale(xy.x, xy.y, dest)
    }

    fun scale(xy: Vector2d): Matrix2d {
        return this.scale(xy.x, xy.y, this)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, dest: Matrix2d = this): Matrix2d {
        dest.m00 = m00 * x
        dest.m01 = m01 * x
        dest.m10 = m10 * y
        dest.m11 = m11 * y
        return dest
    }

    fun scale(xy: Double, dest: Matrix2d): Matrix2d {
        return this.scale(xy, xy, dest)
    }

    fun scale(xy: Double): Matrix2d {
        return this.scale(xy, xy)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, dest: Matrix2d = this): Matrix2d {
        dest.m00 = x * m00
        dest.m01 = y * m01
        dest.m10 = x * m10
        dest.m11 = y * m11
        return dest
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

    fun transform(v: Vector2d, dest: Vector2d): Vector2d {
        v.mul(this, dest)
        return dest
    }

    fun transform(x: Double, y: Double, dest: Vector2d): Vector2d {
        dest.set(m00 * x + m10 * y, m01 * x + m11 * y)
        return dest
    }

    fun transformTranspose(v: Vector2d): Vector2d {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector2d, dest: Vector2d): Vector2d {
        v.mulTranspose(this, dest)
        return dest
    }

    fun transformTranspose(x: Double, y: Double, dest: Vector2d): Vector2d {
        dest.set(m00 * x + m01 * y, m10 * x + m11 * y)
        return dest
    }

    @JvmOverloads
    fun rotate(angle: Double, dest: Matrix2d = this): Matrix2d {
        val s = sin(angle)
        val c = cos(angle)
        val nm00 = m00 * c + m10 * s
        val nm01 = m01 * c + m11 * s
        val nm10 = m10 * c - m00 * s
        val nm11 = m11 * c - m01 * s
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    @JvmOverloads
    fun rotateLocal(angle: Double, dest: Matrix2d = this): Matrix2d {
        val s = sin(angle)
        val c = cos(angle)
        val nm00 = c * m00 - s * m01
        val nm01 = s * m00 + c * m01
        val nm10 = c * m10 - s * m11
        val nm11 = s * m10 + c * m11
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    @Throws(IndexOutOfBoundsException::class)
    fun getRow(row: Int, dest: Vector2d): Vector2d {
        when (row) {
            0 -> {
                dest.x = m00
                dest.y = m10
            }
            1 -> {
                dest.x = m01
                dest.y = m11
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setRow(row: Int, src: Vector2d): Matrix2d {
        return this.setRow(row, src.x, src.y)
    }

    @Throws(IndexOutOfBoundsException::class)
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

    @Throws(IndexOutOfBoundsException::class)
    fun getColumn(column: Int, dest: Vector2d): Vector2d {
        when (column) {
            0 -> {
                dest.x = m00
                dest.y = m01
            }
            1 -> {
                dest.x = m10
                dest.y = m11
            }
            else -> throw IndexOutOfBoundsException()
        }
        return dest
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setColumn(column: Int, src: Vector2d): Matrix2d {
        return this.setColumn(column, src.x, src.y)
    }

    @Throws(IndexOutOfBoundsException::class)
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
    fun normal(dest: Matrix2d = this): Matrix2d {
        val det = m00 * m11 - m10 * m01
        val s = 1.0 / det
        val nm00 = m11 * s
        val nm01 = -m10 * s
        val nm10 = -m01 * s
        val nm11 = m00 * s
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        return dest
    }

    fun getScale(dest: Vector2d): Vector2d {
        dest.x = sqrt(m00 * m00 + m01 * m01)
        dest.y = sqrt(m10 * m10 + m11 * m11)
        return dest
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
        var temp = java.lang.Double.doubleToLongBits(m00)
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = java.lang.Double.doubleToLongBits(m01)
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = java.lang.Double.doubleToLongBits(m10)
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        temp = java.lang.Double.doubleToLongBits(m11)
        result = 31 * result + (temp ushr 32 xor temp).toInt()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null) {
            false
        } else if (this.javaClass != other.javaClass) {
            false
        } else {
            other as Matrix2d
            if (java.lang.Double.doubleToLongBits(m00) != java.lang.Double.doubleToLongBits(other.m00)) {
                false
            } else if (java.lang.Double.doubleToLongBits(m01) != java.lang.Double.doubleToLongBits(other.m01)) {
                false
            } else if (java.lang.Double.doubleToLongBits(m10) != java.lang.Double.doubleToLongBits(other.m10)) {
                false
            } else {
                java.lang.Double.doubleToLongBits(m11) == java.lang.Double.doubleToLongBits(other.m11)
            }
        }
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
    fun add(other: Matrix2d, dest: Matrix2d = this): Matrix2d {
        dest.m00 = m00 + other.m00
        dest.m01 = m01 + other.m01
        dest.m10 = m10 + other.m10
        dest.m11 = m11 + other.m11
        return dest
    }

    fun sub(subtrahend: Matrix2d): Matrix2d {
        return this.sub(subtrahend, this)
    }

    fun sub(other: Matrix2d, dest: Matrix2d): Matrix2d {
        dest.m00 = m00 - other.m00
        dest.m01 = m01 - other.m01
        dest.m10 = m10 - other.m10
        dest.m11 = m11 - other.m11
        return dest
    }

    fun mulComponentWise(other: Matrix2d): Matrix2d {
        return this.sub(other, this)
    }

    fun mulComponentWise(other: Matrix2d, dest: Matrix2d): Matrix2d {
        dest.m00 = m00 * other.m00
        dest.m01 = m01 * other.m01
        dest.m10 = m10 * other.m10
        dest.m11 = m11 * other.m11
        return dest
    }

    @JvmOverloads
    fun lerp(other: Matrix2d, t: Double, dest: Matrix2d = this): Matrix2d {
        dest.m00 = JomlMath.fma(other.m00 - m00, t, m00)
        dest.m01 = JomlMath.fma(other.m01 - m01, t, m01)
        dest.m10 = JomlMath.fma(other.m10 - m10, t, m10)
        dest.m11 = JomlMath.fma(other.m11 - m11, t, m11)
        return dest
    }

    val isFinite: Boolean
        get() = m00.isFinite() && m01.isFinite() && m10.isFinite() && m11.isFinite()
}