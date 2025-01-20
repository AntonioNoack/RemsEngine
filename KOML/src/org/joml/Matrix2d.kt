package org.joml

import org.joml.JomlMath.addSigns
import org.joml.JomlMath.hash
import org.joml.Runtime.f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Matrix2d : Matrix<Matrix2d, Vector2d, Vector2d> {

    var m00 = 0.0
    var m01 = 0.0
    var m10 = 0.0
    var m11 = 0.0

    constructor() {
        m00 = 1.0
        m11 = 1.0
    }

    constructor(mat: Matrix2d) {
        set(mat)
    }

    constructor(mat: Matrix2f) {
        set(mat)
    }

    constructor(mat: Matrix3d) {
        set(mat)
    }

    constructor(mat: Matrix3f) {
        set(mat)
    }

    constructor(m00: Double, m01: Double, m10: Double, m11: Double) {
        set(m00, m01, m10, m11)
    }

    constructor(col0: Vector2d, col1: Vector2d) {
        set(col0, col1)
    }

    override val numCols: Int get() = 2
    override val numRows: Int get() = 2

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
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix2f): Matrix2d {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3x2d): Matrix2d {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3x2f): Matrix2d {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3d): Matrix2d {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    fun set(m: Matrix3f): Matrix2d {
        return set(m.m00, m.m01, m.m10, m.m11)
    }

    @JvmOverloads
    fun mul(right: Matrix2d, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            m00 * right.m00 + m10 * right.m01,
            m01 * right.m00 + m11 * right.m01,
            m00 * right.m10 + m10 * right.m11,
            m01 * right.m10 + m11 * right.m11
        )
    }

    @JvmOverloads
    fun mul(right: Matrix2f, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            m00 * right.m00.toDouble() + m10 * right.m01.toDouble(),
            m01 * right.m00.toDouble() + m11 * right.m01.toDouble(),
            m00 * right.m10.toDouble() + m10 * right.m11.toDouble(),
            m01 * right.m10.toDouble() + m11 * right.m11.toDouble()
        )
    }

    @JvmOverloads
    fun mulLocal(left: Matrix2d, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            left.m00 * m00 + left.m10 * m01,
            left.m01 * m00 + left.m11 * m01,
            left.m00 * m10 + left.m10 * m11,
            left.m01 * m10 + left.m11 * m11
        )
    }

    fun set(m00: Double, m01: Double, m10: Double, m11: Double): Matrix2d {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        return this
    }

    fun set(m00: Float, m01: Float, m10: Float, m11: Float): Matrix2d {
        return set(m00.toDouble(), m01.toDouble(), m10.toDouble(), m11.toDouble())
    }

    fun set(m: DoubleArray) = set(m[0], m[1], m[2], m[3])

    fun set(col0: Vector2d, col1: Vector2d): Matrix2d {
        return set(col0.x, col0.y, col1.x, col1.y)
    }

    fun determinant(): Double {
        return m00 * m11 - m10 * m01
    }

    @JvmOverloads
    fun invert(dst: Matrix2d = this): Matrix2d {
        val s = 1.0 / determinant()
        return dst.set(
            m11 * s, -m01 * s,
            -m10 * s, m00 * s
        )
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

    fun zero(): Matrix2d {
        return set(0.0, 0.0, 0.0, 0.0)
    }

    fun identity(): Matrix2d {
        return set(1.0, 0.0, 0.0, 1.0)
    }

    @JvmOverloads
    fun scale(xy: Vector2d, dst: Matrix2d = this): Matrix2d {
        return scale(xy.x, xy.y, dst)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, dst: Matrix2d = this): Matrix2d {
        return dst.set(m00 * x, m01 * x, m10 * y, m11 * y)
    }

    @JvmOverloads
    fun scale(xy: Double, dst: Matrix2d = this): Matrix2d {
        return scale(xy, xy, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, dst: Matrix2d = this): Matrix2d {
        return dst.set(x * m00, y * m01, x * m10, y * m11)
    }

    fun scaling(factor: Double): Matrix2d {
        return scaling(factor, factor)
    }

    fun scaling(x: Double, y: Double): Matrix2d {
        return set(x, 0.0, 0.0, y)
    }

    fun scaling(xy: Vector2d): Matrix2d {
        return scaling(xy.x, xy.y)
    }

    fun rotation(angle: Double): Matrix2d {
        val sin = sin(angle)
        val cos = cos(angle)
        return set(cos, sin, -sin, cos)
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
        return dst.set(nm00, nm01, nm10, nm11)
    }

    @JvmOverloads
    fun rotateLocal(angle: Double, dst: Matrix2d = this): Matrix2d {
        val s = sin(angle)
        val c = cos(angle)
        val nm00 = c * m00 - s * m01
        val nm01 = s * m00 + c * m01
        val nm10 = c * m10 - s * m11
        val nm11 = s * m10 + c * m11
        return dst.set(nm00, nm01, nm10, nm11)
    }

    override fun getRow(row: Int, dst: Vector2d): Vector2d {
        if (row == 0) dst.set(m00, m10)
        else dst.set(m01, m11)
        return dst
    }

    override fun setRow(row: Int, src: Vector2d): Matrix2d {
        return setRow(row, src.x, src.y)
    }

    fun setRow(row: Int, x: Double, y: Double): Matrix2d {
        if (row == 0) {
            m00 = x
            m10 = y
        } else {
            m01 = x
            m11 = y
        }
        return this
    }

    override fun getColumn(column: Int, dst: Vector2d): Vector2d {
        if (column == 0) dst.set(m00, m01)
        else dst.set(m10, m11)
        return dst
    }

    override fun setColumn(column: Int, src: Vector2d): Matrix2d {
        return setColumn(column, src.x, src.y)
    }

    fun setColumn(column: Int, x: Double, y: Double): Matrix2d {
        if (column == 0) {
            m00 = x
            m01 = y
        } else {
            m10 = x
            m11 = y
        }
        return this
    }

    override operator fun get(column: Int, row: Int): Double {
        return when (column * 2 + row) {
            0 -> m00
            1 -> m01
            2 -> m10
            else -> m11
        }
    }

    override operator fun set(column: Int, row: Int, value: Double): Matrix2d {
        when (column * 2 + row) {
            0 -> m00 = value
            1 -> m01 = value
            2 -> m10 = value
            else -> m11 = value
        }
        return this
    }

    @JvmOverloads
    fun normal(dst: Matrix2d = this): Matrix2d {
        val det = m00 * m11 - m10 * m01
        val s = 1.0 / det
        val nm00 = m11 * s
        val nm01 = -m10 * s
        val nm10 = -m01 * s
        val nm11 = m00 * s
        return dst.set(nm00, nm01, nm10, nm11)
    }

    fun getScale(dst: Vector2d): Vector2d {
        return dst.set(
            sqrt(m00 * m00 + m01 * m01),
            sqrt(m10 * m10 + m11 * m11)
        )
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
        result = 31 * result + hash(m00)
        result = 31 * result + hash(m01)
        result = 31 * result + hash(m10)
        result = 31 * result + hash(m11)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Matrix2d &&
                m00 == other.m00 && m01 == other.m01 && m10 == other.m10 && m11 == other.m11
    }

    override fun equals(other: Matrix2d?, threshold: Double): Boolean {
        if (other === this) return true
        return other != null &&
                Runtime.equals(m00, other.m00, threshold) && Runtime.equals(m01, other.m01, threshold) &&
                Runtime.equals(m10, other.m10, threshold) && Runtime.equals(m11, other.m11, threshold)
    }

    @JvmOverloads
    fun add(other: Matrix2d, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            m00 + other.m00, m01 + other.m01,
            m10 + other.m10, m11 + other.m11
        )
    }

    @JvmOverloads
    fun sub(other: Matrix2d, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            m00 - other.m00, m01 - other.m01,
            m10 - other.m10, m11 - other.m11
        )
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix2d, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            m00 * other.m00, m01 * other.m01,
            m10 * other.m10, m11 * other.m11
        )
    }

    fun mix(other: Matrix2d, t: Double, dst: Matrix2d = this): Matrix2d {
        return dst.set(
            (other.m00 - m00) * t + m00, (other.m01 - m01) * t + m01,
            (other.m10 - m10) * t + m10, (other.m11 - m11) * t + m11
        )
    }

    @JvmOverloads
    fun lerp(other: Matrix2d, t: Double, dst: Matrix2d = this): Matrix2d {
        return mix(other, t, dst)
    }

    val isFinite: Boolean
        get() = m00.isFinite() && m01.isFinite() &&
                m10.isFinite() && m11.isFinite()
}