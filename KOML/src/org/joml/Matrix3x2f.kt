package org.joml

import org.joml.JomlMath.addSigns
import org.joml.JomlMath.invsqrt
import org.joml.JomlMath.isFinite
import org.joml.Runtime.f
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Suppress("unused")
open class Matrix3x2f {

    var m00 = 0f
    var m01 = 0f
    var m10 = 0f
    var m11 = 0f
    var m20 = 0f
    var m21 = 0f

    constructor() {
        m00 = 1f
        m11 = 1f
    }

    constructor(mat: Matrix3x2f) {
        setMatrix3x2fc(mat)
    }

    constructor(mat: Matrix2f) {
        setMatrix2fc(mat)
    }

    constructor(m00: Float, m01: Float, m10: Float, m11: Float, m20: Float, m21: Float) {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        this.m20 = m20
        this.m21 = m21
    }

    fun _m00(m00: Float): Matrix3x2f {
        this.m00 = m00
        return this
    }

    fun _m01(m01: Float): Matrix3x2f {
        this.m01 = m01
        return this
    }

    fun _m10(m10: Float): Matrix3x2f {
        this.m10 = m10
        return this
    }

    fun _m11(m11: Float): Matrix3x2f {
        this.m11 = m11
        return this
    }

    fun _m20(m20: Float): Matrix3x2f {
        this.m20 = m20
        return this
    }

    fun _m21(m21: Float): Matrix3x2f {
        this.m21 = m21
        return this
    }

    fun set(m: Matrix3x2f): Matrix3x2f {
        setMatrix3x2fc(m)
        return this
    }

    private fun setMatrix3x2fc(mat: Matrix3x2f) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
        m20 = mat.m20
        m21 = mat.m21
    }

    fun set(m: Matrix2f): Matrix3x2f {
        setMatrix2fc(m)
        return this
    }

    private fun setMatrix2fc(mat: Matrix2f) {
        m00 = mat.m00
        m01 = mat.m01
        m10 = mat.m10
        m11 = mat.m11
    }

    fun putInto(arr: FloatBuffer): FloatBuffer {
        arr.put(m00).put(m01)
        arr.put(m10).put(m11)
        arr.put(m20).put(m21)
        return arr
    }

    @JvmOverloads
    fun mul(right: Matrix3x2f, dest: Matrix3x2f = this): Matrix3x2f {
        val nm00 = m00 * right.m00 + m10 * right.m01
        val nm01 = m01 * right.m00 + m11 * right.m01
        val nm10 = m00 * right.m10 + m10 * right.m11
        val nm11 = m01 * right.m10 + m11 * right.m11
        val nm20 = m00 * right.m20 + m10 * right.m21 + m20
        val nm21 = m01 * right.m20 + m11 * right.m21 + m21
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m20 = nm20
        dest.m21 = nm21
        return dest
    }

    @JvmOverloads
    fun mulLocal(left: Matrix3x2f, dest: Matrix3x2f = this): Matrix3x2f {
        val nm00 = left.m00 * m00 + left.m10 * m01
        val nm01 = left.m01 * m00 + left.m11 * m01
        val nm10 = left.m00 * m10 + left.m10 * m11
        val nm11 = left.m01 * m10 + left.m11 * m11
        val nm20 = left.m00 * m20 + left.m10 * m21 + left.m20
        val nm21 = left.m01 * m20 + left.m11 * m21 + left.m21
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m20 = nm20
        dest.m21 = nm21
        return dest
    }

    fun set(m00: Float, m01: Float, m10: Float, m11: Float, m20: Float, m21: Float): Matrix3x2f {
        this.m00 = m00
        this.m01 = m01
        this.m10 = m10
        this.m11 = m11
        this.m20 = m20
        this.m21 = m21
        return this
    }

    fun set(m: FloatArray): Matrix3x2f {
        m00 = m[0]
        m01 = m[1]
        m10 = m[2]
        m11 = m[3]
        m20 = m[4]
        m21 = m[5]
        return this
    }

    fun determinant(): Float {
        return m00 * m11 - m01 * m10
    }

    @JvmOverloads
    fun invert(dest: Matrix3x2f = this): Matrix3x2f {
        val s = 1f / (m00 * m11 - m01 * m10)
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        val nm20 = (m10 * m21 - m20 * m11) * s
        val nm21 = (m20 * m01 - m00 * m21) * s
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m20 = nm20
        dest.m21 = nm21
        return dest
    }

    fun translation(x: Float, y: Float): Matrix3x2f {
        m00 = 1f
        m01 = 0f
        m10 = 0f
        m11 = 1f
        m20 = x
        m21 = y
        return this
    }

    fun translation(offset: Vector2f): Matrix3x2f {
        return this.translation(offset.x, offset.y)
    }

    fun setTranslation(x: Float, y: Float): Matrix3x2f {
        m20 = x
        m21 = y
        return this
    }

    fun setTranslation(offset: Vector2f): Matrix3x2f {
        return this.setTranslation(offset.x, offset.y)
    }

    @JvmOverloads
    fun translate(x: Float, y: Float, dest: Matrix3x2f = this): Matrix3x2f {
        dest.m20 = m00 * x + m10 * y + m20
        dest.m21 = m01 * x + m11 * y + m21
        dest.m00 = m00
        dest.m01 = m01
        dest.m10 = m10
        dest.m11 = m11
        return dest
    }

    fun translate(offset: Vector2f, dest: Matrix3x2f): Matrix3x2f {
        return this.translate(offset.x, offset.y, dest)
    }

    fun translate(offset: Vector2f): Matrix3x2f {
        return this.translate(offset.x, offset.y, this)
    }

    fun translateLocal(offset: Vector2f): Matrix3x2f {
        return this.translateLocal(offset.x, offset.y)
    }

    fun translateLocal(offset: Vector2f, dest: Matrix3x2f): Matrix3x2f {
        return this.translateLocal(offset.x, offset.y, dest)
    }

    @JvmOverloads
    fun translateLocal(x: Float, y: Float, dest: Matrix3x2f = this): Matrix3x2f {
        dest.m00 = m00
        dest.m01 = m01
        dest.m10 = m10
        dest.m11 = m11
        dest.m20 = m20 + x
        dest.m21 = m21 + y
        return dest
    }

    override fun toString() =
        ("[[${f(m00)} ${f(m10)} ${f(m20)}] " +
                "[${f(m01)} ${f(m11)} ${f(m21)}]]").addSigns()

    operator fun get(dest: Matrix3x2f): Matrix3x2f {
        return dest.set(this)
    }

    @JvmOverloads
    fun get(arr: FloatArray, offset: Int = 0): FloatArray {
        arr[offset] = m00
        arr[offset + 1] = m01
        arr[offset + 2] = m10
        arr[offset + 3] = m11
        arr[offset + 4] = m20
        arr[offset + 5] = m21
        return arr
    }

    /*@JvmOverloads
    fun get3x3(arr: FloatArray, offset: Int = 0): FloatArray {
        MemUtil.INSTANCE.copy3x3(this, arr, offset)
        return arr
    }

    @JvmOverloads
    fun get4x4(arr: FloatArray, offset: Int = 0): FloatArray {
        MemUtil.INSTANCE.copy4x4(this, arr, offset)
        return arr
    }*/

    fun zero(): Matrix3x2f {
        m00 = 0f
        m01 = 0f
        m10 = 0f
        m11 = 0f
        m20 = 0f
        m21 = 0f
        return this
    }

    fun identity(): Matrix3x2f {
        m00 = 1f
        m01 = 0f
        m10 = 0f
        m11 = 1f
        m20 = 0f
        m21 = 0f
        return this
    }

    @JvmOverloads
    fun scale(x: Float, y: Float, dest: Matrix3x2f = this): Matrix3x2f {
        dest.m00 = m00 * x
        dest.m01 = m01 * x
        dest.m10 = m10 * y
        dest.m11 = m11 * y
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    fun scale(xy: Vector2f): Matrix3x2f {
        return this.scale(xy.x, xy.y, this)
    }

    fun scale(xy: Vector2f, dest: Matrix3x2f): Matrix3x2f {
        return this.scale(xy.x, xy.y, dest)
    }

    fun scale(xy: Float, dest: Matrix3x2f): Matrix3x2f {
        return this.scale(xy, xy, dest)
    }

    fun scale(xy: Float): Matrix3x2f {
        return this.scale(xy, xy)
    }

    @JvmOverloads
    fun scaleLocal(x: Float, y: Float, dest: Matrix3x2f = this): Matrix3x2f {
        dest.m00 = x * m00
        dest.m01 = y * m01
        dest.m10 = x * m10
        dest.m11 = y * m11
        dest.m20 = x * m20
        dest.m21 = y * m21
        return dest
    }

    fun scaleLocal(xy: Float, dest: Matrix3x2f): Matrix3x2f {
        return this.scaleLocal(xy, xy, dest)
    }

    fun scaleLocal(xy: Float): Matrix3x2f {
        return this.scaleLocal(xy, xy, this)
    }

    @JvmOverloads
    fun scaleAround(sx: Float, sy: Float, ox: Float, oy: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val nm20 = m00 * ox + m10 * oy + m20
        val nm21 = m01 * ox + m11 * oy + m21
        dest.m00 = m00 * sx
        dest.m01 = m01 * sx
        dest.m10 = m10 * sy
        dest.m11 = m11 * sy
        dest.m20 = dest.m00 * -ox + dest.m10 * -oy + nm20
        dest.m21 = dest.m01 * -ox + dest.m11 * -oy + nm21
        return dest
    }

    fun scaleAround(factor: Float, ox: Float, oy: Float, dest: Matrix3x2f?): Matrix3x2f {
        return this.scaleAround(factor, factor, ox, oy, this)
    }

    fun scaleAround(factor: Float, ox: Float, oy: Float): Matrix3x2f {
        return this.scaleAround(factor, factor, ox, oy, this)
    }

    fun scaleAroundLocal(sx: Float, sy: Float, ox: Float, oy: Float, dest: Matrix3x2f): Matrix3x2f {
        dest.m00 = sx * m00
        dest.m01 = sy * m01
        dest.m10 = sx * m10
        dest.m11 = sy * m11
        dest.m20 = sx * m20 - sx * ox + ox
        dest.m21 = sy * m21 - sy * oy + oy
        return dest
    }

    fun scaleAroundLocal(factor: Float, ox: Float, oy: Float, dest: Matrix3x2f): Matrix3x2f {
        return this.scaleAroundLocal(factor, factor, ox, oy, dest)
    }

    fun scaleAroundLocal(sx: Float, sy: Float, sz: Float, ox: Float, oy: Float, oz: Float): Matrix3x2f {
        return this.scaleAroundLocal(sx, sy, ox, oy, this)
    }

    fun scaleAroundLocal(factor: Float, ox: Float, oy: Float): Matrix3x2f {
        return this.scaleAroundLocal(factor, factor, ox, oy, this)
    }

    fun scaling(factor: Float): Matrix3x2f {
        return this.scaling(factor, factor)
    }

    fun scaling(x: Float, y: Float): Matrix3x2f {
        m00 = x
        m01 = 0f
        m10 = 0f
        m11 = y
        m20 = 0f
        m21 = 0f
        return this
    }

    fun rotation(angle: Float): Matrix3x2f {
        val cos = cos(angle)
        val sin = sin(angle)
        m00 = cos
        m10 = -sin
        m20 = 0f
        m01 = sin
        m11 = cos
        m21 = 0f
        return this
    }

    fun transform(v: Vector3f): Vector3f {
        return v.mul(this)
    }

    fun transform(v: Vector3f, dest: Vector3f?): Vector3f {
        return v.mul(this, dest!!)
    }

    fun transform(x: Float, y: Float, z: Float, dest: Vector3f): Vector3f {
        return dest.set(m00 * x + m10 * y + m20 * z, m01 * x + m11 * y + m21 * z, z)
    }

    fun transformPosition(v: Vector2f): Vector2f {
        v.set(m00 * v.x + m10 * v.y + m20, m01 * v.x + m11 * v.y + m21)
        return v
    }

    fun transformPosition(v: Vector2f, dest: Vector2f): Vector2f {
        dest.set(m00 * v.x + m10 * v.y + m20, m01 * v.x + m11 * v.y + m21)
        return dest
    }

    fun transformPosition(x: Float, y: Float, dest: Vector2f): Vector2f {
        return dest.set(m00 * x + m10 * y + m20, m01 * x + m11 * y + m21)
    }

    fun transformDirection(v: Vector2f): Vector2f {
        v.set(m00 * v.x + m10 * v.y, m01 * v.x + m11 * v.y)
        return v
    }

    fun transformDirection(v: Vector2f, dest: Vector2f): Vector2f {
        dest.set(m00 * v.x + m10 * v.y, m01 * v.x + m11 * v.y)
        return dest
    }

    fun transformDirection(x: Float, y: Float, dest: Vector2f): Vector2f {
        return dest.set(m00 * x + m10 * y, m01 * x + m11 * y)
    }

    @JvmOverloads
    fun rotate(ang: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val cos = cos(ang)
        val sin = sin(ang)
        val rm10 = -sin
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        dest.m10 = m00 * rm10 + m10 * cos
        dest.m11 = m01 * rm10 + m11 * cos
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    @JvmOverloads
    fun rotateLocal(ang: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val sin = sin(ang)
        val cos = cos(ang)
        val nm00 = cos * m00 - sin * m01
        val nm01 = sin * m00 + cos * m01
        val nm10 = cos * m10 - sin * m11
        val nm11 = sin * m10 + cos * m11
        val nm20 = cos * m20 - sin * m21
        val nm21 = sin * m20 + cos * m21
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m20 = nm20
        dest.m21 = nm21
        return dest
    }

    @JvmOverloads
    fun rotateAbout(ang: Float, x: Float, y: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val tm20 = m00 * x + m10 * y + m20
        val tm21 = m01 * x + m11 * y + m21
        val cos = cos(ang)
        val sin = sin(ang)
        val nm00 = m00 * cos + m10 * sin
        val nm01 = m01 * cos + m11 * sin
        dest.m10 = m00 * -sin + m10 * cos
        dest.m11 = m01 * -sin + m11 * cos
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m20 = dest.m00 * -x + dest.m10 * -y + tm20
        dest.m21 = dest.m01 * -x + dest.m11 * -y + tm21
        return dest
    }

    @JvmOverloads
    fun rotateTo(fromDir: Vector2f, toDir: Vector2f, dest: Matrix3x2f = this): Matrix3x2f {
        val dot = fromDir.x * toDir.x + fromDir.y * toDir.y
        val det = fromDir.x * toDir.y - fromDir.y * toDir.x
        val rm10 = -det
        val nm00 = m00 * dot + m10 * det
        val nm01 = m01 * dot + m11 * det
        dest.m10 = m00 * rm10 + m10 * dot
        dest.m11 = m01 * rm10 + m11 * dot
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    @JvmOverloads
    fun view(left: Float, right: Float, bottom: Float, top: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val rm00 = 2f / (right - left)
        val rm11 = 2f / (top - bottom)
        val rm20 = (left + right) / (left - right)
        val rm21 = (bottom + top) / (bottom - top)
        dest.m20 = m00 * rm20 + m10 * rm21 + m20
        dest.m21 = m01 * rm20 + m11 * rm21 + m21
        dest.m00 = m00 * rm00
        dest.m01 = m01 * rm00
        dest.m10 = m10 * rm11
        dest.m11 = m11 * rm11
        return dest
    }

    fun setView(left: Float, right: Float, bottom: Float, top: Float): Matrix3x2f {
        m00 = 2f / (right - left)
        m01 = 0f
        m10 = 0f
        m11 = 2f / (top - bottom)
        m20 = (left + right) / (left - right)
        m21 = (bottom + top) / (bottom - top)
        return this
    }

    fun origin(origin: Vector2f): Vector2f {
        val s = 1f / (m00 * m11 - m01 * m10)
        origin.x = (m10 * m21 - m20 * m11) * s
        origin.y = (m20 * m01 - m00 * m21) * s
        return origin
    }

    fun viewArea(area: FloatArray): FloatArray {
        val s = 1f / (m00 * m11 - m01 * m10)
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

    fun positiveX(dir: Vector2f): Vector2f {
        var s = m00 * m11 - m01 * m10
        s = 1f / s
        dir.x = m11 * s
        dir.y = -m01 * s
        return dir.normalize(dir)
    }

    fun normalizedPositiveX(dir: Vector2f): Vector2f {
        dir.x = m11
        dir.y = -m01
        return dir
    }

    fun positiveY(dir: Vector2f): Vector2f {
        var s = m00 * m11 - m01 * m10
        s = 1f / s
        dir.x = -m10 * s
        dir.y = m00 * s
        return dir.normalize(dir)
    }

    fun normalizedPositiveY(dir: Vector2f): Vector2f {
        dir.x = -m10
        dir.y = m00
        return dir
    }

    fun unproject(winX: Float, winY: Float, viewport: IntArray, dest: Vector2f): Vector2f {
        val s = 1f / (m00 * m11 - m01 * m10)
        val im00 = m11 * s
        val im01 = -m01 * s
        val im10 = -m10 * s
        val im11 = m00 * s
        val im20 = (m10 * m21 - m20 * m11) * s
        val im21 = (m20 * m01 - m00 * m21) * s
        val ndcX = (winX - viewport[0]) / viewport[2] * 2f - 1f
        val ndcY = (winY - viewport[1]) / viewport[3] * 2f - 1f
        dest.x = im00 * ndcX + im10 * ndcY + im20
        dest.y = im01 * ndcX + im11 * ndcY + im21
        return dest
    }

    fun unprojectInv(winX: Float, winY: Float, viewport: IntArray, dest: Vector2f): Vector2f {
        val ndcX = (winX - viewport[0]) / viewport[2] * 2f - 1f
        val ndcY = (winY - viewport[1]) / viewport[3] * 2f - 1f
        dest.x = m00 * ndcX + m10 * ndcY + m20
        dest.y = m01 * ndcX + m11 * ndcY + m21
        return dest
    }

    @JvmOverloads
    fun shearX(yFactor: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val nm10 = m00 * yFactor + m10
        val nm11 = m01 * yFactor + m11
        dest.m00 = m00
        dest.m01 = m01
        dest.m10 = nm10
        dest.m11 = nm11
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    @JvmOverloads
    fun shearY(xFactor: Float, dest: Matrix3x2f = this): Matrix3x2f {
        val nm00 = m00 + m10 * xFactor
        val nm01 = m01 + m11 * xFactor
        dest.m00 = nm00
        dest.m01 = nm01
        dest.m10 = m10
        dest.m11 = m11
        dest.m20 = m20
        dest.m21 = m21
        return dest
    }

    fun span(corner: Vector2f, xDir: Vector2f, yDir: Vector2f): Matrix3x2f {
        val s = 1f / (m00 * m11 - m01 * m10)
        val nm00 = m11 * s
        val nm01 = -m01 * s
        val nm10 = -m10 * s
        val nm11 = m00 * s
        corner.x = -nm00 - nm10 + (m10 * m21 - m20 * m11) * s
        corner.y = -nm01 - nm11 + (m20 * m01 - m00 * m21) * s
        xDir.x = 2f * nm00
        xDir.y = 2f * nm01
        yDir.x = 2f * nm10
        yDir.y = 2f * nm11
        return this
    }

    fun testPoint(x: Float, y: Float): Boolean {
        val nxX = m00
        val nxY = m10
        val nxW = 1f + m20
        val pxX = -m00
        val pxY = -m10
        val pxW = 1f - m20
        val nyX = m01
        val nyY = m11
        val nyW = 1f + m21
        val pyX = -m01
        val pyY = -m11
        val pyW = 1f - m21
        return nxX * x + nxY * y + nxW >= 0f && pxX * x + pxY * y + pxW >= 0f && nyX * x + nyY * y + nyW >= 0f && pyX * x + pyY * y + pyW >= 0f
    }

    fun testCircle(x: Float, y: Float, r: Float): Boolean {
        var nxX = m00
        var nxY = m10
        var nxW = 1f + m20
        var invl = invsqrt(nxX * nxX + nxY * nxY)
        nxX *= invl
        nxY *= invl
        nxW *= invl
        var pxX = -m00
        var pxY = -m10
        var pxW = 1f - m20
        invl = invsqrt(pxX * pxX + pxY * pxY)
        pxX *= invl
        pxY *= invl
        pxW *= invl
        var nyX = m01
        var nyY = m11
        var nyW = 1f + m21
        invl = invsqrt(nyX * nyX + nyY * nyY)
        nyX *= invl
        nyY *= invl
        nyW *= invl
        var pyX = -m01
        var pyY = -m11
        var pyW = 1f - m21
        invl = invsqrt(pyX * pyX + pyY * pyY)
        pyX *= invl
        pyY *= invl
        pyW *= invl
        return nxX * x + nxY * y + nxW >= -r && pxX * x + pxY * y + pxW >= -r && nyX * x + nyY * y + nyW >= -r && pyX * x + pyY * y + pyW >= -r
    }

    fun testAar(minX: Float, minY: Float, maxX: Float, maxY: Float): Boolean {
        val nxX = m00
        val nxY = m10
        val nxW = 1f + m20
        val pxX = -m00
        val pxY = -m10
        val pxW = 1f - m20
        val nyX = m01
        val nyY = m11
        val nyW = 1f + m21
        val pyX = -m01
        val pyY = -m11
        val pyW = 1f - m21
        return nxX * (if (nxX < 0f) minX else maxX) + nxY * (if (nxY < 0f) minY else maxY) >= -nxW && pxX * (if (pxX < 0f) minX else maxX) + pxY * (if (pxY < 0f) minY else maxY) >= -pxW && nyX * (if (nyX < 0f) minX else maxX) + nyY * (if (nyY < 0f) minY else maxY) >= -nyW && pyX * (if (pyX < 0f) minX else maxX) + pyY * (if (pyY < 0f) minY else maxY) >= -pyW
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + (m00).toBits()
        result = 31 * result + (m01).toBits()
        result = 31 * result + (m10).toBits()
        result = 31 * result + (m11).toBits()
        result = 31 * result + (m20).toBits()
        result = 31 * result + (m21).toBits()
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
            other as Matrix3x2f
            if ((m00) != (other.m00)) {
                false
            } else if ((m01) != (other.m01)) {
                false
            } else if ((m10) != (other.m10)) {
                false
            } else if ((m11) != (other.m11)) {
                false
            } else if ((m20) != (other.m20)) {
                false
            } else {
                (m21) == (other.m21)
            }
        }
    }

    fun equals(m: Matrix3x2f?, delta: Float): Boolean {
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
        } else if (!Runtime.equals(m11, m.m11, delta)) {
            false
        } else if (!Runtime.equals(m20, m.m20, delta)) {
            false
        } else {
            Runtime.equals(m21, m.m21, delta)
        }
    }

    val isFinite: Boolean
        get() = isFinite(m00) && isFinite(m01) && isFinite(m10) && isFinite(m11) && isFinite(m20) && isFinite(m21)
}