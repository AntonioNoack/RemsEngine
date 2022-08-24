package org.joml

import java.text.NumberFormat

class Matrix2f {
    var m00 = 0f
    var m01 = 0f
    var m10 = 0f
    var m11 = 0f

    constructor() {
        m00 = 1.0f
        m11 = 1.0f
    }

    constructor(mat: Matrix2f) {
        setMatrix2f(mat)
    }

    constructor(mat: Matrix3f) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    constructor(m00: Float, m01: Float, m10: Float, m11: Float) {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
    }

    constructor(col0: Vector2f, col1: Vector2f) {
        m00 = col0.x
        m01 = col0.y
        m10 = col1.x
        m11 = col1.y
    }

    fun m00(m00: Float): Matrix2f {
        this.m00 = m00
        return this
    }

    fun m01(m01: Float): Matrix2f {
        this.m01 = m01
        return this
    }

    fun m10(m10: Float): Matrix2f {
        this.m10 = m10
        return this
    }

    fun m11(m11: Float): Matrix2f {
        this.m11 = m11
        return this
    }

    fun _m00(m00: Float): Matrix2f {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Float): Matrix2f {
        this.m01 = m01
        return this
    }

    fun _m10(m10: Float): Matrix2f {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Float): Matrix2f {
        this.m11 = m11
        return this
    }

    fun set(m: Matrix2f): Matrix2f {
        setMatrix2f(m)
        return this
    }

    private fun setMatrix2f(mat: Matrix2f) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    fun set(m: Matrix3x2f): Matrix2f {
        m00 = m.m00
        m01 = m.m01
        m10 = m.m10
        m11 = m.m11
        return this
    }

    fun set(m: Matrix3f): Matrix2f {
        m00 = m.m00
        m01 = m.m01
        m10 = m.m10
        m11 = m.m11
        return this
    }

    private fun setMatrix3f(mat: Matrix3f) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dest: Matrix2f = this): Matrix2f {
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
    fun mulLocal(left: Matrix2f, dest: Matrix2f = this): Matrix2f {
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

    operator fun set(m00: Float, m01: Float, m10: Float, m11: Float): Matrix2f {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m: FloatArray?): Matrix2f {
        MemUtil.INSTANCE.copy(m, 0, this)
        return this
    }

    operator fun set(col0: Vector2f, col1: Vector2f): Matrix2f {
        m00 = col0.x
        m01 = col0.y
        m10 = col1.x
        m11 = col1.y
        return this
    }

    fun determinant(): Float {
        return m00 * m11 - m10 * m01
    }

    @JvmOverloads
    fun invert(dest: Matrix2f = this): Matrix2f {
        val s = 1.0f / determinant()
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
    fun transpose(dest: Matrix2f = this): Matrix2f {
        dest[m00, m10, m01] = m11
        return dest
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
        return """${Runtime.format(m00.toDouble(), formatter)} ${Runtime.format(m10.toDouble(), formatter)}
${Runtime.format(m01.toDouble(), formatter)} ${Runtime.format(m11.toDouble(), formatter)}
"""
    }

    operator fun get(dest: Matrix2f): Matrix2f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix3x2f): Matrix3x2f {
        return dest.set(this)
    }

    operator fun get(dest: Matrix3f): Matrix3f {
        return dest.set(this)
    }

    val rotation: Float
        get() = Math.atan2(m01, m11)

    @JvmOverloads
    operator fun get(arr: FloatArray, offset: Int = 0): FloatArray {
        MemUtil.INSTANCE.copy(this, arr, offset)
        return arr
    }

    fun zero(): Matrix2f {
        MemUtil.INSTANCE.zero(this)
        return this
    }

    fun identity(): Matrix2f {
        MemUtil.INSTANCE.identity(this)
        return this
    }

    fun scale(xy: Vector2f, dest: Matrix2f): Matrix2f {
        return this.scale(xy.x, xy.y, dest)
    }

    fun scale(xy: Vector2f): Matrix2f {
        return this.scale(xy.x, xy.y, this)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, dest: Matrix2f = this): Matrix2f {
        dest.m00 = m00 * x
        dest.m01 = m01 * x
        dest.m10 = m10 * y
        dest.m11 = m11 * y
        return dest
    }

    fun scale(xy: Float, dest: Matrix2f): Matrix2f {
        return this.scale(xy, xy, dest)
    }

    fun scale(xy: Float): Matrix2f {
        return this.scale(xy, xy)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, dest: Matrix2f = this): Matrix2f {
        dest.m00 = x * m00
        dest.m01 = y * m01
        dest.m10 = x * m10
        dest.m11 = y * m11
        return dest
    }

    fun scaling(factor: Float): Matrix2f {
        MemUtil.INSTANCE.zero(this)
        m00 = factor
        m11 = factor
        return this
    }

    fun scaling(x: Float, y: Float): Matrix2f {
        MemUtil.INSTANCE.zero(this)
        m00 = x
        m11 = y
        return this
    }

    fun scaling(xy: Vector2f): Matrix2f {
        return this.scaling(xy.x, xy.y)
    }

    fun rotation(angle: Float): Matrix2f {
        val sin = Math.sin(angle)
        val cos = Math.cosFromSin(sin, angle)
        m00 = cos
        m01 = sin
        m10 = -sin
        m11 = cos
        return this
    }

    fun transform(v: Vector2f): Vector2f {
        return v.mul(this)
    }

    fun transform(v: Vector2f, dest: Vector2f): Vector2f {
        v.mul(this, dest)
        return dest
    }

    fun transform(x: Float, y: Float, dest: Vector2f): Vector2f {
        dest.set(m00 * x + m10 * y, m01 * x + m11 * y)
        return dest
    }

    fun transformTranspose(v: Vector2f): Vector2f {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector2f, dest: Vector2f): Vector2f {
        v.mulTranspose(this, dest)
        return dest
    }

    fun transformTranspose(x: Float, y: Float, dest: Vector2f): Vector2f {
        dest.set(m00 * x + m01 * y, m10 * x + m11 * y)
        return dest
    }

    @JvmOverloads
    fun rotate(angle: Float, dest: Matrix2f = this): Matrix2f {
        val s = Math.sin(angle)
        val c = Math.cosFromSin(s, angle)
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
    fun rotateLocal(angle: Float, dest: Matrix2f = this): Matrix2f {
        val s = Math.sin(angle)
        val c = Math.cosFromSin(s, angle)
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
    fun getRow(row: Int, dest: Vector2f): Vector2f {
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
    fun setRow(row: Int, src: Vector2f): Matrix2f {
        return this.setRow(row, src.x, src.y)
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setRow(row: Int, x: Float, y: Float): Matrix2f {
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
    fun getColumn(column: Int, dest: Vector2f): Vector2f {
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
    fun setColumn(column: Int, src: Vector2f): Matrix2f {
        return this.setColumn(column, src.x, src.y)
    }

    @Throws(IndexOutOfBoundsException::class)
    fun setColumn(column: Int, x: Float, y: Float): Matrix2f {
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

    operator fun get(column: Int, row: Int): Float {
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

    operator fun set(column: Int, row: Int, value: Float): Matrix2f {
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
    fun normal(dest: Matrix2f = this): Matrix2f {
        val det = m00 * m11 - m10 * m01
        val s = 1.0f / det
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

    fun getScale(dest: Vector2f): Vector2f {
        dest.x = Math.sqrt(m00 * m00 + m01 * m01)
        dest.y = Math.sqrt(m10 * m10 + m11 * m11)
        return dest
    }

    fun positiveX(dir: Vector2f): Vector2f {
        if (m00 * m11 < m01 * m10) {
            dir.x = -m11
            dir.y = m01
        } else {
            dir.x = m11
            dir.y = -m01
        }
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector2f): Vector2f {
        if (m00 * m11 < m01 * m10) {
            dir.x = -m11
            dir.y = m01
        } else {
            dir.x = m11
            dir.y = -m01
        }
        return dir
    }

    fun positiveY(dir: Vector2f): Vector2f {
        if (m00 * m11 < m01 * m10) {
            dir.x = m10
            dir.y = -m00
        } else {
            dir.x = -m10
            dir.y = m00
        }
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector2f): Vector2f {
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
        result = 31 * result + java.lang.Float.floatToIntBits(m00)
        result = 31 * result + java.lang.Float.floatToIntBits(m01)
        result = 31 * result + java.lang.Float.floatToIntBits(m10)
        result = 31 * result + java.lang.Float.floatToIntBits(m11)
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
            val other = obj as Matrix2f
            if (java.lang.Float.floatToIntBits(m00) != java.lang.Float.floatToIntBits(other.m00)) {
                false
            } else if (java.lang.Float.floatToIntBits(m01) != java.lang.Float.floatToIntBits(other.m01)) {
                false
            } else if (java.lang.Float.floatToIntBits(m10) != java.lang.Float.floatToIntBits(other.m10)) {
                false
            } else {
                java.lang.Float.floatToIntBits(m11) == java.lang.Float.floatToIntBits(other.m11)
            }
        }
    }

    fun equals(m: Matrix2f?, delta: Float): Boolean {
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

    fun swap(other: Matrix2f?): Matrix2f {
        MemUtil.INSTANCE.swap(this, other)
        return this
    }

    @JvmOverloads
    fun add(other: Matrix2f, dest: Matrix2f = this): Matrix2f {
        dest.m00 = m00 + other.m00
        dest.m01 = m01 + other.m01
        dest.m10 = m10 + other.m10
        dest.m11 = m11 + other.m11
        return dest
    }

    fun sub(subtrahend: Matrix2f): Matrix2f {
        return this.sub(subtrahend, this)
    }

    fun sub(other: Matrix2f, dest: Matrix2f): Matrix2f {
        dest.m00 = m00 - other.m00
        dest.m01 = m01 - other.m01
        dest.m10 = m10 - other.m10
        dest.m11 = m11 - other.m11
        return dest
    }

    fun mulComponentWise(other: Matrix2f): Matrix2f {
        return this.sub(other, this)
    }

    fun mulComponentWise(other: Matrix2f, dest: Matrix2f): Matrix2f {
        dest.m00 = m00 * other.m00
        dest.m01 = m01 * other.m01
        dest.m10 = m10 * other.m10
        dest.m11 = m11 * other.m11
        return dest
    }

    @JvmOverloads
    fun lerp(other: Matrix2f, t: Float, dest: Matrix2f = this): Matrix2f {
        dest.m00 = (other.m00 - m00) * t + m00
        dest.m01 = (other.m01 - m01) * t + m01
        dest.m10 = (other.m10 - m10) * t + m10
        dest.m11 = (other.m11 - m11) * t + m11
        return dest
    }

    val isFinite: Boolean
        get() = Math.isFinite(m00) && Math.isFinite(m01) && Math.isFinite(m10) && Math.isFinite(
            m11
        )
}