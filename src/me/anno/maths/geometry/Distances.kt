package me.anno.maths.geometry

import me.anno.maths.Maths.clamp
import org.joml.Vector2d
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.max

object Distances {

    fun pointPointDistance(a: Vector3d, b: Vector3d): Double {
        return a.distance(b)
    }

    fun linePointDistance(a: Vector3d, b: Vector3d, pt: Vector3d): Double {
        return linePointClosest(a, b, pt).distance(pt)
    }

    // todo unit-test this
    fun linePointClosest(a: Vector3d, b: Vector3d, pt: Vector3d): Vector3d {
        val dir = b - a
        val tx = (pt.dot(dir) - a.dot(dir)) / dir.lengthSquared()
        return dir.mulAdd(tx, a, Vector3d())
    }

    @Suppress("unused") // just an alias
    fun pointLineDistance(pt: Vector3d, a: Vector3d, b: Vector3d): Double {
        return linePointDistance(a, b, pt)
    }

    @Suppress("unused") // just an alias
    fun pointLineClosest(pt: Vector3d, a: Vector3d, b: Vector3d): Vector3d {
        return linePointClosest(a, b, pt)
    }

    @Suppress("unused") // just an alias
    fun pointSegmentClosest(pt: Vector3d, a: Vector3d, b: Vector3d): Vector3d {
        return segmentPointClosest(a, b, pt)
    }

    // todo unit-test this
    fun segmentPointClosest(a: Vector3d, b: Vector3d, pt: Vector3d): Vector3d {
        val dir = b - a
        val tx = clamp((pt.dot(dir) - a.dot(dir)) / dir.lengthSquared())
        return dir.mulAdd(tx, a, Vector3d())
    }

    // todo unit-test this
    fun rayPointDistance(pos: Vector3d, dir: Vector3d, pt: Vector3d): Double {
        return rayPointClosest(pos, dir, pt).distance(pt)
    }

    // todo unit-test this
    // https://stackoverflow.com/questions/73452295/find-the-closest-point-on-a-ray-from-another-point/73452460#73452460 by Jesús Jangwon Kim
    fun rayPointClosest(pos: Vector3d, dir: Vector3d, pt: Vector3d): Vector3d {
        // val v = pt - pos
        // val t = max(0.0, v.dot(dir))
        val t = max(0.0, pt.dot(dir) - pos.dot(dir))
        return dir.mulAdd(t, pos, Vector3d())
    }

    // https://math.stackexchange.com/a/2083614/1136025
    fun lineLineDistance(a0: Vector3d, b0: Vector3d, a1: Vector3d, b1: Vector3d): Double {
        val dir0 = (b0 - a0).normalize()
        val dir1 = (b1 - a1).normalize()
        val cross = dir0.cross(dir1, Vector3d())
        return abs(a0.dot(cross) - a1.dot(cross)) / cross.length()
    }

    @Suppress("unused") // just an alias
    fun lineRayDistance(a: Vector3d, b: Vector3d, pos: Vector3d, dir: Vector3d): Double {
        return rayLineDistance(pos, dir, a, b)
    }

    fun rayLineDistance(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): Double {
        val ts = rayRayClosestTs(pos, dir, a, b - a)
        val D = pos + dir * max(ts.x, 0.0)
        val E = a.lerp(b, ts.y, Vector3d())
        return D.distance(E)
    }

    @Suppress("unused") // just an alias
    fun segmentRayDistance(a: Vector3d, b: Vector3d, pos: Vector3d, dir: Vector3d): Double {
        return raySegmentDistance(pos, dir, a, b)
    }

    fun raySegmentDistance(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): Double {
        val ts = rayRayClosestTs(pos, dir, a, b - a)
        val closest0 = pos + dir * max(ts.x, 0.0)
        val closest1 = a.lerp(b, clamp(ts.y), Vector3d())
        return closest0.distance(closest1)
    }

    // todo test this
    fun segmentSegmentDistance(a0: Vector3d, b0: Vector3d, a1: Vector3d, b1: Vector3d): Double {
        val ts = rayRayClosestTs(a0, b0 - a0, a1, b1 - a1)
        val closest0 = a0.lerp(b0, clamp(ts.x), Vector3d())
        val closest1 = a1.lerp(b1, clamp(ts.y), Vector3d())
        return closest0.distance(closest1)
    }

    // https://palitri.com/vault/stuff/maths/Rays%20closest%20point.pdf
    fun rayRayClosestPoints(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Pair<Vector3d, Vector3d> {
        val ts = rayRayClosestTs(pos0, dir0, pos1, dir1)
        val D = pos0 + dir0 * max(ts.x, 0.0)
        val E = pos1 + dir1 * max(ts.y, 0.0)
        return D to E
    }

    fun rayRayClosestTs(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Vector2d {
        val A = pos0
        val a = dir0
        val B = pos1
        val b = dir1
        val c = B.sub(A, Vector3d())
        val aa = a.lengthSquared()
        val bb = b.lengthSquared()
        val ac = a.dot(c)
        val ab = a.dot(b)
        val bc = b.dot(c)
        val div = aa * bb - ab * ab
        val t = (ac * bb - bc * ab) / div
        val s = (ac * ab - bc * aa) / div
        return Vector2d(t, s)
    }

    // todo unit-test this
    fun rayRayClosestPoint(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Vector3d {
        val (a, b) = rayRayClosestPoints(pos0, dir0, pos1, dir1)
        return a.add(b).mul(0.5)
    }

    // todo unit-test this
    fun rayRayDistance(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Double {
        val (a, b) = rayRayClosestPoints(pos0, dir0, pos1, dir1)
        return pointPointDistance(a, b)
    }
}