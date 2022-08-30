package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix2f {

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

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01)
        arr.put(m10).put(m11)
        return arr
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dst: Matrix2f = this): Matrix2f {
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
    fun mulLocal(left: Matrix2f, dst: Matrix2f = this): Matrix2f {
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

    fun set(m00: Float, m01: Float, m10: Float, m11: Float): Matrix2f {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m: FloatArray) = set(m[0], m[1], m[2], m[3])

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
    fun invert(dst: Matrix2f = this): Matrix2f {
        val s = 1.0f / determinant()
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
    fun transpose(dst: Matrix2f = this) = dst.set(m00, m10, m01, m11)

    override fun toString() =
        "[[${f(m00.toDouble())} ${f(m10.toDouble())}] [${f(m01.toDouble())} ${f(m11.toDouble())}]]".addSigns()

    fun get(dst: Matrix2f): Matrix2f {
        return dst.set(this)
    }

    fun get(dst: Matrix3x2f): Matrix3x2f {
        return dst.set(this)
    }

    fun get(dst: Matrix3f): Matrix3f {
        return dst.set(this)
    }

    val rotation: Float
        get() = atan2(m01, m11)

    fun get(arr: FloatArray, offset: Int = 0): FloatArray {
        arr[offset] = m00
        arr[offset + 1] = m01
        arr[offset + 2] = m10
        arr[offset + 3] = m11
        return arr
    }

    fun zero(): Matrix2f {
        m00 = 0f
        m01 = 0f
        m10 = 0f
        m11 = 0f
        return this
    }

    fun identity(): Matrix2f {
        m00 = 1f
        m01 = 0f
        m10 = 0f
        m11 = 1f
        return this
    }

    fun scale(xy: Vector2f, dst: Matrix2f): Matrix2f {
        return this.scale(xy.x, xy.y, dst)
    }

    fun scale(xy: Vector2f): Matrix2f {
        return this.scale(xy.x, xy.y, this)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, dst: Matrix2f = this): Matrix2f {
        dst.m00 = m00 * x
        dst.m01 = m01 * x
        dst.m10 = m10 * y
        dst.m11 = m11 * y
        return dst
    }

    fun scale(xy: Float, dst: Matrix2f): Matrix2f {
        return this.scale(xy, xy, dst)
    }

    fun scale(xy: Float): Matrix2f {
        return this.scale(xy, xy)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, dst: Matrix2f = this): Matrix2f {
        dst.m00 = x * m00
        dst.m01 = y * m01
        dst.m10 = x * m10
        dst.m11 = y * m11
        return dst
    }

    fun scaling(x: Float, y: Float = x): Matrix2f {
        m00 = x
        m01 = 0f
        m10 = 0f
        m11 = y
        return this
    }

    fun scaling(xy: Vector2f) = scaling(xy.x, xy.y)

    fun rotation(angle: Float): Matrix2f {
        val sin = sin(angle)
        val cos = cos(angle)
        m00 = cos
        m01 = sin
        m10 = -sin
        m11 = cos
        return this
    }

    fun transform(v: Vector2f): Vector2f {
        return v.mul(this)
    }

    fun transform(v: Vector2f, dst: Vector2f): Vector2f {
        v.mul(this, dst)
        return dst
    }

    fun transform(x: Float, y: Float, dst: Vector2f): Vector2f {
        dst.set(m00 * x + m10 * y, m01 * x + m11 * y)
        return dst
    }

    fun transformTranspose(v: Vector2f): Vector2f {
        return v.mulTranspose(this)
    }

    fun transformTranspose(v: Vector2f, dst: Vector2f): Vector2f {
        v.mulTranspose(this, dst)
        return dst
    }

    fun transformTranspose(x: Float, y: Float, dst: Vector2f): Vector2f {
        dst.set(m00 * x + m01 * y, m10 * x + m11 * y)
        return dst
    }

    @JvmOverloads
    fun rotate(angle: Float, dst: Matrix2f = this): Matrix2f {
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
    fun rotateLocal(angle: Float, dst: Matrix2f = this): Matrix2f {
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

    fun getRow(row: Int, dst: Vector2f): Vector2f {
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

    fun setRow(row: Int, src: Vector2f): Matrix2f {
        return this.setRow(row, src.x, src.y)
    }

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

    fun getColumn(column: Int, dst: Vector2f): Vector2f {
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

    fun setColumn(column: Int, src: Vector2f): Matrix2f {
        return this.setColumn(column, src.x, src.y)
    }

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
    fun normal(dst: Matrix2f = this): Matrix2f {
        val det = m00 * m11 - m10 * m01
        val s = 1.0f / det
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

    fun getScale(dst: Vector2f): Vector2f {
        dst.x = sqrt(m00 * m00 + m01 * m01)
        dst.y = sqrt(m10 * m10 + m11 * m11)
        return dst
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
        result = 31 * result + (m00).toBits()
        result = 31 * result + (m01).toBits()
        result = 31 * result + (m10).toBits()
        result = 31 * result + (m11).toBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is Matrix2f) false
        else m00 == other.m00 && m01 == other.m01 && m10 == other.m10 && m11 == other.m11
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

    @JvmOverloads
    fun add(other: Matrix2f, dst: Matrix2f = this): Matrix2f {
        dst.m00 = m00 + other.m00
        dst.m01 = m01 + other.m01
        dst.m10 = m10 + other.m10
        dst.m11 = m11 + other.m11
        return dst
    }

    fun sub(subtrahend: Matrix2f): Matrix2f {
        return this.sub(subtrahend, this)
    }

    fun sub(other: Matrix2f, dst: Matrix2f): Matrix2f {
        dst.m00 = m00 - other.m00
        dst.m01 = m01 - other.m01
        dst.m10 = m10 - other.m10
        dst.m11 = m11 - other.m11
        return dst
    }

    fun mulComponentWise(other: Matrix2f): Matrix2f {
        return this.sub(other, this)
    }

    fun mulComponentWise(other: Matrix2f, dst: Matrix2f): Matrix2f {
        dst.m00 = m00 * other.m00
        dst.m01 = m01 * other.m01
        dst.m10 = m10 * other.m10
        dst.m11 = m11 * other.m11
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix2f, t: Float, dst: Matrix2f = this): Matrix2f {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        return dst
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m10) && JomlMath.isFinite(
            m11
        )
}