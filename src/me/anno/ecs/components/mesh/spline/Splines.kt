package me.anno.ecs.components.mesh.spline

import me.anno.maths.Maths
import me.anno.maths.Maths.mix
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Splines {

    /**
     * posNormals: mixed positions and normals, pnpn-pnpn-pnpn-pnpn <br>
     * returns list of positions
     * */
    fun generateSplineLinePair(pns: Array<Vector3d>, ptsPerRadiant: Double, close: Boolean): List<Vector3d> {

        val result = ArrayList<Vector3d>()

        val p1a = Vector3d()
        val p1b = Vector3d()

        val p2a = Vector3d()
        val p2b = Vector3d()

        val p3b = Vector3d()
        val p3a = Vector3d()

        var p0a = pns[0]
        var n0a = pns[1]
        var p0b = pns[2]
        var n0b = pns[3]

        var end = pns.size
        if (!close) end -= 4
        for (i in 0 until end step 4) {

            val p4a = pns[(i + 4) % pns.size]
            val n4a = pns[(i + 5) % pns.size]
            val p4b = pns[(i + 6) % pns.size]
            val n4b = pns[(i + 7) % pns.size]

            result.add(p0a)
            result.add(p0b)

            getIntermediates(p0a, n0a, p4a, n4a, p1a, p3a)
            getIntermediates(p0b, n0b, p4b, n4b, p1b, p3b)

            interpolate(p0a, p1a, p3a, p4a, 0.5, p2a)
            interpolate(p0b, p1b, p3b, p4b, 0.5, p2b)

            // calculate using curviness, how many pts we need
            val angle = angle(p0a, p1a, p2a) + angle(p0b, p1b, p2b) +
                    angle(p1a, p2a, p3a) + angle(p1b, p2b, p3b) +
                    angle(p2a, p3a, p4a) + angle(p2b, p3b, p4b)
            val stopsF = (angle * ptsPerRadiant * 0.5)
            if (stopsF.isFinite() && stopsF >= 0.5f) {
                val stops = stopsF.roundToInt()
                for (j in 1 until stops) {
                    val t = j.toDouble() / stops
                    result.add(interpolate(p0a, p1a, p3a, p4a, t))
                    result.add(interpolate(p0b, p1b, p3b, p4b, t))
                }
            }

            p0a = p4a
            n0a = n4a
            p0b = p4b
            n0b = n4b
        }

        result.add(pns[pns.size - 4])
        result.add(pns[pns.size - 2])

        return result
    }

    fun angle(p0: Vector3d, p1: Vector3d, p2: Vector3d): Double {
        val ax = p1.x - p0.x
        val ay = p1.y - p0.y
        val az = p1.z - p0.z
        val bx = p2.x - p1.x
        val by = p2.y - p1.y
        val bz = p2.z - p1.z
        val cross = Maths.sq(
            ay * bz - az * by,
            az * bx + ax * bz,
            ax * by - ay * bx
        )
        val al = ax * ax + ay * ay + az * az
        val bl = bx * bx + by * by + bz * bz
        return asin(sqrt(cross / max(1e-16, al * bl)))
    }

    fun interpolate(
        p0: Vector3d,
        p1: Vector3d,
        p2: Vector3d,
        p3: Vector3d,
        t: Double,
        dst: Vector3d = Vector3d()
    ): Vector3d {
        // 1 3 3 1
        val s = 1.0 - t
        val ss = s * s
        val tt = t * t
        dst.set(p0).mul(ss * s)
        p1.mulAdd(3.0 * ss * t, dst, dst)
        p2.mulAdd(3.0 * s * tt, dst, dst)
        p3.mulAdd(t * tt, dst, dst)
        return dst
    }

    fun getIntermediates(p0: Vector3d, d0: Vector3d, p1: Vector3d, d1: Vector3d, dst0: Vector3d, dst1: Vector3d) {
        // calculate the intermediate point(s)
        // kept simple
        val extend = p1.distance(p0) * 0.5
        d0.mulAdd(+extend, p0, dst0)
        d1.mulAdd(-extend, p1, dst1)
    }

    /**
     * given two lines, it will compute a triangulated surface between them,
     * such that they are ideally distributed in most cases
     * */
    fun generateSurface(l0: List<Vector3f>, l1: List<Vector3f>): IntArray {
        val offset = l0.size
        val result = IntArray((l0.size + l1.size - 2) * 3)
        var out = 0
        var i0 = 1
        var i1 = 1
        while (i0 < l0.size && i1 < l1.size) {
            val n00 = l0[i0 - 1]
            val n10 = l1[i1 - 1]
            val n01 = l0[i0]
            val n11 = l1[i1]
            result[out++] = i0 - 1
            result[out++] = i1 - 1 + offset
            // compute the general direction
            val dx = (n01.x + n11.x) - (n00.x + n10.x)
            val dy = (n01.y + n11.y) - (n00.y + n10.y)
            val dz = (n01.z + n11.z) - (n00.z + n10.z)
            // compute, which point comes first
            result[out++] = if (
                n00.dot(dx, dy, dz) + n01.dot(dx, dy, dz) <
                n10.dot(dx, dy, dz) + n11.dot(dx, dy, dz)
            ) {
                i0++
            } else {
                i1++ + offset
            }
        }
        if (i0 < l0.size) {
            while (i0 < l0.size) {
                result[out++] = i0 - 1
                result[out++] = i1 - 1 + offset
                result[out++] = i0++
            }
        } else {
            while (i1 < l1.size) {
                result[out++] = i0 - 1
                result[out++] = i1 - 1 + offset
                result[out++] = i1++ + offset
            }
        }
        return result
    }

    fun generateCurve(a0: Float, a1: Float, n: Int): Array<Vector2f> {
        return when {
            n < 0 -> throw IllegalArgumentException("n must be >= 0, got $n")
            n == 0 -> emptyArray()
            n == 1 -> {
                val angle = (a0 + a1) * 0.5f
                arrayOf(Vector2f(cos(angle), sin(angle)))
            }
            else -> {
                val div = 1f / (n - 1f)
                Array(n) {
                    val angle = mix(a0, a1, it * div)
                    Vector2f(cos(angle), sin(angle))
                }
            }
        }
    }
}