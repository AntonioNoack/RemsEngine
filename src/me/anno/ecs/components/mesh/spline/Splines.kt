package me.anno.ecs.components.mesh.spline

import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Splines {

    /**
     * posNormals: mixed positions and normals, pnpn²-pnpn²-pnpn²-pnpn² <br>
     * returns list of positions
     * */
    fun generateSplineLineQuad(posNormals: Array<Vector3d>, ptsPerRadiant: Double): List<Vector3d> {

        val result = ArrayList<Vector3d>()

        for (i in 1 until posNormals.size step 2) {
            val v = posNormals[i]
            if (v.length() !in 0.99..1.01)
                throw IllegalStateException("$i: $v")
        }

        val p1a = Vector3d()
        val p2a = Vector3d()
        val p1b = Vector3d()
        val p2b = Vector3d()
        val p1c = Vector3d()
        val p2c = Vector3d()
        val p1d = Vector3d()
        val p2d = Vector3d()

        var p0a = posNormals[0]
        var n0a = posNormals[1]
        var p0b = posNormals[2]
        var n0b = posNormals[3]
        var p0c = posNormals[4]
        var n0c = posNormals[5]
        var p0d = posNormals[6]
        var n0d = posNormals[7]

        for (i in 8 until posNormals.size step 8) {

            result.add(Vector3d(p0a))
            result.add(Vector3d(p0b))
            result.add(Vector3d(p0c))
            result.add(Vector3d(p0d))

            val p3a = posNormals[i]
            val n3a = posNormals[i + 1]
            val p3b = posNormals[i + 2]
            val n3b = posNormals[i + 3]
            val p3c = posNormals[i + 4]
            val n3c = posNormals[i + 5]
            val p3d = posNormals[i + 6]
            val n3d = posNormals[i + 7]

            createControlPoints(p0a, n0a, p3a, n3a, p1a, p2a)
            createControlPoints(p0b, n0b, p3b, n3b, p1b, p2b)
            createControlPoints(p0c, n0c, p3c, n3c, p1c, p2c)
            createControlPoints(p0d, n0d, p3d, n3d, p1d, p2d)

            // calculate using curviness, how many pts we need
            val angle = angle(p0a, p1a, p2a) + angle(p0b, p1b, p2b) + angle(p0c, p1c, p2c) + angle(p0d, p1d, p2d) +
                    angle(p1a, p2a, p3a) + angle(p1b, p2b, p3b) + angle(p1c, p2c, p3c) + angle(p1d, p2d, p3d)

            val stopsF = (angle * ptsPerRadiant * 0.125)
            if (stopsF.isFinite()) {
                val stops = stopsF.roundToInt()
                for (j in 1 until stops) {
                    val t = j.toDouble() / stops
                    result.add(interpolate(p0a, p1a, p2a, p3a, t))
                    result.add(interpolate(p0b, p1b, p2b, p3b, t))
                    result.add(interpolate(p0c, p1c, p2c, p3c, t))
                    result.add(interpolate(p0d, p1d, p2d, p3d, t))
                }
            }

            p0a = p3a
            n0a = n3a
            p0b = p3b
            n0b = n3b
            p0c = p3c
            n0c = n3c
            p0d = p3d
            n0d = n3d

        }

        result.add(Vector3d(p0a))
        result.add(Vector3d(p0b))
        result.add(Vector3d(p0c))
        result.add(Vector3d(p0d))

        return result

    }

    fun angle(p0: Vector3d, p1: Vector3d, p2: Vector3d): Double {
        val tmp1 = JomlPools.vec3d.create()
        val tmp2 = JomlPools.vec3d.create()
        tmp1.set(p1).sub(p0)
        tmp2.set(p2).sub(p1)
        JomlPools.vec3d.sub(2)
        return tmp1.angle(tmp2)
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

    fun createControlPoints(p0: Vector3d, n0: Vector3d, p1: Vector3d, n1: Vector3d, dst0: Vector3d, dst1: Vector3d) {

        if (n0.length() !in 0.99..1.01)
            throw IllegalStateException("$n0,$n1")

        // calculate the intermediate point(s)

        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        val dz = p1.z - p0.z

        val vertical = JomlPools.vec3d.create()
            .set(dx, dy, dz)
            .cross(n0.x + n1.x, n0.y + n1.y, n0.z + n1.z)
            .mul(0.5) // length: distance, perpendicular to p1-p0 and n0/n1

        dst0.set(n0).cross(vertical).add(p0)
        dst1.set(vertical).cross(n1).add(p1)

        JomlPools.vec3d.sub(1)

        // todo this was working great, why is it no longer working?
        // Vectors.intersectSafely(p0, n0, p1, n1, SplineMesh.curveFactor, dst0, dst1)
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