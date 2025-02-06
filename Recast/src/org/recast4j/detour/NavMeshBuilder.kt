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
package org.recast4j.detour

import org.joml.Vector3f
import org.recast4j.IntPair
import org.recast4j.Vectors
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object NavMeshBuilder {

    const val MESH_NULL_IDX = 0xffff

    private fun calcExtends(nodes: Array<BVNode>, imin: Int, imax: Int, dst: BVNode) {
        nodes[imin].copyBoundsInto(dst)
        for (i in imin + 1 until imax) {
            dst.union(nodes[i])
        }
    }

    private fun longestAxis(x: Int, y: Int, z: Int): Int {
        var axis = 0
        var maxVal = x
        if (y > maxVal) {
            axis = 1
            maxVal = y
        }
        return if (z > maxVal) 2 else axis
    }

    fun subdivide(srcNodes: Array<BVNode>, iMin: Int, imax: Int, curBVNode: Int, dstNodes: Array<BVNode>): Int {
        var curBVNodeI = curBVNode
        val iNum = imax - iMin
        val icur = curBVNodeI
        val n = dstNodes[curBVNodeI++]
        if (iNum == 1) {
            // Leaf
            srcNodes[iMin].copyInto(n)
        } else {
            // Split
            calcExtends(srcNodes, iMin, imax, n)
            val maxAxis = longestAxis(n.maxX - n.minX, n.maxY - n.minY, n.maxZ - n.minZ)
            val sorter = when (maxAxis) {
                0 -> CompareItemX
                1 -> CompareItemY
                else -> CompareItemZ
            }
            Arrays.sort(srcNodes, iMin, iMin + iNum, sorter)
            val iSplit = iMin + iNum / 2
            // Left
            curBVNodeI = subdivide(srcNodes, iMin, iSplit, curBVNodeI, dstNodes)
            // Right
            curBVNodeI = subdivide(srcNodes, iSplit, imax, curBVNodeI, dstNodes)
            val iEscape = curBVNodeI - icur
            // Negative index means escape.
            n.index = -iEscape
        }
        return curBVNodeI
    }

    private fun createBVTree(params: NavMeshDataCreateParams, BVNodes: Array<BVNode>): Int {
        // Build tree
        val quantFactor = 1 / params.cellSize
        val items = Array(params.polyCount) { BVNode() }
        for (i in 0 until params.polyCount) {
            val node = items[i]
            node.index = i
            // Calc polygon bounds. Use detail meshes if available.
            val pm = params.detailMeshes
            if (pm != null) {
                calculatePolygonBounds(params, pm, i, quantFactor, node)
            } else {
                calculatePolygonBounds(params, i, quantFactor, node)
            }
        }
        return subdivide(items, 0, params.polyCount, 0, BVNodes)
    }

    private fun calculatePolygonBounds(
        params: NavMeshDataCreateParams, pm: IntArray, i: Int,
        quantFactor: Float, dst: BVNode
    ) {
        val dvs = params.detailVertices!!
        val vb = pm[i * 4]
        val ndv = pm[i * 4 + 1]
        val dv = vb * 3
        val bmin = Vector3f(dvs, dv)
        val bmax = Vector3f(bmin)
        for (j in 1 until ndv) {
            Vectors.min(bmin, dvs, dv + j * 3)
            Vectors.max(bmax, dvs, dv + j * 3)
        }
        // BV-tree uses cs for all dimensions
        dst.setQuantitized(bmin, bmax, params.bmin, quantFactor)
    }

    private fun calculatePolygonBounds(params: NavMeshDataCreateParams, i: Int, quantFactor: Float, node: BVNode) {
        val p = i * params.maxVerticesPerPolygon * 2
        val pv = params.vertices!!
        val pp = params.polys!!
        node.maxX = pv[pp[p] * 3]
        node.maxY = pv[pp[p] * 3 + 1]
        node.maxZ = pv[pp[p] * 3 + 2]
        node.minX = node.maxX
        node.minY = node.maxY
        node.minZ = node.maxZ
        for (j in 1 until params.maxVerticesPerPolygon) {
            if (pp[p + j] == MESH_NULL_IDX) break
            val pi = pp[p + j] * 3
            val x = pv[pi]
            val y = pv[pi + 1]
            val z = pv[pi + 2]
            node.union(x, y, z)
        }
        // Remap y
        node.minY = floor((node.minY * params.cellHeight * quantFactor)).toInt()
        node.maxY = ceil((node.maxY * params.cellHeight * quantFactor)).toInt()
    }

    const val XP = 1
    const val ZP = 2
    const val XM = 4
    const val ZM = 8

    fun classifyOffMeshPoint(pt: VectorPtr, bmin: Vector3f, bmax: Vector3f): Int {
        var outcode = 0
        outcode = outcode or if (pt[0] >= bmax.x) XP else 0
        outcode = outcode or if (pt[2] >= bmax.z) ZP else 0
        outcode = outcode or if (pt[0] < bmin.x) XM else 0
        outcode = outcode or if (pt[2] < bmin.z) ZM else 0
        when (outcode) {
            XP -> return 0
            XP or ZP -> return 1
            ZP -> return 2
            XM or ZP -> return 3
            XM -> return 4
            XM or ZM -> return 5
            ZM -> return 6
            XP or ZM -> return 7
        }
        return 0xff
    }

    /**
     * Builds navigation mesh tile data from the provided tile creation data.
     *
     * @param params Tile creation data.
     * @return created tile data
     */
    fun createNavMeshData(params: NavMeshDataCreateParams): MeshData? {
        if (params.vertCount >= 0xffff) return null
        if (params.vertCount == 0 || params.vertices == null) return null
        if (params.polyCount == 0 || params.polys == null) return null
        val nvp = params.maxVerticesPerPolygon

        // Classify off-mesh connection points. We store only the connections
        // whose start point is inside the tile.
        var offMeshConClass: IntArray? = null
        var storedOffMeshConCount = 0
        var offMeshConLinkCount = 0
        if (params.offMeshConCount > 0) {
            offMeshConClass = IntArray(params.offMeshConCount * 2)
            val counts = findTightHeightBounds(params, offMeshConClass, offMeshConLinkCount, storedOffMeshConCount)
            offMeshConLinkCount = counts.first
            storedOffMeshConCount = counts.second
        }

        // Off-mesh connections are stored as polygons, adjust values.
        val totPolyCount = params.polyCount + storedOffMeshConCount
        val totVertCount = params.vertCount + storedOffMeshConCount * 2

        // Find portal edges, which are at tile borders.
        val pp = params.polys!!
        val maxLinkCount = findPortalEdgesAtTileBorders(params, pp, nvp, offMeshConLinkCount)

        // Find unique detail vertices.
        var uniqueDetailVertCount = 0
        var detailTriCount = 0
        val pm = params.detailMeshes
        if (pm != null) {
            // Has detail mesh, count unique detail vertex count and use input detail tri count.
            detailTriCount = params.detailTriCount
            uniqueDetailVertCount = countUniqueDetailVertexCount(params, nvp, pm, pp)
        } else {
            // No input detail mesh, build detail mesh from nav polys.
            // No extra detail vertices.
            detailTriCount = countDetailTriCount(params, nvp, pp)
        }

        val bvTreeSize = if (params.buildBvTree) params.polyCount * 2 else 0
        val header = MeshData()
        val navVertices = FloatArray(3 * totVertCount)
        val navPolys = Array(totPolyCount) { Poly(it, nvp) }
        val navDMeshes = Array(params.polyCount) { PolyDetail() }
        val navDVertices = FloatArray(3 * uniqueDetailVertCount)
        val navDTris = ByteArray(4 * detailTriCount)
        val navBVTree = Array(bvTreeSize) { BVNode() }
        val offMeshCons = Array(storedOffMeshConCount) { OffMeshConnection() }

        // Store header
        storeHeader(
            header, params, totPolyCount, totVertCount, maxLinkCount,
            uniqueDetailVertCount, detailTriCount, storedOffMeshConCount, bvTreeSize
        )

        val offMeshVerticesBase = params.vertCount
        val offMeshPolyBase = params.polyCount
        storeMeshVertices(params, navVertices)
        createOffMeshLinkVertices(params, offMeshConClass, offMeshVerticesBase, navVertices)

        storeMeshPolygons(params, navPolys, nvp, pp)
        createOffMeshConnectionVertices(
            params, nvp, offMeshConClass, offMeshPolyBase,
            navPolys, offMeshVerticesBase
        )

        // Store detail meshes and vertices.
        // The nav polygon vertices are stored as the first vertices on each mesh.
        // We compress the mesh data by skipping them and using the navmesh coordinates.
        if (pm != null) {
            storeDetailMeshesAndVertices(params, navDMeshes, navPolys, navDTris, navDVertices)
        } else {
            createDummyDetailMeshByTriangulatingPolys(params, navDMeshes, navPolys, navDTris)
        }

        // Store and create BVtree.
        // TODO: take detail mesh into account! use byte per bbox extent?
        if (params.buildBvTree) {
            // Do not set header.bvBVNodeCount set to make it work look exactly the same as in original Detour
            header.bvNodeCount = createBVTree(params, navBVTree)
        }

        storeOffMeshConnections(params, offMeshConClass, offMeshCons, offMeshPolyBase)
        header.vertices = navVertices
        header.polygons = navPolys
        header.detailMeshes = navDMeshes
        header.detailVertices = navDVertices
        header.detailTriangles = navDTris
        header.bvTree = navBVTree
        header.offMeshCons = offMeshCons
        return header
    }

    private fun storeHeader(
        header: MeshHeader, params: NavMeshDataCreateParams,
        totPolyCount: Int, totVertCount: Int, maxLinkCount: Int,
        uniqueDetailVertCount: Int, detailTriCount: Int,
        storedOffMeshConCount: Int, bvTreeSize: Int
    ) {
        header.magic = MeshHeader.DT_NAVMESH_MAGIC
        header.version = MeshHeader.DT_NAVMESH_VERSION
        header.x = params.tileX
        header.y = params.tileZ
        header.layer = params.tileLayer
        header.userId = params.userId
        header.polyCount = totPolyCount
        header.vertCount = totVertCount
        header.maxLinkCount = maxLinkCount
        header.bmin.set(params.bmin)
        header.bmax.set(params.bmax)
        header.detailMeshCount = params.polyCount
        header.detailVertCount = uniqueDetailVertCount
        header.detailTriCount = detailTriCount
        header.bvQuantizationFactor = 1f / params.cellSize
        header.offMeshBase = params.polyCount
        header.walkableHeight = params.walkableHeight
        header.walkableRadius = params.walkableRadius
        header.walkableClimb = params.walkableClimb
        header.offMeshConCount = storedOffMeshConCount
        header.bvNodeCount = bvTreeSize
    }

    private fun countUniqueDetailVertexCount(
        params: NavMeshDataCreateParams, nvp: Int,
        pm: IntArray, pp: IntArray
    ): Int {
        var uniqueDetailVertCount = 0
        for (i in 0 until params.polyCount) {
            val p = i * nvp * 2
            var ndv = pm[i * 4 + 1]
            var nv = 0
            for (j in 0 until nvp) {
                if (pp[p + j] == MESH_NULL_IDX) break
                nv++
            }
            ndv -= nv
            uniqueDetailVertCount += ndv
        }
        return uniqueDetailVertCount
    }

    private fun countDetailTriCount(params: NavMeshDataCreateParams, nvp: Int, pp: IntArray): Int {
        var detailTriCount = 0
        for (i in 0 until params.polyCount) {
            val p = i * nvp * 2
            var nv = 0
            for (j in 0 until nvp) {
                if (pp[p + j] == MESH_NULL_IDX) break
                nv++
            }
            detailTriCount += nv - 2
        }
        return detailTriCount
    }

    private fun findTightHeightBounds(
        params: NavMeshDataCreateParams, offMeshConClass: IntArray,
        offMeshConLinkCount0: Int, storedOffMeshConCount0: Int
    ): IntPair {
        var hmin = Float.MAX_VALUE
        var hmax = -Float.MAX_VALUE
        val dv = params.detailVertices
        if (dv != null && params.detailVerticesCount != 0) {
            for (i in 0 until params.detailVerticesCount) {
                val h = dv[i * 3 + 1]
                hmin = min(hmin, h)
                hmax = max(hmax, h)
            }
        } else {
            val pv = params.vertices!!
            for (i in 0 until params.vertCount) {
                val iv = i * 3
                val h = params.bmin.y + pv[iv + 1] * params.cellHeight
                hmin = min(hmin, h)
                hmax = max(hmax, h)
            }
        }
        hmin -= params.walkableClimb
        hmax += params.walkableClimb
        val bmin = Vector3f(params.bmin)
        val bmax = Vector3f(params.bmax)
        bmin.y = hmin
        bmax.y = hmax

        var offMeshConLinkCount = offMeshConLinkCount0
        var storedOffMeshConCount = storedOffMeshConCount0
        for (i in 0 until params.offMeshConCount) {
            val p0 = VectorPtr(params.offMeshConVertices, i * 2 * 3)
            val p1 = VectorPtr(params.offMeshConVertices, (i * 2 + 1) * 3)
            offMeshConClass[i * 2] = classifyOffMeshPoint(p0, bmin, bmax)
            offMeshConClass[i * 2 + 1] = classifyOffMeshPoint(p1, bmin, bmax)

            // Zero out off-mesh start positions which are not even
            // potentially touching the mesh.
            if (offMeshConClass[i * 2] == 0xff) {
                if (p0[1] < bmin.y || p0[1] > bmax.y) offMeshConClass[i * 2] = 0
            }

            // Count how many links should be allocated for off-mesh connections.
            if (offMeshConClass[i * 2] == 0xff) offMeshConLinkCount++
            if (offMeshConClass[i * 2 + 1] == 0xff) offMeshConLinkCount++
            if (offMeshConClass[i * 2] == 0xff) storedOffMeshConCount++
        }

        return IntPair(offMeshConLinkCount, storedOffMeshConCount)
    }

    private fun findPortalEdgesAtTileBorders(
        params: NavMeshDataCreateParams,
        pp: IntArray,
        nvp: Int,
        offMeshConLinkCount: Int
    ): Int {
        var edgeCount = 0
        var portalCount = 0
        for (i in 0 until params.polyCount) {
            val p = i * 2 * nvp
            for (j in 0 until nvp) {
                if (pp[p + j] == MESH_NULL_IDX) break
                edgeCount++
                if (pp[p + nvp + j] and 0x8000 != 0) {
                    val dir = pp[p + nvp + j] and 0xf
                    if (dir != 0xf) portalCount++
                }
            }
        }
        val maxLinkCount = edgeCount + portalCount * 2 + offMeshConLinkCount * 2
        return maxLinkCount
    }

    private fun storeMeshVertices(params: NavMeshDataCreateParams, navVertices: FloatArray) {
        val pv = params.vertices ?: return
        for (i in 0 until params.vertCount) {
            val iv = i * 3
            val v = i * 3
            navVertices[v] = params.bmin.x + pv[iv] * params.cellSize
            navVertices[v + 1] = params.bmin.y + pv[iv + 1] * params.cellHeight
            navVertices[v + 2] = params.bmin.z + pv[iv + 2] * params.cellSize
        }
    }

    private fun createOffMeshLinkVertices(
        params: NavMeshDataCreateParams,
        offMeshConClass: IntArray?, offMeshVerticesBase: Int,
        navVertices: FloatArray
    ) {
        var n = 0
        for (i in 0 until params.offMeshConCount) {
            // Only store connections which start from this tile.
            if (offMeshConClass!![i * 2] == 0xff) {
                val linkv = i * 2 * 3
                val v = (offMeshVerticesBase + n * 2) * 3
                System.arraycopy(params.offMeshConVertices, linkv, navVertices, v, 6)
                n++
            }
        }
    }

    private fun storeMeshPolygons(
        params: NavMeshDataCreateParams,
        navPolys: Array<Poly>, nvp: Int, pp: IntArray
    ) {
        var src = 0
        for (i in 0 until params.polyCount) {
            val p = navPolys[i]
            p.vertCount = 0
            p.flags = params.polyFlags[i]
            p.area = params.polyAreas[i]
            p.type = Poly.DT_POLYTYPE_GROUND
            for (j in 0 until nvp) {
                if (pp[src + j] == MESH_NULL_IDX) break
                p.vertices[j] = pp[src + j]
                if (pp[src + nvp + j] and 0x8000 != 0) {
                    // Border or portal edge.
                    when (pp[src + nvp + j] and 0xf) {
                        0 -> p.neighborData[j] = NavMesh.DT_EXT_LINK or 4 // Portal x-
                        1 -> p.neighborData[j] = NavMesh.DT_EXT_LINK or 2 // Portal z+
                        2 -> p.neighborData[j] = NavMesh.DT_EXT_LINK // Portal x+
                        3 -> p.neighborData[j] = NavMesh.DT_EXT_LINK or 6 // Portal z-
                        0xf -> p.neighborData[j] = 0 // Border
                    }
                } else {
                    // Normal connection
                    p.neighborData[j] = pp[src + nvp + j] + 1
                }
                p.vertCount++
            }
            src += nvp * 2
        }
    }

    private fun createOffMeshConnectionVertices(
        params: NavMeshDataCreateParams, nvp: Int,
        offMeshConClass: IntArray?, offMeshPolyBase: Int,
        navPolys: Array<Poly>, offMeshVerticesBase: Int
    ) {
        var n = 0
        offMeshConClass ?: return
        for (i in 0 until params.offMeshConCount) {
            // Only store connections which start from this tile.
            if (offMeshConClass[i * 2] == 0xff) {
                val p = Poly(offMeshPolyBase + n, nvp)
                navPolys[offMeshPolyBase + n] = p
                p.vertCount = 2
                p.vertices[0] = offMeshVerticesBase + n * 2
                p.vertices[1] = offMeshVerticesBase + n * 2 + 1
                p.flags = params.offMeshConFlags[i]
                p.area = params.offMeshConAreas[i]
                p.type = Poly.DT_POLYTYPE_OFFMESH_CONNECTION
                n++
            }
        }
    }

    private fun storeDetailMeshesAndVertices(
        params: NavMeshDataCreateParams, navDMeshes: Array<PolyDetail>,
        navPolys: Array<Poly>, navDTris: ByteArray, navDVertices: FloatArray
    ) {
        var vbase = 0
        val dm = params.detailMeshes ?: return
        val detailVertices = params.detailVertices ?: return
        for (i in 0 until params.polyCount) {
            val dtl = navDMeshes[i]
            val vb = dm[i * 4]
            val ndv = dm[i * 4 + 1]
            val nv = navPolys[i].vertCount
            dtl.vertBase = vbase
            dtl.vertCount = ndv - nv
            dtl.triBase = dm[i * 4 + 2]
            dtl.triCount = dm[i * 4 + 3]
            // Copy vertices except the first 'nv' vertices, which are equal to nav poly vertices.
            if (ndv - nv != 0) {
                detailVertices.copyInto(navDVertices, vbase * 3, (vb + nv) * 3, 3 * (vb + ndv))
                vbase += ndv - nv
            }
        }
        // Store triangles.
        val detailTris = params.detailTris ?: return
        detailTris.copyInto(navDTris, 0, 0, 4 * params.detailTriCount)
    }

    private fun createDummyDetailMeshByTriangulatingPolys(
        params: NavMeshDataCreateParams,
        navDMeshes: Array<PolyDetail>, navPolys: Array<Poly>,
        navDTris: ByteArray
    ) {
        var tbase = 0
        for (i in 0 until params.polyCount) {
            val dtl = navDMeshes[i]
            val nv = navPolys[i].vertCount
            dtl.vertBase = 0
            dtl.vertCount = 0
            dtl.triBase = tbase
            dtl.triCount = nv - 2
            tbase = triangulatePolygonWithLocalIndices(nv, tbase, navDTris)
        }
    }

    private fun triangulatePolygonWithLocalIndices(nv: Int, tbase0: Int, navDTris: ByteArray): Int {
        var tbase = tbase0
        // Triangulate polygon (local indices).
        for (j in 2 until nv) {
            val t = tbase * 4
            navDTris[t] = 0
            navDTris[t + 1] = (j - 1).toByte()
            navDTris[t + 2] = j.toByte()
            // Bit for each edge that belongs to poly boundary.
            navDTris[t + 3] = (1 shl 2).toByte()
            if (j == 2) navDTris[t + 3] = (navDTris[t + 3].toInt() or 1).toByte()
            if (j == nv - 1) navDTris[t + 3] = (navDTris[t + 3].toInt() or (1 shl 4)).toByte()
            tbase++
        }
        return tbase
    }

    private fun storeOffMeshConnections(
        params: NavMeshDataCreateParams,
        offMeshConClass: IntArray?,
        offMeshCons: Array<OffMeshConnection>,
        offMeshPolyBase: Int
    ) {
        offMeshConClass ?: return
        var n = 0
        for (i in 0 until params.offMeshConCount) {
            // Only store connections, which start from this tile.
            if (offMeshConClass[i * 2] == 0xff) {
                val con = offMeshCons[n]
                con.poly = offMeshPolyBase + n
                // Copy connection end-points.
                val endPts = i * 2 * 3
                con.posA.set(params.offMeshConVertices, endPts)
                con.posB.set(params.offMeshConVertices, endPts + 3)
                con.rad = params.offMeshConRad[i]
                con.flags = if (params.offMeshConDir[i] != 0) NavMesh.DT_OFFMESH_CON_BIDIR else 0
                con.side = offMeshConClass[i * 2 + 1]
                if (params.offMeshConUserID.isNotEmpty()) con.userId = params.offMeshConUserID[i]
                n++
            }
        }
    }

    private object CompareItemX : Comparator<BVNode> {
        override fun compare(a: BVNode, b: BVNode): Int {
            return a.minX.compareTo(b.minX)
        }
    }

    private object CompareItemY : Comparator<BVNode> {
        override fun compare(a: BVNode, b: BVNode): Int {
            return a.minY.compareTo(b.minY)
        }
    }

    private object CompareItemZ : Comparator<BVNode> {
        override fun compare(a: BVNode, b: BVNode): Int {
            return a.minZ.compareTo(b.minZ)
        }
    }
}