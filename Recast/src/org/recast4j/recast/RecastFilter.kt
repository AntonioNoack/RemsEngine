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

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object RecastFilter {
    /**
     * Allows the formation of walkable regions that will flow over low lying objects such as curbs, and up structures such as stairways.
     * Two neighboring spans are walkable if: <tt>rcAbs(currentSpan.smax - neighborSpan.smax) < waklableClimb</tt>
     *
     * @warning Will override the effect of #rcFilterLedgeSpans. So if both filters are used, call #rcFilterLedgeSpans after calling this filter.
     */
    fun filterLowHangingWalkableObstacles(ctx: Telemetry?, walkableClimb: Int, solid: Heightfield) {
        ctx?.startTimer(TelemetryType.FILTER_LOW_OBSTACLES)
        val w = solid.width
        val h = solid.height
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = solid.spans[x + y * w]
                filterLowHangingWalkableObstacles(walkableClimb, s)
            }
        }
        ctx?.stopTimer(TelemetryType.FILTER_LOW_OBSTACLES)
    }

    private fun filterLowHangingWalkableObstacles(walkableClimb: Int, s0: Span?) {
        var ps: Span? = null
        var previousWalkable = false
        var previousArea = RecastConstants.RC_NULL_AREA
        var s = s0
        while (s != null) {
            val walkable = s.area != RecastConstants.RC_NULL_AREA
            // If current span is not walkable, but there is walkable
            // span just below it, mark the span above it walkable too.
            if (!walkable && previousWalkable && ps != null) {
                if (abs(s.max - ps.max) <= walkableClimb) s.area = previousArea
            }
            // Copy walkable flag so that it cannot propagate
            // past multiple non-walkable objects.
            previousWalkable = walkable
            previousArea = s.area
            ps = s
            s = s.next
        }
    }

    /**
     * A ledge is a span with one or more neighbors whose maximum is further away than @p walkableClimb from the current span's maximum.
     * This method removes the impact of the overestimation of conservative voxelization, so the resulting mesh will not have regions hanging in the air over ledges.
     * A span is a ledge if: <tt>rcAbs(currentSpan.smax - neighborSpan.smax) > walkableClimb</tt>
     */
    fun filterLedgeSpans(ctx: Telemetry?, walkableHeight: Int, walkableClimb: Int, solid: Heightfield) {
        ctx?.startTimer(TelemetryType.FILTER_LEDGE)
        val w = solid.width
        val h = solid.height

        // Mark border spans.
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = solid.spans[x + y * w]
                filterLedgeSpans(walkableHeight, walkableClimb, solid, s, x, y, w, h)
            }
        }
        ctx?.stopTimer(TelemetryType.FILTER_LEDGE)
    }

    private fun filterLedgeSpans(
        walkableHeight: Int, walkableClimb: Int, solid: Heightfield, s0: Span?,
        x: Int, y: Int, w: Int, h: Int
    ) {
        var s = s0
        while (s != null) {

            // Skip non walkable spans.
            if (s.area == RecastConstants.RC_NULL_AREA) {
                s = s.next
                continue
            }

            val bot = s.max
            val top = s.next?.min ?: RecastConstants.SPAN_MAX_HEIGHT

            // Find neighbours minimum height.
            var minh = RecastConstants.SPAN_MAX_HEIGHT

            // Min and max height of accessible neighbours.
            var asmin = s.max
            var asmax = s.max
            for (dir in 0..3) {
                val dx = x + RecastCommon.getDirOffsetX(dir)
                val dy = y + RecastCommon.getDirOffsetY(dir)
                // Skip neighbours, which are out of bounds.
                if (dx < 0 || dy < 0 || dx >= w || dy >= h) {
                    minh = min(minh, -walkableClimb - bot)
                    continue
                }

                // From minus infinity to the first span.
                var ns = solid.spans[dx + dy * w]
                var nbot = -walkableClimb
                var ntop = ns?.min ?: RecastConstants.SPAN_MAX_HEIGHT
                // Skip neightbour if the gap between the spans is too small.
                if (min(top, ntop) - max(bot, nbot) > walkableHeight) minh =
                    min(minh, nbot - bot)

                // Rest of the spans.
                ns = solid.spans[dx + dy * w]
                while (ns != null) {
                    nbot = ns.max
                    ntop = ns.next?.min ?: RecastConstants.SPAN_MAX_HEIGHT
                    // Skip neightbour if the gap between the spans is too small.
                    if (min(top, ntop) - max(bot, nbot) > walkableHeight) {
                        minh = min(minh, nbot - bot)

                        // Find min/max accessible neighbour height.
                        if (abs(nbot - bot) <= walkableClimb) {
                            if (nbot < asmin) asmin = nbot
                            if (nbot > asmax) asmax = nbot
                        }
                    }
                    ns = ns.next
                }
            }

            // The current span is close to a ledge if the drop to any
            // neighbour span is less than the walkableClimb.
            if (minh < -walkableClimb) s.area = RecastConstants.RC_NULL_AREA

            // If the difference between all neighbours is too large,
            // we are at steep slope, mark the span as ledge.
            if (asmax - asmin > walkableClimb) {
                s.area = RecastConstants.RC_NULL_AREA
            }
            s = s.next
        }
    }

    /**
     * For this filter, the clearance above the span is the distance from the span's maximum to the next higher span's minimum. (Same grid column.)
     */
    fun filterWalkableLowHeightSpans(ctx: Telemetry?, walkableHeight: Int, solid: Heightfield) {
        ctx?.startTimer(TelemetryType.FILTER_WALKABLE)
        val w = solid.width
        val h = solid.height

        // Remove walkable flag from spans, which do not have enough
        // space above them for the agent to stand there.
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = solid.spans[x + y * w]
                filterWalkableLowHeightSpans(s, walkableHeight)
            }
        }
        ctx?.stopTimer(TelemetryType.FILTER_WALKABLE)
    }

    private fun filterWalkableLowHeightSpans(s0: Span?, walkableHeight: Int) {
        var s = s0
        while (s != null) {
            val bot = s.max
            val top = s.next?.min ?: RecastConstants.SPAN_MAX_HEIGHT
            if (top - bot <= walkableHeight) s.area = RecastConstants.RC_NULL_AREA
            s = s.next
        }
    }
}