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

import org.joml.Vector3f
import org.recast4j.IntArrayList
import org.recast4j.Vectors.overlapRange
import org.recast4j.recast.RecastCommon.getCon
import org.recast4j.recast.RecastCommon.getDirOffsetX
import org.recast4j.recast.RecastCommon.getDirOffsetY
import org.recast4j.recast.RecastConstants.RC_NOT_CONNECTED
import org.recast4j.recast.RecastConstants.RC_NULL_AREA
import org.recast4j.recast.RecastRegion.SweepSpan
import kotlin.math.max
import kotlin.math.min

object RecastLayers {

    private fun addUnique(a: IntArrayList, v: Int) {
        if (!a.contains(v)) {
            a.add(v)
        }
    }

    fun buildHeightfieldLayers(
        ctx: Telemetry?,
        chf: CompactHeightfield,
        walkableHeight: Int
    ): Array<HeightfieldLayer>? {
        ctx?.startTimer(TelemetryType.BUILD_LAYERS)
        val w = chf.width
        val h = chf.height
        val borderSize = chf.borderSize
        val srcReg = IntArray(chf.spanCount)
        srcReg.fill(0xff)
        val numSweeps = chf.width // max(chf.width, chf.height);
        val sweeps = Array(numSweeps) { SweepSpan() }
        // Partition walkable area into monotone regions.
        val prevCount = IntArray(256)
        var regId = 0
        // Sweep one line at a time.
        for (y in borderSize until h - borderSize) {
            // Collect spans from this row.
            prevCount.fill(0, 0, regId)
            var sweepId = 0
            for (x in borderSize until w - borderSize) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    if (chf.areas[i] == RC_NULL_AREA) {
                        continue
                    }
                    var sid = 0xFF
                    // -x
                    if (getCon(s, 0) != RC_NOT_CONNECTED) {
                        val ax: Int = x + getDirOffsetX(0)
                        val ay: Int = y + getDirOffsetY(0)
                        val ai: Int = chf.index[ax + ay * w] + getCon(s, 0)
                        if (chf.areas[ai] != RC_NULL_AREA && srcReg[ai] != 0xff) sid = srcReg[ai]
                    }
                    if (sid == 0xff) {
                        sid = sweepId++
                        val sweep = sweeps[sid]
                        sweep.neighborId = 0xff
                        sweep.numSamples = 0
                    }

                    // -y
                    if (getCon(s, 3) != RC_NOT_CONNECTED) {
                        val ax: Int = x + getDirOffsetX(3)
                        val ay: Int = y + getDirOffsetY(3)
                        val ai: Int = chf.index[ax + ay * w] + getCon(s, 3)
                        val nr = srcReg[ai]
                        if (nr != 0xff) {
                            // Set neighbour when first valid neighbour is
                            // encoutered.
                            val sweep = sweeps[sid]
                            if (sweep.numSamples == 0) sweep.neighborId = nr
                            if (sweep.neighborId == nr) {
                                // Update existing neighbour
                                sweep.numSamples++
                                prevCount[nr]++
                            } else {
                                // This is hit if there is nore than one
                                // neighbour.
                                // Invalidate the neighbour.
                                sweep.neighborId = 0xff
                            }
                        }
                    }
                    srcReg[i] = sid
                }
            }

            // Create unique ID.
            for (i in 0 until sweepId) {
                // If the neighbour is set and there is only one continuous
                // connection to it,
                // the sweep will be merged with the previous one, else new
                // region is created.
                val sweep = sweeps[i]
                if (sweep.neighborId != 0xff && prevCount[sweep.neighborId] == sweep.numSamples) {
                    sweep.regionId = sweep.neighborId
                } else {
                    if (regId == 255) {
                        throw RuntimeException("rcBuildHeightfieldLayers: Region ID overflow.")
                    }
                    sweep.regionId = regId++
                }
            }

            // Remap local sweep ids to region ids.
            for (x in borderSize until w - borderSize) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    if (srcReg[i] != 0xff) {
                        srcReg[i] = sweeps[srcReg[i]].regionId
                    }
                }
            }
        }
        val nregs = regId
        // Construct regions
        val regions = Array(nregs) { LayerRegion(it) }

        // Find region neighbours and overlapping regions.
        val lregs = IntArrayList()
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = x + y * w
                lregs.clear()

                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    val ri = srcReg[i]
                    if (ri == 0xff) {
                        continue
                    }
                    val regRi = regions[ri]
                    regRi.yMin = min(regRi.yMin, s.y)
                    regRi.yMax = max(regRi.yMax, s.y)

                    // Collect all region layers.
                    lregs.add(ri)

                    // Update neighbours
                    for (dir in 0..3) {
                        if (getCon(s, dir) != RC_NOT_CONNECTED) {
                            val ax: Int = x + getDirOffsetX(dir)
                            val ay: Int = y + getDirOffsetY(dir)
                            val ai: Int = chf.index[ax + ay * w] + getCon(s, dir)
                            val rai = srcReg[ai]
                            if (rai != 0xff && rai != ri) {
                                addUnique(regRi.neis, rai)
                            }
                        }
                    }
                }

                // Update overlapping regions.
                for (i in 0 until lregs.size - 1) {
                    for (j in i + 1 until lregs.size) {
                        val lri = lregs[i]
                        val lrj = lregs[j]
                        if (lri != lrj) {
                            addUnique(regions[lri].layers, lrj)
                            addUnique(regions[lrj].layers, lri)
                        }
                    }
                }
            }
        }

        // Create 2D layers from regions.
        var layerId = 0
        val stack = IntArrayList()
        for (i in 0 until nregs) {
            val root = regions[i]
            // Skip already visited.
            if (root.layerId != 0xff) continue

            // Start search.
            root.layerId = layerId
            root.base = true
            stack.add(i)
            while (!stack.isEmpty()) {
                // Pop front
                val reg = regions[stack.remove(0)]
                for (neii in 0 until reg.neis.size) {
                    val nei = reg.neis[neii]
                    val region = regions[nei]
                    // Skip already visited.
                    if (region.layerId != 0xff) continue
                    // Skip if the neighbour is overlapping root region.
                    if (root.layers.contains(nei)) continue
                    // Skip if the height range would become too large.
                    val yMin = min(root.yMin, region.yMin)
                    val yMax = max(root.yMax, region.yMax)
                    if (yMax - yMin >= 255) continue

                    // Deepen
                    stack.add(nei)

                    // Mark layer id
                    region.layerId = layerId
                    // Merge current layers to root.
                    for (layerI in 0 until region.layers.size) {
                        addUnique(root.layers, region.layers[layerI])
                    }
                    root.yMin = min(root.yMin, region.yMin)
                    root.yMax = max(root.yMax, region.yMax)
                }
            }
            layerId++
        }

        // Merge non-overlapping regions that are close in height.
        val mergeHeight = walkableHeight * 4
        for (i in 0 until nregs) {
            val ri = regions[i]
            if (!ri.base) continue
            val newId = ri.layerId
            while (true) {
                var oldId = 0xff
                for (j in 0 until nregs) {
                    if (i == j) continue
                    val rj = regions[j]
                    if (!rj.base) continue

                    // Skip if the regions are not close to each other.
                    if (!overlapRange(ri.yMin, ri.yMax + mergeHeight, rj.yMin, rj.yMax + mergeHeight)) continue
                    // Skip if the height range would become too large.
                    val ymin = min(ri.yMin, rj.yMin)
                    val ymax = max(ri.yMax, rj.yMax)
                    if (ymax - ymin >= 255) continue

                    // Make sure that there is no overlap when merging 'ri' and
                    // 'rj'.
                    var overlap = false
                    // Iterate over all regions which have the same layerId as
                    // 'rj'
                    for (k in 0 until nregs) {
                        if (regions[k].layerId != rj.layerId) continue
                        // Check if region 'k' is overlapping region 'ri'
                        // Index to 'regs' is the same as region id.
                        if (ri.layers.contains(k)) {
                            overlap = true
                            break
                        }
                    }
                    // Cannot merge of regions overlap.
                    if (overlap) continue

                    // Can merge i and j.
                    oldId = rj.layerId
                    break
                }

                // Could not find anything to merge with, stop.
                if (oldId == 0xff) break

                // Merge
                for (j in 0 until nregs) {
                    val rj = regions[j]
                    if (rj.layerId == oldId) {
                        rj.base = false
                        // Remap layerIds.
                        rj.layerId = newId
                        // Add overlaid layers from 'rj' to 'ri'.
                        for (layerI in 0 until rj.layers.size) {
                            addUnique(ri.layers, rj.layers[layerI])
                        }
                        // Update height bounds.
                        ri.yMin = min(ri.yMin, rj.yMin)
                        ri.yMax = max(ri.yMax, rj.yMax)
                    }
                }
            }
        }

        // Compact layerIds
        val remap = IntArray(256)

        // Find number of unique layers.
        layerId = 0
        for (i in 0 until nregs) remap[regions[i].layerId] = 1
        for (i in 0..255) {
            if (remap[i] != 0) remap[i] = layerId++ else remap[i] = 0xff
        }
        // Remap ids.
        for (i in 0 until nregs) regions[i].layerId = remap[regions[i].layerId]

        // No layers, return empty.
        if (layerId == 0) {
            // ctx.stopTimer(RC_TIMER_BUILD_LAYERS);
            return null
        }

        // Create layers.
        // rcAssert(lset.layers == 0);
        val lw = w - borderSize * 2
        val lh = h - borderSize * 2

        // Build contracted bbox for layers.
        val bmin = Vector3f(chf.bmin)
        val bmax = Vector3f(chf.bmax)
        bmin.add(borderSize * chf.cellSize, 0f, borderSize * chf.cellSize, bmin)
        bmax.sub(borderSize * chf.cellSize, 0f, borderSize * chf.cellSize, bmin)
        val lset = Array(layerId) { HeightfieldLayer() }

        // Store layers.
        for (curId in lset.indices) {
            val layer = lset[curId]
            val gridSize = lw * lh
            layer.heights = IntArray(gridSize)
            layer.heights.fill(0xFF)
            layer.areas = IntArray(gridSize)
            layer.cons = IntArray(gridSize)

            // Find layer height bounds.
            var hmin = 0
            var hmax = 0
            for (j in 0 until nregs) {
                val regJ = regions[j]
                if (regJ.base && regJ.layerId == curId) {
                    hmin = regJ.yMin
                    hmax = regJ.yMax
                }
            }
            layer.width = lw
            layer.height = lh
            layer.cellSize = chf.cellSize
            layer.cellHeight = chf.cellHeight

            // Adjust the bbox to fit the heightfield.
            layer.bmin.set(bmin)
            layer.bmax.set(bmax)
            layer.bmin.y = bmin.y + hmin * chf.cellHeight
            layer.bmax.y = bmin.y + hmax * chf.cellHeight
            layer.minH = hmin
            layer.maxH = hmax

            // Update usable data region.
            layer.minX = layer.width
            layer.maxX = 0
            layer.minZ = layer.height
            layer.maxZ = 0

            // Copy height and area from compact heightfield.
            for (y in 0 until lh) {
                for (x in 0 until lw) {
                    val cx = borderSize + x
                    val cy = borderSize + y
                    val c = cx + cy * w
                    for (j in chf.index[c] until chf.endIndex[c]) {
                        val s = chf.spans[j]
                        // Skip unassigned regions.
                        if (srcReg[j] == 0xff) {
                            continue
                        }
                        // Skip of does nto belong to current layer.
                        val lid = regions[srcReg[j]].layerId
                        if (lid != curId) {
                            continue
                        }

                        // Update data bounds.
                        layer.minX = min(layer.minX, x)
                        layer.maxX = max(layer.maxX, x)
                        layer.minZ = min(layer.minZ, y)
                        layer.maxZ = max(layer.maxZ, y)

                        // Store height and area type.
                        val idx = x + y * lw
                        layer.heights[idx] = (s.y - hmin).toChar().code
                        layer.areas[idx] = chf.areas[j]

                        // Check connection.
                        var portal = 0.toChar()
                        var con = 0.toChar()
                        for (dir in 0..3) {
                            if (getCon(s, dir) != RC_NOT_CONNECTED) {
                                val ax: Int = cx + getDirOffsetX(dir)
                                val ay: Int = cy + getDirOffsetY(dir)
                                val ai: Int = chf.index[ax + ay * w] + getCon(s, dir)
                                val alid = if (srcReg[ai] != 0xff) regions[srcReg[ai]].layerId else 0xff
                                // Portal mask
                                if (chf.areas[ai] != RC_NULL_AREA && lid != alid) {
                                    portal = (portal.code or (1 shl dir)).toChar()
                                    // Update height so that it matches on both
                                    // sides of the portal.
                                    val ass = chf.spans[ai]
                                    if (ass.y > hmin) layer.heights[idx] =
                                        max(layer.heights[idx], (ass.y - hmin).toChar().code)
                                }
                                // Valid connection mask
                                if (chf.areas[ai] != RC_NULL_AREA && lid == alid) {
                                    val nx = ax - borderSize
                                    val ny = ay - borderSize
                                    if (nx >= 0 && ny >= 0 && nx < lw && ny < lh) con =
                                        (con.code or (1 shl dir)).toChar()
                                }
                            }
                        }
                        layer.cons[idx] = portal.code shl 4 or con.code
                    }
                }
            }
            if (layer.minX > layer.maxX) {
                layer.maxX = 0
                layer.minX = layer.maxX
            }
            if (layer.minZ > layer.maxZ) {
                layer.maxZ = 0
                layer.minZ = layer.maxZ
            }
        }

        ctx?.stopTimer(TelemetryType.BUILD_LAYERS)
        return lset
    }

    internal class LayerRegion(var id: Int) {
        var layerId = 0xff
        var base = false
        var yMin = 0xFFFF
        var yMax = 0
        val layers = IntArrayList()
        val neis = IntArrayList()

    }
}