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
import org.recast4j.LongArrayList
import org.recast4j.Vectors.ilog2
import org.recast4j.Vectors.nextPow2
import org.recast4j.Vectors.overlapBounds
import org.recast4j.detour.NavMesh
import org.recast4j.detour.NavMesh.Companion.computeTileHash
import org.recast4j.detour.NavMeshBuilder.createNavMeshData
import org.recast4j.detour.NavMeshDataCreateParams
import org.recast4j.detour.tilecache.TileCacheObstacle.TileCacheObstacleType
import org.recast4j.detour.tilecache.io.TileCacheLayerHeaderReader
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.*

class TileCache(
    val params: TileCacheParams, private val storageParams: TileCacheStorageParams, val navMesh: NavMesh,
    private val tmProcess: TileCacheMeshProcess?
) {

    /** Tile hash lookup.  */
    private val posLookup = HashMap<Long, CompressedTile>()

    private val tiles: Array<CompressedTile?>

    private val numSaltBitsInTileID: Int
    private val numTileBitsInTileID: Int
    private val obstacles: MutableList<TileCacheObstacle> = ArrayList()
    private var nextFreeObstacle: TileCacheObstacle? = null
    private val requests: MutableList<ObstacleRequest> = ArrayList()
    private val update = LongArrayList()
    private val builder = TileCacheBuilder()
    private fun contains(a: LongArrayList, v: Long): Boolean {
        return a.contains(v)
    }

    private fun encodeTileId(salt: Int, it: Int): Long {
        return salt.toLong() shl numTileBitsInTileID or it.toLong()
    }

    private fun decodeTileIdSalt(ref: Long): Int {
        val saltMask = (1L shl numSaltBitsInTileID) - 1
        return (ref shr numTileBitsInTileID and saltMask).toInt()
    }

    private fun decodeTileIdTile(ref: Long): Int {
        val tileMask = (1L shl numTileBitsInTileID) - 1
        return (ref and tileMask).toInt()
    }

    private fun encodeObstacleId(salt: Int, it: Int): Long {
        return salt.toLong() shl 16 or it.toLong()
    }

    private fun decodeObstacleIdSalt(ref: Long): Int {
        val saltMask = (1L shl 16) - 1
        return (ref shr 16 and saltMask).toInt()
    }

    private fun decodeObstacleIdObstacle(ref: Long): Int {
        val tileMask = (1L shl 16) - 1
        return (ref and tileMask).toInt()
    }

    init {
        tiles = arrayOfNulls(params.maxTiles)
        numTileBitsInTileID = ilog2(nextPow2(params.maxTiles))
        numSaltBitsInTileID = min(31, 32 - numTileBitsInTileID)
        if (numSaltBitsInTileID < 10) {
            throw RuntimeException("Too few salt bits: $numSaltBitsInTileID")
        }
    }

    fun getTileByRef(ref: Long): CompressedTile? {
        if (ref == 0L) return null
        val tileIndex = decodeTileIdTile(ref)
        val tileSalt = decodeTileIdSalt(ref)
        if (tileIndex >= params.maxTiles) return null
        val tile = tiles[tileIndex]
        return if (tile!!.salt != tileSalt) null else tile
    }

    fun getTilesAt(tx: Int, ty: Int): LongArrayList {
        val tiles = LongArrayList()

        // Find tile based on hash.
        val h = computeTileHash(tx, ty)
        var tile = posLookup[h]
        while (tile != null) {
            if (tile.tx == tx && tile.ty == ty) {
                tiles.add(getTileRef(tile))
            }
            tile = tile.next
        }
        return tiles
    }

    fun getTileAt(tx: Int, ty: Int, tlayer: Int): CompressedTile? {
        // Find tile based on hash.
        var tile = posLookup[computeTileHash(tx, ty)]
        while (tile != null) {
            if (tile.tx == tx && tile.ty == ty && tile.tlayer == tlayer) {
                return tile
            }
            tile = tile.next
        }
        return null
    }

    fun getTileRef(tile: CompressedTile?): Long {
        if (tile == null) {
            return 0
        }
        val it = tile.index
        return encodeTileId(tile.salt, it)
    }

    fun getObstacleRef(ob: TileCacheObstacle?): Long {
        if (ob == null) return 0
        val idx = ob.index
        return encodeObstacleId(ob.salt, idx)
    }

    fun getObstacleByRef(ref: Long): TileCacheObstacle? {
        if (ref == 0L) return null
        val idx = decodeObstacleIdObstacle(ref)
        if (idx >= obstacles.size) return null
        val ob = obstacles[idx]
        val salt = decodeObstacleIdSalt(ref)
        return if (ob.salt != salt) null else ob
    }

    @Throws(IOException::class)
    fun addTile(data: ByteArray, flags: Int): Long {
        // Make sure the data is in right format.
        val buf = ByteBuffer.wrap(data)
        buf.order(storageParams.byteOrder)
        // todo assign proper index...
        val tile = TileCacheLayerHeaderReader.read(buf, storageParams.cCompatibility, CompressedTile(0))
        // Make sure the location is free.
        if (getTileAt(tile.tx, tile.ty, tile.tlayer) != null) {
            return 0
        }
        // Insert tile into the position lut.
        val h = computeTileHash(tile.tx, tile.ty)
        tile.next = posLookup.put(h, tile)

        // Init tile.
        tile.data = data
        tile.compressed = align4(buf.position())
        tile.flags = flags
        return getTileRef(tile)
    }

    private fun align4(i: Int): Int {
        return i + 3 and 3.inv()
    }

    fun removeTile(ref: Long) {
        if (ref == 0L) {
            throw RuntimeException("Invalid tile ref")
        }
        val tileIndex = decodeTileIdTile(ref)
        val tileSalt = decodeTileIdSalt(ref)
        if (tileIndex >= params.maxTiles) {
            throw RuntimeException("Invalid tile index")
        }
        val tile = tiles[tileIndex]
        if (tile!!.salt != tileSalt) {
            throw RuntimeException("Invalid tile salt")
        }

        // Remove tile from hash lookup.
        posLookup.remove(computeTileHash(tile.tx, tile.ty))
        tile.data = null
        tile.compressed = 0
        tile.flags = 0

        // Update salt, salt should never be zero.
        tile.salt = tile.salt + 1 and (1 shl numSaltBitsInTileID) - 1
        if (tile.salt == 0) {
            tile.salt++
        }
    }

    /**
     * Cylinder obstacle
     */
    fun addObstacle(pos: Vector3f, radius: Float, height: Float): Long {
        val ob = allocObstacle()
        ob.type = TileCacheObstacleType.CYLINDER
        ob.pos.set(pos)
        ob.radius = radius
        ob.height = height
        return addObstacleRequest(ob).ref
    }

    /**
     * Aabb obstacle
     */
    fun addBoxObstacle(bmin: FloatArray, bmax: FloatArray): Long {
        val ob = allocObstacle()
        ob.type = TileCacheObstacleType.BOX
        ob.bmin.set(bmin)
        ob.bmax.set(bmax)
        return addObstacleRequest(ob).ref
    }

    /**
     * Box obstacle: can be rotated in Y
     */
    fun addBoxObstacle(center: Vector3f, extents: FloatArray, yRadians: Float): Long {
        val ob = allocObstacle()
        ob.type = TileCacheObstacleType.ORIENTED_BOX
        ob.center.set(center)
        ob.extents.set(extents)
        val coshalf = cos((0.5f * yRadians))
        val sinhalf = sin((-0.5f * yRadians))
        ob.rotAux[0] = coshalf * sinhalf
        ob.rotAux[1] = coshalf * coshalf - 0.5f
        return addObstacleRequest(ob).ref
    }

    private fun addObstacleRequest(ob: TileCacheObstacle): ObstacleRequest {
        val req = ObstacleRequest()
        req.action = ObstacleRequestAction.REQUEST_ADD
        req.ref = getObstacleRef(ob)
        requests.add(req)
        return req
    }

    fun removeObstacle(ref: Long) {
        if (ref == 0L) {
            return
        }
        val req = ObstacleRequest()
        req.action = ObstacleRequestAction.REQUEST_REMOVE
        req.ref = ref
        requests.add(req)
    }

    private fun allocObstacle(): TileCacheObstacle {
        var o = nextFreeObstacle
        if (o == null) {
            o = TileCacheObstacle(obstacles.size)
            obstacles.add(o)
        } else {
            nextFreeObstacle = o.next
        }
        o.state = ObstacleState.DT_OBSTACLE_PROCESSING
        o.touched.clear()
        o.pending.clear()
        o.next = null
        return o
    }

    fun queryTiles(bmin: Vector3f, bmax: Vector3f): LongArrayList {
        val results = LongArrayList()
        val tw = params.width * params.cellSize
        val th = params.height * params.cellSize
        val tx0 = floor(((bmin.x - params.orig.x) / tw)).toInt()
        val tx1 = floor(((bmax.x - params.orig.x) / tw)).toInt()
        val ty0 = floor(((bmin.z - params.orig.z) / th)).toInt()
        val ty1 = floor(((bmax.z - params.orig.z) / th)).toInt()
        val tbmin = Vector3f()
        val tbmax = Vector3f()
        for (ty in ty0..ty1) {
            for (tx in tx0..tx1) {
                val tiles = getTilesAt(tx, ty)
                var i = 0
                val l = tiles.size
                while (i < l) {
                    val t = tiles[i]
                    val tile = this.tiles[decodeTileIdTile(t)]
                    calcTightTileBounds(tile!!, tbmin, tbmax)
                    if (overlapBounds(bmin, bmax, tbmin, tbmax)) {
                        results.add(t)
                    }
                    i++
                }
            }
        }
        return results
    }

    /**
     * Updates the tile cache by rebuilding tiles touched by unfinished obstacle requests.
     *
     * @return Returns true if the tile cache is fully up to date with obstacle requests and tile rebuilds. If the tile
     * cache is up to date another (immediate) call to update will have no effect; otherwise another call will
     * continue processing obstacle requests and tile rebuilds.
     */
    fun update(): Boolean {
        if (update.isEmpty()) {
            // Process requests.
            for (req in requests) {
                val idx = decodeObstacleIdObstacle(req.ref)
                if (idx >= obstacles.size) {
                    continue
                }
                val ob = obstacles[idx]
                val salt = decodeObstacleIdSalt(req.ref)
                if (ob.salt != salt) {
                    continue
                }
                if (req.action == ObstacleRequestAction.REQUEST_ADD) {
                    // Find touched tiles.
                    val bmin = Vector3f()
                    val bmax = Vector3f()
                    getObstacleBounds(ob, bmin, bmax)
                    ob.touched = queryTiles(bmin, bmax)
                    // Add tiles to update list.
                    ob.pending.clear()
                    var i = 0
                    val l = ob.touched.size
                    while (i < l) {
                        val j = ob.touched[i]
                        if (!contains(update, j)) {
                            update.add(j)
                        }
                        ob.pending.add(j)
                        i++
                    }
                } else if (req.action == ObstacleRequestAction.REQUEST_REMOVE) {
                    // Prepare to remove obstacle.
                    ob.state = ObstacleState.DT_OBSTACLE_REMOVING
                    // Add tiles to update list.
                    ob.pending.clear()
                    var i = 0
                    val l = ob.touched.size
                    while (i < l) {
                        val j = ob.touched[i]
                        if (!contains(update, j)) {
                            update.add(j)
                        }
                        ob.pending.add(j)
                        i++
                    }
                }
            }
            requests.clear()
        }

        // Process updates
        if (!update.isEmpty()) {
            val ref = update.remove(0)
            // Build mesh
            buildNavMeshTile(ref)

            // Update obstacle states.
            for (ob in obstacles) {
                if (ob.state == ObstacleState.DT_OBSTACLE_PROCESSING
                    || ob.state == ObstacleState.DT_OBSTACLE_REMOVING
                ) {
                    // Remove handled tile from pending list.
                    ob.pending.remove(ref)

                    // If all pending tiles processed, change state.
                    if (ob.pending.isEmpty()) {
                        if (ob.state == ObstacleState.DT_OBSTACLE_PROCESSING) {
                            ob.state = ObstacleState.DT_OBSTACLE_PROCESSED
                        } else if (ob.state == ObstacleState.DT_OBSTACLE_REMOVING) {
                            ob.state = ObstacleState.DT_OBSTACLE_EMPTY
                            // Update salt, salt should never be zero.
                            ob.salt = ob.salt + 1 and (1 shl 16) - 1
                            if (ob.salt == 0) {
                                ob.salt++
                            }
                            // Return obstacle to free list.
                            ob.next = nextFreeObstacle
                            nextFreeObstacle = ob
                        }
                    }
                }
            }
        }
        return update.isEmpty() && requests.isEmpty()
    }

    fun buildNavMeshTile(ref: Long) {
        val idx = decodeTileIdTile(ref)
        if (idx > params.maxTiles) {
            throw RuntimeException("Invalid tile index")
        }
        val tile = tiles[idx]
        val salt = decodeTileIdSalt(ref)
        if (tile!!.salt != salt) {
            throw RuntimeException("Invalid tile salt")
        }
        val walkableClimbVx = (params.walkableClimb / params.cellHeight).toInt()

        // Decompress tile layer data.
        val layer = decompressTile(tile)

        // Rasterize obstacles.
        for (ob in obstacles) {
            if (ob.state == ObstacleState.DT_OBSTACLE_EMPTY || ob.state == ObstacleState.DT_OBSTACLE_REMOVING) {
                continue
            }
            if (contains(ob.touched, ref)) {
                when (ob.type) {
                    TileCacheObstacleType.CYLINDER -> {
                        builder.markCylinderArea(
                            layer, tile.bmin, params.cellSize, params.cellHeight, ob.pos, ob.radius,
                            ob.height, 0
                        )
                    }
                    TileCacheObstacleType.BOX -> {
                        builder.markBoxArea(
                            layer,
                            tile.bmin,
                            params.cellSize,
                            params.cellHeight,
                            ob.bmin,
                            ob.bmax,
                            0
                        )
                    }
                    TileCacheObstacleType.ORIENTED_BOX -> {
                        builder.markBoxArea(
                            layer, tile.bmin, params.cellSize, params.cellHeight, ob.center, ob.extents,
                            ob.rotAux, 0
                        )
                    }
                    else -> {}
                }
            }
        }
        // Build navmesh
        builder.buildTileCacheRegions(layer, walkableClimbVx)
        val contours = builder.buildTileCacheContours(
            layer, walkableClimbVx,
            params.maxSimplificationError
        )
        val polyMesh = builder.buildTileCachePolyMesh(contours, navMesh.maxVerticesPerPoly)
        // Early out if the mesh tile is empty.
        if (polyMesh.numPolygons == 0) {
            navMesh.removeTile(navMesh.getTileRefAt(tile.tx, tile.ty, tile.tlayer))
            return
        }
        val params = NavMeshDataCreateParams()
        params.vertices = polyMesh.vertices
        params.vertCount = polyMesh.numVertices
        params.polys = polyMesh.polys
        params.polyAreas = polyMesh.areas
        params.polyFlags = polyMesh.flags
        params.polyCount = polyMesh.numPolygons
        params.maxVerticesPerPolygon = navMesh.maxVerticesPerPoly
        params.walkableHeight = this.params.walkableHeight
        params.walkableRadius = this.params.walkableRadius
        params.walkableClimb = this.params.walkableClimb
        params.tileX = tile.tx
        params.tileZ = tile.ty
        params.tileLayer = tile.tlayer
        params.cellSize = this.params.cellSize
        params.cellHeight = this.params.cellHeight
        params.buildBvTree = false
        params.bmin = tile.bmin
        params.bmax = tile.bmax
        tmProcess?.process(params)
        val meshData = createNavMeshData(params)
        // Remove existing tile.
        navMesh.removeTile(navMesh.getTileRefAt(tile.tx, tile.ty, tile.tlayer))
        // Add new tile, or leave the location empty. if (navData) { // Let the
        if (meshData != null) {
            navMesh.addTile(meshData, 0, 0)
        }
    }

    fun decompressTile(tile: CompressedTile): TileCacheLayer {
        return builder.decompressTileCacheLayer(
            tile.data!!, storageParams.byteOrder,
            storageParams.cCompatibility
        )
    }

    fun calcTightTileBounds(header: TileCacheLayerHeader, bmin: Vector3f, bmax: Vector3f) {
        val cs = params.cellSize
        bmin.x = header.bmin.x + header.minx * cs
        bmin.y = header.bmin.y
        bmin.z = header.bmin.z + header.miny * cs
        bmax.x = header.bmin.x + (header.maxx + 1) * cs
        bmax.y = header.bmax.y
        bmax.z = header.bmin.z + (header.maxy + 1) * cs
    }

    fun getObstacleBounds(ob: TileCacheObstacle, bmin: Vector3f, bmax: Vector3f) {
        when (ob.type) {
            TileCacheObstacleType.CYLINDER -> {
                val pos = ob.pos
                bmin.x = pos.x - ob.radius
                bmin.y = pos.y
                bmin.z = pos.z - ob.radius
                bmax.x = pos.x + ob.radius
                bmax.y = pos.y + ob.height
                bmax.z = pos.z + ob.radius
            }
            TileCacheObstacleType.BOX -> {
                bmin.set(ob.bmin)
                bmax.set(ob.bmax)
            }
            TileCacheObstacleType.ORIENTED_BOX -> {
                val maxRadius = 1.41f * max(ob.extents.x, ob.extents.z)
                val center = ob.center
                bmin.x = center.x - maxRadius
                bmax.x = center.x + maxRadius
                bmin.y = center.y - ob.extents.y
                bmax.y = center.y + ob.extents.y
                bmin.z = center.z - maxRadius
                bmax.z = center.z + maxRadius
            }
            else -> {}
        }
    }

    val tileCount: Int
        get() = params.maxTiles

    fun getTile(i: Int): CompressedTile? {
        return tiles[i]
    }
}