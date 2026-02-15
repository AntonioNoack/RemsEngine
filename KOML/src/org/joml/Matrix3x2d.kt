package org.joml

import org.joml.JomlMath.addSigns
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Suppress("unused")
open class Matrix3x2d : Matrix<Matrix3x2d, Vector2d, Vector3d> {

    var m00 = 0.0
    var m01 = 0.0
    var m10 = 0.0
    var m11 = 0.0
    var m20 = 0.0
    var m21 = 0.0

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

    constructor(mat: Matrix3x2d) {
        setMatrix3x2d(mat)
    }

    constructor(v0: Vector2d, v1: Vector2d, v2: Vector2d) : this(v0.x, v0.y, v1.x, v1.y, v2.x, v2.y)

    constructor(m00: Double, m01: Double, m10: Double, m11: Double, m20: Double, m21: Double) {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        this.m20 = m20
        this.m21 = m21
    }

    override val numCols: Int get() = 3
    override val numRows: Int get() = 2

    fun _m00(m00: Double): Matrix3x2d {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Double): Matrix3x2d {
        this.m01 = m01
        return this
    }

    fun _m10(m10: Double): Matrix3x2d {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Double): Matrix3x2d {
        this.m11 = m11
        return this
    }

    fun _m20(m20: Double): Matrix3x2d {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Double): Matrix3x2d {
        this.m21 = m21
        return this
    }

    fun set(m: Matrix3x2d): Matrix3x2d {
        setMatrix3x2d(m)
        return this
    }

    override fun getRow(row: Int, dst: Vector3d): Vector3d {
        when (row) {
            0 -> dst.set(m00, m10, m20)
            else -> dst.set(m01, m11, m21)
        }
        return dst
    }

    override fun setRow(row: Int, src: Vector3d): Matrix3x2d {
        when (row) {
            0 -> {
                m00 = src.x
                m10 = src.y
                m20 = src.z
            }
            else -> {
                m01 = src.x
                m11 = src.y
                m21 = src.z
            }
        }
        return this
    }

    override fun getColumn(column: Int, dst: Vector2d): Vector2d {
        when (column) {
            0 -> dst.set(m00, m01)
            1 -> dst.set(m10, m11)
            else -> dst.set(m20, m21)
        }
        return dst
    }

    override fun setColumn(column: Int, src: Vector2d): Matrix3x2d {
        when (column) {
            0 -> {
                m00 = src.x
                m01 = src.y
            }
            1 -> {
                m10 = src.x
                m11 = src.y
            }
            else -> {
                m20 = src.x
                m21 = src.y
            }
        }
        return this
    }

    private fun setMatrix3x2d(mat: Matrix3x2d) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
        m20 = mat.m20
        m21 = mat.m21
    }

    fun set(m: Matrix2d): Matrix3x2d {
        setMatrix2d(m)
        return this
    }

    private fun setMatrix2d(mat: Matrix2d) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    fun set(m: Matrix2f): Matrix3x2d {
        m00 = m.m00.toDouble()
        m01 = m.m01.toDouble()
        m10 = m.m10.toDouble()
        m11 = m.m11.toDouble()
        return this
    }

    override operator fun get(column: Int, row: Int): Double {
        return when (column * 2 + row) {
            0 -> m00
            1 -> m01
            2 -> m10
            3 -> m11
            4 -> m20
            else -> m21
        }
    }

    override operator fun set(column: Int, row: Int, value: Double): Matrix3x2d {
        when (column * 2 + row) {
            0 -> m00 = value
            1 -> m01 = value
            2 -> m10 = value
            3 -> m11 = value
            4 -> m20 = value
            else -> m21 = value
        }
        return this
    }

    @JvmOverloads
    fun mul(right: Matrix3x2d, dst: Matrix3x2d = this): Matrix3x2d {
        val nm00 = m00 * right.m00 + m10 * right.m01
        val nm01 = m01 * right.m00 + m11 * right.m01
        val nm10 = m00 * right.m10 + m10 * right.m11
        val nm11 = m01 * right.m10 + m11 * right.m11
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21
        return dst.set(nm00, nm01, nm10, nm11, nm20, nm21)
    }

    @JvmOverloads
    fun mulLocal(left: Matrix3x2d, dst: Matrix3x2d = this): Matrix3x2d {
        val nm00 = left.m00 * m00 + left.m10 * m01
        val nm01 = left.m01 * m00 + left.m11 * m01
        val nm10 = left.m00 * m10 + left.m10 * m11
        val nm11 = left.m01 * m10 + left.m11 * m11
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21
        return dst.set(nm00, nm01, nm10, nm11, nm20, nm21)
    }

    @JvmOverloads
    fun add(other: Matrix3x2d, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = m00 + other.m00
        dst.m01 = m01 + other.m01
        dst.m10 = m10 + other.m10
        dst.m11 = m11 + other.m11
        dst.m20 = m20 + other.m20
        dst.m21 = m21 + other.m21
        return dst
    }

    @JvmOverloads
    fun sub(subtrahend: Matrix3x2d, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = m00 - subtrahend.m00
        dst.m01 = m01 - subtrahend.m01
        dst.m10 = m10 - subtrahend.m10
        dst.m11 = m11 - subtrahend.m11
        dst.m20 = m20 - subtrahend.m20
        dst.m21 = m21 - subtrahend.m21
        return dst
    }

    @JvmOverloads
    fun mix(other: Matrix3x2d, t: Double, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = (other.m00 - m00) * t + m00
        dst.m01 = (other.m01 - m01) * t + m01
        dst.m10 = (other.m10 - m10) * t + m10
        dst.m11 = (other.m11 - m11) * t + m11
        dst.m20 = (other.m20 - m20) * t + m20
        dst.m21 = (other.m21 - m21) * t + m21
        return dst
    }

    @JvmOverloads
    fun lerp(other: Matrix3x2d, t: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return mix(other, t, dst)
    }

    @JvmOverloads
    fun mulComponentWise(other: Matrix3x2d, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = m00 * other.m00
        dst.m01 = m01 * other.m01
        dst.m10 = m10 * other.m10
        dst.m11 = m11 * other.m11
        dst.m20 = m20 * other.m20
        dst.m21 = m21 * other.m21
        return dst
    }

    fun set(m00: Double, m01: Double, m10: Double, m11: Double, m20: Double, m21: Double): Matrix3x2d {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        this.m20 = m20
        this.m21 = m21
        return this
    }

    fun determinant(): Double {
        return m00 * m11 - m01 * m10
    }

    @JvmOverloads
    fun invert(dst: Matrix3x2d = this): Matrix3x2d {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        val nm20 = (m10 * m21 - m20 * m11) * s
        val nm21 = (m20 * m01 - m00 * m21) * s
        return dst.set(nm00, nm01, nm10, nm11, nm20, nm21)
    }

    fun translation(x: Double, y: Double): Matrix3x2d {
        m00 = 1.0
        m01 = 0.0
        m10 = 0.0
        m11 = 1.0
        m20 = x
        m21 = y
        return this
    }

    fun translation(offset: Vector2d): Matrix3x2d {
        return translation(offset.x, offset.y)
    }

    fun setTranslation(x: Double, y: Double): Matrix3x2d {
        m20 = x
        m21 = y
        return this
    }

    fun setTranslation(offset: Vector2d): Matrix3x2d {
        return setTranslation(offset.x, offset.y)
    }

    @JvmOverloads
    fun translate(x: Double, y: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val nm20 = m00 * x + m10 * y + m20
        val nm21 = m01 * x + m11 * y + m21
        return dst.set(m00, m01, m10, m11, nm20, nm21)
    }

    fun translate(offset: Vector2d, dst: Matrix3x2d = this): Matrix3x2d {
        return translate(offset.x, offset.y, dst)
    }

    fun translate(offset: Vector2d): Matrix3x2d {
        return translate(offset.x, offset.y, this)
    }

    fun translateLocal(offset: Vector2d): Matrix3x2d {
        return translateLocal(offset.x, offset.y)
    }

    fun translateLocal(offset: Vector2d, dst: Matrix3x2d = this): Matrix3x2d {
        return translateLocal(offset.x, offset.y, dst)
    }

    @JvmOverloads
    fun translateLocal(x: Double, y: Double, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = m00
        dst.m01 = m01
        dst.m10 = m10
        dst.m11 = m11
        dst.m20 = m20 + x
        dst.m21 = m21 + y
        return dst
    }

    override fun toString() =
        ("[[${Runtime.f(m00)} ${Runtime.f(m10)} ${Runtime.f(m20)}] " +
                "[${Runtime.f(m01)} ${Runtime.f(m11)} ${Runtime.f(m21)}]]").addSigns()

    fun get(dst: Matrix3x2d = this): Matrix3x2d {
        return dst.set(this)
    }

    @JvmOverloads
    fun get(arr: DoubleArray, offset: Int = 0): DoubleArray {
        arr[offset] = m00
        arr[offset + 1] = m01
        arr[offset + 2] = m10
        arr[offset + 3] = m11
        arr[offset + 4] = m20
        arr[offset + 5] = m21
        return arr
    }

    fun zero(): Matrix3x2d {
        return scaling(0.0)
    }

    fun identity(): Matrix3x2d {
        return scaling(1.0)
    }

    @JvmOverloads
    fun scale(x: Double, y: Double, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = m00 * x
        dst.m01 = m01 * x
        dst.m10 = m10 * y
        dst.m11 = m11 * y
        dst.m20 = m20
        dst.m21 = m21
        return dst
    }

    @JvmOverloads
    fun scale(xy: Vector2d, dst: Matrix3x2d = this): Matrix3x2d {
        return scale(xy.x, xy.y, dst)
    }

    @JvmOverloads
    fun scale(xy: Vector2f, dst: Matrix3x2d = this): Matrix3x2d {
        return scale(xy.x.toDouble(), xy.y.toDouble(), dst)
    }

    @JvmOverloads
    fun scale(xy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return scale(xy, xy, dst)
    }

    @JvmOverloads
    fun scaleLocal(x: Double, y: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return dst.set(m00 * x, m01 * y, m10 * x, m11 * y, m20 * x, m21 * y)
    }

    fun scaleLocal(xy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return scaleLocal(xy, xy, dst)
    }

    @JvmOverloads
    fun scaleAround(sx: Double, sy: Double, ox: Double, oy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val nm20 = m00 * ox + m10 * oy + m20
        val nm21 = m01 * ox + m11 * oy + m21
        dst.m00 = m00 * sx
        dst.m01 = m01 * sx
        dst.m10 = m10 * sy
        dst.m11 = m11 * sy
        dst.m20 = dst.m00 * -ox + dst.m10 * -oy + nm20
        dst.m21 = dst.m01 * -ox + dst.m11 * -oy + nm21
        return dst
    }

    fun scaleAround(factor: Double, ox: Double, oy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return scaleAround(factor, factor, ox, oy, dst)
    }

    @JvmOverloads
    fun scaleAroundLocal(sx: Double, sy: Double, ox: Double, oy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        dst.m00 = sx * m00
        dst.m01 = sy * m01
        dst.m10 = sx * m10
        dst.m11 = sy * m11
        dst.m20 = sx * m20 - sx * ox + ox
        dst.m21 = sy * m21 - sy * oy + oy
        return dst
    }

    @JvmOverloads
    fun scaleAroundLocal(factor: Double, ox: Double, oy: Double, dst: Matrix3x2d = this): Matrix3x2d {
        return scaleAroundLocal(factor, factor, ox, oy, dst)
    }

    fun scaling(factor: Double): Matrix3x2d {
        return scaling(factor, factor)
    }

    fun scaling(x: Double, y: Double): Matrix3x2d {
        return set(x, 0.0, 0.0, y, 0.0, 0.0)
    }

    fun rotation(angle: Double): Matrix3x2d {
        val cos = cos(angle)
        val sin = sin(angle)
        return set(cos, sin, -sin, cos, 0.0, 0.0)
    }

    @JvmOverloads
    fun transform(v: Vector3d, dst: Vector3d = v): Vector3d {
        return v.mul(this, dst)
    }

    fun transform(x: Double, y: Double, z: Double, dst: Vector3d): Vector3d {
        return transform(dst.set(x, y, z))
    }

    @JvmOverloads
    fun transformPosition(v: Vector2d, dst: Vector2d = v): Vector2d {
        return v.mulPosition(this, dst)
    }

    fun transformPosition(x: Double, y: Double, dst: Vector2d): Vector2d {
        return transformPosition(dst.set(x, y))
    }

    @JvmOverloads
    fun transformDirection(v: Vector2d, dst: Vector2d = v): Vector2d {
        return v.mulDirection(this, dst)
    }

    fun transformDirection(x: Double, y: Double, dst: Vector2d): Vector2d {
        return dst.set(x, y).mulDirection(this, dst)
    }

    /**
     * inverts this matrix without saving the result, and then transforming v as a position
     * */
    fun transformPositionInverse(v: Vector2d, dst: Vector2d = v): Vector2d {
        v.sub(m20, m21, dst)
        return transformDirectionInverse(dst)
    }

    /**
     * inverts this matrix without saving the result, and then transforming v as a direction
     * */
    fun transformDirectionInverse(v: Vector2d, dst: Vector2d = v): Vector2d {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        if (!s.isFinite()) return dst.set(0.0)

        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        return dst.set(nm00 * v.x + nm10 * v.y, nm01 * v.x + nm11 * v.y)
    }

    @JvmOverloads
    fun rotate(ang: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val cos = cos(ang)
        val sin = sin(ang)
        val rm10 = -sin
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        val nm10 = m00 * rm10 + m10 * cos
        val nm11 = m01 * rm10 + m11 * cos
        return dst.set(nm00, nm01, nm10, nm11, m20, m21)
    }

    @JvmOverloads
    fun rotateLocal(ang: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        return dst.set(nm00, nm01, nm10, nm11, nm20, nm21)
    }

    @JvmOverloads
    fun rotateAbout(ang: Double, x: Double, y: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val tm20 = m00 * x + m10 * y + m20
        val tm21 = m01 * x + m11 * y + m21
        val cos = cos(ang)
        val sin = sin(ang)
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        val nm10 = m00 * -sin + m10 * cos
        val nm11 = m01 * -sin + m11 * cos
        val nm20 = nm00 * -x + nm10 * -y + tm20
        val nm21 = nm01 * -x + nm11 * -y + tm21
        return dst.set(nm00, nm01, nm10, nm11, nm20, nm21)
    }

    @JvmOverloads
    fun rotateTo(fromDir: Vector2d, toDir: Vector2d, dst: Matrix3x2d = this): Matrix3x2d {
        val dot = fromDir.x * toDir.x + fromDir.y * toDir.y
        val det = fromDir.x * toDir.y - fromDir.y * toDir.x
        val rm10 = -det
        val nm00 = m00 * dot + m10 * det
        val nm01 = m01 * dot + m11 * det
        val nm10 = m00 * rm10 + m10 * dot
        val nm11 = m01 * rm10 + m11 * dot
        return dst.set(nm00, nm01, nm10, nm11, m20, m21)
    }

    @JvmOverloads
    fun view(left: Double, right: Double, bottom: Double, top: Double, dst: Matrix3x2d = this): Matrix3x2d {
        val rm00 = 2.0 / (right - left)
        val rm11 = 2.0 / (top - bottom)
        val rm20 = (left + right) / (left - right)
        val rm21 = (bottom + top) / (bottom - top)
        dst.m20 = m00 * rm20 + m10 * rm21 + m20
        dst.m21 = m01 * rm20 + m11 * rm21 + m21
        dst.m00 = m00 * rm00
        dst.m01 = m01 * rm00
        dst.m10 = m10 * rm11
        dst.m11 = m11 * rm11
        return dst
    }

    fun setView(left: Double, right: Double, bottom: Double, top: Double): Matrix3x2d {
        m00 = 2.0 / (right - left)
        m01 = 0.0
        m10 = 0.0
        m11 = 2.0 / (top - bottom)
        m20 = (left + right) / (left - right)
        m21 = (bottom + top) / (bottom - top)
        return this
    }

    fun origin(origin: Vector2d): Vector2d {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        origin.x = (m10 * m21 - m20 * m11) * s
        origin.y = (m20 * m01 - m00 * m21) * s
        return origin
    }

    fun viewArea(area: DoubleArray): DoubleArray {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        val rm00 = m11 * s
        val rm01 = -m01 * s
        val rm10 = -m10 * s
        val rm11 = m00 * s
        val rm20 = (m10 * m21 - m20 * m11) * s
        val rm21 = (m20 * m01 - m00 * m21) * s
        val nxnyX = -rm00 - rm10
        val nxnyY = -rm01 - rm11
        val pxnyX = rm00 - rm10
        val pxnyY = rm01 - rm11
        val nxpyX = -rm00 + rm10
        val nxpyY = -rm01 + rm11
        val pxpyX = rm00 + rm10
        val pxpyY = rm01 + rm11
        var minX = min(nxnyX, nxpyX)
        minX = min(minX, pxnyX)
        minX = min(minX, pxpyX)
        var minY = min(nxnyY, nxpyY)
        minY = min(minY, pxnyY)
        minY = min(minY, pxpyY)
        var maxX = max(nxnyX, nxpyX)
        maxX = max(maxX, pxnyX)
        maxX = max(maxX, pxpyX)
        var maxY = max(nxnyY, nxpyY)
        maxY = max(maxY, pxnyY)
        maxY = max(maxY, pxpyY)
        area[0] = minX + rm20
        area[1] = minY + rm21
        area[2] = maxX + rm20
        area[3] = maxY + rm21
        return area
    }

    fun positiveX(dir: Vector2d): Vector2d {
        var s = m00 * m11 - m01 * m10
        s = 1.0 / s
        dir.x = m11 * s
        dir.y = -m01 * s
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector2d): Vector2d {
        dir.x = m11
        dir.y = -m01
        return dir
    }

    fun positiveY(dir: Vector2d): Vector2d {
        var s = m00 * m11 - m01 * m10
        s = 1.0 / s
        dir.x = -m10 * s
        dir.y = m00 * s
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector2d): Vector2d {
        dir.x = -m10
        dir.y = m00
        return dir
    }

    fun unproject(winX: Double, winY: Double, viewport: IntArray, dst: Vector2d): Vector2d {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        val im00 = m11 * s
        val im01 = -m01 * s
        val im10 = -m10 * s
        val im11 = m00 * s
        val im20 = (m10 * m21 - m20 * m11) * s
        val im21 = (m20 * m01 - m00 * m21) * s
        val ndcX = (winX - viewport[0].toDouble()) / viewport[2].toDouble() * 2.0 - 1.0
        val ndcY = (winY - viewport[1].toDouble()) / viewport[3].toDouble() * 2.0 - 1.0
        dst.x = im00 * ndcX + im10 * ndcY + im20
        dst.y = im01 * ndcX + im11 * ndcY + im21
        return dst
    }

    fun unprojectInv(winX: Double, winY: Double, viewport: IntArray, dst: Vector2d): Vector2d {
        val ndcX = (winX - viewport[0].toDouble()) / viewport[2].toDouble() * 2.0 - 1.0
        val ndcY = (winY - viewport[1].toDouble()) / viewport[3].toDouble() * 2.0 - 1.0
        dst.x = m00 * ndcX + m10 * ndcY + m20
        dst.y = m01 * ndcX + m11 * ndcY + m21
        return dst
    }

    fun span(corner: Vector2d, xDir: Vector2d, yDir: Vector2d): Matrix3x2d {
        val s = 1.0 / (m00 * m11 - m01 * m10)
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        corner.x = -nm00 - nm10 + (m10 * m21 - m20 * m11) * s
        corner.y = -nm01 - nm11 + (m20 * m01 - m00 * m21) * s
        xDir.x = 2.0 * nm00
        xDir.y = 2.0 * nm01
        yDir.x = 2.0 * nm10
        yDir.y = 2.0 * nm11
        return this
    }

    fun testPoint(x: Double, y: Double): Boolean {
        val nxX = m00
        val nxY = m10
        val nxW = 1.0 + m20
        val pxX = -m00
        val pxY = -m10
        val pxW = 1.0 - m20
        val nyX = m01
        val nyY = m11
        val nyW = 1.0 + m21
        val pyX = -m01
        val pyY = -m11
        val pyW = 1.0 - m21
        return nxX * x + nxY * y + nxW >= 0.0 && pxX * x + pxY * y + pxW >= 0.0 &&
                nyX * x + nyY * y + nyW >= 0.0 && pyX * x + pyY * y + pyW >= 0.0
    }

    fun testCircle(x: Double, y: Double, r: Double): Boolean {
        var nxX = m00
        var nxY = m10
        var nxW = 1.0 + m20
        var invl = JomlMath.invsqrt(nxX * nxX + nxY * nxY)
        nxX *= invl
        nxY *= invl
        nxW *= invl
        var pxX = -m00
        var pxY = -m10
        var pxW = 1.0 - m20
        invl = JomlMath.invsqrt(pxX * pxX + pxY * pxY)
        pxX *= invl
        pxY *= invl
        pxW *= invl
        var nyX = m01
        var nyY = m11
        var nyW = 1.0 + m21
        invl = JomlMath.invsqrt(nyX * nyX + nyY * nyY)
        nyX *= invl
        nyY *= invl
        nyW *= invl
        var pyX = -m01
        var pyY = -m11
        var pyW = 1.0 - m21
        invl = JomlMath.invsqrt(pyX * pyX + pyY * pyY)
        pyX *= invl
        pyY *= invl
        pyW *= invl
        return nxX * x + nxY * y + nxW >= -r && pxX * x + pxY * y + pxW >= -r &&
                nyX * x + nyY * y + nyW >= -r && pyX * x + pyY * y + pyW >= -r
    }

    fun testAar(minX: Double, minY: Double, maxX: Double, maxY: Double): Boolean {
        val nxX = m00
        val nxY = m10
        val nxW = 1.0 + m20
        val pxX = -m00
        val pxY = -m10
        val pxW = 1.0 - m20
        val nyX = m01
        val nyY = m11
        val nyW = 1.0 + m21
        val pyX = -m01
        val pyY = -m11
        val pyW = 1.0 - m21
        return nxX * (if (nxX < 0.0) minX else maxX) + nxY * (if (nxY < 0.0) minY else maxY) >= -nxW &&
                pxX * (if (pxX < 0.0) minX else maxX) + pxY * (if (pxY < 0.0) minY else maxY) >= -pxW &&
                nyX * (if (nyX < 0.0) minX else maxX) + nyY * (if (nyY < 0.0) minY else maxY) >= -nyW &&
                pyX * (if (pyX < 0.0) minX else maxX) + pyY * (if (pyY < 0.0) minY else maxY) >= -pyW
    }

    override fun hashCode(): Int {
        var result = m00.hashCode()
        result = 31 * result + m01.hashCode()
        result = 31 * result + m10.hashCode()
        result = 31 * result + m11.hashCode()
        result = 31 * result + m20.hashCode()
        result = 31 * result + m21.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Matrix3x2d &&
                m00 == other.m00 && m01 == other.m01 &&
                m10 == other.m10 && m11 == other.m11 &&
                m20 == other.m20 && m21 == other.m21
    }

    override fun equals(other: Matrix3x2d?, threshold: Double): Boolean {
        if (this === other) return true
        return other != null &&
                Runtime.equals(m00, other.m00, threshold) && Runtime.equals(m01, other.m01, threshold) &&
                Runtime.equals(m10, other.m10, threshold) && Runtime.equals(m11, other.m11, threshold) &&
                Runtime.equals(m20, other.m20, threshold) && Runtime.equals(m21, other.m21, threshold)
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(m00) && JomlMath.isFinite(m01) && JomlMath.isFinite(m10) &&
                JomlMath.isFinite(m11) && JomlMath.isFinite(m20) && JomlMath.isFinite(m21)
}