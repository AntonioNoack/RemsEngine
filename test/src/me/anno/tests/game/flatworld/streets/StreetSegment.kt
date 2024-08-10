package me.anno.tests.game.flatworld.streets

import me.anno.ecs.components.mesh.MeshComponent
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.geometry.Distances.rayRayClosestTs
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Vector3d
import kotlin.math.max

data class StreetSegment(val a: Vector3d, val b: Vector3d?, val c: Vector3d) {

    val roughLength = if (b == null) a.distance(c) else a.distance(b) + b.distance(c)
    var component: MeshComponent? = null

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
        val angleRadians = b.sub(a, Vector3d()).angle(c.sub(b, Vector3d()))
        val numSplits = (angleRadians * 4.0).toInt()
        return if (numSplits < 2) {
            distanceToRay(pos, dir, a, c)
        } else {
            createArrayList(numSplits) {
                val t0 = (it) / numSplits.toDouble()
                val t1 = (it + 1) / numSplits.toDouble()
                val subSegment = splitSegment(t0, t1)
                val hit = distanceToRay(pos, dir, subSegment.a, subSegment.c)
                DistanceHit(hit.distance, mix(t0, t1, hit.t))
            }.min()
        }
    }

    fun distanceToRay(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): DistanceHit {
        val ts = rayRayClosestTs(pos, dir, a, b - a)
        val closest0 = pos + dir * max(ts.x, 0.0)
        val closest1 = a.lerp(b, clamp(ts.y), Vector3d())
        return DistanceHit(closest0.distance(closest1), ts.y)
    }

    fun extrude(a: Vector3d, b: Vector3d, c: Vector3d) {
        b.mul(4.0).sub(a).sub(c).mul(0.5)
    }

    fun splitSegment(t0: Double, t1: Double): StreetSegment {
        val a = interpolate(t0)
        val b = interpolate((t0 + t1) * 0.5)
        val c = interpolate(t1)
        extrude(a, b, c)
        return StreetSegment(a, b, c)
    }

    fun splitSegment(n: Int): List<StreetSegment> {
        return createArrayList(n) {
            val t0 = (it) / n.toDouble()
            val t1 = (it + 1) / n.toDouble()
            splitSegment(t0, t1)
        }
    }

    fun interpolate(t: Double): Vector3d {
        return if (b == null) {
            a.lerp(c, t, Vector3d())
        } else {
            val ab = a.lerp(b, t, Vector3d())
            val bc = b.lerp(c, t, Vector3d())
            ab.lerp(bc, t)
        }
    }
}