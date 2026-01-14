package org.recast4j.recast

/*
+recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

import me.anno.maths.Maths.clamp
import me.anno.utils.types.Booleans.hasFlag
import org.joml.AABBf
import org.joml.Vector3f
import org.recast4j.recast.RasterizeSphere.rasterizationFilledShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object RasterizeCylinderCapsule {

    private const val EPSILON = 0.00001f

    fun rasterizeCapsule(
        hf: Heightfield, start: Vector3f, end: Vector3f, radius: Float, area: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_CAPSULE)
        val bounds = AABBf(start).union(end).addMargin(radius)
        val axis = Vector3f(end).sub(start)
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: Rectangle, dst -> dst.set(intersectCapsule(rectangle, start, end, axis, radius * radius)) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CAPSULE)
    }

    fun rasterizeCylinder(
        hf: Heightfield, start: Vector3f, end: Vector3f, radius: Float, area: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_CYLINDER)
        val bounds = AABBf(start).union(end).addMargin(radius)
        val axis = Vector3f(end).sub(start)
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: Rectangle, dst -> dst.set(intersectCylinder(rectangle, start, end, axis, radius * radius)) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CYLINDER)
    }

    private fun intersectSphere(rectangle: Rectangle, center: Vector3f, radiusSqr: Float): HeightRange? {
        val tmp = HeightRange(0f, 0f)
        return if (RasterizeSphere.intersectSphere(rectangle, tmp, center, radiusSqr)) tmp
        else null
    }

    private fun intersectCapsule(
        rectangle: Rectangle,
        start: Vector3f,
        end: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): HeightRange? {
        var s = mergeIntersections(
            intersectSphere(rectangle, start, radiusSqr),
            intersectSphere(rectangle, end, radiusSqr)
        )
        val axisLen2dSqr = axis.x * axis.x + axis.z * axis.z
        if (axisLen2dSqr > EPSILON) {
            s = slabsCylinderIntersection(rectangle, start, end, axis, radiusSqr, s)
        }
        return s
    }

    private fun intersectCylinder(
        rectangle: Rectangle,
        start: Vector3f,
        end: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): HeightRange? {
        var s = rayCylinderIntersection(
            Vector3f(
                clamp(start.x, rectangle.minX, rectangle.maxX), rectangle.minY,
                clamp(start.z, rectangle.minZ, rectangle.maxZ)
            ), start, axis, radiusSqr
        )
        s = mergeIntersections(
            s, rayCylinderIntersection(
                Vector3f(
                    clamp(end.x, rectangle.minX, rectangle.maxX), rectangle.minY,
                    clamp(end.z, rectangle.minZ, rectangle.maxZ)
                ), start, axis, radiusSqr
            )
        )
        val axisLen2dSqr = axis.x * axis.x + axis.z * axis.z
        if (axisLen2dSqr > EPSILON) {
            s = slabsCylinderIntersection(rectangle, start, end, axis, radiusSqr, s)
        }
        if (axis.y * axis.y > EPSILON) {
            val rectangleOnStartPlane = Array(4) { Vector3f() }
            val rectangleOnEndPlane = Array(4) { Vector3f() }
            val ds = axis.dot(start)
            val de = axis.dot(end)
            for (i in 0..3) {
                val x = if (i.hasFlag(1)) rectangle.minX else rectangle.maxX
                val z = if (i.hasFlag(2)) rectangle.minZ else rectangle.maxZ
                val dotAxisA = axis.dot(x, rectangle.minY, z)
                var t = (ds - dotAxisA) / axis.y
                rectangleOnStartPlane[i].set(x, rectangle.minY + t, z)
                t = (de - dotAxisA) / axis.y
                rectangleOnEndPlane[i].set(x, rectangle.minY + t, z)
            }
            for (i in 0..3) {
                s = cylinderCapIntersection(start, radiusSqr, s, i, rectangleOnStartPlane)
                s = cylinderCapIntersection(end, radiusSqr, s, i, rectangleOnEndPlane)
            }
        }
        return s
    }

    private fun cylinderCapIntersection(
        start: Vector3f, radiusSqr: Float,
        s: HeightRange?, i: Int,
        rectangleOnPlane: Array<Vector3f>
    ): HeightRange? {
        val j = (i + 1) % 4
        // Ray against sphere intersection
        val m = rectangleOnPlane[i].sub(start, Vector3f())
        val d = rectangleOnPlane[j].sub(rectangleOnPlane[i], Vector3f())
        val dl = d.lengthSquared()
        val b = m.dot(d) / dl
        val c = (m.lengthSquared() - radiusSqr) / dl
        val discr = b * b - c
        if (discr > EPSILON) {
            val discrSqrt = sqrt(discr)
            var t1 = -b - discrSqrt
            var t2 = -b + discrSqrt
            if (t1 <= 1 && t2 >= 0) {
                t1 = max(0f, t1)
                t2 = min(1f, t2)
                val y1 = rectangleOnPlane[i][1] + t1 * d[1]
                val y2 = rectangleOnPlane[i][1] + t2 * d[1]
                val y = HeightRange(min(y1, y2), max(y1, y2))
                return mergeIntersections(s, y)
            }
        }
        return s
    }

    private fun slabsCylinderIntersection(
        rectangle: Rectangle, start: Vector3f, end: Vector3f, axis: Vector3f, radiusSqr: Float,
        s0: HeightRange?
    ): HeightRange? {
        var s = s0
        if (min(start.x, end.x) < rectangle.minX) {
            s = mergeIntersections(s, xSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle.minX))
        }
        if (max(start.x, end.x) > rectangle.maxX) {
            s = mergeIntersections(s, xSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle.maxX))
        }
        if (min(start.z, end.z) < rectangle.minZ) {
            s = mergeIntersections(s, zSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle.minZ))
        }
        if (max(start.z, end.z) > rectangle.maxZ) {
            s = mergeIntersections(s, zSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle.maxZ))
        }
        return s
    }

    private fun xSlabCylinderIntersection(
        rectangle: Rectangle,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float,
        x: Float
    ) = rayCylinderIntersection(xSlabRayIntersection(rectangle, start, axis, x), start, axis, radiusSqr)

    private fun xSlabRayIntersection(rectangle: Rectangle, start: Vector3f, direction: Vector3f, x: Float): Vector3f {
        // 2d intersection of plane and segment
        val t = (x - start.x) / direction.x
        val z = clamp(start.z + t * direction.z, rectangle.minZ, rectangle.maxZ)
        return Vector3f(x, rectangle.minY, z)
    }

    private fun zSlabCylinderIntersection(
        rectangle: Rectangle,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float,
        z: Float
    ) = rayCylinderIntersection(zSlabRayIntersection(rectangle, start, axis, z), start, axis, radiusSqr)

    private fun zSlabRayIntersection(rectangle: Rectangle, start: Vector3f, direction: Vector3f, z: Float): Vector3f {
        // 2d intersection of plane and segment
        val t = (z - start.z) / direction.z
        val x = clamp(start.x + t * direction.x, rectangle.minX, rectangle.maxX)
        return Vector3f(x, rectangle.minY, z)
    }

    /**
     * Based on Christer Ericsons's "Real-Time Collision Detection"
     * */
    private fun rayCylinderIntersection(
        point: Vector3f,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): HeightRange? {
        val m = Vector3f(point.x - start.x, point.y - start.y, point.z - start.z)
        // float[] n = { 0, 1, 0 };
        val md = m.dot(axis)
        // float nd = dot(n, d);
        val nd = axis.y
        val dd = axis.lengthSquared()

        // float nn = dot(n, n);
        val nn = 1f
        // float mn = dot(m, n);
        val mn = m.y
        // float a = dd * nn - nd * nd;
        val a = dd - nd * nd
        val k = m.lengthSquared() - radiusSqr
        val c = dd * k - md * md
        if (abs(a) < EPSILON) {
            // Segment runs parallel to cylinder axis
            if (c > 0f) {
                return null // ’a’ and thus the segment lie outside cylinder
            }
            // Now known that segment intersects cylinder; figure out how it intersects
            val t1 = -mn / nn // Intersect segment against ’p’ endcap
            val t2 = (nd - mn) / nn // Intersect segment against ’q’ endcap
            return HeightRange(point.y + min(t1, t2), point.y + max(t1, t2))
        }
        val b = dd * mn - nd * md
        val discr = b * b - a * c
        if (discr < 0f) {
            return null // No real roots; no intersection
        }
        val discSqrt = sqrt(discr)
        var t1 = (-b - discSqrt) / a
        var t2 = (-b + discSqrt) / a
        if (md + t1 * nd < 0f) {
            // Intersection outside cylinder on ’p’ side
            t1 = -md / nd
            if (k + t1 * (2 * mn + t1 * nn) > 0f) {
                return null
            }
        } else if (md + t1 * nd > dd) {
            // Intersection outside cylinder on ’q’ side
            t1 = (dd - md) / nd
            if (k + dd - 2 * md + t1 * (2 * (mn - nd) + t1 * nn) > 0f) {
                return null
            }
        }
        if (md + t2 * nd < 0f) {
            // Intersection outside cylinder on ’p’ side
            t2 = -md / nd
            if (k + t2 * (2 * mn + t2 * nn) > 0f) {
                return null
            }
        } else if (md + t2 * nd > dd) {
            // Intersection outside cylinder on ’q’ side
            t2 = (dd - md) / nd
            if (k + dd - 2 * md + t2 * (2 * (mn - nd) + t2 * nn) > 0f) {
                return null
            }
        }
        return HeightRange(point.y + min(t1, t2), point.y + max(t1, t2))
    }

    private fun mergeIntersections(s1: HeightRange?, s2: HeightRange?): HeightRange? {
        if (s1 == null && s2 == null) return null
        if (s1 == null) return s2
        return if (s2 == null) s1 else HeightRange(min(s1.minY, s2.minY), max(s1.maxY, s2.maxY))
    }
}