package me.anno.tests.game.flatworld.streets

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.Distances.rayRayClosestTs
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.createList
import org.joml.AABBd
import org.joml.Vector2d
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

data class StreetSegment(val a: Vector3d, val b: Vector3d?, val c: Vector3d) {

    init {
        if (a == b || c == b || a == c) {
            throw IllegalStateException("Illegal Segment [$a,$b,$c]")
        }
    }

    var entity: Entity? = null
    var streetMesh: Mesh? = null
    // todo extra mesh for decorations
    // todo extra mesh for road markings

    data class DistanceHit(val distance: Double, val t: Double) : Comparable<DistanceHit> {
        override fun compareTo(other: DistanceHit): Int {
            return distance.compareTo(other.distance)
        }
    }

    fun save(): StreetSegmentData {
        val data = StreetSegmentData()
        data.a.set(a)
        data.c.set(c)
        data.b = b
        return data
    }

    // todo this method could/should be able to return two results...
    fun intersects(other: StreetSegment, allowT01: Boolean): Vector2d? {
        // find the (parametric intersection) between these two streets
        //  return this.t, other.t
        val matchesStartOrEnd = !allowT01 && (
                a == other.a || c == other.c ||
                        a == other.c || c == other.a)
        if (bounds.testAABB(other.bounds)) {
            var bestDistanceSq = sq(3.0)
            var bestST: Vector2d? = null
            for (si in 1 until splits.size) {
                val s0 = splits[si - 1]
                val s1 = splits[si]
                for (ti in 1 until other.splits.size) {
                    val t0 = other.splits[ti - 1]
                    val t1 = other.splits[ti]
                    val st = rayRayClosestTs(s0, s1 - s0, t0, t1 - t0)
                    val sx = clamp(st.x)
                    val tx = clamp(st.y)
                    val sv = s0.mix(s1, sx, Vector3d())
                    val tv = t0.mix(t1, tx, Vector3d())
                    val distSq = sv.distanceSquared(tv)
                    if (distSq < bestDistanceSq) {
                        st.x = mix(si - 1.0, si.toDouble(), st.x) / (splits.size - 1.0)
                        st.y = mix(ti - 1.0, ti.toDouble(), st.y) / (other.splits.size - 1.0)
                        if (matchesStartOrEnd && max(
                                abs(st.x - 0.5),
                                abs(st.y - 0.5)
                            ) > 0.4999
                        ) continue // skip this intersection
                        bestDistanceSq = distSq
                        // map st into the correct range
                        bestST = st
                    }
                }
            }
            return bestST
        } else return null
    }

    fun distanceToRay(pos: Vector3d, dir: Vector3d): DistanceHit {
        // find distance to this segment...
        //  - first find closest t
        //  - then find distance to point
        if (b == null) {
            return distanceToRay(pos, dir, a, c)
        }
        // if is too curvy, just split this in half, and try again
        // this could be solved better, I think...
        val splits = splits
        val numSplits = splits.size - 1
        return if (numSplits < 2) {
            distanceToRay(pos, dir, a, c)
        } else {
            createList(numSplits) {
                val t0 = (it) / numSplits.toDouble()
                val t1 = (it + 1) / numSplits.toDouble()
                val hit = distanceToRay(pos, dir, splits[it], splits[it + 1])
                DistanceHit(hit.distance, mix(t0, t1, hit.t))
            }.min()
        }
    }

    @Suppress("IfThenToElvis")
    val angleRadians = if (b != null) {
        b.sub(a, Vector3d()).angle(c.sub(b, Vector3d()))
    } else 0.0

    val splits by lazy {
        val numSplits = (angleRadians * 4.0).toInt()
        if (b != null && numSplits >= 2) {
            createList(numSplits + 1) {
                interpolate(it / numSplits.toDouble())
            }
        } else listOf(a, c)
    }

    val bounds by lazy {
        val bounds = AABBd()
        for (s in splits) bounds.union(s)
        bounds.addMargin(3.0) // street radius
        bounds
    }

    val length by lazy { // just an estimate...
        (1 until splits.size).sumOf {
            splits[it - 1].distance(splits[it])
        }
    }

    init {
        assertTrue(length > 1e-9) {
            "Illegal segment [$a, $b, $c], splits: $splits"
        }
    }

    fun distanceToRay(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): DistanceHit {
        val ts = rayRayClosestTs(pos, dir, a, b - a)
        val closest0 = pos + dir * max(ts.x, 0.0)
        val closest1 = a.mix(b, clamp(ts.y), Vector3d())
        return DistanceHit(closest0.distance(closest1), ts.y)
    }

    fun extrudeVector(a: Vector3d, b: Vector3d, c: Vector3d) {
        b.mul(4.0).sub(a).sub(c).mul(0.5)
    }

    fun splitSegment(t0: Double, t1: Double): StreetSegment {
        if (t0 == 0.0 && t1 == 1.0) return this
        val a = interpolate(t0)
        val c = interpolate(t1)
        return splitSegment(t0, t1, a, c)
    }

    fun splitSegmentDx(t0: Double, t1: Double, dx: Double): StreetSegment {
        val a = interpolateDx(t0, dx)
        val c = interpolateDx(t1, dx)
        val b = interpolateDx((t0 + t1) * 0.5, dx)
        extrudeVector(a, b, c)
        return StreetSegment(a, b, c)
    }

    fun splitSegment(t0: Double, t1: Double, a: Vector3d, c: Vector3d): StreetSegment {
        val b = interpolate((t0 + t1) * 0.5)
        extrudeVector(a, b, c)
        return StreetSegment(a, b, c)
    }

    fun interpolate(t: Double, dst: Vector3d = Vector3d()): Vector3d {
        return if (b == null) {
            a.mix(c, t, dst)
        } else {
            val s = 1.0 - t
            a.mul(s * s, dst)
            b.mulAdd(2.0 * s * t, dst, dst)
            c.mulAdd(t * t, dst, dst)
            dst
        }
    }

    fun interpolateDx(t: Double, dx: Double, dst: Vector3d = Vector3d()): Vector3d {
        val result = interpolate(t, dst)
        val normal = interpolate(t + 0.001).sub(result)
        normal.cross(0.0, 1.0, 0.0)
        normal.mulAdd(dx / normal.length(), result, result)
        return result
    }

    companion object {
        fun getAngle(v: Vector3d, vi: Vector3d): Double {
            val dx = vi.x - v.x
            val dz = vi.z - v.z
            if (abs(dx) < 1e-38 && abs(dz) < 1e-38) return 0.0 // meh
            return atan2(dx, dz)
        }
    }
}