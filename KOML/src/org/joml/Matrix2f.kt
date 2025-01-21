package org.joml

import org.joml.JomlMath.addSigns
import org.joml.Runtime.f
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix2f : Matrix<Matrix2f, Vector2f, Vector2f> {

    var m00 = 0f
    var m01 = 0f
    var m10 = 0f
    var m11 = 0f

    constructor() {
        m00 = 1.0f
        m11 = 1.0f
    }

    constructor(mat: Matrix2f) {
        set(mat)
    }

    constructor(mat: Matrix3f) {
        set(mat)
    }

    constructor(m00: Float, m01: Float, m10: Float, m11: Float) {
        set(m00, m01, m10, m11)
    }

    constructor(col0: Vector2f, col1: Vector2f) {
        set(col0, col1)
    }

    override val numRows: Int get() = 2
    override val numCols: Int get() = 2

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
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3x2f): Matrix2f {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3f): Matrix2f {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01)
        arr.put(m10).put(m11)
        return arr
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            m00 * right.m00 + m10 * right.m01,
            m01 * right.m00 + m11 * right.m01,
            m00 * right.m10 + m10 * right.m11,
            m01 * right.m10 + m11 * right.m11
        )
    }

    @JvmOverloads
    fun mulLocal(left: Matrix2f, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            left.m00 * m00 + left.m10 * m01,
            left.m01 * m00 + left.m11 * m01,
            left.m00 * m10 + left.m10 * m11,
            left.m01 * m10 + left.m11 * m11
        )
    }

    fun set(m00: Float, m01: Float, m10: Float, m11: Float): Matrix2f {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m: FloatArray): Matrix2f {
        return set(m[0], m[1], m[2], m[3])
    }

    fun set(col0: Vector2f, col1: Vector2f): Matrix2f {
        return set(col0.x, col0.y, col1.x, col1.y)
    }

    fun determinant(): Float {
        return m00 * m11 - m10 * m01
    }

    @JvmOverloads
    fun invert(dst: Matrix2f = this): Matrix2f {
        val s = 1.0f / determinant()
        return dst.set(m11 * s, -m01 * s, -m10 * s, m00 * s)
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

    fun zero(): Matrix2f = set(0f, 0f, 0f, 0f)
    fun identity(): Matrix2f = set(1f, 0f, 0f, 1f)

    @JvmOverloads
    fun scale(xy: Vector2f, dst: Matrix2f = this): Matrix2f {
        return scale(xy.x, xy.y, dst)
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, dst: Matrix2f = this): Matrix2f {
        return dst.set(m00 * x, m01 * x, m10 * y, m11 * y)
    }

    @JvmOverloads
    fun scale(xy: Float, dst: Matrix2f = this): Matrix2f {
        return this.scale(xy, xy, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, dst: Matrix2f = this): Matrix2f {
        return dst.set(x * m00, y * m01, x * m10, y * m11)
    }

    fun scaling(x: Float, y: Float = x): Matrix2f {
        return set(x, 0f, 0f, y)
    }

    fun scaling(xy: Vector2f) = scaling(xy.x, xy.y)

    fun rotation(angle: Float): Matrix2f {
        val sin = sin(angle)
        val cos = cos(angle)
        return set(cos, sin, -sin, cos)
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
        return dst.set(
            m00 * c + m10 * s, m01 * c + m11 * s,
            m10 * c - m00 * s, m11 * c - m01 * s
        )
    }

    @JvmOverloads
    fun rotateLocal(angle: Float, dst: Matrix2f = this): Matrix2f {
        val s = sin(angle)
        val c = cos(angle)
        return dst.set(
            c * m00 - s * m01, s * m00 + c * m01,
            c * m10 - s * m11, s * m10 + c * m11
        )
    }

    override fun getRow(row: Int, dst: Vector2f): Vector2f {
        when (row) {
            0 -> dst.set(m00, m10)
            else -> dst.set(m01, m11)
        }
        return dst
    }

    override fun setRow(row: Int, src: Vector2f): Matrix2f {
        return setRow(row, src.x, src.y)
    }

    fun setRow(row: Int, x: Float, y: Float): Matrix2f {
        when (row) {
            0 -> {
                m00 = x
                m10 = y
            }
            else -> {
                m01 = x
                m11 = y
            }
        }
        return this
    }

    override fun getColumn(column: Int, dst: Vector2f): Vector2f {
        when (column) {
            0 -> dst.set(m00, m01)
            else -> dst.set(m10, m11)
        }
        return dst
    }

    override fun setColumn(column: Int, src: Vector2f): Matrix2f {
        return setColumn(column, src.x, src.y)
    }

    fun setColumn(column: Int, x: Float, y: Float): Matrix2f {
        when (column) {
            0 -> {
                m00 = x
                m01 = y
            }
            else -> {
                m10 = x
                m11 = y
            }
        }
        return this
    }

    override operator fun get(column: Int, row: Int): Double {
        return when (column * 2 + row) {
            0 -> m00
            1 -> m01
            2 -> m10
            else -> m11
        }.toDouble()
    }

    override operator fun set(column: Int, row: Int, value: Double): Matrix2f {
        return set(column, row, value.toFloat())
    }

    operator fun set(column: Int, row: Int, value: Float): Matrix2f {
        when (column * 2 + row) {
            0 -> m00 = value
            1 -> m01 = value
            2 -> m10 = value
            else -> m11 = value
        }
        return this
    }

    @JvmOverloads
    fun normal(dst: Matrix2f = this): Matrix2f {
        val det = m00 * m11 - m10 * m01
        val s = 1.0f / det
        return dst.set(
            m11 * s, -m10 * s,
            -m01 * s, m00 * s
        )
    }

    fun getScale(dst: Vector2f): Vector2f {
        return dst.set(
            sqrt(m00 * m00 + m01 * m01),
            sqrt(m10 * m10 + m11 * m11)
        )
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
        result = 31 * result + m00.toRawBits()
        result = 31 * result + m01.toRawBits()
        result = 31 * result + m10.toRawBits()
        result = 31 * result + m11.toRawBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Matrix2f && m00 == other.m00 && m01 == other.m01 && m10 == other.m10 && m11 == other.m11
    }

    override fun equals(other: Matrix2f?, threshold: Double): Boolean {
        return equals(other, threshold.toFloat())
    }

    fun equals(m: Matrix2f?, delta: Float): Boolean {
        return m != null &&
                Runtime.equals(m00, m.m00, delta) && Runtime.equals(m01, m.m01, delta) &&
                Runtime.equals(m10, m.m10, delta) && Runtime.equals(m11, m.m11, delta)
    }

    @JvmOverloads
    fun add(other: Matrix2f, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            m00 + other.m00, m01 + other.m01,
            m10 + other.m10, m11 + other.m11
        )
    }

    @JvmOverloads
    fun sub(other: Matrix2f, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            m00 - other.m00, m01 - other.m01,
            m10 - other.m10, m11 - other.m11
        )
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix2f, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            m00 * other.m00, m01 * other.m01,
            m10 * other.m10, m11 * other.m11
        )
    }

    @JvmOverloads
    fun mix(other: Matrix2f, t: Float, dst: Matrix2f = this): Matrix2f {
        return dst.set(
            (other.m00 - m00) * t + m00, (other.m01 - m01) * t + m01,
            (other.m10 - m10) * t + m10, (other.m11 - m11) * t + m11
        )
    }

    @JvmOverloads
    fun lerp(other: Matrix2f, t: Float, dst: Matrix2f = this): Matrix2f {
        return mix(other, t, dst)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) &&
                JomlMath.isFinite(m10) && JomlMath.isFinite(m11)
}