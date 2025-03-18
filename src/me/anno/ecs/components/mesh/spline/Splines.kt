package me.anno.ecs.components.mesh.spline

import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.posMod
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Splines {

    /**
     * returns pp-pp-pp-pp (left/right pairs)
     * */
    fun generateSplinePoints(
        points: List<SplineControlPoint>,
        pointsPerRadian: Double,
        isClosed: Boolean,
    ): List<Vector3d> {
        val capacity = points.size + isClosed.toInt()
        val posNormals = createArrayList(capacity * 4) { Vector3d() }
        for (i in 0 until capacity) {
            val i4 = i * 4
            val pt = points[posMod(i, points.size)]
            pt.getLocalPosition(posNormals[i4], -1.0)
            pt.getLocalForward(posNormals[i4 + 1])
            pt.getLocalPosition(posNormals[i4 + 2], +1.0)
            pt.getLocalForward(posNormals[i4 + 3])
            /*fun showDir(v: Vector3d, d: Vector3d) {
                DebugShapes.debugArrows.add(DebugLine(v, v + d * 2.0, UIColors.greenYellow, 1e3f))
            }
            showDir(posNormals[i4], posNormals[i4 + 1])
            showDir(posNormals[i4 + 2], posNormals[i4 + 3])*/
        }
        return generateSplineLinePair(posNormals, pointsPerRadian, isClosed)
    }

    /**
     * pns: mixed positions and normals, pnpn-pnpn-pnpn-pnpn <br>
     * returns list of positions, pp-pp-pp-pp
     * */
    fun generateSplineLinePair(pns: List<Vector3d>, ptsPerRadiant: Double, close: Boolean): List<Vector3d> {

        val result = ArrayList<Vector3d>()

        val p1a = Vector3d()
        val p1b = Vector3d()

        val p2b = Vector3d()
        val p2a = Vector3d()

        var p0a = pns[0]
        var n0a = pns[1]
        var p0b = pns[2]
        var n0b = pns[3]

        var end = pns.size
        if (!close) end -= 4
        forLoop(0, end, 4) { i ->

            val p3a = pns[posMod(i + 4, pns.size)]
            val n3a = pns[posMod(i + 5, pns.size)]
            val p3b = pns[posMod(i + 6, pns.size)]
            val n3b = pns[posMod(i + 7, pns.size)]

            result.add(p0a)
            result.add(p0b)

            getIntermediatePointsForSpline(p0a, n0a, p3a, n3a, p1a, p2a)
            getIntermediatePointsForSpline(p0b, n0b, p3b, n3b, p1b, p2b)

            val c0a = interpolate(p0a, p1a, p2a, p3a, 0.5)

            // calculate using curviness, how many pts we need
            val angle = max(abs((p0a - c0a).angle(n0a)) + abs((c0a - p3a).angle(n3a)), abs(n0a.angle(n3a)))
            val stopsF = angle * ptsPerRadiant
            if (stopsF.isFinite() && stopsF >= 0.5) {
                val stops = stopsF.roundToIntOr(2)
                for (j in 1 until stops) {
                    val t = j.toDouble() / stops
                    result.add(interpolate(p0a, p1a, p2a, p3a, t))
                    result.add(interpolate(p0b, p1b, p2b, p3b, t))
                }
            }

            p0a = p3a
            n0a = n3a
            p0b = p3b
            n0b = n3b
        }

        result.add(p0a)
        result.add(p0b)

        /*for (i in result.indices step 2) {
            val v = result[i]
            val d = result[i + 1]
            DebugShapes.debugArrows.add(DebugLine(v, d, UIColors.axisXColor, 0f))
        }*/

        return result
    }

    fun angle(p0: Vector3d, p1: Vector3d, p2: Vector3d): Double {
        val tmp0 = JomlPools.vec3d.create()
        val tmp1 = JomlPools.vec3d.create()
        p1.sub(p0, tmp0)
        p2.sub(p1, tmp1)
        val result = tmp1.angle(tmp0)
        JomlPools.vec3d.sub(2)
        return result
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

    fun getIntermediatePointsForSpline(
        p0: Vector3d, d0: Vector3d,
        p1: Vector3d, d1: Vector3d,
        dst0: Vector3d, dst1: Vector3d
    ) {

        // calculate the intermediate points for spline interpolation
        // assumes d0 and d1 are normalized
        val extend = p1.distance(p0) * 0.5
        d0.mulAdd(-extend, p0, dst0)
        d1.mulAdd(+extend, p1, dst1)

        // DebugShapes.debugPoints.add(DebugPoint(dst0, UIColors.gold, 1e3f))
        // DebugShapes.debugPoints.add(DebugPoint(dst1, UIColors.gold, 1e3f))
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

    fun generateCurve(a0: Float, a1: Float, n: Int): List<Vector2f> {
        assertTrue(n >= 0)
        return when (n) {
            0 -> emptyList()
            1 -> {
                val angle = (a0 + a1) * 0.5f
                listOf(Vector2f(cos(angle), sin(angle)))
            }
            else -> {
                val div = 1f / (n - 1f)
                createArrayList(n) {
                    val angle = mix(a0, a1, it * div)
                    Vector2f(cos(angle), sin(angle))
                }
            }
        }
    }
}