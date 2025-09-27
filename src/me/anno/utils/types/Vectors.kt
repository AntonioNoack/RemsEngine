package me.anno.utils.types

import me.anno.utils.types.Floats.f2s
import me.anno.utils.types.Floats.f2x
import me.anno.utils.types.Triangles.getParallelogramArea
import org.hsluv.HSLuvColorSpace
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.sqrt

@Suppress("unused")
object Vectors {

    @JvmStatic
    fun avg(a: Vector2f, b: Vector2f): Vector2f = (a + b).mul(0.5f)

    @JvmStatic
    fun avg(a: Vector2d, b: Vector2d): Vector2d = (a + b).mul(0.5)

    @JvmStatic
    fun avg(a: Vector3f, b: Vector3f): Vector3f = (a + b).mul(0.5f)

    @JvmStatic
    fun avg(a: Vector3d, b: Vector3d): Vector3d = (a + b).mul(0.5)

    @JvmStatic
    fun avg(a: Vector2f, b: Vector2f, c: Vector2f) = (a + b).add(c).div(3f)

    @JvmStatic
    fun avg(a: Vector2d, b: Vector2d, c: Vector2d) = (a + b).add(c).div(3.0)

    @JvmStatic
    fun avg(a: Vector3f, b: Vector3f, c: Vector3f) = (a + b).add(c).div(3f)

    @JvmStatic
    fun avg(a: Vector3d, b: Vector3d, c: Vector3d) = (a + b).add(c).div(3.0)

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
    fun normalToQuaternionY(x: Double, y: Double, z: Double, dst: Quaterniond = Quaterniond()): Quaterniond {
        return dst.rotateTo(0.0, 1.0, 0.0, x, y, z)
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
        return getParallelogramArea(a, b, c)
    }

    @JvmStatic
    fun crossLength(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        return sqrt(crossLengthSq(ax, ay, az, bx, by, bz))
    }

    @JvmStatic
    fun crossLength(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double): Double {
        return sqrt(crossLengthSq(ax, ay, az, bx, by, bz))
    }

    @JvmStatic
    fun crossLengthSq(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        return cx * cx + cy * cy + cz * cz
    }

    @JvmStatic
    fun crossLengthSq(ax: Double, ay: Double, az: Double, bx: Double, by: Double, bz: Double): Double {
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val cz = ax * by - ay * bx
        return cx * cx + cy * cy + cz * cz
    }

    @JvmStatic
    fun cross(ax: Float, ay: Float, bx: Float, by: Float): Float {
        return ax * by - ay * bx
    }

    @JvmStatic
    fun cross(ax: Double, ay: Double, bx: Double, by: Double): Double {
        return ax * by - ay * bx
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