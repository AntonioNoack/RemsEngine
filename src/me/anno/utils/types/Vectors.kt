package me.anno.utils.types

import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f2s
import me.anno.utils.types.Floats.f2x
import org.hsluv.HSLuvColorSpace
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

@Suppress("unused")
object Vectors {

    @JvmStatic
    fun avg(a: Vector2f, b: Vector2f): Vector2f = Vector2f(a).add(b).mul(0.5f)

    @JvmStatic
    fun avg(a: Vector2d, b: Vector2d): Vector2d = Vector2d(a).add(b).mul(0.5)

    @JvmStatic
    fun avg(a: Vector3f, b: Vector3f): Vector3f = Vector3f(a).add(b).mul(0.5f)

    @JvmStatic
    fun avg(a: Vector2f, b: Vector2f, c: Vector2f) =
        Vector2f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

    @JvmStatic
    fun avg(a: Vector2d, b: Vector2d, c: Vector2d) =
        Vector2d((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

    @JvmStatic
    fun avg(a: Vector3f, b: Vector3f, c: Vector3f) =
        Vector3f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f)

    @JvmStatic
    fun avg(a: Vector3d, b: Vector3d, c: Vector3d) =
        Vector3d((a.x + b.x + c.x) / 3.0, (a.y + b.y + c.y) / 3.0, (a.z + b.z + c.z) / 3.0)

    @JvmStatic
    fun Matrix4f.print() = "" +
            "[($m00 $m10 $m20 $m30)\n" +
            " ($m01 $m11 $m21 $m31)\n" +
            " ($m02 $m12 $m22 $m32)\n" +
            " ($m03 $m13 $m23 $m33)]"

    @JvmStatic
    fun Matrix4x3f.print() = "" +
            "[($m00 $m10 $m20 $m30)\n" +
            " ($m01 $m11 $m21 $m31)\n" +
            " ($m02 $m12 $m22 $m32)]"

    @JvmStatic
    fun Matrix4x3f.f2() = "" +
            "[(${m00.f2s()} ${m10.f2s()} ${m20.f2s()} ${m30.f2s()})\n" +
            " (${m01.f2s()} ${m11.f2s()} ${m21.f2s()} ${m31.f2s()})\n" +
            " (${m02.f2s()} ${m12.f2s()} ${m22.f2s()} ${m32.f2s()})]"

    @JvmStatic
    fun Matrix4x3f.f2x() = "" +
            "[(${m00.f2x()} ${m10.f2x()} ${m20.f2x()} ${m30.f2x()})\n" +
            " (${m01.f2x()} ${m11.f2x()} ${m21.f2x()} ${m31.f2x()})\n" +
            " (${m02.f2x()} ${m12.f2x()} ${m22.f2x()} ${m32.f2x()})]"

    @JvmStatic
    fun findTangent(normal: Vector3f, dst: Vector3f = Vector3f()): Vector3f {
        return normal.findSecondAxis(dst)
    }

    @JvmStatic
    fun findTangent(normal: Vector3d, dst: Vector3d = Vector3d()): Vector3d {
        return normal.findSecondAxis(dst)
    }

    /**
     * approximate line intersection
     * http://paulbourke.net/geometry/pointlineplane/calclineline.cs
     * */
    @JvmStatic
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

    @JvmStatic
    fun Vector3f.addScaled(other: Vector3f, scale: Float): Vector3f {
        other.mulAdd(scale, this, this)
        return this
    }

    /**
     * converts this normal to a quaternion such that vec3(0,1,0).rot(q) is equal to this vector;
     * identical to Matrix3f(.., this, ..).getNormalizedRotation(dst)
     * */
    @JvmStatic
    fun Vector3f.normalToQuaternionY(dst: Quaternionf = Quaternionf()): Quaternionf {
        return normalToQuaternionY(x, y, z, dst)
    }

    @JvmStatic
    fun normalToQuaternionY(x: Float, y: Float, z: Float, dst: Quaternionf = Quaternionf()): Quaternionf {
        // todo this works perfectly, but the y-angle shouldn't change :/
        // uses ~ 28 ns/e on R5 2600
        if (x * x + z * z > 0.001f) {
            val v3 = JomlPools.vec3f
            val v0 = v3.create()
            val v2 = v3.create()
            v0.set(z, 0f, -x).normalize()
            v0.cross(x, y, z, v2)
            val v00 = v0.x
            val v22 = v2.z
            val diag = v00 + y + v22
            if (diag >= 0f) {
                dst.set(z - v2.y, v2.x - v0.z, v0.y - x, diag + 1f)
            } else if (v00 >= y && v00 >= v22) {
                dst.set(v00 - (y + v22) + 1f, x + v0.y, v0.z + v2.x, z - v2.y)
            } else if (y > v22) {
                dst.set(x + v0.y, y - (v22 + v00) + 1f, v2.y + z, v2.x - v0.z)
            } else {
                dst.set(v0.z + v2.x, v2.y + z, v22 - (v00 + y) + 1f, v0.y - x)
            }
            v3.sub(2)
            return dst.normalize()
        } else if (y > 0f) { // up
            return dst.identity()
        } else { // down
            return dst.set(1f, 0f, 0f, 0f)
        }
    }

    /**
     * converts this normal to a quaternion such that vec3(0,1,0).rot(q) is equal to this vector;
     * identical to Matrix3f(.., this, ..).getNormalizedRotation(dst)
     * */
    @JvmStatic
    fun Vector3f.normalToQuaternionY2(dst: Quaternionf): Quaternionf {
        normalToQuaternionY(dst)
        // todo better formula?
        val test = JomlPools.vec3f.borrow()
        for (i in 0 until 4) {
            dst.transform(test.set(1f, 0f, 0f))
            rotateY(atan2(test.z, test.x))
        }
        return dst
    }

    @JvmStatic
    fun crossLength(pos: FloatArray, ai: Int, bi: Int, ci: Int): Float {
        val ax = pos[ai]
        val ay = pos[ai + 1]
        val az = pos[ai + 2]
        return crossLength(
            pos[bi] - ax, pos[bi + 1] - ay, pos[bi + 2] - az,
            pos[ci] - ax, pos[ci + 1] - ay, pos[ci + 2] - az
        )
    }

    @JvmStatic
    fun crossLength(a: Vector3f, b: Vector3f, c: Vector3f): Float {
        return crossLength(
            b.x - a.x, b.y - a.y, b.z - a.z,
            c.x - a.x, c.y - a.y, c.z - a.z
        )
    }

    @JvmStatic
    fun crossLength(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        return sqrt(crossLengthSq(ax, ay, az, bx, by, bz))
    }

    @JvmStatic
    fun crossLengthSq(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        return cx * cx + cy * cy + cz * cz
    }

    fun getMaxComponent(x: Double, y: Double): Int = if (y >= x) 1 else 0
    fun getMaxComponent(x: Double, y: Double, z: Double): Int = if (z >= max(x, y)) 2 else getMaxComponent(x, y)
    fun getMaxComponent(x: Double, y: Double, z: Double, w: Double): Int =
        if (w >= max(x, max(y, z))) 3 else getMaxComponent(x, y)

    /**
     * transforms color from sRGB (standard) to linear space (light calculations)
     * */
    @JvmStatic
    fun Vector3f.toLinear(dst: Vector3f = this): Vector3f {
        return dst.set(
            HSLuvColorSpace.toLinear(x),
            HSLuvColorSpace.toLinear(y),
            HSLuvColorSpace.toLinear(z)
        )
    }

    /**
     * transforms color from linear space (light calculations) to sRGB (standard)
     * */
    @JvmStatic
    fun Vector3f.toSRGB(dst: Vector3f = this): Vector3f {
        return dst.set(
            HSLuvColorSpace.toSRGB(x),
            HSLuvColorSpace.toSRGB(y),
            HSLuvColorSpace.toSRGB(z)
        )
    }
}