package me.anno.maths.geometry

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3d.Companion.length
import org.joml.Vector3d.Companion.lengthSquared
import org.joml.Vector3f
import org.joml.Vector3f.Companion.length
import org.joml.Vector3f.Companion.lengthSquared
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Contains all combinations of {point,line,segment,ray}²-distances, and closest, and squared where it makes sense.
 * A segment is a line with 0 <= t <= 1.
 * A ray is a line with 0 <= t.
 *
 * Directions don't need to be normalized, but they should be non-zero and finite.
 *
 * Most methods have squared distance versions, so you can save the sqrt operation to improve performance by a little.
 * */
object Distances {

    /**
     * Given two points a and b, calculate their distance.
     * You could also just use a.distance(b).
     * */
    fun pointPointDistance(a: Vector3d, b: Vector3d): Double {
        return a.distance(b)
    }

    /**
     * Given two points a and b, calculate their distance.
     * You could also just use a.distance(b).
     * */
    fun pointPointDistance(a: Vector3f, b: Vector3f): Float {
        return a.distance(b)
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the distance squared between them.
     * */
    fun linePointDistanceSquared(a: Vector3d, b: Vector3d, pt: Vector3d): Double {
        val t = linePointClosestT(a, b, pt)
        val closestX = mix(a.x, b.x, t)
        val closestY = mix(a.y, b.y, t)
        val closestZ = mix(a.z, b.z, t)
        return pt.distanceSquared(closestX, closestY, closestZ)
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the distance squared between them.
     * */
    fun linePointDistanceSquared(a: Vector3f, b: Vector3f, pt: Vector3f): Float {
        val t = linePointClosestT(a, b, pt)
        val closestX = mix(a.x, b.x, t)
        val closestY = mix(a.y, b.y, t)
        val closestZ = mix(a.z, b.z, t)
        return pt.distanceSquared(closestX, closestY, closestZ)
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the distance between them.
     * */
    fun linePointDistance(a: Vector3d, b: Vector3d, pt: Vector3d): Double {
        return sqrt(linePointDistanceSquared(a, b, pt))
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the distance between them.
     * */
    fun linePointDistance(a: Vector3f, b: Vector3f, pt: Vector3f): Float {
        return sqrt(linePointDistanceSquared(a, b, pt))
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the closest point on (a,b) to (pt).
     * */
    fun linePointClosest(a: Vector3d, b: Vector3d, pt: Vector3d, dst: Vector3d): Vector3d {
        return a.mix(b, linePointClosestT(a, b, pt), dst)
    }

    /**
     * Given a line (mix(a,b,t), any t) and a point (pt), find the closest point on (a,b) to (pt).
     * */
    fun linePointClosest(a: Vector3f, b: Vector3f, pt: Vector3f, dst: Vector3f): Vector3f {
        return a.mix(b, linePointClosestT(a, b, pt), dst)
    }

    /**
     * Given a line (mix(a,b,t), any t), calculate the closest t to point (pt).
     * */
    fun linePointClosestT(a: Vector3d, b: Vector3d, pt: Vector3d): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val dotPt = pt.dot(dx, dy, dz)
        val dotA = a.dot(dx, dy, dz)
        return (dotPt - dotA) / lengthSquared(dx, dy, dz)
    }

    /**
     * Given a line (mix(a,b,t), any t), calculate the closest t to point (pt).
     * */
    fun linePointClosestT(a: Vector3f, b: Vector3f, pt: Vector3f): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        val dotPt = pt.dot(dx, dy, dz)
        val dotA = a.dot(dx, dy, dz)
        return (dotPt - dotA) / lengthSquared(dx, dy, dz)
    }

    /**
     * Given a ray (pos + dir * t, any t), calculate the closest t to point (pt).
     * */
    fun rayPointClosestT(pos: Vector3d, dir: Vector3d, pt: Vector3d): Double {
        val dotPt = pt.dot(dir)
        val dotA = pos.dot(dir)
        return (dotPt - dotA) / dir.lengthSquared()
    }

    /**
     * Given a ray (pos + dir * t, any t), calculate the closest t to point (pt).
     * */
    fun rayPointClosestT(pos: Vector3f, dir: Vector3f, pt: Vector3f): Float {
        val dotPt = pt.dot(dir)
        val dotA = pos.dot(dir)
        return (dotPt - dotA) / dir.lengthSquared()
    }

    /**
     * Find the closest point on a segment [a,b] to a third point pt.
     * */
    fun segmentPointClosest(a: Vector3d, b: Vector3d, pt: Vector3d, dst: Vector3d): Vector3d {
        val t = linePointClosestT(a, b, pt)
        return a.mix(b, clamp(t), dst)
    }

    /**
     * Find the closest point on a segment [a,b] to a third point pt.
     * */
    fun segmentPointClosest(a: Vector3f, b: Vector3f, pt: Vector3f, dst: Vector3f): Vector3f {
        val t = linePointClosestT(a, b, pt)
        return a.mix(b, clamp(t), dst)
    }

    /**
     * Finds the distance of a point (pt) to a ray (pos + dir * t, t >= 0) squared.
     * */
    fun rayPointDistanceSquared(pos: Vector3d, dir: Vector3d, pt: Vector3d): Double {
        return rayPointClosest(pos, dir, pt, JomlPools.vec3d.borrow())
            .distanceSquared(pt)
    }

    /**
     * Finds the distance of a point (pt) to a ray (pos + dir * t, t >= 0) squared.
     * */
    fun rayPointDistanceSquared(pos: Vector3f, dir: Vector3f, pt: Vector3f): Float {
        return rayPointClosest(pos, dir, pt, JomlPools.vec3f.borrow())
            .distanceSquared(pt)
    }

    /**
     * Finds the distance of a point (pt) to a ray (pos + dir * t, t >= 0).
     * */
    fun rayPointDistance(pos: Vector3d, dir: Vector3d, pt: Vector3d): Double {
        return sqrt(rayPointDistanceSquared(pos, dir, pt))
    }

    /**
     * Finds the distance of a point (pt) to a ray (pos + dir * t, t >= 0).
     * */
    fun rayPointDistance(pos: Vector3f, dir: Vector3f, pt: Vector3f): Float {
        return sqrt(rayPointDistanceSquared(pos, dir, pt))
    }

    /**
     * Calculates the closest point on the ray to a given point.
     * from https://stackoverflow.com/questions/73452295/find-the-closest-point-on-a-ray-from-another-point/73452460#73452460
     * by Jesús Jangwon Kim
     * */
    fun rayPointClosest(pos: Vector3d, dir: Vector3d, pt: Vector3d, dst: Vector3d): Vector3d {
        val vx = pt.x - pos.x
        val vy = pt.y - pos.y
        val vz = pt.z - pos.z
        val t = max(0.0, dir.dot(vx, vy, vz) / dir.lengthSquared())
        return pos.fma(t, dir, dst)
    }

    /**
     * Calculates the closest point on the ray to a given point.
     * from https://stackoverflow.com/questions/73452295/find-the-closest-point-on-a-ray-from-another-point/73452460#73452460
     * by Jesús Jangwon Kim
     * */
    fun rayPointClosest(pos: Vector3f, dir: Vector3f, pt: Vector3f, dst: Vector3f): Vector3f {
        val vx = pt.x - pos.x
        val vy = pt.y - pos.y
        val vz = pt.z - pos.z
        val t = max(0f, dir.dot(vx, vy, vz) / dir.lengthSquared())
        return dir.mulAdd(t, pos, dst)
    }

    /**
     * Calculates the distance between two lines (a0, b0) and (a1, b1).
     * from https://math.stackexchange.com/a/2083614/1136025 by Chappers
     * */
    fun lineLineDistance(a0: Vector3d, b0: Vector3d, a1: Vector3d, b1: Vector3d): Double {
        // directions
        val dx0 = b0.x - a0.x
        val dy0 = b0.y - a0.y
        val dz0 = b0.z - a0.z

        val dx1 = b1.x - a1.x
        val dy1 = b1.y - a1.y
        val dz1 = b1.z - a1.z

        // cross product
        val cx = dy0 * dz1 - dz0 * dy1
        val cy = dz0 * dx1 - dx0 * dz1
        val cz = dx0 * dy1 - dy0 * dx1
        if (lengthSquared(cx, cy, cz) <= 1e-16) {
            // lines are parallel; todo create a unit test for this case
            return linePointDistance(a1, b1, a0)
        }

        val dot0 = a0.dot(cx, cy, cz)
        val dot1 = a1.dot(cx, cy, cz)
        return abs(dot0 - dot1) / length(cx, cy, cz)
    }

    /**
     * Calculates the distance between two lines (a0, b0) and (a1, b1).
     * from https://math.stackexchange.com/a/2083614/1136025 by Chappers
     * */
    fun lineLineDistance(a0: Vector3f, b0: Vector3f, a1: Vector3f, b1: Vector3f): Float {
        // directions
        val dx0 = b0.x - a0.x
        val dy0 = b0.y - a0.y
        val dz0 = b0.z - a0.z

        val dx1 = b1.x - a1.x
        val dy1 = b1.y - a1.y
        val dz1 = b1.z - a1.z

        // cross product
        val cx = dy0 * dz1 - dz0 * dy1
        val cy = dz0 * dx1 - dx0 * dz1
        val cz = dx0 * dy1 - dy0 * dx1
        if (lengthSquared(cx, cy, cz) <= 1e-7f) {
            // lines are parallel; todo create a unit test for this case
            return linePointDistance(a1, b1, a0)
        }

        val dot0 = a0.dot(cx, cy, cz)
        val dot1 = a1.dot(cx, cy, cz)
        return abs(dot0 - dot1) / length(cx, cy, cz)
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line (mix(a,b,t1), any t1),
     * find the closest points between them and then their distance squared.
     * */
    fun rayLineDistanceSquared(
        pos: Vector3d, dir: Vector3d,
        a: Vector3d, b: Vector3d
    ): Double {
        val dir1 = b.sub(a, JomlPools.vec3d.create())
        val ts = rayRayClosestTs(pos, dir, a, dir1, JomlPools.vec2d.borrow())
        JomlPools.vec3d.sub(1) // dir1

        val tx = max(ts.x, 0.0)
        val ty = ts.y

        val dx = (pos.x + dir.x * tx) - mix(a.x, b.x, ty)
        val dy = (pos.y + dir.y * tx) - mix(a.y, b.y, ty)
        val dz = (pos.z + dir.z * tx) - mix(a.z, b.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line (mix(a,b,t1), any t1),
     * find the closest points between them and then their distance squared.
     * */
    fun rayLineDistanceSquared(
        pos: Vector3f, dir: Vector3f,
        a: Vector3f, b: Vector3f
    ): Float {
        val dir1 = b.sub(a, JomlPools.vec3f.create())
        val ts = rayRayClosestTs(pos, dir, a, dir1, JomlPools.vec2f.borrow())
        JomlPools.vec3f.sub(1) // dir1

        val tx = max(ts.x, 0f)
        val ty = ts.y

        val dx = (pos.x + dir.x * tx) - mix(a.x, b.x, ty)
        val dy = (pos.y + dir.y * tx) - mix(a.y, b.y, ty)
        val dz = (pos.z + dir.z * tx) - mix(a.z, b.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line (mix(a,b,t1), any t1),
     * find the closest points between them and then their distance.
     * */
    fun rayLineDistance(pos: Vector3d, dir: Vector3d, a: Vector3d, b: Vector3d): Double {
        return sqrt(rayLineDistanceSquared(pos, dir, a, b))
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line (mix(a,b,t1), any t1),
     * find the closest points between them and then their distance.
     * */
    fun rayLineDistance(pos: Vector3f, dir: Vector3f, a: Vector3f, b: Vector3f): Float {
        return sqrt(rayLineDistanceSquared(pos, dir, a, b))
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line segment (mix(a,b,t1), 0 <= t1 <= 1),
     * find the closest points between them and then their distance squared.
     * */
    fun raySegmentDistanceSquared(
        pos: Vector3d, dir: Vector3d,
        a: Vector3d, b: Vector3d
    ): Double {
        val dir1 = b.sub(a, JomlPools.vec3d.create())
        val ts = rayRayClosestTs(pos, dir, a, dir1, JomlPools.vec2d.borrow())
        JomlPools.vec3d.sub(1) // dir1

        val tx = max(ts.x, 0.0)
        val ty = clamp(ts.y)
        val dx = (pos.x + dir.x * tx) - mix(a.x, b.x, ty)
        val dy = (pos.y + dir.y * tx) - mix(a.y, b.y, ty)
        val dz = (pos.z + dir.z * tx) - mix(a.z, b.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line segment (mix(a,b,t1), 0 <= t1 <= 1),
     * find the closest points between them and then their distance squared.
     * */
    fun raySegmentDistanceSquared(
        pos: Vector3f, dir: Vector3f,
        a: Vector3f, b: Vector3f
    ): Float {
        val dir1 = b.sub(a, JomlPools.vec3f.create())
        val ts = rayRayClosestTs(pos, dir, a, dir1, JomlPools.vec2f.borrow())
        JomlPools.vec3f.sub(1) // dir1

        val tx = max(ts.x, 0f)
        val ty = clamp(ts.y)
        val dx = (pos.x + dir.x * tx) - mix(a.x, b.x, ty)
        val dy = (pos.y + dir.y * tx) - mix(a.y, b.y, ty)
        val dz = (pos.z + dir.z * tx) - mix(a.z, b.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line segment (mix(a,b,t1), 0 <= t1 <= 1),
     * find the closest points between them and then their distance.
     * */
    fun raySegmentDistance(
        pos: Vector3d, dir: Vector3d,
        a: Vector3d, b: Vector3d
    ): Double = sqrt(raySegmentDistanceSquared(pos, dir, a, b))

    /**
     * Given a ray (pos + dir * t0, t0 >= 0) and a line segment (mix(a,b,t1), 0 <= t1 <= 1),
     * find the closest points between them and then their distance.
     * */
    fun raySegmentDistance(
        pos: Vector3f, dir: Vector3f,
        a: Vector3f, b: Vector3f
    ): Float = sqrt(raySegmentDistanceSquared(pos, dir, a, b))

    /**
     * Finds the closest two points between two segments, and then calculates their distance squared.
     * Segment A = mix(a0,b0,t0), segment B = mix(a1,b1,t1), 0 <= t0, t1 <= 1.
     * */
    fun segmentSegmentDistanceSquared(
        a0: Vector3d, b0: Vector3d, // segment A
        a1: Vector3d, b1: Vector3d, // segment B
    ): Double {
        val dir0 = b0.sub(a0, JomlPools.vec3d.create())
        val dir1 = b1.sub(a1, JomlPools.vec3d.create())
        val ts = rayRayClosestTs(a0, dir0, a1, dir1, JomlPools.vec2d.borrow())
        JomlPools.vec3d.sub(2) // dir1

        val tx = clamp(ts.x)
        val ty = clamp(ts.y)
        val dx = mix(a0.x, b0.x, tx) - mix(a1.x, b1.x, ty)
        val dy = mix(a0.y, b0.y, tx) - mix(a1.y, b1.y, ty)
        val dz = mix(a0.z, b0.z, tx) - mix(a1.z, b1.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Finds the closest two points between two segments, and then calculates their distance squared.
     * Segment A = mix(a0,b0,t0), segment B = mix(a1,b1,t1), 0 <= t0, t1 <= 1.
     * */
    fun segmentSegmentDistanceSquared(
        a0: Vector3f, b0: Vector3f, // segment A
        a1: Vector3f, b1: Vector3f, // segment B
    ): Float {
        val dir0 = b0.sub(a0, JomlPools.vec3f.create())
        val dir1 = b1.sub(a1, JomlPools.vec3f.create())
        val ts = rayRayClosestTs(a0, dir0, a1, dir1, JomlPools.vec2f.borrow())
        JomlPools.vec3f.sub(2) // dir1

        val tx = clamp(ts.x)
        val ty = clamp(ts.y)
        val dx = mix(a0.x, b0.x, tx) - mix(a1.x, b1.x, ty)
        val dy = mix(a0.y, b0.y, tx) - mix(a1.y, b1.y, ty)
        val dz = mix(a0.z, b0.z, tx) - mix(a1.z, b1.z, ty)
        return lengthSquared(dx, dy, dz)
    }

    /**
     * Finds the closest two points between two segments, and then calculates their distance.
     * Segment A = mix(a0,b0,t0), segment B = mix(a1,b1,t1), 0 <= t0, t1 <= 1.
     *
     * todo test this
     * */
    fun segmentSegmentDistance(
        a0: Vector3d, b0: Vector3d, // segment A
        a1: Vector3d, b1: Vector3d, // segment B
    ): Double = sqrt(segmentSegmentDistanceSquared(a0, b0, a1, b1))

    /**
     * Finds the closest two points between two segments, and then calculates their distance.
     * Segment A = mix(a0,b0,t0), segment B = mix(a1,b1,t1), 0 <= t0, t1 <= 1.
     *
     * todo test this
     * */
    fun segmentSegmentDistance(
        a0: Vector3f, b0: Vector3f, // segment A
        a1: Vector3f, b1: Vector3f, // segment B
    ): Float = sqrt(segmentSegmentDistanceSquared(a0, b0, a1, b1))

    /**
     * Given two rays (posI + dirI * tI, tI >= 0), find their closest points to each other (dstI).
     *
     * From https://palitri.com/vault/stuff/maths/Rays%20closest%20point.pdf
     * */
    fun rayRayClosestPoints(
        pos0: Vector3d, dir0: Vector3d, // ray A
        pos1: Vector3d, dir1: Vector3d, // ray B
        dst0: Vector3d, dst1: Vector3d, // closest A, closest B
    ) {
        val tmp = JomlPools.vec2d.borrow()
        val ts = rayRayClosestTs(pos0, dir0, pos1, dir1, tmp)
        pos0.fma(max(ts.x, 0.0), dir0, dst0)
        pos1.fma(max(ts.y, 0.0), dir1, dst1)
    }

    /**
     * Given two rays (posI + dirI * tI, tI >= 0), find their closest points to each other (dstI).
     *
     * From https://palitri.com/vault/stuff/maths/Rays%20closest%20point.pdf
     * */
    fun rayRayClosestPoints(
        pos0: Vector3f, dir0: Vector3f, // ray A
        pos1: Vector3f, dir1: Vector3f, // ray B
        dst0: Vector3f, dst1: Vector3f, // closest A, closest B
    ) {
        val tmp = JomlPools.vec2f.borrow()
        val ts = rayRayClosestTs(pos0, dir0, pos1, dir1, tmp)
        pos0.fma(max(ts.x, 0f), dir0, dst0)
        pos1.fma(max(ts.y, 0f), dir1, dst1)
    }

    /**
     * Given two rays posI + dirI * t, I e {0,1},
     * calculate the ts of their closest points.
     * Meaning that pos0 + dir0 * t.x is closest to pos1 + dir1 * t.y.
     *
     * If the lines are parallel, pos0 and the corresponding point on ray1 will be chosen.
     *
     * From https://palitri.com/vault/stuff/maths/Rays%20closest%20point.pdf
     * */
    fun rayRayClosestTs(
        pos0: Vector3d, dir0: Vector3d,
        pos1: Vector3d, dir1: Vector3d,
        dst: Vector2d
    ): Vector2d {
        val cx = pos1.x - pos0.x
        val cy = pos1.y - pos0.y
        val cz = pos1.z - pos0.z
        val aa = dir0.lengthSquared()
        val bb = dir1.lengthSquared()
        val ac = dir0.dot(cx, cy, cz)
        val ab = dir0.dot(dir1)
        val bc = dir1.dot(cx, cy, cz)
        val scale = aa * bb
        val div = scale - ab * ab
        if (abs(div) < 1e-12 * scale) {
            // given the lines are parallel,
            //  set dst.x = 0, and calculate dst.y
            return dst.set(0.0, rayPointClosestT(pos1, dir1, pos0))
        }
        val t = (ac * bb - bc * ab) / div
        val s = (ac * ab - bc * aa) / div
        return dst.set(t, s)
    }

    /**
     * Given two rays posI + dirI * t, I e {0,1},
     * calculate the ts of their closest points.
     * Meaning that pos0 + dir0 * t.x is closest to pos1 + dir1 * t.y.
     *
     * If the lines are parallel, pos0 and the corresponding point on ray1 will be chosen.
     * */
    fun rayRayClosestTs(
        pos0: Vector3f, dir0: Vector3f,
        pos1: Vector3f, dir1: Vector3f,
        dst: Vector2f
    ): Vector2f {
        val cx = pos1.x - pos0.x
        val cy = pos1.y - pos0.y
        val cz = pos1.z - pos0.z
        val aa = dir0.lengthSquared()
        val bb = dir1.lengthSquared()
        val ac = dir0.dot(cx, cy, cz)
        val ab = dir0.dot(dir1)
        val bc = dir1.dot(cx, cy, cz)
        val scale = aa * bb
        val div = scale - ab * ab
        if (abs(div) < 1e-12 * scale) {
            // given the lines are parallel,
            //  set dst.x = 0, and calculate dst.y
            return dst.set(0f, rayPointClosestT(pos1, dir1, pos0))
        }
        val t = (ac * bb - bc * ab) / div
        val s = (ac * ab - bc * aa) / div
        return dst.set(t, s)
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their average.
     * */
    fun rayRayClosestPoint(
        pos0: Vector3d, dir0: Vector3d,
        pos1: Vector3d, dir1: Vector3d,
        dst: Vector3d
    ): Vector3d {
        val tmp = JomlPools.vec3d.borrow()
        rayRayClosestPoints(pos0, dir0, pos1, dir1, tmp, dst)
        return dst.add(tmp).mul(0.5)
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their average.
     * */
    fun rayRayClosestPoint(
        pos0: Vector3f, dir0: Vector3f,
        pos1: Vector3f, dir1: Vector3f,
        dst: Vector3f
    ): Vector3f {
        val tmp = JomlPools.vec3f.borrow()
        rayRayClosestPoints(pos0, dir0, pos1, dir1, tmp, dst)
        return dst.add(tmp).mul(0.5f)
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their distance squared.
     * */
    fun rayRayDistanceSquared(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Double {
        val closest0 = JomlPools.vec3d.create()
        val closest1 = JomlPools.vec3d.create()
        rayRayClosestPoints(pos0, dir0, pos1, dir1, closest0, closest1)
        val distance = closest0.distanceSquared(closest1)
        JomlPools.vec3d.sub(2)
        return distance
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their distance squared.
     * */
    fun rayRayDistanceSquared(pos0: Vector3f, dir0: Vector3f, pos1: Vector3f, dir1: Vector3f): Float {
        val closest0 = JomlPools.vec3f.create()
        val closest1 = JomlPools.vec3f.create()
        rayRayClosestPoints(pos0, dir0, pos1, dir1, closest0, closest1)
        val distance = closest0.distanceSquared(closest1)
        JomlPools.vec3f.sub(2)
        return distance
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their distance.
     * todo unit-test this
     * */
    fun rayRayDistance(pos0: Vector3d, dir0: Vector3d, pos1: Vector3d, dir1: Vector3d): Double {
        return sqrt(rayRayDistanceSquared(pos0, dir0, pos1, dir1))
    }

    /**
     * Finds the closest two points for a pair of rays, and then returns their distance.
     * todo unit-test this
     * */
    fun rayRayDistance(pos0: Vector3f, dir0: Vector3f, pos1: Vector3f, dir1: Vector3f): Float {
        return sqrt(rayRayDistanceSquared(pos0, dir0, pos1, dir1))
    }

    // below this are all the aliases

    /**
     * alias for rayLineDistance(pos,dir,a,b)
     * */
    @Suppress("unused")
    fun lineRayDistance(a: Vector3d, b: Vector3d, pos: Vector3d, dir: Vector3d): Double {
        return rayLineDistance(pos, dir, a, b)
    }

    /**
     * alias for rayLineDistance(pos,dir,a,b)
     * */
    @Suppress("unused")
    fun lineRayDistance(a: Vector3f, b: Vector3f, pos: Vector3f, dir: Vector3f): Float {
        return rayLineDistance(pos, dir, a, b)
    }

    /**
     * alias for raySegmentDistance(pos,dir,a,b)
     * */
    @Suppress("unused")
    fun segmentRayDistance(a: Vector3d, b: Vector3d, pos: Vector3d, dir: Vector3d): Double {
        return raySegmentDistance(pos, dir, a, b)
    }

    /**
     * alias for raySegmentDistance(pos,dir,a,b)
     * */
    @Suppress("unused")
    fun segmentRayDistance(a: Vector3f, b: Vector3f, pos: Vector3f, dir: Vector3f): Float =
        raySegmentDistance(pos, dir, a, b)

    /**
     * alias for linePointDistance(a,b,pt)
     * */
    @Suppress("unused")
    fun pointLineDistance(pt: Vector3d, a: Vector3d, b: Vector3d): Double =
        linePointDistance(a, b, pt)

    /**
     * alias for linePointDistance(a,b,pt)
     * */
    @Suppress("unused")
    fun pointLineDistance(pt: Vector3f, a: Vector3f, b: Vector3f): Float =
        linePointDistance(a, b, pt)

    /**
     * alias for linePointClosest(a,b,pt,dst)
     * */
    @Suppress("unused")
    fun pointLineClosest(pt: Vector3d, a: Vector3d, b: Vector3d, dst: Vector3d): Vector3d =
        linePointClosest(a, b, pt, dst)

    /**
     * alias for linePointClosest(a,b,pt,dst)
     * */
    @Suppress("unused")
    fun pointLineClosest(pt: Vector3f, a: Vector3f, b: Vector3f, dst: Vector3f): Vector3f =
        linePointClosest(a, b, pt, dst)

    /**
     * alias for segmentPointClosest(a,b,pt,dst)
     * */
    @Suppress("unused")
    fun pointSegmentClosest(pt: Vector3d, a: Vector3d, b: Vector3d, dst: Vector3d): Vector3d =
        segmentPointClosest(a, b, pt, dst)

    /**
     * alias for segmentPointClosest(a,b,pt,dst)
     * */
    @Suppress("unused")
    fun pointSegmentClosest(pt: Vector3f, a: Vector3f, b: Vector3f, dst: Vector3f): Vector3f =
        segmentPointClosest(a, b, pt, dst)

    /**
     * Given two points a and b, calculate their distance squared.
     * You could also just use a.distanceSquared(b).
     * */
    @Suppress("unused")
    fun pointPointDistanceSquared(a: Vector3d, b: Vector3d): Double = a.distanceSquared(b)

    /**
     * Given two points a and b, calculate their distance squared.
     * You could also just use a.distanceSquared(b).
     * */
    @Suppress("unused")
    fun pointPointDistanceSquared(a: Vector3f, b: Vector3f): Float = a.distanceSquared(b)
}