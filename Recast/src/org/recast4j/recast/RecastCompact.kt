/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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

import org.recast4j.Vectors
import org.recast4j.recast.RecastCommon.getDirOffsetX
import org.recast4j.recast.RecastCommon.getDirOffsetY
import org.recast4j.recast.RecastCommon.setCon
import org.recast4j.recast.RecastConstants.RC_NOT_CONNECTED
import org.recast4j.recast.RecastConstants.RC_NULL_AREA
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RecastCompact {
    private val MAX_LAYERS: Int = RC_NOT_CONNECTED - 1
    private val MAX_HEIGHT = RecastConstants.SPAN_MAX_HEIGHT

    /**
     * This is just the beginning of the process of fully building a compact heightfield.
     * Various filters may be applied, then the distance field and regions built.
     * E.g: rcBuildDistanceField and rcBuildRegions
     */
    fun buildCompactHeightfield(
        ctx: Telemetry?, walkableHeight: Int, walkableClimb: Int,
        hf: Heightfield
    ): CompactHeightfield {
        ctx?.startTimer(TelemetryType.BUILD_COMPACTHEIGHTFIELD)
        val w = hf.width
        val h = hf.height
        val spanCount = getHeightFieldSpanCount(hf)
        val result = CompactHeightfield(w, h, spanCount)
        fillInHeader(result, hf, walkableHeight, walkableClimb)
        fillInCellsAndSpans(result, w, h, hf)
        findNeighborConnections(result, w, h, walkableHeight, walkableClimb)
        ctx?.stopTimer(TelemetryType.BUILD_COMPACTHEIGHTFIELD)
        return result
    }

    private fun fillInHeader(chf: CompactHeightfield, hf: Heightfield, walkableHeight: Int, walkableClimb: Int) {
        chf.borderSize = hf.borderSize
        chf.walkableHeight = walkableHeight
        chf.walkableClimb = walkableClimb
        chf.maxRegions = 0
        chf.bmin.set(hf.bmin)
        chf.bmax.set(hf.bmax)
        chf.bmax.y = chf.bmax.y + walkableHeight * hf.cellHeight
        chf.cellSize = hf.cellSize
        chf.cellHeight = hf.cellHeight
    }

    private fun fillInCellsAndSpans(chf: CompactHeightfield, w: Int, h: Int, hf: Heightfield) {
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s: Span? = hf.spans[x + y * w] ?: continue
                // If there are no spans at this cell, just leave the data to index=0, count=0.
                val c = x + y * w
                chf.index[c] = idx
                chf.endIndex[c] = idx
                while (s != null) {
                    if (s.area != RC_NULL_AREA) {
                        val bot = s.max
                        val top = s.next?.min ?: MAX_HEIGHT
                        chf.spans[idx].y = Vectors.clamp(bot, 0, MAX_HEIGHT)
                        chf.spans[idx].height = Vectors.clamp(top - bot, 0, MAX_HEIGHT)
                        chf.areas[idx] = s.area
                        idx++
                        chf.endIndex[c]++
                    }
                    s = s.next
                }
            }
        }
    }

    private fun findNeighborConnections(
        chf: CompactHeightfield, w: Int, h: Int,
        walkableHeight: Int, walkableClimb: Int
    ) {
        var tooHighNeighbour = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    for (dir in 0..3) {
                        val thn = findNeighborConnections(chf, w, h, walkableHeight, walkableClimb, y, x, s, dir)
                        tooHighNeighbour = max(tooHighNeighbour, thn)
                    }
                }
            }
        }
        if (tooHighNeighbour > MAX_LAYERS) {
            throw RuntimeException("rcBuildCompactHeightfield: Heightfield has too many layers $tooHighNeighbour (max: $MAX_LAYERS)")
        }
    }

    private fun findNeighborConnections(
        chf: CompactHeightfield, w: Int, h: Int,
        walkableHeight: Int, walkableClimb: Int,
        y: Int, x: Int, s: CompactSpan, dir: Int
    ): Int {
        var tooHighNeighbour = 0
        setCon(s, dir, RC_NOT_CONNECTED)
        val nx = x + getDirOffsetX(dir)
        val ny = y + getDirOffsetY(dir)
        // First check, that the neighbour cell is in bounds.
        if (nx < 0 || ny < 0 || nx >= w || ny >= h) return 0

        // Iterate over all neighbour spans and check if any of the is
        // accessible from current cell.
        val nc = nx + ny * w
        val ncIndex = chf.index[nc]
        for (k in ncIndex until chf.endIndex[nc]) {
            val ns = chf.spans[k]
            val bot = max(s.y, ns.y)
            val top = min(s.y + s.height, ns.y + ns.height)

            // Check, that the gap between the spans is walkable,
            // and that the climb height between the gaps is not too high.
            if (top - bot >= walkableHeight && abs(ns.y - s.y) <= walkableClimb) {
                // Mark direction as walkable.
                val lidx = k - ncIndex
                if (lidx < 0 || lidx > MAX_LAYERS) {
                    tooHighNeighbour = max(tooHighNeighbour, lidx)
                    continue
                }
                setCon(s, dir, lidx)
                break
            }
        }
        return tooHighNeighbour
    }

    private fun getHeightFieldSpanCount(hf: Heightfield): Int {
        val w = hf.width
        val h = hf.height
        var spanCount = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = hf.spans[x + y * w]
                while (s != null) {
                    if (s.area != RC_NULL_AREA) spanCount++
                    s = s.next
                }
            }
        }
        return spanCount
    }
}