package me.anno.utils.types

import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f2s
import org.joml.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Suppress("unused")
object Vectors {

    /**
     * the following functions allow for comfortable debugging with vectors;
     * they shouldn't be used in production to keep allocations at a minimum
     * */

    operator fun Vector2f.plus(s: Vector2f) = Vector2f(x + s.x, y + s.y)
    operator fun Vector2f.minus(s: Vector2f) = Vector2f(x - s.x, y - s.y)
    operator fun Vector2f.times(f: Float) = Vector2f(x * f, y * f)
    operator fun Vector2f.times(s: Vector2f) = Vector2f(x * s.x, y * s.y)

    operator fun Vector2d.plus(s: Vector2d) = Vector2d(x + s.x, y + s.y)
    operator fun Vector2d.minus(s: Vector2d) = Vector2d(x - s.x, y - s.y)
    operator fun Vector2d.times(f: Double) = Vector2d(x * f, y * f)

    operator fun Vector3f.plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3f.minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun Vector3f.times(s: Float) = Vector3f(x * s, y * s, z * s)
    operator fun Vector3f.times(s: Vector3f) = Vector3f(x * s.x, y * s.y, z * s.z)

    operator fun Vector3i.plus(s: Vector3i) = Vector3i(x + s.x, y + s.y, z + s.z)
    operator fun Vector3i.minus(s: Vector3i) = Vector3i(x - s.x, y - s.y, z - s.z)
    operator fun Vector3i.times(s: Float) = Vector3f(x * s, y * s, z * s)

    operator fun Vector3d.plus(s: Vector3d) = Vector3d(x + s.x, y + s.y, z + s.z)
    operator fun Vector3d.minus(s: Vector3d) = Vector3d(x - s.x, y - s.y, z - s.z)
    operator fun Vector3d.times(s: Double) = Vector3d(x * s, y * s, z * s)

    operator fun Vector3f.plus(s: Vector3i) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3i.plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3f.minus(s: Vector3i) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun Vector3i.minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)

    operator fun Vector4f.minus(s: Vector4f) = Vector4f(x - s.x, y - s.y, z - s.z, w - s.w)
    operator fun Vector4f.plus(s: Vector4f) = Vector4f(x + s.x, y + s.y, z + s.z, w + s.w)
    operator fun Vector4f.plus(s: Float) = if (s == 0f) this else Vector4f(x + s, y + s, z + s, w + s)
    operator fun Vector4f.times(s: Float) = Vector4f(x * s, y * s, z * s, w * s)
    operator fun Vector4f.times(s: Vector4f) = Vector4f(x * s.x, y * s.y, z * s.z, w * s.w)

    fun Vector4f.mulAlpha(m: Float, dst: Vector4f = Vector4f()): Vector4f = dst.set(x, y, z, w * m)

    fun avg(a: Vector2f, b: Vector2f): Vector2f = Vector2f(a).add(b).mul(0.5f)
    fun avg(a: Vector2d, b: Vector2d): Vector2d = Vector2d(a).add(b).mul(0.5)
    fun avg(a: Vector3f, b: Vector3f): Vector3f = Vector3f(a).add(b).mul(0.5f)

    fun avg(a: Vector2f, b: Vector2f, c: Vector2f) =
        Vector2f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

    fun avg(a: Vector2d, b: Vector2d, c: Vector2d) =
        Vector2d((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

    fun avg(a: Vector3f, b: Vector3f, c: Vector3f) =
        Vector3f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f)

    fun avg(a: Vector3d, b: Vector3d, c: Vector3d) =
        Vector3d((a.x + b.x + c.x) / 3.0, (a.y + b.y + c.y) / 3.0, (a.z + b.z + c.z) / 3.0)

    fun Vector2f.print(pts: List<Vector2f>) = "${pts.indexOf(this)}"
    fun Vector2d.print(pts: List<Vector2d>) = "${pts.indexOf(this)}"

    fun Vector2f.print() = "($x $y)"
    fun Vector2d.print() = "($x $y)"
    fun Vector2i.print() = "($x $y)"
    fun Vector3f.print() = "($x $y $z)"
    fun Vector3d.print() = "($x $y $z)"
    fun Vector3i.print() = "($x $y $z)"
    fun Vector4f.print() = "($x $y $z $w)"
    fun Vector4d.print() = "($x $y $z $w)"
    fun Vector4i.print() = "($x $y $z $w)"
    fun Quaternionf.print() = "($x $y $z $w)"
    fun Quaterniond.print() = "($x $y $z $w)"

    fun Vector2f.toVector3d() = Vector2d(this)
    fun Vector2d.toVector3f() = Vector2f(x.toFloat(), y.toFloat())
    fun Vector3f.toVector3d() = Vector3d(this)
    fun Vector3d.toVector3f(dst: Vector3f = Vector3f()): Vector3f =
        dst.set(x.toFloat(), y.toFloat(), z.toFloat())

    fun Vector4f.toVector3d() = Vector4d(this)
    fun Vector4d.toVector3f() = Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun Matrix4f.print() = "" +
            "[($m00 $m10 $m20 $m30)\n" +
            " ($m01 $m11 $m21 $m31)\n" +
            " ($m02 $m12 $m22 $m32)\n" +
            " ($m03 $m13 $m23 $m33)]"

    fun Matrix4x3f.print() = "" +
            "[($m00 $m10 $m20 $m30)\n" +
            " ($m01 $m11 $m21 $m31)\n" +
            " ($m02 $m12 $m22 $m32)]"

    fun Matrix4x3f.f2() = "" +
            "[(${m00.f2s()} ${m10.f2s()} ${m20.f2s()} ${m30.f2s()})\n" +
            " (${m01.f2s()} ${m11.f2s()} ${m21.f2s()} ${m31.f2s()})\n" +
            " (${m02.f2s()} ${m12.f2s()} ${m22.f2s()} ${m32.f2s()})]"

    fun Vector4f.toVec3f(): Vector3f {
        val w = w
        return Vector3f(x / w, y / w, z / w)
    }

    fun Vector3f.is000() = x == 0f && y == 0f && z == 0f
    fun Vector3f.is111() = x == 1f && y == 1f && z == 1f
    fun Vector4f.is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    val Vector3f.yzx get() = Vector3f(y, z, x)
    val Vector3f.zxy get() = Vector3f(z, x, y)

    fun Vector3f.get2(dst: FloatArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
    }

    fun Vector3d.get2(dst: DoubleArray, i: Int) {
        dst[i] = x
        dst[i + 1] = y
        dst[i + 2] = z
    }

    fun Vector3f.set2(src: FloatArray, ai: Int) {
        set(src[ai], src[ai + 1], src[ai + 2])
    }

    fun Vector3d.set2(src: DoubleArray, ai: Int) {
        set(src[ai], src[ai + 1], src[ai + 2])
    }

    fun Vector3d.set2(src: FloatArray, ai: Int) {
        set(src[ai].toDouble(), src[ai + 1].toDouble(), src[ai + 2].toDouble())
    }

    fun DoubleArray.toVec3f(offset: Int = 0) =
        Vector3f(this[offset].toFloat(), this[offset + 1].toFloat(), this[offset + 2].toFloat())

    fun Vector3i.cross(v: Vector3i): Vector3i {
        val rx = y * v.z - z * v.y
        val ry = z * v.x - x * v.z
        val rz = x * v.y - y * v.x
        this.x = rx
        this.y = ry
        this.z = rz
        return this
    }

    fun Vector3i.normalize(): Vector3i {
        val length = length()
        return Vector3i((x / length).roundToInt(), (y / length).roundToInt(), (z / length).roundToInt())
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

    fun Vector3f.safeNormalize(length: Float = 1f): Vector3f {
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

    fun Vector2f.cross(other: Vector2f): Float {
        return x * other.y - y * other.x
    }

    fun Vector2f.mulAdd(f: Float, b: Vector2f, dst: Vector2f): Vector2f {
        return dst.set(x * f + b.x, y * f + b.y)
    }

    fun crossLength(pos: FloatArray, ai: Int, bi: Int, ci: Int): Float {
        val ax = pos[ai]
        val ay = pos[ai + 1]
        val az = pos[ai + 2]
        return crossLength(
            pos[bi] - ax, pos[bi + 1] - ay, pos[bi + 2] - az,
            pos[ci] - ax, pos[ci + 1] - ay, pos[ci + 2] - az
        )
    }

    fun crossLength(a: Vector3f, b: Vector3f, c: Vector3f): Float {
        return crossLength(
            b.x - a.x, b.y - a.y, b.z - a.z,
            c.x - a.x, c.y - a.y, c.z - a.z
        )
    }

    fun crossLength(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        return sqrt(cx * cx + cy * cy + cz * cz)
    }

}