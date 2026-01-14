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
import org.joml.Planef
import org.joml.Vector3f
import org.recast4j.recast.RasterizeSphere.rasterizationFilledShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RasterizeBox {

    private const val EPSILON = 0.00001f
    private val BOX_EDGES = intArrayOf(
        0, 1,
        0, 2,
        0, 4,
        1, 3,
        1, 5,
        2, 3,
        2, 6,
        3, 7,
        4, 5,
        4, 6,
        5, 7,
        6, 7
    )

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
        val bounds = AABBf()
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
            bounds.minX = min(bounds.minX, vertices[i3])
            bounds.minY = min(bounds.minY, vertices[i3 + 1])
            bounds.minZ = min(bounds.minZ, vertices[i3 + 2])
            bounds.maxX = max(bounds.maxX, vertices[i3])
            bounds.maxY = max(bounds.maxY, vertices[i3 + 1])
            bounds.maxZ = max(bounds.maxZ, vertices[i3 + 2])
        }
        val planes = Array(6) { Planef() }
        for (i in 0..5) {
            val m = if (i < 3) -1f else 1f
            val vi = if (i < 3) 0 else 7
            val i3 = if (i > 3) i - 3 else i
            val plane = planes[i]
            val normal = normals[i3]
            plane.dirX = m * normal.x
            plane.dirY = m * normal.y
            plane.dirZ = m * normal.z
            val vi3 = vi * 3
            plane.distance = vertices[vi3] * plane.dirX +
                    vertices[vi3 + 1] * plane.dirY +
                    vertices[vi3 + 2] * plane.dirZ
        }
        rasterizationFilledShape(hf, bounds, area, flagMergeThr) { rectangle, dst ->
            intersectBox(rectangle, dst, vertices, planes)
        }
        ctx?.stopTimer(TelemetryType.RASTERIZE_BOX)
    }

    private fun intersectBox(
        rectangle: Rectangle,
        dst: HeightRange,
        vertices: FloatArray,
        planes: Array<Planef>
    ): Boolean {
        var yMin = Float.POSITIVE_INFINITY
        var yMax = Float.NEGATIVE_INFINITY
        // check intersection with rays starting in box vertices first
        for (i in 0..7) {
            val vi = i * 3
            if (vertices[vi] >= rectangle.minX && vertices[vi] < rectangle.maxX &&
                vertices[vi + 2] >= rectangle.minZ && vertices[vi + 2] < rectangle.maxZ
            ) {
                yMin = min(yMin, vertices[vi + 1])
                yMax = max(yMax, vertices[vi + 1])
            }
        }

        // check intersection with rays starting in rectangle vertices
        val point = Vector3f(0f, rectangle.minY, 0f)
        for (i in 0..3) {
            point.x = if (i and 1 == 0) rectangle.minX else rectangle.maxX
            point.z = if (i and 2 == 0) rectangle.minZ else rectangle.maxZ
            for (j in 0..5) {
                if (abs(planes[j].dirY) > EPSILON) {
                    val plane = planes[j]
                    val dotNormalPoint = point.dot(plane.dirX, plane.dirY, plane.dirZ)
                    val t = (planes[j].distance - dotNormalPoint) / planes[j].dirY
                    val y = point.y + t
                    var valid = true
                    for (k in 0..5) {
                        if (k != j) {
                            if (point.x * planes[k].dirX + y * planes[k].dirY + point.z * planes[k].dirZ > planes[k].distance) {
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
                var iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.minX)
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
                iy = xSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.maxX)
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
            }
            if (abs(dz) > EPSILON) {
                var iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.minZ)
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
                iy = zSlabSegmentIntersection(rectangle, x, y, z, dx, dy, dz, rectangle.maxZ)
                if (!iy.isNaN()) {
                    yMin = min(yMin, iy)
                    yMax = max(yMax, iy)
                }
            }
            i += 2
        }
        return if (yMin <= yMax) {
            dst.set(yMin, yMax)
        } else false
    }

    fun xSlabSegmentIntersection(
        rectangle: Rectangle,
        x: Float, y: Float, z: Float,
        dx: Float, dy: Float, dz: Float,
        slabX: Float
    ): Float {
        val x2 = x + dx
        if ((x < slabX && x2 > slabX || x > slabX) && x2 < slabX) {
            val t = (slabX - x) / dx
            val iz = z + dz * t
            if (iz >= rectangle.minZ && iz <= rectangle.maxZ) {
                return y + dy * t
            }
        }
        return Float.NaN
    }

    fun zSlabSegmentIntersection(
        rectangle: Rectangle,
        x: Float, y: Float, z: Float,
        dx: Float, dy: Float, dz: Float,
        slabZ: Float
    ): Float {
        val z2 = z + dz
        if (z < slabZ && z2 > slabZ || z > slabZ && z2 < slabZ) {
            val t = (slabZ - z) / dz
            val ix = x + dx * t
            if (ix >= rectangle.minX && ix <= rectangle.maxX) {
                return y + dy * t
            }
        }
        return Float.NaN
    }
}