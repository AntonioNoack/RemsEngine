package me.anno.tests.game.flatworld.streets

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.geometry.Distances.rayRayClosestTs
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.max

data class StreetSegment(val a: Vector3d, val b: Vector3d?, val c: Vector3d) {

    var entity: Entity? = null
    var mesh: Mesh? = null

    data class DistanceHit(val distance: Double, val t: Double) : Comparable<DistanceHit> {
        override fun compareTo(other: DistanceHit): Int {
            return distance.compareTo(other.distance)
        }
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
        if (b != null) {
            val numSplits = (angleRadians * 4.0).toInt()
            createList(numSplits + 1) {
                interpolate(it / numSplits.toDouble())
            }
        } else listOf(a, c)
    }

    val length by lazy { // just an estimate...
        (1 until splits.size).sumOf {
            splits[it - 1].distance(splits[it])
        }
    }

    fun distanceToRay(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): DistanceHit {
        val ts = rayRayClosestTs(pos, dir, a, b - a)
        val closest0 = pos + dir * max(ts.x, 0.0)
        val closest1 = a.lerp(b, clamp(ts.y), Vector3d())
        return DistanceHit(closest0.distance(closest1), ts.y)
    }

    fun extrudeVector(a: Vector3d, b: Vector3d, c: Vector3d) {
        b.mul(4.0).sub(a).sub(c).mul(0.5)
    }

    fun splitSegment(t0: Double, t1: Double): StreetSegment {
        val a = interpolate(t0)
        val b = interpolate((t0 + t1) * 0.5)
        val c = interpolate(t1)
        extrudeVector(a, b, c)
        return StreetSegment(a, b, c)
    }

    fun interpolate(t: Double): Vector3d {
        return if (b == null) {
            a.lerp(c, t, Vector3d())
        } else {
            val s = 1.0 - t
            val ret = Vector3d()
            a.mul(s * s, ret)
            b.mulAdd(2.0 * s * t, ret, ret)
            c.mulAdd(t * t, ret, ret)
            ret
        }
    }

    companion object {
        fun getAngle(v: Vector3d, vi: Vector3d): Double {
            val dx = vi.x - v.x
            val dz = vi.z - v.z
            return atan2(dx, dz)
        }
    }
}