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
import org.joml.Vector3i
import org.recast4j.LongArrayList
import org.recast4j.ReStack
import org.recast4j.Vectors
import java.util.*
import kotlin.math.*

class NavMesh(
    /**
     * Current initialization params.
     */
    val params: NavMeshParams,
    /**
     * The maximum number of vertices per navigation polygon.
     */
    val maxVerticesPerPoly: Int
) {

    /**
     * Origin of the tile (0,0)
     */
    private val origin = params.origin

    /**
     * Dimensions of each tile.
     */
    var tileWidth = params.tileWidth
    var tileHeight = params.tileHeight

    private val tileByXY = HashMap<Long, MeshTile>()
    private val tileById = HashMap<Int, MeshTile>()
    private val availableTileIds = HashMap<Int, Int>() // id,salt
    private var nextTileId = 1

    /**
     * Returns tile in the tile array.
     */
    fun getTile(i: Int): MeshTile? {
        return tileById[i]
    }

    val allTiles get() = tileById.values
    val numTiles get() = tileById.size

    /**
     * Gets the polygon reference for the tile's base polygon.
     *
     * @param tile The tile.
     * @return The polygon reference for the base polygon in the specified tile.
     */
    fun getPolyRefBase(tile: MeshTile?): Long {
        if (tile == null) {
            return 0
        }
        val it = tile.id
        return encodePolyId(tile.salt, it, 0)
    }

    private fun allocLink(tile: MeshTile?): Int {
        if (tile!!.linksFreeList == DT_NULL_LINK) {
            val link = Link()
            link.indexOfNextLink = DT_NULL_LINK
            tile.links.add(link)
            return tile.links.size - 1
        }
        val link = tile.linksFreeList
        tile.linksFreeList = tile.links[link].indexOfNextLink
        return link
    }

    private fun freeLink(tile: MeshTile, link: Int) {
        tile.links[link].indexOfNextLink = tile.linksFreeList
        tile.linksFreeList = link
    }

    /**
     * Calculates the tile grid location for the specified world position.
     *
     * @param pos The world position for the query. [(x, y, z)]
     * @return 2-element int array with (tx,ty) tile location
     */
    fun calcTileLoc(pos: Vector3f): IntArray {
        val tx = floor(((pos.x - origin.x) / tileWidth)).toInt()
        val ty = floor(((pos.z - origin.z) / tileHeight)).toInt()
        return intArrayOf(tx, ty)
    }

    fun calcTileLocX(pos: Vector3f): Int {
        return floor(((pos.x - origin.x) / tileWidth)).toInt()
    }

    fun calcTileLocY(pos: Vector3f): Int {
        return floor(((pos.z - origin.z) / tileHeight)).toInt()
    }

    fun getPolyByRef(ref: Long, tile: MeshTile?): Poly? {
        if (tile == null) return null
        val ip = decodePolyIdPoly(ref)
        val data = tile.data
        return if (ip < data.polyCount) data.polygons[ip]
        else null
    }

    /**
     * @warning Only use this function if it is known that the provided polygon reference is valid.
     * This function is faster than getTileAndPolyByRef, but it does not validate the reference.
     */
    fun getTileByRefUnsafe(ref: Long): MeshTile {
        return tileById[decodePolyIdTile(ref)]!!
    }

    /**
     * @warning Only use this function if it is known that the provided polygon reference is valid.
     * This function is faster than getTileAndPolyByRef, but it does not validate the reference.
     */
    fun getPolyByRefUnsafe(ref: Long, tile: MeshTile): Poly {
        return tile.data.polygons[decodePolyIdPoly(ref)]
    }

    fun isValidPolyRef(ref: Long): Boolean {
        if (ref == 0L) return false
        val it = decodePolyIdTile(ref)
        val tile = tileById[it] ?: return false
        val salt = decodePolyIdSalt(ref)
        val ip = decodePolyIdPoly(ref)
        return if (tile.salt != salt) false
        else ip < tile.data.polyCount
    }

    constructor(data: MeshData, maxVerticesPerPoly: Int, flags: Int) : this(
        getNavMeshParams(data),
        maxVerticesPerPoly
    ) {
        addTile(data, flags, 0)
    }

    fun queryPolygonsInTile(tile: MeshTile, qmin: Vector3f, qmax: Vector3f): LongArrayList {
        val polys = LongArrayList()
        if (tile.data.bvTree != null) {
            var nodeIndex = 0
            val header = tile.data
            val tbmin = header.bmin
            val tbmax = header.bmax
            val qfac = header.bvQuantizationFactor
            // Calculate quantized box
            val bmin = Vector3i()
            val bmax = Vector3i()
            // dtClamp query box to world box.
            val minx = Vectors.clamp(qmin.x, tbmin.x, tbmax.x) - tbmin.x
            val miny = Vectors.clamp(qmin.y, tbmin.y, tbmax.y) - tbmin.y
            val minz = Vectors.clamp(qmin.z, tbmin.z, tbmax.z) - tbmin.z
            val maxx = Vectors.clamp(qmax.x, tbmin.x, tbmax.x) - tbmin.x
            val maxy = Vectors.clamp(qmax.y, tbmin.y, tbmax.y) - tbmin.y
            val maxz = Vectors.clamp(qmax.z, tbmin.z, tbmax.z) - tbmin.z
            // Quantize
            bmin.x = (qfac * minx).toInt() and 0x7ffffffe
            bmin.y = (qfac * miny).toInt() and 0x7ffffffe
            bmin.z = (qfac * minz).toInt() and 0x7ffffffe
            bmax.x = (qfac * maxx + 1).toInt() or 1
            bmax.y = (qfac * maxy + 1).toInt() or 1
            bmax.z = (qfac * maxz + 1).toInt() or 1

            // Traverse tree
            val base = getPolyRefBase(tile)
            val end = header.bvNodeCount
            while (nodeIndex < end) {
                val node = tile.data.bvTree!![nodeIndex]
                val overlap: Boolean = Vectors.overlapQuantBounds(bmin, bmax, node)
                val isLeafNode = node.index >= 0
                if (isLeafNode && overlap) {
                    polys.add(base or node.index.toLong())
                }
                if (overlap || isLeafNode) {
                    nodeIndex++
                } else {
                    val escapeIndex = -node.index
                    nodeIndex += escapeIndex
                }
            }
        } else {
            val bmin = ReStack.vec3fs.create()
            val bmax = ReStack.vec3fs.create()
            val base = getPolyRefBase(tile)
            val data = tile.data
            val vertices = data.vertices
            for (i in 0 until data.polyCount) {
                val p = data.polygons[i]
                // Do not return off-mesh connection polygons.
                if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                    continue
                }
                // Calc polygon bounds.
                var v = p.vertices[0] * 3
                bmin.set(vertices, v)
                bmax.set(bmin)
                for (j in 1 until p.vertCount) {
                    v = p.vertices[j] * 3
                    Vectors.min(bmin, vertices, v)
                    Vectors.max(bmax, vertices, v)
                }
                if (Vectors.overlapBounds(qmin, qmax, bmin, bmax)) {
                    polys.add(base or i.toLong())
                }
            }
            ReStack.vec3fs.sub(2)
        }
        return polys
    }

    fun updateTile(data: MeshData, flags: Int): Long {
        val header = data
        var ref = getTileRefAt(header.x, header.y, header.layer)
        ref = removeTile(ref)
        return addTile(data, flags, ref)
    }

    /**
     * Adds a tile to the navigation mesh.
     *
     * The add operation will fail if the data is in the wrong format, the allocated tile space is full, or there is a tile already at the specified reference.
     *
     * The lastRef parameter is used to restore a tile with the same tile
     * reference it had previously used. In this case the #long's for the
     * tile will be restored to the same values they were before the tile was removed.
     *
     * The nav mesh assumes exclusive access to the data passed and will make
     * changes to the dynamic portion of the data. For that reason the data
     * should not be reused in other nav meshes until the tile has been successfully
     * removed from this nav mesh.
     *
     * @param data Data for the new tile mesh. (See: #dtCreateNavMeshData)
     * @param flags Tile flags. (See: #dtTileFlags)
     * @param lastRef The desired reference for the tile. (When reloading a tile.) [opt] [Default: 0]
     * @return The tile reference. (If the tile was successfully added.) [opt]
     */
    fun addTile(data: MeshData, flags: Int, lastRef: Long): Long {
        // Make sure the data is in right format.
        val header = data

        // Make sure the location is free.
        if (getTileAt(header.x, header.y, header.layer) != null) {
            throw RuntimeException("Tile already exists")
        }

        // Allocate a tile.
        val tile: MeshTile?
        if (lastRef == 0L) {
            // Make sure we could allocate a tile.
            val tileId = availableTileIds.keys.firstOrNull()
            if (tileId != null) {
                tile = MeshTile(tileId)
                tile.salt = availableTileIds.remove(tileId)!! + 1 // new salt, because its different
            } else {
                tile = MeshTile(nextTileId++)
            }
        } else {
            // Try to relocate the tile to specific index with same salt.
            val tileId = decodePolyIdTile(lastRef)
            // Try to find the specific tile id from the free list & remove from freelist
            if (availableTileIds.remove(tileId) == null) {
                // Could not find the correct location.
                throw RuntimeException("Could not find tile")
            }
            tile = MeshTile(tileId)
            // Restore salt.
            tile.salt = decodePolyIdSalt(lastRef)
        }

        tileById[tile.id] = tile // unique

        tile.data = data
        tile.flags = flags
        tile.links.clear()
        tile.polyLinks = IntArray(data.polygons.size)
        tile.polyLinks.fill(DT_NULL_LINK)

        // Insert tile into the position lut.
        val hash = computeTileHash(header.x, header.y)
        tile.next = tileByXY.put(hash, tile)

        // Patch header pointers.

        // If there are no items in the bvtree, reset the tree pointer.
        if (data.bvTree != null && data.bvTree!!.isEmpty()) {
            data.bvTree = null
        }

        // Init tile.
        connectIntLinks(tile)
        // Base off-mesh connections to their starting polygons and connect connections inside the tile.
        baseOffMeshLinks(tile)
        connectExtOffMeshLinks(tile, tile, -1)

        // Connect with layers in current tile.
        var neis = getTilesAt(header.x, header.y)
        while (neis != null) {
            if (neis !== tile) {
                connectExtLinks(tile, neis, -1)
                connectExtLinks(neis, tile, -1)
                connectExtOffMeshLinks(tile, neis, -1)
                connectExtOffMeshLinks(neis, tile, -1)
            }
            neis = neis.next
        }

        // Connect with neighbour tiles.
        for (i in 0..7) {
            neis = getNeighbourTilesAt(header.x, header.y, i)
            while (neis != null) {
                connectExtLinks(tile, neis, i)
                connectExtLinks(neis, tile, Vectors.oppositeTile(i))
                connectExtOffMeshLinks(tile, neis, i)
                connectExtOffMeshLinks(neis, tile, Vectors.oppositeTile(i))
                neis = neis.next
            }
        }
        return getTileRef(tile)
    }

    private fun removeTileFromChain(h: Long, tile: MeshTile) {
        var list = tileByXY[h]
        if (list === tile) {
            val next = list.next
            if (next != null) tileByXY[h] = next
            else tileByXY.remove(h)
            return
        }
        while (list != null) {
            val next = list.next
            if (next === tile) {
                // found it, remove it
                list.next = tile.next
                return
            }
            list = next
        }
    }

    /**
     * Removes the specified tile from the navigation mesh.
     *
     * This function returns the data for the tile so that, if desired, it can be added back to the navigation mesh at a later point.
     * */
    fun removeTile(ref: Long): Long {
        if (ref == 0L) {
            return 0
        }
        val tileIndex = decodePolyIdTile(ref)
        val tileSalt = decodePolyIdSalt(ref)
        val tile = tileById[tileIndex]
            ?: throw RuntimeException("Invalid tile index")
        if (tile.salt != tileSalt) {
            throw RuntimeException("Invalid tile salt")
        }

        // Remove tile from hash lookup.
        val header = tile.data
        removeTileFromChain(computeTileHash(header.x, header.y), tile)

        // Remove connections to neighbour tiles.
        // Create connections with neighbour tiles.

        // Disconnect from other layers in current tile.
        var nneis = getTilesAt(header.x, header.y)
        while (nneis != null) {
            if (nneis !== tile) {
                unconnectLinks(nneis, tile)
            }
            nneis = nneis.next
        }

        // Disconnect from neighbour tiles.
        for (i in 0..7) {
            nneis = getNeighbourTilesAt(header.x, header.y, i)
            while (nneis !== null) {
                unconnectLinks(nneis, tile)
                nneis = nneis.next
            }
        }

        // Reset tile.
        tile.data = MeshData.empty
        tile.flags = 0
        tile.links.clear()
        tile.linksFreeList = DT_NULL_LINK

        // Update salt, salt should never be zero.
        tile.salt = tile.salt + 1 and (1 shl DT_SALT_BITS) - 1
        if (tile.salt == 0) {
            tile.salt++
        }

        availableTileIds[tile.id] = tile.salt
        tileById.remove(tile.id)

        return getTileRef(tile)
    }

    /**
     * Builds internal polygons links for a tile.
     * */
    fun connectIntLinks(tile: MeshTile?) {
        if (tile == null) return
        val data = tile.data
        val base = getPolyRefBase(tile)
        for (i in 0 until data.polyCount) {
            val poly = data.polygons[i]
            tile.polyLinks[poly.index] = DT_NULL_LINK
            if (poly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                continue
            }

            // Build edge links backwards so that the links will be
            // in the linked list from lowest index to highest.
            for (j in poly.vertCount - 1 downTo 0) {
                // Skip hard and non-internal edges.
                if (poly.neighborData[j] == 0 || poly.neighborData[j] and DT_EXT_LINK != 0) {
                    continue
                }
                val idx = allocLink(tile)
                val link = tile.links[idx]
                link.neighborRef = base or (poly.neighborData[j] - 1).toLong()
                link.indexOfPolyEdge = j
                link.side = 0xff
                link.bmax = 0
                link.bmin = 0
                // Add to linked list.
                link.indexOfNextLink = tile.polyLinks[poly.index]
                tile.polyLinks[poly.index] = idx
            }
        }
    }

    fun unconnectLinks(tile: MeshTile?, target: MeshTile?) {
        if (tile == null || target == null) {
            return
        }
        val data = tile.data
        val targetNum = decodePolyIdTile(getTileRef(target))
        for (i in 0 until data.polyCount) {
            val poly = data.polygons[i]
            var j = tile.polyLinks[poly.index]
            var pj = DT_NULL_LINK
            while (j != DT_NULL_LINK) {
                if (decodePolyIdTile(tile.links[j].neighborRef) == targetNum) {
                    // Remove link.
                    val nj = tile.links[j].indexOfNextLink
                    if (pj == DT_NULL_LINK) {
                        tile.polyLinks[poly.index] = nj
                    } else {
                        tile.links[pj].indexOfNextLink = nj
                    }
                    freeLink(tile, j)
                    j = nj
                } else {
                    // Advance
                    pj = j
                    j = tile.links[j].indexOfNextLink
                }
            }
        }
    }

    fun connectExtLinks(tile: MeshTile?, target: MeshTile?, side: Int) {
        if (tile == null) {
            return
        }

        // Connect border links.
        val data = tile.data
        for (i in 0 until data.polyCount) {
            val poly = data.polygons[i]

            // Create new links.
            // short m = DT_EXT_LINK | (short)side;
            val nv = poly.vertCount
            for (j in 0 until nv) {
                // Skip non-portal edges.
                if (poly.neighborData[j] and DT_EXT_LINK == 0) {
                    continue
                }
                val dir = poly.neighborData[j] and 0xff
                if (side != -1 && dir != side) {
                    continue
                }

                // Create new links
                val va = poly.vertices[j] * 3
                val vb = poly.vertices[(j + 1) % nv] * 3
                val (nei, neia, nnei) = findConnectingPolys(
                    data.vertices, va, vb, target,
                    Vectors.oppositeTile(dir), 4
                )
                for (k in 0 until nnei) {
                    val idx = allocLink(tile)
                    val link = tile.links[idx]
                    link.neighborRef = nei!![k]
                    link.indexOfPolyEdge = j
                    link.side = dir
                    link.indexOfNextLink = tile.polyLinks[poly.index]
                    tile.polyLinks[poly.index] = idx

                    // Compress portal limits to a byte value.
                    if (dir == 0 || dir == 4) {
                        var tmin = ((neia!![k * 2] - data.vertices[va + 2])
                                / (data.vertices[vb + 2] - data.vertices[va + 2]))
                        var tmax = ((neia[k * 2 + 1] - data.vertices[va + 2])
                                / (data.vertices[vb + 2] - data.vertices[va + 2]))
                        if (tmin > tmax) {
                            val temp = tmin
                            tmin = tmax
                            tmax = temp
                        }
                        link.bmin = round(Vectors.clamp(tmin, 0f, 1f) * 255f).toInt()
                        link.bmax = round(Vectors.clamp(tmax, 0f, 1f) * 255f).toInt()
                    } else if (dir == 2 || dir == 6) {
                        var tmin = ((neia!![k * 2] - data.vertices[va])
                                / (data.vertices[vb] - data.vertices[va]))
                        var tmax = ((neia[k * 2 + 1] - data.vertices[va])
                                / (data.vertices[vb] - data.vertices[va]))
                        if (tmin > tmax) {
                            val temp = tmin
                            tmin = tmax
                            tmax = temp
                        }
                        link.bmin = round(Vectors.clamp(tmin, 0f, 1f) * 255f).toInt()
                        link.bmax = round(Vectors.clamp(tmax, 0f, 1f) * 255f).toInt()
                    }
                }
            }
        }
    }

    fun connectExtOffMeshLinks(tile: MeshTile?, target: MeshTile?, side: Int) {
        if (tile == null || target == null) {
            return
        }

        // Connect off-mesh links.
        // We are interested on links which land from target tile to this tile.
        val oppositeSide = if (side == -1) 0xff else Vectors.oppositeTile(side)
        val data2 = target.data
        for (i in 0 until data2.offMeshConCount) {
            val targetCon = data2.offMeshCons[i]
            if (targetCon.side != oppositeSide) {
                continue
            }
            val targetPoly = data2.polygons[targetCon.poly]
            // Skip off-mesh connections which start location could not be
            // connected at all.
            if (target.polyLinks[targetPoly.index] == DT_NULL_LINK) {
                continue
            }
            val ext = Vector3f(targetCon.rad, data2.walkableClimb, targetCon.rad)

            // Find polygon to connect to.
            val p = Vector3f(targetCon.posB)
            val nearest = findNearestPolyInTile(tile, p, ext)
            val ref = nearest.nearestRef
            if (ref == 0L) continue
            val nearestPt = nearest.nearestPos ?: continue
            // findNearestPoly may return too optimistic results, further check
            // to make sure.
            if (Vectors.sq(nearestPt.x - p.x) + Vectors.sq(nearestPt.z - p.z) > targetCon.rad * targetCon.rad) {
                continue
            }
            // Make sure the location is on current mesh.
            data2.vertices[targetPoly.vertices[1] * 3] = nearestPt.x
            data2.vertices[targetPoly.vertices[1] * 3 + 1] = nearestPt.y
            data2.vertices[targetPoly.vertices[1] * 3 + 2] = nearestPt.z

            // Link off-mesh connection to target poly.
            val idx = allocLink(target)
            var link = target.links[idx]
            link.neighborRef = ref
            link.indexOfPolyEdge = 1
            link.side = oppositeSide
            link.bmax = 0
            link.bmin = 0
            // Add to linked list.
            link.indexOfNextLink = target.polyLinks[targetPoly.index]
            target.polyLinks[targetPoly.index] = idx

            // Link target poly to off-mesh connection.
            if (targetCon.flags and DT_OFFMESH_CON_BIDIR != 0) {
                val tidx = allocLink(tile)
                val landPolyIdx = decodePolyIdPoly(ref)
                val landPoly = tile.data.polygons[landPolyIdx]
                link = tile.links[tidx]
                link.neighborRef = getPolyRefBase(target) or targetCon.poly.toLong()
                link.indexOfPolyEdge = 0xff
                link.side = if (side == -1) 0xff else side
                link.bmax = 0
                link.bmin = 0
                // Add to linked list.
                link.indexOfNextLink = tile.polyLinks[landPoly.index]
                tile.polyLinks[landPoly.index] = tidx
            }
        }
    }

    fun findConnectingPolys(
        vertices: FloatArray,
        va: Int,
        vb: Int,
        tile: MeshTile?,
        side: Int,
        maxcon: Int
    ): Triple<LongArray?, FloatArray?, Int> {
        if (tile == null) {
            return Triple<LongArray?, FloatArray?, Int>(null, null, 0)
        }
        val con = LongArray(maxcon)
        val conarea = FloatArray(maxcon * 2)
        val amin = FloatArray(2)
        val amax = FloatArray(2)
        calcSlabEndPoints(vertices, va, vb, amin, amax, side)
        val apos = getSlabCoord(vertices, va, side)

        // Remove links pointing to 'side' and compact the links array.
        val bmin = FloatArray(2)
        val bmax = FloatArray(2)
        val m = DT_EXT_LINK or side
        var n = 0
        val base = getPolyRefBase(tile)
        val data = tile.data
        for (i in 0 until data.polyCount) {
            val poly = data.polygons[i]
            val nv = poly.vertCount
            for (j in 0 until nv) {
                // Skip edges which do not point to the right side.
                if (poly.neighborData[j] != m) {
                    continue
                }
                val vc = poly.vertices[j] * 3
                val vd = poly.vertices[(j + 1) % nv] * 3
                val bpos = getSlabCoord(data.vertices, vc, side)
                // Segments are not close enough.
                if (abs(apos - bpos) > 0.01f) {
                    continue
                }

                // Check if the segments touch.
                calcSlabEndPoints(data.vertices, vc, vd, bmin, bmax, side)
                if (!overlapSlabs(amin, amax, bmin, bmax, 0.01f, data.walkableClimb)) {
                    continue
                }

                // Add return value.
                if (n < maxcon) {
                    conarea[n * 2] = max(amin[0], bmin[0])
                    conarea[n * 2 + 1] = min(amax[0], bmax[0])
                    con[n] = base or i.toLong()
                    n++
                }
                break
            }
        }
        return Triple(con, conarea, n)
    }

    fun overlapSlabs(
        amin: FloatArray,
        amax: FloatArray,
        bmin: FloatArray,
        bmax: FloatArray,
        px: Float,
        py: Float
    ): Boolean {
        // Check for horizontal overlap.
        // The segment is shrunken a little so that slabs, which touch
        // at end points are not connected.
        val minX = max(amin[0] + px, bmin[0] + px)
        val maxX = min(amax[0] - px, bmax[0] - px)
        if (minX > maxX) {
            return false
        }

        // Check vertical overlap.
        val ad = (amax[1] - amin[1]) / (amax[0] - amin[0])
        val ak = amin[1] - ad * amin[0]
        val bd = (bmax[1] - bmin[1]) / (bmax[0] - bmin[0])
        val bk = bmin[1] - bd * bmin[0]
        val aminy = ad * minX + ak
        val amaxy = ad * maxX + ak
        val bminy = bd * minX + bk
        val bmaxy = bd * maxX + bk
        val dmin = bminy - aminy
        val dmax = bmaxy - amaxy

        // Crossing segments always overlap.
        if (dmin * dmax < 0) return true

        // Check for overlap at endpoints.
        val thr = py * 2 * (py * 2)
        return dmin * dmin <= thr || dmax * dmax <= thr
    }

    /**
     * Builds internal polygons links for a tile.
     */
    fun baseOffMeshLinks(tile: MeshTile?) {
        if (tile == null) {
            return
        }
        val base = getPolyRefBase(tile)

        // Base off-mesh connection start points.
        val data = tile.data
        val header = data
        for (i in 0 until header.offMeshConCount) {
            val con = data.offMeshCons[i]
            val poly = data.polygons[con.poly]
            val ext = Vector3f(con.rad, header.walkableClimb, con.rad)

            // Find polygon to connect to.
            val nearestPoly = findNearestPolyInTile(tile, con.posA, ext)
            val ref = nearestPoly.nearestRef
            if (ref == 0L) continue
            val p = con.posA // First vertex
            val nearestPt = nearestPoly.nearestPos ?: continue
            // findNearestPoly may return too optimistic results, further check
            // to make sure.
            if (Vectors.sq(nearestPt.x - p.x) + Vectors.sq(nearestPt.z - p.z) > Vectors.sq(con.rad)) {
                continue
            }
            // Make sure the location is on current mesh.
            nearestPt.get(data.vertices, poly.vertices[0] * 3)

            // Link off-mesh connection to target poly.
            val idx = allocLink(tile)
            var link = tile.links[idx]
            link.neighborRef = ref
            link.indexOfPolyEdge = 0
            link.side = 0xff
            link.bmax = 0
            link.bmin = 0
            // Add to linked list.
            link.indexOfNextLink = tile.polyLinks[poly.index]
            tile.polyLinks[poly.index] = idx

            // Start end-point is always connect back to off-mesh connection.
            val tidx = allocLink(tile)
            val landPolyIdx = decodePolyIdPoly(ref)
            val landPoly = data.polygons[landPolyIdx]
            link = tile.links[tidx]
            link.neighborRef = base or con.poly.toLong()
            link.indexOfPolyEdge = 0xff
            link.side = 0xff
            link.bmax = 0
            link.bmin = 0
            // Add to linked list.
            link.indexOfNextLink = tile.polyLinks[landPoly.index]
            tile.polyLinks[landPoly.index] = tidx
        }
    }

    /**
     * Returns closest point on polygon.
     */
    fun closestPointOnDetailEdges(tile: MeshTile, poly: Poly, pos: Vector3f, onlyBoundary: Boolean): Vector3f {
        val flagAnyBoundaryEdge = DT_DETAIL_EDGE_BOUNDARY or
                (DT_DETAIL_EDGE_BOUNDARY shl 2) or
                (DT_DETAIL_EDGE_BOUNDARY shl 4)
        val ip = poly.index
        var dmin = Float.MAX_VALUE
        var tmin = 0f
        lateinit var pmin: Vector3f
        lateinit var pmax: Vector3f
        val tileData = tile.data
        if (tileData.detailMeshes != null) {
            val pd = tileData.detailMeshes!![ip]
            for (i in 0 until pd.triCount) {
                val ti = (pd.triBase + i) * 4
                val tris = tile.data.detailTriangles
                if (onlyBoundary && tris[ti + 3].toInt() and flagAnyBoundaryEdge == 0) {
                    continue
                }
                val v = tripletVec3f.get()
                fill(tileData, ti + 0, poly, v[0], pd)
                fill(tileData, ti + 1, poly, v[1], pd)
                fill(tileData, ti + 2, poly, v[2], pd)
                var k = 0
                var j = 2
                while (k < 3) {
                    if (getDetailTriEdgeFlags(tris[ti + 3].toInt().and(0xff), j) and DT_DETAIL_EDGE_BOUNDARY == 0
                        && (onlyBoundary || tris[ti + j].toInt().and(0xff) < tris[ti + k].toInt().and(0xff))
                    ) {
                        // Only looking at boundary edges and this is internal, or
                        // this is an inner edge, that we will see again or have already seen.
                        j = k++
                        continue
                    }
                    val (d, t) = Vectors.distancePtSegSqr2D(pos, v[j], v[k])
                    if (d < dmin) {
                        dmin = d
                        tmin = t
                        pmin = v[j]
                        pmax = v[k]
                    }
                    j = k++
                }
            }
        } else {
            if (poly.vertCount <= 0) return Vector3f() // invalid/empty
            val (v0, v1) = tripletVec3f.get()
            val vertices = tileData.vertices
            for (j in 0 until poly.vertCount) {
                val k = (j + 1) % poly.vertCount
                v0.set(vertices, poly.vertices[j] * 3)
                v1.set(vertices, poly.vertices[k] * 3)
                val (d, t) = Vectors.distancePtSegSqr2D(pos, v0, v1)
                if (d < dmin) {
                    dmin = d
                    tmin = t
                    pmin = v0
                    pmax = v0
                }
            }
        }
        return pmin.lerp(pmax, tmin, Vector3f())
    }

    /**
     * Returns closest point on polygon.
     */
    fun closestPointOnDetailEdgesY(tile: MeshTile, poly: Poly, pos: Vector3f, onlyBoundary: Boolean): Float {
        val flagAnyBoundaryEdge =
            DT_DETAIL_EDGE_BOUNDARY or (DT_DETAIL_EDGE_BOUNDARY shl 2) or (DT_DETAIL_EDGE_BOUNDARY shl 4)
        val ip = poly.index
        var dmin = Float.MAX_VALUE
        var y = 0f
        val tileData = tile.data
        if (tileData.detailMeshes != null) {
            val pd = tileData.detailMeshes!![ip]
            for (i in 0 until pd.triCount) {
                val ti = (pd.triBase + i) * 4
                val tris = tileData.detailTriangles
                if (onlyBoundary && tris[ti + 3].toInt() and flagAnyBoundaryEdge == 0) {
                    continue
                }
                val v = tripletVec3f.get()
                fill(tileData, ti + 0, poly, v[0], pd)
                fill(tileData, ti + 1, poly, v[1], pd)
                fill(tileData, ti + 2, poly, v[2], pd)
                var k = 0
                var j = 2
                while (k < 3) {
                    if (getDetailTriEdgeFlags(tris[ti + 3].toInt().and(0xff), j) and DT_DETAIL_EDGE_BOUNDARY == 0
                        && (onlyBoundary || tris[ti + j].toInt().and(0xff) < tris[ti + k].toInt().and(0xff))
                    ) {
                        // Only looking at boundary edges and this is internal, or
                        // this is an inner edge, that we will see again or have already seen.
                        j = k++
                        continue
                    }
                    val (d, t) = Vectors.distancePtSegSqr2D(pos, v[j], v[k])
                    if (d < dmin) {
                        dmin = d
                        y = v[j].y + (v[k].y - v[j].y) * t
                    }
                    j = k++
                }
            }
        } else {
            val (v0, v1) = tripletVec3f.get()
            val vs = tile.data.vertices
            val ix = poly.vertices
            for (j in 0 until poly.vertCount) {
                val k = (j + 1) % poly.vertCount
                v0.set(vs, ix[j] * 3)
                v1.set(vs, ix[k] * 3)
                val d = Vectors.distancePtSegSqr2DFirst(pos, v0, v1)
                if (d < dmin) {
                    dmin = d
                    y = v0.y
                }
            }
        }
        return y
    }

    private fun fill(tileData: MeshData, tk: Int, poly: Poly, vk: Vector3f, pd: PolyDetail) {
        val index: Int
        val data: FloatArray
        if (tileData.detailTriangles[tk].toInt().and(0xff) < poly.vertCount) {
            index = poly.vertices[tileData.detailTriangles[tk].toInt().and(0xff)]
            data = tileData.vertices
        } else {
            index = (pd.vertBase + (tileData.detailTriangles[tk].toInt().and(0xff) - poly.vertCount))
            data = tileData.detailVertices
        }
        vk.set(data, index * 3)
    }

    fun getPolyHeight(tile: MeshTile?, poly: Poly, pos: Vector3f): Float {
        // Off-mesh connections do not have detail polys and getting height
        // over them does not make sense.
        if (poly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) return Float.NaN
        if (tile == null || poly.vertCount <= 0) return Float.NaN
        val ip = poly.index
        val tileData = tile.data
        val vertices = tmpVertices.get()
        val nv = poly.vertCount
        for (i in 0 until nv) {
            val srcI = poly.vertices[i] * 3
            tileData.vertices.copyInto(vertices, i * 3, srcI, srcI + 3)
        }
        if (!Vectors.pointInPolygon(pos, vertices, nv)) {
            return Float.NaN
        }

        // Find height at the location.
        val (v0, v1, v2) = tripletVec3f.get()
        val detailMeshes = tileData.detailMeshes
        if (detailMeshes != null) {
            val pd = detailMeshes[ip]
            for (j in 0 until pd.triCount) {
                val t = (pd.triBase + j) * 4
                fill(tileData, t + 0, poly, v0, pd)
                fill(tileData, t + 1, poly, v1, pd)
                fill(tileData, t + 2, poly, v2, pd)
                val h = Vectors.closestHeightPointTriangle(pos, v0, v1, v2)
                if (h.isFinite()) return h
            }
        } else {
            val vs = tileData.vertices
            val ix = poly.vertices
            v0.set(vs, ix[0] * 3)
            for (j in 1 until poly.vertCount - 1) {
                v1.set(vs, ix[j + 1] * 3)
                v2.set(vs, ix[j + 2] * 3)
                val h = Vectors.closestHeightPointTriangle(pos, v0, v1, v2)
                if (h.isFinite()) return h
            }
        }

        // If all triangle checks failed above (can happen with degenerate triangles
        // or larger floating point values) the point is on an edge, so just select
        // closest. This should almost never happen, so the extra iteration here is ok.
        return closestPointOnDetailEdgesY(tile, poly, pos, false)
    }

    fun closestPointOnPoly(ref: Long, pos: Vector3f): ClosestPointOnPolyResult {
        val tile = getTileByRefUnsafe(ref)
        val poly = getPolyByRefUnsafe(ref, tile)
        val h = getPolyHeight(tile, poly, pos)
        if (h.isFinite()) {
            val closest = Vector3f(pos.x, h, pos.z)
            return ClosestPointOnPolyResult(true, closest)
        }

        // Off-mesh connections don't have detail polygons.
        val tileData = tile.data
        if (poly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
            val v = tileData.vertices
            val v0 = Vector3f().set(v, poly.vertices[0] * 3)
            val v1 = Vector3f().set(v, poly.vertices[1] * 3)
            val (_, second) = Vectors.distancePtSegSqr2D(pos, v0, v1)
            return ClosestPointOnPolyResult(false, v0.lerp(v1, second))
        }
        // Outside poly that is not an offmesh connection.
        return ClosestPointOnPolyResult(false, closestPointOnDetailEdges(tile, poly, pos, true))
    }

    fun findNearestPolyInTile(tile: MeshTile, center: Vector3f, extents: Vector3f): FindNearestPolyResult {

        val bmin = center.sub(extents, ReStack.vec3fs.create())
        val bmax = center.add(extents, ReStack.vec3fs.create())

        // Get nearby polygons from proximity grid.
        val polys = queryPolygonsInTile(tile, bmin, bmax)
        ReStack.vec3fs.sub(2)

        // Find the nearest polygon amongst the nearby polygons.
        var nearest = 0L
        var nearestDistanceSqr = Float.MAX_VALUE
        var nearestPt: Vector3f? = null
        var overPoly = false

        for (i in 0 until polys.size) {
            val ref = polys[i]
            val cpp = closestPointOnPoly(ref, center)
            val posOverPoly = cpp.isPosOverPoly
            val closestPtPoly = cpp.pos

            // If a point is directly over a polygon and closer than
            // climb height, favor that instead of straight line nearest point.
            val distSq = if (posOverPoly) {
                val d = abs(center.y - closestPtPoly.y) - tile.data.walkableClimb
                if (d > 0) d * d else 0f
            } else {
                center.distanceSquared(closestPtPoly)
            }
            if (distSq < nearestDistanceSqr) {
                nearestPt = closestPtPoly
                nearestDistanceSqr = distSq
                nearest = ref
                overPoly = posOverPoly
            }
        }
        return FindNearestPolyResult(nearest, nearestPt, overPoly)
    }

    fun getTileAt(x: Int, y: Int, layer: Int): MeshTile? {
        var tile = getTileListByPos(x, y)
        while (tile != null) {
            if (tile.data.layer == layer) return tile
            tile = tile.next
        }
        return null
    }

    fun getNeighbourTilesAt(x: Int, y: Int, side: Int): MeshTile? {
        var nx = x
        var ny = y
        when (side) {
            0 -> nx++
            1 -> {
                nx++
                ny++
            }
            2 -> ny++
            3 -> {
                nx--
                ny++
            }
            4 -> nx--
            5 -> {
                nx--
                ny--
            }
            6 -> ny--
            7 -> {
                nx++
                ny--
            }
        }
        return getTilesAt(nx, ny)
    }

    fun getTilesAt(x: Int, y: Int): MeshTile? {
        return getTileListByPos(x, y)
    }

    fun getTileRefAt(x: Int, y: Int, layer: Int): Long {
        return getTileRef(getTileAt(x, y, layer))
    }

    fun getTileByRef(ref: Long): MeshTile? {
        if (ref == 0L) return null
        val tileIndex = decodePolyIdTile(ref)
        val tileSalt = decodePolyIdSalt(ref)
        val tile = tileById[tileIndex] ?: return null
        return if (tile.salt == tileSalt) tile else null
    }

    fun getTileRef(tile: MeshTile?): Long {
        return if (tile == null) 0
        else encodePolyId(tile.salt, tile.id, 0)
    }

    /**
     * Off-mesh connections are stored in the navigation mesh as special 2-vertex polygons with a single edge.
     * At least one of the vertices is expected to be inside a normal polygon. So an off-mesh connection is "entered"
     * from a normal polygon at one of its endpoints. This is the polygon identified by the prevRef parameter.
     */
    fun getOffMeshConnectionPolyEndPoints(prevRef: Long, polyRef: Long): Pair<Vector3f, Vector3f>? {
        if (polyRef == 0L) return null

        // Get current polygon
        val tile = getTileByRef(polyRef) ?: return null
        val poly = getPolyByRef(polyRef, tile) ?: return null

        // Make sure that the current poly is indeed off-mesh link.
        if (poly.type != Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
            return null
        }

        // Figure out which way to hand out the vertices.
        var idx0 = 0
        var idx1 = 1

        // Find link that points to first vertex.
        var i = tile.polyLinks[poly.index]
        while (i != DT_NULL_LINK) {
            if (tile.links[i].indexOfPolyEdge == 0) {
                if (tile.links[i].neighborRef != prevRef) {
                    idx0 = 1
                    idx1 = 0
                }
                break
            }
            i = tile.links[i].indexOfNextLink
        }
        val startPos = Vector3f()
        val endPos = Vector3f()
        val data = tile.data
        startPos.set(data.vertices, poly.vertices[idx0] * 3)
        endPos.set(data.vertices, poly.vertices[idx1] * 3)
        return Pair(startPos, endPos)
    }

    fun setPolyFlags(ref: Long, flags: Int): Status {
        if (ref == 0L) {
            return Status.FAILURE
        }
        val tileId = decodePolyIdTile(ref)
        val tile = tileById[tileId]
        val data = tile?.data
        val salt = decodePolyIdSalt(ref)
        if (data == null || tile.salt != salt) {
            return Status.FAILURE_INVALID_PARAM
        }
        val ip = decodePolyIdPoly(ref)
        if (ip >= data.polyCount) {
            return Status.FAILURE_INVALID_PARAM
        }
        val poly = data.polygons[ip]

        // Change flags.
        poly.flags = flags
        return Status.SUCCESS
    }

    fun getPolyFlags(ref: Long): Result<Int> {
        if (ref == 0L) {
            return Result.failure()
        }
        val tileId = decodePolyIdTile(ref)
        val salt = decodePolyIdSalt(ref)
        val tile = tileById[tileId]
        if (tile == null || tile.salt != salt) {
            return Result.invalidParam()
        }
        val data = tile.data
        val polyId = decodePolyIdPoly(ref)
        if (polyId >= data.polyCount) {
            return Result.invalidParam()
        }
        val poly = data.polygons[polyId]
        return Result.success(poly.flags)
    }

    fun setPolyArea(ref: Long, area: Char): Status {
        if (ref == 0L) {
            return Status.FAILURE
        }
        val tileId = decodePolyIdTile(ref)
        val salt = decodePolyIdSalt(ref)
        val tile = tileById[tileId]
        val tileData = tile?.data
        if (tileData == null || tile.salt != salt) {
            return Status.FAILURE_INVALID_PARAM
        }
        val ip = decodePolyIdPoly(ref)
        if (ip >= tileData.polyCount) {
            return Status.FAILURE_INVALID_PARAM
        }
        val poly = tileData.polygons[ip]
        poly.area = area.code
        return Status.SUCCESS
    }

    fun getPolyArea(ref: Long): Result<Int> {
        if (ref == 0L) {
            return Result.failure()
        }
        val tileId = decodePolyIdTile(ref)
        val salt = decodePolyIdSalt(ref)
        val tile = tileById[tileId]
        val tileData = tile?.data
        if (tileData == null || tile.salt != salt) {
            return Result.invalidParam()
        }
        val ip = decodePolyIdPoly(ref)
        if (ip >= tileData.polyCount) {
            return Result.invalidParam()
        }
        val poly = tileData.polygons[ip]
        return Result.success(poly.area)
    }

    private fun getTileListByPos(x: Int, z: Int): MeshTile? {
        return tileByXY[computeTileHash(x, z)]
    }

    companion object {

        const val DT_SALT_BITS = 16
        const val DT_TILE_BITS = 28
        const val DT_POLY_BITS = 20

        const val saltMask = (1L shl DT_SALT_BITS) - 1
        const val tileMask = (1L shl DT_TILE_BITS) - 1
        const val polyMask = (1L shl DT_POLY_BITS) - 1

        const val DT_DETAIL_EDGE_BOUNDARY = 0x01

        /**
         * A flag that indicates that an entity links to an external entity.
         * (E.g. A polygon edge is a portal that links to another polygon.)
         */
        const val DT_EXT_LINK = 0x8000

        /**
         * A value that indicates the entity does not link to anything.
         */
        const val DT_NULL_LINK = -0x1

        /**
         * A flag that indicates that an off-mesh connection can be traversed in both directions. (Is bidirectional.)
         */
        const val DT_OFFMESH_CON_BIDIR = 1

        /**
         * The maximum number of user defined area ids.
         */
        const val DT_MAX_AREAS = 64

        /**
         * Limit raycasting during any angle pathfinding.
         * The limit is given as a multiple of the character radius
         */
        var DT_RAY_CAST_LIMIT_PROPORTIONS = 50f

        private val tripletVec3f = object : ThreadLocal<Array<Vector3f>>() {
            override fun initialValue() = Array(3) { Vector3f() }
        }

        private val tmpVertices = object : ThreadLocal<FloatArray>() {
            override fun initialValue() = FloatArray(6 * 3)
        }

        /**
         * Derives a standard polygon reference.
         *
         * @param salt The tile's salt value.
         * @param tileId   The index of the tile.
         * @param polygonId   The index of the polygon within the tile.
         * @return encoded polygon reference
         * @note This function is generally meant for internal use only.
         */
        fun encodePolyId(salt: Int, tileId: Int, polygonId: Int): Long {
            return salt.toLong().shl(DT_POLY_BITS + DT_TILE_BITS) or
                    (tileId.toLong().shl(DT_POLY_BITS)) or
                    polygonId.toLong()
        }

        /**
         * Extracts a tile's salt value from the specified polygon reference.
         * @note This function is generally meant for internal use only.
         * @see #encodePolyId
         * */
        fun decodePolyIdSalt(ref: Long): Int {
            return ref.shr(DT_POLY_BITS + DT_TILE_BITS).and(saltMask).toInt()
        }

        /**
         * Extracts the tile's index from the specified polygon reference.
         * @note This function is generally meant for internal use only.
         * @see #encodePolyId
         * */
        fun decodePolyIdTile(ref: Long): Int {
            return ref.shr(DT_POLY_BITS).and(tileMask).toInt()
        }

        /**
         * Extracts the polygon's index (within its tile) from the specified polygon reference.
         * @note This function is generally meant for internal use only.
         * @see #encodePolyId
         * */
        fun decodePolyIdPoly(ref: Long): Int {
            return ref.and(polyMask).toInt()
        }

        private fun getNavMeshParams(data: MeshData): NavMeshParams {
            val params = NavMeshParams()
            val header = data
            params.origin.set(header.bmin)
            params.tileWidth = header.bmax.x - header.bmin.x
            params.tileHeight = header.bmax.z - header.bmin.z
            params.maxPolys = header.polyCount
            return params
        }

        fun getSlabCoord(vertices: FloatArray, va: Int, side: Int): Float {
            return when (side) {
                0, 4 -> vertices[va]
                2, 6 -> vertices[va + 2]
                else -> 0f
            }
        }

        fun calcSlabEndPoints(vertices: FloatArray, va: Int, vb: Int, bmin: FloatArray, bmax: FloatArray, side: Int) {
            var d0 = 0
            var v0 = vb
            var v1 = va
            if (side == 0 || side == 4) {
                d0 = 2
                if (vertices[va + 2] < vertices[vb + 2]) {
                    v0 = va
                    v1 = vb
                }
            } else if (side == 2 || side == 6) {
                if (vertices[va] < vertices[vb]) {
                    v0 = va
                    v1 = vb
                }
            } else return
            bmin[0] = vertices[v0 + d0]
            bmin[1] = vertices[v0 + 1]
            bmax[0] = vertices[v1 + d0]
            bmax[1] = vertices[v1 + 1]
        }

        fun computeTileHash(x: Int, y: Int): Long {
            return x.toLong().shl(32) xor y.toLong()
        }

        /**
         * Get flags for edge in detail triangle.
         *
         * @param triFlags  The flags for the triangle (last component of detail vertices above).
         * @param edgeIndex The index of the first vertex of the edge. For instance, if 0,
         * @return flags for edge AB.
         */
        fun getDetailTriEdgeFlags(triFlags: Int, edgeIndex: Int): Int {
            return triFlags shr edgeIndex * 2 and 0x3
        }
    }
}