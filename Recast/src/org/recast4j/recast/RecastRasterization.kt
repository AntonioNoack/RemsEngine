/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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

import me.anno.maths.Maths.clamp
import org.joml.AABBf
import org.recast4j.Vectors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object RecastRasterization {
    /**
     * The span addition can be set to favor flags. If the span is merged to another span and the new 'smax' is within
     * 'flagMergeThr' units from the existing span, the span flags are merged.
     *
     * @see Heightfield, Span.
     */
    fun addSpan(hf: Heightfield, x: Int, y: Int, smin: Int, smax: Int, area: Int, flagMergeThr: Int) {
        val idx = x + y * hf.width
        val s = Span()
        s.min = smin
        s.max = smax
        s.area = area
        s.next = null

        // Empty cell, add the first span.
        if (hf.spans[idx] == null) {
            hf.spans[idx] = s
            return
        }
        var prev: Span? = null
        var cur = hf.spans[idx]

        // Insert and merge spans.
        while (cur != null) {
            if (cur.min > s.max) {
                // Current span is further than the new span, break.
                break
            } else if (cur.max < s.min) {
                // Current span is before the new span advance.
                prev = cur
                cur = cur.next
            } else {
                // Merge spans.
                if (cur.min < s.min) s.min = cur.min
                if (cur.max > s.max) s.max = cur.max

                // Merge flags.
                if (abs(s.max - cur.max) <= flagMergeThr) s.area = max(s.area, cur.area)

                // Remove current span.
                val next = cur.next
                if (prev != null) prev.next = next else hf.spans[idx] = next
                cur = next
            }
        }

        // Insert new span.
        if (prev != null) {
            s.next = prev.next
            prev.next = s
        } else {
            s.next = hf.spans[idx]
            hf.spans[idx] = s
        }
    }

    // divides a convex polygons into two convex polygons on both sides of a line
    private fun dividePoly(buf: FloatArray, `in`: Int, nin: Int, out1: Int, out2: Int, x: Float, axis: Int): IntArray {
        val d = FloatArray(12)
        for (i in 0 until nin) d[i] = x - buf[`in` + i * 3 + axis]
        var m = 0
        var n = 0
        var i = 0
        var j = nin - 1
        while (i < nin) {
            val ina = d[j] >= 0
            val inb = d[i] >= 0
            if (ina != inb) {
                val s = d[j] / (d[j] - d[i])
                buf[out1 + m * 3] = buf[`in` + j * 3] + (buf[`in` + i * 3] - buf[`in` + j * 3]) * s
                buf[out1 + m * 3 + 1] = buf[`in` + j * 3 + 1] + (buf[`in` + i * 3 + 1] - buf[`in` + j * 3 + 1]) * s
                buf[out1 + m * 3 + 2] = buf[`in` + j * 3 + 2] + (buf[`in` + i * 3 + 2] - buf[`in` + j * 3 + 2]) * s
                Vectors.copy(buf, out2 + n * 3, buf, out1 + m * 3)
                m++
                n++
                // add the i'th point to the right polygon. Do NOT add points that are on the dividing line
                // since these were already added above
                if (d[i] > 0) {
                    Vectors.copy(buf, out1 + m * 3, buf, `in` + i * 3)
                    m++
                } else if (d[i] < 0) {
                    Vectors.copy(buf, out2 + n * 3, buf, `in` + i * 3)
                    n++
                }
            } else  // same side
            {
                // add the i'th point to the right polygon. Addition is done even for points on the dividing line
                if (d[i] >= 0) {
                    Vectors.copy(buf, out1 + m * 3, buf, `in` + i * 3)
                    m++
                    if (d[i] != 0f) {
                        j = i
                        ++i
                        continue
                    }
                }
                Vectors.copy(buf, out2 + n * 3, buf, `in` + i * 3)
                n++
            }
            j = i
            ++i
        }
        return intArrayOf(m, n)
    }

    private fun rasterizeTriangle(
        vertices: FloatArray, v0: Int, v1: Int, v2: Int, area: Int, hf: Heightfield, bounds: AABBf,
        cs: Float, ics: Float, ich: Float, flagMergeThr: Int
    ) {
        val w = hf.width
        val h = hf.height
        val triBounds = AABBf()
        val deltaY = bounds.deltaY

        // Calculate the bounding box of the triangle.
        triBounds.union(vertices, v0 * 3)
        triBounds.union(vertices, v1 * 3)
        triBounds.union(vertices, v2 * 3)

        // If the triangle does not touch the bbox of the heightfield, skip the triangle.
        if (!bounds.testAABB(triBounds)) return

        // Calculate the footprint of the triangle on the grid's y-axis
        var y0 = ((triBounds.minZ - bounds.minZ) * ics).toInt()
        var y1 = ((triBounds.maxZ - bounds.minZ) * ics).toInt()
        // use -1 rather than 0 to cut the polygon properly at the start of the tile
        y0 = clamp(y0, -1, h - 1)
        y1 = clamp(y1, 0, h - 1)

        // Clip the triangle into all grid cells it touches.
        val buf = FloatArray(7 * 3 * 4)
        var `in` = 0
        var inrow = 7 * 3
        var p1 = inrow + 7 * 3
        var p2 = p1 + 7 * 3
        Vectors.copy(buf, 0, vertices, v0 * 3)
        Vectors.copy(buf, 3, vertices, v1 * 3)
        Vectors.copy(buf, 6, vertices, v2 * 3)
        var nvrow: Int
        var nvIn = 3
        for (y in y0..y1) {
            // Clip polygon to row. Store the remaining polygon as well
            val cz = bounds.minZ + y * cs
            val nvrowin = dividePoly(buf, `in`, nvIn, inrow, p1, cz + cs, 2)
            nvrow = nvrowin[0]
            nvIn = nvrowin[1]
            run {
                val temp = `in`
                `in` = p1
                p1 = temp
            }
            if (nvrow < 3 || y < 0) {
                continue
            }
            // find the horizontal bounds in the row
            var minX = buf[inrow]
            var maxX = buf[inrow]
            for (i in 1 until nvrow) {
                val v = buf[inrow + i * 3]
                minX = min(minX, v)
                maxX = max(maxX, v)
            }
            var x0 = ((minX - bounds.minX) * ics).toInt()
            var x1 = ((maxX - bounds.minX) * ics).toInt()
            if (x1 < 0 || x0 >= w) {
                continue
            }
            x0 = clamp(x0, -1, w - 1)
            x1 = clamp(x1, 0, w - 1)
            var nv: Int
            var nv2 = nvrow
            for (x in x0..x1) {
                // Clip polygon to column. store the remaining polygon as well
                val cx = bounds.minX + x * cs
                val nvnv2 = dividePoly(buf, inrow, nv2, p1, p2, cx + cs, 0)
                nv = nvnv2[0]
                nv2 = nvnv2[1]
                run {
                    val temp = inrow
                    inrow = p2
                    p2 = temp
                }
                if (nv < 3 || x < 0) {
                    continue
                }

                // Calculate min and max of the span.
                var smin = buf[p1 + 1]
                var smax = buf[p1 + 1]
                for (i in 1 until nv) {
                    smin = min(smin, buf[p1 + i * 3 + 1])
                    smax = max(smax, buf[p1 + i * 3 + 1])
                }
                smin -= bounds.minY
                smax -= bounds.minY
                // Skip the span if it is outside the heightfield bbox
                if (smax < 0f || smin > deltaY) continue
                // Clamp the span to the heightfield bbox.
                if (smin < 0f || smax > deltaY) smax = deltaY

                // Snap the span to the heightfield height grid.
                val ismin = clamp(floor((smin * ich)).toInt(), 0, RecastConstants.SPAN_MAX_HEIGHT)
                val ismax = clamp(ceil((smax * ich)).toInt(), ismin + 1, RecastConstants.SPAN_MAX_HEIGHT)
                addSpan(hf, x, y, ismin, ismax, area, flagMergeThr)
            }
        }
    }

    /**
     * No spans will be added if the triangle does not overlap the heightfield grid.
     *
     * @see Heightfield
     */
    fun rasterizeTriangle(
        solid: Heightfield, vertices: FloatArray, v0: Int, v1: Int, v2: Int, area: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_TRIANGLES)
        val ics = 1f / solid.cellSize
        val ich = 1f / solid.cellHeight
        rasterizeTriangle(vertices, v0, v1, v2, area, solid, solid.bounds, solid.cellSize, ics, ich, flagMergeThr)
        ctx?.stopTimer(TelemetryType.RASTERIZE_TRIANGLES)
    }

    /**
     * Spans will only be added for triangles that overlap the heightfield grid.
     *
     * @see Heightfield
     */
    fun rasterizeTriangles(
        solid: Heightfield, vertices: FloatArray, tris: IntArray, areas: IntArray, numTriangles: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_TRIANGLES)
        val invCellSize = 1f / solid.cellSize
        val invCellHeight = 1f / solid.cellHeight
        // Rasterize triangles.
        for (i in 0 until numTriangles) {
            val v0 = tris[i * 3]
            val v1 = tris[i * 3 + 1]
            val v2 = tris[i * 3 + 2]

            rasterizeTriangle(
                vertices, v0, v1, v2,
                areas[i], solid, solid.bounds, solid.cellSize,
                invCellSize, invCellHeight, flagMergeThr
            )
        }
        ctx?.stopTimer(TelemetryType.RASTERIZE_TRIANGLES)
    }

    /**
     * Spans will only be added for triangles that overlap the heightfield grid.
     *
     * @see Heightfield
     */
    fun rasterizeTriangles(
        solid: Heightfield, vertices: FloatArray, areas: IntArray, numTriangles: Int, flagMergeThr: Int,
        ctx: Telemetry?
    ) {
        ctx?.startTimer(TelemetryType.RASTERIZE_TRIANGLES)
        val ics = 1f / solid.cellSize
        val ich = 1f / solid.cellHeight
        // Rasterize triangles.
        for (i in 0 until numTriangles) {
            val v0 = i * 3
            val v1 = i * 3 + 1
            val v2 = i * 3 + 2
            // Rasterize.
            rasterizeTriangle(
                vertices, v0, v1, v2,
                areas[i], solid, solid.bounds, solid.cellSize,
                ics, ich, flagMergeThr
            )
        }
        ctx?.stopTimer(TelemetryType.RASTERIZE_TRIANGLES)
    }
}