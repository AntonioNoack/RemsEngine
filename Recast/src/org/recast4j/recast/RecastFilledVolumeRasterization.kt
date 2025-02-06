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
package org.recast4j.recast

import org.joml.Vector3f
import org.recast4j.Vectors
import org.recast4j.Vectors.clamp
import java.util.function.Function
import kotlin.math.*

object RecastFilledVolumeRasterization {

    private const val EPSILON = 0.00001f
    private val BOX_EDGES = intArrayOf(0, 1, 0, 2, 0, 4, 1, 3, 1, 5, 2, 3, 2, 6, 3, 7, 4, 5, 4, 6, 5, 7, 6, 7)

    fun rasterizeSphere(
        hf: Heightfield,
        center: Vector3f,
        radius: Float,
        area: Int,
        flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_SPHERE)
        val bounds = floatArrayOf(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        )
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: FloatArray -> intersectSphere(rectangle, center, radius * radius) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_SPHERE)
    }

    fun rasterizeCapsule(
        hf: Heightfield, start: Vector3f, end: Vector3f, radius: Float, area: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_CAPSULE)
        val bounds = floatArrayOf(
            min(start.x, end.x) - radius, min(start.y, end.y) - radius,
            min(start.z, end.z) - radius, max(start.x, end.x) + radius, max(start.y, end.y) + radius,
            max(start.z, end.z) + radius
        )
        val axis = Vector3f(end).sub(start)
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: FloatArray -> intersectCapsule(rectangle, start, end, axis, radius * radius) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CAPSULE)
    }

    fun rasterizeCylinder(
        hf: Heightfield, start: Vector3f, end: Vector3f, radius: Float, area: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_CYLINDER)
        val bounds = floatArrayOf(
            min(start.x, end.x) - radius, min(start.y, end.y) - radius, min(start.z, end.z) - radius,
            max(start.x, end.x) + radius, max(start.y, end.y) + radius, max(start.z, end.z) + radius
        )
        val axis = Vector3f(end).sub(start)
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: FloatArray -> intersectCylinder(rectangle, start, end, axis, radius * radius) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CYLINDER)
    }

    fun rasterizeBox(
        hf: Heightfield,
        center: Vector3f,
        halfEdges: Array<FloatArray>,
        area: Int,
        flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_BOX)
        val normals = arrayOf(
            Vector3f(halfEdges[0]),
            Vector3f(halfEdges[1]),
            Vector3f(halfEdges[2]),
        )
        normals[0].normalize()
        normals[1].normalize()
        normals[2].normalize()
        val vertices = FloatArray(8 * 3)
        val bounds = floatArrayOf(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )
        for (i in 0..7) {
            val s0 = if (i and 1 != 0) 1f else -1f
            val s1 = if (i and 2 != 0) 1f else -1f
            val s2 = if (i and 4 != 0) 1f else -1f
            val he0 = halfEdges[0]
            val he1 = halfEdges[1]
            val he2 = halfEdges[0]
            val i3 = i * 3
            vertices[i3] = center.x + s0 * he0[0] + s1 * he1[0] + s2 * he2[0]
            vertices[i3 + 1] = center.y + s0 * he0[1] + s1 * he1[1] + s2 * he2[1]
            vertices[i3 + 2] = center.z + s0 * he0[2] + s1 * he1[2] + s2 * he2[2]
            bounds[0] = min(bounds[0], vertices[i3])
            bounds[1] = min(bounds[1], vertices[i3 + 1])
            bounds[2] = min(bounds[2], vertices[i3 + 2])
            bounds[3] = max(bounds[3], vertices[i3])
            bounds[4] = max(bounds[4], vertices[i3 + 1])
            bounds[5] = max(bounds[5], vertices[i3 + 2])
        }
        val planes = Array(6) { FloatArray(4) }
        for (i in 0..5) {
            val m = if (i < 3) -1f else 1f
            val vi = if (i < 3) 0 else 7
            val i3 = if (i > 3) i - 3 else i
            val plane = planes[i]
            val normal = normals[i3]
            plane[0] = m * normal.x
            plane[1] = m * normal.y
            plane[2] = m * normal.z
            val vi3 = vi * 3
            plane[3] = vertices[vi3] * plane[0] + vertices[vi3 + 1] * plane[1] + vertices[vi3 + 2] * plane[2]
        }
        rasterizationFilledShape(hf, bounds, area, flagMergeThr) { rectangle: FloatArray ->
            intersectBox(
                rectangle,
                vertices,
                planes
            )
        }
        ctx?.stopTimer(TelemetryType.RASTERIZE_BOX)
    }

    fun rasterizeConvex(
        hf: Heightfield,
        vertices: FloatArray,
        triangles: IntArray,
        area: Int,
        flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_CONVEX)
        val bounds = floatArrayOf(vertices[0], vertices[1], vertices[2], vertices[0], vertices[1], vertices[2])
        run {
            var i = 0
            while (i < vertices.size) {
                bounds[0] = min(bounds[0], vertices[i])
                bounds[1] = min(bounds[1], vertices[i + 1])
                bounds[2] = min(bounds[2], vertices[i + 2])
                bounds[3] = max(bounds[3], vertices[i])
                bounds[4] = max(bounds[4], vertices[i + 1])
                bounds[5] = max(bounds[5], vertices[i + 2])
                i += 3
            }
        }
        val planes = Array(triangles.size) { FloatArray(4) }
        val triBounds = Array(triangles.size / 3) { FloatArray(4) }
        var i = 0
        var j = 0
        while (i < triangles.size) {
            val a = triangles[i] * 3
            val b = triangles[i + 1] * 3
            val c = triangles[i + 2] * 3
            val ab = Vector3f(
                vertices[b] - vertices[a],
                vertices[b + 1] - vertices[a + 1],
                vertices[b + 2] - vertices[a + 2]
            )
            val ac = Vector3f(
                vertices[c] - vertices[a],
                vertices[c + 1] - vertices[a + 1],
                vertices[c + 2] - vertices[a + 2]
            )
            val bc = Vector3f(
                vertices[c] - vertices[b],
                vertices[c + 1] - vertices[b + 1],
                vertices[c + 2] - vertices[b + 2]
            )
            val ca = Vector3f(
                vertices[a] - vertices[c],
                vertices[a + 1] - vertices[c + 1],
                vertices[a + 2] - vertices[c + 2]
            )
            plane(planes, i, ab, ac, vertices, a)
            plane(planes, i + 1, Vector3f(planes[i]), bc, vertices, b)
            plane(planes, i + 2, Vector3f(planes[i]), ca, vertices, c)
            var s =
                1f / (vertices[a] * planes[i + 1][0] + vertices[a + 1] * planes[i + 1][1] + vertices[a + 2] * planes[i + 1][2] - planes[i + 1][3])
            planes[i + 1][0] *= s
            planes[i + 1][1] *= s
            planes[i + 1][2] *= s
            planes[i + 1][3] *= s
            s =
                1f / (vertices[b] * planes[i + 2][0] + vertices[b + 1] * planes[i + 2][1] + vertices[b + 2] * planes[i + 2][2]
                        - planes[i + 2][3])
            planes[i + 2][0] *= s
            planes[i + 2][1] *= s
            planes[i + 2][2] *= s
            planes[i + 2][3] *= s
            triBounds[j][0] = min(min(vertices[a], vertices[b]), vertices[c])
            triBounds[j][1] = min(min(vertices[a + 2], vertices[b + 2]), vertices[c + 2])
            triBounds[j][2] = max(max(vertices[a], vertices[b]), vertices[c])
            triBounds[j][3] = max(max(vertices[a + 2], vertices[b + 2]), vertices[c + 2])
            i += 3
            j++
        }
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle: FloatArray -> intersectConvex(rectangle, triangles, vertices, planes, triBounds) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CONVEX)
    }

    private fun plane(planes: Array<FloatArray>, p: Int, v1: Vector3f, v2: Vector3f, vertices: FloatArray, vert: Int) {
        val tmp = Vector3f()
        v1.cross(v2, tmp)
        tmp.set(planes[p])
        planes[p][3] =
            planes[p][0] * vertices[vert] + planes[p][1] * vertices[vert + 1] + planes[p][2] * vertices[vert + 2]
    }

    private fun rasterizationFilledShape(
        hf: Heightfield,
        bounds: FloatArray,
        area: Int,
        flagMergeThr: Int,
        intersection: Function<FloatArray, FloatArray?>
    ) {
        if (!overlapBounds(hf.bmin, hf.bmax, bounds)) {
            return
        }
        bounds[3] = min(bounds[3], hf.bmax.x)
        bounds[5] = min(bounds[5], hf.bmax.z)
        bounds[0] = max(bounds[0], hf.bmin.x)
        bounds[2] = max(bounds[2], hf.bmin.z)
        if (bounds[3] <= bounds[0] || bounds[4] <= bounds[1] || bounds[5] <= bounds[2]) {
            return
        }
        val ics = 1f / hf.cellSize
        val ich = 1f / hf.cellHeight
        val xMin = ((bounds[0] - hf.bmin.x) * ics).toInt()
        val zMin = ((bounds[2] - hf.bmin.z) * ics).toInt()
        val xMax = min(hf.width - 1, ((bounds[3] - hf.bmin.x) * ics).toInt())
        val zMax = min(hf.height - 1, ((bounds[5] - hf.bmin.z) * ics).toInt())
        val rectangle = FloatArray(5)
        rectangle[4] = hf.bmin.y
        for (x in xMin..xMax) {
            for (z in zMin..zMax) {
                rectangle[0] = x * hf.cellSize + hf.bmin.x
                rectangle[1] = z * hf.cellSize + hf.bmin.z
                rectangle[2] = rectangle[0] + hf.cellSize
                rectangle[3] = rectangle[1] + hf.cellSize
                val h = intersection.apply(rectangle)
                if (h != null) {
                    val smin = floor(((h[0] - hf.bmin.y) * ich)).toInt()
                    val smax = ceil(((h[1] - hf.bmin.y) * ich)).toInt()
                    if (smin != smax) {
                        val ismin: Int = clamp(smin, 0, RecastConstants.SPAN_MAX_HEIGHT)
                        val ismax: Int = clamp(smax, ismin + 1, RecastConstants.SPAN_MAX_HEIGHT)
                        RecastRasterization.addSpan(hf, x, z, ismin, ismax, area, flagMergeThr)
                    }
                }
            }
        }
    }

    private fun intersectSphere(rectangle: FloatArray, center: Vector3f, radiusSqr: Float): FloatArray? {
        val x = max(rectangle[0], min(center.x, rectangle[2]))
        val y = rectangle[4]
        val z = max(rectangle[1], min(center.z, rectangle[3]))
        val mx = x - center.x
        val my = y - center.y
        val mz = z - center.z
        val c = lenSqr(mx, my, mz) - radiusSqr
        if (c > 0f && my > 0f) {
            return null
        }
        val discr = my * my - c
        if (discr < 0f) {
            return null
        }
        val discrSqrt = sqrt(discr)
        var tmin = -my - discrSqrt
        val tmax = -my + discrSqrt
        if (tmin < 0f) {
            tmin = 0f
        }
        return floatArrayOf(y + tmin, y + tmax)
    }

    private fun intersectCapsule(
        rectangle: FloatArray,
        start: Vector3f,
        end: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): FloatArray? {
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
        rectangle: FloatArray,
        start: Vector3f,
        end: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): FloatArray? {
        var s = rayCylinderIntersection(
            Vector3f(
                clamp(start.x, rectangle[0], rectangle[2]), rectangle[4],
                clamp(start.z, rectangle[1], rectangle[3])
            ), start, axis, radiusSqr
        )
        s = mergeIntersections(
            s, rayCylinderIntersection(
                Vector3f(
                    clamp(end.x, rectangle[0], rectangle[2]), rectangle[4],
                    clamp(end.z, rectangle[1], rectangle[3])
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
                val x = rectangle[i + 1 and 2]
                val z = rectangle[(i and 2) + 1]
                val dotAxisA = axis.dot(x, rectangle[4], z)
                var t = (ds - dotAxisA) / axis.y
                rectangleOnStartPlane[i].set(x, rectangle[4] + t, z)
                t = (de - dotAxisA) / axis.y
                rectangleOnEndPlane[i].set(x, rectangle[4] + t, z)
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
        s: FloatArray?, i: Int,
        rectangleOnPlane: Array<Vector3f>
    ): FloatArray? {
        val j = (i + 1) % 4
        // Ray against sphere intersection
        val m = rectangleOnPlane[i].sub(start, Vector3f())
        val d = rectangleOnPlane[j].sub(rectangleOnPlane[i], Vector3f())
        val dl = d.lengthSquared()
        val b = m.dot(d) / dl
        val c = (m.lengthSquared() - radiusSqr) / dl
        val discr = b * b - c
        if (discr > EPSILON) {
            val discrSqrt = sqrt(discr).toFloat()
            var t1 = -b - discrSqrt
            var t2 = -b + discrSqrt
            if (t1 <= 1 && t2 >= 0) {
                t1 = max(0f, t1)
                t2 = min(1f, t2)
                val y1 = rectangleOnPlane[i][1] + t1 * d[1]
                val y2 = rectangleOnPlane[i][1] + t2 * d[1]
                val y = floatArrayOf(min(y1, y2), max(y1, y2))
                return mergeIntersections(s, y)
            }
        }
        return s
    }

    private fun slabsCylinderIntersection(
        rectangle: FloatArray, start: Vector3f, end: Vector3f, axis: Vector3f, radiusSqr: Float,
        s0: FloatArray?
    ): FloatArray? {
        var s = s0
        if (min(start.x, end.x) < rectangle[0]) {
            s = mergeIntersections(s, xSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle[0]))
        }
        if (max(start.x, end.x) > rectangle[2]) {
            s = mergeIntersections(s, xSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle[2]))
        }
        if (min(start.z, end.z) < rectangle[1]) {
            s = mergeIntersections(s, zSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle[1]))
        }
        if (max(start.z, end.z) > rectangle[3]) {
            s = mergeIntersections(s, zSlabCylinderIntersection(rectangle, start, axis, radiusSqr, rectangle[3]))
        }
        return s
    }

    private fun xSlabCylinderIntersection(
        rectangle: FloatArray,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float,
        x: Float
    ) = rayCylinderIntersection(xSlabRayIntersection(rectangle, start, axis, x), start, axis, radiusSqr)

    private fun xSlabRayIntersection(rectangle: FloatArray, start: Vector3f, direction: Vector3f, x: Float): Vector3f {
        // 2d intersection of plane and segment
        val t = (x - start.x) / direction.x
        val z = clamp(start.z + t * direction.z, rectangle[1], rectangle[3])
        return Vector3f(x, rectangle[4], z)
    }

    private fun zSlabCylinderIntersection(
        rectangle: FloatArray,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float,
        z: Float
    ): FloatArray? {
        return rayCylinderIntersection(zSlabRayIntersection(rectangle, start, axis, z), start, axis, radiusSqr)
    }

    private fun zSlabRayIntersection(rectangle: FloatArray, start: Vector3f, direction: Vector3f, z: Float): Vector3f {
        // 2d intersection of plane and segment
        val t = (z - start.z) / direction.z
        val x = clamp(start.x + t * direction.x, rectangle[0], rectangle[2])
        return Vector3f(x, rectangle[4], z)
    }

    /**
     * Based on Christer Ericsons's "Real-Time Collision Detection"
     * */
    private fun rayCylinderIntersection(
        point: Vector3f,
        start: Vector3f,
        axis: Vector3f,
        radiusSqr: Float
    ): FloatArray? {
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
            return floatArrayOf(point.y + min(t1, t2), point.y + max(t1, t2))
        }
        val b = dd * mn - nd * md
        val discr = b * b - a * c
        if (discr < 0f) {
            return null // No real roots; no intersection
        }
        val discSqrt = sqrt(discr).toFloat()
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
        return floatArrayOf(point.y + min(t1, t2), point.y + max(t1, t2))
    }

    private fun intersectBox(rectangle: FloatArray, vertices: FloatArray, planes: Array<FloatArray>): FloatArray? {
        var yMin = Float.POSITIVE_INFINITY
        var yMax = Float.NEGATIVE_INFINITY
        // check intersection with rays starting in box vertices first
        for (i in 0..7) {
            val vi = i * 3
            if (vertices[vi] >= rectangle[0] && vertices[vi] < rectangle[2] && vertices[vi + 2] >= rectangle[1] && vertices[vi + 2] < rectangle[3]) {
                yMin = min(yMin, vertices[vi + 1])
                yMax = max(yMax, vertices[vi + 1])
            }
        }

        // check intersection with rays starting in rectangle vertices
        val point = Vector3f(0f, rectangle[1], 0f)
        for (i in 0..3) {
            point.x = if (i and 1 == 0) rectangle[0] else rectangle[2]
            point.z = if (i and 2 == 0) rectangle[1] else rectangle[3]
            for (j in 0..5) {
                if (abs(planes[j][1]) > EPSILON) {
                    val plane = planes[j]
                    val dotNormalPoint = point.dot(plane[0], plane[1], plane[2])
                    val t = (planes[j][3] - dotNormalPoint) / planes[j][1]
                    val y = point.y + t
                    var valid = true
                    for (k in 0..5) {
                        if (k != j) {
                            if (point.x * planes[k][0] + y * planes[k][1] + point.z * planes[k][2] > planes[k][3]) {
                                valid = false
                                break
                            }
                        }
                    }
                    if (valid) {
                        yMin = min(yMin, y)
                        yMax = max(yMax, y)
                    }
                }
            }
        }

        // check intersection with box edges
        var i = 0
        while (i < BOX_EDGES.size) {
            val vi = BOX_EDGES[i] * 3
            val vj = BOX_EDGES[i + 1] * 3
            val x = vertices[vi]
            val z = vertices[vi + 2]
            // edge slab intersection
            val y = vertices[vi + 1]
            val dx = vertices[vj] - x
            val dy = vertices[vj + 1] - y
            val dz = vertices[vj + 2] - z
            if (abs(dx) > EPSILON) {
                var iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[0])
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
                iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[2])
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
            }
            if (abs(dz) > EPSILON) {
                var iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[1])
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
                iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[3])
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
            }
            i += 2
        }
        return if (yMin <= yMax) {
            floatArrayOf(yMin, yMax)
        } else null
    }

    private fun intersectConvex(
        rectangle: FloatArray, triangles: IntArray, vertices: FloatArray, planes: Array<FloatArray>,
        triBounds: Array<FloatArray>
    ): FloatArray? {
        var imin = Float.POSITIVE_INFINITY
        var imax = Float.NEGATIVE_INFINITY
        var tr = 0
        var tri = 0
        while (tri < triangles.size) {
            if (triBounds[tr][0] > rectangle[2] ||
                triBounds[tr][2] < rectangle[0] ||
                triBounds[tr][1] > rectangle[3] ||
                triBounds[tr][3] < rectangle[1]
            ) {
                tr++
                tri += 3
                continue
            }
            if (abs(planes[tri][1]) < EPSILON) {
                tr++
                tri += 3
                continue
            }
            for (i in 0..2) {
                val vi = triangles[tri + i] * 3
                val vj = triangles[tri + (i + 1) % 3] * 3
                val x = vertices[vi]
                val z = vertices[vi + 2]
                // triangle vertex
                if (x >= rectangle[0] && x <= rectangle[2] && z >= rectangle[1] && z <= rectangle[3]) {
                    imin = min(imin, vertices[vi + 1])
                    imax = max(imax, vertices[vi + 1])
                }
                // triangle slab intersection
                val y = vertices[vi + 1]
                val dx = vertices[vj] - x
                val dy = vertices[vj + 1] - y
                val dz = vertices[vj + 2] - z
                if (abs(dx) > EPSILON) {
                    var iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[0])
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                    iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[2])
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                }
                if (abs(dz) > EPSILON) {
                    var iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[1])
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                    iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle[3])
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                }
            }
            // rectangle vertex
            val point = floatArrayOf(0f, rectangle[1], 0f)
            for (i in 0..3) {
                point[0] = if (i and 1 == 0) rectangle[0] else rectangle[2]
                point[2] = if (i and 2 == 0) rectangle[1] else rectangle[3]
                val y = rayTriangleIntersection(point, tri, planes)
                if (!y.isNaN()) {
                    imin = min(imin, y)
                    imax = max(imax, y)
                }
            }
            tr++
            tri += 3
        }
        return if (imin < imax) {
            floatArrayOf(imin, imax)
        } else null
    }

    private fun xSlabSegmentIntersection(
        rectangle: FloatArray, x: Float, y: Float, z: Float, dx: Float, dy: Float, dz: Float,
        slabX: Float
    ): Float {
        val x2 = x + dx
        if ((x < slabX && x2 > slabX || x > slabX) && x2 < slabX) {
            val t = (slabX - x) / dx
            val iz = z + dz * t
            if (iz >= rectangle[1] && iz <= rectangle[3]) {
                return y + dy * t
            }
        }
        return Float.NaN
    }

    private fun zSlabSegmentIntersection(
        rectangle: FloatArray, x: Float, y: Float, z: Float, dx: Float, dy: Float, dz: Float,
        slabZ: Float
    ): Float {
        val z2 = z + dz
        if (z < slabZ && z2 > slabZ || z > slabZ && z2 < slabZ) {
            val t = (slabZ - z) / dz
            val ix = x + dx * t
            if (ix >= rectangle[0] && ix <= rectangle[2]) {
                return y + dy * t
            }
        }
        return Float.NaN
    }

    private fun rayTriangleIntersection(point: FloatArray, plane: Int, planes: Array<FloatArray>): Float {
        val t = (planes[plane][3] - Vectors.dot(planes[plane], point)) / planes[plane][1]
        val s = floatArrayOf(point[0], point[1] + t, point[2])
        val u = Vectors.dot(s, planes[plane + 1]) - planes[plane + 1][3]
        if (u < 0f || u > 1f) return Float.NaN
        val v = Vectors.dot(s, planes[plane + 2]) - planes[plane + 2][3]
        if (v < 0f) return Float.NaN
        val w = 1f - u - v
        return if (w < 0f) Float.NaN else s[1]
    }

    private fun mergeIntersections(s1: FloatArray?, s2: FloatArray?): FloatArray? {
        if (s1 == null && s2 == null) return null
        if (s1 == null) return s2
        return if (s2 == null) s1 else floatArrayOf(min(s1[0], s2[0]), max(s1[1], s2[1]))
    }

    private fun lenSqr(dx: Float, dy: Float, dz: Float): Float {
        return dx * dx + dy * dy + dz * dz
    }

    private fun overlapBounds(amin: Vector3f, amax: Vector3f, bounds: FloatArray): Boolean {
        return amin.x in bounds[0]..bounds[3] &&
                amin.y <= bounds[4] &&
                amax.z in bounds[2]..bounds[5]
    }
}