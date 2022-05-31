package org.hsluv

import me.anno.utils.hpc.ThreadLocal2
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

/**
 * HSL, XYZ and HSLuv conversion from https://github.com/hsluv/hsluv-java
 * converted to Kotlin & made to use JOML vectors
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object HSLuvColorSpace {

    private val xyz2rgb = doubleArrayOf(
        3.240969941904521, -1.537383177570093, -0.498610760293,
        -0.96924363628087, 1.87596750150772, 0.041555057407175,
        0.055630079696993, -0.20397695888897, 1.056971514242878,
    )

    private val xyz2rgb2 = floatArrayOf(
        3.24097f, -1.5373832f, -0.49861076f,
        -0.96924365f, 1.8759675f, 0.04155506f,
        0.05563008f, -0.20397696f, 1.0569715f,
    )

    private val rgb2xyz = doubleArrayOf(
        0.41239079926595, 0.35758433938387, 0.18048078840183,
        0.21263900587151, 0.71516867876775, 0.072192315360733,
        0.019330818715591, 0.11919477979462, 0.95053215224966,
    )

    private const val refY = 1.0

    private const val refU = 0.19783000664283
    private const val refV = 0.46831999493879

    private const val kappa = 903.2962962
    private const val epsilon = 0.0088564516

    private val threadLocalBounds = ThreadLocal2 { Array(6) { Vector2d() } }

    private fun getBounds(l: Double, limit: Int): Array<Vector2d> {

        val result = threadLocalBounds.get()

        val sub1 = (l + 16.0).pow(3.0) / 1560896.0
        val sub2 = if (sub1 > epsilon) sub1 else l / kappa

        var index = 0
        for (c in 0 until 3) {

            val m1 = xyz2rgb[c * 3]
            val m2 = xyz2rgb[c * 3 + 1]
            val m3 = xyz2rgb[c * 3 + 2]

            for (t in 0 until 2) {
                val top1 = (284517 * m1 - 94839 * m3) * sub2
                val top2 = (838422 * m3 + 769860 * m2 + 731718 * m1) * l * sub2 - 769860 * t * l
                val bottom = (632260 * m3 - 126452 * m2) * sub2 + 126452 * t
                result[index++].set(top1 / bottom, top2 / bottom)
                if (index >= limit) return result
            }
        }

        return result

    }

    private fun intersectLineLine(lineAx: Double, lineAy: Double, lineBx: Double): Double {
        // lineB.y is always 0
        return lineAy / (lineBx - lineAx)
    }

    private fun lengthOfRayUntilIntersect(theta: Double, line: Vector2d): Double {
        return line.y / (sin(theta) - line.x * cos(theta))
    }

    private fun maxSafeChromaForL(l: Double): Double {

        val bounds = getBounds(l, 2)
        var min = Double.MAX_VALUE

        for (i in 0 until 2) {// why only the first two bounds? because they must belong to l

            val line0 = bounds[i]
            val m1 = line0.x
            val b1 = line0.y

            val x = intersectLineLine(m1, b1, -1.0 / m1)
            val length = hypot(x, b1 + x * m1)

            min = min(min, length)

        }

        return min
    }

    private fun maxChromaForLH(l: Double, h: Double): Double {
        val hRadians = h / 360 * Math.PI * 2

        val bounds = getBounds(l, 6)
        var min = Double.MAX_VALUE

        for (i in 0 until 6) {
            val bound = bounds[i]
            val length = lengthOfRayUntilIntersect(hRadians, bound)
            if (length >= 0.0) {
                min = min(min, length)
            }
        }

        return min
    }

    private fun dotProduct(a: DoubleArray, aOffset: Int, b: Vector3d): Double {
        return b.dot(a[aOffset], a[aOffset + 1], a[aOffset + 2])
    }

    private fun dotProduct(a: FloatArray, aOffset: Int, b: Vector3f): Float {
        return b.dot(a[aOffset], a[aOffset + 1], a[aOffset + 2])
    }

    fun fromLinear(c: Double): Double {
        return if (c <= 0.0031308) {
            12.92 * c
        } else {
            1.055 * c.pow(1.0 / 2.4) - 0.055
        }
    }

    fun fromLinear(c: Float): Float {
        return if (c <= 0.0031308f) {
            12.92f * c
        } else {
            1.055f * c.pow(1f / 2.4f) - 0.055f
        }
    }

    fun toLinear(c: Double): Double {
        return if (c > 0.04045) {
            ((c + 0.055) / (1 + 0.055)).pow(2.4)
        } else {
            c / 12.92
        }
    }

    fun xyzToRgb(src: Vector3d, dst: Vector3d = src): Vector3d {
        return dst.set(
            fromLinear(dotProduct(xyz2rgb, 0, src)),
            fromLinear(dotProduct(xyz2rgb, 3, src)),
            fromLinear(dotProduct(xyz2rgb, 6, src)),
        )
    }

    fun xyzToRgb(src: Vector3f, dst: Vector3f = src): Vector3f {
        return dst.set(
            fromLinear(dotProduct(xyz2rgb2, 0, src)),
            fromLinear(dotProduct(xyz2rgb2, 3, src)),
            fromLinear(dotProduct(xyz2rgb2, 6, src)),
        )
    }

    fun rgbToXyz(src: Vector3d, dst: Vector3d = src): Vector3d {
        dst.set(
            toLinear(src.x),
            toLinear(src.y),
            toLinear(src.z),
        )
        return dst.set(
            dotProduct(rgb2xyz, 0, dst),
            dotProduct(rgb2xyz, 3, dst),
            dotProduct(rgb2xyz, 6, dst),
        )
    }

    private fun yToL(y: Double) = if (y <= epsilon) {
        (y / refY) * kappa
    } else {
        116.0 * (y / refY).pow(1.0 / 3.0) - 16.0
    }

    private fun lToY(l: Double) = if (l <= 8.0) {
        refY * l / kappa
    } else {
        refY * ((l + 16.0) / 116.0).pow(3.0)
    }

    fun xyzToLuv(src: Vector3d, dst: Vector3d = src): Vector3d {

        val x = src.x
        val y = src.y
        val z = src.z

        val varU = (4.0 * x) / (x + (15.0 * y) + (3.0 * z))
        val varV = (9.0 * y) / (x + (15.0 * y) + (3.0 * z))

        val l = yToL(y)

        if (l == 0.0) {
            return dst.set(0.0)
        }

        val u = 13.0 * l * (varU - refU)
        val v = 13.0 * l * (varV - refV)

        return dst.set(l, u, v)

    }

    fun luvToXyz(src: Vector3d, dst: Vector3d = src): Vector3d {
        val l = src.x
        val u = src.y
        val v = src.z

        if (l == 0.0) {
            return dst.set(0.0)
        }

        val varU = u / (13.0 * l) + refU
        val varV = v / (13.0 * l) + refV

        val y = lToY(l)
        val x = 0 - (9.0 * y * varU) / ((varU - 4.0) * varV - varU * varV)
        val z = (9.0 * y - (15.0 * varV * y) - (varV * x)) / (3.0 * varV)

        return dst.set(x, y, z)
    }

    fun luvToLch(src: Vector3d, dst: Vector3d = src): Vector3d {

        val l = src.x
        val u = src.y
        val v = src.z

        val c = sqrt(u * u + v * v)
        var h: Double

        if (c < 0.00000001) {

            h = 0.0

        } else {

            h = StrictMath.toDegrees(atan2(v, u))

            if (h < 0.0) {
                h += 360.0
            }

        }

        return dst.set(l, c, h)

    }

    fun lchToLuv(src: Vector3d, dst: Vector3d = src): Vector3d {

        val l = src.x
        val c = src.y
        val h = src.z

        val hRadians = StrictMath.toRadians(h)
        val u = cos(hRadians) * c
        val v = sin(hRadians) * c

        return dst.set(l, u, v)

    }

    fun hsluvToLch(src: Vector3d, dst: Vector3d = src): Vector3d {

        val h = src.x
        val s = src.y
        val l = src.z

        if (l > 99.9999999) {
            return dst.set(100.0, 0.0, h)
        }

        if (l < 0.00000001) {
            return dst.set(0.0, 0.0, h)
        }

        val max = maxChromaForLH(l, h)
        val c = max * 0.01 * s

        return dst.set(l, c, h)

    }

    fun lchToHsluv(src: Vector3d, dst: Vector3d): Vector3d {
        val l = src.x
        val c = src.y
        val h = src.z

        if (l > 99.9999999) {
            return dst.set(h, 0.0, 100.0)
        }

        if (l < 0.00000001) {
            return dst.set(h, 0.0, 0.0)
        }

        val max = maxChromaForLH(l, h)
        val s = c / max * 100.0

        return dst.set(h, s, l)
    }

    fun hpluvToLch(src: Vector3d, dst: Vector3d = src): Vector3d {
        val h = src.x
        val s = src.y
        val l = src.z

        if (l > 99.9999999) {
            return dst.set(100.0, 0.0, h)
        }

        if (l < 0.00000001) {
            return dst.set(0.0, 0.0, h)
        }

        val max = maxSafeChromaForL(l)
        val c = max / 100 * s

        return dst.set(l, c, h)
    }

    fun lchToHpluv(src: Vector3d, dst: Vector3d = src): Vector3d {
        val l = src.x
        val c = src.y
        val h = src.z

        if (l > 99.9999999) {
            return dst.set(h, 0.0, 100.0)
        }

        if (l < 0.00000001) {
            return dst.set(h, 0.0, 0.0)
        }

        val max = maxSafeChromaForL(l)
        val s = c / max * 100.0

        return dst.set(h, s, l)
    }

    fun lchToRgb(src: Vector3d, dst: Vector3d = src) = xyzToRgb(luvToXyz(lchToLuv(src), dst), dst)
    fun rgbToLch(src: Vector3d, dst: Vector3d = src) = luvToLch(xyzToLuv(rgbToXyz(src, dst), dst), dst)
    fun hsluvToRgb(src: Vector3d, dst: Vector3d = src) = lchToRgb(hsluvToLch(src, dst), dst)
    fun rgbToHsluv(src: Vector3d, dst: Vector3d = src) = lchToHsluv(rgbToLch(src, dst), dst)
    fun hpluvToRgb(src: Vector3d, dst: Vector3d = src) = lchToRgb(hpluvToLch(src, dst), dst)
    fun rgbToHpluv(src: Vector3d, dst: Vector3d = src) = lchToHpluv(rgbToLch(src, dst), dst)

}