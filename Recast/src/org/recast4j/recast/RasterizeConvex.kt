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

import org.joml.AABBf
import org.joml.Vector3f
import org.recast4j.recast.RasterizeBox.xSlabSegmentIntersection
import org.recast4j.recast.RasterizeBox.zSlabSegmentIntersection
import org.recast4j.recast.RasterizeSphere.rasterizationFilledShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RasterizeConvex {

    private const val EPSILON = 0.00001f

    private fun Vector3f.sub(f: FloatArray, i: Int) {
        sub(f[i], f[i + 1], f[i + 2])
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
        val bounds = AABBf()
        for (i in 0 until vertices.size / 3) {
            bounds.union(vertices, i * 3)
        }
        val planes = FloatArray(triangles.size * 4)
        val triBounds = FloatArray(triangles.size / 3 * 4)
        var i = 0
        var j = 0
        val ab = Vector3f()
        val ac = Vector3f()
        val bc = Vector3f()
        val ca = Vector3f()
        val v1 = Vector3f()
        while (i < triangles.size) {
            val a = triangles[i] * 3
            val b = triangles[i + 1] * 3
            val c = triangles[i + 2] * 3
            ab.set(vertices, b).sub(vertices, a)
            ac.set(vertices, c).sub(vertices, a)
            bc.set(vertices, c).sub(vertices, b)
            ca.set(vertices, a).sub(vertices, c)
            plane(planes, i, ab, ac, vertices, a)
            v1.set(planes, i * 4)
            plane(planes, i + 1, v1, bc, vertices, b)
            plane(planes, i + 2, v1, ca, vertices, c)
            scalePlane(planes, i + 1, vertices, a)
            scalePlane(planes, i + 2, vertices, b)
            triBounds[j * 4 + 0] = min(min(vertices[a], vertices[b]), vertices[c])
            triBounds[j * 4 + 1] = min(min(vertices[a + 2], vertices[b + 2]), vertices[c + 2])
            triBounds[j * 4 + 2] = max(max(vertices[a], vertices[b]), vertices[c])
            triBounds[j * 4 + 3] = max(max(vertices[a + 2], vertices[b + 2]), vertices[c + 2])
            i += 3
            j++
        }
        rasterizationFilledShape(
            hf, bounds, area, flagMergeThr
        ) { rectangle, dst -> intersectConvex(rectangle, dst, triangles, vertices, planes, triBounds) }
        ctx?.stopTimer(TelemetryType.RASTERIZE_CONVEX)
    }

    private fun scalePlane(
        planes: FloatArray, p: Int,
        vertices: FloatArray, a: Int
    ) {
        val p4 = p * 4
        val s = 1f / (vertices[a] * planes[p4 + 0] +
                vertices[a + 1] * planes[p4 + 1] +
                vertices[a + 2] * planes[p4 + 2] -
                planes[p4 + 3])
        planes[p4 + 0] *= s
        planes[p4 + 1] *= s
        planes[p4 + 2] *= s
        planes[p4 + 3] *= s
    }

    private fun plane(planes: FloatArray, p: Int, v1: Vector3f, v2: Vector3f, vertices: FloatArray, vert: Int) {
        val tmp = Vector3f()
        v1.cross(v2, tmp)
        val p4 = p * 4
        tmp.set(planes[p4], planes[p4 + 1], planes[p4 + 2])
        planes[p4 + 3] = planes[p4] * vertices[vert] +
                planes[p4 + 1] * vertices[vert + 1] +
                planes[p4 + 2] * vertices[vert + 2]
    }

    private fun intersectConvex(
        rectangle: Rectangle, dst: HeightRange,
        triangles: IntArray, vertices: FloatArray,
        planes: FloatArray,
        triBounds: FloatArray
    ): Boolean {
        var imin = Float.POSITIVE_INFINITY
        var imax = Float.NEGATIVE_INFINITY
        var tr = 0
        var tri = 0
        while (tri < triangles.size) {
            if (triBounds[tr * 4 + 0] > rectangle.maxX ||
                triBounds[tr * 4 + 2] < rectangle.minX ||
                triBounds[tr * 4 + 1] > rectangle.maxZ ||
                triBounds[tr * 4 + 3] < rectangle.minZ
            ) {
                tr++
                tri += 3
                continue
            }
            if (abs(planes[tri * 4 + 1]) < EPSILON) {
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
                if (x >= rectangle.minX && x <= rectangle.maxX &&
                    z >= rectangle.minZ && z <= rectangle.maxZ
                ) {
                    imin = min(imin, vertices[vi + 1])
                    imax = max(imax, vertices[vi + 1])
                }
                // triangle slab intersection
                val y = vertices[vi + 1]
                val dx = vertices[vj] - x
                val dy = vertices[vj + 1] - y
                val dz = vertices[vj + 2] - z
                if (abs(dx) > EPSILON) {
                    var iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.minX)
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                    iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.maxX)
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                }
                if (abs(dz) > EPSILON) {
                    var iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.minZ)
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                    iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.maxZ)
                    if (!iy.isNaN()) {
                        imin = min(imin, iy)
                        imax = max(imax, iy)
                    }
                }
            }
            // rectangle vertex
            val point = Vector3f(0f, rectangle.minY, 0f)
            for (i in 0..3) {
                point.x = if (i and 1 == 0) rectangle.minX else rectangle.maxX
                point.z = if (i and 2 == 0) rectangle.minZ else rectangle.maxZ
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
            dst.set(imin, imax)
        } else false
    }

    private fun rayTriangleIntersection(point: Vector3f, plane: Int, planes: FloatArray): Float {
        val t = -dot(planes, plane, point) / planes[plane * 4 + 1]
        val s = Vector3f(point[0], point[1] + t, point[2])
        val u = dot(planes, plane + 1, s)
        if (u !in 0f..1f) return Float.NaN
        val v = dot(planes, plane + 2, s)
        if (v < 0f) return Float.NaN
        val w = 1f - u - v
        return if (w < 0f) Float.NaN else s[1]
    }

    private fun dot(v: FloatArray, i: Int, p: Vector3f): Float {
        val i4 = i * 4
        return p.dot(v[i4], v[i4 + 1], v[i4 + 2]) - v[i4 + 3]
    }
}