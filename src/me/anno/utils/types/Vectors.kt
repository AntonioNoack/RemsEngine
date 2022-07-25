package me.anno.utils.types

import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f2s
import me.anno.utils.types.Triangles.subCross
import me.anno.utils.types.Triangles.subCrossDot
import org.joml.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Suppress("unused")
object Vectors {

    /**
     * the following functions allow for comfortable debugging with vectors;
     * they shouldn't be used in production to keep allocations at a minimum
     * */

    operator fun Vector2fc.plus(s: Vector2fc) = Vector2f(x() + s.x(), y() + s.y())
    operator fun Vector2fc.minus(s: Vector2fc) = Vector2f(x() - s.x(), y() - s.y())
    operator fun Vector2fc.times(f: Float) = Vector2f(x() * f, y() * f)
    operator fun Vector2fc.times(s: Vector2fc) = Vector2f(x() * s.x(), y() * s.y())

    operator fun Vector2dc.plus(s: Vector2dc) = Vector2d(x() + s.x(), y() + s.y())
    operator fun Vector2dc.minus(s: Vector2dc) = Vector2d(x() - s.x(), y() - s.y())
    operator fun Vector2dc.times(f: Double) = Vector2d(x() * f, y() * f)

    operator fun Vector3fc.plus(s: Vector3fc) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3fc.minus(s: Vector3fc) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3fc.times(s: Float) = Vector3f(x() * s, y() * s, z() * s)
    operator fun Vector3fc.times(s: Vector3fc) = Vector3f(x() * s.x(), y() * s.y(), z() * s.z())

    operator fun Vector3ic.plus(s: Vector3ic) = Vector3i(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3ic.minus(s: Vector3ic) = Vector3i(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3ic.times(s: Float) = Vector3f(x() * s, y() * s, z() * s)

    operator fun Vector3dc.plus(s: Vector3dc) = Vector3d(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3dc.minus(s: Vector3dc) = Vector3d(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3dc.times(s: Double) = Vector3d(x() * s, y() * s, z() * s)

    operator fun Vector3fc.plus(s: Vector3ic) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3ic.plus(s: Vector3fc) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3fc.minus(s: Vector3ic) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3ic.minus(s: Vector3fc) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())

    operator fun Vector4fc.minus(s: Vector4fc) = Vector4f(x() - s.x(), y() - s.y(), z() - s.z(), w() - s.w())
    operator fun Vector4fc.plus(s: Vector4fc) = Vector4f(x() + s.x(), y() + s.y(), z() + s.z(), w() + s.w())
    operator fun Vector4fc.plus(s: Float) = if (s == 0f) this else Vector4f(x() + s, y() + s, z() + s, w() + s)
    operator fun Vector4fc.times(s: Float) = Vector4f(x() * s, y() * s, z() * s, w() * s)
    operator fun Vector4fc.times(s: Vector4fc) = Vector4f(x() * s.x(), y() * s.y(), z() * s.z(), w() * s.w())

    fun Vector4fc.mulAlpha(m: Float, dst: Vector4f = Vector4f()): Vector4fc = dst.set(x(), y(), z(), w() * m)

    fun avg(a: Vector2fc, b: Vector2fc): Vector2f = Vector2f(a).add(b).mul(0.5f)
    fun avg(a: Vector2dc, b: Vector2dc): Vector2d = Vector2d(a).add(b).mul(0.5)
    fun avg(a: Vector3fc, b: Vector3fc): Vector3f = Vector3f(a).add(b).mul(0.5f)

    fun avg(a: Vector2fc, b: Vector2fc, c: Vector2fc) =
        Vector2f((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f)

    fun avg(a: Vector2dc, b: Vector2dc, c: Vector2dc) =
        Vector2d((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f)

    fun avg(a: Vector3fc, b: Vector3fc, c: Vector3fc) =
        Vector3f((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f, (a.z() + b.z() + c.z()) / 3f)

    fun avg(a: Vector3dc, b: Vector3dc, c: Vector3dc) =
        Vector3d((a.x() + b.x() + c.x()) / 3.0, (a.y() + b.y() + c.y()) / 3.0, (a.z() + b.z() + c.z()) / 3.0)

    fun Vector2fc.print(pts: List<Vector2fc>) = "${pts.indexOf(this)}"
    fun Vector2dc.print(pts: List<Vector2dc>) = "${pts.indexOf(this)}"

    fun getSideSign(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx0 = ax - px
        val dy0 = ay - py
        val dx1 = bx - px
        val dy1 = by - py
        return dx1 * dy0 - dy1 * dx0
    }

    fun getSideSign(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx0 = ax - px
        val dy0 = ay - py
        val dx1 = bx - px
        val dy1 = by - py
        return dx1 * dy0 - dy1 * dx0
    }

    fun Vector2fc.getSideSign(b: Vector2fc, c: Vector2fc): Float {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2dc.getSideSign(b: Vector2dc, c: Vector2dc): Double {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2fc.isInsideTriangle(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {
        val asX = x() - a.x()
        val asY = y() - a.y()
        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0
        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false
        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb
    }

    fun Vector2fc.isInsideTriangle2(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }


    fun Vector2d.isInsideTriangle(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {
        val asX = x() - a.x()
        val asY = y() - a.y()
        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0
        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false
        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb
    }

    fun Vector2dc.isInsideTriangle2(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        allowBackside: Boolean
    ): Pair<Vector3fc, Float>? {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, Vector3f())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return null
        val q = origin + direction * t
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return if (sum == 0 || (allowBackside && sum == 3)) q to t else null
    }

    fun rayTriangleIntersect(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float,
        allowBackside: Boolean
    ): Boolean {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, JomlPools.vec3f.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return false
        val q = origin + direction * t
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return sum == 0 || (allowBackside && sum == 3)
    }

    fun rayTriangleIntersect(
        origin: Vector3dc, direction: Vector3dc,
        a: Vector3dc, b: Vector3dc, c: Vector3dc,
        maxDistance: Double,
        allowBackside: Boolean
    ): Boolean {
        val n = subCross(a, b, c, JomlPools.vec3d.borrow())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0.0 || t >= maxDistance) return false
        val q = origin + direction * t
        var sum = 0
        if (subCrossDot(a, b, q, n) < 0.0) sum++
        if (subCrossDot(b, c, q, n) < 0.0) sum++
        if (subCrossDot(c, a, q, n) < 0.0) sum++
        return sum == 0 || (allowBackside && sum == 3)
    }

    fun Vector2fc.print() = "(${x()} ${y()})"
    fun Vector2dc.print() = "(${x()} ${y()})"
    fun Vector2ic.print() = "(${x()} ${y()})"
    fun Vector3fc.print() = "(${x()} ${y()} ${z()})"
    fun Vector3dc.print() = "(${x()} ${y()} ${z()})"
    fun Vector3ic.print() = "(${x()} ${y()} ${z()})"
    fun Vector4fc.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Vector4dc.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Vector4ic.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Quaternionf.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Quaterniond.print() = "(${x()} ${y()} ${z()} ${w()})"

    fun Vector2fc.toVector3d() = Vector2d(this)
    fun Vector2dc.toVector3f() = Vector2f(x().toFloat(), y().toFloat())
    fun Vector3fc.toVector3d() = Vector3d(this)
    fun Vector3dc.toVector3f(dst: Vector3f = Vector3f()): Vector3f =
        dst.set(x().toFloat(), y().toFloat(), z().toFloat())

    fun Vector4fc.toVector3d() = Vector4d(this)
    fun Vector4dc.toVector3f() = Vector4f(x().toFloat(), y().toFloat(), z().toFloat(), w().toFloat())

    fun Matrix4fc.print() = "" +
            "[(${get(0, 0)} ${get(1, 0)} ${get(2, 0)} ${get(3, 0)})\n" +
            " (${get(0, 1)} ${get(1, 1)} ${get(2, 1)} ${get(3, 1)})\n" +
            " (${get(0, 2)} ${get(1, 2)} ${get(2, 2)} ${get(3, 2)})\n" +
            " (${get(0, 3)} ${get(1, 3)} ${get(2, 3)} ${get(3, 3)})]"

    fun Matrix4x3f.print() = "" +
            "[(${m00()} ${m10()} ${m20()} ${m30()})\n" +
            " (${m01()} ${m11()} ${m21()} ${m31()})\n" +
            " (${m02()} ${m12()} ${m22()} ${m32()})]"

    fun Matrix4x3f.f2() = "" +
            "[(${m00().f2s()} ${m10().f2s()} ${m20().f2s()} ${m30().f2s()})\n" +
            " (${m01().f2s()} ${m11().f2s()} ${m21().f2s()} ${m31().f2s()})\n" +
            " (${m02().f2s()} ${m12().f2s()} ${m22().f2s()} ${m32().f2s()})]"

    fun Vector4fc.toVec3f(): Vector3f {
        val w = w()
        return Vector3f(x() / w, y() / w, z() / w)
    }

    fun Vector3f.is000() = x == 0f && y == 0f && z == 0f
    fun Vector3f.is111() = x == 1f && y == 1f && z == 1f
    fun Vector4f.is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    fun Vector3fc.is000() = x() == 0f && y() == 0f && z() == 0f
    fun Vector3fc.is111() = x() == 1f && y() == 1f && z() == 1f
    fun Vector4fc.is1111() = x() == 1f && y() == 1f && z() == 1f && w() == 1f

    fun Vector3fi(x: Int, y: Int, z: Int): Vector3f = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

    val Vector3f.yzx get() = Vector3f(y, z, x)
    val Vector3f.zxy get() = Vector3f(z, x, y)

    fun DoubleArray.toVec3() = Vector3f(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())


    fun Vector3i.cross(v: Vector3ic): Vector3i {
        val rx = (this.y * v.z() - this.z * v.y())
        val ry = (this.z * v.x() - this.x * v.z())
        val rz = (this.x * v.y() - this.y * v.x())
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun Vector3i.normalize(): Vector3i {
        val length = length()
        return Vector3i((x / length).roundToInt(), (y / length).roundToInt(), (z / length).roundToInt())
    }

    operator fun Vector3i.plus(second: Vector3f): Vector3f {
        return Vector3f(this).add(second)
    }

    fun findTangent(normal: Vector3f, dst: Vector3f = Vector3f()): Vector3f {
        return normal.findSecondAxis(dst)
    }

    fun findTangent(normal: Vector3d, dst: Vector3d = Vector3d()): Vector3d {
        return normal.findSecondAxis(dst)
    }

    fun Vector3f.roundToInt() = Vector3i(x.roundToInt(), y.roundToInt(), z.roundToInt())
    fun Vector3d.roundToInt() = Vector3i(x.roundToInt(), y.roundToInt(), z.roundToInt())
    fun Vector3d.floorToInt() =
        Vector3i(kotlin.math.floor(x).toInt(), kotlin.math.floor(y).toInt(), kotlin.math.floor(z).toInt())

    fun Vector3f.safeNormalize(length: Float): Vector3f {
        val f = length / length()
        if (!f.isFinite() || f == 0f) {
            set(0.0)
        } else {
            mul(f)
        }
        return this
    }

    fun Vector3d.safeNormalize(length: Double = 1.0): Vector3d {
        normalize(length)
        if (!isFinite) set(0.0)
        return this
    }

    fun Vector3f.setAxis(axis: Int, value: Float) {
        when (axis) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
        }
    }

    fun Vector3d.setAxis(axis: Int, value: Double): Vector3d {
        setComponent(axis, value)
        return this
    }


    /**
     * approximate line intersection
     * http://paulbourke.net/geometry/pointlineplane/calclineline.cs
     * */
    fun intersect(
        pos0: Vector3d, dir0: Vector3d,
        pos1: Vector3d, dir1: Vector3d,
        factor0: Double,
        factor1: Double,
        dst0: Vector3d, dst1: Vector3d
    ): Boolean {

        val p13x = pos0.x - pos1.x
        val p13y = pos0.y - pos1.y
        val p13z = pos0.z - pos1.z

        val d1321 = dir0.dot(p13x, p13y, p13z)
        val d1343 = dir1.dot(p13x, p13y, p13z)
        val d4321 = dir0.dot(dir1)
        val d2121 = dir0.lengthSquared()
        val d4343 = dir1.lengthSquared()
        val denominator = d2121 * d4343 - d4321 * d4321
        if (abs(denominator) < 1e-7) return false

        val numerator = d1343 * d4321 - d1321 * d4343
        val mua = numerator / denominator
        val mub = (d1343 + d4321 * mua) / d4343

        dir0.mulAdd(mua * factor0, pos0, dst0)
        dir1.mulAdd(mub * factor1, pos1, dst1)
        return true

    }

    fun Vector3f.findSecondAxis(dst: Vector3f = Vector3f()): Vector3f {
        val thirdAxis = if (abs(x) > abs(y)) dst.set(0f, 1f, 0f)
        else dst.set(1f, 0f, 0f)
        return cross(thirdAxis, dst).normalize()
    }

    fun Vector3d.findSecondAxis(dst: Vector3d = Vector3d()): Vector3d {
        val thirdAxis = if (abs(x) > abs(y)) dst.set(0.0, 1.0, 0.0)
        else dst.set(1.0, 0.0, 0.0)
        return cross(thirdAxis, dst).normalize()
    }

    fun Vector3f.findSystem(dstY: Vector3f = Vector3f(), dstZ: Vector3f = Vector3f()) {
        findSecondAxis(dstY)
        cross(dstY, dstZ).normalize()
    }

    fun Vector3d.findSystem(dstY: Vector3d = Vector3d(), dstZ: Vector3d = Vector3d()) {
        findSecondAxis(dstY)
        cross(dstY, dstZ).normalize()
    }

    fun Vector3f.addScaled(other: Vector3f, scale: Float): Vector3f {
        other.mulAdd(scale, this, this)
        return this
    }

    fun Vector3f.addSmoothly(other: Vector3f, scale: Float): Vector3f {
        mul(1f - scale)
        other.mulAdd(scale, this, this)
        return this
    }

    fun Vector2f.dot2(x: Float, y: Float) = this.x * x + this.y * y

    fun Vector3f.fract(dst: Vector3f = this): Vector3f = dst.set(Maths.fract(x), Maths.fract(y), Maths.fract(z))
    fun Vector3d.fract(dst: Vector3d = this): Vector3d = dst.set(Maths.fract(x), Maths.fract(y), Maths.fract(z))

    // missing from Joml :/
    fun Vector3d.rotate2(q: Quaternionf): Vector3d =
        rotate(JomlPools.quat4d.borrow().set(q))

    fun Vector3f.rotateInv(q: Quaternionf): Vector3f =
        rotate(JomlPools.quat4f.borrow().set(q).conjugate())

    fun Vector3f.rotateInv(q: Quaterniond): Vector3f =
        rotate(JomlPools.quat4f.borrow().set(q).conjugate())

    fun Vector3d.rotateInv(q: Quaternionf): Vector3d =
        rotate(JomlPools.quat4d.borrow().set(q).conjugate())

    fun Vector3d.rotateInv(q: Quaterniond): Vector3d =
        rotate(JomlPools.quat4d.borrow().set(q).conjugate())

    /**
     * converts this normal to a quaternion such that vec3(0,1,0).rot(q) is equal to this vector;
     * identical to Matrix3f(.., this, ..).getNormalizedRotation(dst)
     * */
    fun Vector3f.normalToQuaternion(dst: Quaternionf): Quaternionf {
        // uses ~ 28 ns/e on R5 2600
        val x = x
        val y = y
        val z = z
        if (x * x + z * z > 0.001f) {
            val v3 = JomlPools.vec3f
            val v0 = v3.create()
            val v2 = v3.create()
            v0.set(z, 0f, -x).normalize()
            v0.cross(this, v2)
            val v00 = v0.x
            val v11 = y
            val v22 = v2.z
            val diag = v00 + v11 + v22
            if (diag >= 0f) {
                dst.set(z - v2.y, v2.x - v0.z, v0.y - x, diag + 1f)
            } else if (v00 >= v11 && v00 >= v22) {
                dst.set(v00 - (v11 + v22) + 1f, x + v0.y, v0.z + v2.x, z - v2.y)
            } else if (v11 > v22) {
                dst.set(x + v0.y, v11 - (v22 + v00) + 1f, v2.y + z, v2.x - v0.z)
            } else {
                dst.set(v0.z + v2.x, v2.y + z, v22 - (v00 + v11) + 1f, v0.y - x)
            }
            v3.sub(2)
            return dst.normalize()
        } else if (y > 0f) { // up
            return dst.identity()
        } else { // down
            return dst.set(1f, 0f, 0f, 0f)
        }
    }

    fun Vector2fc.cross(other: Vector2fc): Float {
        return x() * other.y() - y() * other.x()
    }

}