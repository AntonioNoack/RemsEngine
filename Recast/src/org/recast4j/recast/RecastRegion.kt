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

import org.recast4j.IntArrayList
import java.util.*
import kotlin.math.max
import kotlin.math.min

object RecastRegion {
    const val RC_NULL_NEI = 0xffff
    fun calculateDistanceField(chf: CompactHeightfield, src: IntArray): Int {
        val w = chf.width
        val h = chf.height

        // Init distance and points.
        src.fill(0xffff, 0, chf.spanCount)

        markBoundaryCells(w, h, chf, src)

        // Pass 1
        for (y in 0 until h) {
            for (x in 0 until w) {
                distCellStep(x, y, w, chf, src, 0, 3, 2)
            }
        }

        // Pass 2
        for (y in h - 1 downTo 0) {
            for (x in w - 1 downTo 0) {
                distCellStep(x, y, w, chf, src, 2, 1, 0)
            }
        }

        return findMaxDist(src, chf.spanCount)
    }

    private fun findMaxDist(src: IntArray, size: Int): Int {
        var maxDist = 0
        for (i in 0 until size) {
            maxDist = max(src[i], maxDist)
        }
        return maxDist
    }

    private fun markBoundaryCells(w: Int, h: Int, chf: CompactHeightfield, src: IntArray) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    val area = chf.areas[i]
                    var nc = 0
                    for (dir in 0..3) {
                        if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
                            val ax = x + RecastCommon.getDirOffsetX(dir)
                            val ay = y + RecastCommon.getDirOffsetY(dir)
                            val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, dir)
                            if (area == chf.areas[ai]) {
                                nc++
                            }
                        }
                    }
                    if (nc != 4) {
                        src[i] = 0
                    }
                }
            }
        }
    }

    private fun distCellStep(
        x: Int, y: Int, w: Int, chf: CompactHeightfield,
        src: IntArray, d0: Int, d1: Int, d2: Int
    ) {
        val c = x + y * w
        for (i in chf.index[c] until chf.endIndex[c]) {
            val s = chf.spans[i]
            distCellStep1(x, y, w, chf, s, src, i, d0, d1)
            distCellStep1(x, y, w, chf, s, src, i, d1, d2)
        }
    }

    private fun distCellStep1(
        x: Int, y: Int, w: Int, chf: CompactHeightfield, s: CompactSpan,
        src: IntArray, i: Int, d0: Int, d1: Int
    ) {
        if (RecastCommon.getCon(s, d0) != RecastConstants.RC_NOT_CONNECTED) {
            // (0,1) if 1
            val ax = x + RecastCommon.getDirOffsetX(d0)
            val ay = y + RecastCommon.getDirOffsetY(d0)
            val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, d0)
            val asp = chf.spans[ai]
            if (src[ai] + 2 < src[i]) {
                src[i] = src[ai] + 2
            }

            // (-1,1) if 0
            if (RecastCommon.getCon(asp, d1) != RecastConstants.RC_NOT_CONNECTED) {
                val aax = ax + RecastCommon.getDirOffsetX(d1)
                val aay = ay + RecastCommon.getDirOffsetY(d1)
                val aai = chf.index[aax + aay * w] + RecastCommon.getCon(asp, d1)
                if (src[aai] + 3 < src[i]) {
                    src[i] = src[aai] + 3
                }
            }
        }
    }

    private fun boxBlur(chf: CompactHeightfield, src: IntArray): IntArray {
        val w = chf.width
        val dst = IntArray(chf.spanCount)
        val thr = 2
        for (y in 0 until chf.height) {
            for (x in 0 until w) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    boxBlurCell(x, y, w, chf, i, src, thr, dst)
                }
            }
        }
        return dst
    }

    private fun boxBlurCell(
        x: Int, y: Int, w: Int, chf: CompactHeightfield, i: Int,
        src: IntArray, thr: Int, dst: IntArray
    ) {
        val s = chf.spans[i]
        val cd = src[i]
        if (cd <= thr) {
            dst[i] = cd
            return
        }
        var d = cd
        for (dir in 0..3) {
            if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
                val ax = x + RecastCommon.getDirOffsetX(dir)
                val ay = y + RecastCommon.getDirOffsetY(dir)
                val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, dir)
                d += src[ai]
                val asp = chf.spans[ai]
                val dir2 = dir + 1 and 0x3
                d += if (RecastCommon.getCon(asp, dir2) != RecastConstants.RC_NOT_CONNECTED) {
                    val ax2 = ax + RecastCommon.getDirOffsetX(dir2)
                    val ay2 = ay + RecastCommon.getDirOffsetY(dir2)
                    val ai2 = chf.index[ax2 + ay2 * w] + RecastCommon.getCon(asp, dir2)
                    src[ai2]
                } else {
                    cd
                }
            } else {
                d += cd * 2
            }
        }
        dst[i] = (d + 5) / 9
    }

    private fun floodRegion(
        x: Int, y: Int, i: Int, level: Int, r: Int, chf: CompactHeightfield, srcReg: IntArray,
        srcDist: IntArray, stack: IntArrayList
    ): Boolean {
        val w = chf.width
        val area = chf.areas[i]

        // Flood fill mark region.
        stack.clear()
        stack.add(x)
        stack.add(y)
        stack.add(i)
        srcReg[i] = r
        srcDist[i] = 0
        val lev = if (level >= 2) level - 2 else 0
        var changed = false
        while (stack.size > 0) {
            val ci = stack.remove(stack.size - 1)
            val cy = stack.remove(stack.size - 1)
            val cx = stack.remove(stack.size - 1)
            val cs = chf.spans[ci]

            // Check if any of the neighbours already have a valid region set.
            val neighborIsValid = anyNeighborHasValidRegion(cs, cx, cy, w, chf, area, srcReg, r)
            if (neighborIsValid) {
                srcReg[ci] = 0
            } else {
                changed = true
                expandNeighbors(cs, cx, cy, chf, w, area, lev, srcReg, r, srcDist, stack)
            }
        }
        return changed
    }

    private fun anyNeighborHasValidRegion(
        cs: CompactSpan, cx: Int, cy: Int, w: Int, chf: CompactHeightfield,
        area: Int, srcReg: IntArray, r: Int
    ): Boolean {
        for (dir in 0..3) {
            // 8 connected
            if (RecastCommon.getCon(cs, dir) != RecastConstants.RC_NOT_CONNECTED) {
                val ax = cx + RecastCommon.getDirOffsetX(dir)
                val ay = cy + RecastCommon.getDirOffsetY(dir)
                val ai = chf.index[ax + ay * w] + RecastCommon.getCon(cs, dir)
                if (chf.areas[ai] != area) {
                    continue
                }
                val nr = srcReg[ai]
                if (nr and RecastConstants.RC_BORDER_REG != 0) {
                    continue
                }
                if (nr != 0 && nr != r) {
                    return true
                }
                val asp = chf.spans[ai]
                val dir2 = dir + 1 and 0x3
                if (RecastCommon.getCon(asp, dir2) != RecastConstants.RC_NOT_CONNECTED) {
                    val ax2 = ax + RecastCommon.getDirOffsetX(dir2)
                    val ay2 = ay + RecastCommon.getDirOffsetY(dir2)
                    val ai2 = chf.index[ax2 + ay2 * w] + RecastCommon.getCon(asp, dir2)
                    if (chf.areas[ai2] != area) {
                        continue
                    }
                    val nr2 = srcReg[ai2]
                    if (nr2 != 0 && nr2 != r) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun expandNeighbors(
        cs: CompactSpan, cx: Int, cy: Int, chf: CompactHeightfield, w: Int,
        area: Int, lev: Int, srcReg: IntArray, r: Int,
        srcDist: IntArray, stack: IntArrayList
    ) {
        for (dir in 0..3) {
            if (RecastCommon.getCon(cs, dir) != RecastConstants.RC_NOT_CONNECTED) {
                val ax = cx + RecastCommon.getDirOffsetX(dir)
                val ay = cy + RecastCommon.getDirOffsetY(dir)
                val ai = chf.index[ax + ay * w] + RecastCommon.getCon(cs, dir)
                if (chf.areas[ai] != area) {
                    continue
                }
                if (chf.dist[ai] >= lev && srcReg[ai] == 0) {
                    srcReg[ai] = r
                    srcDist[ai] = 0
                    stack.add(ax)
                    stack.add(ay)
                    stack.add(ai)
                }
            }
        }
    }

    private fun expandRegions(
        maxIter: Int, level: Int, chf: CompactHeightfield, srcReg: IntArray, srcDist: IntArray,
        stack: IntArrayList, fillStack: Boolean
    ) {
        val w = chf.width
        val h = chf.height
        if (fillStack) {
            // Find cells revealed by the raised level.
            stack.clear()
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val c = x + y * w
                    for (i in chf.index[c] until chf.endIndex[c]) {
                        if (chf.dist[i] >= level && srcReg[i] == 0 && chf.areas[i] != RecastConstants.RC_NULL_AREA) {
                            stack.add(x)
                            stack.add(y)
                            stack.add(i)
                        }
                    }
                }
            }
        } else {  // use cells in the input stack
            // mark all cells which already have a region
            var j = 0
            while (j < stack.size) {
                val i = stack[j + 2]
                if (srcReg[i] != 0) {
                    stack[j + 2] = -1
                }
                j += 3
            }
        }
        val dirtyEntries = IntArrayList()
        var iter = 0
        while (stack.size > 0) {
            var failed = 0
            dirtyEntries.clear()
            var j = 0
            while (j < stack.size) {
                val x = stack[j]
                val y = stack[j + 1]
                val i = stack[j + 2]
                if (i < 0) {
                    failed++
                    j += 3
                    continue
                }
                var r = srcReg[i]
                var d2 = 0xffff
                val area = chf.areas[i]
                val s = chf.spans[i]
                for (dir in 0..3) {
                    if (RecastCommon.getCon(s, dir) == RecastConstants.RC_NOT_CONNECTED) {
                        continue
                    }
                    val ax = x + RecastCommon.getDirOffsetX(dir)
                    val ay = y + RecastCommon.getDirOffsetY(dir)
                    val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, dir)
                    if (chf.areas[ai] != area) {
                        continue
                    }
                    if (srcReg[ai] > 0 && srcReg[ai] and RecastConstants.RC_BORDER_REG == 0) {
                        if (srcDist[ai] + 2 < d2) {
                            r = srcReg[ai]
                            d2 = srcDist[ai] + 2
                        }
                    }
                }
                if (r != 0) {
                    stack[j + 2] = -1 // mark as used
                    dirtyEntries.add(i)
                    dirtyEntries.add(r)
                    dirtyEntries.add(d2)
                } else {
                    failed++
                }
                j += 3
            }

            // Copy entries that differ between src and dst to keep them in sync.
            var i = 0
            while (i < dirtyEntries.size) {
                val idx = dirtyEntries[i]
                srcReg[idx] = dirtyEntries[i + 1]
                srcDist[idx] = dirtyEntries[i + 2]
                i += 3
            }
            if (failed * 3 == stack.size) {
                break
            }
            if (level > 0) {
                ++iter
                if (iter >= maxIter) {
                    break
                }
            }
        }
    }

    private fun sortCellsByLevel(
        startLevel: Int, chf: CompactHeightfield, srcReg: IntArray, nbStacks: Int,
        stacks: List<IntArrayList>, loglevelsPerStack: Int
    ) { // the levels per stack (2 in our case) as a bit shift
        val startLevelI = startLevel shr loglevelsPerStack
        val w = chf.width
        val h = chf.height
        for (j in 0 until nbStacks) {
            stacks[j].clear()
        }

        // put all cells in the level range into the appropriate stacks
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    if (chf.areas[i] == RecastConstants.RC_NULL_AREA || srcReg[i] != 0) {
                        continue
                    }
                    val level = chf.dist[i] shr loglevelsPerStack
                    var sId = startLevelI - level
                    if (sId >= nbStacks) {
                        continue
                    }
                    if (sId < 0) {
                        sId = 0
                    }
                    stacks[sId].add(x)
                    stacks[sId].add(y)
                    stacks[sId].add(i)
                }
            }
        }
    }

    private fun appendStacks(srcStack: IntArrayList, dstStack: IntArrayList, srcReg: IntArray) {
        var j = 0
        while (j < srcStack.size) {
            val i = srcStack[j + 2]
            if (i < 0 || srcReg[i] != 0) {
                j += 3
                continue
            }
            dstStack.add(srcStack[j])
            dstStack.add(srcStack[j + 1])
            dstStack.add(srcStack[j + 2])
            j += 3
        }
    }

    private fun removeAdjacentDuplicates(reg: Region) {
        var i = 0
        val con = reg.connections
        while (i < con.size && con.size > 1) {
            val ni = (i + 1) % con.size
            if (con[i] == con[ni]) {
                con.remove(i)
            } else i++
        }
    }

    private fun replaceNeighbour(reg: Region, oldId: Int, newId: Int) {
        var neiChanged = false
        val con = reg.connections
        for (i in 0 until con.size) {
            if (con[i] == oldId) {
                con[i] = newId
                neiChanged = true
            }
        }
        val flo = reg.floors
        for (i in 0 until flo.size) {
            if (flo[i] == oldId) {
                flo[i] = newId
            }
        }
        if (neiChanged) {
            removeAdjacentDuplicates(reg)
        }
    }

    private fun canMergeWithRegion(rega: Region, regb: Region): Boolean {
        if (rega.areaType != regb.areaType) {
            return false
        }
        var n = 0
        val con = rega.connections
        for (i in 0 until con.size) {
            if (con[i] == regb.id) {
                n++
            }
        }
        if (n > 1) {
            return false
        }
        val flo = rega.floors
        for (i in 0 until flo.size) {
            if (flo[i] == regb.id) {
                return false
            }
        }
        return true
    }

    private fun addUniqueFloorRegion(reg: Region, n: Int) {
        if (!reg.floors.contains(n)) {
            reg.floors.add(n)
        }
    }

    private fun mergeRegions(rega: Region, regb: Region): Boolean {
        val aid = rega.id
        val bid = regb.id

        // Duplicate current neighbourhood.
        val acon = IntArrayList(rega.connections)
        val bcon = regb.connections

        // Find insertion point on A and B.
        var insa = findInsertionPoint(acon, bid)
        var insb = findInsertionPoint(bcon, aid)
        if (insa == -1 || insb == -1) {
            return false
        }

        // Merge neighbours.
        rega.connections.clear()
        addConnections(rega, acon, insa)
        addConnections(rega, bcon, insb)

        removeAdjacentDuplicates(rega)
        val flo = regb.floors
        for (j in 0 until flo.size) {
            addUniqueFloorRegion(rega, flo[j])
        }
        rega.spanCount += regb.spanCount
        regb.spanCount = 0
        regb.connections.clear()
        return true
    }

    private fun findInsertionPoint(acon: IntArrayList, bid: Int): Int {
        for (i in 0 until acon.size) {
            if (acon[i] == bid) {
                return i
            }
        }
        return -1
    }

    private fun addConnections(rega: Region, acon: IntArrayList, insa: Int) {
        val ni1 = acon.size
        for (i in 0 until ni1 - 1) {
            rega.connections.add(acon[(insa + 1 + i) % ni1])
        }
    }

    private fun isRegionConnectedToBorder(reg: Region?): Boolean {
        // Region is connected to border if
        // one of the neighbours is null id.
        return reg!!.connections.contains(0)
    }

    private fun isSolidEdge(chf: CompactHeightfield, srcReg: IntArray, x: Int, y: Int, i: Int, dir: Int): Boolean {
        val s = chf.spans[i]
        var r = 0
        if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
            val ax = x + RecastCommon.getDirOffsetX(dir)
            val ay = y + RecastCommon.getDirOffsetY(dir)
            val ai = chf.index[ax + ay * chf.width] + RecastCommon.getCon(s, dir)
            r = srcReg[ai]
        }
        return r != srcReg[i]
    }

    private fun walkContour(
        x0: Int, y0: Int, i0: Int, dir0: Int, chf: CompactHeightfield,
        srcReg: IntArray, cont: IntArrayList
    ) {
        var x = x0
        var y = y0
        var i = i0
        var dir = dir0
        val startDir = dir
        val starti = i

        val ss = chf.spans[i]
        var curReg = 0
        if (RecastCommon.getCon(ss, dir) != RecastConstants.RC_NOT_CONNECTED) {
            val ax = x + RecastCommon.getDirOffsetX(dir)
            val ay = y + RecastCommon.getDirOffsetY(dir)
            val ai = chf.index[ax + ay * chf.width] + RecastCommon.getCon(ss, dir)
            curReg = srcReg[ai]
        }

        cont.add(curReg)
        var iter = 0
        while (++iter < 40000) {
            val s = chf.spans[i]
            if (isSolidEdge(chf, srcReg, x, y, i, dir)) {
                // Choose the edge corner
                var r = 0
                if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
                    val ax = x + RecastCommon.getDirOffsetX(dir)
                    val ay = y + RecastCommon.getDirOffsetY(dir)
                    val ai = chf.index[ax + ay * chf.width] + RecastCommon.getCon(s, dir)
                    r = srcReg[ai]
                }
                if (r != curReg) {
                    curReg = r
                    cont.add(curReg)
                }
                dir = dir + 1 and 0x3 // Rotate CW
            } else {
                var ni = -1
                val nx = x + RecastCommon.getDirOffsetX(dir)
                val ny = y + RecastCommon.getDirOffsetY(dir)
                if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
                    ni = chf.index[nx + ny * chf.width] + RecastCommon.getCon(s, dir)
                }
                if (ni == -1) {
                    // Should not happen.
                    return
                }
                x = nx
                y = ny
                i = ni
                dir = dir + 3 and 0x3 // Rotate CCW
            }
            if (starti == i && startDir == dir) {
                break
            }
        }

        removeAdjacentDuplicates(cont)
    }

    private fun removeAdjacentDuplicates(cont: IntArrayList) {
        if (cont.size <= 1) return
        var j = 0
        while (j < cont.size) {
            val nj = (j + 1) % cont.size
            if (cont[j] == cont[nj]) {
                cont.remove(j)
            } else j++
        }
    }

    private fun mergeAndFilterRegions(
        minRegionArea: Int, mergeRegionSize: Int, maxRegionId0: Int,
        chf: CompactHeightfield, srcReg: IntArray, overlaps: IntArrayList
    ): Int {
        val nreg = maxRegionId0 + 1
        val regions = Array(nreg) { Region(it) }

        findRegionEdgeAndConnectionsAroundContour(nreg, regions, chf, srcReg)

        removeTooSmallRegions(nreg, regions, minRegionArea)
        mergeTooSmallRegionsWithNeighbors(nreg, regions, mergeRegionSize)

        val maxRegionId = compressRegionIds(nreg, regions)
        remapRegions(chf, srcReg, regions)

        // Return regions that we found to be overlapping.
        collectOverlappingRegions(nreg, regions, overlaps)
        return maxRegionId
    }

    /**
     * Find edge of a region and find connections around the contour.
     * */
    private fun findRegionEdgeAndConnectionsAroundContour(
        nreg: Int, regions: Array<Region>, chf: CompactHeightfield,
        srcReg: IntArray,
    ) {
        val w = chf.width
        for (y in 0 until chf.height) {
            for (x in 0 until w) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    findRegionEdgeAndConnectionsAroundContourCell(nreg, regions, srcReg, i, c, chf, x, y)
                }
            }
        }
    }

    private fun findRegionEdgeAndConnectionsAroundContourCell(
        nreg: Int, regions: Array<Region>,
        srcReg: IntArray, i: Int, c: Int,
        chf: CompactHeightfield, x: Int, y: Int
    ) {
        val r = srcReg[i]
        if (r == 0 || r >= nreg) {
            return
        }
        val reg = regions[r]
        reg.spanCount++

        // Update floors.
        for (j in chf.index[c] until chf.endIndex[c]) {
            if (i == j) continue
            val floorId = srcReg[j]
            if (floorId == 0 || floorId >= nreg) continue
            if (floorId == r) {
                reg.overlap = true
            }
            addUniqueFloorRegion(reg, floorId)
        }

        // Have found contour
        if (reg.connections.size > 0) {
            return
        }
        reg.areaType = chf.areas[i]

        // Check if this cell is next to a border.
        var ndir = -1
        for (dir in 0..3) {
            if (isSolidEdge(chf, srcReg, x, y, i, dir)) {
                ndir = dir
                break
            }
        }
        if (ndir != -1) {
            // The cell is at border.
            // Walk around the contour to find all the neighbours.
            walkContour(x, y, i, ndir, chf, srcReg, reg.connections)
        }
    }

    private fun removeTooSmallRegions(nreg: Int, regions: Array<Region>, minRegionArea: Int) {
        val stack = IntArrayList(32)
        val trace = IntArrayList(32)
        for (i in 0 until nreg) {
            val reg = regions[i]
            if (reg.id == 0 || reg.id and RecastConstants.RC_BORDER_REG != 0) {
                continue
            }
            if (reg.spanCount == 0) {
                continue
            }
            if (reg.visited) {
                continue
            }

            // Count the total size of all the connected regions.
            // Also keep track of the regions connects to a tile border.
            var connectsToBorder = false
            var spanCount = 0
            trace.clear()
            reg.visited = true
            stack.add(i)
            while (stack.size > 0) {
                // Pop
                val ri = stack.remove(stack.size - 1)
                val creg = regions[ri]
                spanCount += creg.spanCount
                trace.add(ri)
                val con = creg.connections
                for (j in 0 until con.size) {
                    if (con[j] and RecastConstants.RC_BORDER_REG != 0) {
                        connectsToBorder = true
                        continue
                    }
                    val neireg = regions[creg.connections[j]]
                    if (neireg.visited) {
                        continue
                    }
                    if (neireg.id == 0 || neireg.id and RecastConstants.RC_BORDER_REG != 0) {
                        continue
                    }
                    // Visit
                    stack.add(neireg.id)
                    neireg.visited = true
                }
            }

            // If the accumulated regions size is too small, remove it.
            // Do not remove areas which connect to tile borders
            // as their size cannot be estimated correctly and removing them
            // can potentially remove necessary areas.
            if (spanCount < minRegionArea && !connectsToBorder) {
                // Kill all visited regions.
                for (tri in 0 until trace.size) {
                    val tr = trace[tri]
                    val rg = regions[tr]
                    rg.spanCount = 0
                    rg.id = 0
                }
            }
        }
    }

    private fun mergeTooSmallRegionsWithNeighbors(nreg: Int, regions: Array<Region>, mergeRegionSize: Int) {
        // Merge too small regions to neighbour regions.
        do {
            var mergeCount = 0
            for (i in 0 until nreg) {
                val reg = regions[i]
                if (reg.id == 0 || reg.id and RecastConstants.RC_BORDER_REG != 0 ||
                    reg.overlap || reg.spanCount == 0
                ) {
                    continue
                }

                // Check to see if the region should be merged.
                if (reg.spanCount > mergeRegionSize && isRegionConnectedToBorder(reg)) {
                    continue
                }

                // Small region with more than 1 connection.
                // Or region which is not connected to a border at all.
                // Find smallest neighbour region that connects to this one.
                var smallest = 0xfffffff
                var mergeId = reg.id
                val con = reg.connections
                for (j in 0 until con.size) {
                    if (con[j] and RecastConstants.RC_BORDER_REG != 0) {
                        continue
                    }
                    val mreg = regions[con[j]]
                    if (mreg.id == 0 || mreg.id and RecastConstants.RC_BORDER_REG != 0 || mreg.overlap) {
                        continue
                    }
                    if (mreg.spanCount < smallest && canMergeWithRegion(reg, mreg) && canMergeWithRegion(mreg, reg)) {
                        smallest = mreg.spanCount
                        mergeId = mreg.id
                    }
                }
                // Found new id.
                if (mergeId != reg.id) {
                    val oldId = reg.id
                    val target = regions[mergeId]

                    // Merge neighbours.
                    if (mergeRegions(target, reg)) {
                        // Fixup regions pointing to current region.
                        for (j in 0 until nreg) {
                            val regJ = regions[j]
                            if (regJ.id == 0 || regJ.id and RecastConstants.RC_BORDER_REG != 0) {
                                continue
                            }
                            // If another region was already merged into current region
                            // change the nid of the previous region too.
                            if (regJ.id == oldId) {
                                regJ.id = mergeId
                            }
                            // Replace the current region with the new one if the
                            // current regions is neighbour.
                            replaceNeighbour(regJ, oldId, mergeId)
                        }
                        mergeCount++
                    }
                }
            }
        } while (mergeCount > 0)
    }

    private fun collectOverlappingRegions(nreg: Int, regions: Array<Region>, overlaps: IntArrayList) {
        for (i in 0 until nreg) {
            val regionI = regions[i]
            if (regionI.overlap) {
                overlaps.add(regionI.id)
            }
        }
    }

    private fun addUniqueConnection(reg: Region, n: Int) {
        if (!reg.connections.contains(n)) {
            reg.connections.add(n)
        }
    }

    private fun mergeAndFilterLayerRegions(
        minRegionArea: Int, maxRegionId0: Int,
        chf: CompactHeightfield, srcReg: IntArray
    ): Int {
        val w = chf.width
        val h = chf.height
        val nreg = maxRegionId0 + 1

        // Construct regions
        val regions = Array(nreg) { Region(it) }
        findRegionNeighborsAndOverlappingRegions(w, h, chf, srcReg, nreg, regions)

        // Create 2D layers from regions.
        for (i in 0 until nreg) {
            regions[i].id = 0
        }

        mergeMonotoneRegions(nreg, regions)

        removeSmallRegions(nreg, regions, minRegionArea)
        val maxRegionId = compressRegionIds(nreg, regions)
        remapRegions(chf, srcReg, regions)
        return maxRegionId
    }

    private fun findRegionNeighborsAndOverlappingRegions(
        w: Int, h: Int, chf: CompactHeightfield,
        srcReg: IntArray, nreg: Int, regions: Array<Region>
    ) {
        val lregs = IntArrayList(32)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = x + y * w
                lregs.clear()
                for (i in chf.index[c] until chf.endIndex[c]) {
                    findRegionNeighborsCell(w, chf, srcReg, nreg, regions, i, lregs, x, y)
                }
                updateOverlappingRegions(lregs, regions)
            }
        }
    }

    private fun findRegionNeighborsCell(
        w: Int, chf: CompactHeightfield,
        srcReg: IntArray, nreg: Int, regions: Array<Region>,
        i: Int, lregs: IntArrayList, x: Int, y: Int
    ) {
        val s = chf.spans[i]
        val ri = srcReg[i]
        if (ri == 0 || ri >= nreg) {
            return
        }
        val reg = regions[ri]
        reg.spanCount++
        reg.areaType = chf.areas[i]
        reg.ymin = min(reg.ymin, s.y)
        reg.ymax = max(reg.ymax, s.y)
        // Collect all region layers.
        lregs.add(ri)

        updateNeighbors(x, y, w, chf, s, srcReg, nreg, ri, reg)
    }

    private fun updateNeighbors(
        x: Int, y: Int, w: Int, chf: CompactHeightfield, s: CompactSpan,
        srcReg: IntArray, nreg: Int, ri: Int, reg: Region
    ) {
        for (dir in 0..3) {
            if (RecastCommon.getCon(s, dir) != RecastConstants.RC_NOT_CONNECTED) {
                val ax = x + RecastCommon.getDirOffsetX(dir)
                val ay = y + RecastCommon.getDirOffsetY(dir)
                val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, dir)
                val rai = srcReg[ai]
                if (rai in 1 until nreg && rai != ri) {
                    addUniqueConnection(reg, rai)
                }
                if (rai and RecastConstants.RC_BORDER_REG != 0) {
                    reg.connectsToBorder = true
                }
            }
        }
    }

    private fun updateOverlappingRegions(lregs: IntArrayList, regions: Array<Region>) {
        for (i in 0 until lregs.size - 1) {
            for (j in i + 1 until lregs.size) {
                if (lregs[i] != lregs[j]) {
                    val ri = regions[lregs[i]]
                    val rj = regions[lregs[j]]
                    addUniqueFloorRegion(ri, lregs[j])
                    addUniqueFloorRegion(rj, lregs[i])
                }
            }
        }
    }

    /**
     * Merge montone regions to create non-overlapping areas.
     * */
    private fun mergeMonotoneRegions(nreg: Int, regions: Array<Region>) {
        var layerId = 1
        val stack = IntArrayList(32)
        for (i in 1 until nreg) {
            val root = regions[i]
            // Skip already visited.
            if (root.id != 0) {
                continue
            }

            // Start search.
            root.id = layerId
            stack.clear()
            stack.add(i)
            while (stack.size > 0) {
                // Pop front
                val reg = regions[stack.remove(0)]
                val ncons = reg.connections.size
                for (j in 0 until ncons) {
                    val nei = reg.connections[j]
                    val regn = regions[nei]
                    // Skip already visited.
                    if (regn.id != 0) {
                        continue
                    }
                    // Skip if different area type, do not connect regions with different area type.
                    if (reg.areaType != regn.areaType) {
                        continue
                    }
                    // Skip if the neighbour is overlapping root region.
                    var overlap = false
                    val flo = root.floors
                    for (k in 0 until flo.size) {
                        if (flo[k] == nei) {
                            overlap = true
                            break
                        }
                    }
                    if (overlap) {
                        continue
                    }

                    // Deepen
                    stack.add(nei)

                    // Mark layer id
                    regn.id = layerId
                    // Merge current layers to root.
                    val flo2 = regn.floors
                    for (k in 0 until flo2.size) {
                        addUniqueFloorRegion(root, flo2[k])
                    }
                    root.ymin = min(root.ymin, regn.ymin)
                    root.ymax = max(root.ymax, regn.ymax)
                    root.spanCount += regn.spanCount
                    regn.spanCount = 0
                    root.connectsToBorder = root.connectsToBorder || regn.connectsToBorder
                }
            }
            layerId++
        }
    }

    private fun removeSmallRegions(nreg: Int, regions: Array<Region>, minRegionArea: Int) {
        for (i in 0 until nreg) {
            if (regions[i].spanCount in 1 until minRegionArea && !regions[i].connectsToBorder) {
                val reg = regions[i].id
                for (j in 0 until nreg) {
                    if (regions[j].id == reg) {
                        regions[j].id = 0
                    }
                }
            }
        }
    }

    private fun compressRegionIds(nreg: Int, regions: Array<Region>): Int {
        for (i in 0 until nreg) {
            regions[i].remap = false
            if (regions[i].id == 0) {
                continue  // Skip nil regions.
            }
            if (regions[i].id and RecastConstants.RC_BORDER_REG != 0) {
                continue  // Skip external regions.
            }
            regions[i].remap = true
        }
        var regIdGen = 0
        for (i in 0 until nreg) {
            if (!regions[i].remap) {
                continue
            }
            val oldId = regions[i].id
            val newId = ++regIdGen
            for (j in i until nreg) {
                if (regions[j].id == oldId) {
                    regions[j].id = newId
                    regions[j].remap = false
                }
            }
        }
        return regIdGen
    }

    private fun remapRegions(chf: CompactHeightfield, srcReg: IntArray, regions: Array<Region>) {
        for (i in 0 until chf.spanCount) {
            if (srcReg[i] and RecastConstants.RC_BORDER_REG == 0) {
                srcReg[i] = regions[srcReg[i]].id
            }
        }
    }

    /**
     * This is usually the second to the last step in creating a fully built
     * compact heightfield. This step is required before regions are built
     * using #rcBuildRegions or #rcBuildRegionsMonotone.
     *
     * After this step, the distance data is available via the rcCompactHeightfield::maxDistance
     * and rcCompactHeightfield::dist fields.
     *
     * @see rcCompactHeightfield, rcBuildRegions, rcBuildRegionsMonotone
     */
    fun buildDistanceField(ctx: Telemetry?, chf: CompactHeightfield) {
        ctx?.startTimer(TelemetryType.DISTANCEFIELD)
        var src = IntArray(chf.spanCount)
        ctx?.startTimer(TelemetryType.DISTANCEFIELD_DIST)
        chf.maxDistance = calculateDistanceField(chf, src)
        ctx?.stopTimer(TelemetryType.DISTANCEFIELD_DIST)
        ctx?.startTimer(TelemetryType.DISTANCEFIELD_BLUR)

        // Blur
        src = boxBlur(chf, src)

        // Store distance.
        src.copyInto(chf.dist)
        ctx?.stopTimer(TelemetryType.DISTANCEFIELD_BLUR)
        ctx?.stopTimer(TelemetryType.DISTANCEFIELD)
    }

    private fun paintRectRegion(
        minx: Int, maxx: Int, miny: Int, maxy: Int, regId: Int, chf: CompactHeightfield,
        srcReg: IntArray
    ) {
        val w = chf.width
        for (y in miny until maxy) {
            for (x in minx until maxx) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    if (chf.areas[i] != RecastConstants.RC_NULL_AREA) {
                        srcReg[i] = regId
                    }
                }
            }
        }
    }

    /**
     * Non-null regions will consist of connected, non-overlapping walkable spans that form a single contour.
     * Contours will form simple polygons.
     *
     * If multiple regions form an area that is smaller than @p minRegionArea, then all spans will be
     * re-assigned to the zero (null) region.
     *
     * Partitioning can result in smaller than necessary regions. @p mergeRegionArea helps
     * reduce unecessarily small regions.
     *
     * See the #rcConfig documentation for more information on the configuration parameters.
     *
     * The region data will be available via the rcCompactHeightfield::maxRegions
     * and rcCompactSpan::reg fields.
     *
     * @warning The distance field must be created using #rcBuildDistanceField before attempting to build regions.
     *
     * @see rcCompactHeightfield, rcCompactSpan, rcBuildDistanceField, rcBuildRegionsMonotone, rcConfig
     */
    fun buildRegionsMonotone(
        ctx: Telemetry?, chf: CompactHeightfield, minRegionArea: Int,
        mergeRegionArea: Int
    ) {
        ctx?.startTimer(TelemetryType.REGIONS)
        val w = chf.width
        val h = chf.height
        val borderSize = chf.borderSize
        var id = 1
        val srcReg = IntArray(chf.spanCount)
        val nsweeps = max(chf.width, chf.height)
        val sweeps = Array(nsweeps) { SweepSpan() }

        // Mark border regions.
        if (borderSize > 0) {
            // Make sure border will not overflow.
            val bw = min(w, borderSize)
            val bh = min(h, borderSize)
            // Paint regions
            paintRectRegion(0, bw, 0, h, (id++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(w - bw, w, 0, h, (id++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(0, w, 0, bh, (id++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(0, w, h - bh, h, (id++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
        }
        var prev = IntArray(1024)

        // Sweep one line at a time.
        for (y in borderSize until h - borderSize) {
            // Collect spans from this row.
            if (prev.size < id * 2) {
                prev = IntArray(id * 2)
            } else {
                prev.fill(0, 0, id)
            }
            var rid = 1
            for (x in borderSize until w - borderSize) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    if (chf.areas[i] == RecastConstants.RC_NULL_AREA) {
                        continue
                    }

                    // -x
                    var previd = 0
                    if (RecastCommon.getCon(s, 0) != RecastConstants.RC_NOT_CONNECTED) {
                        val ax = x + RecastCommon.getDirOffsetX(0)
                        val ay = y + RecastCommon.getDirOffsetY(0)
                        val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, 0)
                        if (srcReg[ai] and RecastConstants.RC_BORDER_REG == 0 && chf.areas[i] == chf.areas[ai]) {
                            previd = srcReg[ai]
                        }
                    }
                    if (previd == 0) {
                        previd = rid++
                        val sweep = sweeps[previd]
                        sweep.rowId = previd
                        sweep.numSamples = 0
                        sweep.neighborId = 0
                    }

                    // -y
                    if (RecastCommon.getCon(s, 3) != RecastConstants.RC_NOT_CONNECTED) {
                        val ax = x + RecastCommon.getDirOffsetX(3)
                        val ay = y + RecastCommon.getDirOffsetY(3)
                        val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, 3)
                        if (srcReg[ai] != 0 && srcReg[ai] and RecastConstants.RC_BORDER_REG == 0 && chf.areas[i] == chf.areas[ai]) {
                            val nr = srcReg[ai]
                            val sweep = sweeps[previd]
                            if (sweep.neighborId == 0 || sweep.neighborId == nr) {
                                sweep.neighborId = nr
                                sweep.numSamples++
                                if (prev.size <= nr) {
                                    prev = prev.copyOf(prev.size * 2)
                                }
                                prev[nr]++
                            } else {
                                sweep.neighborId = RC_NULL_NEI
                            }
                        }
                    }
                    srcReg[i] = previd
                }
            }

            id = createUniqueIDs(sweeps, rid, prev, id)
            remapIDs(y, w, borderSize, chf, srcReg, rid, sweeps)
        }
        ctx?.startTimer(TelemetryType.REGIONS_FILTER)

        // Merge regions and filter out small regions.
        val overlaps = IntArrayList()
        chf.maxRegions = mergeAndFilterRegions(minRegionArea, mergeRegionArea, id, chf, srcReg, overlaps)

        // Monotone partitioning does not generate overlapping regions.
        ctx?.stopTimer(TelemetryType.REGIONS_FILTER)

        // Store the result out.
        for (i in 0 until chf.spanCount) {
            chf.spans[i].regionId = srcReg[i]
        }
        ctx?.stopTimer(TelemetryType.REGIONS)
    }

    private fun createUniqueIDs(sweeps: Array<SweepSpan>, rid: Int, prev: IntArray, id0: Int): Int {
        var id = id0
        for (i in 1 until rid) {
            val sweep = sweeps[i]
            if (sweep.neighborId != RC_NULL_NEI && sweep.neighborId != 0 && prev[sweep.neighborId] == sweep.numSamples) {
                sweep.regionId = sweep.neighborId
            } else {
                sweep.regionId = id++
            }
        }
        return id
    }

    private fun remapIDs(
        y: Int, w: Int, borderSize: Int,
        chf: CompactHeightfield, srcReg: IntArray, rid: Int,
        sweeps: Array<SweepSpan>
    ) {
        for (x in borderSize until w - borderSize) {
            val c = x + y * w
            for (i in chf.index[c] until chf.endIndex[c]) {
                if (srcReg[i] in 1 until rid) {
                    srcReg[i] = sweeps[srcReg[i]].regionId
                }
            }
        }
    }

    /**
     * Non-null regions will consist of connected, non-overlapping walkable spans that form a single contour.
     * Contours will form simple polygons.
     *
     * If multiple regions form an area that is smaller than @p minRegionArea, then all spans will be
     * re-assigned to the zero (null) region.
     *
     * Watershed partitioning can result in smaller than necessary regions, especially in diagonal corridors.
     * @p mergeRegionArea helps reduce unecessarily small regions.
     *
     * See the #rcConfig documentation for more information on the configuration parameters.
     *
     * The region data will be available via the rcCompactHeightfield::maxRegions
     * and rcCompactSpan::reg fields.
     *
     * @warning The distance field must be created using #rcBuildDistanceField before attempting to build regions.
     *
     * @see rcCompactHeightfield, rcCompactSpan, rcBuildDistanceField, rcBuildRegionsMonotone, rcConfig
     */
    fun buildRegions(
        ctx: Telemetry?, chf: CompactHeightfield, minRegionArea: Int,
        mergeRegionArea: Int
    ) {
        ctx?.startTimer(TelemetryType.REGIONS)
        val w = chf.width
        val h = chf.height
        val borderSize = chf.borderSize
        ctx?.startTimer(TelemetryType.REGIONS_WATERSHED)
        val LOG_NB_STACKS = 3
        val NB_STACKS = 1 shl LOG_NB_STACKS
        val lvlStacks: MutableList<IntArrayList> = ArrayList()
        for (i in 0 until NB_STACKS) {
            lvlStacks.add(IntArrayList(1024))
        }
        val stack = IntArrayList(1024)
        val srcReg = IntArray(chf.spanCount)
        val srcDist = IntArray(chf.spanCount)
        var regionId = 1
        var level = chf.maxDistance + 1 and 1.inv()

        // TODO: Figure better formula, expandIters defines how much the
        // watershed "overflows" and simplifies the regions. Tying it to
        // agent radius was usually good indication how greedy it could be.
        // const int expandIters = 4 + walkableRadius * 2;
        val expandIters = 8
        if (borderSize > 0) {
            // Make sure border will not overflow.
            val bw = min(w, borderSize)
            val bh = min(h, borderSize)
            // Paint regions
            paintRectRegion(0, bw, 0, h, (regionId++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(w - bw, w, 0, h, (regionId++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(0, w, 0, bh, (regionId++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
            paintRectRegion(0, w, h - bh, h, (regionId++) or RecastConstants.RC_BORDER_REG, chf, srcReg)
        }
        chf.borderSize = borderSize
        var sId = -1
        while (level > 0) {
            level = if (level >= 2) level - 2 else 0
            sId = sId + 1 and NB_STACKS - 1

            // ctx->startTimer(RC_TIMER_DIVIDE_TO_LEVELS);
            if (sId == 0) {
                sortCellsByLevel(level, chf, srcReg, NB_STACKS, lvlStacks, 1)
            } else {
                appendStacks(lvlStacks[sId - 1], lvlStacks[sId], srcReg) // copy left overs from last level
            }

            // ctx->stopTimer(RC_TIMER_DIVIDE_TO_LEVELS);
            ctx?.startTimer(TelemetryType.REGIONS_EXPAND)

            // Expand current regions until no empty connected cells found.
            expandRegions(expandIters, level, chf, srcReg, srcDist, lvlStacks[sId], false)
            ctx?.stopTimer(TelemetryType.REGIONS_EXPAND)
            ctx?.startTimer(TelemetryType.REGIONS_FLOOD)

            // Mark new regions with IDs.
            var j = 0
            while (j < lvlStacks[sId].size) {
                val x = lvlStacks[sId][j]
                val y = lvlStacks[sId][j + 1]
                val i = lvlStacks[sId][j + 2]
                if (i >= 0 && srcReg[i] == 0) {
                    if (floodRegion(x, y, i, level, regionId, chf, srcReg, srcDist, stack)) {
                        regionId++
                    }
                }
                j += 3
            }
            ctx?.stopTimer(TelemetryType.REGIONS_FLOOD)
        }

        // Expand current regions until no empty connected cells found.
        expandRegions(expandIters * 8, 0, chf, srcReg, srcDist, stack, true)
        ctx?.stopTimer(TelemetryType.REGIONS_WATERSHED)
        ctx?.startTimer(TelemetryType.REGIONS_FILTER)

        // Merge regions and filter out smalle regions.
        val overlaps = IntArrayList()
        chf.maxRegions = mergeAndFilterRegions(minRegionArea, mergeRegionArea, regionId, chf, srcReg, overlaps)

        // If overlapping regions were found during merging, split those regions.
        if (overlaps.size > 0 && ctx != null) {
            ctx.warn("rcBuildRegions: " + overlaps.size + " overlapping regions.")
        }
        ctx?.stopTimer(TelemetryType.REGIONS_FILTER)

        // Write the result out.
        for (i in 0 until chf.spanCount) {
            chf.spans[i].regionId = srcReg[i]
        }
        ctx?.stopTimer(TelemetryType.REGIONS)
    }

    fun buildLayerRegions(ctx: Telemetry?, chf: CompactHeightfield, minRegionArea: Int) {
        ctx?.startTimer(TelemetryType.REGIONS)
        val w = chf.width
        val h = chf.height
        val borderSize = chf.borderSize
        var id = 1
        val srcReg = IntArray(chf.spanCount)
        val nsweeps = max(chf.width, chf.height)
        val sweeps = Array(nsweeps) { SweepSpan() }

        // Mark border regions.
        if (borderSize > 0) {
            // Make sure border will not overflow.
            val bw = min(w, borderSize)
            val bh = min(h, borderSize)
            // Paint regions
            paintRectRegion(0, bw, 0, h, id or RecastConstants.RC_BORDER_REG, chf, srcReg)
            id++
            paintRectRegion(w - bw, w, 0, h, id or RecastConstants.RC_BORDER_REG, chf, srcReg)
            id++
            paintRectRegion(0, w, 0, bh, id or RecastConstants.RC_BORDER_REG, chf, srcReg)
            id++
            paintRectRegion(0, w, h - bh, h, id or RecastConstants.RC_BORDER_REG, chf, srcReg)
            id++
        }

        var prev = IntArray(1024)
        // Sweep one line at a time.
        for (y in borderSize until h - borderSize) {
            // Collect spans from this row.
            if (prev.size <= id * 2) {
                prev = IntArray(id * 2)
            } else {
                prev.fill(0, 0, id)
            }
            var rid = 1
            for (x in borderSize until w - borderSize) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    val s = chf.spans[i]
                    if (chf.areas[i] == RecastConstants.RC_NULL_AREA) {
                        continue
                    }

                    // -x
                    var previd = 0
                    if (RecastCommon.getCon(s, 0) != RecastConstants.RC_NOT_CONNECTED) {
                        val ax = x + RecastCommon.getDirOffsetX(0)
                        val ay = y + RecastCommon.getDirOffsetY(0)
                        val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, 0)
                        if (srcReg[ai] and RecastConstants.RC_BORDER_REG == 0 && chf.areas[i] == chf.areas[ai]) {
                            previd = srcReg[ai]
                        }
                    }
                    if (previd == 0) {
                        previd = rid++
                        val sweep = sweeps[previd]
                        sweep.rowId = previd
                        sweep.numSamples = 0
                        sweep.neighborId = 0
                    }

                    // -y
                    if (RecastCommon.getCon(s, 3) != RecastConstants.RC_NOT_CONNECTED) {
                        val ax = x + RecastCommon.getDirOffsetX(3)
                        val ay = y + RecastCommon.getDirOffsetY(3)
                        val ai = chf.index[ax + ay * w] + RecastCommon.getCon(s, 3)
                        if (srcReg[ai] != 0 && srcReg[ai] and RecastConstants.RC_BORDER_REG == 0 && chf.areas[i] == chf.areas[ai]) {
                            val nr = srcReg[ai]
                            val sweep = sweeps[previd]
                            if (sweep.neighborId == 0 || sweep.neighborId == nr) {
                                sweep.neighborId = nr
                                sweep.numSamples++
                                if (prev.size <= nr) {
                                    prev = prev.copyOf(prev.size * 2)
                                }
                                prev[nr]++
                            } else {
                                sweep.neighborId = RC_NULL_NEI
                            }
                        }
                    }
                    srcReg[i] = previd
                }
            }

            // Create unique ID.
            for (i in 1 until rid) {
                val sweep = sweeps[i]
                if (sweep.neighborId != RC_NULL_NEI && sweep.neighborId != 0 && prev[sweep.neighborId] == sweep.numSamples) {
                    sweep.regionId = sweep.neighborId
                } else {
                    sweep.regionId = id++
                }
            }

            // Remap IDs
            for (x in borderSize until w - borderSize) {
                val c = x + y * w
                for (i in chf.index[c] until chf.endIndex[c]) {
                    if (srcReg[i] in 1 until rid) {
                        srcReg[i] = sweeps[srcReg[i]].regionId
                    }
                }
            }
        }
        ctx?.startTimer(TelemetryType.REGIONS_FILTER)

        // Merge monotone regions to layers and remove small regions.
        chf.maxRegions = mergeAndFilterLayerRegions(minRegionArea, id, chf, srcReg)
        ctx?.stopTimer(TelemetryType.REGIONS_FILTER)

        // Store the result out.
        for (i in 0 until chf.spanCount) {
            chf.spans[i].regionId = srcReg[i]
        }
        ctx?.stopTimer(TelemetryType.REGIONS)
    }

    internal class SweepSpan {
        var rowId = 0
        var regionId = 0
        var numSamples = 0
        var neighborId = 0
    }

    internal class Region(var id: Int) {
        // ID of the region
        var spanCount = 0 // Number of spans belonging to this region
        var areaType = 0
        var remap = false
        var visited = false
        var overlap = false
        var connectsToBorder = false
        var ymin = 0xFFFF
        var ymax = 0
        val connections = IntArrayList()
        val floors = IntArrayList()
    }
}