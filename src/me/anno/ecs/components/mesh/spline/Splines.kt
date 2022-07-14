package me.anno.ecs.components.mesh.spline

import me.anno.maths.Maths
import me.anno.maths.Maths.mix
import me.anno.utils.types.Vectors
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

object Splines {

    /**
     * posNormals: mixed positions and normals, pn-pn-pn-pn
     * */
    fun generateSplineLine(posNormals: List<Vector3d>, ptsPerRadiant: Double): List<Vector3d> {

        val result = ArrayList<Vector3d>()

        val p1 = Vector3d()
        val p2 = Vector3d()
        for (i in 2 until posNormals.size step 2) {

            val p0 = posNormals[i - 2]
            val n0 = posNormals[i - 1]
            val p3 = posNormals[i]
            val n3 = posNormals[i + 1]

            getIntermediates(p0, n0, p3, n3, p1, p2)

            // calculate using curviness, how many pts we need
            val angle0 = angle(p0, p1, p2)
            val angle1 = angle(p1, p2, p3)
            val stops = Maths.max(1, ((angle0 + angle1) * ptsPerRadiant).roundToInt())

            result.add(p0)

            for (j in 1 until stops) {
                val t = j.toDouble() / stops
                result.add(interpolate(p0, p1, p2, p3, t))
            }

        }

        result.add(posNormals[posNormals.size - 1])
        return result

    }

    /**
     * posNormals: mixed positions and normals, pnpn-pnpn-pnpn-pnpn <br>
     * returns list of positions
     * */
    fun generateSplineLinePair(posNormals: Array<Vector3d>, ptsPerRadiant: Double): List<Vector3d> {

        val result = ArrayList<Vector3d>()

        val p1a = Vector3d()
        val p2a = Vector3d()
        val p1b = Vector3d()
        val p2b = Vector3d()
        for (i in 4 until posNormals.size step 4) {

            val p0a = posNormals[i - 4]
            val n0a = posNormals[i - 3]
            val p3a = posNormals[i]
            val n3a = posNormals[i + 1]
            val p0b = posNormals[i - 2]
            val n0b = posNormals[i - 1]
            val p3b = posNormals[i + 2]
            val n3b = posNormals[i + 3]

            getIntermediates(p0a, n0a, p3a, n3a, p1a, p2a)
            getIntermediates(p0b, n0b, p3b, n3b, p1b, p2b)

            // calculate using curviness, how many pts we need
            val angle0 = angle(p0a, p1a, p2a) + angle(p0b, p1b, p2b)
            val angle1 = angle(p1a, p2a, p3a) + angle(p1b, p2b, p3b)
            val stopsF = ((angle0 + angle1) * ptsPerRadiant * 0.5)
            val stops = if (stopsF.isFinite()) Maths.max(1, stopsF.roundToInt()) else 1

            result.add(p0a)
            result.add(p0b)

            for (j in 1 until stops) {
                val t = j.toDouble() / stops
                result.add(interpolate(p0a, p1a, p2a, p3a, t))
                result.add(interpolate(p0b, p1b, p2b, p3b, t))
            }
        }

        result.add(posNormals[posNormals.size - 3])
        result.add(posNormals[posNormals.size - 1])

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
        return asin(sqrt(cross / (al * bl)))
    }

    fun interpolate(p0: Vector3d, p1: Vector3d, p2: Vector3d, t: Double, dst: Vector3d = Vector3d()): Vector3d {
        // 1 2 1
        val s = 1.0 - t
        dst.set(p0).mul(s * s)
        p1.mulAdd(2.0 * s * t, dst, dst)
        p2.mulAdd(t * t, dst, dst)
        return dst
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

    fun getIntermediate(p0: Vector3d, n0: Vector3d, p1: Vector3d, n1: Vector3d, dst: Vector3d) {
        // calculate the intermediate point(s)
        Vectors.intersectSafely(p0, n0, p1, n1, SplineMesh.curveFactor, dst)
    }

    fun getIntermediates(p0: Vector3d, n0: Vector3d, p1: Vector3d, n1: Vector3d, dst0: Vector3d, dst1: Vector3d) {
        // calculate the intermediate point(s)
        Vectors.intersectSafely(p0, n0, p1, n1, SplineMesh.curveFactor, dst0, dst1)
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