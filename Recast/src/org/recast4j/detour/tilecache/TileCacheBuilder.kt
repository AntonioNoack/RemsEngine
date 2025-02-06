/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour.tilecache

import org.joml.Vector3f
import org.recast4j.Edge
import org.recast4j.IntArrayList
import org.recast4j.Vectors.sq
import org.recast4j.detour.tilecache.io.TileCacheLayerHeaderReader
import org.recast4j.detour.tilecache.io.TileCacheLayerHeaderWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TileCacheBuilder {
    private class LayerSweepSpan {
        var numSamples = 0
        var regionId = 0
        var neighborId = 0
    }

    class LayerMonotoneRegion {
        var area = 0
        var regId = 0
        var areaId = 0
        var neighbors = IntArrayList()
    }

    private class TempContour() {

        var vertices = IntArrayList()
        var numVertices = 0
        var poly = IntArrayList()

        fun npoly(): Int {
            return poly.size
        }

        fun clear() {
            numVertices = 0
            vertices.clear()
        }
    }

    fun buildTileCacheRegions(layer: TileCacheLayer, walkableClimb: Int) {

        val w = layer.width
        val h = layer.height
        layer.fillRegs(0xff)

        val sweeps = Array(w) {
            LayerSweepSpan()
        }
        // Partition walkable area into monotone regions.
        val prevCount = IntArray(256)
        var regId = 0
        for (y in 0 until h) {
            if (regId > 0) {
                prevCount.fill(0, 0, regId)
            }
            // memset(prevCount,0,sizeof(char)*regId);
            var sweepId = 0
            for (x in 0 until w) {
                val idx = x + y * w
                if (layer.getArea(idx) == TILECACHE_NULL_AREA) continue
                var sid = 0xff

                // -x
                val xidx = x - 1 + y * w
                if (x > 0 && isConnected(layer, idx, xidx, walkableClimb)) {
                    if (layer.getReg(xidx) != 0xff) {
                        sid = layer.getReg(xidx)
                    }
                }
                if (sid == 0xff) {
                    sid = sweepId++
                    val sweep = sweeps[sid]
                    sweep.neighborId = 0xff
                    sweep.numSamples = 0
                }

                // -y
                val yidx = x + (y - 1) * w
                if (y > 0 && isConnected(layer, idx, yidx, walkableClimb)) {
                    val nr = layer.getReg(yidx)
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
                            // This is hit if there is nore than one neighbour.
                            // Invalidate the neighbour.
                            sweep.neighborId = 0xff
                        }
                    }
                }
                layer.setReg(idx, sid)
            }

            // Create unique ID.
            for (i in 0 until sweepId) {
                // If the neighbour is set and there is only one continuous
                // connection to it,
                // the sweep will be merged with the previous one, else new
                // region is created.
                val sweepI = sweeps[i]
                if (sweepI.neighborId != 0xff && prevCount[sweepI.neighborId] == sweepI.numSamples) {
                    sweepI.regionId = sweepI.neighborId
                } else {
                    if (regId == 255) {
                        // Region ID's overflow.
                        throw RuntimeException("Buffer too small")
                    }
                    sweepI.regionId = regId++
                }
            }

            // Remap local sweep ids to region ids.
            for (x in 0 until w) {
                val idx = x + y * w
                if (layer.getReg(idx) != 0xff) {
                    layer.setReg(idx, sweeps[layer.getReg(idx)].regionId)
                }
            }
        }

        // Allocate and init layer regions.
        val nregs = regId
        val regs = Array<LayerMonotoneRegion>(nregs) {
            LayerMonotoneRegion()
        }
        for (i in 0 until nregs) {
            regs[i].regId = 0xff
        }

        // Find region neighbours.
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = x + y * w
                val ri = layer.getReg(idx)
                if (ri == 0xff) continue

                // Update area.
                val regRi = regs[ri]
                regRi.area++
                regRi.areaId = layer.getArea(idx)

                // Update neighbours
                val ymi = x + (y - 1) * w
                if (y > 0 && isConnected(layer, idx, ymi, walkableClimb)) {
                    val rai = layer.getReg(ymi).toInt()
                    if (rai != 0xff && rai != ri) {
                        addUniqueLast(regRi.neighbors, rai)
                        addUniqueLast(regs[rai].neighbors, ri)
                    }
                }
            }
        }
        for (i in 0 until nregs) regs[i].regId = i
        for (i in 0 until nregs) {
            val reg = regs[i]
            var merge = -1
            var mergea = 0
            val neighbors = reg.neighbors
            for (neii in 0 until neighbors.size) {
                val nei = neighbors[neii]
                val regn = regs[nei]
                if (reg.regId == regn.regId) continue
                if (reg.areaId != regn.areaId) continue
                if (regn.area > mergea) {
                    if (canMerge(reg.regId, regn.regId, regs, nregs)) {
                        mergea = regn.area
                        merge = nei
                    }
                }
            }
            if (merge != -1) {
                val oldId = reg.regId
                val newId = regs[merge].regId
                for (j in 0 until nregs) if (regs[j].regId == oldId) regs[j].regId = newId
            }
        }

        // Compact ids.
        val remap = IntArray(256)
        // Find number of unique regions.
        regId = 0
        for (i in 0 until nregs) remap[regs[i].regId] = 1
        for (i in 0..255) if (remap[i] != 0) remap[i] = regId++
        // Remap ids.
        for (i in 0 until nregs) regs[i].regId = remap[regs[i].regId]
        layer.regCount = regId
        for (i in 0 until w * h) {
            if (layer.getReg(i) != 0xff) {
                layer.setReg(i, regs[layer.getReg(i)].regId)
            }
        }
    }

    fun addUniqueLast(a: IntArrayList, v: Int) {
        val n = a.size
        if (n > 0 && a[n - 1] == v) return
        a.add(v)
    }

    fun isConnected(layer: TileCacheLayer, ia: Int, ib: Int, walkableClimb: Int): Boolean {
        if (layer.getArea(ia) != layer.getArea(ib)) return false
        return abs(layer.getHeight(ia) - layer.getHeight(ib)) <= walkableClimb
    }

    fun canMerge(oldRegId: Int, newRegId: Int, regs: Array<LayerMonotoneRegion>, nregs: Int): Boolean {
        var count = 0
        for (i in 0 until nregs) {
            val reg = regs[i]
            if (reg.regId != oldRegId) continue
            val neighbors = reg.neighbors
            for (neii in 0 until neighbors.size) {
                val nei = neighbors[neii]
                if (regs[nei].regId == newRegId) count++
            }
        }
        return count == 1
    }

    private fun appendVertex(cont: TempContour, x: Int, y: Int, z: Int, r: Int) {
        // Try to merge with existing segments.
        val conv = cont.vertices
        if (cont.numVertices > 1) {
            val pa = (cont.numVertices - 2) * 4
            val pb = (cont.numVertices - 1) * 4
            if (conv[pb + 3] == r) {
                if (conv[pa] == conv[pb] && conv[pb] == x) {
                    // The vertices are aligned along x-axis, update z.
                    conv[pb + 1] = y
                    conv[pb + 2] = z
                    return
                } else if (conv[pa + 2] == conv[pb + 2] && conv[pb + 2] == z) {
                    // The vertices are aligned along z-axis, update x.
                    conv[pb] = x
                    conv[pb + 1] = y
                    return
                }
            }
        }
        conv.add(x)
        conv.add(y)
        conv.add(z)
        conv.add(r)
        cont.numVertices++
    }

    private fun getNeighbourReg(layer: TileCacheLayer, ax: Int, ay: Int, dir: Int): Int {
        val w = layer.width
        val ia = ax + ay * w
        val cons = layer.getCon(ia)
        val con = cons and 0xf
        val portal = cons shr 4
        val mask = 1 shl dir
        if (con and mask == 0) {
            // No connection, return portal or hard edge.
            return if (portal and mask != 0) 0xf8 + dir else 0xff
        }
        val bx = ax + getDirOffsetX(dir)
        val by = ay + getDirOffsetY(dir)
        val ib = bx + by * w
        return layer.getReg(ib)
    }

    private fun getDirOffsetX(dir: Int): Int {
        val offset = intArrayOf(-1, 0, 1, 0)
        return offset[dir and 0x03]
    }

    private fun getDirOffsetY(dir: Int): Int {
        val offset = intArrayOf(0, 1, 0, -1)
        return offset[dir and 0x03]
    }

    private fun walkContour(layer: TileCacheLayer, x: Int, y: Int, cont: TempContour) {
        var x = x
        var y = y
        val w = layer.width
        val h = layer.height
        cont.clear()
        val startX = x
        val startY = y
        var startDir = -1
        for (i in 0..3) {
            val dir = i + 3 and 3
            val rn = getNeighbourReg(layer, x, y, dir)
            if (rn != layer.getReg(x + y * w)) {
                startDir = dir
                break
            }
        }
        if (startDir == -1) return
        var dir = startDir
        val maxIter = w * h
        var iter = 0
        while (iter < maxIter) {
            val rn = getNeighbourReg(layer, x, y, dir)
            var nx = x
            var ny = y
            var ndir: Int
            if (rn != layer.getReg(x + y * w)) {
                // Solid edge.
                var px = x
                var pz = y
                when (dir) {
                    0 -> pz++
                    1 -> {
                        px++
                        pz++
                    }
                    2 -> px++
                }

                // Try to merge with previous vertex.
                appendVertex(cont, px, layer.getHeight(x + y * w), pz, rn)
                ndir = dir + 1 and 0x3 // Rotate CW
            } else {
                // Move to next.
                nx = x + getDirOffsetX(dir)
                ny = y + getDirOffsetY(dir)
                ndir = dir + 3 and 0x3 // Rotate CCW
            }
            if (iter > 0 && x == startX && y == startY && dir == startDir) break
            x = nx
            y = ny
            dir = ndir
            iter++
        }

        // Remove last vertex if it is duplicate of the first one.
        val pa = (cont.numVertices - 1) * 4
        val pb = 0
        if (cont.vertices[pa] == cont.vertices[pb]
            && cont.vertices[pa + 2] == cont.vertices[pb + 2]
        ) cont.numVertices--
    }

    private fun distancePtSeg(x: Int, z: Int, px: Int, pz: Int, qx: Int, qz: Int): Float {
        val pqx = (qx - px).toFloat()
        val pqz = (qz - pz).toFloat()
        var dx = (x - px).toFloat()
        var dz = (z - pz).toFloat()
        val d = pqx * pqx + pqz * pqz
        var t = pqx * dx + pqz * dz
        if (d > 0) t /= d
        if (t < 0) t = 0f else if (t > 1) t = 1f
        dx = px + t * pqx - x
        dz = pz + t * pqz - z
        return dx * dx + dz * dz
    }

    private fun simplifyContour(cont: TempContour, maxError: Float) {
        cont.poly.clear()
        for (i in 0 until cont.numVertices) {
            val j = (i + 1) % cont.numVertices
            // Check for start of a wall segment.
            val ra = j * 4 + 3
            val rb = i * 4 + 3
            if (cont.vertices[ra] != cont.vertices[rb]) cont.poly.add(i)
        }
        if (cont.npoly() < 2) {
            // If there is no transitions at all,
            // create some initial points for the simplification process.
            // Find lower-left and upper-right vertices of the contour.
            var llx = cont.vertices[0]
            var llz = cont.vertices[2]
            var lli = 0
            var urx = cont.vertices[0]
            var urz = cont.vertices[2]
            var uri = 0
            for (i in 1 until cont.numVertices) {
                val x = cont.vertices[i * 4]
                val z = cont.vertices[i * 4 + 2]
                if (x < llx || x == llx && z < llz) {
                    llx = x
                    llz = z
                    lli = i
                }
                if (x > urx || x == urx && z > urz) {
                    urx = x
                    urz = z
                    uri = i
                }
            }
            cont.poly.clear()
            cont.poly.add(lli)
            cont.poly.add(uri)
        }

        // Add points until all raw points are within
        // error tolerance to the simplified shape.
        run {
            var i = 0
            while (i < cont.npoly()) {
                val ii = (i + 1) % cont.npoly()
                val ai = cont.poly[i]
                val ax = cont.vertices[ai * 4]
                val az = cont.vertices[ai * 4 + 2]
                val bi = cont.poly[ii]
                val bx = cont.vertices[bi * 4]
                val bz = cont.vertices[bi * 4 + 2]

                // Find maximum deviation from the segment.
                var maxd = 0f
                var maxi = -1
                var ci: Int
                var cinc: Int
                var endi: Int

                // Traverse the segment in lexilogical order so that the
                // max deviation is calculated similarly when traversing
                // opposite segments.
                if (bx > ax || bx == ax && bz > az) {
                    cinc = 1
                    ci = (ai + cinc) % cont.numVertices
                    endi = bi
                } else {
                    cinc = cont.numVertices - 1
                    ci = (bi + cinc) % cont.numVertices
                    endi = ai
                }

                // Tessellate only outer edges or edges between areas.
                while (ci != endi) {
                    val d = distancePtSeg(cont.vertices[ci * 4], cont.vertices[ci * 4 + 2], ax, az, bx, bz)
                    if (d > maxd) {
                        maxd = d
                        maxi = ci
                    }
                    ci = (ci + cinc) % cont.numVertices
                }

                // If the max deviation is larger than accepted error,
                // add new point, else continue to next segment.
                if (maxi != -1 && maxd > maxError * maxError) {
                    cont.poly.add(i + 1, maxi)
                } else {
                    ++i
                }
            }
        }

        // Remap vertices
        var start = 0
        for (i in 1 until cont.npoly()) if (cont.poly[i] < cont.poly[start]) start = i
        cont.numVertices = 0
        for (i in 0 until cont.npoly()) {
            val j = (start + i) % cont.npoly()
            val src = cont.poly[j] * 4
            val dst = cont.numVertices * 4
            cont.vertices[dst] = cont.vertices[src]
            cont.vertices[dst + 1] = cont.vertices[src + 1]
            cont.vertices[dst + 2] = cont.vertices[src + 2]
            cont.vertices[dst + 3] = cont.vertices[src + 3]
            cont.numVertices++
        }
    }

    // TODO: move this somewhere else, once the layer meshing is done.
    fun buildTileCacheContours(layer: TileCacheLayer, walkableClimb: Int, maxError: Float): TileCacheContourSet {
        val w = layer.width
        val h = layer.height
        val lcset = TileCacheContourSet()
        lcset.nconts = layer.regCount
        lcset.conts = Array(lcset.nconts) { TileCacheContour() }
        // Allocate temp buffer for contour tracing.
        val temp = TempContour()

        // Find contours.
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = x + y * w
                val ri = layer.getReg(idx)
                if (ri == 0xff) continue
                val cont = lcset.conts[ri]
                if (cont.nvertices > 0) continue
                cont.reg = ri
                cont.area = layer.getArea(idx)
                walkContour(layer, x, y, temp)
                simplifyContour(temp, maxError)

                // Store contour.
                cont.nvertices = temp.numVertices
                if (cont.nvertices > 0) {
                    cont.vertices = IntArray(4 * temp.numVertices)
                    var i = 0
                    var j = temp.numVertices - 1
                    while (i < temp.numVertices) {
                        val dst = j * 4
                        val v = j * 4
                        val vn = i * 4
                        val nei = temp.vertices[vn + 3] // The neighbour reg
                        // is
                        // stored at segment
                        // vertex of a
                        // segment.
                        val (lh, shouldRemove) = getCornerHeight(
                            layer, temp.vertices[v], temp.vertices[v + 1],
                            temp.vertices[v + 2], walkableClimb
                        )
                        cont.vertices[dst] = temp.vertices[v]
                        cont.vertices[dst + 1] = lh
                        cont.vertices[dst + 2] = temp.vertices[v + 2]

                        // Store portal direction and remove status to the
                        // fourth component.
                        cont.vertices[dst + 3] = 0x0f
                        if (nei != 0xff && nei >= 0xf8) cont.vertices[dst + 3] = nei - 0xf8
                        if (shouldRemove) cont.vertices[dst + 3] = cont.vertices[dst + 3] or 0x80
                        j = i++
                    }
                }
            }
        }
        return lcset
    }

    private fun computeVertexHash2(x: Int, y: Int, z: Int): Int {
        val h1 = -0x72594cbd // Large multiplicative constants;
        val h2 = -0x27e9c7bf // here arbitrarily chosen primes
        val h3 = -0x34e54ce1
        val n = h1 * x + h2 * y + h3 * z
        return n and VERTEX_BUCKET_COUNT2 - 1
    }

    private fun addVertex(
        x: Int, y: Int, z: Int,
        vertices: IntArray,
        firstVert: IntArray,
        nextVert: IntArray,
        nv: Int
    ): Int {
        val bucket = computeVertexHash2(x, 0, z)
        var i = firstVert[bucket]
        while (i != TILECACHE_NULL_IDX) {
            val v = i * 3
            if (vertices[v] == x && vertices[v + 2] == z && abs(vertices[v + 1] - y) <= 2) return i
            i = nextVert[i] // next
        }

        // Could not find, create new.
        i = nv
        val v = i * 3
        vertices[v] = x
        vertices[v + 1] = y
        vertices[v + 2] = z
        nextVert[i] = firstVert[bucket]
        firstVert[bucket] = i
        return i
    }

    private fun buildMeshAdjacency(
        polys: IntArray, npolys: Int, vertices: IntArray, nvertices: Int, lcset: TileCacheContourSet,
        maxVerticesPerPoly: Int
    ) {
        // Based on code by Eric Lengyel from:
        // http://www.terathon.com/code/edges.php
        val maxEdgeCount = npolys * maxVerticesPerPoly
        val firstEdge = IntArray(nvertices + maxEdgeCount)
        var edgeCount = 0
        val edges = Array(maxEdgeCount) { Edge() }
        for (i in 0 until nvertices) firstEdge[i] = TILECACHE_NULL_IDX
        for (i in 0 until npolys) {
            val t = i * maxVerticesPerPoly * 2
            for (j in 0 until maxVerticesPerPoly) {
                if (polys[t + j] == TILECACHE_NULL_IDX) break
                val v0 = polys[t + j]
                val v1 =
                    if (j + 1 >= maxVerticesPerPoly || polys[t + j + 1] == TILECACHE_NULL_IDX) polys[t] else polys[t + j + 1]
                if (v0 < v1) {
                    val edge = edges[edgeCount]
                    edge.vert0 = v0
                    edge.vert0 = v1
                    edge.poly0 = i
                    edge.polyEdge0 = j
                    edge.poly1 = i
                    edge.polyEdge1 = 0xff
                    // Insert edge
                    firstEdge[nvertices + edgeCount] = firstEdge[v0]
                    firstEdge[v0] = edgeCount.toShort().toInt()
                    edgeCount++
                }
            }
        }
        for (i in 0 until npolys) {
            val t = i * maxVerticesPerPoly * 2
            for (j in 0 until maxVerticesPerPoly) {
                if (polys[t + j] == TILECACHE_NULL_IDX) break
                val v0 = polys[t + j]
                val v1 =
                    if (j + 1 >= maxVerticesPerPoly || polys[t + j + 1] == TILECACHE_NULL_IDX) polys[t] else polys[t + j + 1]
                if (v0 > v1) {
                    var found = false
                    var e = firstEdge[v1]
                    while (e != TILECACHE_NULL_IDX) {
                        val edge = edges[e]
                        if (edge.vert1 == v0 && edge.poly0 == edge.poly1) {
                            edge.poly1 = i
                            edge.polyEdge1 = j
                            found = true
                            break
                        }
                        e = firstEdge[nvertices + e]
                    }
                    if (!found) {
                        // Matching edge not found, it is an open edge, add it.
                        val edge = edges[edgeCount]
                        edge.vert0 = v1
                        edge.vert1 = v0
                        edge.poly0 = i.toShort().toInt()
                        edge.polyEdge0 = j.toShort().toInt()
                        edge.poly1 = i.toShort().toInt()
                        edge.polyEdge1 = 0xff
                        // Insert edge
                        firstEdge[nvertices + edgeCount] = firstEdge[v1]
                        firstEdge[v1] = edgeCount.toShort().toInt()
                        edgeCount++
                    }
                }
            }
        }

        // Mark portal edges.
        for (i in 0 until lcset.nconts) {
            val cont = lcset.conts[i]
            if (cont.nvertices < 3) continue
            var j = 0
            var k = cont.nvertices - 1
            while (j < cont.nvertices) {
                val va = k * 4
                val vb = j * 4
                val dir = cont.vertices[va + 3] and 0xf
                if (dir == 0xf) {
                    k = j++
                    continue
                }
                if (dir == 0 || dir == 2) {
                    // Find matching vertical edge
                    val x = cont.vertices[va]
                    var zmin = cont.vertices[va + 2]
                    var zmax = cont.vertices[vb + 2]
                    if (zmin > zmax) {
                        val tmp = zmin
                        zmin = zmax
                        zmax = tmp
                    }
                    for (m in 0 until edgeCount) {
                        val e = edges[m]
                        // Skip connected edges.
                        if (e.poly0 != e.poly1) continue
                        val eva = e.vert0 * 3
                        val evb = e.vert1 * 3
                        if (vertices[eva] == x && vertices[evb] == x) {
                            var ezmin = vertices[eva + 2]
                            var ezmax = vertices[evb + 2]
                            if (ezmin > ezmax) {
                                val tmp = ezmin
                                ezmin = ezmax
                                ezmax = tmp
                            }
                            if (overlapRangeExl(zmin, zmax, ezmin, ezmax)) {
                                // Reuse the other polyedge to store dir.
                                e.polyEdge1 = dir
                            }
                        }
                    }
                } else {
                    // Find matching vertical edge
                    val z = cont.vertices[va + 2]
                    var xmin = cont.vertices[va]
                    var xmax = cont.vertices[vb]
                    if (xmin > xmax) {
                        val tmp = xmin
                        xmin = xmax
                        xmax = tmp
                    }
                    for (m in 0 until edgeCount) {
                        val e = edges[m]
                        // Skip connected edges.
                        if (e.poly0 != e.poly1) continue
                        val eva = e.vert0 * 3
                        val evb = e.vert1 * 3
                        if (vertices[eva + 2] == z && vertices[evb + 2] == z) {
                            var exmin = vertices[eva]
                            var exmax = vertices[evb]
                            if (exmin > exmax) {
                                val tmp = exmin
                                exmin = exmax
                                exmax = tmp
                            }
                            if (overlapRangeExl(xmin, xmax, exmin, exmax)) {
                                // Reuse the other polyedge to store dir.
                                e.polyEdge1 = dir
                            }
                        }
                    }
                }
                k = j++
            }
        }

        // Store adjacency
        for (i in 0 until edgeCount) {
            val e = edges[i]
            if (e.poly0 != e.poly1) {
                val p0 = e.poly0 * maxVerticesPerPoly * 2
                val p1 = e.poly1 * maxVerticesPerPoly * 2
                polys[p0 + maxVerticesPerPoly + e.polyEdge0] = e.poly1
                polys[p1 + maxVerticesPerPoly + e.polyEdge1] = e.poly0
            } else if (e.polyEdge1 != 0xff) {
                val p0 = e.poly0 * maxVerticesPerPoly * 2
                polys[p0 + maxVerticesPerPoly + e.polyEdge0] = 0x8000 or e.polyEdge1.toShort().toInt()
            }
        }
    }

    private fun overlapRangeExl(amin: Int, amax: Int, bmin: Int, bmax: Int): Boolean {
        return amin < bmax && amax > bmin
    }

    private fun prev(i: Int, n: Int): Int {
        return if (i - 1 >= 0) i - 1 else n - 1
    }

    private fun next(i: Int, n: Int): Int {
        return if (i + 1 < n) i + 1 else 0
    }

    private fun area2(vertices: IntArray, a: Int, b: Int, c: Int): Int {
        return ((vertices[b] - vertices[a]) * (vertices[c + 2] - vertices[a + 2])
                - (vertices[c] - vertices[a]) * (vertices[b + 2] - vertices[a + 2]))
    }

    // Returns true iff c is strictly to the left of the directed
    // line through a to b.
    private fun left(vertices: IntArray, a: Int, b: Int, c: Int): Boolean {
        return area2(vertices, a, b, c) < 0
    }

    private fun leftOn(vertices: IntArray, a: Int, b: Int, c: Int): Boolean {
        return area2(vertices, a, b, c) <= 0
    }

    private fun collinear(vertices: IntArray, a: Int, b: Int, c: Int): Boolean {
        return area2(vertices, a, b, c) == 0
    }

    // Returns true iff ab properly intersects cd: they share
    // a point interior to both segments. The properness of the
    // intersection is ensured by using strict leftness.
    private fun intersectProp(vertices: IntArray, a: Int, b: Int, c: Int, d: Int): Boolean {
        // Eliminate improper cases.
        return if (
            collinear(vertices, a, b, c) ||
            collinear(vertices, a, b, d) ||
            collinear(vertices, c, d, a) ||
            collinear(vertices, c, d, b)
        ) false else (left(vertices, a, b, c) xor left(vertices, a, b, d)) &&
                (left(vertices, c, d, a) xor left(vertices, c, d, b))
    }

    // Returns T iff (a,b,c) are collinear and point c lies
    // on the closed segement ab.
    private fun between(vertices: IntArray, a: Int, b: Int, c: Int): Boolean {
        if (!collinear(vertices, a, b, c)) return false
        // If ab not vertical, check betweenness on x; else on y.
        return if (vertices[a] != vertices[b]) vertices[a] <= vertices[c] && vertices[c] <= vertices[b] || vertices[a] >= vertices[c] && vertices[c] >= vertices[b]
        else (vertices[a + 2] <= vertices[c + 2] && vertices[c + 2] <= vertices[b + 2] || vertices[a + 2] >= vertices[c + 2] && vertices[c + 2] >= vertices[b + 2])
    }

    // Returns true iff segments ab and cd intersect, properly or improperly.
    private fun intersect(vertices: IntArray, a: Int, b: Int, c: Int, d: Int): Boolean {
        return if (intersectProp(vertices, a, b, c, d)) true
        else between(vertices, a, b, c) || between(vertices, a, b, d) ||
                between(vertices, c, d, a) || between(vertices, c, d, b)
    }

    private fun vequal(vertices: IntArray, a: Int, b: Int): Boolean {
        return vertices[a] == vertices[b] && vertices[a + 2] == vertices[b + 2]
    }

    // Returns T iff (v_i, v_j) is a proper internal *or* external
    // diagonal of P, *ignoring edges incident to v_i and v_j*.
    private fun diagonalie(i: Int, j: Int, n: Int, vertices: IntArray, indices: IntArray): Boolean {
        val d0 = (indices[i] and 0x7fff) * 4
        val d1 = (indices[j] and 0x7fff) * 4

        // For each edge (k,k+1) of P
        for (k in 0 until n) {
            val k1 = next(k, n)
            // Skip edges incident to i or j
            if (k == i || k1 == i || k == j || k1 != j) {
                val p0 = (indices[k] and 0x7fff) * 4
                val p1 = (indices[k1] and 0x7fff) * 4
                if (vequal(vertices, d0, p0) || vequal(vertices, d1, p0) || vequal(vertices, d0, p1) || vequal(
                        vertices,
                        d1,
                        p1
                    )
                ) continue
                if (intersect(vertices, d0, d1, p0, p1)) return false
            }
        }
        return true
    }

    // Returns true iff the diagonal (i,j) is strictly internal to the
    // polygon P in the neighborhood of the i endpoint.
    private fun inCone(i: Int, j: Int, n: Int, vertices: IntArray, indices: IntArray): Boolean {
        val pi = (indices[i] and 0x7fff) * 4
        val pj = (indices[j] and 0x7fff) * 4
        val pi1 = (indices[next(i, n)] and 0x7fff) * 4
        val pin1 = (indices[prev(i, n)] and 0x7fff) * 4

        // If P[i] is a convex vertex [ i+1 left or on (i-1,i) ].
        return if (leftOn(vertices, pin1, pi, pi1)) left(vertices, pi, pj, pin1) && left(
            vertices,
            pj,
            pi,
            pi1
        ) else !(leftOn(vertices, pi, pj, pi1) && leftOn(vertices, pj, pi, pin1))
        // Assume (i-1,i,i+1) not collinear.
        // else P[i] is reflex.
    }

    // Returns T iff (v_i, v_j) is a proper internal
    // diagonal of P.
    private fun diagonal(i: Int, j: Int, n: Int, vertices: IntArray, indices: IntArray): Boolean {
        return inCone(i, j, n, vertices, indices) && diagonalie(i, j, n, vertices, indices)
    }

    private fun triangulate(n: Int, vertices: IntArray, indices: IntArray, tris: IntArray): Int {
        var ni = n
        var ntris = 0
        var dst = 0 // tris;
        // The last bit of the index is used to indicate if the vertex can be
        // removed.
        for (i in 0 until ni) {
            val i1 = next(i, ni)
            val i2 = next(i1, ni)
            if (diagonal(i, i2, ni, vertices, indices)) indices[i1] = indices[i1] or 0x8000
        }
        while (ni > 3) {
            var minLen = -1
            var mini = -1
            for (i in 0 until ni) {
                val i1 = next(i, ni)
                if (indices[i1] and 0x8000 != 0) {
                    val p0 = (indices[i] and 0x7fff) * 4
                    val p2 = (indices[next(i1, ni)] and 0x7fff) * 4
                    val dx = vertices[p2] - vertices[p0]
                    val dz = vertices[p2 + 2] - vertices[p0 + 2]
                    val len = dx * dx + dz * dz
                    if (minLen < 0 || len < minLen) {
                        minLen = len
                        mini = i
                    }
                }
            }
            if (mini == -1) {
                // Should not happen.
                /*
                 * printf("mini == -1 ntris=%d n=%d\n", ntris, n); for (int i = 0; i < n; i++) { printf("%d ",
                 * indices[i] & 0x0fffffff); } printf("\n");
                 */
                return -ntris
            }
            var i = mini
            var i1 = next(i, ni)
            val i2 = next(i1, ni)
            tris[dst++] = indices[i] and 0x7fff
            tris[dst++] = indices[i1] and 0x7fff
            tris[dst++] = indices[i2] and 0x7fff
            ntris++

            // Removes P[i1] by copying P[i+1]...P[n-1] left one index.
            ni--
            for (k in i1 until ni) indices[k] = indices[k + 1]
            if (i1 >= ni) i1 = 0
            i = prev(i1, ni)
            // Update diagonal flags.
            if (diagonal(prev(i, ni), i1, ni, vertices, indices)) indices[i] = indices[i] or 0x8000 else indices[i] =
                indices[i] and 0x7fff
            if (diagonal(i, next(i1, ni), ni, vertices, indices)) indices[i1] = indices[i1] or 0x8000 else indices[i1] =
                indices[i1] and 0x7fff
        }

        // Append the remaining triangle.
        tris[dst++] = indices[0] and 0x7fff
        tris[dst++] = indices[1] and 0x7fff
        tris[dst] = indices[2] and 0x7fff
        ntris++
        return ntris
    }

    private fun countPolyVertices(polys: IntArray, p: Int, maxVerticesPerPoly: Int): Int {
        for (i in 0 until maxVerticesPerPoly) if (polys[p + i] == TILECACHE_NULL_IDX) return i
        return maxVerticesPerPoly
    }

    private fun uleft(vertices: IntArray, a: Int, b: Int, c: Int): Boolean {
        return (vertices[b] - vertices[a]) * (vertices[c + 2] - vertices[a + 2]) - (vertices[c] - vertices[a]) * (vertices[b + 2] - vertices[a + 2]) < 0
    }

    private fun getPolyMergeValue(
        polys: IntArray,
        pa: Int,
        pb: Int,
        vertices: IntArray,
        maxVerticesPerPoly: Int
    ): IntArray {
        val na = countPolyVertices(polys, pa, maxVerticesPerPoly)
        val nb = countPolyVertices(polys, pb, maxVerticesPerPoly)

        // If the merged polygon would be too big, do not merge.
        if (na + nb - 2 > maxVerticesPerPoly) return intArrayOf(-1, 0, 0)

        // Check if the polygons share an edge.
        var ea = -1
        var eb = -1
        for (i in 0 until na) {
            var va0 = polys[pa + i]
            var va1 = polys[pa + (i + 1) % na]
            if (va0 > va1) {
                val tmp = va0
                va0 = va1
                va1 = tmp
            }
            for (j in 0 until nb) {
                var vb0 = polys[pb + j]
                var vb1 = polys[pb + (j + 1) % nb]
                if (vb0 > vb1) {
                    val tmp = vb0
                    vb0 = vb1
                    vb1 = tmp
                }
                if (va0 == vb0 && va1 == vb1) {
                    ea = i
                    eb = j
                    break
                }
            }
        }

        // No common edge, cannot merge.
        if (ea == -1 || eb == -1) return intArrayOf(-1, ea, eb)

        // Check to see if the merged polygon would be convex.
        var va: Int
        var vb: Int
        var vc: Int
        va = polys[pa + (ea + na - 1) % na]
        vb = polys[pa + ea]
        vc = polys[pb + (eb + 2) % nb]
        if (!uleft(vertices, va * 3, vb * 3, vc * 3)) return intArrayOf(-1, ea, eb)
        va = polys[pb + (eb + nb - 1) % nb]
        vb = polys[pb + eb]
        vc = polys[pa + (ea + 2) % na]
        if (!uleft(vertices, va * 3, vb * 3, vc * 3)) return intArrayOf(-1, ea, eb)
        va = polys[pa + ea]
        vb = polys[pa + (ea + 1) % na]
        val dx = vertices[va * 3] - vertices[vb * 3]
        val dy = vertices[va * 3 + 2] - vertices[vb * 3 + 2]
        return intArrayOf(dx * dx + dy * dy, ea, eb)
    }

    private fun mergePolys(polys: IntArray, pa: Int, pb: Int, ea: Int, eb: Int, maxVerticesPerPoly: Int) {
        val tmp = IntArray(maxVerticesPerPoly * 2)
        val na = countPolyVertices(polys, pa, maxVerticesPerPoly)
        val nb = countPolyVertices(polys, pb, maxVerticesPerPoly)

        // Merge polygons.
        tmp.fill(TILECACHE_NULL_IDX)
        var n = 0
        // Add pa
        for (i in 0 until na - 1) tmp[n++] = polys[pa + (ea + 1 + i) % na]
        // Add pb
        for (i in 0 until nb - 1) tmp[n++] = polys[pb + (eb + 1 + i) % nb]
        System.arraycopy(tmp, 0, polys, pa, maxVerticesPerPoly)
    }

    private fun pushFront(v: Int, arr: IntArrayList): Int {
        arr.add(0, v)
        return arr.size
    }

    private fun pushBack(v: Int, arr: IntArrayList): Int {
        arr.add(v)
        return arr.size
    }

    private fun canRemoveVertex(mesh: TileCachePolyMesh, rem: Int): Boolean {
        // Count number of polygons to remove.
        val maxVerticesPerPoly = mesh.nvp
        var numRemainingEdges = 0
        for (i in 0 until mesh.numPolygons) {
            val p = i * mesh.nvp * 2
            val nv = countPolyVertices(mesh.polys, p, maxVerticesPerPoly)
            var numRemoved = 0
            var numVertices = 0
            for (j in 0 until nv) {
                if (mesh.polys[p + j] == rem) {
                    numRemoved++
                }
                numVertices++
            }
            if (numRemoved != 0) {
                numRemainingEdges += numVertices - (numRemoved + 1)
            }
        }

        // There would be too few edges remaining to create a polygon.
        // This can happen for example when a tip of a triangle is marked
        // as deletion, but there are no other polys that share the vertex.
        // In this case, the vertex should not be removed.
        if (numRemainingEdges <= 2) return false

        // Find edges which share the removed vertex.
        val edges = IntArrayList()
        var nedges = 0
        for (i in 0 until mesh.numPolygons) {
            val p = i * mesh.nvp * 2
            val nv = countPolyVertices(mesh.polys, p, maxVerticesPerPoly)

            // Collect edges which touches the removed vertex.
            var j = 0
            var k = nv - 1
            while (j < nv) {
                if (mesh.polys[p + j] == rem || mesh.polys[p + k] == rem) {
                    // Arrange edge so that a=rem.
                    var a = mesh.polys[p + j]
                    var b = mesh.polys[p + k]
                    if (b == rem) {
                        val tmp = a
                        a = b
                        b = tmp
                    }

                    // Check if the edge exists
                    var exists = false
                    for (m in 0 until nedges) {
                        val e = m * 3
                        if (edges[e + 1] == b) {
                            // Exists, increment vertex share count.
                            edges[e + 2] = edges[e + 2] + 1
                            exists = true
                        }
                    }
                    // Add new edge.
                    if (!exists) {
                        edges.add(a)
                        edges.add(b)
                        edges.add(1)
                        nedges++
                    }
                }
                k = j++
            }
        }

        // There should be no more than 2 open edges.
        // This catches the case that two non-adjacent polygons
        // share the removed vertex. In that case, do not remove the vertex.
        var numOpenEdges = 0
        for (i in 0 until nedges) {
            if (edges[i * 3 + 2] < 2) numOpenEdges++
        }
        return numOpenEdges <= 2
    }

    private fun removeVertex(mesh: TileCachePolyMesh, rem: Int, maxTris: Int) {
        // Count number of polygons to remove.
        val maxVerticesPerPoly = mesh.nvp
        var nedges = 0
        val edges = IntArrayList()
        var nhole: Int
        val hole = IntArrayList()
        val harea = IntArrayList()
        run {
            var i = 0
            while (i < mesh.numPolygons) {
                val p = i * maxVerticesPerPoly * 2
                val nv = countPolyVertices(mesh.polys, p, maxVerticesPerPoly)
                var hasRem = false
                for (j in 0 until nv) if (mesh.polys[p + j] == rem) {
                    hasRem = true
                    break
                }
                if (hasRem) {
                    // Collect edges which does not touch the removed vertex.
                    var j = 0
                    var k = nv - 1
                    while (j < nv) {
                        if (mesh.polys[p + j] != rem && mesh.polys[p + k] != rem) {
                            edges.add(mesh.polys[p + k])
                            edges.add(mesh.polys[p + j])
                            edges.add(mesh.areas[i])
                            nedges++
                        }
                        k = j++
                    }
                    // Remove the polygon.
                    val p2 = (mesh.numPolygons - 1) * maxVerticesPerPoly * 2
                    System.arraycopy(mesh.polys, p2, mesh.polys, p, maxVerticesPerPoly)
                    mesh.polys.fill(TILECACHE_NULL_IDX, p + maxVerticesPerPoly, p + 2 * maxVerticesPerPoly)
                    mesh.areas[i] = mesh.areas[mesh.numPolygons - 1]
                    mesh.numPolygons--
                    --i
                }
                ++i
            }
        }

        // Remove vertex.
        for (i in rem until mesh.numVertices) {
            mesh.vertices[i * 3] = mesh.vertices[(i + 1) * 3]
            mesh.vertices[i * 3 + 1] = mesh.vertices[(i + 1) * 3 + 1]
            mesh.vertices[i * 3 + 2] = mesh.vertices[(i + 1) * 3 + 2]
        }
        mesh.numVertices--

        // Adjust indices to match the removed vertex layout.
        for (i in 0 until mesh.numPolygons) {
            val p = i * maxVerticesPerPoly * 2
            val nv = countPolyVertices(mesh.polys, p, maxVerticesPerPoly)
            for (j in 0 until nv) if (mesh.polys[p + j] > rem) mesh.polys[p + j]--
        }
        for (i in 0 until nedges) {
            if (edges[i * 3] > rem) edges[i * 3] = edges[i * 3] - 1
            if (edges[i * 3 + 1] > rem) edges[i * 3 + 1] = edges[i * 3 + 1] - 1
        }
        if (nedges == 0) return

        // Start with one vertex, keep appending connected
        // segments to the start and end of the hole.
        nhole = pushBack(edges[0], hole)
        pushBack(edges[2], harea)
        while (nedges != 0) {
            var match = false
            var i = 0
            while (i < nedges) {
                val ea = edges[i * 3]
                val eb = edges[i * 3 + 1]
                val a = edges[i * 3 + 2]
                var add = false
                if (hole[0] == eb) {
                    // The segment matches the beginning of the hole boundary.
                    nhole = pushFront(ea, hole)
                    pushFront(a, harea)
                    add = true
                } else if (hole[nhole - 1] == ea) {
                    // The segment matches the end of the hole boundary.
                    nhole = pushBack(eb, hole)
                    pushBack(a, harea)
                    add = true
                }
                if (add) {
                    // The edge segment was added, remove it.
                    edges[i * 3] = edges[(nedges - 1) * 3]
                    edges[i * 3 + 1] = edges[(nedges - 1) * 3] + 1
                    edges[i * 3 + 2] = edges[(nedges - 1) * 3] + 2
                    --nedges
                    match = true
                    --i
                }
                ++i
            }
            if (!match) break
        }
        val tris = IntArray(nhole * 3)
        val tvertices = IntArray(nhole * 4)
        val tpoly = IntArray(nhole)

        // Generate temp vertex array for triangulation.
        for (i in 0 until nhole) {
            val pi = hole[i]
            tvertices[i * 4] = mesh.vertices[pi * 3]
            tvertices[i * 4 + 1] = mesh.vertices[pi * 3 + 1]
            tvertices[i * 4 + 2] = mesh.vertices[pi * 3 + 2]
            tvertices[i * 4 + 3] = 0
            tpoly[i] = i
        }

        // Triangulate the hole.
        var ntris = triangulate(nhole, tvertices, tpoly, tris)
        if (ntris < 0) {
            // TODO: issue warning!
            ntris = -ntris
        }
        val polys = IntArray(ntris * maxVerticesPerPoly)
        val pareas = IntArray(ntris)

        // Build initial polygons.
        var npolys = 0
        polys.fill(TILECACHE_NULL_IDX, 0, ntris * maxVerticesPerPoly)
        for (j in 0 until ntris) {
            val t = j * 3
            if (tris[t] != tris[t + 1] && tris[t] != tris[t + 2] && tris[t + 1] != tris[t + 2]) {
                polys[npolys * maxVerticesPerPoly] = hole[tris[t]]
                polys[npolys * maxVerticesPerPoly + 1] = hole[tris[t + 1]]
                polys[npolys * maxVerticesPerPoly + 2] = hole[tris[t + 2]]
                pareas[npolys] = harea[tris[t]]
                npolys++
            }
        }
        if (npolys == 0) return

        // Merge polygons.
        if (maxVerticesPerPoly > 3) {
            while (true) {

                // Find best polygons to merge.
                var bestMergeVal = 0
                var bestPa = 0
                var bestPb = 0
                var bestEa = 0
                var bestEb = 0
                for (j in 0 until npolys - 1) {
                    val pj = j * maxVerticesPerPoly
                    for (k in j + 1 until npolys) {
                        val pk = k * maxVerticesPerPoly
                        val pm = getPolyMergeValue(polys, pj, pk, mesh.vertices, maxVerticesPerPoly)
                        val v = pm[0]
                        val ea = pm[1]
                        val eb = pm[2]
                        if (v > bestMergeVal) {
                            bestMergeVal = v
                            bestPa = j
                            bestPb = k
                            bestEa = ea
                            bestEb = eb
                        }
                    }
                }
                if (bestMergeVal > 0) {
                    // Found best, merge.
                    val pa = bestPa * maxVerticesPerPoly
                    val pb = bestPb * maxVerticesPerPoly
                    mergePolys(polys, pa, pb, bestEa, bestEb, maxVerticesPerPoly)
                    System.arraycopy(polys, (npolys - 1) * maxVerticesPerPoly, polys, pb, maxVerticesPerPoly)
                    pareas[bestPb] = pareas[npolys - 1]
                    npolys--
                } else {
                    // Could not merge any polygons, stop.
                    break
                }
            }
        }

        // Store polygons.
        for (i in 0 until npolys) {
            if (mesh.numPolygons >= maxTris) break
            val p = mesh.numPolygons * maxVerticesPerPoly * 2
            mesh.polys.fill(TILECACHE_NULL_IDX, p, p + maxVerticesPerPoly * 2)
            for (j in 0 until maxVerticesPerPoly) {
                mesh.polys[p + j] = polys[i * maxVerticesPerPoly + j]
            }
            mesh.areas[mesh.numPolygons] = pareas[i]
            mesh.numPolygons++
            if (mesh.numPolygons > maxTris) {
                throw RuntimeException("Buffer too small")
            }
        }
    }

    fun buildTileCachePolyMesh(lcset: TileCacheContourSet, maxVerticesPerPoly: Int): TileCachePolyMesh {
        var maxVertices = 0
        var maxTris = 0
        var maxVerticesPerCont = 0
        for (i in 0 until lcset.nconts) {
            // Skip null contours.
            if (lcset.conts[i].nvertices < 3) continue
            maxVertices += lcset.conts[i].nvertices
            maxTris += lcset.conts[i].nvertices - 2
            maxVerticesPerCont = max(maxVerticesPerCont, lcset.conts[i].nvertices)
        }

        // TODO: warn about too many vertices?
        val mesh = TileCachePolyMesh(maxVerticesPerPoly)
        val vflags = IntArray(maxVertices)
        mesh.vertices = IntArray(maxVertices * 3)
        mesh.polys = IntArray(maxTris * maxVerticesPerPoly * 2)
        mesh.areas = IntArray(maxTris)
        // Just allocate and clean the mesh flags array. The user is resposible
        // for filling it.
        mesh.flags = IntArray(maxTris)
        mesh.numVertices = 0
        mesh.numPolygons = 0
        mesh.polys.fill(TILECACHE_NULL_IDX)
        val firstVert = IntArray(VERTEX_BUCKET_COUNT2)
        for (i in 0 until VERTEX_BUCKET_COUNT2) firstVert[i] = TILECACHE_NULL_IDX
        val nextVert = IntArray(maxVertices)
        val indices = IntArray(maxVerticesPerCont)
        val tris = IntArray(maxVerticesPerCont * 3)
        val polys = IntArray(maxVerticesPerCont * maxVerticesPerPoly)
        for (i in 0 until lcset.nconts) {
            val cont = lcset.conts[i]

            // Skip null contours.
            if (cont.nvertices < 3) continue

            // Triangulate contour
            for (j in 0 until cont.nvertices) indices[j] = j
            var ntris = triangulate(cont.nvertices, cont.vertices, indices, tris)
            if (ntris <= 0) {
                // TODO: issue warning!
                ntris = -ntris
            }

            // Add and merge vertices.
            for (j in 0 until cont.nvertices) {
                val v = j * 4
                indices[j] = addVertex(
                    cont.vertices[v], cont.vertices[v + 1], cont.vertices[v + 2], mesh.vertices, firstVert,
                    nextVert, mesh.numVertices
                )
                mesh.numVertices = max(mesh.numVertices, indices[j] + 1)
                if (cont.vertices[v + 3] and 0x80 != 0) {
                    // This vertex should be removed.
                    vflags[indices[j]] = 1
                }
            }

            // Build initial polygons.
            var npolys = 0
            polys.fill(TILECACHE_NULL_IDX)
            for (j in 0 until ntris) {
                val t = j * 3
                if (tris[t] != tris[t + 1] && tris[t] != tris[t + 2] && tris[t + 1] != tris[t + 2]) {
                    polys[npolys * maxVerticesPerPoly] = indices[tris[t]]
                    polys[npolys * maxVerticesPerPoly + 1] = indices[tris[t + 1]]
                    polys[npolys * maxVerticesPerPoly + 2] = indices[tris[t + 2]]
                    npolys++
                }
            }
            if (npolys == 0) continue

            // Merge polygons.
            if (maxVerticesPerPoly > 3) {
                while (true) {

                    // Find best polygons to merge.
                    var bestMergeVal = 0
                    var bestPa = 0
                    var bestPb = 0
                    var bestEa = 0
                    var bestEb = 0
                    for (j in 0 until npolys - 1) {
                        val pj = j * maxVerticesPerPoly
                        for (k in j + 1 until npolys) {
                            val pk = k * maxVerticesPerPoly
                            val pm = getPolyMergeValue(polys, pj, pk, mesh.vertices, maxVerticesPerPoly)
                            val v = pm[0]
                            val ea = pm[1]
                            val eb = pm[2]
                            if (v > bestMergeVal) {
                                bestMergeVal = v
                                bestPa = j
                                bestPb = k
                                bestEa = ea
                                bestEb = eb
                            }
                        }
                    }
                    if (bestMergeVal > 0) {
                        // Found best, merge.
                        val pa = bestPa * maxVerticesPerPoly
                        val pb = bestPb * maxVerticesPerPoly
                        mergePolys(polys, pa, pb, bestEa, bestEb, maxVerticesPerPoly)
                        System.arraycopy(polys, (npolys - 1) * maxVerticesPerPoly, polys, pb, maxVerticesPerPoly)
                        npolys--
                    } else {
                        // Could not merge any polygons, stop.
                        break
                    }
                }
            }

            // Store polygons.
            for (j in 0 until npolys) {
                val p = mesh.numPolygons * maxVerticesPerPoly * 2
                val q = j * maxVerticesPerPoly
                if (maxVerticesPerPoly >= 0) System.arraycopy(polys, q, mesh.polys, p, maxVerticesPerPoly)
                mesh.areas[mesh.numPolygons] = cont.area
                mesh.numPolygons++
                if (mesh.numPolygons > maxTris) throw RuntimeException("Buffer too small")
            }
        }

        // Remove edge vertices.
        var i = 0
        while (i < mesh.numVertices) {
            if (vflags[i] != 0) {
                if (!canRemoveVertex(mesh, i)) {
                    ++i
                    continue
                }
                removeVertex(mesh, i, maxTris)
                // Remove vertex
                // Note: mesh.nvertices is already decremented inside
                // removeVertex()!
                if (mesh.numVertices - i >= 0) System.arraycopy(vflags, i + 1, vflags, i, mesh.numVertices - i)
                --i
            }
            ++i
        }

        // Calculate adjacency.
        buildMeshAdjacency(mesh.polys, mesh.numPolygons, mesh.vertices, mesh.numVertices, lcset, maxVerticesPerPoly)
        return mesh
    }

    fun markCylinderArea(
        layer: TileCacheLayer, orig: Vector3f, cs: Float, ch: Float, pos: Vector3f, radius: Float,
        height: Float, areaId: Int
    ) {
        val r2 = sq(radius / cs + 0.5f)
        val w = layer.width
        val h = layer.height
        val ics = 1f / cs
        val ich = 1f / ch
        val px = (pos.x - orig.x) * ics
        val pz = (pos.z - orig.z) * ics
        var minx = floor(((pos.x - radius - orig.x) * ics)).toInt()
        val miny = floor(((pos.y - orig.y) * ich)).toInt()
        var minz = floor(((pos.z - radius - orig.z) * ics)).toInt()
        var maxx = floor(((pos.x + radius - orig.x) * ics)).toInt()
        val maxy = floor(((pos.y + height - orig.y) * ich)).toInt()
        var maxz = floor(((pos.z + radius - orig.z) * ics)).toInt()
        minx = max(minx, 0)
        maxx = min(maxx, w - 1)
        minz = max(minz, 0)
        maxz = min(maxz, h - 1)
        for (z in minz..maxz) {
            for (x in minx..maxx) {
                val dx = x + 0.5f - px
                val dz = z + 0.5f - pz
                if (dx * dx + dz * dz > r2) continue
                val y = layer.getHeight(x + z * w)
                if (y < miny || y > maxy) continue
                layer.setArea(x + z * w, areaId)
            }
        }
    }

    fun markBoxArea(
        layer: TileCacheLayer, orig: Vector3f, cs: Float, ch: Float, bmin: Vector3f, bmax: Vector3f,
        areaId: Int
    ) {
        val w = layer.width
        val h = layer.height
        val ics = 1f / cs
        val ich = 1f / ch
        var minx = floor(((bmin.x - orig.x) * ics)).toInt()
        val miny = floor(((bmin.y - orig.y) * ich)).toInt()
        var minz = floor(((bmin.z - orig.z) * ics)).toInt()
        var maxx = floor(((bmax.x - orig.x) * ics)).toInt()
        val maxy = floor(((bmax.y - orig.y) * ich)).toInt()
        var maxz = floor(((bmax.z - orig.z) * ics)).toInt()
        minx = max(minx, 0)
        maxx = min(maxx, w - 1)
        minz = max(minz, 0)
        maxz = min(maxz, h - 1)
        for (z in minz..maxz) {
            for (x in minx..maxx) {
                val y = layer.getHeight(x + z * w)
                if (y < miny || y > maxy) continue
                layer.setArea(x + z * w, areaId)
            }
        }
    }

    fun compressTileCacheLayer(layer: TileCacheLayer, order: ByteOrder, cCompatibility: Boolean): ByteArray {
        val baos = ByteArrayOutputStream()
        val hw = TileCacheLayerHeaderWriter()
        hw.write(baos, layer, order, cCompatibility)
        val gridSize = layer.width * layer.height
        val buffer = ByteArray(gridSize * 3)
        for (i in 0 until gridSize) {
            buffer[i] = layer.getHeight(i).toByte()
            buffer[gridSize + i] = layer.getArea(i).toByte()
            buffer[gridSize * 2 + i] = layer.getCon(i).toByte()
        }
        baos.write(buffer)
        return baos.toByteArray()
    }

    fun compressTileCacheLayer(
        header: TileCacheLayerHeader, heights: IntArray, areas: IntArray, cons: IntArray,
        order: ByteOrder, cCompatibility: Boolean
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val hw = TileCacheLayerHeaderWriter()
        return try {
            hw.write(baos, header, order, cCompatibility)
            val gridSize = header.width * header.height
            val buffer = ByteArray(gridSize * 3)
            for (i in 0 until gridSize) {
                buffer[i] = heights[i].toByte()
                buffer[gridSize + i] = areas[i].toByte()
                buffer[gridSize * 2 + i] = cons[i].toByte()
            }
            baos.write(buffer)
            baos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e.message, e)
        }
    }

    @Throws(IOException::class)
    fun decompressTileCacheLayer(
        compressed: ByteArray, order: ByteOrder,
        cCompatibility: Boolean
    ): TileCacheLayer {
        val buf = ByteBuffer.wrap(compressed)
        buf.order(order)
        val layer = TileCacheLayerHeaderReader.read(buf, cCompatibility, TileCacheLayer())
        val gridSize = layer.width * layer.height
        layer.init(gridSize)
        val go = buf.position()
        layer.setHeights(compressed, go)
        layer.setAreas(compressed, go + gridSize)
        layer.setCons(compressed, go + gridSize * 2)
        return layer
    }

    fun markBoxArea(
        layer: TileCacheLayer,
        orig: Vector3f,
        cs: Float,
        ch: Float,
        center: Vector3f,
        extents: Vector3f,
        rotAux: FloatArray,
        areaId: Int
    ) {
        val w = layer.width
        val h = layer.height
        val ics = 1f / cs
        val ich = 1f / ch
        val cx = (center.x - orig.x) * ics
        val cz = (center.z - orig.z) * ics
        val maxr = 1.41f * max(extents.x, extents.z)
        var minx = floor((cx - maxr * ics)).toInt()
        var maxx = floor((cx + maxr * ics)).toInt()
        var minz = floor((cz - maxr * ics)).toInt()
        var maxz = floor((cz + maxr * ics)).toInt()
        val miny = floor(((center.y - extents.y - orig.y) * ich)).toInt()
        val maxy = floor(((center.y + extents.y - orig.y) * ich)).toInt()
        minx = max(minx, 0)
        maxx = min(maxx, w - 1)
        minz = max(minz, 0)
        maxz = min(maxz, h - 1)
        val xhalf = extents.x * ics + 0.5f
        val zhalf = extents.z * ics + 0.5f
        for (z in minz..maxz) {
            for (x in minx..maxx) {
                val x2 = 2f * (x - cx)
                val z2 = 2f * (z - cz)
                val xrot = rotAux[1] * x2 + rotAux[0] * z2
                if (xrot > xhalf || xrot < -xhalf) continue
                val zrot = rotAux[1] * z2 - rotAux[0] * x2
                if (zrot > zhalf || zrot < -zhalf) continue
                val y = layer.getHeight(x + z * w)
                if (y < miny || y > maxy) continue
                layer.setArea(x + z * w, areaId)
            }
        }
    }

    companion object {
        const val TILECACHE_NULL_AREA = 0
        const val TILECACHE_WALKABLE_AREA = 63
        const val TILECACHE_NULL_IDX = 0xffff
        fun getCornerHeight(layer: TileCacheLayer, x: Int, y: Int, z: Int, walkableClimb: Int): Pair<Int, Boolean> {
            val w = layer.width
            val h = layer.height
            var n = 0
            var portal = 0xf
            var height = 0
            var preg = 0xff
            var allSameReg = true
            for (dz in -1..0) {
                for (dx in -1..0) {
                    val px = x + dx
                    val pz = z + dz
                    if (px >= 0 && pz >= 0 && px < w && pz < h) {
                        val idx = px + pz * w
                        val lh = layer.getHeight(idx)
                        if (abs(lh - y) <= walkableClimb && layer.getArea(idx) != TILECACHE_NULL_AREA) {
                            height = max(height, lh.toChar().code)
                            portal = portal and (layer.getCon(idx) shr 4)
                            if (preg != 0xff && preg != layer.getReg(idx)) allSameReg = false
                            preg = layer.getReg(idx)
                            n++
                        }
                    }
                }
            }
            var portalCount = 0
            for (dir in 0..3) if (portal and (1 shl dir) != 0) portalCount++
            var shouldRemove = false
            if (n > 1 && portalCount == 1 && allSameReg) {
                shouldRemove = true
            }
            return Pair(height, shouldRemove)
        }

        const val VERTEX_BUCKET_COUNT2 = 1 shl 8
    }
}